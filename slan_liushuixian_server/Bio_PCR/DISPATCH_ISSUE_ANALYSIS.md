# 调度问题分析：连续调度同一个 PCR + 移盘后不启动

## 🔴 问题描述

用户报告：
1. **新增两个盘实验时，为什么会连续调度 PCR1**？
2. **移盘1到 PCR1 后不触发启动 PCR**？

---

## 🔍 根本原因分析

### 问题1：连续调度同一个 PCR

#### **调度循环逻辑**

```java
// APLManager.java:999-1057
private boolean checktrayurgent() {
    // 1. 加急RNA
    for (int i = 0; i < trays.size(); i++) {
        if (tray.urgent.get()) {
            result = checktrayRnawork(tray.index.get());  // 处理加急RNA
        }
    }
    // 2. 加急PCR
    for (int i = 0; i < trays.size(); i++) {
        if (tray.urgent.get()) {
            result = checktrayPcrwork(tray.index.get());  // 处理加急PCR
        }
    }
    // 3. 普通RNA
    for (int i = 0; i < trays.size(); i++) {
        result = checktrayRnawork(tray.index.get());  // 处理所有托盘的RNA
    }
    // 4. 普通PCR  ← 问题在这里！
    for (int i = 0; i < trays.size(); i++) {
        result = checktrayPcrwork(tray.index.get());  // 遍历所有托盘
    }
}
```

#### **问题：每次轮询都会遍历所有托盘**

**场景**：假设有 2 个托盘（Tray1、Tray2），都需要分配 PCR

```
第1次调度循环：
  checktrayurgent()
    → 遍历托盘1：checktrayPcrwork(1)
      → 托盘1.dispatchindex = 1，STATUS_FREE
      → getFreeModel() 返回 PCR1
      → 分配：托盘1 → PCR1
      → dm.status = STATUS_RUN
      → PCR1.armStep = WORK_GET
      → TrayManager.setCurrentDispatch(1, 1)  // 设置托盘1当前调度索引为1
    → 遍历托盘2：checktrayPcrwork(2)
      → 托盘2.dispatchindex = 1，STATUS_FREE
      → getFreeModel() 返回 PCR1  ← ⚠️ 又返回 PCR1！
      → 分配：托盘2 → PCR1
      → 覆盖：PCR1.currentTray = 2  ← ⚠️ 覆盖了托盘1的数据！
```

**根本原因**：
1. `checktrayurgent()` 每次都遍历**所有托盘**
2. `getFreeModel()` 只检查 PCR 的 `status` 和 `linuxStatus` 是否为 `"free"`
3. 在**同一次调度循环**中，第一个托盘分配了 PCR1 后，`PCR1.status` 仍然是 `"free"`（因为还没取盘）
4. 第二个托盘调用 `getFreeModel()` 时，PCR1 仍然满足"空闲"条件，于是又被分配给托盘2
5. 第二次分配会覆盖 `PCR1.currentTray` 和 `PCR1.dispatchindex`，导致托盘1的调度数据丢失

---

### 问题2：移盘后不触发启动 PCR

#### **正常流程**

```
机械臂移盘完成
  ↓
WSSManager_ArmMoveWorkFinish 事件
  ↓
PipeMainActivity Handler: MSG_ArmMoveTrayFinish
  ↓
APLManager.checkArmMoveTray(from, to)
  ↓ 如果 to 是 PCR
  ↓ PCRManager.checkPcrRunTemplate(to)
  ↓   PCR.armStep = WORK_RUN
  ↓   PCR.excutestart = false
  ↓
下次 checkArmTask() 轮询
  ↓
checktrayPcrwork(tray)
  ↓
checkpcrwork(pcr, tray)
  ↓ 检查 armStep == WORK_RUN && excutestart == false
  ↓ startRunTemplate(pcr)  ← 启动 PCR 模板
```

#### **可能的断点**

**断点A：`checkArmMoveTray` 没有被调用**
- `WSSManager_ArmMoveWorkFinish` 事件没有发送
- 或者 Handler 没有收到 `MSG_ArmMoveTrayFinish`

**断点B：`checkArmMoveTray` 中 `checkPcrRunTemplate` 失败**
- 找不到 PCR 模型
- 或者 PCR 已经在运行中

**断点C：`startRunTemplate` 找不到调度数据**
```java
// APLManager.java:766-779
case ConstantData.WORK_RUN:
    if (Boolean.FALSE.equals(m.excutestart.get())) {
        DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(m.currentTray.get());
        if (dispatchModel == null) {
            Log.e("ArmTaskWorker", "WORK_RUN 找不到调度数据");  // ← 静默失败
            // ❌ 没有设置 excutestart = true，下次还会尝试
            // ❌ 但如果一直找不到，就永远不会启动
        }
    }
}
```

**最可能的原因**：托盘数据被覆盖，`getCurrentDispatch(tray)` 返回 `null`

---

## 🔧 修复方案

### 修复1：`getFreeModel()` 增加判断，排除已分配但未启动的 PCR

**问题**：`getFreeModel()` 只检查 `status == "free"`，不检查是否已经被分配（`currentTray != null`）

**修复**：在 `PCRManager.getFreeModel()` 中增加判断
