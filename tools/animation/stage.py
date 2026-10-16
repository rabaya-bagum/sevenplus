"""Scene, lights, floor, camera framing and rendering for exercise clips."""
import math
import os

import bpy
import numpy as np
from mathutils import Vector

import mannequin as M


def reset_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    sc = bpy.context.scene
    sc.render.engine = "BLENDER_WORKBENCH"
    sh = sc.display.shading
    sh.light = "STUDIO"
    sh.studio_light = "paint.sl"
    sh.use_world_space_lighting = True
    sh.color_type = "MATERIAL"
    sh.show_shadows = True
    sh.shadow_intensity = 0.35
    sh.show_cavity = True
    sh.cavity_type = "WORLD"
    sh.curvature_ridge_factor = 0.6
    sh.curvature_valley_factor = 0.6
    sh.show_object_outline = False
    sh.show_specular_highlight = True
    sc.display.light_direction = (-0.12, -0.18, 0.976)  # nearly overhead: a pool of shadow under the body
    sc.display.shadow_shift = 0.02
    sc.display.shadow_focus = 0.35
    world = bpy.data.worlds.new("bg")
    sc.world = world
    world.color = (0.62, 0.63, 0.65)
    sc.view_settings.view_transform = "Standard"
    sc.view_settings.exposure = 0.7
    sc.render.film_transparent = False
    sc.render.resolution_x = sc.render.resolution_y = 720
    sc.render.fps = 30
    sc.display.render_aa = "8"
    return sc


def backdrop(view, center=(0.0, 0.0), depth=2.2, radius=1.6, height=6.0, width=14.0):
    """Studio sweep: a floor that curves up into a wall behind the subject, so
    there is no horizon line. Built facing the camera for `view`."""
    import bmesh
    old = bpy.data.objects.get("backdrop")
    if old:
        bpy.data.objects.remove(old, do_unlink=True)
    d = view_dir(view)
    back = Vector((-d.x, -d.y, 0)).normalized()          # away from the camera, on the floor
    side = Vector((back.y, -back.x, 0))
    # Profile in (u = distance along `back`, z): floor from the camera side, arc, wall.
    prof = [(-30.0, 0.0), (-4.0, 0.0), (depth - 0.4, 0.0), (depth, 0.0)]
    for i in range(1, 17):
        a = math.radians(90 * i / 16)
        prof.append((depth + radius * math.sin(a), radius * (1 - math.cos(a))))
    prof.append((depth + radius, height))
    bm = bmesh.new()
    rows = []
    for w in (-width / 2, width / 2):
        rows.append([bm.verts.new(Vector((center[0], center[1], 0)) + back * u + side * w + Vector((0, 0, z)))
                     for u, z in prof])
    for i in range(len(prof) - 1):
        bm.faces.new((rows[0][i], rows[0][i + 1], rows[1][i + 1], rows[1][i]))
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    me = bpy.data.meshes.new("backdrop")
    bm.to_mesh(me)
    bm.free()
    for p in me.polygons:
        p.use_smooth = p.center.z > 0.001      # flat floor, smooth curve
    ob = bpy.data.objects.new("backdrop", me)
    bpy.context.scene.collection.objects.link(ob)
    ob.data.materials.append(M.material("floor"))
    # Normals must face up / toward the camera.
    if ob.data.polygons[0].normal.z < 0:
        ob.data.flip_normals()
    return ob


def body_meshes():
    """Visible character meshes (not the solver's contact markers)."""
    return [o for o in bpy.data.objects
            if o.type == "MESH" and o.parent is not None and not o.name.endswith("_mark")]


def world_verts(objs):
    """All vertex positions (N, 3) of the given objects in world space."""
    bpy.context.view_layer.update()
    out = []
    for o in objs:
        n = len(o.data.vertices)
        co = np.empty(n * 3)
        o.data.vertices.foreach_get("co", co)
        co = co.reshape(n, 3)
        m = np.array(o.matrix_world)
        out.append(co @ m[:3, :3].T + m[:3, 3])
    return np.vstack(out)


def view_dir(view):
    """Unit vector from subject toward the camera."""
    az, el = {"front": (0, 12), "three_quarter": (-35, 14), "side": (-90, 6),
              "side_high": (-90, 22), "back_three_quarter": (-145, 14)}[view]
    az, el = math.radians(az), math.radians(el)
    # azimuth 0 = in front of the character (it faces -Y), negative = toward its right
    return Vector((math.sin(az) * math.cos(el), -math.cos(az) * math.cos(el), math.sin(el)))


