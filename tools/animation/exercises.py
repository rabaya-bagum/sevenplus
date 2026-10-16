"""Key poses for every exercise in the SevenPlus library.

Angles are degrees (see POSE KEYS in mannequin.py). `t` is the position in the
loop (0..1); the loop closes back on the first key automatically. See clips.py
for `free`, `goals`, `support`, `anchor`, `tie` and `flat_feet`.
"""
import mannequin as M

SOLES = ["L_sole", "R_sole"]
TOES = ["L_toe", "R_toe"]
HANDS = ["L_hand", "R_hand"]
PALMS = ["L_palm", "R_palm"]
FOREARMS = ["L_forearm", "R_forearm"]
SHINS = ["L_shin", "R_shin"]
BACK = ["torso", "neckpart", "head", "hair"]

STEP_H = 0.40          # step-up box height
SEAT_H = 0.45          # dip bench height


def both(**kw):
    """Same value on both sides: both(sh_flex=90) -> L_sh_flex and R_sh_flex."""
    out = {}
    for k, v in kw.items():
        out[f"L_{k}"] = v
        out[f"R_{k}"] = v
    return out


def pose(*dicts, **kw):
    p = {}
    for d in dicts:
        p.update(d)
    p.update(kw)
    return p


def mirror(p):
    """Swap left and right (and flip sideways directions)."""
    out = {}
    for k, v in p.items():
        if k.startswith("L_"):
            out["R_" + k[2:]] = v
        elif k.startswith("R_"):
            out["L_" + k[2:]] = v
        elif k in ("roll", "yaw", "spine_side", "spine_twist", "x"):
            out[k] = -v
        else:
            out[k] = v
    return out


def arms_vertical(offset=0.0):
    """Upper arms point straight down whatever the body pitch (prone poses)."""
    return {"L_sh_flex": lambda p: p.get("pitch", 0) + offset,
            "R_sh_flex": lambda p: p.get("pitch", 0) + offset}


def forearms_vertical():
    """Forearms point straight down: shoulder flex = pitch - elbow."""
    return {"L_sh_flex": lambda p: p.get("pitch", 0) - p.get("L_elbow", 0),
            "R_sh_flex": lambda p: p.get("pitch", 0) - p.get("R_elbow", 0)}


ARMS_DOWN = both(sh_abd=6, elbow=8)
RELAXED = pose(ARMS_DOWN)


# ===================================================================== cardio
def jumping_jacks():
    closed = pose(both(sh_abd=8, elbow=10, knee=6), neck_bend=-4)
    mid = pose(both(sh_abd=95, elbow=6, hip_abd=10, knee=10, ankle=25))
    opened = pose(both(sh_abd=168, elbow=8, hip_abd=17, knee=8))
    return {"id": "jumping-jacks", "view": "front", "seconds": 1.1,
            "support": TOES + SOLES, "anchor": SOLES,
            "keys": [{"t": 0.0, "pose": closed},
                     {"t": 0.25, "pose": mid, "lift": 0.07},
                     {"t": 0.5, "pose": opened},
                     {"t": 0.75, "pose": mid, "lift": 0.07}]}


def step_jacks():
    stand = pose(both(sh_abd=8, elbow=10))
    out_l = pose(both(sh_abd=165, elbow=8), L_hip_abd=20, R_hip_abd=3, R_knee=4)
    goals = [{"parts": ["L_sole"], "z": 0}, {"parts": ["R_sole"], "z": 0}, {"parts": ["R_sole"], "xy": "first"}]
    goals_r = [{"parts": ["L_sole"], "z": 0}, {"parts": ["R_sole"], "z": 0}, {"parts": ["L_sole"], "xy": "first"}]
    return {"id": "step-jacks", "view": "front", "seconds": 2.2,
            "support": SOLES,
            "keys": [{"t": 0.0, "pose": stand, "anchor": SOLES},
                     {"t": 0.25, "pose": out_l, "free": ["roll", "x", "y"], "goals": goals},
                     {"t": 0.5, "pose": stand, "anchor": SOLES},
                     {"t": 0.75, "pose": mirror(out_l), "free": ["roll", "x", "y"], "goals": goals_r}]}


