# -*- coding: utf-8 -*-
"""生成「机械臂调度逻辑与 UI 解耦重构」总结 PPT。"""
from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE

NAVY   = RGBColor(0x1F, 0x2A, 0x44)
BLUE   = RGBColor(0x2E, 0x6F, 0xF2)
CYAN   = RGBColor(0x18, 0xB0, 0xC4)
GREEN  = RGBColor(0x27, 0xAE, 0x60)
RED    = RGBColor(0xE7, 0x4C, 0x3C)
AMBER  = RGBColor(0xF2, 0x9D, 0x38)
GREY   = RGBColor(0x5A, 0x63, 0x72)
LGREY  = RGBColor(0xEC, 0xEF, 0xF4)
WHITE  = RGBColor(0xFF, 0xFF, 0xFF)
DARK   = RGBColor(0x22, 0x2A, 0x38)
FONT = "Microsoft YaHei"

prs = Presentation()
prs.slide_width  = Inches(13.333)
prs.slide_height = Inches(7.5)
SW, SH = prs.slide_width, prs.slide_height
BLANK = prs.slide_layouts[6]

def slide():
    return prs.slides.add_slide(BLANK)

def rect(s, x, y, w, h, fill, line=None, line_w=None):
    sp = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, x, y, w, h)
    sp.fill.solid(); sp.fill.fore_color.rgb = fill
    if line is None: sp.line.fill.background()
    else: sp.line.color.rgb = line; sp.line.width = line_w or Pt(1)
    sp.shadow.inherit = False
    return sp

def rrect(s, x, y, w, h, fill, line=None):
    sp = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, y, w, h)
    sp.fill.solid(); sp.fill.fore_color.rgb = fill
    if line is None: sp.line.fill.background()
    else: sp.line.color.rgb = line; sp.line.width = Pt(1.25)
    sp.shadow.inherit = False
    try: sp.adjustments[0] = 0.08
    except Exception: pass
    return sp

def txt(s, x, y, w, h, runs, align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.TOP, space_after=4, line_spacing=1.05):
    tb = s.shapes.add_textbox(x, y, w, h)
    tf = tb.text_frame; tf.word_wrap = True
    tf.vertical_anchor = anchor
    tf.margin_left = tf.margin_right = Pt(2); tf.margin_top = tf.margin_bottom = Pt(1)
    for i, para in enumerate(runs):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = align; p.space_after = Pt(space_after); p.space_before = Pt(0); p.line_spacing = line_spacing
        for (t, sz, col, bold) in para:
            r = p.add_run(); r.text = t
            r.font.size = Pt(sz); r.font.color.rgb = col; r.font.bold = bold; r.font.name = FONT
    return tb

def bullet(s, x, y, w, h, items, size=15, col=DARK, gap=7, mark="▪ ", mark_col=BLUE):
    tb = s.shapes.add_textbox(x, y, w, h)
    tf = tb.text_frame; tf.word_wrap = True
    for i, it in enumerate(items):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.space_after = Pt(gap); p.line_spacing = 1.05
        r = p.add_run(); r.text = mark; r.font.size = Pt(size); r.font.color.rgb = mark_col; r.font.bold = True; r.font.name = FONT
        r = p.add_run(); r.text = it; r.font.size = Pt(size); r.font.color.rgb = col; r.font.name = FONT
    return tb

def header(s, no, title, sub=None):
    rect(s, 0, 0, SW, Inches(1.15), NAVY)
    rect(s, 0, Inches(1.15), SW, Pt(4), CYAN)
    txt(s, Inches(0.55), Inches(0.12), Inches(1.1), Inches(0.9), [[(no, 34, CYAN, True)]], anchor=MSO_ANCHOR.MIDDLE)
    runs = [[(title, 25, WHITE, True)]]
    if sub: runs.append([(sub, 13, RGBColor(0xB9,0xC6,0xDC), False)])
    txt(s, Inches(1.5), Inches(0.14), Inches(11.4), Inches(0.9), runs, anchor=MSO_ANCHOR.MIDDLE)

def footer(s, n):
    txt(s, Inches(11.3), Inches(7.02), Inches(1.9), Inches(0.4),
        [[("机械臂调度重构  ·  " + str(n), 9, GREY, False)]], align=PP_ALIGN.RIGHT)

