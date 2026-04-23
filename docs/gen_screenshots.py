"""
Генерация mockup-скриншотов UI FilmLightMeter v1.2.0.
Не требует эмулятора — рисует прямо копию ретро-интерфейса.
Цвета и расположение элементов соответствуют MeterScreen.kt.
"""

from PIL import Image, ImageDraw, ImageFont, ImageFilter
from pathlib import Path

# Палитра из Theme.kt
LEATHER_DARK = (26, 24, 20)
LEATHER_BROWN = (43, 36, 28)
CREAM = (232, 223, 196)
CREAM_MUTED = (232, 223, 196, 180)
BRASS = (197, 155, 88)
BRASS_DIM = (197, 155, 88, 130)
RED_WARN = (180, 68, 47)

# Размер "экрана" (пропорции Android 9:18)
W, H = 480, 960
SCALE = 2  # нарисуем в 2x и отрендерим как антиалиасинг
W2, H2 = W * SCALE, H * SCALE


def load_font(size, bold=False, serif=False, mono=False):
    candidates = []
    if mono:
        candidates = [
            "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
        ]
    elif serif:
        candidates = [
            "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf",
        ]
    else:
        candidates = [
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        ]
    for c in candidates:
        if Path(c).exists():
            return ImageFont.truetype(c, size * SCALE)
    return ImageFont.load_default()


def vertical_gradient(w, h, top, mid, bot):
    img = Image.new("RGB", (w, h), top)
    px = img.load()
    for y in range(h):
        t = y / (h - 1)
        if t < 0.5:
            k = t / 0.5
            r = int(top[0] + (mid[0] - top[0]) * k)
            g = int(top[1] + (mid[1] - top[1]) * k)
            b = int(top[2] + (mid[2] - top[2]) * k)
        else:
            k = (t - 0.5) / 0.5
            r = int(mid[0] + (bot[0] - mid[0]) * k)
            g = int(mid[1] + (bot[1] - mid[1]) * k)
            b = int(mid[2] + (bot[2] - mid[2]) * k)
        for x in range(w):
            px[x, y] = (r, g, b)
    return img


def rounded_rect(draw, xy, radius, fill=None, outline=None, width=1):
    draw.rounded_rectangle(xy, radius=radius * SCALE, fill=fill, outline=outline, width=width * SCALE if outline else 0)


