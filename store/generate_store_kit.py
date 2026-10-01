#!/usr/bin/env python3
"""Build the Cafe Bazaar / Google Play publishing kit for Novo.

Run it from the repository root:

    PYTHONPATH=/home/user/.tools python3 store/generate_store_kit.py

Output (all committed, so the marketing team can regenerate or tweak them):

    store/icon-512.png                  store icon (Bazaar: >=512px, <=3MB)
    store/feature-graphic-1024x500.png  header image (Bazaar: >=720x288, ratio 5:2)
    store/screenshots/01..06-*.png      1080x1920 phone screenshots

The screenshots are high fidelity mockups of the real screens (dark theme, the same
accent colours, the same Persian strings). Replace them with real device captures
before the final submission if the store reviewer asks for screenshots of the app.
"""

import os

import arabic_reshaper
from bidi.algorithm import get_display
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "store")
SHOTS = os.path.join(OUT, "screenshots")
FONTS = "/home/user/.fonts"

# ── design tokens (mirrors Theme.kt) ──────────────────────────────────────────
BG = (12, 12, 14)
SURFACE = (20, 20, 25)
SURFACE_VAR = (28, 28, 34)
TEXT = (240, 240, 245)
MUTED = (142, 142, 154)
GOLD = (255, 193, 7)
PURPLE = (124, 77, 255)
CYAN = (0, 229, 255)
GREEN = (0, 230, 118)
RED = (255, 82, 82)

F_REG = ImageFont.truetype(f"{FONTS}/Vazirmatn-Regular.ttf", 34)
F_MED = ImageFont.truetype(f"{FONTS}/Vazirmatn-Medium.ttf", 34)
F_BOLD = ImageFont.truetype(f"{FONTS}/Vazirmatn-Bold.ttf", 34)


def font(size, weight="regular"):
    return ImageFont.truetype(f"{FONTS}/Vazirmatn-{weight.capitalize()}.ttf", size)


def fa(text):
    """Shape + reorder Persian text so Pillow draws it correctly without libraqm."""
    return get_display(arabic_reshaper.reshape(text))


def rtl(draw, xy, text, f, fill=TEXT, anchor="ra"):
    """Right-aligned Persian text (xy is the right edge)."""
    draw.text(xy, fa(text), font=f, fill=fill, anchor=anchor, direction=None)


def ltr(draw, xy, text, f, fill=TEXT, anchor="la"):
    draw.text(xy, text, font=f, fill=fill, anchor=anchor)


def fit_fa(draw, text, max_width, start_size, weight="regular", min_size=18):
    """Largest Vazirmatn size at which the shaped Persian text fits max_width."""
    size = start_size
    while size > min_size:
        f = font(size, weight)
        if draw.textlength(fa(text), font=f) <= max_width:
            return f
        size -= 2
    return font(min_size, weight)


def fit_ltr(draw, text, max_width, start_size, weight="regular", min_size=18):
    size = start_size
    while size > min_size:
        f = font(size, weight)
        if draw.textlength(text, font=f) <= max_width:
            return f
        size -= 2
    return font(min_size, weight)


def rrect(draw, box, radius, fill=None, outline=None, width=2):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def gradient(size, top, bottom, radius=0):
    """Vertical gradient, optionally with rounded corners."""
    w, h = size
    base = Image.new("RGB", (1, h))
    for y in range(h):
        t = y / max(h - 1, 1)
        base.putpixel((0, y), tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))
    img = base.resize((w, h))
    if radius:
        mask = Image.new("L", (w, h), 0)
        ImageDraw.Draw(mask).rounded_rectangle((0, 0, w - 1, h - 1), radius=radius, fill=255)
        out = Image.new("RGB", (w, h), BG)
        out.paste(img, (0, 0), mask)
        return out
    return img


