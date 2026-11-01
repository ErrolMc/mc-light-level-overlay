"""Draws the mod icon: four blocks seen from above, three of them with spawn crosses, as 32x32 pixel art scaled to 128x128.

Run from the repo root:  python -I tools/make-icon.py
Writes src/main/resources/assets/light-level-overlay/icon.png. No dependencies beyond the standard library.
"""
import struct
import sys
import zlib
from pathlib import Path

SIZE = 32
SCALE = 4
OUT = Path("src/main/resources/assets/light-level-overlay/icon.png")

# Colours: the stone and water tops, and the cross colours from OverlayRenderer.java.
STONE = [(0x6B, 0x6B, 0x6B), (0x5E, 0x5E, 0x5E), (0x77, 0x77, 0x77), (0x52, 0x52, 0x52)]
WATER = [(0x2B, 0x4F, 0xC4), (0x24, 0x45, 0xB0), (0x35, 0x5E, 0xD8), (0x20, 0x3D, 0x9E)]
GRASS = [(0x5B, 0x9A, 0x3C), (0x4F, 0x8A, 0x33), (0x68, 0xA8, 0x45), (0x46, 0x7C, 0x2D)]
EDGE = (0x1A, 0x1A, 0x1A)
RED = (0xFF, 0x3A, 0x3A)
YELLOW = (0xFF, 0xD8, 0x3A)

img = [[None] * SIZE for _ in range(SIZE)]


def px(x, y, c):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        img[y][x] = c


def tile(x0, y0, palette):
    """A 16x16 block top with a fixed speckle pattern and a dark border."""
    for y in range(16):
        for x in range(16):
            px(x0 + x, y0 + y, palette[(x * 7 + y * 13 + (x * y) % 5) % len(palette)])
    for i in range(16):
        px(x0 + i, y0, EDGE)
        px(x0 + i, y0 + 15, EDGE)
        px(x0, y0 + i, EDGE)
        px(x0 + 15, y0 + i, EDGE)


def cross(x0, y0, colour):
    """A two-pixel-thick X inside the tile, with a dark rim so it reads on any background."""
    for i in range(3, 13):
        for x, y in ((x0 + i, y0 + i), (x0 + 15 - i, y0 + i)):
            for ox, oy in ((-1, 0), (1, 0), (0, -1), (0, 1), (1, 1), (-1, -1), (1, -1), (-1, 1)):
                if img[y + oy][x + ox] not in (RED, YELLOW):
                    px(x + ox, y + oy, EDGE)
    for i in range(3, 13):
        for x, y in ((x0 + i, y0 + i), (x0 + 15 - i, y0 + i)):
            px(x, y, colour)
            px(x + 1, y, colour)


tile(0, 0, STONE)
tile(16, 0, GRASS)
tile(0, 16, WATER)
tile(16, 16, STONE)
cross(0, 0, RED)
cross(16, 0, YELLOW)
cross(0, 16, RED)


def write_png(path, pixels, scale):
    rows = []
    for row in pixels:
        line = bytearray()
        for c in row:
            rgba = (0, 0, 0, 0) if c is None else (*c, 255)
            line += bytes(rgba) * scale
        rows += [b"\x00" + bytes(line)] * scale
    width = len(pixels[0]) * scale
    height = len(pixels) * scale

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(b"".join(rows), 9))
    png += chunk(b"IEND", b"")
    path.write_bytes(png)


if __name__ == "__main__":
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else OUT
    out.parent.mkdir(parents=True, exist_ok=True)
    write_png(out, img, SCALE)
    print(f"wrote {out} ({SIZE * SCALE}x{SIZE * SCALE})")
