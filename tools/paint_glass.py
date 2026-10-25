#!/usr/bin/env python3
"""Safety glass, polycarbonate and the blast door: glass that stays see-through, its panes' edges, and the door's 3x3 face."""
import random

from PIL import Image

from paint_separation import STEEL, TEXTURES, heap

CLEAR = (0, 0, 0, 0)


def frame(img, edge, light, dark, width=1):
    """Vanilla glass's rim: lit along the top and left, shaded along the bottom and right."""
    n = img.width
    for i in range(n):
        for w in range(width):
            img.putpixel((i, w), light)
            img.putpixel((w, i), light)
            img.putpixel((i, n - 1 - w), dark)
            img.putpixel((n - 1 - w, i), dark)
    img.putpixel((n - 1, 0), edge)
    img.putpixel((0, n - 1), edge)


def streaks(img, colour, marks=((4, 2, 3), (2, 4, 1), (13, 12, 2))):
    """Short diagonal glints, as on vanilla glass."""
    for x, y, length in marks:
        for k in range(length):
            img.putpixel((x - k, y + k), colour)


def borosilicate():
    # Pyrex-type glass: water-clear, with the faint green-yellow of its edge where it is thick
    img = Image.new("RGBA", (16, 16), (232, 240, 228, 18))
    frame(img, (186, 204, 184, 255), (226, 236, 222, 255), (168, 186, 166, 255))
    streaks(img, (246, 250, 244, 200))
    return img


def tempered():
    # Toughened glass shows the quench pattern of its air jets, a grid of faint spots, under polarised light and a low sun
    img = Image.new("RGBA", (16, 16), (226, 232, 236, 14))
    for y in range(1, 15):
        for x in range(1, 15):
            dx, dy = (x - 1) % 5 - 2, (y - 1) % 5 - 2
            if dx * dx + dy * dy <= 1:
                img.putpixel((x, y), (150, 172, 200, 96))
    frame(img, (150, 162, 172, 255), (214, 222, 228, 255), (136, 148, 160, 255))
    streaks(img, (244, 248, 252, 210))
    return img


def laminated():
    # A sandwich of tempered glass and polycarbonate an inch and a half thick: green-blue through it, darker at the double rim
    img = Image.new("RGBA", (16, 16), (150, 196, 186, 70))
    frame(img, (64, 98, 92, 255), (150, 192, 182, 255), (70, 110, 102, 255), width=2)
    for i in range(2, 14):
        img.putpixel((i, 2), (112, 158, 148, 150))
        img.putpixel((2, i), (112, 158, 148, 150))
    streaks(img, (226, 244, 238, 190), ((6, 3, 3), (4, 5, 1), (12, 10, 2)))
    return img


def sapphire_window():
    # A sapphire disc set in a steel bezel with eight bolts: the viewport of a pressure hull or a furnace
    img = Image.new("RGBA", (16, 16), CLEAR)
    steel = [STEEL[i] + (255,) for i in range(len(STEEL))]
    for y in range(16):
        for x in range(16):
            r2 = (x - 7.5) ** 2 + (y - 7.5) ** 2
            if r2 <= 4.6 ** 2:
                glint = x + y in (8, 9) and r2 < 3.8 ** 2
                img.putpixel((x, y), (236, 244, 252, 190) if glint else (176, 200, 232, 64))
            elif r2 <= 5.6 ** 2:
                img.putpixel((x, y), steel[3] if x + y < 15 else steel[1])
            else:
                lit = x == 0 or y == 0
                img.putpixel((x, y), steel[6] if lit else steel[0] if x == 15 or y == 15 else steel[4] if (x + y) % 7 else steel[5])
    for x, y in ((2, 2), (7, 1), (13, 2), (14, 7), (13, 13), (8, 14), (2, 13), (1, 8)):
        img.putpixel((x, y), steel[7])
        img.putpixel((min(15, x + 1), min(15, y + 1)), steel[0])
    return img


