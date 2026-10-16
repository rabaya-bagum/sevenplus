"""SevenPlus exercise mannequin: a jointed 3D character built from code in Blender.

Coordinates: Blender units are metres, Z is up. In its rest pose the character
stands at the origin facing -Y (so its left side is +X), arms hanging down.

Every joint is an Empty; body parts are meshes parented to joints, so posing is
plain forward kinematics on the Empties' Euler rotations (see POSE KEYS below).
"""
import math

import bmesh
import bpy
from mathutils import Vector

# ---------------------------------------------------------------- dimensions
HIP_HEIGHT = 0.95
SPINE_UP = 0.10            # root (pelvis) -> spine joint
CHEST = 0.40               # spine joint -> neck base
SHOULDER = (0.185, 0.0, 0.34)
HIP = (0.095, 0.0, -0.035)
UPPER_ARM, FOREARM, HAND = 0.29, 0.26, 0.085
THIGH, SHIN = 0.43, 0.42
HEAD_R = 0.105

COLORS = {
    # Albedos are toned down because the render brightens exposure for a light backdrop.
    "skin": (0.66, 0.56, 0.49),
    "shirt": (0.78, 0.27, 0.05),      # SevenPlus orange
    "shorts": (0.10, 0.11, 0.14),
    "shoe": (0.70, 0.71, 0.73),
    "sole": (0.12, 0.12, 0.13),
    "hair": (0.08, 0.06, 0.05),
    "prop": (0.40, 0.43, 0.48),
    "floor": (0.62, 0.63, 0.65),
}


def material(name):
    if name in bpy.data.materials:
        return bpy.data.materials[name]
    m = bpy.data.materials.new(name)
    c = (*COLORS[name], 1.0)
    m.diffuse_color = c                       # Workbench uses this
    m.use_nodes = True                        # Cycles / Eevee use the BSDF
    bsdf = m.node_tree.nodes.get("Principled BSDF")
    if bsdf:
        bsdf.inputs["Base Color"].default_value = c
        bsdf.inputs["Roughness"].default_value = 0.55
    return m


def _mesh_object(name, bm, mat, parent):
    me = bpy.data.meshes.new(name)
    bm.to_mesh(me)
    bm.free()
    for p in me.polygons:
        p.use_smooth = True
    ob = bpy.data.objects.new(name, me)
    ob.data.materials.append(material(mat))
    bpy.context.scene.collection.objects.link(ob)
    ob.parent = parent
    return ob


def capsule(name, r1, r2, length, mat, parent, offset=(0, 0, 0), axis="-Z", squash=(1, 1)):
    """Tapered capsule from `offset` along `axis` for `length` (radius r1 -> r2)."""
    bm = bmesh.new()
    bmesh.ops.create_uvsphere(bm, u_segments=28, v_segments=14, radius=1.0)
    for v in bm.verts:
        z = v.co.z
        if z < -1e-6:                         # lower hemisphere: end cap
            v.co.x *= r2 * squash[0]
            v.co.y *= r2 * squash[1]
            v.co.z = z * r2 - length
        else:                                 # upper hemisphere: start cap
            v.co.x *= r1 * squash[0]
            v.co.y *= r1 * squash[1]
            v.co.z = z * r1
    rot = {"-Z": None, "-Y": ("X", -90), "+Z": ("X", 180), "+X": ("Y", -90), "-X": ("Y", 90)}[axis]
    if rot:
        from mathutils import Matrix
        bmesh.ops.rotate(bm, verts=bm.verts, cent=(0, 0, 0), matrix=Matrix.Rotation(math.radians(rot[1]), 3, rot[0]))
    bmesh.ops.translate(bm, verts=bm.verts, vec=Vector(offset))
    return _mesh_object(name, bm, mat, parent)


def ellipsoid(name, size, mat, parent, offset=(0, 0, 0), cut_below=None):
    bm = bmesh.new()
    bmesh.ops.create_uvsphere(bm, u_segments=32, v_segments=16, radius=1.0)
    for v in bm.verts:
        v.co = Vector((v.co.x * size[0], v.co.y * size[1], v.co.z * size[2]))
    if cut_below is not None:                 # flat bottom (soles)
        for v in bm.verts:
            v.co.z = max(v.co.z, cut_below)
    bmesh.ops.translate(bm, verts=bm.verts, vec=Vector(offset))
    return _mesh_object(name, bm, mat, parent)


def box(name, size, mat, location, bevel=0.02):
    bm = bmesh.new()
    bmesh.ops.create_cube(bm, size=1.0)
    for v in bm.verts:
        v.co = Vector((v.co.x * size[0], v.co.y * size[1], v.co.z * size[2]))
    ob = _mesh_object(name, bm, mat, None)
    ob.location = location
    for p in ob.data.polygons:
        p.use_smooth = False
    mod = ob.modifiers.new("bevel", "BEVEL")
    mod.width = bevel
    mod.segments = 3
    return ob


def marker(name, parent, location):
    """Invisible 1 cm point used by the pose solver (names end in `_mark`)."""
    bm = bmesh.new()
    bmesh.ops.create_cube(bm, size=0.01)
    ob = _mesh_object(name + "_mark", bm, "skin", parent)
    ob.location = location
    ob.hide_render = True
    return ob


def joint(name, parent, location):
    e = bpy.data.objects.new(name, None)
    e.empty_display_size = 0.05
    bpy.context.scene.collection.objects.link(e)
    e.parent = parent
    e.location = location
    e.rotation_mode = "XYZ"
    return e


