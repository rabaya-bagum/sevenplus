"""Turns an exercise definition (a few key poses) into a rendered, looping clip.

Each key pose is solved before it is keyframed:
  * `free`:   pose keys the solver may adjust (e.g. "pitch", "L_sh_flex")
  * `goals`:  what must hold, e.g. {"parts": ["L_hand", "R_hand"], "z": 0}
              (lowest point of those parts at that height) or
              {"parts": [...], "xy": "first"} (stay where they were in key 0)
  * then the whole body is lowered/raised so `support` parts rest at `height`
    (plus `lift` for jumps), and slid so `anchor` parts keep their floor position.
"""
import math
import os
import shutil
import subprocess

import bpy
import numpy as np
from scipy.optimize import minimize

import mannequin as M
import stage as S

FPS = 30


def parts(prefixes):
    """Meshes whose names start with any of `prefixes` ("all" = every visible part).
    Contact markers (e.g. "L_toe_mark") are only included when asked for."""
    if prefixes in (None, "all"):
        return S.body_meshes()
    objs = [o for o in bpy.data.objects if o.type == "MESH" and o.parent is not None]
    found = [o for o in objs if any(o.name.startswith(p) for p in prefixes)]
    assert found, f"no parts match {prefixes}"
    return found


def stats(prefixes):
    v = S.world_verts(parts(prefixes))
    return v[:, 2].min(), v[:, :2].mean(axis=0), v


class Solver:
    def __init__(self, J, exercise):
        self.J = J
        self.ex = exercise
        self.solved = []           # poses of keys already solved

    # -- references to earlier keys -------------------------------------------
    def _centroid_at(self, key_index, prefixes):
        M.apply_pose(self.J, self.solved[key_index])
        _, c, _ = stats(prefixes)
        return c

    def _target_xy(self, spec, prefixes):
        if isinstance(spec, str):            # "first" or "key:N"
            k = 0 if spec == "first" else int(spec.split(":")[1])
            return self._centroid_at(k, prefixes)
        return np.array(spec, dtype=float)

    # -- pose completion ---------------------------------------------------------
    @staticmethod
    def _expand(pose, free, x):
        p = dict(pose)
        for k, v in zip(free, x):
            if k.startswith("both:"):
                p["L_" + k[5:]] = v
                p["R_" + k[5:]] = v
            else:
                p[k] = v
        return p

    def _finish(self, p, key):
        for k, fn in {**self.ex.get("tie", {}), **key.get("tie", {})}.items():
            p[k] = fn(p)
        if key.get("flat_feet", self.ex.get("flat_feet")):
            for s in key.get("flat_sides", self.ex.get("flat_sides", ("L", "R"))):
                p[f"{s}_ankle"] = p.get(f"{s}_hip_flex", 0) - p.get("pitch", 0) - p.get(f"{s}_knee", 0)
        return p

    def _place(self, pose, key):
        """Vertical grounding and horizontal anchoring (exact, no search)."""
        p = self._finish(dict(pose), key)
        p.setdefault("x", 0.0)
        p.setdefault("y", 0.0)
        p["z"] = 2.0
        M.apply_pose(self.J, p)
        support = key.get("support", self.ex.get("support", "all"))
        zmin, _, _ = stats(support)
        p["z"] += key.get("height", self.ex.get("height", 0.0)) - zmin + key.get("lift", 0.0)
        anchor = key.get("anchor", self.ex.get("anchor"))
        if anchor:
            M.apply_pose(self.J, p)
            _, c, _ = stats(anchor)
            tx, ty = key.get("anchor_xy", self.ex.get("anchor_xy", (0.0, 0.0)))
            p["x"] += tx - c[0]
            p["y"] += ty - c[1]
        M.apply_pose(self.J, p)
        return p

    def _error(self, p, key, targets):
        err = 0.0
        for g, target in zip(key.get("goals", []), targets):
            zmin, c, _ = stats(g["parts"])
            if "z" in g:
                err += 100 * (zmin - g["z"]) ** 2
            if "xy" in g:
                err += 100 * float(((c - target) ** 2).sum())
            if "y" in g:
                err += 100 * (c[1] - target[1]) ** 2
            if "ymax" in g:
                _, _, v = stats(g["parts"])
                over = float(v[:, 1].max()) - g["ymax"]
                if over > 0:
                    err += 1000 * over ** 2
        allmin, _, _ = stats("all")
        floor = key.get("floor", 0.0)
        if allmin < floor - 0.003:
            err += 1000 * (allmin - floor) ** 2
        return err

    def solve(self, key):
        pose = dict(key["pose"])
        free = key.get("free", [])
        # Resolve goal targets that point at earlier keys before searching.
        goals, targets = [], []
        for g in key.get("goals", []):
            spec = g.get("xy", g.get("y"))
            if spec is None:                                   # height-only goal
                goals.append(g)
                targets.append(None)
            elif spec == "first" and not self.solved:          # key 0 defines "first"
                if "z" in g:
                    goals.append({k: v for k, v in g.items() if k not in ("xy", "y")})
                    targets.append(None)
            else:
                goals.append(g)
                targets.append(self._target_xy(spec, g["parts"]))
        key = dict(key, goals=goals)
        if free:
            x0 = np.array([pose.get(k if not k.startswith("both:") else "L_" + k[5:], 0.0) for k in free], float)

            def f(x):
                return self._error(self._place(self._expand(pose, free, x), key), key, targets)

            simplex = np.vstack([x0] + [x0 + np.eye(len(free))[i] * (0.08 if free[i] in ("x", "y") else 8.0)
                                        for i in range(len(free))])
            res = minimize(f, x0, method="Nelder-Mead",
                           options={"xatol": 0.02, "fatol": 1e-8, "maxiter": 500 * len(free),
                                    "initial_simplex": simplex})
            pose = self._expand(pose, free, res.x)
            residual = res.fun
        else:
            residual = self._error(self._place(pose, key), key, targets)
        p = self._place(pose, key)
        self.solved.append(p)
        if residual > 0.05:
            print(f"  ! {self.ex['id']} key {len(self.solved) - 1}: goals not fully met (error {residual:.3f})")
        return p