def pane_edge(colour):
    # what a pane shows on its thin edges: the glass seen end on is its own colour, dense
    img = Image.new("RGBA", (16, 16), CLEAR)
    for y in range(16):
        for x in range(7, 9):
            img.putpixel((x, y), colour)
    return img


def hot(img, fill=True):
    """Glass at 620 °C, glowing a dull orange."""
    out = Image.new("RGBA", img.size)
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            lum = (r + g + b) / 765
            out.putpixel((x, y), (min(255, int(200 + 55 * lum)), int(70 + 110 * lum), int(20 + 40 * lum), a) if a else (236, 120, 40, 80) if fill else (0, 0, 0, 0))
    return out


def sheet(fill, rim):
    # a clear sheet seen in perspective: a parallelogram, lit edge on top
    img = Image.new("RGBA", (16, 16), CLEAR)
    for y in range(4, 13):
        for x in range(2 + (12 - y) // 3, 13 + (12 - y) // 3):
            edge = y in (4, 12) or x in (2 + (12 - y) // 3, 12 + (12 - y) // 3)
            img.putpixel((x, y), rim if edge else fill)
    for x, y in ((7, 6), (8, 6), (6, 7)):
        img.putpixel((x, y), (250, 252, 255, 230))
    return img


def riot_shield():
    """32x32 for the shield model: front 12x22 at (0,0), back at (12,0), rim strips at (24,0), the grip at (26,0)."""
    img = Image.new("RGBA", (32, 32), CLEAR)
    clear, rim, glint, band = (200, 214, 226, 70), (34, 36, 40, 255), (246, 250, 255, 170), (226, 228, 230, 200)
    for ox in (0, 12):
        for y in range(22):
            for x in range(12):
                edge = x in (0, 11) or y in (0, 21)
                img.putpixel((ox + x, y), rim if edge else band if y in (15, 16) else clear)
        for k in range(5):
            img.putpixel((ox + 3 + k, 3 + k), glint)
            img.putpixel((ox + 2 + k, 4 + k), glint)
    for y in range(22):
        img.putpixel((24, y), rim)
        img.putpixel((25, y), (58, 60, 66, 255))
    for y in range(8):
        for x in range(26, 32):
            img.putpixel((x, y), (48, 50, 56, 255) if (x + y) % 3 else (70, 72, 80, 255))
    return img


def hazard(x, y):
    return (226, 178, 26, 255) if (x + y) // 3 % 2 == 0 else (24, 24, 26, 255)


def door_face():
    """The whole closed 3x3 door, 48 px square: two plate leaves meeting in the middle, hazard-striped along the meeting edge and the foot, a laminated viewport in each."""
    rng = random.Random("blast door")
    n = 48
    img = Image.new("RGBA", (n, n))
    for y in range(n):
        for x in range(n):
            tone = 3 + rng.choice((0, 0, 0, 1, -1))
            if y % 16 == 0 or x % 24 == 0:
                tone = 1
            elif y % 16 == 1 or x % 24 == 1:
                tone = 5
            img.putpixel((x, y), STEEL[tone] + (255,))
    # the meeting edge and the foot
    for y in range(n):
        for x in list(range(20, 23)) + list(range(25, 28)):
            img.putpixel((x, y), hazard(x, y))
        for x in (23, 24):
            img.putpixel((x, y), STEEL[0] + (255,))
    for y in range(41, 46):
        for x in range(n):
            if x not in (23, 24):
                img.putpixel((x, y), hazard(x, y))
    for x in range(n):
        img.putpixel((x, 0), STEEL[6] + (255,))
        img.putpixel((x, n - 1), STEEL[0] + (255,))
    # rivets along each leaf's border
    for leaf in (0, 24):
        for y in range(4, 40, 6):
            for x in (leaf + 3, leaf + 20):
                if 20 <= x <= 27:
                    continue
                img.putpixel((x, y), STEEL[7] + (255,))
                img.putpixel((x + 1, y + 1), STEEL[0] + (255,))
    # one viewport per leaf, at eye height: laminated glass, a steel bezel round it
    for x0 in (8, 32):
        for y in range(9, 21):
            for x in range(x0, x0 + 8):
                if x in (x0, x0 + 7) or y in (9, 20):
                    img.putpixel((x, y), STEEL[1] + (255,))
                elif x in (x0 + 1, x0 + 6) or y in (10, 19):
                    img.putpixel((x, y), STEEL[6] + (255,))
                else:
                    img.putpixel((x, y), (236, 248, 244, 190) if x - x0 + y in (14, 15) else (130, 184, 172, 96))
    return img


def door_icon():
    """The item: the closed door in small, plate, stripes at the meeting edge and foot, two viewports."""
    img = Image.new("RGBA", (16, 16), CLEAR)
    for y in range(1, 16):
        for x in range(1, 15):
            img.putpixel((x, y), (STEEL[6] if y == 1 else STEEL[1] if x in (1, 14) or y == 15 else STEEL[4]) + (255,))
    for y in range(2, 15):
        for x in (7, 8):
            img.putpixel((x, y), hazard(x * 3, y * 3 + x))
    for x in range(2, 14):
        if x not in (7, 8):
            img.putpixel((x, 13), hazard(x * 3, 39))
    for x0 in (3, 10):
        for y in range(4, 8):
            for x in (x0, x0 + 1):
                img.putpixel((x, y), (190, 232, 222, 255) if y == 4 else (110, 168, 156, 255))
    return img


def door_edge():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), STEEL[2 if x in (0, 15) else 4 if x % 5 else 3] + (255,))
    return img


