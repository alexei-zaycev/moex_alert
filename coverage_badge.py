
import xml.etree.ElementTree as ET
from pathlib import Path
from html import escape

REPORT = Path("target/site/jacoco/jacoco.xml")
OUTPUT = Path("coverage.svg")

root = ET.parse(REPORT).getroot()

line_counter = next(
    (c for c in root.findall("counter")
     if c.get("type") == "LINE"),
    None
)

if line_counter is None:
    raise SystemExit("JaCoCo report has no LINE counter")

covered = int(line_counter.get("covered", "0"))
missed = int(line_counter.get("missed", "0"))
total = covered + missed

if total == 0:
    raise SystemExit("No executable lines found in JaCoCo report")

percent = covered * 100 / total
label = "coverage"
value = f"{percent:.1f}%"

if percent >= 80:
    color = "#4c1"
elif percent >= 50:
    color = "#dfb317"
else:
    color = "#e05d44"

label_width = 70
value_width = max(40, len(value) * 7 + 12)
width = label_width + value_width

svg = f"""<svg xmlns="http://www.w3.org/2000/svg"
 width="{width}" height="20" role="img"
 aria-label="{escape(label)}: {escape(value)}">
 <title>{escape(label)}: {escape(value)}</title>
 <linearGradient id="b" x2="0" y2="100%">
  <stop offset="0" stop-color="#bbb" stop-opacity=".1"/>
  <stop offset="1" stop-opacity=".1"/>
 </linearGradient>
 <clipPath id="r"><rect width="{width}" height="20" rx="3"/></clipPath>
 <g clip-path="url(#r)">
  <rect width="{label_width}" height="20" fill="#555"/>
  <rect x="{label_width}" width="{value_width}"
        height="20" fill="{color}"/>
  <rect width="{width}" height="20" fill="url(#b)"/>
 </g>
 <g fill="#fff" text-anchor="middle"
    font-family="Verdana,sans-serif" font-size="11">
  <text x="{label_width / 2}" y="14">{escape(label)}</text>
  <text x="{label_width + value_width / 2}" y="14">{escape(value)}</text>
 </g>
</svg>
"""

OUTPUT.write_text(svg, encoding="utf-8")
print(f"Coverage: {value}; badge saved to {OUTPUT}")
