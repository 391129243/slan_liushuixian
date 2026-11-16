# 修复：PCR/RNA 启动时清空曲线图

## 问题描述

每次开始新的 PCR/RNA 实验时，扩增曲线图和熔解曲线图**没有立即清空**，而是要等到新数据上来才触发清除，导致：
- 旧实验的曲线残留显示
- 用户体验差，以为新实验还没开始
- 曲线数据混乱，新旧数据叠加

---

## 根本原因

### 🔴 问题根源：`MSG_START_PCR` / `MSG_START_RNA` 时没有清空曲线图

**位置**：`PipeMainActivity.java:719-725` (PCR) / `796-802` (RNA)

**原有代码**：
```java
case ConstantData.MSG_START_PCR:
    if(msg.obj!=null) {
        int target = (int)msg.obj;
        // ❌ 只设置运行状态，没有清空曲线图
        APLManager.getInstance().checkPcrRunTemplate(target);
        PCRManager.getInstance().saveModel(target);
    }
    break;
```

**对比**：`MSG_CLEAR_PCR` 会调用 `pcrClear(target)` 清空曲线，但 `MSG_START_PCR` 没有。

---

## 流程分析

### **原有流程（有问题）**

```
PCR 启动
  ↓
MSG_START_PCR
  ↓ checkPcrRunTemplate(target)
  ↓ 只设置 armStep = WORK_RUN
  ↓
【曲线图没有清空，显示旧数据】
  ↓
PCR 开始运行，收到第一个数据点
  ↓
updateWellResult() → clearAllCharts()  ← 第一次数据更新时才清空
```

**问题**：
1. PCR 启动到收到第一个数据点之间，曲线图显示的是旧实验数据
2. 用户看到旧曲线，误以为新实验还没开始
3. 如果启动失败，旧曲线会一直残留

---

### **修复后流程（正确）**

```
PCR 启动
  ↓
MSG_START_PCR
  ↓ mainFragment.pcrClear(target)  ← 立即清空曲线图
  ↓ PipePcrFragment.clearModel()
  ↓ PcrCurveFragment.clearModel()
  ↓ clearAllCharts()  ← 清空 rawChart 和 meltChart
  ↓ checkPcrRunTemplate(target)
  ↓ 设置 armStep = WORK_RUN
  ↓
【曲线图已清空，等待新数据】
  ↓
PCR 开始运行，收到第一个数据点
  ↓
updateWellResult() → 绘制新曲线
```

**优势**：
1. ✅ PCR 启动时立即清空曲线图
2. ✅ 用户体验好，看到空白图表知道新实验即将开始
3. ✅ 即使启动失败，也不会残留旧数据
4. ✅ 新旧数据不会混淆

---

## 修复方案

### ✅ 修复1：`MSG_START_PCR` 启动时清空 PCR 曲线图

**位置**：`PipeMainActivity.java:719-728`

```java
case ConstantData.MSG_START_PCR:
    if(msg.obj!=null) {
        int target = (int)msg.obj;
        Log.i("ArmTaskWorker", "【PCR启动】MSG_START_PCR:"+target+" 清空曲线图");
        
        // ⭐ 新实验开始，先清空曲线图
        mActivity.mainFragment.pcrClear(target);
        
        // 然后设置运行状态
        APLManager.getInstance().checkPcrRunTemplate(target);
        PCRManager.getInstance().saveModel(target);
    }
    break;
```

**调用链**：
```
mainFragment.pcrClear(target)
  → PipePcrFragment.clearModel()
    → PcrCurveFragment.clearModel()
      → clearAllCharts()
        → clearChart(rawChart)  // 清空扩增曲线
        → clearChart(meltChart)  // 清空熔解曲线
```

---

### ✅ 修复2：`MSG_START_RNA` 启动时清空 RNA 曲线图

**位置**：`PipeMainActivity.java:796-805`

```java
case ConstantData.MSG_START_RNA:
    if(msg.obj!=null) {
        int target = (int)msg.obj;
        Log.i("ArmTaskWorker", "【RNA启动】MSG_START_RNA:"+target+" 清空曲线图");
        
        // ⭐ 新实验开始，先清空曲线图
        mActivity.mainFragment.rnaClear(target);
        
        // 然后设置运行状态
        APLManager.getInstance().checkRnaRunTemplate(target);
        RNAManager.getInstance().saveModel(target);
    }
    break;
```

