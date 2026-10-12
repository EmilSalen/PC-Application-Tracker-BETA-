"""Render the app's clock mark into a multi-size Windows .ico.

Mirrors AppIcon.kt so the installed app's icon matches the window/tray icon.
"""
from PIL import Image, ImageDraw

PLATE = (0x4F, 0x5B, 0xD5, 255)  # indigo, matches the theme primary
HAND = (255, 255, 255, 255)

SIZES = [16, 24, 32, 48, 64, 128, 256]
SS = 8  # supersample factor for smooth edges


def draw(size):
    s = size * SS
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = s / 2.0

    # Indigo disc
    r = s * 0.47
    d.ellipse([c - r, c - r, c + r, c + r], fill=PLATE)

    # Stroke width matches the Kotlin: 6% of size, floor 1.5px (scaled here).
    w = max(int(round(s * 0.06)), int(round(1.5 * SS)))
    # Face ring
    rr = s * 0.31
    d.ellipse([c - rr, c - rr, c + rr, c + rr], outline=HAND, width=w)
    # Minute hand: up
    d.line([c, c, c, c - s * 0.21], fill=HAND, width=w)
    # Hour hand: right
    d.line([c, c, c + s * 0.16, c], fill=HAND, width=w)

    return img.resize((size, size), Image.LANCZOS)


frames = [draw(sz) for sz in SIZES]
out = r"C:\Users\ejsal\DesktopProjects\TimeKeeperPC\icons\app.ico"
frames[-1].save(out, format="ICO", sizes=[(s, s) for s in SIZES])
print("wrote", out, "sizes", SIZES)