def high_knees():
    left_up = pose(L_hip_flex=92, L_knee=100, L_ankle=25, R_knee=8, R_ankle=22,
                   L_sh_flex=-35, L_elbow=95, R_sh_flex=55, R_elbow=90, pitch=4, neck_bend=-6)
    return {"id": "high-knees", "view": "side", "seconds": 0.8,
            "anchor": ["pelvis"],
            "keys": [{"t": 0.0, "pose": left_up, "support": ["R_toe", "R_sole"], "lift": 0.03},
                     {"t": 0.5, "pose": mirror(left_up), "support": ["L_toe", "L_sole"], "lift": 0.03}]}


def marching():
    left_up = pose(L_hip_flex=70, L_knee=75, L_ankle=15,
                   L_sh_flex=-25, L_elbow=80, R_sh_flex=40, R_elbow=80, neck_bend=-4)
    return {"id": "marching", "view": "side", "seconds": 1.4,
            "anchor": ["pelvis"], "flat_feet": True, "flat_sides": ("R",),
            "keys": [{"t": 0.0, "pose": left_up, "support": ["R_sole"]},
                     {"t": 0.5, "pose": mirror(left_up), "support": ["L_sole"], "flat_sides": ("L",)}]}


def burpee(step_back=False):
    stand = pose(RELAXED)
    crouch = pose(both(hip_flex=115, knee=125, sh_abd=4, elbow=4, wrist=-60), pitch=48, neck_bend=-30)
    plank = pose(both(elbow=0, wrist=-80, ankle=0), pitch=76, neck_bend=-12)
    jump = pose(both(sh_flex=172, elbow=6, ankle=35, knee=4), neck_bend=-8)
    hands_down = [{"parts": PALMS, "z": 0}, {"parts": SOLES, "z": 0}]
    in_plank = [{"parts": PALMS, "z": 0}, {"parts": TOES, "z": 0}, {"parts": PALMS, "xy": "key:1"}]
    keys = [{"t": 0.0, "pose": stand, "anchor": SOLES, "support": SOLES},
            {"t": 0.16, "pose": crouch, "anchor": SOLES, "support": SOLES + PALMS, "flat_feet": True,
             "free": ["both:sh_flex", "pitch"], "goals": hands_down}]
    if not step_back:
        keys += [{"t": 0.32, "pose": plank, "support": PALMS + TOES, "tie": arms_vertical(),
                  "free": ["pitch", "x", "y"], "goals": in_plank},
                 {"t": 0.46, "pose": plank, "support": PALMS + TOES, "tie": arms_vertical(),
                  "free": ["pitch", "x", "y"], "goals": in_plank},
                 {"t": 0.62, "pose": crouch, "anchor": SOLES, "support": SOLES + PALMS, "flat_feet": True,
                  "free": ["both:sh_flex", "pitch"], "goals": hands_down},
                 {"t": 0.8, "pose": jump, "anchor": SOLES, "support": TOES, "lift": 0.16}]
        seconds = 3.2
    else:
        half = pose(plank, R_hip_flex=110, R_knee=125, R_ankle=-10)
        keys += [{"t": 0.26, "pose": half, "support": PALMS + ["L_toe", "R_sole"], "tie": arms_vertical(),
                  "free": ["pitch", "x", "y"], "goals": [{"parts": PALMS, "xy": "key:1"}, {"parts": ["L_toe"], "z": 0}]},
                 {"t": 0.4, "pose": plank, "support": PALMS + TOES, "tie": arms_vertical(),
                  "free": ["pitch", "x", "y"], "goals": in_plank},
                 {"t": 0.54, "pose": plank, "support": PALMS + TOES, "tie": arms_vertical(),
                  "free": ["pitch", "x", "y"], "goals": in_plank},
                 {"t": 0.68, "pose": half, "support": PALMS + ["L_toe", "R_sole"], "tie": arms_vertical(),
                  "free": ["pitch", "x", "y"], "goals": [{"parts": PALMS, "xy": "key:1"}, {"parts": ["L_toe"], "z": 0}]},
                 {"t": 0.82, "pose": crouch, "anchor": SOLES, "support": SOLES + PALMS, "flat_feet": True,
                  "free": ["both:sh_flex", "pitch"], "goals": hands_down}]
        seconds = 4.6
    return {"id": "step-back-burpee" if step_back else "burpee", "view": "side", "seconds": seconds, "keys": keys}


