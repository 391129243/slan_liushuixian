# -*- coding: utf-8 -*-
import os, re, glob

DOCS = "docs_异常复位放回修复"
OUT = os.path.join(DOCS, "diagrams")
os.makedirs(OUT, exist_ok=True)

files = [
    "01_时序图_修复前后对比.md",
    "02_状态图_托盘与设备.md",
    "03_泳道结合状态_一图流.md",
]

pat = re.compile(r"```mermaid\s*\n(.*?)```", re.DOTALL)
manifest = []
for f in files:
    path = os.path.join(DOCS, f)
    with open(path, encoding="utf-8") as fh:
        text = fh.read()
    stem = f.split("_")[0]  # 01/02/03
    for i, m in enumerate(pat.finditer(text), 1):
        code = m.group(1).strip() + "\n"
        name = f"{stem}_{i}"
        mmd = os.path.join(OUT, name + ".mmd")
        with open(mmd, "w", encoding="utf-8") as w:
            w.write(code)
        manifest.append(name)
        print("wrote", mmd)

with open(os.path.join(OUT, "manifest.txt"), "w", encoding="utf-8") as w:
    w.write("\n".join(manifest))
print("TOTAL", len(manifest))