# 1 封面
s = slide()
rect(s, 0, 0, SW, SH, NAVY)
rect(s, 0, Inches(2.7), SW, Pt(3), CYAN)
rect(s, Inches(0.9), Inches(3.05), Inches(2.2), Pt(5), BLUE)
txt(s, Inches(0.9), Inches(1.5), Inches(11.5), Inches(1.4),
    [[("机械臂调度逻辑", 44, WHITE, True)], [("与 UI (databinding) 解耦重构", 40, CYAN, True)]], line_spacing=1.0)
txt(s, Inches(0.95), Inches(3.35), Inches(11.6), Inches(1.2),
    [[("APLManager 调度状态机 → databinding-free 引擎 · 端口回调式 · 52 单测锁定行为", 17, RGBColor(0xC7,0xD3,0xE8), False)]])
metrics = [("5", "重构链路"), ("68", "单元测试"), ("10", "新增核心类"), ("0", "行为回归")]
mx = Inches(0.95); mw = Inches(2.7); gap = Inches(0.25)
for v, lab in metrics:
    rrect(s, mx, Inches(5.1), mw, Inches(1.25), RGBColor(0x2A,0x37,0x55))
    txt(s, mx, Inches(5.2), mw, Inches(0.7), [[(v, 30, CYAN, True)]], align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    txt(s, mx, Inches(5.85), mw, Inches(0.4), [[(lab, 13, WHITE, False)]], align=PP_ALIGN.CENTER)
    mx = Emu(int(mx) + int(mw) + int(gap))
txt(s, Inches(0.9), Inches(6.75), Inches(11), Inches(0.4),
    [[("PCRinOne · Bio_PCR_liushuixian_hongshi", 12, RGBColor(0x8C,0x9A,0xB5), False)]])

# 2 背景
s = slide(); header(s, "01", "背景与痛点", "为什么要重构")
txt(s, Inches(0.55), Inches(1.45), Inches(12), Inches(0.7),
    [[("APLManager 是调度总管，但调度状态机（checkrnawork / checkpcrwork / checktray*）", 15, DARK, False)],
     [("直接读写 databinding 的 UI 模型（ObservableField），造成三大问题：", 15, DARK, False)]], line_spacing=1.15)
cards = [("耦合 UI 框架", "编译期强依赖 androidx.databinding，调度逻辑无法脱离 Android 运行", RED),
         ("无法单测", "决策逻辑埋在 UI 模型读写里，纯 JVM 下跑不起来，回归全靠真机", AMBER),
         ("状态可被乱改", "UI 层能随手改调度状态字段，线程读写归属混乱，隐患难查", RED)]
cx = Inches(0.55); cw = Inches(4.0); cgap = Inches(0.18)
for title, desc, col in cards:
    rrect(s, cx, Inches(2.5), cw, Inches(2.6), LGREY)
    rect(s, cx, Inches(2.5), Inches(0.12), Inches(2.6), col)
    txt(s, Emu(int(cx)+int(Inches(0.35))), Inches(2.75), Emu(int(cw)-int(Inches(0.5))), Inches(0.6), [[(title, 19, col, True)]])
    txt(s, Emu(int(cx)+int(Inches(0.35))), Inches(3.45), Emu(int(cw)-int(Inches(0.5))), Inches(1.5), [[(desc, 14, GREY, False)]], line_spacing=1.2)
    cx = Emu(int(cx) + int(cw) + int(cgap))
rrect(s, Inches(0.55), Inches(5.45), Inches(12.25), Inches(1.35), NAVY)
txt(s, Inches(0.85), Inches(5.6), Inches(11.7), Inches(1.1),
    [[("目标：", 16, CYAN, True), ("把调度决策逻辑抽到不依赖 databinding 的 SchedulingEngine，", 16, WHITE, False)],
     [("只依赖端口接口 + 纯 POJO/枚举，用 JUnit + Mockito 锁定行为，UI 与业务彻底分离。", 16, WHITE, False)]],
    anchor=MSO_ANCHOR.MIDDLE, line_spacing=1.15)
footer(s, 2)

# 3 架构对比
s = slide(); header(s, "02", "架构：解耦前 → 解耦后")
rrect(s, Inches(0.55), Inches(1.5), Inches(5.9), Inches(5.4), LGREY)
txt(s, Inches(0.55), Inches(1.65), Inches(5.9), Inches(0.5), [[("解耦前", 20, RED, True)]], align=PP_ALIGN.CENTER)
b1 = [("UI Fragment / XML", WHITE, BLUE), ("↓  直接读写 ObservableField", None, None),
      ("PipeRNAModel / PipePCRModel", WHITE, CYAN), ("↕  状态字段混用", None, None),
      ("APLManager 调度状态机", WHITE, RED), ("（决策 + databinding + 命令 混在一起）", None, None)]
yy = Inches(2.25)
for t, fg, bg in b1:
    if bg is None:
        txt(s, Inches(0.75), yy, Inches(5.5), Inches(0.4), [[(t, 13, GREY, False)]], align=PP_ALIGN.CENTER)
        yy = Emu(int(yy)+int(Inches(0.5)))
    else:
        rrect(s, Inches(1.15), yy, Inches(4.7), Inches(0.62), bg)
        txt(s, Inches(1.15), yy, Inches(4.7), Inches(0.62), [[(t, 14, fg, True)]], align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
        yy = Emu(int(yy)+int(Inches(0.78)))
txt(s, Inches(6.5), Inches(3.7), Inches(0.55), Inches(0.8), [[("➜", 40, CYAN, True)]], align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
rrect(s, Inches(7.0), Inches(1.5), Inches(5.85), Inches(5.4), RGBColor(0xEA,0xF6,0xEE))
txt(s, Inches(7.0), Inches(1.65), Inches(5.85), Inches(0.5), [[("解耦后", 20, GREEN, True)]], align=PP_ALIGN.CENTER)
b2 = [("UI Fragment / XML", WHITE, BLUE), ("↓", None, None),
      ("PipeRNA/PCR/TrayModel（UI 模型）", WHITE, CYAN), ("↓  适配层 AplSchedulingPort", None, None),
      ("SchedulingPort（端口接口）", WHITE, AMBER), ("↓  POJO 快照 / 意图回调", None, None),
      ("SchedulingEngine（databinding-free）", WHITE, GREEN)]
yy = Inches(2.2)
for t, fg, bg in b2:
    if bg is None:
        txt(s, Inches(7.2), yy, Inches(5.45), Inches(0.36), [[(t, 12, GREY, False)]], align=PP_ALIGN.CENTER)
        yy = Emu(int(yy)+int(Inches(0.44)))
    else:
        rrect(s, Inches(7.5), yy, Inches(4.85), Inches(0.58), bg)
        txt(s, Inches(7.5), yy, Inches(4.85), Inches(0.58), [[(t, 13, fg, True)]], align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
        yy = Emu(int(yy)+int(Inches(0.72)))
footer(s, 3)

# 4 分层
s = slide(); header(s, "03", "分层设计", "引擎只发意图，副作用留适配层")
layers = [("SchedulingEngine", "纯决策状态机：switch(armStep) 复刻原逻辑逐行等价；无 android/androidx/manager import，可纯 JVM 单测", GREEN),
          ("SchedulingPort", "端口接口：门控读 / 查询 / 命令 / 快照 / log 钩子；约 40 个方法", AMBER),
          ("AplSchedulingPort", "APLManager 内部类，把端口调用 1:1 适配到现有 Manager / 门状态 / 回调 / createArm* 命令", CYAN),
          ("POJO 快照", "RnaSchedulingState / PcrSchedulingState / TrayDispatchSnapshot / DeviceRuntimeState / ArmStep 枚举", BLUE)]
yy = Inches(1.55)
for name, desc, col in layers:
    rrect(s, Inches(0.55), yy, Inches(12.25), Inches(1.15), LGREY)
    rect(s, Inches(0.55), yy, Inches(0.14), Inches(1.15), col)
    txt(s, Inches(0.95), yy, Inches(3.4), Inches(1.15), [[(name, 18, col, True)]], anchor=MSO_ANCHOR.MIDDLE)
    txt(s, Inches(4.4), yy, Inches(8.2), Inches(1.15), [[(desc, 13.5, GREY, False)]], anchor=MSO_ANCHOR.MIDDLE, line_spacing=1.15)
    yy = Emu(int(yy)+int(Inches(1.32)))
footer(s, 4)

# 5 四步
s = slide(); header(s, "04", "分四步渐进实施", "每步可编译可测 · 不动 UI")
steps = [("第 1 步", "字段枚举/POJO 化", "armStep → ArmStep 枚举（Gson int-adapter 保存档兼容）；11 个运行态布尔收进 DeviceRuntimeState", BLUE),
         ("第 2 步", "RNA 单链路试点", "抽出 SchedulingEngine + SchedulingPort + RnaSchedulingState；14 单测锁定 RNA 状态机", CYAN),
         ("第 3 步", "PCR 并行推广", "stepPcr 与 stepRna 并列；复刻 PCR 差异（空 tray 守卫 / MOVE 找空闲 RNA 方向相反）；12 单测", GREEN),
         ("第 4 步", "Tray 编排层全量抽出", "checktrayurgent/error/end/Rnawork/Pcrwork 6 方法端口回调式抽出；26 单测含 InOrder 时序", AMBER)]
yy = Inches(1.55)
for tag, title, desc, col in steps:
    rrect(s, Inches(0.55), yy, Inches(12.25), Inches(1.2), WHITE, line=col)
    rrect(s, Inches(0.75), yy, Inches(1.7), Inches(1.2), col)
    txt(s, Inches(0.75), yy, Inches(1.7), Inches(1.2), [[(tag, 17, WHITE, True)]], align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    txt(s, Inches(2.7), Emu(int(yy)+int(Inches(0.12))), Inches(10), Inches(0.5), [[(title, 17, DARK, True)]])
    txt(s, Inches(2.7), Emu(int(yy)+int(Inches(0.6))), Inches(9.9), Inches(0.55), [[(desc, 12.5, GREY, False)]], line_spacing=1.1)
    yy = Emu(int(yy)+int(Inches(1.36)))
footer(s, 5)

# 6 端口回调式
s = slide(); header(s, "05", "核心设计：端口回调式", "为什么不做整盘快照")
txt(s, Inches(0.55), Inches(1.4), Inches(12.3), Inches(0.9),
    [[("Tray 编排层跨 tray + dispatchlist + 设备模型同时读写，还夹着 LED / saveModel / 委托设备等副作用。", 14.5, DARK, False)],
     [("若做整盘快照 + 副作用回放，需把「LED / 存盘 / 状态推进」的精确时序编码进 replay，风险极高。", 14.5, DARK, False)]], line_spacing=1.2)
colL = Inches(0.55); colR = Inches(6.75); cw = Inches(6.05)
rrect(s, colL, Inches(3.05), cw, Inches(3.7), RGBColor(0xEA,0xF6,0xEE))
txt(s, colL, Inches(3.2), cw, Inches(0.5), [[("✓ 采用：端口回调式", 18, GREEN, True)]], align=PP_ALIGN.CENTER)
bullet(s, Emu(int(colL)+int(Inches(0.35))), Inches(3.85), Emu(int(cw)-int(Inches(0.6))), Inches(2.8),
    ["引擎自己驱动遍历，读/写/分配/委托/副作用全经 port 回调", "适配层对真 ObservableField 按原顺序执行",
     "副作用拆成独立原子 port 方法，杜绝时序漂移", "设备委托复用已引擎化的 checkrnawork/pcrwork"],
    size=13.5, mark_col=GREEN, gap=9)
rrect(s, colR, Inches(3.05), cw, Inches(3.7), RGBColor(0xFB,0xEC,0xEC))
txt(s, colR, Inches(3.2), cw, Inches(0.5), [[("✗ 放弃：整盘快照 + 回放", 18, RED, True)]], align=PP_ALIGN.CENTER)
bullet(s, Emu(int(colR)+int(Inches(0.35))), Inches(3.85), Emu(int(cw)-int(Inches(0.6))), Inches(2.8),
    ["需编码「pass2 仅在 pass1 未中止时跑」等控制流", "LED / 存盘 / 状态推进的精确顺序必须完整重放",
     "巨大 replay 面，极易 reorder 承重时序", "快照对象庞大（tray + dispatchlist + 设备）"],
    size=13.5, mark="✗ ", mark_col=RED, gap=9)
footer(s, 6)

# 7 承重点
s = slide(); header(s, "06", "行为保真承重点", "逐一被单测锁定，禁止归一化")
rows = [("返回值语义", "默认 true(空闲)；仅 GET/MOVE/BACK 成功下发命令返回 false —— 靠它短路整轮调度"),
        ("BACK 门控不对称", "BACK 只判 WSSStart，不含 SelfCheck（刻意保留历史行为，单测锁定）"),
        ("MOVE 无空闲设备回退", "找不到空闲对侧设备 → 清 backtray 标志 + armStep=BACK + 返回 true"),
        ("checktrayerror 无参不对称", "内层中止返回 true（跳过整轮）；异常 try/catch 吞掉返回 false"),
        ("checktrayend 时序", "推进分支不关 LED；saveModel 落在 if/else 之后；先设备写回再标 FINISH"),
        ("armStep 存档兼容", "枚举经 Gson int-adapter 读写，旧存档覆盖安装续跑，二进制兼容")]
yy = Inches(1.5)
for i,(k,v) in enumerate(rows):
    bg = LGREY if i%2==0 else WHITE
    rrect(s, Inches(0.55), yy, Inches(12.25), Inches(0.82), bg)
    txt(s, Inches(0.8), yy, Inches(3.5), Inches(0.82), [[(k, 14.5, BLUE, True)]], anchor=MSO_ANCHOR.MIDDLE)
    txt(s, Inches(4.35), yy, Inches(8.3), Inches(0.82), [[(v, 12.5, GREY, False)]], anchor=MSO_ANCHOR.MIDDLE, line_spacing=1.1)
    yy = Emu(int(yy)+int(Inches(0.9)))
footer(s, 7)

# 8 单测
s = slide(); header(s, "07", "单元测试覆盖", "纯 JVM · Mockito mock 端口")
tests = [("RNA", "14", CYAN), ("PCR", "12", GREEN), ("Tray", "26", AMBER), ("Arm", "16", RED), ("合计", "68", BLUE)]
tx = Inches(0.55); tw = Inches(2.32); tgap = Inches(0.16)
for lab, v, col in tests:
    rrect(s, tx, Inches(1.55), tw, Inches(1.85), col)
    txt(s, tx, Inches(1.75), tw, Inches(0.95), [[(v, 46, WHITE, True)]], align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    txt(s, tx, Inches(2.75), tw, Inches(0.5), [[(lab + "  用例", 15, WHITE, True)]], align=PP_ALIGN.CENTER)
    tx = Emu(int(tx)+int(tw)+int(tgap))
txt(s, Inches(0.55), Inches(3.75), Inches(12), Inches(0.5), [[("关键锁定项", 18, DARK, True)]])
bullet(s, Inches(0.7), Inches(4.3), Inches(6.0), Inches(2.6),
    ["GET→busy / 命令失败→idle / doorHold 拦截", "MOVE 有空闲→false + markFinish",
     "MOVE 无空闲→armStep=BACK 且返回 true", "BACK 仅需 WSSStart（SelfCheck=false 仍下发）"], size=13.5, gap=10)
bullet(s, Inches(6.9), Inches(4.3), Inches(6.0), Inches(2.6),
    ["4 趟顺序：加急RNA→加急PCR→普通RNA→普通PCR", "checktrayend 推进 vs 整盘结束 vs 回盘(InOrder)",
     "Arm fail 写回顺序：Get/Back 先报错; Move 反", "Arm finish：FINISH 在 null 检查前无条件执行"], size=13.5, gap=10, mark_col=AMBER)
footer(s, 8)

# 8.5 Arm 回调层解耦
s = slide(); header(s, "08", "机械臂命令回调层解耦", "同构推广：路由决策抽引擎")
txt(s, Inches(0.55), Inches(1.4), Inches(12.3), Inches(0.85),
    [[("把 Get/Back/Move 三类命令回调（finish/fail）里的路由决策 + 写回，抽到 databinding-free 的", 14, DARK, False)],
     [("ArmCallbackEngine —— 与调度引擎同构：引擎跑决策、端口做适配、单测锁行为。", 14, DARK, False)]], line_spacing=1.2)
acards = [("ArmCallbackEngine", "6 方法：onGet/Back/Move × Finish/Fail；按 ItemProperty 类型路由", GREEN),
          ("ArmCallbackPort", "9 端口方法；per-invocation 新建，闭包捕获 msg + armErrorCallback", AMBER),
          ("ArmItemSnapshot", "ItemProperty 的纯 POJO 快照（type+position），空安全 of()", CYAN)]
ax = Inches(0.55); aw = Inches(4.0); agap = Inches(0.18)
for title, desc, col in acards:
    rrect(s, ax, Inches(2.5), aw, Inches(1.7), LGREY)
    rect(s, ax, Inches(2.5), Inches(0.12), Inches(1.7), col)
    txt(s, Emu(int(ax)+int(Inches(0.32))), Inches(2.68), Emu(int(aw)-int(Inches(0.45))), Inches(0.5), [[(title, 15.5, col, True)]])
    txt(s, Emu(int(ax)+int(Inches(0.32))), Inches(3.2), Emu(int(aw)-int(Inches(0.45))), Inches(0.9), [[(desc, 12, GREY, False)]], line_spacing=1.15)
    ax = Emu(int(ax) + int(aw) + int(agap))
rrect(s, Inches(0.55), Inches(4.45), Inches(12.25), Inches(2.25), RGBColor(0xFB,0xEC,0xEC))
txt(s, Inches(0.85), Inches(4.6), Inches(11.7), Inches(0.5), [[("承重点（单测 InOrder 锁定，禁止归一化）", 15, RED, True)]])
bullet(s, Inches(0.9), Inches(5.15), Inches(11.6), Inches(1.4),
    ["三个 fail 写回顺序不一致：Get/Back 先 notifyArmError 再标 ERROR；Move 相反（先标 ERROR 再 notify）",
     "finish 里 updateDispatchStatus(FINISH) 在 null 检查之前无条件执行（Back/Move）",
     "Get.finish 的 armStep==GET 守卫经端口布尔化，引擎不碰 databinding；pre-check/startArmRobot 留在原地"],
    size=12.5, mark_col=RED, gap=8)
footer(s, 9)

# 9 附加修复
s = slide(); header(s, "09", "重构基础上的连带修复", "解耦后可精准定位的问题")  # header 保持09（Arm 用08）
fixes = [("手动放回自动 reset", "详情页「放回」时自动 reset 一次，省掉「先复位再放回」；只试一次，成功才清 usererror", CYAN),
         ("模板下发 ack 接回调度", "RNAAck_OK/Error 事件接回调度层：ack 失败→标异常+取回托盘（守卫 armStep==RUN）", BLUE),
         ("复位后状态/告警不清", "reset 清 usererror + sendAlarm；状态色由设备轮询真值驱动，不掩盖真实故障", GREEN),
         ("异常放回后仍继续流程", "放回/复位补 NotifyXxxTrayError → 托盘标 ERROR，checktrayend 守卫跳过，不再误推进", RED),
         ("一键清除保留样本", "清历史批次/项目/调度信息，保留各孔样本号 serialNum + 条码 reagentCode", AMBER)]
yy = Inches(1.5)
for title, desc, col in fixes:
    rrect(s, Inches(0.55), yy, Inches(12.25), Inches(1.0), LGREY)
    rect(s, Inches(0.55), yy, Inches(0.14), Inches(1.0), col)
    txt(s, Inches(0.95), yy, Inches(3.7), Inches(1.0), [[(title, 15.5, col, True)]], anchor=MSO_ANCHOR.MIDDLE)
    txt(s, Inches(4.7), yy, Inches(7.9), Inches(1.0), [[(desc, 12.5, GREY, False)]], anchor=MSO_ANCHOR.MIDDLE, line_spacing=1.12)
    yy = Emu(int(yy)+int(Inches(1.12)))
footer(s, 9)

# 10 成果
s = slide(); header(s, "10", "成果与后续")
rrect(s, Inches(0.55), Inches(1.5), Inches(6.0), Inches(5.2), RGBColor(0xEA,0xF6,0xEE))
txt(s, Inches(0.55), Inches(1.65), Inches(6.0), Inches(0.5), [[("✓ 已交付", 20, GREEN, True)]], align=PP_ALIGN.CENTER)
bullet(s, Inches(0.9), Inches(2.35), Inches(5.4), Inches(4.2),
    ["整条调度决策链（RNA/PCR/Tray）脱离 databinding", "APLManager 中 check* 方法退化为薄委托层",
     "52 单元测试，纯 JVM 可跑，行为零回归", "全量中文注释 + 存档兼容",
     "5 份文档：总结/回归清单/修改历史/时序图/状态图", "可复用 skill：decouple-scheduling-from-databinding"],
    size=14, mark_col=GREEN, gap=12)
rrect(s, Inches(6.85), Inches(1.5), Inches(5.95), Inches(5.2), LGREY)
txt(s, Inches(6.85), Inches(1.65), Inches(5.95), Inches(0.5), [[("→ 后续建议", 20, BLUE, True)]], align=PP_ALIGN.CENTER)
bullet(s, Inches(7.2), Inches(2.35), Inches(5.35), Inches(4.2),
    ["真机端到端回归（对照回归清单逐项）", "重点验证：存档升级 / 门互锁 / 异常还盘 reset 前置",
     "Arm 机械臂命令回调层可按同法继续解耦", "自检模块状态同步可纳入引擎", "ack 反馈 / 异常托盘等连带修复的真机确认"],
    size=14, mark_col=BLUE, gap=12)
footer(s, 11)

# 10.5 代码前后对比（放回/复位漏标托盘异常）
def code_box(s, x, y, w, h, title, title_col, lines):
    rrect(s, x, y, w, h, RGBColor(0x1E, 0x28, 0x3A))
    rect(s, x, y, w, Inches(0.5), title_col)
    txt(s, x, y, w, Inches(0.5), [[(title, 14, WHITE, True)]], align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    tb = s.shapes.add_textbox(Emu(int(x)+int(Inches(0.22))), Emu(int(y)+int(Inches(0.62))),
                              Emu(int(w)-int(Inches(0.4))), Emu(int(h)-int(Inches(0.75))))
    tf = tb.text_frame; tf.word_wrap = True
    tf.margin_left = tf.margin_right = Pt(2); tf.margin_top = tf.margin_bottom = Pt(1)
    for i, (t, col) in enumerate(lines):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.space_after = Pt(2); p.line_spacing = 1.0
        r = p.add_run(); r.text = t
        r.font.size = Pt(11); r.font.name = "Consolas"; r.font.color.rgb = col
    return tb

CODE_GREY = RGBColor(0x8A, 0x97, 0xA8)
CODE_WHT  = RGBColor(0xE6, 0xEC, 0xF2)
CODE_RED  = RGBColor(0xF2, 0x8B, 0x82)
CODE_GRN  = RGBColor(0x7E, 0xD3, 0x9E)
CODE_CYN  = RGBColor(0x6F, 0xD3, 0xE0)

s = slide(); header(s, "11", "代码前后对比", "sendBackTray() / resetBtn —— 补一行 NotifyXxxTrayError")
code_box(s, Inches(0.55), Inches(1.5), Inches(6.05), Inches(5.15), "修复前（Bug）", RED,
    [("private boolean sendBackTray(){", CODE_WHT),
     ("    int rnaindex = model.index.get();", CODE_WHT),
     ("    // 只复位设备 + 推设备到 BACK", CODE_GREY),
     ("    RNAManager.getInstance()", CODE_WHT),
     ("            .reset(rnaindex);", CODE_WHT),
     ("", CODE_WHT),
     ("    APLManager.getInstance()", CODE_WHT),
     ("        .checkRnaFinishError(rnaindex);", CODE_CYN),
     ("    // armStep = BACK", CODE_GREY),
     ("", CODE_WHT),
     ("    // ❌ 未标托盘异常", CODE_RED),
     ("    // tray.status 仍 = str_RUN", CODE_RED),
     ("    return true;", CODE_WHT),
     ("}", CODE_WHT),
     ("", CODE_WHT),
     ("// 后果: checkTrayEnd 守卫通过", CODE_RED),
     ("//   → advanceTrayDispatch(di+1)", CODE_RED),
     ("//   → 异常托盘继续跑后续流程", CODE_RED)])
code_box(s, Inches(6.75), Inches(1.5), Inches(6.05), Inches(5.15), "修复后", GREEN,
    [("private boolean sendBackTray(){", CODE_WHT),
     ("    int rnaindex = model.index.get();", CODE_WHT),
     ("    RNAManager.getInstance()", CODE_WHT),
     ("            .reset(rnaindex);", CODE_WHT),
     ("", CODE_WHT),
     ("    // ★ 先标托盘异常", CODE_GRN),
     ("    APLManager.getInstance()", CODE_WHT),
     ("        .NotifyRnaTrayError(rnaindex);", CODE_GRN),
     ("    // → errorDispatch: tray=ERROR", CODE_GREY),
     ("", CODE_WHT),
     ("    APLManager.getInstance()", CODE_WHT),
     ("        .checkRnaFinishError(rnaindex);", CODE_CYN),
     ("    // armStep = BACK", CODE_GREY),
     ("    return true;", CODE_WHT),
     ("}", CODE_WHT),
     ("", CODE_WHT),
     ("// 效果: checkTrayEnd 守卫跳过(!RUN)", CODE_GRN),
     ("//   checkTrayError 取回, 保持 ERROR", CODE_GRN),
     ("//   ✔ 不再误推进, 需手动恢复", CODE_GRN)])
footer(s, 12)

# 11~ 流程图（Mermaid 渲染 PNG）
import os
DIAG = r"D:\PCRinOne\framework\bowei_liushuixian\liushuixian_melody\Bio_PCR_liushuixian_hongshi\docs_异常复位放回修复\diagrams"
diagrams = [
    ("01_1.png", "12", "时序图 · 修复前（Bug）", "放回/复位后托盘仍被 checkTrayEnd 推进下一步"),
    ("01_2.png", "13", "时序图 · 修复后", "先 NotifyXxxTrayError 标异常，checkTrayError 取回并保持 ERROR"),
    ("02_1.png", "14", "状态图 · 托盘 status（修复后）", "error 为吸收态，checkTrayEnd 守卫跳过，不再推进"),
    ("02_3.png", "15", "状态图 · 设备 armStep", "设备状态线与托盘状态线相互独立"),
    ("02_2.png", "16", "状态对照 · 修复前 vs 修复后", "同一异常事件的托盘走向分叉"),
    ("03_1.png", "17", "一图流 · 放回/复位→异常→取回→恢复", "泳道 + 状态标注（★为本次新增）"),
    ("03_2.png", "18", "修复前后对照 · 路径分叉", "托盘是否被标 str_ERROR 决定后续"),
]
from PIL import Image as _PILImg
for fn, no, title, sub in diagrams:
    s = slide(); header(s, no, title, sub)
    path = os.path.join(DIAG, fn)
    iw, ih = _PILImg.open(path).size
    # 可用区域
    avail_x = Inches(0.4); avail_y = Inches(1.45)
    avail_w = int(Inches(12.55)); avail_h = int(Inches(5.55))
    ratio = min(avail_w / iw, avail_h / ih)
    dw = int(iw * ratio); dh = int(ih * ratio)
    px = Emu(int(avail_x) + (avail_w - dw) // 2)
    py = Emu(int(avail_y) + (avail_h - dh) // 2)
    s.shapes.add_picture(path, px, py, width=Emu(dw), height=Emu(dh))
    footer(s, int(no))

# 17 结尾
s = slide()
rect(s, 0, 0, SW, SH, NAVY)
rect(s, 0, Inches(3.5), SW, Pt(3), CYAN)
txt(s, 0, Inches(2.55), SW, Inches(1.0), [[("解耦完成 · 行为可验证 · 可持续演进", 34, WHITE, True)]], align=PP_ALIGN.CENTER)
txt(s, 0, Inches(3.75), SW, Inches(0.7), [[("从「UI 里跑调度」到「引擎里跑调度，UI 只做展示」", 17, CYAN, False)]], align=PP_ALIGN.CENTER)
txt(s, 0, Inches(6.7), SW, Inches(0.5), [[("PCRinOne  ·  机械臂调度重构总结", 12, RGBColor(0x8C,0x9A,0xB5), False)]], align=PP_ALIGN.CENTER)

out = r"D:\PCRinOne\framework\bowei_liushuixian\liushuixian_melody\Bio_PCR_liushuixian_hongshi\机械臂调度重构总结.pptx"
prs.save(out)
print("SAVED", out, "slides=", len(prs.slides._sldIdLst))