def mountain_climber(slow=False):
    base = pose(both(elbow=0, wrist=-80), neck_bend=-14)
    # Knee drives toward the chest: thigh tucked under the body, foot hovering.
    l_in = pose(base, L_hip_flex=132, L_knee=138, L_ankle=25, R_ankle=0)
    goals = [{"parts": PALMS, "z": 0}, {"parts": ["R_toe"], "z": 0}, {"parts": PALMS, "xy": "first"}]
    goals_r = [{"parts": PALMS, "z": 0}, {"parts": ["L_toe"], "z": 0}, {"parts": PALMS, "xy": "first"}]
    if slow:
        l_in = pose(l_in, L_hip_flex=124, L_knee=132)
    keys = [{"t": 0.0, "pose": l_in, "pitch": 74, "free": ["pitch", "x", "y"], "goals": goals,
             "support": PALMS + ["R_toe"]},
            {"t": 0.5, "pose": mirror(l_in), "free": ["pitch", "x", "y"], "goals": goals_r,
             "support": PALMS + ["L_toe"]}]
    for k in keys:
        k["pose"] = pose(k["pose"], pitch=74)
    if slow:   # pause with both feet back between knees
        mid = pose(base, both(ankle=0), pitch=76)
        keys = [keys[0],
                {"t": 0.25, "pose": mid, "free": ["pitch", "x", "y"], "support": PALMS + TOES,
                 "goals": [{"parts": PALMS, "z": 0}, {"parts": TOES, "z": 0}, {"parts": PALMS, "xy": "first"}]},
                keys[1],
                {"t": 0.75, "pose": mid, "free": ["pitch", "x", "y"], "support": PALMS + TOES,
                 "goals": [{"parts": PALMS, "z": 0}, {"parts": TOES, "z": 0}, {"parts": PALMS, "xy": "first"}]}]
    return {"id": "slow-climber" if slow else "mountain-climber", "view": "side",
            "seconds": 2.6 if slow else 0.9, "tie": arms_vertical(), "keys": keys}


# ================================================================ lower body
def squat():
    down = pose(both(hip_flex=100, knee=110, sh_flex=118, elbow=5, hip_abd=8), pitch=22, spine_bend=8, neck_bend=-24)
    up = pose(both(sh_flex=8, elbow=10, hip_abd=8))
    return {"id": "squat", "view": "three_quarter", "seconds": 2.4, "flat_feet": True,
            "support": SOLES, "anchor": SOLES,
            "keys": [{"t": 0.0, "pose": up}, {"t": 0.12, "pose": up},
                     {"t": 0.5, "pose": down}, {"t": 0.62, "pose": down}]}


def wall_sit():
    hold = pose(both(hip_flex=90, knee=90, hip_abd=6, sh_flex=90, elbow=4), neck_bend=-4)
    breathe = pose(hold, both(sh_flex=88), spine_bend=-1.5, neck_bend=-6)
    def wall(J, solved):
        import stage as S
        v = S.world_verts([o for o in S.body_meshes() if o.name in ("torso", "pelvis", "hair", "head")])
        back = float(v[:, 1].max())
        M.box("prop_wall", (1.4, 0.12, 2.1), "prop", (0, back + 0.065, 1.05), bevel=0.01)
    return {"id": "wall-sit", "view": "side", "seconds": 2.6, "flat_feet": True,
            "support": SOLES, "anchor": SOLES, "props": [wall], "margin": 1.25,
            "keys": [{"t": 0.0, "pose": hold}, {"t": 0.5, "pose": breathe}]}