def main():
    glasses = {"borosilicate_glass": (borosilicate(), (196, 214, 194, 230)), "tempered_glass": (tempered(), (170, 186, 198, 230)),
               "laminated_glass": (laminated(), (88, 132, 122, 255))}
    for name, (img, edge) in glasses.items():
        img.save(TEXTURES / f"block/{name}.png")
        pane_edge(edge).save(TEXTURES / f"block/{name}_pane_top.png")
    sapphire_window().save(TEXTURES / "block/sapphire_window.png")
    door_face().save(TEXTURES / "block/blast_door.png")
    door_edge().save(TEXTURES / "block/blast_door_edge.png")
    door_icon().save(TEXTURES / "item/blast_door.png")
    riot_shield().save(TEXTURES / "item/riot_shield.png")
    vanilla_like = Image.new("RGBA", (16, 16), CLEAR)
    frame(vanilla_like, (168, 168, 168, 255), (208, 208, 208, 255), (139, 139, 139, 255))
    streaks(vanilla_like, (208, 208, 208, 255))
    hot(vanilla_like).save(TEXTURES / "item/hot_glass.png")
    # a pane cut to size, seen edge-on in the furnace's glow
    pane = Image.new("RGBA", (16, 16), CLEAR)
    for y in range(1, 15):
        for x in range(6, 10):
            pane.putpixel((x, y), (208, 208, 208, 255) if x in (6, 9) or y in (1, 14) else (190, 190, 190, 150))
    hot(pane, fill=False).save(TEXTURES / "item/hot_glass_pane.png")
    heap("bisphenol_a", (255, 255, 255), (236, 234, 226), (184, 182, 172)).save(TEXTURES / "item/bisphenol_a.png")
    heap("polycarbonate_resin", (244, 248, 252), (214, 224, 232), (160, 172, 184)).save(TEXTURES / "item/polycarbonate_resin.png")
    sheet((206, 220, 232, 90), (150, 166, 182, 230)).save(TEXTURES / "item/polycarbonate_sheet.png")
    print("glass textures written")


if __name__ == "__main__":
    main()
