"""Makes list thumbnails from rendered clips.

    python thumbnails.py CLIPDIR

For every <id>.mp4 / <id>-dark.mp4 in CLIPDIR, grabs the exercise's most
recognisable moment, crops to the character and writes <id>-thumb.jpg /
<id>-dark-thumb.jpg (160 x 160).
"""
import glob
import os
import subprocess
import sys
import tempfile

import numpy as np
from PIL import Image

# Point in the loop (0..1) that shows each exercise best; default 0.5.
MOMENT = {
    "high-knees": 0.0, "marching": 0.0, "mountain-climber": 0.0, "slow-climber": 0.0,
    "step-jacks": 0.25, "lunge": 0.25, "bird-dog": 0.25, "step-up": 0.2,
    "push-up-rotation": 0.4, "burpee": 0.32, "step-back-burpee": 0.4,
    "plank": 0.0, "side-plank": 0.0, "wall-sit": 0.0, "push-up": 0.0,
}
SIZE = 160


def grab(mp4, fraction):
    out = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", mp4],
                         capture_output=True, text=True, check=True).stdout
    t = float(out) * fraction
    with tempfile.TemporaryDirectory() as d:
        png = os.path.join(d, "f.png")
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-ss", f"{t:.3f}", "-i", mp4, "-frames:v", "1", png],
                       check=True)
        return Image.open(png).convert("RGB")


def crop_to_character(img, pad=0.14):
    a = np.asarray(img, dtype=np.int16)
    # Background is a smooth vertical gradient: compare each pixel with its row's edge colour.
    edge = (a[:, :8].mean(axis=1, keepdims=True) + a[:, -8:].mean(axis=1, keepdims=True)) / 2
    mask = (np.abs(a - edge).max(axis=2) > 28)
    ys, xs = np.nonzero(mask)
    if len(xs) == 0:
        return img
    x0, x1, y0, y1 = xs.min(), xs.max(), ys.min(), ys.max()
    side = max(x1 - x0, y1 - y0) * (1 + 2 * pad)
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    w, h = img.size
    side = min(side, w, h)
    left = int(np.clip(cx - side / 2, 0, w - side))
    top = int(np.clip(cy - side / 2, 0, h - side))
    return img.crop((left, top, left + int(side), top + int(side)))


def main():
    clipdir = sys.argv[1]
    for mp4 in sorted(glob.glob(os.path.join(clipdir, "*.mp4"))):
        name = os.path.basename(mp4)[:-4]
        eid = name[:-5] if name.endswith("-dark") else name
        thumb = crop_to_character(grab(mp4, MOMENT.get(eid, 0.5))).resize((SIZE, SIZE), Image.LANCZOS)
        out = os.path.join(clipdir, f"{name}-thumb.jpg")
        thumb.save(out, quality=82, optimize=True)
        print(os.path.basename(out), os.path.getsize(out) // 1024, "KB")


if __name__ == "__main__":
    main()
