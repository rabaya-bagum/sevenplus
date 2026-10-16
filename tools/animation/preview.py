"""Renders a contact sheet of a few frames per exercise for checking poses.

    python preview.py OUTDIR squat push-up      (blank = all)
"""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))

import numpy as np
from PIL import Image, ImageDraw

import clips
import exercises as E


def main():
    outdir = sys.argv[1]
    wanted = sys.argv[2:]
    os.makedirs(outdir, exist_ok=True)
    for make in E.ALL:
        ex = make()
        if wanted and ex["id"] not in wanted:
            continue
        n = int(round(ex["seconds"] * clips.FPS))
        frames = [int(n * f) for f in (0, 0.17, 0.33, 0.5, 0.67, 0.83)]
        files = clips.render_clip(ex, outdir, size=300, themes=("light",),
                                  workdir=os.path.join(outdir, "_w_" + ex["id"]), preview_frames=frames)
        ims = [Image.open(f) for f in files]
        sheet = Image.new("RGB", (300 * 3, 300 * 2))
        for i, im in enumerate(ims):
            sheet.paste(im, ((i % 3) * 300, (i // 3) * 300))
            ImageDraw.Draw(sheet).text(((i % 3) * 300 + 6, (i // 3) * 300 + 4), f"{ex['id']} f{frames[i]}", fill=(90, 90, 90))
        sheet.save(os.path.join(outdir, f"sheet_{ex['id']}.png"))
        print("sheet", ex["id"])


if __name__ == "__main__":
    main()