def lunge():
    stand = pose(RELAXED)
    left = pose(both(sh_abd=8, elbow=10), L_hip_flex=88, L_knee=92, R_hip_flex=-22, R_knee=105, R_ankle=-5,
                neck_bend=-4)
    return {"id": "lunge", "view": "side", "seconds": 3.4,
            "keys": [{"t": 0.0, "pose": stand, "anchor": SOLES, "support": SOLES},
                     {"t": 0.25, "pose": left, "support": ["L_sole", "R_toe"], "flat_feet": True, "flat_sides": ("L",),
                      "free": ["R_hip_flex", "R_ankle", "x", "y"],
                      "goals": [{"parts": ["R_toe"], "z": 0}, {"parts": ["L_sole"], "z": 0},
                                {"parts": ["R_toe"], "xy": "first"}]},
                     {"t": 0.5, "pose": stand, "anchor": SOLES, "support": SOLES},
                     {"t": 0.75, "pose": mirror(left), "support": ["R_sole", "L_toe"], "flat_feet": True,
                      "flat_sides": ("R",), "free": ["L_hip_flex", "L_ankle", "x", "y"],
                      "goals": [{"parts": ["L_toe"], "z": 0}, {"parts": ["R_sole"], "z": 0},
                                {"parts": ["L_toe"], "xy": "first"}]}]}


def step_up():
    stand = pose(RELAXED)
    on_box_l = pose(both(sh_abd=8, elbow=12), L_hip_flex=72, L_knee=95, R_sh_flex=20, L_sh_flex=-10, pitch=6)
    # Halfway up: weight on the box leg, trailing knee lifted so the foot clears the box edge.
    rising = pose(both(sh_abd=8, elbow=12), L_hip_flex=28, L_knee=30, R_hip_flex=62, R_knee=95, R_ankle=20,
                  L_sh_flex=20, R_sh_flex=-10, pitch=8)
    top = pose(RELAXED, both(knee=3))
    def box(J, solved):
        M.box("prop_step", (0.62, 0.42, STEP_H), "prop", (0, -0.47, STEP_H / 2), bevel=0.02)
    l_goal = [{"parts": ["L_sole"], "z": STEP_H}, {"parts": ["L_sole"], "xy": (0.095, -0.44)}]
    return {"id": "step-up", "view": "side", "seconds": 3.0, "props": [box], "margin": 1.2,
            "keys": [{"t": 0.0, "pose": stand, "anchor": SOLES, "support": SOLES},
                     {"t": 0.2, "pose": on_box_l, "anchor": ["R_sole"], "anchor_xy": (-0.095, 0.0), "support": ["R_sole"],
                      "flat_feet": True, "free": ["L_hip_flex", "L_knee"], "goals": l_goal, "floor": 0.0},
                     {"t": 0.33, "pose": rising, "anchor": ["L_sole"], "anchor_xy": (0.095, -0.44),
                      "support": ["L_sole"], "height": STEP_H, "flat_feet": True, "flat_sides": ("L",)},
                     {"t": 0.45, "pose": top, "anchor": SOLES, "anchor_xy": (0.0, -0.44), "support": SOLES,
                      "height": STEP_H},
                     {"t": 0.55, "pose": top, "anchor": SOLES, "anchor_xy": (0.0, -0.44), "support": SOLES,
                      "height": STEP_H},
                     {"t": 0.67, "pose": rising, "anchor": ["L_sole"], "anchor_xy": (0.095, -0.44),
                      "support": ["L_sole"], "height": STEP_H, "flat_feet": True, "flat_sides": ("L",)},
                     {"t": 0.8, "pose": on_box_l, "anchor": ["R_sole"], "anchor_xy": (-0.095, 0.0), "support": ["R_sole"],
                      "flat_feet": True, "free": ["L_hip_flex", "L_knee"], "goals": l_goal}]}


def calf_raise():
    down = pose(both(sh_abd=10, elbow=10))
    up = pose(down, both(ankle=42), neck_bend=-3)
    return {"id": "calf-raise", "view": "side", "seconds": 1.8,
            "anchor": TOES,
            "keys": [{"t": 0.0, "pose": down, "support": SOLES}, {"t": 0.12, "pose": down, "support": SOLES},
                     {"t": 0.5, "pose": up, "support": TOES}, {"t": 0.62, "pose": up, "support": TOES}]}