def cover_art(base_palette, size=200, seed=0):
    """A synthetic album cover (the mockups have no real artwork to embed)."""
    img = gradient((size, size), base_palette[0], base_palette[1], radius=int(size * 0.16))
    d = ImageDraw.Draw(img)
    c = size // 2
    for i, r in enumerate(range(int(size * 0.20), int(size * 0.52), max(1, size // 14))):
        alpha = 60 - i * 6
        d.ellipse((c - r, c - r, c + r, c + r), outline=(255, 255, 255, max(alpha, 12)), width=2)
    d.ellipse((c - size // 7, c - size // 7, c + size // 7, c + size // 7), fill=base_palette[2])
    return img


def paste_cover(canvas, box, palette, radius=None, seed=0):
    x, y, s = box
    art = cover_art(palette, size=s, seed=seed)
    if radius is not None and radius < s // 2:
        mask = Image.new("L", (s, s), 0)
        ImageDraw.Draw(mask).rounded_rectangle((0, 0, s - 1, s - 1), radius=radius, fill=255)
        canvas.paste(art, (x, y), mask)
    else:
        canvas.paste(art, (x, y))


def phone_chrome(img, title, subtitle, accent=GOLD):
    """Status bar + a caption band, so every screenshot looks like a finished store asset."""
    d = ImageDraw.Draw(img)
    # status bar
    ltr(d, (48, 34), "9:41", font(30, "bold"), TEXT)
    for i, x in enumerate((940, 902, 864)):
        d.rounded_rectangle((x, 38, x + 18, 56), radius=4, fill=TEXT if i < 2 else MUTED)
    d.rounded_rectangle((982, 32, 1032, 60), radius=8, outline=TEXT, width=2)
    d.rectangle((986, 36, 1014, 56), fill=TEXT)
    # header
    rrect(d, (48, 96, 130, 178), 22, fill=accent)
    ltr(d, (72, 112), "N", font(48, "bold"), (20, 20, 24))
    rtl(d, (1032, 96), title, font(44, "bold"), TEXT)
    rtl(d, (1032, 150), subtitle, font(26, "regular"), MUTED)
    d.line((48, 208, 1032, 208), fill=(44, 44, 52), width=2)


def track_row(img, y, palette, title, artist, duration, *, playing=False, favorite=False,
              accent=GOLD, x0=48, x1=1032):
    d = ImageDraw.Draw(img)
    if playing:
        rrect(d, (x0, y - 10, x1, y + 118), 20, fill=(accent[0] // 6, accent[1] // 6, accent[2] // 6))
    paste_cover(img, (x1 - 104, y, 104), palette, radius=18)
    rtl(d, (x1 - 128, y + 8), title, font(32, "bold") if playing else font(32, "medium"),
        accent if playing else TEXT)
    rtl(d, (x1 - 128, y + 54), artist, font(26, "regular"), MUTED)
    rtl(d, (x0 + 16, y + 30), duration, font(24, "regular"), MUTED)
    # heart
    heart(d, x0 + 78, y + 46, 30, RED if favorite else None)


def heart(d, cx, cy, size, color):
    """A real heart: two lobes + a triangle, filled or outlined."""
    r = size * 0.30
    if color:
        d.ellipse((cx - size * 0.52, cy - size * 0.40, cx + size * 0.02, cy + size * 0.14), fill=color)
        d.ellipse((cx - size * 0.02, cy - size * 0.40, cx + size * 0.52, cy + size * 0.14), fill=color)
        d.polygon([(cx - size * 0.50, cy - size * 0.10), (cx + size * 0.50, cy - size * 0.10), (cx, cy + size * 0.56)], fill=color)
    else:
        d.ellipse((cx - size * 0.52, cy - size * 0.40, cx + size * 0.02, cy + size * 0.14), outline=MUTED, width=3)
        d.ellipse((cx - size * 0.02, cy - size * 0.40, cx + size * 0.52, cy + size * 0.14), outline=MUTED, width=3)
        d.polygon([(cx - size * 0.50, cy - size * 0.10), (cx + size * 0.50, cy - size * 0.10), (cx, cy + size * 0.56)], outline=MUTED, width=3)


def play_icon(d, cx, cy, size, color):
    d.polygon([(cx - size * 0.30, cy - size * 0.42), (cx + size * 0.46, cy), (cx - size * 0.30, cy + size * 0.42)], fill=color)


def pause_icon(d, cx, cy, size, color):
    d.rounded_rectangle((cx - size * 0.30, cy - size * 0.40, cx - size * 0.06, cy + size * 0.40), radius=5, fill=color)
    d.rounded_rectangle((cx + size * 0.06, cy - size * 0.40, cx + size * 0.30, cy + size * 0.40), radius=5, fill=color)


def skip_icon(d, cx, cy, size, color, forward=True):
    s = 1 if forward else -1
    d.polygon([(cx - s * size * 0.36, cy - size * 0.42), (cx + s * size * 0.10, cy), (cx - s * size * 0.36, cy + size * 0.42)], fill=color)
    d.rounded_rectangle((min(cx + s * size * 0.16, cx + s * size * 0.34), cy - size * 0.40,
                         max(cx + s * size * 0.16, cx + s * size * 0.34), cy + size * 0.40), radius=4, fill=color)


def shuffle_icon(d, cx, cy, size, color):
    d.line((cx - size * 0.4, cy - size * 0.2, cx + size * 0.4, cy + size * 0.2), fill=color, width=5)
    d.line((cx - size * 0.4, cy + size * 0.2, cx + size * 0.4, cy - size * 0.2), fill=color, width=5)
    d.polygon([(cx + size * 0.4, cy + size * 0.2), (cx + size * 0.16, cy + size * 0.06), (cx + size * 0.24, cy + size * 0.36)], fill=color)
    d.polygon([(cx + size * 0.4, cy - size * 0.2), (cx + size * 0.16, cy - size * 0.06), (cx + size * 0.24, cy - size * 0.36)], fill=color)


def repeat_icon(d, cx, cy, size, color, one=False):
    d.arc((cx - size * 0.36, cy - size * 0.30, cx + size * 0.36, cy + size * 0.30), 20, 330, fill=color, width=5)
    d.polygon([(cx + size * 0.30, cy - size * 0.34), (cx + size * 0.44, cy - size * 0.10), (cx + size * 0.14, cy - size * 0.14)], fill=color)
    if one:
        ltr(d, (cx - 6, cy - 20), "1", font(int(size * 0.5), "bold"), color, anchor="ma")


def slider(d, x0, x1, y, ratio, color, track=(70, 70, 80), knob=True, h=8):
    d.rounded_rectangle((x0, y - h // 2, x1, y + h // 2), radius=h // 2, fill=track)
    d.rounded_rectangle((x0, y - h // 2, x0 + (x1 - x0) * ratio, y + h // 2), radius=h // 2, fill=color)
    if knob:
        cx = x0 + (x1 - x0) * ratio
        d.ellipse((cx - 17, y - 17, cx + 17, y + 17), fill=color)


# ── screenshots ──────────────────────────────────────────────────────────────
def shot_songs(path):
    img = gradient((1080, 1920), (16, 16, 20), (10, 10, 12)).convert("RGB")
    d = ImageDraw.Draw(img)
    phone_chrome(img, "آهنگ‌ها", "کتابخانهٔ کامل، روی گوشی خودتان")

    # search field
    rrect(d, (48, 236, 1032, 320), 42, fill=SURFACE_VAR)
    rtl(d, (1000, 258), "جست‌وجو در آهنگ‌ها…", font(26), MUTED)
    d.ellipse((72, 262, 104, 294), outline=GOLD, width=4)
    d.line((100, 292, 116, 308), fill=GOLD, width=5)

    # tabs
    tabs = ["همه آهنگ‌ها", "علاقه‌مندی‌ها", "پربازدیدترین", "اخیراً"]
    x = 1032
    for i, t in enumerate(tabs):
        w = d.textlength(fa(t), font=font(28, "bold" if i == 0 else "regular"))
        if i == 0:
            rrect(d, (x - w - 34, 348, x + 12, 412), 32, fill=(GOLD[0] // 5, GOLD[1] // 5, GOLD[2] // 5))
        rtl(d, (x - 11, 362), t, font(28, "bold" if i == 0 else "regular"), GOLD if i == 0 else MUTED)
        x -= w + 60
    d.rounded_rectangle((48, 420, 1032, 426), radius=3, fill=(44, 44, 52))
    d.rounded_rectangle((672, 420, 1032, 426), radius=3, fill=GOLD)
    # swipe affordance
    d.polygon([(78, 500), (104, 518), (78, 536)], fill=MUTED)
    d.polygon([(1002, 500), (976, 518), (1002, 536)], fill=MUTED)
    ltr(d, (540, 520), fa("برای عوض کردن تب، با انگشت بکشید"), font(23), MUTED, anchor="mm")

    rows = [
        (("امتحان", "مهستی", "3:15", ((24, 36, 46), (58, 44, 96), GOLD)), True, True),
        (("دل‌تنگی", "معین", "4:02", ((46, 22, 34), (86, 30, 62), CYAN)), False, True),
        (("شب‌های تهران", "داریوش", "3:48", ((18, 34, 44), (18, 74, 88), GREEN)), False, False),
        (("لالایی", "گوگوش", "5:12", ((40, 26, 18), (110, 62, 24), (255, 152, 0))), False, False),
        (("پرواز", "ابی", "4:25", ((20, 26, 52), (52, 40, 110), PURPLE)), False, False),
        (("همسفر", "ناصر عبداللهی", "3:56", ((22, 40, 30), (26, 82, 62), GREEN)), False, False),
        (("قفس", "ساسی", "3:32", ((44, 18, 24), (96, 26, 44), RED)), False, False),
    ]
    y = 492
    for (t, a, dur, pal), playing, fav in rows:
        track_row(img, y, pal, t, a, dur, playing=playing, favorite=fav)
        y += 150

    d.line((48, 1810, 1032, 1810), fill=(34, 34, 40), width=2)
    nav = [("آهنگ‌ها", True), ("پلی‌لیست‌ها", False), ("تنظیمات", False)]
    x = 900
    for label, active in nav:
        w = d.textlength(fa(label), font=font(26, "bold" if active else "regular"))
        rtl(d, (x, 1844), label, font(26, "bold" if active else "regular"), GOLD if active else MUTED)
        x -= w + 120
    img.save(path)
    return path


def shot_now_playing(path):
    img = gradient((1080, 1920), (18, 16, 26), (10, 10, 12)).convert("RGB")
    d = ImageDraw.Draw(img)
    phone_chrome(img, "در حال پخش", "با کشیدن کاور، آهنگ عوض می‌شود")

    # vinyl
    cx, cy, R = 540, 640, 300
    art = cover_art(((30, 30, 58), (86, 40, 120), GOLD), size=2 * R, seed=3)
    mask = Image.new("L", (2 * R, 2 * R), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, 2 * R - 1, 2 * R - 1), fill=255)
    img.paste(art, (cx - R, cy - R), mask)
    for r in range(R - 20, R // 3, -34):
        d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=(255, 255, 255, 14), width=2)
    d.ellipse((cx - 78, cy - 78, cx + 78, cy + 78), fill=(18, 18, 24))
    d.ellipse((cx - 26, cy - 26, cx + 26, cy + 26), fill=GOLD)
    d.arc((cx - R - 26, cy - R - 26, cx + R + 26, cy + R + 26), 200, 340, fill=(PURPLE[0], PURPLE[1], PURPLE[2]), width=7)

    # swipe hint pill (mid-swipe preview)
    rrect(d, (300, 992, 780, 1058), 33, fill=(38, 38, 46))
    rtl(d, (760, 1006), "بعدی: دل‌تنگی", font(28, "medium"), TEXT)
    skip_icon(d, 330, 1025, 34, GOLD, forward=True)

    rtl(d, (1032, 1090), "امتحان", font(52, "bold"), TEXT)
    rtl(d, (1032, 1160), "مهستی • آلبوم خاطره‌ها", font(28), MUTED)
    spark = ImageDraw.Draw(img)
    spark.ellipse((72, 1104, 104, 1136), outline=GOLD, width=4)
    spark.line((88, 1104, 88, 1136), fill=GOLD, width=3)
    spark.line((72, 1120, 104, 1120), fill=GOLD, width=3)

    slider(d, 72, 1032, 1258, 0.42, GOLD)
    rtl(d, (1032, 1288), "3:15", font(24), MUTED)
    ltr(d, (72, 1288), "1:24", font(24), MUTED)

    # controls
    shuffle_icon(d, 190, 1430, 96, MUTED)
    skip_icon(d, 360, 1430, 110, TEXT, forward=False)
    d.ellipse((486, 1374, 594, 1482), fill=GOLD)
    play_icon(d, 540, 1428, 88, (24, 24, 28))
    skip_icon(d, 720, 1430, 110, TEXT, forward=True)
    repeat_icon(d, 890, 1430, 96, MUTED)

    # secondary actions
    rrect(d, (72, 1580, 1008, 1690), 30, fill=SURFACE_VAR)
    acts = [("علاقه‌مندی", RED), ("پلی‌لیست", MUTED), ("اشتراک‌گذاری", MUTED), ("پوستر برند", GOLD), ("اکولایزر", MUTED)]
    x = 960
    for label, color in acts:
        w = d.textlength(fa(label), font=font(24))
        rtl(d, (x, 1618), label, font(24), color)
        x -= w + 96
    ltr(d, (540, 1762), fa("برچسب «بعدی/قبلی» را در حین کشیدن ببینید"), font(24), MUTED, anchor="mm")
    img.save(path)
    return path


def shot_widget(path):
    img = gradient((1080, 1920), (26, 20, 44), (12, 14, 24)).convert("RGB")
    d = ImageDraw.Draw(img)
    phone_chrome(img, "ویجت صفحهٔ اصلی", "کنترل کامل بدون باز کردن اپ")

    ltr(d, (72, 250), "10:09", font(96, "regular"), TEXT, anchor="la")
    rtl(d, (1032, 260), "شنبه ۸ مهر", font(30), TEXT)

    # full widget 4x2
    rrect(d, (48, 380, 1032, 800), 40, fill=(24, 24, 28), outline=(46, 46, 54), width=2)
    paste_cover(img, (84, 416, 200), ((24, 36, 46), (58, 44, 96), GOLD), radius=32)
    rtl(d, (930, 424), "امتحان", font(44, "bold"), TEXT)
    rtl(d, (930, 484), "مهستی", font(30), MUTED)
    heart(d, 992, 452, 38, RED)  # favourite heart
    slider(d, 320, 948, 580, 0.42, CYAN, h=12)
    rtl(d, (948, 610), "3:15", font(26, "medium"), TEXT)
    ltr(d, (320, 610), "1:24", font(26, "medium"), TEXT)
    shuffle_icon(d, 260, 720, 74, GOLD)
    skip_icon(d, 420, 720, 90, TEXT, forward=False)
    d.ellipse((500, 660, 620, 780), fill=GOLD)
    play_icon(d, 560, 720, 80, (24, 24, 28))
    skip_icon(d, 700, 720, 90, TEXT, forward=True)
    repeat_icon(d, 860, 720, 74, TEXT)

    # compact widget 2x1
    rrect(d, (48, 840, 560, 1010), 32, fill=(24, 24, 28), outline=(46, 46, 54), width=2)
    paste_cover(img, (76, 866, 116), ((46, 22, 34), (86, 30, 62), CYAN), radius=22)
    rtl(d, (452, 862), "دل‌تنگی", font(30, "bold"), TEXT)
    rtl(d, (452, 902), "معین", font(22), MUTED)
    slider(d, 188, 452, 956, 0.35, CYAN, h=8, knob=False)
    d.ellipse((470, 862, 536, 928), fill=GOLD)
    play_icon(d, 503, 895, 44, (24, 24, 28))
    rtl(d, (1032, 880), "اندازهٔ ویجت را بکشید:", font(26), MUTED)
    rtl(d, (1032, 926), "خودکار به حالت فشرده می‌رود", font(26), GOLD)

    # app grid (icon cells)
    labels = ["Novo", "دوربین", "گالری", "پیام‌ها", "ساعت", "تقویم", "مرورگر", "ماشین‌حساب"]
    cols, cell = 4, 168
    x0, y0 = 72, 1080
    for i, name in enumerate(labels):
        r, c = divmod(i, cols)
        x = x0 + c * 240
        y = y0 + r * 210
        base = [(255, 193, 7), (0, 229, 255), (124, 77, 255), (0, 230, 118), (255, 82, 82), (255, 152, 0)][i % 6]
        rrect(d, (x, y, x + cell, y + cell), 40, fill=tuple(int(v * 0.22) for v in base))
        if i == 0:
            ltr(d, (x + 46, y + 34), "N", font(74, "bold"), GOLD)
        else:
            d.ellipse((x + 44, y + 44, x + 124, y + 124), outline=base, width=6)
        rtl(d, (x + cell, y + cell + 12), name, font(24), TEXT)
    # dock
    rrect(d, (48, 1746, 1032, 1878), 40, fill=(30, 30, 38))
    for i in range(4):
        x = 120 + i * 220
        rrect(d, (x, 1778, x + 116, 1894), 30, fill=(60 + i * 20, 60, 90))
    img.save(path)
    return path


def shot_equalizer(path):
    img = gradient((1080, 1920), (14, 20, 22), (10, 10, 12)).convert("RGB")
    d = ImageDraw.Draw(img)
    phone_chrome(img, "اکولایزر", "۵ باند گرافیکی + پریست‌های آماده", accent=GREEN)

    freqs = ["60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz"]
    vals = [0.75, 0.62, 0.40, 0.58, 0.70]
    rrect(d, (48, 250, 1032, 1180), 40, fill=SURFACE)
    for i, (f, v) in enumerate(zip(freqs, vals)):
        x = 150 + i * 190
        d.rounded_rectangle((x - 10, 340, x + 10, 1020), radius=10, fill=(52, 52, 62))
        y = 1020 - (1020 - 340) * v
        d.rounded_rectangle((x - 10, y, x + 10, 1020), radius=10, fill=GREEN)
        d.ellipse((x - 30, y - 30, x + 30, y + 30), fill=(240, 240, 245))
        ltr(d, (x, 1050), f, font(24, "medium"), MUTED, anchor="ma")
        ltr(d, (x - 34, 306), f"+{int(v * 12 - 6)}dB", font(22), MUTED, anchor="ma")
    rtl(d, (1000, 1096), "پریست فعال: باس‌بوست", font(26, "medium"), GREEN)

    presets = ["تخت", "باس‌بوست", "جاز", "راک", "پاپ", "کلاسیک"]
    x = 1032
    for i, p in enumerate(presets):
        w = d.textlength(fa(p), font=font(26, "bold" if i == 1 else "regular"))
        rrect(d, (x - w - 40, 1240, x, 1310), 35, fill=(GREEN[0] // 6, GREEN[1] // 6, GREEN[2] // 6) if i == 1 else SURFACE_VAR)
        rtl(d, (x - 20, 1258), p, font(26, "bold" if i == 1 else "regular"), GREEN if i == 1 else TEXT)
        x -= w + 60

    rows = [("فعال بودن اکولایزر", True), ("باس تقویت‌شده (Bass Boost)", True), ("محدودکننده (Loudness)", False)]
    y = 1380
    for label, on in rows:
        rrect(d, (48, y, 1032, y + 110), 28, fill=SURFACE_VAR)
        rtl(d, (1000, y + 32), label, font(28), TEXT)
        rrect(d, (96, y + 34, 220, y + 78), 22, fill=GREEN if on else (60, 60, 70))
        d.ellipse((166 if on else 104, y + 38, 214 if on else 152, y + 74), fill=(16, 16, 20) if on else (200, 200, 210))
        y += 132
    rtl(d, (1032, 1830), "بدون تبلیغات • روی گوشی اجرا می‌شود", font(26), MUTED)
    img.save(path)
    return path


def shot_settings(path):
    img = gradient((1080, 1920), (16, 16, 22), (10, 10, 12)).convert("RGB")
    d = ImageDraw.Draw(img)
    phone_chrome(img, "تنظیمات", "فارسی، رنگ دلخواه، تایمر خواب")

    rrect(d, (48, 250, 1032, 470), 36, fill=SURFACE)
    rtl(d, (1000, 276), "زبان برنامه", font(30, "bold"), TEXT)
    rtl(d, (1000, 322), "تشخیص خودکار از زبان گوشی (فارسی/انگلیسی)", font(24), MUTED)
    for i, (label, active) in enumerate([("فارسی", True), ("English", False)]):
        w = d.textlength(fa(label), font=font(28, "bold"))
        x1 = 1000 - i * 300
        rrect(d, (x1 - w - 44, 372, x1, 444), 36, fill=(GOLD[0] // 5, GOLD[1] // 5, GOLD[2] // 5) if active else SURFACE_VAR)
        rtl(d, (x1 - 22, 388), label, font(28, "bold" if active else "regular"), GOLD if active else TEXT)

    rrect(d, (48, 494, 1032, 730), 36, fill=SURFACE)
    rtl(d, (1000, 520), "رنگ تم", font(30, "bold"), TEXT)
    rtl(d, (1000, 566), "رنگ دکمه‌ها، تایم‌لاین ویجت و جلوه‌ها", font(24), MUTED)
    colors = [("طلایی", GOLD), ("بنفش", PURPLE), ("زمردی", GREEN), ("فیروزه‌ای", CYAN), ("سرخ", RED)]
    x = 1000
    for name, c in colors:
        d.ellipse((x - 60, 626, x, 686), fill=c)
        if name == "طلایی":
            d.ellipse((x - 74, 612, x + 14, 700), outline=GOLD, width=3)
        x -= 176

    rows = [("اعلان پخش با تصویر کاور", True), ("کنترل روی صفحهٔ قفل", True), ("تایمر خواب", False), ("یکسان‌سازی صدا (Loudness)", True)]
    y = 754
    for label, on in rows:
        rrect(d, (48, y, 1032, y + 116), 28, fill=SURFACE_VAR)
        rtl(d, (1000, y + 34), label, font(28), TEXT)
        if label == "تایمر خواب":
            rtl(d, (120, y + 36), "۱۵ دقیقه ›", font(24), MUTED)
        else:
            rrect(d, (96, y + 38, 220, y + 82), 22, fill=GOLD if on else (60, 60, 70))
            d.ellipse((166 if on else 104, y + 42, 214 if on else 152, y + 78), fill=(16, 16, 20) if on else (200, 200, 210))
        y += 138

    rrect(d, (48, 1310, 1032, 1520), 36, fill=SURFACE)
    rtl(d, (1000, 1336), "مخزن پخش و کتابخانه", font(30, "bold"), TEXT)
    rtl(d, (1000, 1382), "اسکن پوشه‌ها، پخش‌های اخیر، پیشرفت پخش‌شده", font(24), MUTED)
    rtl(d, (1000, 1434), "همهٔ فایل‌ها فقط روی خود گوشی پردازش می‌شوند", font(24), GREEN)
    rtl(d, (1032, 1840), "نسخه 1.1.0 • ساخت تیم نوین‌وب", font(26), MUTED)
    img.save(path)
    return path


def shot_brand(path):
    img = gradient((1080, 1920), (18, 14, 26), (10, 10, 12)).convert("RGB")
    d = ImageDraw.Draw(img)
    phone_chrome(img, "نوین‌وب", "بیشتر از یک پخش‌کنندهٔ موسیقی")

    rrect(d, (48, 250, 1032, 700), 40, fill=SURFACE)
    rrect(d, (96, 292, 206, 402), 30, fill=GOLD)
    ltr(d, (124, 312), "N", font(62, "bold"), (24, 24, 28))
    rtl(d, (856, 300), "بیشتر از نوین‌وب", font(36, "bold"), TEXT)
    rtl(d, (1000, 372), "اپ را به دوستانتان معرفی کنید؛ ما", font(27), MUTED)
    rtl(d, (1000, 414), "پخش‌کننده‌های سریع‌تر و بی‌تبلیغات", font(27), MUTED)
    rtl(d, (1000, 456), "می‌سازیم و همه‌چیز رایگان می‌ماند.", font(27), MUTED)
    rrtl = 1000
    rrect(d, (596, 546, 1000, 646), 30, fill=(GOLD[0] // 5, GOLD[1] // 5, GOLD[2] // 5))
    rtl(d, (976, 572), "کد تخفیف ندارد، خودش رایگان است", font(26), GOLD)

    links = [
        ("وب‌سایت نوین‌وب", "webnovo.ir • محصولات و پروژه‌ها", "link"),
        ("امتیاز دادن به Novo", "با یک امتیاز، دیده‌شدن اپ بیشتر می‌شود", "star"),
        ("آخرین نسخه", "دانلود جدیدترین فایل نصب از صفحهٔ انتشار", "download"),
    ]
    y = 730
    for title, desc, icon in links:
        rrect(d, (48, y, 1032, y + 150), 30, fill=SURFACE_VAR)
        rtl(d, (920, y + 26), title, font(30, "bold"), TEXT)
        rtl(d, (920, y + 76), desc, font(25), MUTED)
        cxb, cyb = 138, y + 76
        d.ellipse((cxb - 34, cyb - 34, cxb + 34, cyb + 34), fill=(GOLD[0] // 6, GOLD[1] // 6, GOLD[2] // 6))
        if icon == "link":
            d.line((cxb - 12, cyb + 12, cxb + 12, cyb - 12), fill=GOLD, width=5)
            d.polygon([(cxb + 12, cyb - 12), (cxb - 2, cyb - 12), (cxb + 12, cyb + 2)], fill=GOLD)
            d.line((cxb - 12, cyb + 12, cxb - 12, cyb - 2), fill=GOLD, width=5)
        elif icon == "star":
            pts = []
            for k in range(10):
                ang = -1.5708 + k * 0.6283
                r = 22 if k % 2 == 0 else 9
                pts.append((cxb + r * __import__("math").cos(ang), cyb + r * __import__("math").sin(ang)))
            d.polygon(pts, fill=GOLD)
        else:
            d.line((cxb, cyb - 20, cxb, cyb + 12), fill=GOLD, width=5)
            d.polygon([(cxb - 12, cyb + 8), (cxb + 12, cyb + 8), (cxb, cyb + 24)], fill=GOLD)
            d.line((cxb - 16, cyb - 26, cxb + 16, cyb - 26), fill=GOLD, width=5)
        y += 172

    # poster preview
    rrect(d, (48, 1290, 520, 1880), 36, fill=(60, 20, 90))
    rrect(d, (48, 1290, 520, 1620), 36, fill=(90, 30, 130))
    ltr(d, (120, 1330), "N", font(120, "bold"), GOLD)
    rtl(d, (470, 1520), "نوین‌وب", font(40, "bold"), TEXT)
    rtl(d, (470, 1580), "رایگان، بدون تبلیغات", font(26), (235, 225, 245))
    rtl(d, (470, 1660), "هنوز دوست داری این", font(24), MUTED)
    rtl(d, (470, 1700), "آهنگ را گوش کنی؟", font(24), MUTED)
    rtl(d, (470, 1760), "webnovo.ir", font(26, "bold"), GOLD)

    rtl(d, (1032, 1320), "پوستر اشتراک‌گذاری", font(32, "bold"), TEXT)
    rtl(d, (1032, 1376), "پوستر ۱۰۸۰×۱۹۲۰ با نام آهنگ", font(25), MUTED)
    rtl(d, (1032, 1424), "و برند نوین‌وب آماده می‌شود؛", font(25), MUTED)
    rtl(d, (1032, 1472), "کافی است در استوری یا وضعیت", font(25), MUTED)
    rtl(d, (1032, 1520), "واتس‌اپ/تلگرام منتشرش کنید.", font(25), MUTED)
    rtl(d, (1032, 1600), "۰ تومان تبلیغات لازم ندارد؛", font(26, "medium"), GOLD)
    rtl(d, (1032, 1650), "هر پخش‌کنندهٔ کوچک، رسانهٔ ماست.", font(26, "medium"), GOLD)
    rtl(d, (1032, 1840), "بدون تبلیغات • بدون خرید درون‌برنامه‌ای", font(26), GREEN)
    img.save(path)
    return path


# ── store icon + feature graphic ─────────────────────────────────────────────
def make_icon():
    src = Image.open(os.path.join(ROOT, "app/src/main/res/drawable/novo_app_icon_1790749229212.jpg")).convert("RGB")
    icon = src.resize((512, 512), Image.LANCZOS)
    # a small trim of the dark border makes the mark fill the tile better
    icon = icon.crop((34, 34, 478, 478)).resize((512, 512), Image.LANCZOS)
    icon.save(os.path.join(OUT, "icon-512.png"))
    return os.path.join(OUT, "icon-512.png")


def make_feature_graphic():
    w, h = 1024, 500
    img = gradient((w, h), (22, 12, 34), (8, 8, 14)).convert("RGB")
    d = ImageDraw.Draw(img)
    for i in range(0, w, 4):
        d.line((i, 0, i + 200, h), fill=(26 + i // 40, 14, 40 + i // 30), width=2)
    img = img.filter(ImageFilter.GaussianBlur(2))
    d = ImageDraw.Draw(img)

    # wordmark on the right, copy flowing leftwards (RTL)
    cx, cy = 830, 250
    d.ellipse((cx - 150, cy - 150, cx + 150, cy + 150), outline=PURPLE, width=3)
    d.ellipse((cx - 116, cy - 116, cx + 116, cy + 116), outline=CYAN, width=3)
    d.ellipse((cx - 82, cy - 82, cx + 82, cy + 82), fill=(20, 20, 30), outline=GOLD, width=4)
    f_n = font(96, "bold")
    ltr(d, (cx - d.textlength("N", font=f_n) / 2, cy - 74), "N", f_n, GOLD)

    right = cx - 190
    f1 = fit_fa(d, "نوین‌وب", right - 24, 84, "bold")
    rtl(d, (right, 92), "نوین‌وب", f1, TEXT)
    f2 = fit_fa(d, "پخش‌کنندهٔ موسیقی آفلاین", right - 24, 44, "medium")
    rtl(d, (right, 212), "پخش‌کنندهٔ موسیقی آفلاین", f2, GOLD)
    f3 = fit_fa(d, "بدون تبلیغات • بدون اینترنت • کاملاً رایگان", right - 24, 32)
    rtl(d, (right, 286), "بدون تبلیغات • بدون اینترنت • کاملاً رایگان", f3, MUTED)
    f4 = fit_fa(d, "ویجت حرفه‌ای • اکولایزر ۵ باندی • کنترل صفحهٔ قفل", right - 24, 30)
    rtl(d, (right, 356), "ویجت حرفه‌ای • اکولایزر ۵ باندی • کنترل صفحهٔ قفل", f4, (200, 200, 215))

    # play motif on the left, fully inside the frame
    d.polygon([(120, 190), (168, 215), (120, 240)], fill=CYAN)
    d.polygon([(66, 158), (140, 215), (66, 272)], fill=CYAN)
    lines = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ld = ImageDraw.Draw(lines)
    for i, x in enumerate((36, 44, 52)):
        ld.line((x, 215 - 30 + i * 12, x, 215 + 30 - i * 12), fill=CYAN + (150 - i * 40,), width=4)
    img = Image.alpha_composite(img.convert("RGBA"), lines).convert("RGB")
    d = ImageDraw.Draw(img)
    img.save(os.path.join(OUT, "feature-graphic-1024x500.png"))
    return os.path.join(OUT, "feature-graphic-1024x500.png")


def main():
    os.makedirs(SHOTS, exist_ok=True)
    made = [make_icon(), make_feature_graphic()]
    made.append(shot_songs(os.path.join(SHOTS, "01-songs-tabs.png")))
    made.append(shot_now_playing(os.path.join(SHOTS, "02-now-playing-swipe.png")))
    made.append(shot_widget(os.path.join(SHOTS, "03-home-widget.png")))
    made.append(shot_equalizer(os.path.join(SHOTS, "04-equalizer.png")))
    made.append(shot_settings(os.path.join(SHOTS, "05-settings-fa.png")))
    made.append(shot_brand(os.path.join(SHOTS, "06-brand-novinweb.png")))
    for p in made:
        print(f"{os.path.relpath(p, ROOT):55s} {os.path.getsize(p) / 1024:7.0f} KB")


if __name__ == "__main__":
    main()
