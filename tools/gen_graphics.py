#!/usr/bin/env python3
"""Generate SAWARGI branding assets (launcher icons, adaptive layers,
Play Store icon, feature graphic, screenshots) using Pillow.
Brand palette from the app theme: #2E7D32 / #60AD5E / #FFB300 / #1B5E20.
Non-committed build/tool script (graphics output in ALTOMEDIA/graphics).
"""
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

GRAPH = "/workspace/project/SAWARGI/ALTOMEDIA/graphics"
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
GREEN = (46, 125, 50)
GREEN_D = (27, 94, 32)
GOLD = (255, 179, 0)
WHITE = (255, 255, 255)


def _font(size):
    if os.path.exists(FONT):
        try:
            return ImageFont.truetype(FONT, size)
        except Exception:
            pass
    return ImageFont.load_default()


def radial_gradient_rect(w, h, top, bottom):
    img = Image.new("RGBA", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(h - 1, 1)
        col = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        d.line([(0, y), (w, y)], fill=col + (255,))
    return img


def rounded_gradient(size, radius):
    grad = radial_gradient_rect(size, size, GREEN, GREEN_D)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=255)
    return Image.composite(grad, Image.new("RGBA", (size, size), (0, 0, 0, 0)), mask)


def glyph(d, size, cx, cy, orb=0.30):
    r = size * orb
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=GOLD)
    rr = size * 0.20
    d.arc([cx - rr, cy - rr, cx + rr, cy + rr], 40, 320, fill=WHITE, width=max(1, int(size * 0.055)))
    d.arc([cx - rr, cy + size * 0.02, cx + rr, cy + size * 0.02 + rr], 200, 140, fill=WHITE, width=max(1, int(size * 0.055)))


def launcher(size, path):
    tile = rounded_gradient(size, int(size * 0.22))
    d = ImageDraw.Draw(tile)
    glyph(d, size, size / 2, size / 2)
    tile.save(path)


def adaptive(fore, back, size=432):
    fg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(fg)
    glyph(d, size, size / 2, size / 2)
    fg.save(fore)
    Image.new("RGBA", (size, size), GREEN).save(back)


def store_icon(size, path):
    tile = rounded_gradient(size, int(size * 0.18))
    d = ImageDraw.Draw(tile)
    glyph(d, size, size / 2, size * 0.44)
    d.text((0, 0), "", fill=WHITE)  # noop keep style
    name = "SAWARGI"
    fnt = _font(int(size * 0.11))
    bb = d.textbbox((0, 0), name, font=fnt)
    d.text(((size - (bb[2] - bb[0])) / 2, size * 0.72), name, font=fnt, fill=WHITE)
    tile.save(path)


def feature_graphic(path, w=1024, h=500):
    img = radial_gradient_rect(w, h, GREEN, GREEN_D)
    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([w * 0.55, h * 0.1, w * 1.05, h * 0.98], fill=(96, 173, 94, 110))
    glow = glow.filter(ImageFilter.GaussianBlur(40))
    img = Image.alpha_composite(img, glow)
    d = ImageDraw.Draw(img)
    cx, cy = w * 0.30, h * 0.5
    r = h * 0.22
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=GOLD)
    rr = h * 0.15
    d.arc([cx - rr, cy - rr, cx + rr, cy + rr], 40, 320, fill=WHITE, width=int(h * 0.045))
    d.arc([cx - rr, cy + h * 0.012, cx + rr, cy + h * 0.012 + rr], 200, 140, fill=WHITE, width=int(h * 0.045))
    d.text((w * 0.46, h * 0.30), "SAWARGI", font=_font(int(h * 0.32)), fill=WHITE)
    d.text((w * 0.46, h * 0.62), "Berbagi cerita & terhubung dengan sahabat", font=_font(int(h * 0.06)), fill=(255, 255, 255, 235))
    img.save(path)


def screenshot(path, title, tag, w=1080, h=1920):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, w, h], fill=(246, 248, 246, 255))
    d.rectangle([0, 0, w, 150], fill=GREEN)
    d.rectangle([0, 150, w, 320], fill=WHITE)
    d.text((70, 205), title, font=_font(58), fill=GREEN_D)
    d.text((70, 280), "SAWARGI - jejaring sosial Anda", font=_font(34), fill=(120, 130, 120, 255))
    cards = [(180, 430), (180, 820), (180, 1210)]
    if "chat" in tag:
        cards = [(120, 470), (120, 680), (120, 890), (120, 1100)]
    if "profile" in tag:
        cards = [(60, 440)]
    for (x, y) in cards:
        d.rounded_rectangle([x, y, w - x, y + (330 if "profile" not in tag else 900)], radius=36, fill=(255, 255, 255, 255))
        d.rounded_rectangle([x + 30, y + 30, x + 150, y + 150], radius=60, fill=GREEN)
        d.rounded_rectangle([x + 180, y + 50, w - x - 40, y + 110], radius=20, fill=(226, 230, 226, 255))
        d.rounded_rectangle([x + 180, y + 130, w - x - 240, y + 185], radius=20, fill=(226, 230, 226, 255))
        d.rounded_rectangle([x + 30, y + 200, w - x - 40, y + 320], radius=20, fill=(226, 230, 226, 255))
    d.rectangle([0, h - 130, w, h], fill=WHITE)
    d.text((w / 2 - 210, h - 88), "Dibuat oleh ALTOMEDIA", font=_font(34), fill=(120, 130, 120, 255))
    img.save(path)


def main():
    for sub in ["launcher", "launcher-round", "adaptive", "play"]:
        os.makedirs(os.path.join(GRAPH, sub), exist_ok=True)
    dens = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for n, s in dens.items():
        launcher(s, os.path.join(GRAPH, "launcher", f"ic_launcher_{n}.png"))
    for n, s in dens.items():
        launcher(s, os.path.join(GRAPH, "launcher-round", f"ic_round_{n}.png"))
    adaptive(os.path.join(GRAPH, "adaptive", "ic_launcher_foreground.png"),
             os.path.join(GRAPH, "adaptive", "ic_launcher_background.png"))
    store_icon(512, os.path.join(GRAPH, "play", "icon_512.png"))
    feature_graphic(os.path.join(GRAPH, "play", "feature_graphic.png"))
    screenshot(os.path.join(GRAPH, "play", "screenshot_home.png"), "Beranda", "home")
    screenshot(os.path.join(GRAPH, "play", "screenshot_chat.png"), "Pesan", "chat")
    screenshot(os.path.join(GRAPH, "play", "screenshot_profile.png"), "Profil", "profile")
    print("=== generated ===")
    for root, _, files in os.walk(GRAPH):
        for f in files:
            print(os.path.join(root, f))


if __name__ == "__main__":
    main()