def glute_bridge():
    arms = both(sh_flex=-10, elbow=0, sh_abd=12)
    down = pose(arms, both(hip_flex=48, knee=105), pitch=-90, neck_bend=5)
    # Hips lift: the body rotates past lying flat (shoulders stay down, pelvis rises).
    up = pose(both(sh_flex=-30, elbow=0, sh_abd=12, hip_flex=4, knee=114), pitch=-114, neck_bend=32)
    feet = [{"parts": SOLES, "z": 0}, {"parts": SOLES, "xy": "first"}, {"parts": HANDS, "z": 0},
            {"parts": BACK, "z": 0}]
    return {"id": "glute-bridge", "view": "side", "seconds": 2.6, "flat_feet": True,
            "keys": [{"t": 0.0, "pose": down, "free": ["both:hip_flex", "both:sh_flex"], "goals": feet,
                      "support": "all"},
                     {"t": 0.12, "pose": down, "free": ["both:hip_flex", "both:sh_flex"], "goals": feet,
                      "support": "all"},
                     {"t": 0.5, "pose": up, "free": ["both:hip_flex", "both:knee", "both:sh_flex", "x", "y"],
                      "goals": feet, "support": BACK + SOLES},
                     {"t": 0.62, "pose": up, "free": ["both:hip_flex", "both:knee", "both:sh_flex", "x", "y"],
                      "goals": feet, "support": BACK + SOLES}]}


# ================================================================ upper body
def push_up():
    common = pose(both(wrist=-85, ankle=8), neck_bend=-10)
    up = pose(common, both(elbow=0), pitch=78)
    down = pose(common, both(elbow=95, sh_flex=20), pitch=84)
    goals = [{"parts": PALMS, "z": 0}, {"parts": TOES, "z": 0}]
    held = [{"parts": PALMS, "z": 0}, {"parts": PALMS, "xy": "first"}]
    return {"id": "push-up", "view": "side", "seconds": 2.2,
            "support": PALMS + TOES, "anchor": TOES,
            "keys": [{"t": 0.0, "pose": up, "free": ["pitch"], "goals": goals, "tie": forearms_vertical()},
                     {"t": 0.5, "pose": down, "support": TOES, "free": ["both:sh_flex", "both:elbow"], "goals": held}]}


def side_orientation(tilt, side=1):
    """(pitch, roll, yaw) for a side plank on the left (side=1) or right (-1) hand:
    head end raised by `tilt` degrees, supporting side down, chest facing -X."""
    import math
    from mathutils import Matrix
    t = math.radians(tilt)
    x_b = (0.0, -math.sin(t), -math.cos(t))      # body left  -> down
    y_b = (1.0, 0.0, 0.0)                         # body back  -> +X (chest faces -X)
    z_b = (0.0, -math.cos(t), math.sin(t))       # body up    -> toward -Y, raised
    e = Matrix((x_b, y_b, z_b)).transposed().to_euler("XYZ")
    pitch, roll, yaw = math.degrees(e.x), -math.degrees(e.y), math.degrees(e.z)
    return (pitch, roll * side, yaw * side)


def push_up_rotation():
    common = pose(both(wrist=-85, ankle=8), neck_bend=-10)
    up = pose(common, both(elbow=0), pitch=78)
    down = pose(common, both(elbow=95, sh_flex=20), pitch=84)
    side = pose(both(elbow=0, sh_flex=0, wrist=0, ankle=0), L_wrist=-70, tilt=20, R_hip_flex=-4, neck_bend=0)
    up_goals = [{"parts": PALMS, "z": 0}, {"parts": TOES, "z": 0}]
    down_goals = [{"parts": PALMS, "z": 0}, {"parts": PALMS, "xy": "first"}]
    side_goals = [{"parts": ["L_palm"], "z": 0}, {"parts": ["L_palm"], "xy": "first"},
                  {"parts": ["L_foot", "L_sole"], "z": 0}]
    side_goals_r = [{"parts": ["R_palm"], "z": 0}, {"parts": ["R_palm"], "xy": "first"},
                    {"parts": ["R_foot", "R_sole"], "z": 0}]
    tie_l = {"pitch": lambda p: side_orientation(p["tilt"])[0], "roll": lambda p: side_orientation(p["tilt"])[1],
             "yaw": lambda p: side_orientation(p["tilt"])[2],
             "L_sh_abd": lambda p: 90 - p["tilt"], "R_sh_abd": lambda p: 90 + p["tilt"]}
    tie_r = {"pitch": lambda p: side_orientation(p["tilt"], -1)[0], "roll": lambda p: side_orientation(p["tilt"], -1)[1],
             "yaw": lambda p: side_orientation(p["tilt"], -1)[2],
             "R_sh_abd": lambda p: 90 - p["tilt"], "L_sh_abd": lambda p: 90 + p["tilt"]}
    up_key = lambda t: {"t": t, "pose": up, "free": ["pitch"], "goals": up_goals, "tie": forearms_vertical(),
                        "support": PALMS + TOES, "anchor": TOES}
    down_key = lambda t: {"t": t, "pose": down, "support": TOES, "anchor": TOES,
                          "free": ["both:sh_flex", "both:elbow"], "goals": down_goals}
    return {"id": "push-up-rotation", "view": "side_high", "seconds": 5.4,
            "keys": [up_key(0.0), down_key(0.13), up_key(0.26),
                     {"t": 0.4, "pose": side, "tie": tie_l, "support": ["L_palm", "L_foot", "L_sole"],
                      "free": ["tilt", "x", "y"], "goals": side_goals},
                     up_key(0.54), down_key(0.67), up_key(0.8),
                     {"t": 0.92, "pose": mirror(side), "tie": tie_r, "support": ["R_palm", "R_foot", "R_sole"],
                      "free": ["tilt", "x", "y"], "goals": side_goals_r}]}


