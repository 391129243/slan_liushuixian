# -*- coding: utf-8 -*-
"""生成"机械臂/核提/PCR模组调度分析"PPT"""
import sys, types
# 环境中的 PIL 缺少 _imaging C 扩展；本脚本不插入图片，stub 掉即可
_pil = types.ModuleType('PIL.Image')
_pil.Image = type('Image', (), {})
def _pil_open(*a, **k): raise RuntimeError('PIL stub')
_pil.open = _pil_open
sys.modules['PIL.Image'] = _pil
try:
    import PIL; PIL.Image = _pil
except Exception:
    pass
from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE
from pptx.oxml.ns import qn

# ---------- 配色 ----------
C_BG      = RGBColor(0xF4, 0xF6, 0xFB)
C_DARK    = RGBColor(0x1B, 0x26, 0x38)   # 深蓝标题
C_BLUE    = RGBColor(0x2E, 0x5B, 0xFF)   # 主蓝
C_TEAL    = RGBColor(0x11, 0x9D, 0xA4)   # 核提
C_ORANGE  = RGBColor(0xE8, 0x7A, 0x1E)   # PCR
C_PURPLE  = RGBColor(0x6C, 0x4A, 0xB6)   # 机械臂
C_GRAY    = RGBColor(0x5A, 0x63, 0x72)
C_WHITE   = RGBColor(0xFF, 0xFF, 0xFF)
C_LIGHT   = RGBColor(0xE6, 0xEC, 0xF7)
C_GREEN   = RGBColor(0x2E, 0x9E, 0x5B)

OUT = r"D:\PCRinOne\framework\bowei_liushuixian\liushuixian_melody\Bio_PCR_liushuixian_hongshi\调度分析_机械臂核提PCR_v8.pptx"

prs = Presentation()
prs.slide_width  = Inches(13.333)
prs.slide_height = Inches(7.5)
SW, SH = prs.slide_width, prs.slide_height
BLANK = prs.slide_layouts[6]

def slide():
    s = prs.slides.add_slide(BLANK)
    bg = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, SW, SH)
    bg.fill.solid(); bg.fill.fore_color.rgb = C_BG
    bg.line.fill.background()
    bg.shadow.inherit = False
    return s

def box(s, x, y, w, h, fill=None, line=None, line_w=1.0, shape=MSO_SHAPE.ROUNDED_RECTANGLE, radius=0.08):
    sp = s.shapes.add_shape(shape, Inches(x), Inches(y), Inches(w), Inches(h))
    if fill is None:
        sp.fill.background()
    else:
        sp.fill.solid(); sp.fill.fore_color.rgb = fill
    if line is None:
        sp.line.fill.background()
    else:
        sp.line.color.rgb = line; sp.line.width = Pt(line_w)
    sp.shadow.inherit = False
    if shape == MSO_SHAPE.ROUNDED_RECTANGLE:
        try:
            sp.adjustments[0] = radius
        except Exception:
            pass
    return sp

def txt(s, x, y, w, h, runs, align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.TOP, wrap=True, space=1.0):
    """runs: list of paragraphs; each paragraph = list of (text,size,color,bold) tuples"""
    tb = s.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = tb.text_frame
    tf.word_wrap = wrap
    tf.vertical_anchor = anchor
    tf.margin_left = tf.margin_right = Inches(0.05)
    tf.margin_top = tf.margin_bottom = Inches(0.02)
    for i, para in enumerate(runs):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = align
        p.line_spacing = space
        for (t, sz, col, bold) in para:
            r = p.add_run(); r.text = t
            r.font.size = Pt(sz); r.font.color.rgb = col; r.font.bold = bold
            r.font.name = 'Microsoft YaHei'
    return tb

def title_bar(s, kicker, title, color=C_BLUE):
    box(s, 0, 0, 0.28, 1.2, fill=color, shape=MSO_SHAPE.RECTANGLE)
    txt(s, 0.55, 0.18, 12, 0.4, [[(kicker, 13, color, True)]])
    txt(s, 0.5, 0.5, 12.3, 0.7, [[(title, 26, C_DARK, True)]])

def arrow(s, x, y, w, h, color=C_GRAY, shape=MSO_SHAPE.RIGHT_ARROW):
    a = s.shapes.add_shape(shape, Inches(x), Inches(y), Inches(w), Inches(h))
    a.fill.solid(); a.fill.fore_color.rgb = color
    a.line.fill.background(); a.shadow.inherit = False
    return a

def connector(s, x1, y1, x2, y2, color=C_GRAY, w=1.5):
    ln = s.shapes.add_connector(2, Inches(x1), Inches(y1), Inches(x2), Inches(y2))
    ln.line.color.rgb = color; ln.line.width = Pt(w)
    ln.shadow.inherit = False
    return ln

print("building slides...")