def draw_main_screen():
    base = vertical_gradient(W2, H2, LEATHER_DARK, LEATHER_BROWN, LEATHER_DARK)
    d = ImageDraw.Draw(base, "RGBA")

    # Шрифты
    f_title = load_font(18, bold=True)
    f_sub = load_font(10)
    f_label = load_font(9, bold=True)
    f_val = load_font(22, bold=True, serif=True)
    f_small_val = load_font(14, bold=True, serif=True)
    f_mono = load_font(10, mono=True)
    f_mono_bold = load_font(11, mono=True, bold=True)
    f_chip = load_font(10)
    f_scene = load_font(10)

    y = 14
    # --- Шапка ---
    d.text((14 * SCALE, y * SCALE), "FILM LIGHT METER", font=f_title, fill=BRASS)
    d.text((14 * SCALE, (y + 22) * SCALE), "Экспонометр для плёнки", font=f_sub, fill=CREAM_MUTED)

    # Кнопка справки ?
    help_cx, help_cy = W - 28, y + 16
    d.ellipse(((help_cx - 14) * SCALE, (help_cy - 14) * SCALE, (help_cx + 14) * SCALE, (help_cy + 14) * SCALE),
              outline=BRASS, width=2 * SCALE)
    d.text((help_cx * SCALE - 10, (help_cy - 10) * SCALE), "?", font=load_font(18, bold=True), fill=BRASS)

    y += 44

    # --- Превью камеры (заглушка) ---
    rounded_rect(d, (14 * SCALE, y * SCALE, (W - 14) * SCALE, (y + 180) * SCALE), 14,
                 fill=(20, 18, 14), outline=BRASS, width=2)
    # Симуляция кадра — градиент неба
    cam_grad = vertical_gradient((W - 28 - 4) * SCALE, (180 - 4) * SCALE, (60, 75, 95), (130, 140, 150), (90, 80, 60))
    mask = Image.new("L", cam_grad.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, cam_grad.size[0], cam_grad.size[1]), radius=12 * SCALE, fill=255)
    base.paste(cam_grad, ((14 + 2) * SCALE, (y + 2) * SCALE), mask)
    d = ImageDraw.Draw(base, "RGBA")

    # Плашка EV
    ev_box = (24 * SCALE, (y + 148) * SCALE, 120 * SCALE, (y + 172) * SCALE)
    rounded_rect(d, ev_box, 8, fill=(20, 18, 14, 210))
    d.text((32 * SCALE, (y + 153) * SCALE), "EV₁₀₀", font=f_mono, fill=(232, 223, 196, 180))
    d.text((68 * SCALE, (y + 151) * SCALE), "13.2", font=f_mono_bold, fill=CREAM)

    # Lock icon
    lk_x, lk_y = W - 40, y + 154
    d.rectangle(((lk_x - 6) * SCALE, (lk_y - 2) * SCALE, (lk_x + 6) * SCALE, (lk_y + 10) * SCALE),
                outline=CREAM, width=2 * SCALE)
    d.arc(((lk_x - 7) * SCALE, (lk_y - 10) * SCALE, (lk_x + 7) * SCALE, (lk_y + 4) * SCALE),
          start=180, end=360, fill=CREAM, width=2 * SCALE)

    y += 190

    # --- Режимы замера + кнопка Снять ---
    def chip(x, txt, selected=False):
        tw = d.textlength(txt, font=f_chip) / SCALE + 18
        box = (x * SCALE, y * SCALE, (x + tw) * SCALE, (y + 24) * SCALE)
        if selected:
            rounded_rect(d, box, 12, fill=BRASS)
            d.text(((x + 9) * SCALE, (y + 6) * SCALE), txt, font=f_chip, fill=LEATHER_DARK)
        else:
            rounded_rect(d, box, 12, fill=LEATHER_DARK, outline=(197, 155, 88, 120), width=1)
            d.text(((x + 9) * SCALE, (y + 6) * SCALE), txt, font=f_chip, fill=CREAM)
        return x + tw + 6

    x = 14
    x = chip(x, "Точка", False)
    x = chip(x, "Центр", True)
    x = chip(x, "Матрица", False)

    # Кнопка Снять
    btn_w = 68
    btn_x = W - 14 - btn_w
    rounded_rect(d, (btn_x * SCALE, y * SCALE, (btn_x + btn_w) * SCALE, (y + 24) * SCALE), 12, fill=BRASS)
    # иконка камеры — маленький прямоугольник
    d.rounded_rectangle((( btn_x + 8) * SCALE, (y + 7) * SCALE, (btn_x + 20) * SCALE, (y + 18) * SCALE),
                        radius=2 * SCALE, fill=LEATHER_DARK)
    d.ellipse(((btn_x + 12) * SCALE, (y + 9) * SCALE, (btn_x + 18) * SCALE, (y + 16) * SCALE),
              outline=BRASS, width=1 * SCALE)
    d.text(((btn_x + 25) * SCALE, (y + 6) * SCALE), "Снять", font=load_font(10, bold=True), fill=LEATHER_DARK)

    y += 36

    # --- Экспопара (крупная карточка) ---
    card_h = 88
    rounded_rect(d, (14 * SCALE, y * SCALE, (W - 14) * SCALE, (y + card_h) * SCALE), 14,
                 fill=LEATHER_BROWN, outline=BRASS, width=2)
    # Левый блок — выдержка
    d.text((45 * SCALE, (y + 12) * SCALE), "ВЫДЕРЖКА", font=f_label, fill=BRASS)
    d.text((55 * SCALE, (y + 24) * SCALE), "1/125", font=f_val, fill=CREAM)
    d.text((45 * SCALE, (y + 62) * SCALE), "рассчитано", font=load_font(8), fill=(197, 155, 88, 200))
    # × по центру
    d.text((W // 2 * SCALE - 8, (y + 28) * SCALE), "×", font=load_font(26, serif=True), fill=BRASS)
    # Диафрагма
    d.text((290 * SCALE, (y + 12) * SCALE), "ДИАФРАГМА", font=f_label, fill=BRASS)
    d.text((310 * SCALE, (y + 24) * SCALE), "f/5.6", font=f_val, fill=CREAM)
    d.text((290 * SCALE, (y + 62) * SCALE), "нажмите чтобы фиксировать", font=load_font(7), fill=(232, 223, 196, 120))

    y += card_h + 12

    # --- Блок «Варианты съёмки» (ГВОЗДЬ v1.2.0) ---
    pairs = [
        ("1/125", "f/5.6", "точно", True),
        ("1/60", "f/8",   "+0.00 EV", False),
        ("1/250", "f/4",  "+0.00 EV", False),
        ("1/30", "f/11",  "+0.00 EV", False),
    ]
    # Чуть корректируем ошибки для реалистичности (шкала Зенита + Гелиоса)
    pairs = [
        ("1/125", "f/5.6", "точно", True),
        ("1/250", "f/4",   "точно", True),
        ("1/60",  "f/8",   "точно", True),
        ("1/30",  "f/11",  "точно", True),
    ]
    # Делаем одну неточную для наглядности
    pairs[3] = ("1/30", "f/11", "+0.33 EV", False)

    card_h = 18 + 14 + 12 + len(pairs) * 36 + 10
    rounded_rect(d, (14 * SCALE, y * SCALE, (W - 14) * SCALE, (y + card_h) * SCALE), 10,
                 fill=LEATHER_DARK, outline=(197, 155, 88, 130), width=1)
    d.text((26 * SCALE, (y + 10) * SCALE), "ВАРИАНТЫ СЪЁМКИ", font=f_label, fill=BRASS)
    d.text((26 * SCALE, (y + 26) * SCALE),
           "Реальные пары из шкал Зенит-12сд / Гелиос-44М — нажмите чтобы выбрать",
           font=load_font(8), fill=(232, 223, 196, 150))

    ry = y + 46
    for sh, ap, err, exact in pairs:
        border_col = BRASS if exact else (197, 155, 88, 120)
        row_box = (26 * SCALE, ry * SCALE, (W - 26) * SCALE, (ry + 30) * SCALE)
        rounded_rect(d, row_box, 8, fill=(43, 36, 28, 90), outline=border_col, width=1)
        d.text((36 * SCALE, (ry + 6) * SCALE), sh, font=load_font(15, bold=True, serif=True), fill=CREAM)
        d.text((108 * SCALE, (ry + 8) * SCALE), "×", font=load_font(13, serif=True), fill=BRASS)
        d.text((125 * SCALE, (ry + 6) * SCALE), ap, font=load_font(15, bold=True, serif=True), fill=CREAM)
        # Значок точности
        err_col = BRASS if exact else (232, 223, 196, 180)
        d.text((220 * SCALE, (ry + 9) * SCALE), err, font=f_mono, fill=err_col)
        # Чекмарк-кнопка
        btn_b = (W - 60) * SCALE, (ry + 6) * SCALE, (W - 36) * SCALE, (ry + 24) * SCALE
        d.rounded_rectangle(btn_b, radius=5 * SCALE, outline=BRASS, width=1 * SCALE)
        d.text(((W - 53) * SCALE, (ry + 8) * SCALE), "✓", font=load_font(11, bold=True), fill=BRASS)
        ry += 36

    y += card_h + 10

    # --- Шкала диафрагм (обрезанная) ---
    d.text((14 * SCALE, y * SCALE), "ДИАФРАГМА  f/", font=f_label, fill=BRASS)
    y += 14
    ap_vals = ["1.4", "2", "2.8", "4", "5.6", "8", "11", "16"]
    cx = 18
    for i, v in enumerate(ap_vals):
        is_sel = v == "5.6"
        box = (cx * SCALE, y * SCALE, (cx + 46) * SCALE, (y + 30) * SCALE)
        if is_sel:
            rounded_rect(d, box, 6, fill=BRASS)
            d.text((cx * SCALE + 10 * SCALE, (y + 6) * SCALE), v, font=load_font(13, bold=True, serif=True), fill=LEATHER_DARK)
        else:
            rounded_rect(d, box, 6, fill=(43, 36, 28, 120), outline=(197, 155, 88, 80), width=1)
            d.text((cx * SCALE + 10 * SCALE, (y + 7) * SCALE), v, font=load_font(12, serif=True), fill=CREAM)
        cx += 50

    y += 38

    # --- Пресеты (плёнка/камера/объектив) — три строки ---
    for label, name, sub in [
        ("ПЛЁНКА",   "Kodak Portra 400", "ISO плёнки 400 · негатив, тёплая палитра"),
        ("КАМЕРА",   "Зенит-12сд",       "1/30 · 1/60 · 1/125 · 1/250 · 1/500 · B"),
        ("ОБЪЕКТИВ", "Гелиос-44М (58mm f/2)", "f/2 · 2.8 · 4 · 5.6 · 8 · 11 · 16"),
    ]:
        d.text((14 * SCALE, y * SCALE), label, font=f_label, fill=BRASS)
        d.text((14 * SCALE, (y + 12) * SCALE), name, font=load_font(12, bold=True), fill=CREAM)
        d.text((14 * SCALE, (y + 28) * SCALE), sub, font=load_font(9), fill=(232, 223, 196, 160))
        # Кнопка "Выбрать"
        rounded_rect(d, ((W - 80) * SCALE, (y + 10) * SCALE, (W - 20) * SCALE, (y + 32) * SCALE), 6,
                     outline=BRASS, width=1)
        d.text(((W - 65) * SCALE, (y + 15) * SCALE), "Выбрать", font=load_font(9), fill=BRASS)
        y += 46

    return base.resize((W, H), Image.LANCZOS)


def draw_help_dialog():
    """Диалог справки с таблицей EV и секциями."""
    base = vertical_gradient(W2, H2, LEATHER_DARK, LEATHER_BROWN, LEATHER_DARK)
    # Затемнение фона
    overlay = Image.new("RGBA", (W2, H2), (0, 0, 0, 140))
    base.paste(overlay, (0, 0), overlay)

    d = ImageDraw.Draw(base, "RGBA")

    # Шрифты
    f_dlg_title = load_font(20, bold=True)
    f_section = load_font(13, bold=True)
    f_body = load_font(10)
    f_scene = load_font(10)
    f_ev = load_font(10, mono=True, bold=True)
    f_step_num = load_font(11, bold=True)

    # Рамка диалога
    pad = 16
    dlg = (pad * SCALE, 40 * SCALE, (W - pad) * SCALE, (H - 40) * SCALE)
    rounded_rect(d, dlg, 14, fill=LEATHER_DARK, outline=(197, 155, 88, 180), width=2)

    x0 = pad + 16
    y = 60

    # Заголовок
    d.text((x0 * SCALE, y * SCALE), "СПРАВКА", font=f_dlg_title, fill=BRASS)
    y += 36

    # Секция «Что такое EV»
    d.text((x0 * SCALE, y * SCALE), "Что такое EV", font=f_section, fill=BRASS)
    y += 20
    ev_text = [
        "EV (Exposure Value) — число, которое",
        "одновременно описывает яркость сцены и",
        "настройки камеры. +1 EV = вдвое больше",
        "света, −1 EV = вдвое меньше. Одна",
        "единица = один стоп экспозиции.",
    ]
    for line in ev_text:
        d.text((x0 * SCALE, y * SCALE), line, font=f_body, fill=CREAM)
        y += 14
    y += 8
    d.text((x0 * SCALE, y * SCALE), "Формула: EV = log₂(N² / t)", font=load_font(9), fill=(232, 223, 196, 180))
    y += 20

    # Секция «Таблица EV»
    d.text((x0 * SCALE, y * SCALE), "Таблица EV для типичных сцен", font=f_section, fill=BRASS)
    y += 18
    rows = [
        ("Звёздное небо, Млечный путь",         "−4 … −2"),
        ("Свет полной луны",                     "−2 … 0"),
        ("Ночная улица, витрины",                "3 … 6"),
        ("Комната с лампой",                     "5 … 7"),
        ("Полчаса до/после заката",              "8 … 10"),
        ("Яркий закат / облачно вне дома",       "10 … 12"),
        ("Пасмурный день",                       "12 … 13"),
        ("Лёгкая дымка, солнце в облаках",       "13 … 14"),
        ("Солнечный день (sunny 16)",            "15"),
        ("Снег / пляж на солнце",                "16"),
    ]
    for scene, ev in rows:
        d.text((x0 * SCALE, y * SCALE), scene, font=f_scene, fill=CREAM)
        # EV справа
        ev_w = d.textlength(ev, font=f_ev)
        d.text(((W - pad - 20) * SCALE - ev_w, y * SCALE), ev, font=f_ev, fill=BRASS)
        y += 16
    y += 6
    d.text((x0 * SCALE, y * SCALE), "Поправка ISO: +log₂(ISO/100). ISO 400 → +2.", font=load_font(9),
           fill=(232, 223, 196, 180))
    y += 20

    # Секция «Как пользоваться»
    d.text((x0 * SCALE, y * SCALE), "Как пользоваться", font=f_section, fill=BRASS)
    y += 18
    steps = [
        ("1.", "Выберите плёнку, камеру и объектив."),
        ("2.", "Наведите телефон на сцену — EV"),
        ("",   "появится слева внизу превью."),
        ("3.", "Выберите режим замера (Точка/"),
        ("",   "Центр/Матрица)."),
        ("4.", "Замок рядом с EV фиксирует замер."),
        ("5.", "В блоке «Варианты съёмки»"),
        ("",   "выберите пару выдержка×диафрагма."),
        ("6.", "«точно» — без ошибки; «+0.33 EV»"),
        ("",   "— на 1/3 стопа светлее."),
    ]
    for num, text in steps:
        d.text((x0 * SCALE, y * SCALE), num, font=f_step_num, fill=BRASS)
        d.text(((x0 + 20) * SCALE, y * SCALE), text, font=load_font(10), fill=CREAM)
        y += 15

    y += 8
    # Кнопки снизу
    by = H - 60
    # Настройки
    rounded_rect(d, ((pad + 18) * SCALE, by * SCALE, (pad + 130) * SCALE, (by + 28) * SCALE), 8, fill=BRASS)
    d.text(((pad + 44) * SCALE, (by + 7) * SCALE), "Настройки", font=load_font(11, bold=True), fill=LEATHER_DARK)
    # Закрыть
    rounded_rect(d, ((W - pad - 130) * SCALE, by * SCALE, (W - pad - 18) * SCALE, (by + 28) * SCALE), 8,
                 outline=CREAM, width=1)
    d.text(((W - pad - 94) * SCALE, (by + 7) * SCALE), "Закрыть", font=load_font(11), fill=CREAM)

    return base.resize((W, H), Image.LANCZOS)


def main():
    out = Path("/home/user/workspace/FilmLightMeter/docs/screenshots")
    out.mkdir(parents=True, exist_ok=True)

    main_img = draw_main_screen()
    main_img.save(out / "pairs-card.jpg", quality=88)
    print(f"Saved {out / 'pairs-card.jpg'}")

    help_img = draw_help_dialog()
    help_img.save(out / "help-dialog.jpg", quality=88)
    print(f"Saved {out / 'help-dialog.jpg'}")


if __name__ == "__main__":
    main()