**调用链**：
```
mainFragment.rnaClear(target)
  → PipeRnaFragment.clearModel()
    → RnaCurveFragment.clearModel()
      → 清空 RNA 曲线图
```

---

## 相关代码结构

### **现有的清空机制**

| 消息/事件 | 是否清空曲线 | 用途 |
|---------|------------|------|
| `MSG_CLEAR_PCR` | ✅ 调用 `pcrClear()` | 手动清除 PCR 数据 |
| `MSG_START_PCR` | ❌ **之前没有**，现已修复 | PCR 实验启动 |
| `MSG_Finish_PCR` | ❌ 不清空 | PCR 实验结束，保留数据 |
| `updateWellResult()` 首次调用 | ✅ 调用 `clearAllCharts()` | 第一次收到数据时清空（延迟） |

**问题**：
- `updateWellResult()` 中的 `clearAllCharts()` 是**延迟清空**，只有收到第一个数据点时才触发
- 如果 PCR 启动失败或延迟，用户会长时间看到旧曲线
- 现在 `MSG_START_PCR` 时**立即清空**，体验更好

---

### **clearModel() 调用链**

```
PipePcrFragment.clearModel()
  ↓
1. mainViewModel.clearPipePCRModel(getContext(), pcrindex)  // 清空数据模型
  ↓
2. curveFragment.clearModel()  // 清空曲线 Fragment
  ↓ initData()  // 重置数据列表
  ↓ initpcrview()  // 重置视图状态
  ↓ initModel()  // 重新绑定数据模型
  ↓ clearAllCharts()  // 清空所有图表 ⭐
  ↓   clearChart(rawChart)
  ↓   clearChart(meltChart)
  ↓ refreshExperiment()
  ↓
3. dataFragment.clearModel()  // 清空数据表格 Fragment
```

**关键点**：
- `clearAllCharts()` 会清空 `rawChart` 和 `meltChart`
- 之前只在 `updateWellResult()` 首次调用时清空（延迟）
- 现在在 `MSG_START_PCR` 时立即调用（及时）

---

## 测试验证

### 1. 部署应用并运行 PCR 实验

### 2. 观察曲线图清空时机

**测试步骤**：
1. 运行一次 PCR 实验，等待完成，曲线图显示完整曲线
2. 启动新的 PCR 实验
3. **观察曲线图是否立即清空**

**预期结果**：
- ✅ 点击启动按钮后，曲线图**立即清空**
- ✅ 不需要等待第一个数据点上来
- ✅ 日志显示：`【PCR启动】MSG_START_PCR:1 清空曲线图`

**失败场景（已修复前）**：
- ❌ 点击启动按钮后，曲线图**仍显示旧数据**
- ❌ 需要等待 10-30 秒，直到第一个数据点上来才清空
- ❌ 用户体验差，误以为新实验没启动

### 3. 查看日志
```bash
adb logcat -s ArmTaskWorker:* | grep -E "PCR启动|RNA启动|清空曲线图"
```

**正常日志**：
```
【PCR启动】MSG_START_PCR:1 清空曲线图
pcrClear:1
PCR:1 WORK_RUN
```

---

## 修改文件清单

1. **修改**：`app/src/main/java/com/paul/pipeline/activity/PipeMainActivity.java`
   - `MSG_START_PCR` Handler - 启动时调用 `pcrClear(target)`
   - `MSG_START_RNA` Handler - 启动时调用 `rnaClear(target)`

2. **无需修改**（已有现成功能）：
   - `PipeMainFragment.java` - 已有 `pcrClear()` / `rnaClear()` 方法
   - `PipePcrFragment.java` - 已有 `clearModel()` 方法
   - `PcrCurveFragment.java` - 已有 `clearAllCharts()` 方法

---

## 总结

本次修复通过在 PCR/RNA 启动时**立即清空曲线图**，彻底解决了旧实验曲线残留的问题：

1. ✅ 用户体验提升：启动新实验时立即看到空白曲线图
2. ✅ 数据准确性：新旧数据不会混淆
3. ✅ 代码简洁：复用现有的 `pcrClear()` 方法，无需重复开发
4. ✅ 一致性：与 `MSG_CLEAR_PCR` 行为保持一致

**预期效果**：
- 启动新实验时，曲线图立即清空
- 不再需要等待第一个数据点上来才清空
- 用户看到空白图表，知道新实验即将开始