def triceps_dip():
    # Bench behind the character: front edge at y = 0, seat at SEAT_H. Hands grip the edge,
    # hips stay just in front of it.
    up = pose(both(sh_flex=-20, elbow=0, wrist=-90, hip_flex=88, knee=88), pitch=4, neck_bend=-4)
    down = pose(up, both(sh_flex=-60, elbow=90, hip_flex=78, knee=78), pitch=6)
    def bench(J, solved):
        M.box("prop_bench", (0.7, 0.42, SEAT_H), "prop", (0, 0.21, SEAT_H / 2), bevel=0.02)
    hands = [{"parts": PALMS, "xy": (0.0, 0.05)}, {"parts": SOLES, "z": 0}, {"parts": ["pelvis"], "ymax": -0.02}]
    held = hands + [{"parts": SOLES, "xy": "first"}]
    return {"id": "triceps-dip", "view": "side", "seconds": 2.4, "flat_feet": True, "props": [bench],
            "support": PALMS, "height": SEAT_H, "margin": 1.2,
            "keys": [{"t": 0.0, "pose": up, "free": ["both:sh_flex", "both:hip_flex", "both:knee", "x", "y"],
                      "goals": hands},
                     {"t": 0.5, "pose": down, "free": ["both:sh_flex", "both:hip_flex", "both:knee", "x", "y"],
                      "goals": held}]}


# ====================================================================== core
def crunch():
    # Arms reach toward the knees and travel with the chest as it curls up.
    arms = both(sh_flex=22, sh_abd=8, elbow=0, wrist=0)
    down = pose(arms, both(hip_flex=50, knee=105, hip_abd=6), pitch=-90, neck_bend=4)
    up = pose(down, both(sh_flex=30), spine_bend=34, neck_bend=16)
    feet = [{"parts": SOLES, "z": 0}, {"parts": SOLES, "xy": "first"}]
    return {"id": "crunch", "view": "side", "seconds": 2.0, "flat_feet": True,
            "keys": [{"t": 0.0, "pose": down, "free": ["both:hip_flex"], "goals": feet},
                     {"t": 0.12, "pose": down, "free": ["both:hip_flex"], "goals": feet},
                     {"t": 0.5, "pose": up, "free": ["both:hip_flex", "x", "y"], "goals": feet},
                     {"t": 0.62, "pose": up, "free": ["both:hip_flex", "x", "y"], "goals": feet}]}


def plank():
    hold = pose(both(elbow=90, wrist=0, ankle=6), pitch=77, neck_bend=-12)
    breathe = pose(hold, spine_bend=1.5)
    goals = [{"parts": FOREARMS, "z": 0}, {"parts": TOES, "z": 0}]
    return {"id": "plank", "view": "side", "seconds": 3.0,
            "support": FOREARMS + TOES, "anchor": TOES,
            "tie": {"L_sh_flex": lambda p: p["pitch"], "R_sh_flex": lambda p: p["pitch"]},
            "keys": [{"t": 0.0, "pose": hold, "free": ["pitch"], "goals": goals},
                     {"t": 0.5, "pose": breathe, "free": ["pitch"], "goals": goals}]}


