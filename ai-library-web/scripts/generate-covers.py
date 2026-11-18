# -*- coding: utf-8 -*-
"""批量生成图书封面 SVG。

数据来源：ai-library-server/db/seed-data.sql（解析 INSERT 中的 ISBN/书名/作者/出版社/分类）
输出目录：ai-library-web/public/covers/<isbn>.svg，另生成 default.svg 兜底图。

用法：python scripts/generate-covers.py
"""

import re
import unicodedata
from pathlib import Path

WEB_ROOT = Path(__file__).resolve().parents[1]
REPO_ROOT = WEB_ROOT.parent
SEED_SQL = REPO_ROOT / "ai-library-server" / "db" / "seed-data.sql"
OUT_DIR = WEB_ROOT / "public" / "covers"

W, H = 400, 560
PAD = 46
CONTENT_W = W - PAD * 2
FONT_STACK = "'Microsoft YaHei','PingFang SC','Noto Sans CJK SC','Helvetica Neue',Arial,sans-serif"

# 分类配色：名称 / 底色 / 书脊色 / 点缀色 / 装饰图形
THEMES = {
    1: ("编程开发", "#12305C", "#0B2140", "#4C9AFF", "grid"),
    2: ("人工智能", "#2A1F55", "#1C1440", "#A78BFA", "circle"),
    3: ("文学小说", "#6E2B34", "#521E26", "#F0A07C", "arc"),
    4: ("经济管理", "#1B4638", "#12332A", "#64C08A", "bars"),
    5: ("历史文化", "#5A3E1B", "#422C11", "#DDB35C", "lines"),
    6: ("科幻悬疑", "#0D1524", "#070C16", "#4FD1C5", "dots"),
}
DEFAULT_THEME = ("馆藏图书", "#2F3542", "#232833", "#B8C0D0", "lines")

# 行首禁则：这些字符不能出现在行首；行尾禁则：这些字符不能出现在行尾
CLOSE_PUNCT = "）】》」』”’、，。：；？！…·%,.:;?!)"
OPEN_PUNCT = "（【《「『“‘([{"


def esc(text: str) -> str:
    return (
        text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
        .replace("'", "&apos;")
    )


def char_width(ch: str, size: float) -> float:
    """粗略估算字符宽度：全角按 1em，半角按 0.55em。"""
    return size * (1.0 if unicodedata.east_asian_width(ch) in ("W", "F") else 0.55)


def text_width(text: str, size: float) -> float:
    return sum(char_width(ch, size) for ch in text)


def tokenize(text: str) -> list:
    """切分排版单元：连续的半角字母数字保持整体，避免单词被拆断。"""
    units, buf = [], ""
    for ch in text:
        if ch.isspace():
            if buf:
                units.append(buf)
                buf = ""
            units.append(" ")
        elif unicodedata.east_asian_width(ch) in ("W", "F"):
            if buf:
                units.append(buf)
                buf = ""
            units.append(ch)
        else:
            buf += ch
    if buf:
        units.append(buf)
    return units


def apply_line_break_rules(lines: list) -> list:
    """中文排版禁则：闭合标点不置行首，开启标点不置行尾。"""
    out = []
    for line in lines:
        line = line.lstrip()
        while out and line and line[0] in CLOSE_PUNCT:
            out[-1] += line[0]
            line = line[1:].lstrip()
        while out and out[-1] and out[-1][-1] in OPEN_PUNCT and line:
            line = out[-1][-1] + line
            out[-1] = out[-1][:-1]
        if line:
            out.append(line)
    return out


def wrap(text: str, size: float, max_w: float) -> list:
    units = tokenize(text)
    lines, cur = [], ""
    for u in units:
        if u == " " and not cur:
            continue
        if cur and text_width(cur + u, size) > max_w:
            lines.append(cur.rstrip())
            cur = "" if u == " " else u
        else:
            cur += u
    if cur.strip():
        lines.append(cur.rstrip())
    return apply_line_break_rules(lines)


def pick_layout(title: str) -> tuple:
    """在保证不超过 4 行的前提下挑选最大的字号。"""
    for size in (38, 34, 30, 26, 22, 19):
        lines = wrap(title, size, CONTENT_W)
        if len(lines) <= 4:
            return size, lines
    return 19, wrap(title, 19, CONTENT_W)