# ============ Slide 1 封面 ============
s = prs.slides.add_slide(BLANK)
bg = box(s, 0, 0, 13.333, 7.5, fill=C_DARK, shape=MSO_SHAPE.RECTANGLE)
# 装饰色块
box(s, 0, 0, 13.333, 0.18, fill=C_BLUE, shape=MSO_SHAPE.RECTANGLE)
box(s, 8.9, 1.3, 3.9, 4.9, fill=RGBColor(0x24,0x31,0x48), shape=MSO_SHAPE.ROUNDED_RECTANGLE, radius=0.06)
# 三个模组小标签
labels = [("机械臂 ARM", C_PURPLE, 1.7), ("核提 RNA", C_TEAL, 2.55), ("PCR 扩增", C_ORANGE, 3.4)]
for name, col, yy in labels:
    box(s, 9.2, yy, 3.3, 0.62, fill=col, radius=0.25)
    txt(s, 9.2, yy+0.05, 3.3, 0.5, [[(name, 15, C_WHITE, True)]], align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
box(s, 9.2, 4.35, 3.3, 0.62, fill=RGBColor(0x38,0x45,0x5e), radius=0.25)
txt(s, 9.2, 4.4, 3.3, 0.5, [[("APLManager 总调度", 13, C_WHITE, True)]], align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)

txt(s, 0.9, 2.0, 8, 0.5, [[("代码架构分析", 18, C_BLUE, True)]])
txt(s, 0.9, 2.55, 8, 1.8, [[("机械臂 · 核提 · PCR 模组", 40, C_WHITE, True)],
                            [("调度逻辑与调用流程", 40, C_WHITE, True)]], space=1.05)
txt(s, 0.95, 4.6, 8, 0.5, [[("Bio_PCR 流水线自动化系统", 16, C_LIGHT, False)]])
txt(s, 0.95, 6.6, 11, 0.4, [[("轮询式状态机调度  ·  串口 + WebSocket 双通道  ·  RNA→PCR 全自动串联", 13, RGBColor(0x9A,0xA6,0xBd), False)]])

# ============ Slide 2 总体架构 ============
s = slide()
title_bar(s, "OVERVIEW / 总体架构", "分层架构：一个大脑，两条通道，三大模组", C_BLUE)

def node(x, y, w, h, title, sub, col, tsz=13):
    box(s, x, y, w, h, fill=col, radius=0.12)
    if sub:
        txt(s, x, y+0.06, w, h-0.1,
            [[(title, tsz, C_WHITE, True)], [(sub, 9.5, C_LIGHT, False)]],
            align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE, space=1.0)
    else:
        txt(s, x, y, w, h, [[(title, tsz, C_WHITE, True)]],
            align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)

# 驱动层
node(3.7, 1.45, 6.0, 0.85, "PipeMainActivity  (驱动入口)", "5秒Timer · UIHandler主线程路由 · EventBus订阅硬件回调", C_DARK)
connector(s, 6.7, 2.30, 6.7, 2.65, C_GRAY, 2)
# 调度层
node(3.7, 2.65, 6.0, 0.85, "APLManager  (总调度器 / 状态机心脏)", "checkArmTask() 每5秒轮询推进 armStep 状态机", C_BLUE)
# 四个manager
mgrs = [("TrayManager","托盘+调度表", C_GRAY, 0.55),
        ("ArmManager","机械臂", C_PURPLE, 3.9),
        ("RNAManager","核提", C_TEAL, 7.25),
        ("PCRManager","PCR", C_ORANGE, 10.6)]
for name, sub, col, xx in mgrs:
    connector(s, 6.7, 3.50, xx+1.1, 3.9, C_GRAY, 1.3)
    node(xx, 3.9, 2.2, 0.8, name, sub, col)
# 底层通道
node(3.9, 5.2, 2.2, 0.95, "RobotWorkManager", "WorkManager任务链", C_PURPLE)
node(7.25, 5.2, 5.55, 0.95, "WSSModuleManager (WebSocket 服务端)", "下发模板 / 接收状态·荧光·完成", C_TEAL)
connector(s, 5.0, 4.70, 5.0, 5.2, C_PURPLE, 1.3)
connector(s, 8.35, 4.70, 8.35, 5.2, C_TEAL, 1.3)
connector(s, 11.7, 4.70, 11.7, 5.2, C_ORANGE, 1.3)
# 硬件
node(3.9, 6.45, 2.2, 0.7, "机械臂 MCU", "串口 CH340", C_DARK, 11)
node(7.25, 6.45, 2.6, 0.7, "RNA 设备 (嵌入式)", "WebSocket", C_DARK, 11)
node(10.2, 6.45, 2.6, 0.7, "PCR 设备 (嵌入式)", "WebSocket", C_DARK, 11)
connector(s, 5.0, 6.15, 5.0, 6.45, C_GRAY, 1.3)
connector(s, 8.35, 6.15, 8.35, 6.45, C_GRAY, 1.3)
connector(s, 11.5, 6.15, 11.5, 6.45, C_GRAY, 1.3)
# 通道标注
txt(s, 0.55, 5.45, 3.2, 0.6, [[("串口通道", 12, C_PURPLE, True)],[("帧协议 · 逐条hex", 10, C_GRAY, False)]], space=1.0)

# ============ Slide 3 调度心脏 ============
s = slide()
title_bar(s, "SCHEDULER / 调度心脏", "5 秒轮询：定时器驱动的状态机", C_BLUE)

steps = [
    ("initTimer()", "周期 ARMTASK_PERIOD = 5000ms", C_DARK),
    ("TimerTask → MSG_ARMTASK", "切回主线程", C_GRAY),
    ("UIHandler → checkArmtask()", ":615 → :1163", C_GRAY),
    ("refreshConnectinfo()", "拉取硬件状态刷新模型", C_TEAL),
    ("APLManager.checkArmTask()", "调度心脏 :267", C_BLUE),
]
yy = 1.55
for i,(t,sub,col) in enumerate(steps):
    box(s, 0.6, yy, 5.7, 0.72, fill=col if col in (C_BLUE,C_TEAL) else C_WHITE,
        line=col if col not in (C_BLUE,C_TEAL) else None, line_w=1.4, radius=0.14)
    tc = C_WHITE if col in (C_BLUE,C_TEAL) else C_DARK
    sc = C_LIGHT if col in (C_BLUE,C_TEAL) else C_GRAY
    txt(s, 0.85, yy+0.06, 5.4, 0.6, [[(t, 13.5, tc, True)],[(sub, 10, sc, False)]], space=1.0)
    if i < len(steps)-1:
        arrow(s, 3.2, yy+0.72, 0.5, 0.28, C_GRAY, MSO_SHAPE.DOWN_ARROW)
    yy += 1.02

box(s, 6.7, 1.55, 6.1, 5.35, fill=C_WHITE, line=C_BLUE, line_w=1.4, radius=0.05)
txt(s, 7.0, 1.7, 5.6, 0.4, [[("checkArmTask() 固定优先级", 15, C_BLUE, True)]])
prio = [
    ("① checkrnaRun()", "WORK_RUN且未启动 → 下发RNA模板", C_TEAL),
    ("② checkpcrRun()", "同上，下发PCR模板", C_ORANGE),
    ("③ checktrayend()", "整盘完成检查、推进 dispatchindex", C_GRAY),
    ("④ if(!doorhold)", "门操作优先级最高", C_GRAY),
    ("⑤ checktrayerror()", "异常托盘优先处理", C_GRAY),
    ("⑥ checktrayurgent()", "加急RNA→加急PCR→普通RNA→普通PCR", C_BLUE),
]
yy = 2.25
for t,sub,col in prio:
    box(s, 7.0, yy, 0.16, 0.62, fill=col, shape=MSO_SHAPE.RECTANGLE)
    txt(s, 7.35, yy, 5.3, 0.7, [[(t, 13, C_DARK, True)],[(sub, 10.5, C_GRAY, False)]], space=1.0)
    yy += 0.75
txt(s, 7.0, 6.55, 5.6, 0.35, [[("机械臂是单一共享资源：任一步被占用则本周期 return", 10.5, C_ORANGE, True)]])

# ============ Slide 4 armStep 状态机 ============
s = slide()
title_bar(s, "STATE MACHINE / 状态机", "每台设备的 armStep 状态迁移", C_PURPLE)

def node(x, y, name, code, col, w=2.35, h=0.95):
    box(s, x, y, w, h, fill=col, radius=0.16)
    txt(s, x, y+0.12, w, 0.7, [[(name, 15, C_WHITE, True)],[(code, 11, C_LIGHT, False)]],
        align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE, space=1.0)

node(0.7, 3.1, "WORK_FREE", "0 · 空闲", C_GRAY)
node(3.7, 3.1, "WORK_GET", "1 · 取托盘", C_PURPLE)
node(6.7, 3.1, "WORK_RUN", "2 · 跑模板", C_BLUE)
node(9.9, 1.6, "WORK_MOVE", "4 · RNA→PCR移盘", C_ORANGE)
node(9.9, 4.6, "WORK_BACK", "3 · 送回原位", C_TEAL)

arrow(s, 3.1, 3.42, 0.55, 0.3, C_GRAY)
arrow(s, 6.1, 3.42, 0.55, 0.3, C_GRAY)
# run -> move / back 分叉
connector(s, 9.05, 3.35, 9.5, 2.05, C_GRAY, 1.6)
arrow(s, 9.5, 1.9, 0.35, 0.28, C_ORANGE)
connector(s, 9.05, 3.65, 9.5, 5.0, C_GRAY, 1.6)
arrow(s, 9.5, 4.85, 0.35, 0.28, C_TEAL)
# back -> free (回流)
connector(s, 9.9, 5.55, 1.85, 5.55, C_TEAL, 1.6)
connector(s, 1.85, 5.55, 1.85, 4.05, C_TEAL, 1.6)
arrow(s, 1.7, 3.75, 0.3, 0.3, C_TEAL, MSO_SHAPE.UP_ARROW)

txt(s, 0.7, 1.55, 8.5, 0.5, [[("创建实验 → status=running, dispatchindex=1", 13, C_DARK, True)]])
arrow(s, 1.7, 2.15, 0.3, 0.85, C_GRAY, MSO_SHAPE.DOWN_ARROW)

txt(s, 6.7, 2.35, 3.0, 0.5, [[("模块跑完(WSS回调)", 11, C_DARK, True)]])
txt(s, 8.9, 2.55, 1.5, 0.4, [[("nextdevice>0", 10, C_ORANGE, True)]])
txt(s, 8.9, 4.15, 1.5, 0.4, [[("nextdevice=0", 10, C_TEAL, True)]])

box(s, 0.7, 6.35, 12.1, 0.75, fill=C_LIGHT, radius=0.1)
txt(s, 0.95, 6.45, 11.7, 0.6, [[("完成通知链：", 12, C_DARK, True),
    ("WSS事件 → EventBus → PipeMainActivity翻译 → Handler消息 → APLManager状态迁移，再靠下一个5秒轮询发现并推进", 12, C_GRAY, False)]],
    anchor=MSO_ANCHOR.MIDDLE)

# ============ Slide 5 全流程调用链 ============
s = slide()
title_bar(s, "PIPELINE / 全流程", "核提 → PCR 串联：一次实验的完整调用链", C_BLUE)

stages = [
    ("①", "取托盘送入 RNA", "串口", C_PURPLE,
     "getFreeModel挑空闲核提 → armStep=WORK_GET → ArmManager.startArmRobot → RobotWorkManager多步Worker链(get→扫码→put)"),
    ("②", "核提执行", "WebSocket", C_TEAL,
     "checkrnaRun → RNAManager.execute → 组装RNAReqMessage(hex透传) → WSSModuleManager下发 ── RNA设备运行 ── RNAFinish回调"),
    ("③", "机械臂移盘 RNA→PCR", "串口", C_PURPLE,
     "checkRnaFinish读nextdevice>0 → WORK_MOVE → startGet_RNAAndPut_PCR 9步链(取盖→开门→抓盘→扫码→关门→放PCR→盖回)"),
    ("④", "PCR 执行", "WebSocket", C_ORANGE,
     "checkpcrRun → PCRManager.execute → 读.prj扩增程序+8孔样本 → 下发PCRReqMessage ── 周期上报PCRCurve荧光 ── PCRFinish"),
    ("⑤", "机械臂送回托盘", "串口", C_PURPLE,
     "WORK_BACK → startArmTaskBackTray → finish: armStep=WORK_FREE, 托盘归位, checktrayend结束整盘"),
]
yy = 1.55
for num, title, chan, col, desc in stages:
    box(s, 0.6, yy, 0.7, 0.92, fill=col, radius=0.2)
    txt(s, 0.6, yy, 0.7, 0.92, [[(num, 24, C_WHITE, True)]], align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    box(s, 1.45, yy, 11.35, 0.92, fill=C_WHITE, line=col, line_w=1.3, radius=0.08)
    chancol = C_PURPLE if chan=="串口" else C_TEAL
    txt(s, 1.7, yy+0.09, 11.0, 0.4, [[(title+"  ", 15, C_DARK, True), ("["+chan+"]", 11, chancol, True)]])
    txt(s, 1.7, yy+0.5, 10.9, 0.4, [[(desc, 10.5, C_GRAY, False)]])
    yy += 1.05

# ============ Slide 6 机械臂三级同步 ============
s = slide()
title_bar(s, "SYNC / 同步机制", "机械臂串口指令的三级同步", C_PURPLE)

lv = [
    ("第 1 级", "步骤间 · 异步", "WorkManager + LiveData", C_BLUE,
     "RobotWorkManager.observeWorkQueue 观察整条链，全部 SUCCEEDED 才触发 onAllTasksCompleted；任一 FAILED → onTaskFailed"),
    ("第 2 级", "单 Worker 内 · 阻塞", "lock.wait() + EventBus唤醒", C_PURPLE,
     "RobotWorker.doWork → robotGet → lock.wait() 阻塞，收到 ProcedureFinishEvent 才 notify；完成后做传感器复核(夹爪ARM+目标位)"),
    ("第 3 级", "单条 hex · 串口应答状态机", "RNAAndPCRAsyncTask", C_TEAL,
     "逐条下发 sendSerialCmdMsg_NeedAck → MCU；应答 0xA4确认 / 0xA2完成驱动下一条 / 0xA1传感器位图上报；超时60s(RNA)/7200s(PCR)重试3次"),
]
yy = 1.6
for tag, mode, tech, col, desc in lv:
    box(s, 0.6, yy, 12.2, 1.55, fill=C_WHITE, line=col, line_w=1.5, radius=0.06)
    box(s, 0.6, yy, 2.1, 1.55, fill=col, radius=0.06)
    txt(s, 0.6, yy+0.2, 2.1, 1.15, [[(tag, 20, C_WHITE, True)],[(mode, 11, C_LIGHT, True)]],
        align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE, space=1.1)
    txt(s, 2.95, yy+0.22, 9.6, 0.5, [[(tech, 15, col, True)]])
    txt(s, 2.95, yy+0.78, 9.7, 0.7, [[(desc, 11.5, C_GRAY, False)]], space=1.05)
    yy += 1.72

# ============ Slide 7 PipeTrayModel 托盘状态机 ============
s = slide()
title_bar(s, "TRAY STATE / 托盘状态机", "PipeTrayModel：托盘 status 迁移驱动调度", C_GREEN)

def tnode(x, y, w, h, name, code, col):
    box(s, x, y, w, h, fill=col, radius=0.16)
    txt(s, x, y+0.08, w, h-0.12, [[(name, 15, C_WHITE, True)],[(code, 10.5, C_LIGHT, False)]],
        align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE, space=1.0)

# 初始
box(s, 0.6, 1.5, 3.3, 0.95, fill=C_LIGHT, line=C_GRAY, line_w=1.2, radius=0.1)
txt(s, 0.75, 1.6, 3.05, 0.8, [[("init() 初始化", 12.5, C_DARK, True)],
    [("status=free, flag=FREE", 9.5, C_GRAY, False)],
    [("dispatchindex=1, createTime=0", 9.5, C_GRAY, False)]], space=1.02)
arrow(s, 2.0, 2.45, 0.4, 0.55, C_GRAY, MSO_SHAPE.DOWN_ARROW)
txt(s, 2.5, 2.5, 5.0, 0.5, [[("用户保存 TrayDetailFragment:196", 10.5, C_GREEN, True)],
    [("校验通过 + createDispatchList()", 9.5, C_GRAY, False)]], space=1.0)

tnode(0.6, 3.15, 3.3, 1.0, "str_RUN  运行中", "flag=RUN, dispatchindex=1", C_GREEN)
# running 内循环
box(s, 4.35, 3.05, 4.15, 1.2, fill=C_WHITE, line=C_BLUE, line_w=1.3, radius=0.08)
txt(s, 4.55, 3.15, 3.85, 1.0, [[("每心跳(5s) checktrayend()", 11.5, C_BLUE, True)],
    [("步 dm.STATUS_FINISH → dispatchindex+1", 9.8, C_GRAY, False)],
    [("(760/781/802) 逐步推进调度游标", 9.8, C_GRAY, False)]], space=1.05)
connector(s, 3.9, 3.5, 4.35, 3.5, C_BLUE, 1.3)
connector(s, 4.35, 3.9, 3.9, 3.9, C_BLUE, 1.3)

# running -> error
tnode(9.0, 1.5, 3.6, 1.05, "str_ERROR  异常", "设备 NotifyRna/PcrTrayError", C_ORANGE)
connector(s, 3.9, 3.4, 9.0, 2.05, C_ORANGE, 1.4)
txt(s, 5.2, 2.15, 3.8, 0.4, [[("errorDispatch()→记 errorDispatchindex", 9.5, C_ORANGE, True)]])
txt(s, 9.0, 2.6, 3.6, 0.7, [[("checktrayerror() 优先取回托盘", 9.8, C_GRAY, True)],
    [("还盘完成→FLAG_FREE, 设备复位", 9.5, C_GRAY, False)]], space=1.05)
# error -> running (继续)
connector(s, 9.0, 2.0, 3.9, 3.3, C_TEAL, 1.2)
txt(s, 5.3, 2.75, 3.5, 0.35, [[("continueTray() 用户\"继续\"→RUN", 9, C_TEAL, True)]])

# running -> finish
arrow(s, 2.0, 4.15, 0.4, 0.6, C_GREEN, MSO_SHAPE.DOWN_ARROW)
txt(s, 2.5, 4.25, 5.5, 0.5, [[("最后一步完成 traydispatchindex>size", 10.5, C_DARK, True)],
    [("flag=FREE, status=finish, createLedTask(0)关灯", 9.5, C_GRAY, False)]], space=1.0)
tnode(0.6, 4.9, 3.3, 0.9, "str_FINISH  完成", "关灯 + saveModel", C_GRAY)

# flagstatus 面板
box(s, 8.8, 3.15, 3.9, 3.0, fill=C_WHITE, line=C_PURPLE, line_w=1.4, radius=0.06)
txt(s, 9.05, 3.28, 3.5, 0.4, [[("flagstatus 用户操作", 13.5, C_PURPLE, True)]])
flags = [("FLAG_RUN", "运行, 禁止新增托盘"),
         ("FLAG_PAUSE", "暂停 (伴随 status=error)"),
         ("FLAG_STOP", "停止, 停设备工作"),
         ("FLAG_FREE", "结束/清除, 允许新增")]
yy = 3.75
for name, d in flags:
    box(s, 9.05, yy, 0.15, 0.5, fill=C_PURPLE, shape=MSO_SHAPE.RECTANGLE)
    txt(s, 9.3, yy, 3.3, 0.55, [[(name, 11, C_DARK, True)],[(d, 9, C_GRAY, False)]], space=0.95)
    yy += 0.58
txt(s, 9.05, 6.08, 3.55, 0.35, [[("flag 约束UI交互, status 驱动主循环", 8.8, C_ORANGE, True)]])

box(s, 0.6, 6.35, 7.9, 0.78, fill=C_LIGHT, radius=0.1)
txt(s, 0.85, 6.42, 7.5, 0.65, [[("核心：", 11.5, C_DARK, True),
    ("真正驱动调度主循环的是 status(str_RUN)；flagstatus 主要约束UI，两者由UI成对写入", 11, C_GRAY, False)]],
    anchor=MSO_ANCHOR.MIDDLE)

# ============ Slide 8 dispatchlist 与优先级 ============
s = slide()
title_bar(s, "DISPATCH LIST / 调度清单", "dispatchindex 游标推进与 nextdevice 串联", C_GREEN)

# 左：dispatchlist 生成
box(s, 0.6, 1.5, 6.0, 2.55, fill=C_WHITE, line=C_GREEN, line_w=1.4, radius=0.05)
txt(s, 0.85, 1.6, 5.6, 0.4, [[("createDispatchList() 四种拆分 (APL:1686)", 13, C_GREEN, True)]])
rows = [("FULL_SINGLE", "RNA(next=PCR) → PCR(savepcr)", C_TEAL),
        ("FULL_MULTI", "RNA→PCR→RNA→PCR 交替4步", C_ORANGE),
        ("RNA", "仅核提 1步 (next=0)", C_TEAL),
        ("PCR", "仅PCR 1步 (savepcr, next=0)", C_ORANGE)]
yy = 2.1
for name, d, col in rows:
    box(s, 0.9, yy, 1.9, 0.42, fill=col, radius=0.2)
    txt(s, 0.9, yy+0.02, 1.9, 0.38, [[(name, 10.5, C_WHITE, True)]], align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    txt(s, 2.95, yy+0.02, 3.5, 0.4, [[(d, 10.5, C_GRAY, False)]], anchor=MSO_ANCHOR.MIDDLE)
    yy += 0.48

# 右：nextdevice 串联
box(s, 6.85, 1.5, 5.85, 2.55, fill=C_WHITE, line=C_BLUE, line_w=1.4, radius=0.05)
txt(s, 7.1, 1.6, 5.4, 0.4, [[("nextdevice 串联机制 (运行期)", 13, C_BLUE, True)]])
txt(s, 7.1, 2.05, 5.5, 1.9, [
    [("设备跑完 → checkRna/PcrFinish 读 nextdevice", 11, C_DARK, True)],
    [("• nextdevice > 0", 11, C_ORANGE, True),("  → armStep=WORK_MOVE 移到对方设备", 10.5, C_GRAY, False)],
    [("• nextdevice = 0", 11, C_TEAL, True),("  → armStep=WORK_BACK 还盘", 10.5, C_GRAY, False)],
    [("落位后 updateDsiapatchNext() (Tray:295)", 11, C_DARK, True)],
    [("  dispatchindex+1 + 绑定实际RNA/PCR编号", 10.5, C_GRAY, False)],
    [("  setCurrentTemplate 建立 托盘↔设备 引用", 10.5, C_GRAY, False)],
    ], space=1.15)

# 下：优先级两级
box(s, 0.6, 4.35, 12.1, 2.75, fill=C_WHITE, line=C_PURPLE, line_w=1.4, radius=0.05)
txt(s, 0.85, 4.45, 11.5, 0.4, [[("checktrayurgent() 优先级：段级 urgent 插队 + 段内 createTime 先到先派", 13.5, C_PURPLE, True)]])
prio = [("①", "加急 RNA", C_TEAL, "urgent==true"),
        ("②", "加急 PCR", C_ORANGE, "urgent==true"),
        ("③", "普通 RNA", C_TEAL, "全部盘"),
        ("④", "普通 PCR", C_ORANGE, "全部盘")]
x = 0.95
for num, name, col, tag in prio:
    box(s, x, 5.0, 2.7, 0.95, fill=col, radius=0.12)
    txt(s, x, 5.08, 2.7, 0.8, [[(num+" "+name, 14, C_WHITE, True)],[(tag, 10, C_LIGHT, False)]],
        align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE, space=1.0)
    if num != "④":
        arrow(s, x+2.72, 5.32, 0.28, 0.3, C_GRAY)
    x += 3.0
txt(s, 0.95, 6.15, 11.6, 0.85, [
    [("• 任一段返回 false(机械臂被占用) 即中断本轮 → 加急盘在同一心跳内优先抢占机械臂实现插队", 11, C_GRAY, False)],
    [("• 同优先级内由 traylist 的 createTime 升序 (updatePriority) 决定，先提交先派", 11, C_GRAY, False)],
    ], space=1.15)

def attr_table(s, x, y, w, rows, headcol, cw):
    """rows: list of (field, type, desc, rowcolor_or_None). cw=(c1,c2,c3) widths"""
    rh = 0.44
    # 表头
    hx = x
    heads = ["字段", "类型", "说明"]
    for i,ht in enumerate(heads):
        box(s, hx, y, cw[i], rh, fill=headcol, shape=MSO_SHAPE.RECTANGLE)
        txt(s, hx+0.08, y, cw[i]-0.1, rh, [[(ht, 11, C_WHITE, True)]], anchor=MSO_ANCHOR.MIDDLE)
        hx += cw[i]
    yy = y + rh
    for ri,(f, t, d, rc) in enumerate(rows):
        bg = C_WHITE if ri % 2 == 0 else C_LIGHT
        hx = x
        cells = [(f, 10.5, C_DARK, True), (t, 9.5, C_GRAY, False), (d, 9.8, C_GRAY, False)]
        for i,(ct, csz, ccol, cb) in enumerate(cells):
            box(s, hx, yy, cw[i], rh, fill=bg, shape=MSO_SHAPE.RECTANGLE)
            fcol = rc if (i == 0 and rc) else ccol
            txt(s, hx+0.08, yy, cw[i]-0.12, rh, [[(ct, csz, fcol, cb)]], anchor=MSO_ANCHOR.MIDDLE, space=0.95)
            hx += cw[i]
        yy += rh

# ============ Slide 9 属性详解① 状态与调度字段 ============
s = slide()
title_bar(s, "ATTRIBUTES ① / 属性详解", "PipeTrayModel 字段：状态 · 调度 · 优先级", C_GREEN)
cw = (2.35, 1.55, 8.2)
rows1 = [
    ("status", "ObsField<String>", "托盘状态 free/acking/running/finish/error — 驱动调度主循环的真正开关", C_GREEN),
    ("flagstatus", "ObsField<Integer>", "操作标记 FLAG_FREE/RUN/PAUSE/STOP — 约束UI交互(能否新增/暂停/停止)", C_PURPLE),
    ("urgent", "ObsField<Boolean>", "加急标志 — checktrayurgent 段级插队(加急RNA/PCR先于普通)", C_ORANGE),
    ("dispatchindex", "ObsField<Integer>", "调度游标(从1)，运行到第几步；定位当前 DispatchModel", C_BLUE),
    ("dispatch", "ObsField<Dispatch>", "当前调度步引用，供UI/调度读取(getCurrentDispatch 写入)", C_BLUE),
    ("dispatchlist", "ObsArrayList", "调度清单(任务队列)，createDispatchList 生成，串起 RNA→PCR", C_BLUE),
    ("errorDispatchindex", "ObsField<Integer>", "出错步索引，errorDispatch 记录，checktrayerror 据此定位收尾", C_ORANGE),
    ("createTime", "ObsField<Long>", "提交时间(保存时=now)，updatePriority 升序排队，先提交先派", C_GRAY),
    ("exist", "ObsField<Boolean>", "在位传感器，refreshConnectinfo 用 PAN1..8 传感器刷新", C_GRAY),
    ("runningtime", "ObsField<String>", "运行时间显示(UI)", C_GRAY),
    ("index", "ObsField<Integer>", "界面唯一ID(托盘位 1-8)", C_GRAY),
    ("name", "ObsField<String>", "托盘名称", C_GRAY),
]
attr_table(s, 0.6, 1.55, 12.1, rows1, C_GREEN, cw)

# ============ Slide 10 属性详解② 样本与模板字段 ============
s = slide()
title_bar(s, "ATTRIBUTES ② / 属性详解", "PipeTrayModel 字段：批次 · 样本 · 模板", C_TEAL)
rows2 = [
    ("batch", "ObsField<String>", "批次号，保存时写入，下发给 RNA/PCR 模块(msg.batch)", C_TEAL),
    ("project", "ObsField<String>", "项目名 = 所选 TemplateModel.name", C_TEAL),
    ("templatenumber", "ObsField<String>", "检测项目号(如 KP03)，由试剂码解析，参与文件名", C_TEAL),
    ("enddate", "ObsField<String>", "有效期(试剂保质期, 如 260112)，扫码校验用", C_GRAY),
    ("template", "ObsField<Template>", "模板对象 TemplateModel：含 hex/prj 路径、类型、CT/峰判定", C_ORANGE),
    ("holes", "ObsArrayList<Hole>", "8个样本孔 SampleHoleModel(扫码解析：样本号/试剂码/有效期)", C_ORANGE),
    ("wellInfolist", "ObsArrayList<Well>", "病人信息 WellInfo(孔名 A1-A8 / 身份证号 / 试剂码)，下发PCR实验", C_ORANGE),
    ("deviceID", "ObsField<String>", "设备ID(托盘沿用基类默认，非托盘核心字段)", C_GRAY),
    ("deviceType", "ObsField<String>", "设备类型(同上)", C_GRAY),
]
attr_table(s, 0.6, 1.55, 12.1, rows2, C_TEAL, (2.35, 1.7, 8.05))
# 右下补充关联模型
box(s, 0.6, 6.15, 12.1, 1.0, fill=C_LIGHT, radius=0.08)
txt(s, 0.85, 6.22, 11.6, 0.9, [
    [("关联模型：", 11, C_DARK, True),
     ("DispatchModel(步:targettype/target/nextdevice/firstSecond/savepcr) · TemplateModel(hex/prj/类型) · ", 10, C_GRAY, False)],
    [("SampleHoleModel(孔号/样本号/试剂码/有效期/canuse) · WellInfo(孔名/身份证/试剂码) — 8孔样本经扫码解析后填充", 10, C_GRAY, False)],
    ], anchor=MSO_ANCHOR.MIDDLE, space=1.1)

# ---------- 字段详解卡片 ----------
def field_card(s, x, y, w, h, name, ftype, col, lines):
    box(s, x, y, w, h, fill=C_WHITE, line=col, line_w=1.3, radius=0.05)
    box(s, x, y, w, 0.5, fill=col, shape=MSO_SHAPE.RECTANGLE)
    txt(s, x+0.15, y+0.02, w-1.9, 0.46,
        [[(name, 12.5, C_WHITE, True)]], anchor=MSO_ANCHOR.MIDDLE)
    txt(s, x+w-1.85, y+0.02, 1.7, 0.46,
        [[(ftype, 8.5, C_WHITE, False)]], align=PP_ALIGN.RIGHT, anchor=MSO_ANCHOR.MIDDLE)
    paras = []
    for label, text in lines:
        paras.append([(label+" ", 9.5, col, True), (text, 9.5, C_GRAY, False)])
    txt(s, x+0.15, y+0.58, w-0.28, h-0.62, paras, space=1.12)

def field_slide(title, kicker, color, cards):
    s = slide()
    title_bar(s, kicker, title, color)
    xs = [0.55, 6.75]
    ys = [1.5, 4.35]
    for i, c in enumerate(cards):
        x = xs[i % 2]; y = ys[i // 2]
        field_card(s, x, y, 6.05, 2.6, c[0], c[1], c[2], c[3])
    return s

FT = C_TEAL; FO = C_ORANGE; FP = C_PURPLE; FB = C_BLUE; FG = C_GREEN; FGr = C_GRAY

# ===== 字段详解 1：标识与状态核心 =====
field_slide("字段详解①：标识与状态核心", "FIELD DETAIL / 属性精讲", FB, [
    ("index", "ObservableField<Integer>", FGr, [
        ("含义", "界面唯一ID，对应物理托盘位 1~8"),
        ("默认", "构造时由 initModel(index,name) 传入"),
        ("读取", "几乎所有调度方法的入参 trayindex"),
        ("作用", "traylist 定位、saveModel/LED/dispatch 均按此寻址"),
    ]),
    ("name", "ObservableField<String>", FGr, [
        ("含义", "托盘显示名称，如 \"托盘1\""),
        ("写入", "initModel() 设置；随 index 一并传入"),
        ("读取", "TrayDetailFragment 保存提示、UI 列表展示"),
        ("作用", "纯展示，不参与调度判定"),
    ]),
    ("status", "ObservableField<String>", FG, [
        ("含义", "托盘生命周期状态：free/acking/running/finish/error"),
        ("写入", "保存→str_RUN(:196)；完成→str_FINISH；异常→str_ERROR"),
        ("读取", "checktrayend/checktray*work 以 str_RUN 为门槛"),
        ("作用", "★真正驱动调度主循环的开关，非running不进推进分支"),
    ]),
    ("flagstatus", "ObservableField<Integer>", FP, [
        ("含义", "用户操作标记 FLAG_FREE/RUN/PAUSE/STOP(0/1/2/3)"),
        ("写入", "保存→RUN；暂停→PAUSE；停止→STOP；结束/清除→FREE"),
        ("读取", "TrayManageFragment 新增/暂停按钮的前置判断"),
        ("作用", "约束UI交互(仅FREE可新增)，与status成对写入"),
    ]),
])

# ===== 字段详解 2：调度核心 =====
field_slide("字段详解②：调度游标与清单", "FIELD DETAIL / 属性精讲", FB, [
    ("dispatchindex", "ObservableField<Integer>", FG, [
        ("含义", "调度游标，指向 dispatchlist 中当前执行到的步序(从1)"),
        ("默认", "init()=1"),
        ("写入", "checktrayend +1(:760/781/802)；updateDsiapatchNext 落位+1"),
        ("作用", "getCurrentDispatch 按此取当前步 dm 分派设备"),
    ]),
    ("dispatchlist", "ObservableArrayList<DispatchModel>", FG, [
        ("含义", "调度清单=整盘任务队列，每元素为一个调度步"),
        ("写入", "保存时 createDispatchList() 按模板类型生成(2/4/1步)"),
        ("读取", "checktrayend 遍历、按 index 定位当前步"),
        ("作用", "★流水线任务的真正载体，串起 RNA→PCR 全流程"),
    ]),
    ("dispatch", "ObservableField<DispatchModel>", FGr, [
        ("含义", "\"当前步\"引用，指向 dispatchlist 中正在执行的那一步"),
        ("写入", "setCurrentDispatch / updateDsiapatchNext(:316) 更新"),
        ("读取", "UI 曲线页(PcrCurveFragment)、状态展示"),
        ("作用", "供 UI 与调度快速读当前步，避免每次遍历查找"),
    ]),
    ("errorDispatchindex", "ObservableField<Integer>", FO, [
        ("含义", "记录出错发生在第几个调度步"),
        ("默认", "init()=0；保存时复位为0"),
        ("写入", "errorDispatch()(Tray:161) 设备通知托盘异常时记录"),
        ("作用", "checktrayerror 按此定位出错步做\"取回托盘\"收尾"),
    ]),
])

# ===== 字段详解 3：优先级与传感 =====
field_slide("字段详解③：优先级 · 传感 · 时间", "FIELD DETAIL / 属性精讲", FB, [
    ("urgent", "ObservableField<Boolean>", FO, [
        ("含义", "加急标志，决定是否插队抢占机械臂"),
        ("默认", "init()=false"),
        ("写入", "托盘详情加急复选框 setUrgent(isChecked)(:298)"),
        ("作用", "checktrayurgent 前两段(加急RNA/PCR)优先于普通盘"),
    ]),
    ("createTime", "ObservableField<Long>", FP, [
        ("含义", "托盘提交时刻(毫秒)，用于排队"),
        ("默认", "init()=0L"),
        ("写入", "保存时 System.currentTimeMillis()(:202)"),
        ("作用", "updatePriority 按其升序排 traylist —— 先提交先派"),
    ]),
    ("exist", "ObservableField<Boolean>", FGr, [
        ("含义", "托盘在位传感器状态(是否放在托盘架上)"),
        ("默认", "init()=false"),
        ("写入", "refreshConnectinfo 读 PAN1~PAN8 传感器刷新(Tray:369)"),
        ("作用", "停止时判断盘是否需要机械臂取回、UI 在位显示"),
    ]),
    ("runningtime", "ObservableField<String>", FGr, [
        ("含义", "托盘运行时长显示串"),
        ("写入", "运行过程中根据 starttime 计算刷新"),
        ("读取", "UI 展示当前盘已运行时间"),
        ("作用", "纯展示，不参与调度判定"),
    ]),
])

# ===== 字段详解 4：批次与检测项目 =====
field_slide("字段详解④：批次与检测项目", "FIELD DETAIL / 属性精讲", FB, [
    ("batch", "ObservableField<String>", FT, [
        ("含义", "批次号，唯一标识一次上样批次"),
        ("写入", "保存时 trayModel.batch.set(batch)(:197)"),
        ("读取", "生成 dispatchlist 时写入每个 dm.batch"),
        ("作用", "随 RNAReq/PCRReq 下发设备、结果归档、曲线页标注"),
    ]),
    ("project", "ObservableField<String>", FT, [
        ("含义", "项目名，取自所选模板名 mtemplateModel.name"),
        ("写入", "保存时 project.set(mtemplateModel.name)(:198)"),
        ("读取", "ExecuteRNA/PCR 组装 Template=projectname+\"-\"+模板"),
        ("作用", "定位项目目录、拼接下发模板全名"),
    ]),
    ("templatenumber", "ObservableField<String>", FO, [
        ("含义", "检测项目号(试剂码解析出)，如 \"KP03\""),
        ("默认", "init() 示例值 \"KP03\""),
        ("写入", "扫码后 set(getTemplateNumber())(:502)"),
        ("作用", "checkPatient 校验非空才允许保存运行(:364)"),
    ]),
    ("enddate", "ObservableField<String>", FO, [
        ("含义", "试剂有效期(试剂码解析)，如 \"260112\""),
        ("默认", "init() 示例值 \"260112\""),
        ("写入", "扫码后 set(getEnddate())(:503)"),
        ("作用", "checkPatient 校验非空、判定试剂是否过期"),
    ]),
])

# ===== 字段详解 5：样本 · 模板 · 设备 =====
field_slide("字段详解⑤：样本 · 模板 · 设备", "FIELD DETAIL / 属性精讲", FB, [
    ("holes", "ObservableArrayList<SampleHoleModel>", FT, [
        ("含义", "8孔样本录入模型(孔号/样本号/试剂码/扫码信息)"),
        ("写入", "init 建8个；扫码 ScanCodeHelper.parseSampleHoleModel"),
        ("读取", "TrayDetailFragment 校验、UI 孔位列表适配器"),
        ("作用", "录入/校验界面用；解析出 canuse、项目号、有效期"),
    ]),
    ("wellInfolist", "ObservableArrayList<WellInfo>", FT, [
        ("含义", "孔位病人信息(孔名/身份证号/试剂卡条码)"),
        ("写入", "init 建8个；扫码后按孔名回填(:604)"),
        ("读取", "PCRManager.getWells(wellInfos,...)(:553/888)"),
        ("作用", "★下发 PCR 实验 Experiment 的孔位样本来源"),
    ]),
    ("template", "ObservableField<TemplateModel>", FGr, [
        ("含义", "所选模板对象(类型/hex/prj/CT/峰型/总时长)"),
        ("写入", "保存时 template.set(mtemplateModel)(:199)"),
        ("读取", "createDispatchList 依 templateType 决定拆分方式"),
        ("作用", "决定 single/multi/rna/pcr 四种调度表结构"),
    ]),
    ("deviceID / deviceType", "ObservableField<String>", FGr, [
        ("含义", "设备标识/类型(托盘模型中基本留空)"),
        ("写入", "PipeTrayModel 未主动设置(PCR/RNA/Arm模型才用)"),
        ("读取", "—"),
        ("作用", "沿用基类字段，托盘场景基本不参与调度"),
    ]),
])

# ---------- 步字段精讲卡片 (6 卡, 2列x3行) ----------
def attr_slide2(title, kicker, color, cards):
    s = slide()
    title_bar(s, kicker, title, color)
    xs = [0.55, 6.75]
    ys = [1.5, 3.34, 5.18]
    for i, c in enumerate(cards):
        x = xs[i % 2]; y = ys[i // 2]
        name, sub, col, desc = c
        box(s, x, y, 6.05, 1.72, fill=C_WHITE, line=col, line_w=1.2, radius=0.05)
        box(s, x, y, 0.18, 1.72, fill=col, shape=MSO_SHAPE.RECTANGLE)
        txt(s, x+0.35, y+0.1, 5.55, 0.42,
            [[(name, 12.5, C_DARK, True), ("   "+sub, 9.5, col, True)]])
        txt(s, x+0.35, y+0.55, 5.5, 1.1, [[(desc, 9.8, C_GRAY, False)]], space=1.12)
    return s

# ============ DispatchModel 概览：一步的生命周期 ============
s = slide()
title_bar(s, "DISPATCH MODEL / 调度步", "DispatchModel：dispatchlist 中的一个调度步", C_GREEN)

txt(s, 0.6, 1.35, 12.2, 0.5, [[("一个 DispatchModel = 整盘任务队列(PipeTrayModel.dispatchlist)中的一个\"步\"；createDispatchList 按模板类型拆成 1/2/4 步", 12, C_GRAY, False)]])

# 生命周期状态条
lc = [("STATUS_FREE", "待分配", C_GRAY),
      ("STATUS_RUN", "已占设备/执行中", C_BLUE),
      ("STATUS_FINISH", "完成→游标+1", C_GREEN),
      ("STATUS_ERROR", "异常收尾", C_ORANGE)]
x = 0.6
for name, d, col in lc:
    box(s, x, 1.95, 2.85, 0.9, fill=col, radius=0.12)
    txt(s, x, 2.03, 2.85, 0.76, [[(name, 13, C_WHITE, True)],[(d, 10, C_LIGHT, False)]],
        align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE, space=1.0)
    if name != "STATUS_ERROR":
        arrow(s, x+2.87, 2.27, 0.26, 0.28, C_GRAY)
    x += 3.1

# 四大关键机制
mechs = [
    ("类型占位 → 实际编号", C_PURPLE,
     "targettype(DEVICE_RNA/PCR，创建即定、恒定)指明设备种类；target(创建为空)在 checktrayRnawork/Pcrwork 分配到 getFreeModel() 时写入具体机器号(1026/1088)"),
    ("RNA→PCR 串联开关", C_BLUE,
     "nextdevice 编码链路方向：checkRnaFinish/checkPcrFinish 据 nextdevice>0 → armStep=WORK_MOVE(移交下游)，=0 → WORK_BACK(送回托盘收尾)"),
    ("firstSecond 程序映射", C_TEAL,
     "single 恒=1；multi 前段=1/后段=2；纯RNA/PCR=0。PCR 侧 1/2→\"first\"/\"second\"，据此选 prj Program 与对应时长"),
    ("断点续跑锚点", C_ORANGE,
     "phase/phasestep(RNA)、circle/runtime(PCR)作为续跑起点传给设备，本版本运行期不回写恒为0；RNA 实时续跑另由 resumeRna 从设备 NO 串解析"),
]
xs = [0.6, 6.75]; ys = [3.2, 5.2]
for i,(t,col,d) in enumerate(mechs):
    x = xs[i%2]; y = ys[i//2]
    box(s, x, y, 5.95, 1.85, fill=C_WHITE, line=col, line_w=1.3, radius=0.06)
    box(s, x, y, 0.2, 1.85, fill=col, shape=MSO_SHAPE.RECTANGLE)
    txt(s, x+0.4, y+0.14, 5.4, 0.4, [[(t, 13, C_DARK, True)]])
    txt(s, x+0.4, y+0.62, 5.4, 1.15, [[(d, 10.5, C_GRAY, False)]], space=1.12)

# ============ DispatchModel 字段详解① 定位/模板/状态 ============
attr_slide2("DispatchModel 字段①：定位 · 模板 · 状态", "DISPATCH FIELD / 步字段精讲", C_GREEN, [
    ("index", "步序号(从1)", C_GREEN, "写:createDispatchList硬编码1/2… · 读:getCurrentDispatch用dispatchindex==index定位当前步(Tray:286) · 步游标锚点，断点续跑/错误定位基础"),
    ("templatename", "模板文件名", C_TEAL, "写:从TemplateModel取single_hex/prj、multi_*、rna_hex、pcr_prj · 读:RNAManager/PCRManager据 path+templatename 读hex/prj下发 · 决定\"跑什么\""),
    ("templateno", "模板代码(默认00)", C_GRAY, "写:来自mtemplateModel.templateno · 读:PcrCurveFragment结果命名(空兜底KK00) · 结果文件/批号命名，调度不依赖"),
    ("status ★", "步调度状态", C_BLUE, "FREE待分配/RUN执行中/FINISH触发dispatchindex+1/ERROR异常。由updateDsiapatchStatus集中写，checktrayend据FINISH推进游标(758-810)"),
    ("batch", "批号", C_ORANGE, "写:createBatchCode(PAN{tray}-时间-模板号-名称) · 读:贯穿RNA/PCR下发的batchID + 结果文件命名"),
    ("path / projectname", "目录 / 项目名", C_PURPLE, "path→prj/hex加载根目录(SLANService.getProjects) · projectname与templatename拼成 projectname-template 设备端完整标识"),
])

# ============ DispatchModel 字段详解② 设备定位 ============
attr_slide2("DispatchModel 字段②：设备定位与串联", "DISPATCH FIELD / 步字段精讲", C_GREEN, [
    ("targettype ★", "目标设备类型", C_PURPLE, "DEVICE_RNA(1)/DEVICE_PCR(2)，创建即定恒定。决定这步\"去哪种设备\"；checktrayend/error/createArmTaskGetTray 据它选RNA/PCR分支(注:与TYPE_*数值相同混用)"),
    ("target ★", "实际设备编号", C_BLUE, "创建为空(null)。首步分配 dm.target=rm.index(1026/1088)，串联步 updateDsiapatchNext 由到位目标位写入(Tray:309)。把\"RNA类\"落到\"RNA第2号机\""),
    ("nextdevice ★", "下一设备", C_ORANGE, ">0 → 跑完 armStep=WORK_MOVE 移交下游；=0 → WORK_BACK 送回收尾。single:RNA→PCR→0；multi:RNA→PCR→RNA→PCR→0。找不到空闲下游则降级WORK_BACK"),
    ("tray", "托盘号(1-8)", C_TEAL, "写:来自trayindex · 读:setCurrentTemplate回填设备currentTray、createArmTaskGetTray取源盘、PCR execute据它取wellInfolist孔位样本"),
    ("firstSecond ★", "第一/二套程序", C_GREEN, "0/1/2。single恒1、multi前段1后段2、纯RNA/PCR为0。PCR侧 1→first.prj、2→second.prj，project.getName().equals(tempflag)选Program"),
    ("totaltime", "总时长(TotalTime)", C_GRAY, "按firstSecond取getFirstRNATime/getSecondRNATime(RNA)或getFirstPcrTime/getSecondPcrTime(PCR)，下发设备作进度/超时判断"),
])

# ============ DispatchModel 字段详解③ 续跑/时间/结果 ============
attr_slide2("DispatchModel 字段③：续跑 · 时间 · 结果", "DISPATCH FIELD / 步字段精讲", C_GREEN, [
    ("phase / phasestep", "RNA续跑锚点(默认0)", C_TEAL, "写:createDispatchList全设0，运行期不回写恒为0 · 读:ExecuteRNA作CurrentPhase/CurrentPhaseStep续跑起点。实时续跑走resumeRna从设备NO串split解析"),
    ("circle / runtime", "PCR续跑锚点(默认0)", C_ORANGE, "写:全设0恒为0(设备实时进度存PipePCRModel而非此) · 读:ExecutePCR作CurrentCycle/RunningTime。续跑写回本版本未启用，总从0跑"),
    ("starttime / stoptime", "步起止时间", C_PURPLE, "starttime:设备启动成功updateDsiapatchStarttime(1450/1465) · stoptime:步完成updateDsiapatchStoptime(1537/1617)，均触发saveModel持久化"),
    ("devicestatus ★", "用户可见执行状态", C_BLUE, "与status(调度层)解耦。free/running/finish/error给用户看，避免调度中间态干扰UI。checktrayerror据它==ERROR进异常收尾(930)"),
    ("savepcr ★", "末步保存PCR结果", C_GREEN, "仅整条流水线最后一个PCR步为true(single m2/multi m4/纯PCR m1)。checkSavePcr把关，只在最后一次PCR落盘，避免multi中间PCR误存"),
    ("selected / indexname / name / createtime", "UI/记录", C_GRAY, "selected:列表选中高亮 · indexname:index字符串 · name:步骤名(核酸提取/第一次PCR) · createtime:创建记录 — 均无调度作用"),
])

# ============ 关联属性类 TemplateModel / SampleHoleModel / WellInfo ============
s = slide()
title_bar(s, "RELATED MODELS / 关联类", "TemplateModel · SampleHoleModel · WellInfo", C_PURPLE)

def relbox(x, y, w, h, title, col, lines):
    box(s, x, y, w, h, fill=C_WHITE, line=col, line_w=1.3, radius=0.05)
    box(s, x, y, w, 0.46, fill=col, shape=MSO_SHAPE.RECTANGLE)
    txt(s, x+0.15, y+0.02, w-0.25, 0.42, [[(title, 12.5, C_WHITE, True)]], anchor=MSO_ANCHOR.MIDDLE)
    paras = [[(lab+" ", 9.5, col, True),(t, 9.5, C_GRAY, False)] for lab,t in lines]
    txt(s, x+0.15, y+0.54, w-0.28, h-0.6, paras, space=1.14)

relbox(0.55, 1.5, 4.0, 5.35, "TemplateModel (模板)", C_TEAL, [
    ("角色", "模板文件内容载体，决定 createDispatchList 拆几步"),
    ("templateType", "1-single 2-multi 3-rna 4-pcr"),
    ("selectType", "0-full 1-rna 2-pcr"),
    ("path", "项目地址(hex/prj根目录)"),
    ("single_hex/prj", "single流程 核提hex+PCR prj"),
    ("multi_*_first/second", "multi双组 各自hex/prj"),
    ("rna_hex", "纯RNA模板"),
    ("pcr_prj", "纯PCR模板"),
    ("totaltime", "TotalTime 各阶段时长"),
    ("usect/ct/usepeak/peak", "Ct/熔解峰判读阈值"),
    ("temparatureCtrl", "0-fast 1-standard 温控模式"),
])
relbox(4.75, 1.5, 4.0, 5.35, "SampleHoleModel (孔)", C_ORANGE, [
    ("角色", "8孔样本+试剂码扫码解析结果"),
    ("index", "孔号 1-8"),
    ("serialNum", "样本号(身份证号)"),
    ("reagentCode", "试剂卡条码"),
    ("scaninfo", "扫码原始/比对信息"),
    ("status", "孔状态(0空/1有样)"),
    ("produceddate/year/month", "试剂码解析:生产日期/质保/月份"),
    ("info/infopath/infotemplate", "试剂码解析:项目号/目录/模板号"),
    ("enddate", "试剂有效期"),
    ("canuse", "是否在保质期内"),
    ("说明", "ScanCodeHelper.parseSampleHoleModel 解析填充"),
])
relbox(8.95, 1.5, 4.0, 5.35, "WellInfo (下发孔)", C_BLUE, [
    ("角色", "精简三元组，下发给PCR设备的孔位样本"),
    ("wellName", "孔名 A1-A8"),
    ("sampleName", "样本名(身份证号)"),
    ("reagentCode", "试剂卡条码"),
    ("数据流", "holes(扫码解析) → wellInfolist(精简)"),
    ("→ PCR", "PCRManager.getWells(wellInfos,prj)"),
    ("→", "生成 Well 列表填入 Experiment"),
    ("→", "setExperimentWells 随 PCRReq 下发"),
    ("区别", "holes重界面/校验，WellInfo重下发"),
    ("对比", "holes(SampleHoleModel)=录入态"),
    ("", "wellInfolist(WellInfo)=下发态"),
])

# ============ ArmManager 概览 ============
s = slide()
title_bar(s, "ARM MANAGER / 机械臂管理器", "ArmManager：门控 + 位置 + 回调 + 续跑", C_PURPLE)

txt(s, 0.6, 1.32, 12.2, 0.4, [[("单臂(index=1)调度门面，本身几乎不持状态——真正门控存于 PipeArmModel(getArmModel(1))，动作全下沉到 RobotWorkManager / SerialPortManager_Robot", 11, C_GRAY, False)]])

# armCanuse 门控链
box(s, 0.6, 1.85, 12.15, 1.35, fill=C_WHITE, line=C_PURPLE, line_w=1.4, radius=0.06)
txt(s, 0.85, 1.95, 11.7, 0.38, [[("armCanuse() 门控判定顺序 (全满足才允许下发)", 13, C_PURPLE, True)]])
gate = [("model≠null", C_GRAY), ("connect=true", C_TEAL), ("status==FREE", C_BLUE),
        ("finish=true", C_ORANGE), ("exist=false", C_GREEN)]
x = 0.9
for name, col in gate:
    box(s, x, 2.45, 2.05, 0.62, fill=col, radius=0.18)
    txt(s, x, 2.5, 2.05, 0.52, [[(name, 11, C_WHITE, True)]], align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    if name != "exist=false":
        arrow(s, x+2.07, 2.66, 0.22, 0.24, C_GRAY)
    x += 2.36
txt(s, 0.9, 3.1, 11.6, 0.3, [[("软锁 finish(占用锁) + 硬状态 connect/status/exist 组合；armSelfcheckCanuse() 同上但不校验 exist(自检允许手上有物)", 9.5, C_ORANGE, True)]])

# 四大机制
am = [("成对位置 source/target", C_BLUE, "itemProperty_source(从哪取)+itemProperty_target(放到哪)描述一次搬运。startArmRobot 限制合法组合为 PAN+PCR 或 PAN+RNA(禁 PAN+PAN/PCR+PCR/RNA+RNA)"),
      ("双回调分流结果", C_TEAL, "armFinishCallback(搬运 finish/fail，带源/目标位点) + armResetCallback(复位 finish)。init() 注册 WorkQueueListener 桥接底层队列结果并按 RESET_EXCEPTION 分流"),
      ("lastAction 断点续跑", C_ORANGE, "lastAction 格式 动作:源:目标(外部注入)。errorContinue() 解析出临时源/目标 → resumeRobotWork，成功后置 currentArmworkMessage.dispatchindex=RUN 重试"),
      ("动作全部下沉", C_PURPLE, "startArmRobot/putTray/putPcrCover/openRNADoor/closeRNADoor → RobotWorkManager；状态来自 SerialPortManager_Robot.getRobotStatus() 经 refreshModel 灌入模型")]
xs = [0.6, 6.75]; ys = [3.5, 5.35]
for i,(t,col,d) in enumerate(am):
    x = xs[i%2]; y = ys[i//2]
    box(s, x, y, 5.95, 1.72, fill=C_WHITE, line=col, line_w=1.3, radius=0.06)
    box(s, x, y, 0.2, 1.72, fill=col, shape=MSO_SHAPE.RECTANGLE)
    txt(s, x+0.4, y+0.12, 5.4, 0.38, [[(t, 12.5, C_DARK, True)]])
    txt(s, x+0.4, y+0.55, 5.4, 1.1, [[(d, 10, C_GRAY, False)]], space=1.1)

# ============ ArmManager / PipeArmModel 字段 ============
attr_slide2("ArmManager 字段：类成员 + PipeArmModel 门控", "ARM FIELD / 属性精讲", C_PURPLE, [
    ("itemPropertyList", "位点总表", C_PURPLE, "机械臂可达位点(TRAY/RNA/PCR/PCRCover/PAN)。从 SerialPortManager_Robot 拉取，getItemProperty* 每次刷新，是一切位点解析的数据源"),
    ("itemProperty_source/target ★", "源/目标位置", C_BLUE, "成对语义：source=取，target=放。setSource/setTarget 按设备类型解析写入；setFree() 置 null 释放。startArmRobot 校验合法组合"),
    ("armlist / getArmModel(1)", "状态载体", C_TEAL, "PipeArmModel 承载全部实时门控与显示字段(本机型仅1台 index=1)。所有 update*/save*/armCanuse 遍历它。经 MMKV 持久化"),
    ("armFinishCallback / armResetCallback", "结果回调", C_ORANGE, "setcallback() 把队列结果转 finish(1,target,source)/fail；成功还调 setFree() 释放位点。RESET_EXCEPTION 走 reset 回调"),
    ("lastAction / lastDetail", "续跑依据", C_GREEN, "lastAction=动作:源:目标(外部注入，本类无写入点)。errorContinue 强依赖，为 null 或不含冒号直接返回错误。lastDetail 预留"),
    ("PipeArmModel: connect/status/finish/exist ★", "门控字段", C_GRAY, "connect 连接 · status free/running/error · finish 软占用锁 · exist 抓手是否夹持物体 · pcrcovertemp 暂存位占用。refreshModel 从 RobotStatus 刷新"),
])

# ============ RNAManager / PipeRNAModel 字段 ============
attr_slide2("RNAManager 字段：核提模组(2台)管理器", "RNA FIELD / 属性精讲", C_TEAL, [
    ("mInstance / seqid", "单例 / 指令序号", C_GRAY, "单例全局唯一 RNA 调度入口。seqid 自增(近MAX归0)，每条下发消息取唯一序号供请求-应答匹配(非线程安全，靠单例串行规避)"),
    ("rnalist ★", "设备模型列表", C_TEAL, "2台核提(index=1/2)的 PipeRNAModel 集合，调度数据中枢。按 index 查设备、getFreeModel 找空闲、refreshConnectinfo 刷新全基于它"),
    ("rnaErrorCallback", "异常回调", C_ORANGE, "createAlarm 检测 alarm/linuxAlarm/sendAlarm 非空 → 上抛 ErrorInfoMessage(type=DEVICE_RNA) 给 UI/告警系统"),
    ("armStep ★", "状态机主状态", C_PURPLE, "0空闲/1取盘WORK_GET/2跑模板WORK_RUN/3送盘WORK_BACK/4移盘WORK_MOVE。setCurrentTemplate 置 GET，finishBacktray/Moveouttray 完成置 FREE"),
    ("getFreeModel 5条件 ★", "空闲判定", C_BLUE, "connect && !exist && armStep==FREE && status==free && linuxStatus==free，缺一不可。逐台打诊断日志便于排查\"为何无空闲\""),
    ("currentTray + start/finish 握手", "绑定 + 阶段推进", C_GREEN, "currentTray 绑托盘(getCurrentTemplate 反查调度)。gettray/excute/movetray/backtray/opendoor 各 start/finish 布尔位驱动取→跑→移/送握手"),
])

# ============ PCRManager / PipePCRModel 字段 ============
attr_slide2("PCRManager 字段：PCR模组(6台)管理器", "PCR FIELD / 属性精讲", C_ORANGE, [
    ("pcrlist ★", "设备模型列表", C_ORANGE, "6台PCR(index=1-6)的 PipePCRModel 集合，调度状态机核心。遍历找空闲、按index定位某台状态"),
    ("pcrExperimentMap ★", "实验/荧光缓存", C_BLUE, "ConcurrentHashMap<index,Experiment> 线程安全。refreshModel 收 status.getExperiment() 落此，承载分析后的曲线/结果供 UI 曲线页读取"),
    ("getFreeModel 5条件 ★", "空闲判定", C_TEAL, "connect && !exist(机内无盘) && armStep==WORK_FREE && status==free && linuxStatus==free。命中返回 PipePCRModel 供分盘"),
    ("firstSecond → prj 程序 ★", "一/二套映射", C_PURPLE, "1→\"first\"、2→\"second\"，SLANService.getProjects 取 name==tempflag 的 Project/Program 下发；并选 getFirst/SecondPcrTime 时长"),
    ("execute → PCRReqMessage", "组装下发链", C_GREEN, "getWells(wellInfolist→Well) → Experiment(Project+Wells) → PCRReqMessage(Template=项目-模板) → sendPCRTemplateWSSMessage"),
    ("degree/degreeTarget + savepcr", "温度 / 结果落盘", C_GRAY, "degree/degreeTarget 实时温度 UI 直显。曲线走 PCRCurveMessage→PipeMainActivity；savepcr(末步)经 checkSavePcr 把关只在最后PCR落盘"),
])

# ============ PipeArmModel 概览 ============
s = slide()
title_bar(s, "ARM MODEL / 机械臂模型", "PipeArmModel：机械臂门控状态与任务载体", C_PURPLE)

txt(s, 0.6, 1.35, 12.2, 0.5, [[("ArmManager 本身几乎不持状态，真正的门控标志全在 PipeArmModel(getArmModel(1))；单臂 index=1，init() 默认 status=error/finish=true/exist=false/armwork=ARM_FREE", 11.5, C_GRAY, False)]])

# armCanuse 门控链
txt(s, 0.6, 1.95, 12, 0.4, [[("armCanuse() 门控判定链（全满足才允许下发搬运）", 14, C_PURPLE, True)]])
gate = [("connect", "已连接", C_GRAY),
        ("status==FREE", "空闲态", C_BLUE),
        ("finish==true", "软锁:上任务完成", C_GREEN),
        ("exist==false", "手上无物", C_ORANGE)]
x = 0.6
for name, d, col in gate:
    box(s, x, 2.5, 2.85, 0.9, fill=col, radius=0.12)
    txt(s, x, 2.58, 2.85, 0.76, [[(name, 13, C_WHITE, True)],[(d, 10, C_LIGHT, False)]],
        align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE, space=1.0)
    if name != "exist==false":
        arrow(s, x+2.87, 2.82, 0.26, 0.28, C_GRAY)
    x += 3.1
txt(s, 0.6, 3.45, 12, 0.35, [[("armSelfcheckCanuse() 自检时同上但不校验 exist（允许手上有物）", 10.5, C_GRAY, False)]])

# 三大机制
mechs = [
    ("软锁 finish + 硬状态", C_PURPLE,
     "finish 是搬运软占用锁：startArmRobot 前置 updateFinish(false) 上锁，队列完成 onAllTasksCompleted 置 true 解锁；connect/status/exist 是来自 RobotStatus 的硬状态"),
    ("成对 source/target 位点", C_BLUE,
     "itemProperty_source/target 描述一次搬运，仅允许 PAN+PCR / PAN+RNA(禁 PAN+PAN 等)；ItemProperty 含 Type/Position/X/Y/Z 坐标，setFree() 置 null 释放"),
    ("armwork 任务类型标记", C_TEAL,
     "ARM_FREE0/RESET1/CONTINUE2/START3/STOP4/GET5/PUT6/CLOSERNA7/OPENRNA8/PCRCOVER9/TEMPPCRCOVER10/PUTTRAY11，标记当前动作语义供 UI + 续跑"),
    ("续跑 currentArmworkMessage", C_ORANGE,
     "承载当前 ArmworkMessage(含 dispatchindex)；errorContinue 解析外部注入的 lastAction(动作:源:目标) + 取 dispatchindex 重试 resumeRobotWork"),
]
xs = [0.6, 6.75]; ys = [3.85, 5.5]
for i,(t,col,d) in enumerate(mechs):
    x = xs[i%2]; y = ys[i//2]
    box(s, x, y, 5.95, 1.5, fill=C_WHITE, line=col, line_w=1.3, radius=0.06)
    box(s, x, y, 0.2, 1.5, fill=col, shape=MSO_SHAPE.RECTANGLE)
    txt(s, x+0.4, y+0.1, 5.4, 0.4, [[(t, 12.5, C_DARK, True)]])
    txt(s, x+0.4, y+0.52, 5.4, 0.95, [[(d, 10, C_GRAY, False)]], space=1.1)

# ============ PipeArmModel 字段详解① 门控/位点 ============
attr_slide2("PipeArmModel 字段①：门控与位点", "ARM FIELD / 属性精讲", C_PURPLE, [
    ("connect ★", "是否连接(默认false)", C_GRAY, "写:refreshModel 来自 RobotStatus · 读:armCanuse/armSelfcheckCanuse 第一关，未连接不可用"),
    ("status ★", "free/running/error", C_BLUE, "写:refreshModel(默认init=error) · 读:armCanuse 要求==str_FREE 才可下发"),
    ("finish ★", "上任务完成(软锁)", C_GREEN, "写:updateFinish—startArmRobot前置false上锁、队列完成置true解锁、clearFinish强制解锁 · 读:armCanuse 未完成即视为忙"),
    ("exist ★", "抓手存在传感器", C_ORANGE, "写:refreshModel 来自传感器(手上是否夹持物) · 读:armCanuse 若已exist则不可用(自检除外)"),
    ("sourcePosition / targetPosition", "源/目标位号", C_TEAL, "写:saveSource/saveTarget/refreshModel · 读:界面绑定显示。与 itemProperty_source/target 的 Position 对应"),
    ("pcrcovertemp", "PCR盖暂存位传感器", C_PURPLE, "写:refreshModel · 读:pcrCoverTempExist() 判断暂存位是否已占用(放/取盖流程用)"),
])

# ============ PipeArmModel 字段详解② 任务/续跑/显示 ============
attr_slide2("PipeArmModel 字段②：任务 · 续跑 · 显示", "ARM FIELD / 属性精讲", C_PURPLE, [
    ("armwork ★", "当前任务类型(默认ARM_FREE)", C_TEAL, "写:updateArmwork—startArmRobot置ARM_START、startGet置ARM_GET、robotReset置ARM_RESET、errorContinue置ARM_CONTINUE等 · 读:UI展示+任务语义"),
    ("currentArmworkMessage ★", "当前任务消息", C_ORANGE, "写:updateArmworkMessage · 读:errorContinue 取其 dispatchindex 重试。含 ArmworkMessage 全部搬运上下文"),
    ("tray", "托盘来源记忆", C_BLUE, "写:updateTray(默认0) · 读:startArmRobot 记住来源盘号，供搬运/送回定位"),
    ("autosamplecode", "自动更正样本编码(默认false)", C_GRAY, "写:模型 init · 读:checkHoles(tray,qrlist) 决定扫码回传时是否回写 scaninfo 更正样本位"),
    ("target / source", "目标/源位置(字符串)", C_PURPLE, "写:saveSource/saveTarget/refreshModel · 读:UI 显示当前搬运两端(与 itemProperty 对象区分,这里是展示串)"),
    ("no/degree/alarm/motorID/taskRunning/action", "实时显示", C_GRAY, "refreshModel 从 RobotStatus 灌入：指令号/温度/报警/马达/运行态/机械臂信息 — 纯UI展示,不参与门控"),
])

# ============ 状态更新概览：双通道 ============
s = slide()
title_bar(s, "STATE UPDATE / 状态更新", "四个模型的状态更新：拉取刷新 + 调度推送 双通道", C_GREEN)

txt(s, 0.6, 1.3, 12.2, 0.45, [[("PipeRNAModel · PipePCRModel · PipeArmModel · PipeTrayModel 都靠两条通道改写 ObservableField，DataBinding 自动刷新 UI", 11.5, C_GRAY, False)]])

# 通道A 拉取刷新
box(s, 0.6, 1.95, 6.0, 2.5, fill=C_WHITE, line=C_TEAL, line_w=1.5, radius=0.05)
txt(s, 0.85, 2.05, 5.5, 0.4, [[("通道A  拉取刷新 (外部真实态 → 模型)", 13, C_TEAL, True)]])
txt(s, 0.85, 2.5, 5.55, 1.9, [
    [("触发：", 10.5, C_TEAL, True),("每 5 秒调度心跳 refreshConnectinfo()", 10.5, C_GRAY, False)],
    [("数据源1：", 10.5, C_TEAL, True),("SerialPortManager_Robot 传感器 → exist/cover", 10.5, C_GRAY, False)],
    [("数据源2：", 10.5, C_TEAL, True),("WSSConnectionItem 连接态 → connect/key/ip", 10.5, C_GRAY, False)],
    [("数据源3：", 10.5, C_TEAL, True),("RNA/PCRStatusItem 回包 → refreshModel 灌入", 10.5, C_GRAY, False)],
    [("  status/linuxStatus/degree/告警/进度/version", 10, C_GRAY, False)],
    [("特点：", 10.5, C_ORANGE, True),("覆盖式刷新，不持久化(实时态)", 10.5, C_GRAY, False)],
    ], space=1.12)

# 通道B 调度推送
box(s, 6.75, 1.95, 6.0, 2.5, fill=C_WHITE, line=C_BLUE, line_w=1.5, radius=0.05)
txt(s, 7.0, 2.05, 5.5, 0.4, [[("通道B  调度推送 (调度决策 → 模型)", 13, C_BLUE, True)]])
txt(s, 7.0, 2.5, 5.55, 1.9, [
    [("触发：", 10.5, C_BLUE, True),("APLManager 状态机 + 机械臂完成回调", 10.5, C_GRAY, False)],
    [("入口：", 10.5, C_BLUE, True),("update* / set* / finish* 系列方法", 10.5, C_GRAY, False)],
    [("改写：", 10.5, C_BLUE, True),("armStep 状态 + start/finish 握手位", 10.5, C_GRAY, False)],
    [("  setCurrentTemplate→WORK_GET、finishBacktray→FREE", 9.8, C_GRAY, False)],
    [("  updateExecuteStart/updateFinish/updateDsiapatch*", 9.8, C_GRAY, False)],
    [("特点：", 10.5, C_ORANGE, True),("每次 saveModel 持久化到 MMKV(可恢复)", 10.5, C_GRAY, False)],
    ], space=1.12)

# 底部：两条通道汇合
box(s, 0.6, 4.65, 12.15, 2.45, fill=C_LIGHT, radius=0.06)
txt(s, 0.85, 4.75, 11.6, 0.4, [[("两通道协作：真实态刷新提供\"能否推进\"的判据，调度推送改写\"推进到哪一步\"", 12.5, C_DARK, True)]])
flow = [
    ("① 拉取", C_TEAL, "refreshConnectinfo 刷新 connect/exist/status/linuxStatus"),
    ("② 判据", C_PURPLE, "getFreeModel 用刷新后的字段做 5 条件空闲判定"),
    ("③ 推送", C_BLUE, "分配成功 → setCurrentTemplate 置 armStep=WORK_GET + saveModel"),
    ("④ 回执", C_ORANGE, "机械臂/设备完成回调 → update*/finish* 推进 armStep + saveModel"),
    ("⑤ 收尾", C_GREEN, "finishBacktray/finishMoveouttray 置 armStep=WORK_FREE 回可分配"),
]
yy = 5.2
for tag, col, d in flow:
    box(s, 0.9, yy, 1.5, 0.34, fill=col, radius=0.2)
    txt(s, 0.9, yy+0.01, 1.5, 0.32, [[(tag, 10.5, C_WHITE, True)]], align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    txt(s, 2.55, yy+0.01, 10.0, 0.34, [[(d, 10.5, C_GRAY, False)]], anchor=MSO_ANCHOR.MIDDLE)
    yy += 0.37

# ============ RNA/PCR 模型更新方法 ============
attr_slide2("状态更新①：RNA / PCR 设备模型 (对称)", "STATE UPDATE / 更新方法", C_TEAL, [
    ("refreshConnectinfo(items)", "拉取·5秒心跳", C_TEAL, "读传感器刷 exist/cover，按 deviceType+position 匹配 WSSConnectionItem 算 connect，写 key/ip/port，转 StatusItem 调 refreshModel"),
    ("refreshModel(model,status)", "拉取·回包灌入", C_TEAL, "覆盖式写 linuxStatus/status/degree/告警/进度/version/allstatus(汇总态)，末尾 createAlarm→errorCallback 上抛告警。不 saveModel"),
    ("setCurrentTemplate(idx,dm,st)", "推送·绑定", C_BLUE, "写 currentTray/dispatchindex、armStep=WORK_GET；st=0 待取盘(start/finish=false)、否则已在机内(=true)。saveModel 持久化"),
    ("updateExecuteStart(idx,v)", "推送·启动", C_PURPLE, "置 excutestart=v 并记 startTime；APLManager 以 excutestart==false 作\"该发起运行\"门闩，下发模板后置 true。saveModel"),
    ("finishBacktray / finishMoveouttray", "推送·收尾★", C_ORANGE, "校验 armStep==WORK_BACK/WORK_MOVE 才执行：置对应 finish=true、currentTray=0、armStep=WORK_FREE、uiinit=true。归零回可分配态"),
    ("updateExist/Cover/Starttime/clearDispatch", "推送·单字段", C_GRAY, "updateExist 由传感器刷、clearDispatch 置 armStep=FREE、updateCurrentTray 写 currentTray/dispatchindex — 多数配 saveModel"),
])

# ============ Arm/Tray 模型更新方法 ============
attr_slide2("状态更新②：机械臂模型 / 托盘调度", "STATE UPDATE / 更新方法", C_PURPLE, [
    ("ArmManager.refreshModel(model)", "拉取·RobotStatus", C_TEAL, "从 SerialPortManager_Robot 的 RobotStatus 灌入 connect/status/exist/pcrcovertemp/no/degree/alarm/motorID，并写 source/target 显示串"),
    ("updateFinish(idx,v) ★", "推送·软锁", C_BLUE, "机械臂占用锁：startArmRobot 下发前 updateFinish(false)上锁，onAllTasksCompleted 回调 updateFinish(true)解锁。armCanuse 据它判忙闲"),
    ("updateArmwork/Tray/Action/ArmworkMessage", "推送·任务态", C_PURPLE, "updateArmwork 写当前任务类型(ARM_GET/PUT/START…)、updateTray 记来源盘、updateArmworkMessage 存含 dispatchindex 的任务消息供续跑。saveModel"),
    ("TrayManager.updateDsiapatchStatus", "推送·步状态★", C_ORANGE, "集中写 DispatchModel.status(FREE/RUN/FINISH/ERROR)；checktrayend 据 FINISH 推进 tray.dispatchindex。配套 updateDsiapatchDeviceStatus 写用户可见态"),
    ("updateDsiapatchNext(tray,idx,type,target)", "推送·串联★", C_GREEN, "托盘落位后 dispatchindex+1，绑定下一步 target(实际设备号)+置 STATUS_RUN+setCurrentTemplate，实现 RNA→PCR 串联推进"),
    ("updateDsiapatchStart/Stoptime + saveModel", "推送·时间/持久化", C_GRAY, "起止时间写入均触发 TrayManager.saveModel→MMKV；updatePriority 按 createTime 排队。托盘状态全程可断电恢复"),
])

# ============ Slide 关键结论 ============
s = slide()
title_bar(s, "KEY FINDINGS / 关键结论", "四个需要注意的设计要点", C_BLUE)

pts = [
    ("调度依赖 Activity 存活", C_ORANGE,
     "APLService 与 ArmTaskWorker 两套保活机制代码存在但被注释停用，当前唯一有效驱动是 Activity 自己的 Timer，Activity 销毁则调度停止"),
    ("机械臂是单一共享资源", C_PURPLE,
     "armCanuse() 门控 + RNAAndPCRAsyncTask 单例串行，保证同时刻只有一个搬运动作；checktrayurgent 任一步被占用即整体 return"),
    ("上位机不解释生化流程", C_TEAL,
     "RNA温控/加液编排在设备端 .hex 模板，PCR温循/荧光通道编排在 .prj(宏石SLAN)程序；安卓只做模板透传、状态聚合、结果接收、流水线调度"),
    ("完成通知链较长且异步", C_BLUE,
     "模组完成不直接回调调度器，经 WSS→EventBus→Handler→APLManager 迁移状态，再靠5秒轮询推进；onRNAFinish/onPCRFinish 特意 sleep(1s) 等设备重启"),
]
xs = [0.6, 6.85]
ys = [1.65, 4.45]
for i,(t, col, d) in enumerate(pts):
    x = xs[i % 2]; y = ys[i // 2]
    box(s, x, y, 5.9, 2.55, fill=C_WHITE, line=col, line_w=1.4, radius=0.06)
    box(s, x, y, 0.22, 2.55, fill=col, shape=MSO_SHAPE.RECTANGLE)
    txt(s, x+0.45, y+0.28, 5.3, 0.55, [[(str(i+1), 18, col, True),("  ", 18, col, True),(t, 16, C_DARK, True)]])
    txt(s, x+0.45, y+1.05, 5.25, 1.35, [[(d, 12.5, C_GRAY, False)]], space=1.15)

prs.save(OUT)
print("saved:", OUT)