def side_plank():
    hold = pose(L_elbow=90, L_wrist=0, R_elbow=0, roll=-74, neck_bend=0)
    breathe = pose(hold, roll=-73)
    goals = [{"parts": ["L_forearm"], "z": 0}, {"parts": ["L_foot", "L_sole"], "z": 0}]
    return {"id": "side-plank", "view": "front", "seconds": 3.0,
            "support": ["L_forearm", "L_foot", "L_sole", "R_foot"], "anchor": ["L_forearm", "L_sole"],
            "tie": {"L_sh_abd": lambda p: -p["roll"], "R_sh_abd": lambda p: 180 + p["roll"]},
            "keys": [{"t": 0.0, "pose": hold, "free": ["roll"], "goals": goals},
                     {"t": 0.5, "pose": breathe, "free": ["roll"], "goals": goals}]}


def bird_dog():
    base = pose(both(knee=90, wrist=-85, ankle=85), neck_bend=-6)
    neutral_tie = {"L_sh_flex": lambda p: p["pitch"], "R_sh_flex": lambda p: p["pitch"],
                   "L_hip_flex": lambda p: p["pitch"], "R_hip_flex": lambda p: p["pitch"]}
    reach = {"L_sh_flex": lambda p: p["pitch"], "R_sh_flex": lambda p: p["pitch"] + 88,
             "R_hip_flex": lambda p: p["pitch"], "L_hip_flex": lambda p: p["pitch"] - 92}
    reach_pose = pose(base, L_knee=0, L_ankle=30, R_elbow=0, R_wrist=0)
    goals = [{"parts": PALMS, "z": 0}, {"parts": SHINS, "z": 0}]
    reach_goals = [{"parts": ["L_palm"], "z": 0}, {"parts": ["R_shin"], "z": 0}, {"parts": ["L_palm"], "xy": "first"}]
    reach_goals_r = [{"parts": ["R_palm"], "z": 0}, {"parts": ["L_shin"], "z": 0}, {"parts": ["R_palm"], "xy": "first"}]
    reach_r = {"R_sh_flex": lambda p: p["pitch"], "L_sh_flex": lambda p: p["pitch"] + 88,
               "L_hip_flex": lambda p: p["pitch"], "R_hip_flex": lambda p: p["pitch"] - 92}
    return {"id": "bird-dog", "view": "side", "seconds": 3.6,
            "keys": [{"t": 0.0, "pose": pose(base, pitch=88), "tie": neutral_tie, "free": ["pitch"], "goals": goals,
                      "support": PALMS + SHINS, "anchor": PALMS + SHINS},
                     {"t": 0.25, "pose": pose(reach_pose, pitch=88), "tie": reach, "free": ["pitch", "x", "y"],
                      "goals": reach_goals, "support": ["L_palm", "R_shin"]},
                     {"t": 0.5, "pose": pose(base, pitch=88), "tie": neutral_tie, "free": ["pitch"], "goals": goals,
                      "support": PALMS + SHINS, "anchor": PALMS + SHINS},
                     {"t": 0.75, "pose": pose(mirror(reach_pose), pitch=88), "tie": reach_r, "free": ["pitch", "x", "y"],
                      "goals": reach_goals_r, "support": ["R_palm", "L_shin"]}]}


def superman():
    tie = {"L_sh_flex": lambda p: p.get("arm_lift", 0) + 180, "R_sh_flex": lambda p: p.get("arm_lift", 0) + 180}
    down = pose(both(elbow=4, ankle=60), pitch=90, neck_bend=-4)
    up = pose(down, both(hip_flex=-14), spine_bend=-18, neck_bend=-12, arm_lift=14)
    return {"id": "superman", "view": "side", "seconds": 2.4, "anchor": ["pelvis"], "tie": tie,
            "keys": [{"t": 0.0, "pose": down}, {"t": 0.12, "pose": down},
                     {"t": 0.5, "pose": up}, {"t": 0.65, "pose": up}]}


ALL = [jumping_jacks, step_jacks, high_knees, marching, lambda: burpee(False), lambda: burpee(True),
       lambda: mountain_climber(False), lambda: mountain_climber(True), squat, wall_sit, lunge, step_up,
       calf_raise, glute_bridge, push_up, push_up_rotation, triceps_dip, crunch, plank, side_plank,
       bird_dog, superman]
