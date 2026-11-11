#!/usr/bin/env python3
"""Gera os assets pixel-art do Truco (vector drawables + padrão do feltro).

Uso (na raiz do projeto):
    python3 tools/gen_assets.py app/src/main/res /tmp/previews

Saídas em <res>/drawable:
    ic_carta_{1,3,6,9,12}.xml   sprite + valor do ponto (viewport 22x30)
    bg_carta_*.xml, bg_botao_*  fundos com "lábio" (face + sombra sólida)
    ic_zerar.xml, ic_historico.xml
    logo_truco.xml              logo do menu
    ic_fogo_{1,2,3}.xml         fogo da sequência de vitórias (3 níveis)
e em <res>/drawable-nodpi/felt_pattern.png (losangos do feltro).
Também grava PNGs ampliados em <previews>/ para conferir o desenho.

Sem dependências externas. Os sprites ficam em tools/pixel_sprites.py; para mudar
um desenho, edite lá e rode de novo (não edite os XMLs gerados à mão).
"""
import math
import os
import struct
import sys
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pixel_sprites as ps  # noqa: E402

RES = sys.argv[1]
PREVIEWS = sys.argv[2]
PAL = ps.PAL


# ---------------------------------------------------------------- vetor
def vector_xml(cells, w, h, note="Gerado por tools/gen_assets.py. Não editar à mão.", size_dp=None, base=None):
    by_color = {}
    for (x, y), key in cells.items():
        by_color.setdefault(key, []).append((x, y))
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        "<!-- %s -->" % note,
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="%ddp"' % (size_dp or w),
        '    android:height="%ddp"' % (size_dp or h),
        '    android:viewportWidth="%d"' % w,
        '    android:viewportHeight="%d">' % h,
    ]
    if base:
        lines.append('    <path android:fillColor="%s" android:pathData="M0,0h%dv%dh-%dz" />' % (PAL[base], w, h, w))
    for key in sorted(by_color):
        pts = sorted(by_color[key], key=lambda p: (p[1], p[0]))
        runs = []
        for (x, y) in pts:
            if runs and runs[-1][1] == y and runs[-1][0] + runs[-1][2] == x:
                runs[-1][2] += 1
            else:
                runs.append([x, y, 1])
        d = " ".join("M%d,%dh%dv1h-%dz" % (x, y, rw, rw) for x, y, rw in runs)
        lines.append('    <path android:fillColor="%s" android:pathData="%s" />' % (PAL[key], d))
    lines.append("</vector>")
    return "\n".join(lines) + "\n"


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(text)


# ---------------------------------------------------------------- cartões
CW, CH = 22, 30          # viewport do cartão
GLYPH = {
    "+": ["000", "010", "111", "010", "000"],
    "0": ["111", "101", "101", "101", "111"],
    "1": ["010", "110", "010", "010", "111"],
    "2": ["111", "001", "111", "100", "111"],
    "3": ["111", "001", "111", "001", "111"],
    "6": ["111", "100", "111", "101", "111"],
    "9": ["111", "101", "111", "001", "111"],
}


def number_cells(text, cx, top):
    on = set()
    x = 0
    for i, ch in enumerate(text):
        for ry, row in enumerate(GLYPH[ch]):
            for rx, v in enumerate(row):
                if v == "1":
                    on.add((x + rx, ry))
        x += 3 + (2 if i < len(text) - 1 else 0)
    width = x
    tx = cx - width // 2
    on = {(tx + px, top + py) for (px, py) in on}
    cells = {}
    for (px, py) in on:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if (px + dx, py + dy) not in on:
                    cells[(px + dx, py + dy)] = "ink"
    for p in on:
        cells[p] = "cream"
    return cells


