"""Renders the exercise clips the app plays.

    python render_all.py OUTDIR [exercise-id ...]     (no ids = all 22)

Writes OUTDIR/<id>.mp4 (light background) and OUTDIR/<id>-dark.mp4.
Needs Blender as a Python module (`pip install bpy`), plus numpy, scipy, pillow and ffmpeg.
"""
import os
import sys
import tempfile
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import clips
import exercises as E


def main():
    outdir = os.path.abspath(sys.argv[1])
    wanted = set(sys.argv[2:])
    os.makedirs(outdir, exist_ok=True)
    for make in E.ALL:
        ex = make()
        if wanted and ex["id"] not in wanted:
            continue
        t = time.time()
        # Frames go to a temp folder outside the app so nothing stray is ever bundled.
        files = clips.render_clip(ex, outdir, size=720, themes=("light", "dark"),
                                  workdir=tempfile.mkdtemp(prefix=f"sevenplus-{ex['id']}-"))
        sizes = ", ".join(f"{os.path.basename(f)} {os.path.getsize(f) // 1024} KB" for f in files)
        print(f"{ex['id']}: {time.time() - t:.0f} s  ({sizes})", flush=True)


if __name__ == "__main__":
    main()