def joints_to_key(J):
    return [J[n] for n in J]


def build_animation(J, exercise):
    """Solves and keyframes every key pose. Returns (frame count, solved poses)."""
    n = int(round(exercise["seconds"] * FPS))
    solver = Solver(J, exercise)
    keys = exercise["keys"]
    # Keys are solved in order so later goals can refer to earlier keys.
    solved = [solver.solve(k) for k in keys]
    for (t, p) in [(k["t"], s) for k, s in zip(keys, solved)] + [(1.0, solved[0])]:
        frame = int(round(t * n))
        M.apply_pose(J, p)
        for ob in J.values():
            ob.keyframe_insert("rotation_euler", frame=frame)
            ob.keyframe_insert("location", frame=frame)
    for ob in J.values():
        if ob.animation_data and ob.animation_data.action:
            for fc in _fcurves(ob.animation_data.action):
                for kp in fc.keyframe_points:
                    kp.interpolation = exercise.get("interpolation", "BEZIER")
                    kp.handle_left_type = kp.handle_right_type = "AUTO_CLAMPED"
                fc.update()
    return n, solved


def _fcurves(action):
    """F-curves of an action (Blender 4.4+ layered actions or older flat ones)."""
    if hasattr(action, "fcurves") and len(action.fcurves):
        return list(action.fcurves)
    out = []
    for layer in getattr(action, "layers", []):
        for strip in layer.strips:
            for bag in getattr(strip, "channelbags", []):
                out.extend(bag.fcurves)
    return out


def all_frames_points(J, n, step=3):
    """Body vertices over the whole loop, for camera framing."""
    sc = bpy.context.scene
    pts = []
    for f in range(0, n, step):
        sc.frame_set(f)
        pts.append(S.world_verts(S.body_meshes() + S.props())[::7])
    return np.vstack(pts)


def render_clip(exercise, outdir, size=720, themes=("light", "dark"), workdir=None, preview_frames=None):
    """Builds the scene, renders every frame of the loop and encodes MP4s.

    `preview_frames`: if given, only those frames are rendered (as PNGs) for checking.
    Returns the list of written files."""
    sc = S.reset_scene()
    sc.render.resolution_x = sc.render.resolution_y = size
    J = M.build()
    n, solved = build_animation(J, exercise)
    for prop in exercise.get("props", []):
        M.apply_pose(J, solved[0])
        bpy.context.view_layer.update()
        prop(J, solved)
    sc.frame_set(0)
    sc.frame_start, sc.frame_end = 0, n - 1
    view = exercise.get("view", "three_quarter")
    pts = all_frames_points(J, n)
    center = pts[:, :2].mean(axis=0)
    S.backdrop(view, center=(float(center[0]), float(center[1])))
    sc.frame_set(0)
    S.frame_camera(pts, view, margin=exercise.get("margin", 1.15))

    workdir = workdir or os.path.join(outdir, "_work", exercise["id"])
    os.makedirs(workdir, exist_ok=True)
    floor_img = S.empty_floor(os.path.join(workdir, "floor.png"))
    frames = preview_frames if preview_frames is not None else range(n)
    written = []
    for f in frames:
        sc.frame_set(f)
        outs = {t: os.path.join(workdir, f"{t}_{f:04d}.png") for t in themes}
        S.composite_frame(workdir, floor_img, outs)
        written += outs.values()
    if preview_frames is not None:
        return written
    files = []
    for t in themes:
        suffix = "" if t == "light" else "-dark"
        out = os.path.join(outdir, f"{exercise['id']}{suffix}.mp4")
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-framerate", str(FPS),
                        "-i", os.path.join(workdir, f"{t}_%04d.png"),
                        "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "24", "-preset", "slow",
                        "-tune", "animation", "-movflags", "+faststart", "-an", out], check=True)
        files.append(out)
    shutil.rmtree(workdir, ignore_errors=True)
    return files