def card_cells(value):
    cells = {}
    icon = ps.card_icon(value)                      # 18x18
    for (x, y), k in icon.items():
        cells[(x + 2, y + 2)] = k
    cells.update(number_cells("+%d" % value, CW // 2, 22))
    return cells


CARDS = {
    1: ("#3FA0FF", "#1C5A9E"),
    3: ("#3DBB8A", "#237A57"),
    6: ("#F28A2E", "#A8561A"),
    9: ("#E8453C", "#A32A24"),
    12: ("#F0B940", "#A87B1B"),
}
BUTTONS = {
    "vermelho": ("#E8453C", "#A32A24"),
    "azul": ("#3FA0FF", "#1C5A9E"),
    "dourado": ("#F0B940", "#A87B1B"),
    "verde": ("#3DBB8A", "#237A57"),
    "cinza": ("#4A5C5D", "#1B2627"),
}


# ---------------------------------------------------------------- caixas pixeladas (cantos em degrau)
def _rect(color, l, t_, r, b_):
    return ('        <item android:left="%gdp" android:top="%gdp" android:right="%gdp" android:bottom="%gdp">\n'
            '            <shape android:shape="rectangle"><solid android:color="%s" /></shape>\n'
            '        </item>\n') % (l, t_, r, b_, color)


def box_items(face, border, p, dt=0, db=0):
    """Caixa com borda de 1 'pixel' (p dp) e canto em 2 degraus, só com retângulos sobrepostos.
    dt/db = deslocamento extra no topo/base (para empilhar a sombra/"lábio")."""
    out = ""
    for (l, t_, r, b_) in ((2 * p, 0, 2 * p, 0), (p, p, p, p), (0, 2 * p, 0, 2 * p)):
        out += _rect(border, l, t_ + dt, r, b_ + db)
    if face:
        for (l, t_, r, b_) in ((2 * p, p, 2 * p, p), (p, 2 * p, p, 2 * p)):
            out += _rect(face, l, t_ + dt, r, b_ + db)
    return out


def layered(face, border, lip, p, lip_h, press=0):
    """Camada de baixo (lábio) + face. press>0: face desce 'press' dp e o lábio some."""
    body = ""
    if lip_h:
        body += box_items(lip, "@color/ink", p, dt=lip_h)
    body += box_items(face, border, p, dt=press, db=lip_h - press if lip_h else 0)
    return "    <layer-list>\n%s    </layer-list>\n" % body


def write_static(name, face, border, lip, p, lip_h):
    xml = ('<?xml version="1.0" encoding="utf-8"?>\n'
           '<!-- Gerado por tools/gen_assets.py: caixa pixelada (canto em degrau). -->\n'
           '<layer-list xmlns:android="http://schemas.android.com/apk/res/android">\n')
    if lip_h:
        xml += box_items(lip, "@color/ink", p, dt=lip_h)
    xml += box_items(face, border, p, db=lip_h)
    xml += "</layer-list>\n"
    write(os.path.join(RES, "drawable", name + ".xml"), xml)


def write_press(name, face, lip, p, lip_h, border="@color/ink"):
    inner = lambda press: ("".join(box_items(lip, "@color/ink", p, dt=lip_h)) if lip_h else "") + \
        box_items(face, border, p, dt=press, db=lip_h - press)
    xml = ('<?xml version="1.0" encoding="utf-8"?>\n'
           '<!-- Gerado por tools/gen_assets.py: botão pixelado. Pressionado, a face desce e o lábio some. -->\n'
           '<selector xmlns:android="http://schemas.android.com/apk/res/android">\n'
           '    <item android:state_pressed="true">\n'
           '    <layer-list>\n%s    </layer-list>\n    </item>\n'
           '    <item>\n    <layer-list>\n%s    </layer-list>\n    </item>\n'
           '</selector>\n') % (inner(3), inner(0))
    write(os.path.join(RES, "drawable", name + ".xml"), xml)


def write_input(name, cor, cor_escura):
    def caixa(border):
        return "    <layer-list>\n%s    </layer-list>\n" % box_items("@color/panel_dark", border, 3)
    xml = ('<?xml version="1.0" encoding="utf-8"?>\n'
           '<!-- Gerado: campo de texto pixelado. Foco = cor viva; activated = erro. -->\n'
           '<selector xmlns:android="http://schemas.android.com/apk/res/android">\n'
           '    <item android:state_activated="true">\n%s    </item>\n'
           '    <item android:state_focused="true">\n%s    </item>\n'
           '    <item>\n%s    </item>\n'
           '</selector>\n') % (caixa("@color/error_text"), caixa(cor), caixa(cor_escura))
    write(os.path.join(RES, "drawable", name + ".xml"), xml)


# ---------------------------------------------------------------- ícones de ação (polígonos; já aprovados)
S, SS, OFF = 18, 4, 1.0


def circle(cx, cy, r):
    return lambda x, y: (x - cx) ** 2 + (y - cy) ** 2 <= r * r


def ring(cx, cy, r0, r1):
    return lambda x, y: r0 * r0 <= (x - cx) ** 2 + (y - cy) ** 2 <= r1 * r1


def rect(x0, y0, x1, y1):
    return lambda x, y: x0 <= x <= x1 and y0 <= y <= y1


def poly(pts):
    def inside(x, y):
        c, n, j = False, len(pts), len(pts) - 1
        for i in range(n):
            xi, yi = pts[i]
            xj, yj = pts[j]
            if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
                c = not c
            j = i
        return c

    return inside


def ring_arc(cx, cy, r0, r1, a0, a1):
    def inside(x, y):
        d2 = (x - cx) ** 2 + (y - cy) ** 2
        if not (r0 * r0 <= d2 <= r1 * r1):
            return False
        ang = math.degrees(math.atan2(y - cy, x - cx)) % 360
        lo, hi = a0 % 360, a1 % 360
        return (lo <= ang <= hi) if lo <= hi else (ang >= lo or ang <= hi)

    return inside


def rasterize(layers):
    grid = [[None] * S for _ in range(S)]
    for key, fn in layers:
        for py in range(S):
            for px in range(S):
                hits = sum(
                    1
                    for sy in range(SS)
                    for sx in range(SS)
                    if fn(px + (sx + 0.5) / SS - OFF, py + (sy + 0.5) / SS - OFF)
                )
                if hits >= SS * SS * 0.5:
                    grid[py][px] = key
    cells = {(x, y): grid[y][x] for y in range(S) for x in range(S) if grid[y][x]}
    return ps.outline(cells)


def icon_zerar():
    cells = ps.center_in(ps.sprite_zerar(), 16)
    return ps.outline(ps.shift(cells, 1, 1))


def icon_historico():
    cup = poly([(4.4, 2.2), (11.6, 2.2), (11.0, 7.0), (9.6, 9.4), (6.4, 9.4), (5.0, 7.0)])
    return rasterize([
        ("gold", cup),
        ("gold", ring(3.4, 5.0, 1.1, 2.3)),
        ("gold", ring(12.6, 5.0, 1.1, 2.3)),
        ("goldLight", rect(5.6, 3.0, 6.7, 6.2)),
        ("goldDark", rect(7.1, 9.4, 8.9, 12.2)),
        ("goldDark", rect(4.6, 12.0, 11.4, 14.0)),
    ])


# ---------------------------------------------------------------- PNG (prévias e padrão do feltro)
def png_bytes(rows, w, h, color_type):
    raw = b"".join(b"\x00" + bytes(c for px in row for c in px) for row in rows)

    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, color_type, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def hex_rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def sheet(path, items, scale=12, pad=2, gap=2):
    """items: lista de (cells, w, h, cor_de_fundo). Uma imagem lado a lado."""
    total_w = sum((w + 2 * pad) * scale + gap * scale for _, w, _, _ in items)
    max_h = max((h + 2 * pad) * scale for _, _, h, _ in items)
    img = [[(18, 40, 34)] * total_w for _ in range(max_h)]
    x_off = 0
    for cells, w, h, bg in items:
        bgc = hex_rgb(bg)
        for y in range((h + 2 * pad) * scale):
            for x in range((w + 2 * pad) * scale):
                gx, gy = x // scale - pad, y // scale - pad
                if 0 <= gx < w and 0 <= gy < h:
                    img[y][x_off + x] = bgc
        for (px, py), key in cells.items():
            if 0 <= px < w and 0 <= py < h:
                c = hex_rgb(PAL[key])
                for dy in range(scale):
                    for dx in range(scale):
                        img[(py + pad) * scale + dy][x_off + (px + pad) * scale + dx] = c
        x_off += ((w + 2 * pad) + gap) * scale
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png_bytes(img, total_w, max_h, 2))


def felt_pattern(path, cell=14, px=6):
    n, c = cell, cell / 2.0
    mask = [[abs(abs(x + 0.5 - c) + abs(y + 0.5 - c) - n / 2.0 + 0.5) < 0.9 for x in range(n)] for y in range(n)]
    rows = [[(255, 255, 255, 14) if mask[y // px][x // px] else (0, 0, 0, 0)
             for x in range(n * px)] for y in range(n * px)]
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png_bytes(rows, n * px, n * px, 6))


# ---------------------------------------------------------------- main
def main():
    drawable = os.path.join(RES, "drawable")
    cards = []
    for value, (face, lip) in CARDS.items():
        cells = card_cells(value)
        write(os.path.join(drawable, "ic_carta_%d.xml" % value), vector_xml(cells, CW, CH))
        write_press("bg_carta_%d" % value, face, lip, 3, 4)
        cards.append((cells, CW, CH, face))
    sheet(os.path.join(PREVIEWS, "cartas.png"), cards)

    for name, fn in (("zerar", icon_zerar), ("historico", icon_historico)):
        cells = fn()
        write(os.path.join(drawable, "ic_%s.xml" % name), vector_xml(cells, S, S))
    sheet(os.path.join(PREVIEWS, "icones.png"),
          [(icon_zerar(), S, S, "#4A5C5D"), (icon_historico(), S, S, "#4A5C5D")], scale=14)

    for name, (face, lip) in BUTTONS.items():
        write_press("bg_botao_" + name, face, lip, 3, 4)

    # caixas estáticas
    write_static("bg_panel", "@color/panel", "@color/ink", "@color/ink", 4, 5)
    write_static("bg_placar_azul", "@color/panel_dark", "@color/blue", "@color/ink", 4, 6)
    write_static("bg_placar_vermelho", "@color/panel_dark", "@color/red", "@color/ink", 4, 6)
    write_static("bg_tag_azul", "@color/blue", "@color/ink", "@color/blue_dark", 2, 3)
    write_static("bg_tag_vermelho", "@color/red", "@color/ink", "@color/red_dark", 2, 3)
    write_static("bg_badge", "@color/gold", "@color/ink", "@color/gold_dark", 2, 3)
    write_static("bg_item_historico", "@color/panel", "@color/ink", "@color/ink", 3, 5)
    write_static("bg_franja", "@color/felt_strip", "@color/ink", None, 3, 0)
    write_static("bg_dialogo", "@color/panel", "@color/gold", "@color/ink", 4, 6)
    write_static("bg_toast", "@color/panel_dark", "@color/gold", "@color/ink", 3, 5)
    write_input("bg_input_j1", "@color/blue", "@color/blue_dark")
    write_input("bg_input_j2", "@color/red", "@color/red_dark")

    logo = ps.logo()
    write(os.path.join(drawable, "logo_truco.xml"), vector_xml(logo, ps.LOGO_W, ps.LOGO_H))
    sheet(os.path.join(PREVIEWS, "logo.png"), [(logo, ps.LOGO_W, ps.LOGO_H, "#14473D")], scale=10)

    fires = []
    for level in (1, 2, 3):
        cells = ps.fire_sprite(level)
        write(os.path.join(drawable, "ic_fogo_%d.xml" % level), vector_xml(cells, 12, 16))
        fires.append((cells, 12, 16, "#0B2B26"))
    sheet(os.path.join(PREVIEWS, "fogo.png"), fires, scale=14)

    # ---- ícone do app
    fg = ps.launcher_foreground()
    write(os.path.join(drawable, "ic_launcher_foreground.xml"), vector_xml(fg, ps.ICON_VP, ps.ICON_VP, size_dp=108))
    # monocromático: contorno das cartas + naipes + coroa (o miolo creme das cartas fica vazado)
    mono = {k: "mono" for k, v in fg.items() if v not in ("cream", "boneDark")}
    write(os.path.join(drawable, "ic_launcher_monochrome.xml"), vector_xml(mono, ps.ICON_VP, ps.ICON_VP, size_dp=108))
    bg = ps.launcher_lattice()
    write(os.path.join(drawable, "ic_launcher_background.xml"),
          vector_xml(bg, ps.ICON_VP, ps.ICON_VP, size_dp=108, base="feltIcon"))
    preview = dict(bg)
    preview.update(fg)
    sheet(os.path.join(PREVIEWS, "icone_app.png"),
          [(preview, ps.ICON_VP, ps.ICON_VP, "#14473D"), (mono, ps.ICON_VP, ps.ICON_VP, "#cccccc")], scale=6)

    felt_pattern(os.path.join(RES, "drawable-nodpi", "felt_pattern.png"))
    print("ok")


main()