def frame_camera(points, view, margin=1.18):
    """Orthographic camera looking along `view` that fits every point."""
    sc = bpy.context.scene
    cam = bpy.data.objects.get("cam") or bpy.data.objects.new("cam", bpy.data.cameras.new("cam"))
    if cam.name not in sc.collection.objects:
        sc.collection.objects.link(cam)
    cam.data.type = "ORTHO"
    d = view_dir(view)
    center = Vector(points.mean(axis=0))
    lo, hi = points.min(axis=0), points.max(axis=0)
    center = Vector(((lo + hi) / 2).tolist())
    cam.location = center + d * 10
    cam.rotation_euler = (-d).to_track_quat("-Z", "Y").to_euler()
    bpy.context.view_layer.update()
    # Size from points projected onto the camera plane.
    m = np.array(cam.matrix_world.inverted())
    local = points @ m[:3, :3].T + m[:3, 3]
    w = local[:, 0].max() - local[:, 0].min()
    h = local[:, 1].max() - local[:, 1].min()
    cx = (local[:, 0].max() + local[:, 0].min()) / 2
    cy = (local[:, 1].max() + local[:, 1].min()) / 2
    cam.data.shift_x = 0
    cam.data.ortho_scale = max(w, h) * margin
    # Re-centre on the projected box, keeping the floor a little below centre.
    right = cam.matrix_world.to_3x3() @ Vector((1, 0, 0))
    up = cam.matrix_world.to_3x3() @ Vector((0, 1, 0))
    cam.location += right * cx + up * (cy + max(w, h) * 0.02)
    sc.camera = cam
    return cam


def render_still(path):
    sc = bpy.context.scene
    sc.render.image_settings.file_format = "PNG"
    sc.render.filepath = os.path.abspath(path)
    bpy.ops.render.render(write_still=True)


# ------------------------------------------------------------ compositing
# Workbench can't make a floor bright enough without washing out the
# character, so each frame is rendered twice and composited:
#   1. character only, transparent film  -> colour + alpha
#   2. character + floor                  -> divided by the empty floor = shadow
# and laid over a clean background whose colour we choose (light and dark).
from PIL import Image

THEMES = {
    "light": {"top": (246, 247, 249), "bottom": (229, 231, 235), "shadow": 0.42},
    "dark": {"top": (40, 41, 45), "bottom": (24, 25, 28), "shadow": 0.6},
}


def _render_png(path, transparent, show_floor):
    sc = bpy.context.scene
    sc.render.film_transparent = transparent
    bd = bpy.data.objects.get("backdrop")
    if bd:
        bd.hide_render = not show_floor
    for o in body_meshes() + props():
        o.hide_render = False
    sc.render.image_settings.file_format = "PNG"
    sc.render.image_settings.color_mode = "RGBA" if transparent else "RGB"
    sc.render.filepath = os.path.abspath(path)
    bpy.ops.render.render(write_still=True)
    return np.asarray(Image.open(path), dtype=np.float32) / 255.0


def props():
    return [o for o in bpy.data.objects if o.type == "MESH" and o.name.startswith("prop_")]


def empty_floor(tmp):
    """Floor with nothing on it, for the shadow ratio."""
    hidden = body_meshes() + props()
    for o in hidden:
        o.hide_render = True
    sc = bpy.context.scene
    sc.render.film_transparent = False
    sc.render.image_settings.file_format = "PNG"
    sc.render.image_settings.color_mode = "RGB"
    sc.render.filepath = os.path.abspath(tmp)
    bpy.ops.render.render(write_still=True)
    for o in hidden:
        o.hide_render = False
    return np.asarray(Image.open(tmp), dtype=np.float32) / 255.0


def gradient(theme, h, w):
    t = np.linspace(0, 1, h)[:, None, None]
    top = np.array(theme["top"], np.float32) / 255
    bottom = np.array(theme["bottom"], np.float32) / 255
    return np.broadcast_to(top * (1 - t) + bottom * t, (h, w, 3)).copy()


def composite_frame(tmpdir, floor_img, out_paths):
    """Renders the current frame and writes one PNG per theme to out_paths[theme]."""
    fg = _render_png(os.path.join(tmpdir, "fg.png"), transparent=True, show_floor=False)
    lit = _render_png(os.path.join(tmpdir, "lit.png"), transparent=False, show_floor=True)
    ratio = np.clip(lit.mean(axis=2) / np.maximum(floor_img.mean(axis=2), 1e-3), 0, 1)
    alpha = fg[..., 3:4]
    h, w = alpha.shape[:2]
    for name, path in out_paths.items():
        theme = THEMES[name]
        bg = gradient(theme, h, w) * (1 - theme["shadow"] * (1 - ratio))[..., None]
        out = fg[..., :3] * alpha + bg * (1 - alpha)
        Image.fromarray((np.clip(out, 0, 1) * 255 + 0.5).astype(np.uint8)).save(path)