def build():
    """Creates the character. Returns {joint name: Empty}."""
    J = {}
    root = J["root"] = joint("root", None, (0, 0, HIP_HEIGHT))

    # Pelvis and torso
    ellipsoid("pelvis", (0.165, 0.11, 0.12), "shorts", root, (0, 0, -0.01))
    spine = J["spine"] = joint("spine", root, (0, 0, SPINE_UP))
    # One tapered capsule from chest down to waist, flattened front-to-back.
    capsule("torso", 0.175, 0.145, 0.27, "shirt", spine, (0, 0, 0.31), squash=(1, 0.66))
    neck = J["neck"] = joint("neck", spine, (0, 0, CHEST))
    capsule("neckpart", 0.045, 0.05, 0.06, "skin", neck, (0, 0, 0.07))
    ellipsoid("head", (HEAD_R * 0.92, HEAD_R, HEAD_R * 1.12), "skin", neck, (0, -0.01, 0.16))
    # Hair: a cap shifted back and up shows which way the head faces.
    ellipsoid("hair", (HEAD_R * 0.97, HEAD_R * 1.0, HEAD_R * 0.95), "hair", neck, (0, 0.02, 0.19), cut_below=-0.02)
    ellipsoid("nose", (0.018, 0.02, 0.025), "skin", neck, (0, -0.112, 0.15))

    for side, sx in (("L", 1), ("R", -1)):
        sh = J[f"{side}_shoulder"] = joint(f"{side}_shoulder", spine, (sx * SHOULDER[0], SHOULDER[1], SHOULDER[2]))
        ellipsoid(f"{side}_deltoid", (0.065, 0.065, 0.065), "shirt", sh, (0, 0, -0.01))
        capsule(f"{side}_upperarm", 0.05, 0.043, UPPER_ARM - 0.02, "skin", sh, (0, 0, -0.02))
        el = J[f"{side}_elbow"] = joint(f"{side}_elbow", sh, (0, 0, -UPPER_ARM))
        capsule(f"{side}_forearm", 0.042, 0.033, FOREARM, "skin", el)
        wr = J[f"{side}_wrist"] = joint(f"{side}_wrist", el, (0, 0, -FOREARM))
        ellipsoid(f"{side}_hand", (0.028, 0.045, 0.06), "skin", wr, (0, 0, -0.05))
        marker(f"{side}_palm", wr, (0, 0, -0.06))

        hp = J[f"{side}_hip"] = joint(f"{side}_hip", root, (sx * HIP[0], HIP[1], HIP[2]))
        capsule(f"{side}_thigh", 0.088, 0.076, THIGH * 0.46, "shorts", hp)          # shorts leg
        capsule(f"{side}_thighskin", 0.07, 0.053, THIGH - 0.12, "skin", hp, (0, 0, -0.12))
        kn = J[f"{side}_knee"] = joint(f"{side}_knee", hp, (0, 0, -THIGH))
        capsule(f"{side}_shin", 0.055, 0.04, SHIN - 0.03, "skin", kn)
        an = J[f"{side}_ankle"] = joint(f"{side}_ankle", kn, (0, 0, -SHIN))
        ellipsoid(f"{side}_foot", (0.048, 0.13, 0.05), "shoe", an, (0, -0.06, -0.035), cut_below=-0.6)
        ellipsoid(f"{side}_sole", (0.05, 0.135, 0.02), "sole", an, (0, -0.06, -0.068))
        marker(f"{side}_toe", an, (0, -0.185, -0.085))
        marker(f"{side}_heel", an, (0, 0.065, -0.085))
        marker(f"{side}_kneecap", kn, (0, -0.055, 0))
    return J


# ------------------------------------------------------------- POSE KEYS
# All angles in degrees, all default 0 (standing straight, arms down).
#   root: x, y, z (location; z is solved from contacts), pitch (+ tips forward,
#         90 = face down), roll (+ tips to the character's right), yaw (+ turns left)
#   spine_bend (+ forward), spine_side (+ to the right), spine_twist (+ turns left)
#   neck_bend (+ chin down)
#   {L,R}_sh_flex (+ arm forward/up, 180 = overhead), {L,R}_sh_abd (+ out to the side,
#   90 = T, 180 = overhead), {L,R}_sh_rot, {L,R}_elbow (+ bend), {L,R}_wrist (+ palm up)
#   {L,R}_hip_flex (+ leg forward/up), {L,R}_hip_abd (+ out), {L,R}_knee (+ bend),
#   {L,R}_ankle (+ toes down)

def apply_pose(J, pose):
    r = math.radians
    g = lambda k: pose.get(k, 0.0)
    root = J["root"]
    root.location = (g("x"), g("y"), pose.get("z", HIP_HEIGHT))
    root.rotation_euler = (r(g("pitch")), r(-g("roll")), r(g("yaw")))
    J["spine"].rotation_euler = (r(g("spine_bend")), r(-g("spine_side")), r(g("spine_twist")))
    J["neck"].rotation_euler = (r(g("neck_bend")), 0, 0)
    for side, sx in (("L", 1), ("R", -1)):
        J[f"{side}_shoulder"].rotation_euler = (r(-g(f"{side}_sh_flex")), r(-sx * g(f"{side}_sh_abd")), r(sx * g(f"{side}_sh_rot")))
        J[f"{side}_elbow"].rotation_euler = (r(-g(f"{side}_elbow")), 0, 0)
        J[f"{side}_wrist"].rotation_euler = (r(g(f"{side}_wrist")), 0, 0)
        J[f"{side}_hip"].rotation_euler = (r(-g(f"{side}_hip_flex")), r(-sx * g(f"{side}_hip_abd")), 0)
        J[f"{side}_knee"].rotation_euler = (r(g(f"{side}_knee")), 0, 0)
        J[f"{side}_ankle"].rotation_euler = (r(g(f"{side}_ankle")), 0, 0)
