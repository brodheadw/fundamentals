#!/usr/bin/env python3
import zipfile
from pathlib import Path

from PIL import Image

from paint_separation import PLASTIC, TEXTURES, heap, moulded, steel

TFMG_JAR = next(Path.home().glob(".gradle/caches/modules-2/files-2.1/maven.modrinth/create-tfmg/*/*/create-tfmg-*.jar"))
TFMG_TEXTURES = TEXTURES.parent.parent / "tfmg/textures"

TFMG_PLASTIC = set(PLASTIC) | {(137, 143, 156), (206, 211, 216), (122, 128, 141)}
TRANSLUCENT = ("plastic_pipes", "plastic_pipes_connected", "plastic_glass_fluid_pipe", "plastic_fluid_valve", "plastic_smart_pipe_1",
               "plastic_smart_pipe_2")
PUMP_PARTS = ((0, 0, 12, 12), (12, 0, 16, 12), (0, 12, 18, 17), (18, 12, 32, 17), (0, 17, 18, 32), (18, 17, 32, 32))
DYES = {
    "white": (207, 213, 214), "orange": (224, 97, 1), "magenta": (169, 48, 159), "light_blue": (36, 137, 199),
    "yellow": (241, 175, 21), "lime": (94, 169, 24), "pink": (214, 101, 143), "gray": (55, 58, 62),
    "light_gray": (125, 125, 115), "cyan": (21, 119, 136), "purple": (100, 32, 156), "blue": (45, 47, 143),
    "brown": (96, 60, 32), "green": (73, 91, 36), "red": (142, 33, 33), "black": (8, 10, 15),
}
PVC = [(58, 62, 68), (80, 85, 92), (100, 106, 113), (118, 124, 131), (140, 146, 152), (168, 173, 178)]


def tfmg_texture(path):
    with zipfile.ZipFile(TFMG_JAR) as jar:
        with jar.open(f"assets/tfmg/textures/{path}.png") as f:
            return Image.open(f).convert("RGBA").copy()


def opaque_plastic(img):
    out = moulded(img, TFMG_PLASTIC)
    out.putdata([(r, g, b, 255) for r, g, b, _ in out.getdata()])
    return out


def dye_ramp(colour):
    shade = [tuple(round(c * k) for c in colour) for k in (0.55, 0.7, 0.85, 1.0)]
    return shade + [tuple(round(c + (255 - c) * k) for c in colour) for k in (0.12, 0.28)]


def main():
    (TFMG_TEXTURES / "block").mkdir(parents=True, exist_ok=True)
    for name in TRANSLUCENT:
        moulded(tfmg_texture(f"block/{name}"), TFMG_PLASTIC).save(TFMG_TEXTURES / f"block/{name}.png")
    moulded(tfmg_texture("block/plastic_pump"), boxes=PUMP_PARTS).save(TFMG_TEXTURES / "block/plastic_pump.png")
    block = tfmg_texture("block/plastic_block")
    block.putdata([(r, g, b, 255) for r, g, b, _ in block.getdata()])
    opaque_plastic(block).save(TFMG_TEXTURES / "block/plastic_block.png")
    for dye, colour in DYES.items():
        steel(block, dye_ramp(colour), 0.4, 0.55).save(TEXTURES / f"block/{dye}_plastic_block.png")
    heap("ziegler_natta_catalyst", (190, 132, 206), (136, 80, 158), (80, 42, 96)).save(TEXTURES / "item/ziegler_natta_catalyst.png")
    heap("pvc_resin", (255, 255, 255), (238, 238, 234), (186, 186, 180)).save(TEXTURES / "item/pvc_resin.png")
    steel(tfmg_texture("item/plastic_sheet"), PVC, 0.3, 0.6).save(TEXTURES / "item/pvc_sheet.png")
    print("plastic textures written")


if __name__ == "__main__":
    main()