def deco(shape: str, accent: str) -> str:
    """右下角低透明度几何装饰，仅用描边/点阵，不抢主体文字。"""
    cx, cy = 318, 412
    if shape == "grid":
        lines = "".join(
            f'<line x1="{cx - 72 + i * 24}" y1="{cy - 72}" x2="{cx - 72 + i * 24}" y2="{cy + 72}"/>'
            for i in range(7)
        ) + "".join(
            f'<line x1="{cx - 72}" y1="{cy - 72 + i * 24}" x2="{cx + 72}" y2="{cy - 72 + i * 24}"/>'
            for i in range(7)
        )
        inner = f'<g stroke="{accent}" stroke-width="1.2" fill="none">{lines}</g>'
    elif shape == "circle":
        circles = "".join(f'<circle cx="{cx}" cy="{cy}" r="{r}"/>' for r in (34, 62, 90))
        inner = f'<g stroke="{accent}" stroke-width="1.4" fill="none">{circles}</g>'
    elif shape == "arc":
        inner = (
            f'<path d="M{cx - 86} {cy + 60} A 100 100 0 0 1 {cx + 54} {cy - 86}" '
            f'stroke="{accent}" stroke-width="1.6" fill="none"/>'
            f'<path d="M{cx - 52} {cy + 60} A 66 66 0 0 1 {cx + 54} {cy - 52}" '
            f'stroke="{accent}" stroke-width="1.6" fill="none"/>'
        )
    elif shape == "bars":
        heights = (44, 78, 58, 104, 70, 92)
        bars = "".join(
            f'<rect x="{cx - 78 + i * 28}" y="{cy + 62 - h}" width="12" height="{h}" rx="3"/>'
            for i, h in enumerate(heights)
        )
        inner = f'<g fill="{accent}">{bars}</g>'
    elif shape == "dots":
        dots = "".join(
            f'<circle cx="{cx - 66 + c * 24}" cy="{cy - 66 + r * 24}" r="3.2"/>'
            for r in range(7)
            for c in range(7)
        )
        inner = f'<g fill="{accent}">{dots}</g>'
    else:  # lines
        lines = "".join(
            f'<line x1="{cx - 84}" y1="{cy - 60 + i * 22}" x2="{cx + 84 - i * 6}" y2="{cy - 60 + i * 22}"/>'
            for i in range(7)
        )
        inner = f'<g stroke="{accent}" stroke-width="1.6" fill="none">{lines}</g>'
    return f'<g opacity="0.13">{inner}</g>'


def build_svg(title: str, author: str, publisher: str, category_id, isbn: str = "") -> str:
    label, bg, deep, accent, shape = THEMES.get(category_id, DEFAULT_THEME)
    size, lines = pick_layout(title)
    line_h = size * 1.38
    block_h = line_h * len(lines)
    block_top = 240 - block_h / 2

    title_svg = "".join(
        f'<text x="{PAD}" y="{block_top + size * 0.85 + i * line_h:.1f}" font-size="{size}" '
        f'font-weight="500" fill="#FFFFFF">{esc(line)}</text>'
        for i, line in enumerate(lines)
    )

    bar_y = block_top + block_h + 22
    author_svg = (
        f'<text x="{PAD}" y="{bar_y + 44:.1f}" font-size="20" fill="#FFFFFF" opacity="0.88">'
        f"{esc(author)}</text>"
        if author
        else ""
    )
    pub_svg = (
        f'<text x="{PAD}" y="506" font-size="15" fill="#FFFFFF" opacity="0.55">{esc(publisher)}</text>'
        if publisher
        else ""
    )
    isbn_svg = (
        f'<text x="{W - PAD}" y="530" font-size="11" fill="#FFFFFF" opacity="0.26" '
        f'text-anchor="end">{esc(isbn)}</text>'
        if isbn
        else ""
    )

    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" font-family="{FONT_STACK}">
<rect width="{W}" height="{H}" fill="{bg}"/>
{deco(shape, accent)}
<rect x="0" y="0" width="14" height="{H}" fill="{deep}"/>
<rect x="14" y="0" width="3" height="{H}" fill="#000000" opacity="0.22"/>
<text x="{PAD}" y="66" font-size="15" fill="{accent}" letter-spacing="4">{esc(label)}</text>
<line x1="{PAD}" y1="82" x2="{W - PAD}" y2="82" stroke="{accent}" stroke-width="1" opacity="0.45"/>
{title_svg}
<rect x="{PAD}" y="{bar_y:.1f}" width="46" height="4" rx="2" fill="{accent}"/>
{author_svg}
{pub_svg}
<text x="{W - PAD}" y="506" font-size="13" fill="#FFFFFF" opacity="0.34" text-anchor="end">AI 智能书屋</text>
{isbn_svg}
</svg>
"""


def parse_books() -> list:
    sql = SEED_SQL.read_text(encoding="utf-8")
    pattern = re.compile(r"\('(\d{10,13})','([^']*)','([^']*)','([^']*)',(\d),")
    books, seen = [], set()
    for m in pattern.finditer(sql):
        isbn, title = m.group(1), m.group(2).strip()
        if isbn in seen:
            continue
        seen.add(isbn)
        books.append((isbn, title, m.group(3).strip(), m.group(4).strip(), int(m.group(5))))
    return books


def main() -> None:
    books = parse_books()
    if not books:
        raise SystemExit(f"未从 {SEED_SQL} 解析到任何书目，请检查文件格式")

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for isbn, title, author, publisher, category_id in books:
        (OUT_DIR / f"{isbn}.svg").write_text(
            build_svg(title, author, publisher, category_id, isbn), encoding="utf-8"
        )
    (OUT_DIR / "default.svg").write_text(
        build_svg("AI 智能书屋", "馆藏图书", "封面待补充", None), encoding="utf-8"
    )
    print(f"已生成 {len(books)} 张封面 + 1 张兜底图 -> {OUT_DIR}")


if __name__ == "__main__":
    main()
