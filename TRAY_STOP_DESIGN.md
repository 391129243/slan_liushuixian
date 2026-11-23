# 托盘停止/中断流程设计文档

## 目录
1. [概述](#概述)
2. [问题背景](#问题背景)
3. [解决方案](#解决方案)
4. [使用指南](#使用指南)
5. [技术细节](#技术细节)
6. [测试场景](#测试场景)
7. [故障排查](#故障排查)

---

## 概述

### 目标
当用户在托盘实验运行中点击"停止"按钮时，机械臂能够**安全地完成所有当前动作**（如关闭RNA门、放回PCR盖子等），然后自动执行还盘操作，避免所有中途中断导致的设备卡住问题。

### 核心特性
✓ RNA门、PCR盖子等复杂机构可以完整完成  
✓ 避免中途中断导致的设备卡住  
✓ 自动触发还盘，无需手动干预  
✓ 完整的日志记录便于问题排查  

---

## 问题背景

### 原始问题
用户在托盘运行过程中点击停止后：

| 场景 | 原状态 | 问题 |
|------|--------|------|
| RNA打开门后停止 | ❌ 中断卡住 | RNA门无法关闭，设备被卡住 |
| PCR盖子放回中停止 | ❌ 中断卡住 | PCR盖子卡在中途 |
| 普通运输中停止 | ⚠️ 不完整 | 托盘位置不确定 |

### 根本原因
原代码立即调用 `cancelAllWork()` 打断 `RobotWorkManager` 的整个任务队列，导致复杂机构（RNA门、PCR盖子）被卡在中途。

---

## 解决方案

### 核心思路
**不打断队列，让其自然完成，完成后再还盘**

```
原方案：停止 → 立即打断 → 还盘 ❌ 中途卡住
新方案：停止 → 让队列完成 → 再还盘 ✓ 完全安全
```

### 架构设计

#### 1. 状态标志
```java
// ArmManager.java
private boolean shouldReturnTrayAfterStop = false;  // 停止后还盘标志
private int returnTrayIndex = -1;                   // 要返回的托盘索引
```

#### 2. 执行流程

```
┌─────────────────────────────────────┐
│   用户点"停止"按钮                   │
│   TrayManageFragment.stopTrayExecution()
└──────────────┬──────────────────────┘
               │
               ├─→ 1. 设置状态 FLAG_STOP
               │
               ├─→ 2. 标记 usererror = 1
               │      (阻止新任务，让当前队列继续)
               │
               ├─→ 3. 调用 ArmManager.setStopAndReturnTray(trayIndex)
               │      (注册"停止后还盘"标志)
               │
               └─→ 4. 保存状态，显示提示
                      "托盘已停止，等待机械臂完成当前动作..."

                      ⏳ RobotWorkManager 继续执行队列
                         ├─ close_rna_door ✓
                         ├─ 其他动作 ✓
                         └─ 全部完成

                      🔔 触发回调
                      RobotWorkManager.onAllTasksCompleted()

┌─────────────────────────────────────┐
│   检查 shouldReturnTrayAfterStop     │
└──────────────┬──────────────────────┘
               │
               ├─→ YES: 调用 triggerReturnTrayAfterStop(trayIndex)
               │        执行还盘逻辑
               │
               └─→ NO: 执行正常的完成回调

┌─────────────────────────────────────┐
│   还盘流程 (ArmManager)              │
│   - 检查是否持有物品                 │
│   - 如果在PCR/RNA → 取回 + 放回托盘 │
│   - 否则 → 直接清理                  │
│   - 重置所有标志                     │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│   完成 ✓                             │
│   用户可清盘后重新开始               │
└─────────────────────────────────────┘
```

---

## 使用指南

### 场景1：用户在托盘运行中点停止

**代码位置：** `TrayManageFragment.java`

```java
private void stopTrayExecution(){
    // 1. 设置状态
    trayModel.flagstatus.set(ConstantData.FLAG_STOP);
    
    // 2. 标记为用户中断
    if (dm != null && dm.targettype.get() != null) {
        // 只标记usererror，不立即停止
        model.usererror.set(1);
    }
    
    // 3. 注册停止后还盘
    ArmManager.getInstance().setStopAndReturnTray(trayIndex);
    
    // 4. 保存状态
    TrayManager.getInstance().saveModel(trayIndex);
    
    showLongToast("托盘已停止，等待机械臂完成当前动作...");
}
```

**执行流程：**
1. ✓ RNA门继续关闭到底
2. ✓ PCR盖子继续放回到底
3. ✓ 托盘运输继续到目标设备
4. ✓ 所有动作完成
5. ✓ 自动触发还盘
6. ✓ 托盘回到起始位置

**用户交互：**
```
用户点"停止"
   ↓
提示："托盘已停止，等待机械臂完成当前动作..."
   ↓
⏳ 等待（通常5-15秒）
   ↓
提示："机械臂已完成，可以清盘"
   ↓
用户点"清盘"
   ↓
提示："清盘成功，可以新增"
```

### 场景2：RNA打开门后停止

**具体过程：**
```
运行中...
  → RNA门打开
    → 放置样本盘
      → 【用户点停止】
        → RNA门继续关闭 ✓（不被打断）
        → 机械臂取回 ✓
        → 放回托盘位 ✓
        → 完成
```

**日志输出：**
```
【用户停止】注册停止后还盘，托盘索引：1
【停止后还盘】机械臂队列完成，开始执行还盘流程
【执行还盘】开始处理停止后的还盘，托盘索引：1
【执行还盘】机械臂持有物品：RNA1
【执行还盘】从RNA1取回托盘
【执行还盘】取回完成，现在放回托盘
【执行还盘】放回完成，还盘流程结束
```

### 场景3：普通运输中停止

**具体过程：**
```
从托盘取出
  → 移动到PCR
    → 【用户点停止】
      → 继续放到PCR ✓
      → 机械臂释放 ✓
      → 完成
```

---

## 技术细节

### ArmManager 修改

#### 新增字段
```java
private boolean shouldReturnTrayAfterStop = false;
private int returnTrayIndex = -1;
```

#### 新增方法1：注册停止后还盘
```java
public void setStopAndReturnTray(int trayIndex) {
    Log.w("ArmManager", "【用户停止】注册停止后还盘，托盘索引：" + trayIndex);
    this.shouldReturnTrayAfterStop = true;
    this.returnTrayIndex = trayIndex;
    // 【关键】不调用 stop() 或 cancelAllWork()
    // RobotWorkManager 中的队列会继续执行完毕
}
```

#### 新增方法2：执行还盘
```java
private void triggerReturnTrayAfterStop(int trayIndex) {
    // 检查itemProperty_target
    if (itemProperty_target != null) {
        String targetType = itemProperty_target.getType();
        
        // 如果在PCR/RNA
        if (targetType.equals("PCR") || targetType.equals("RNA")) {
            // 两步操作：取回 + 放回
            robotGet(source);  // 第一步
            robotPut(target);  // 第二步（在回调中）
        } else {
            // 其他情况直接还回
            startPut(...);
        }
    } else {
        // 无持有物品，清理
        setFree();
    }
    // 重置所有状态
    shouldReturnTrayAfterStop = false;
    returnTrayIndex = -1;
}
```

#### 修改方法：RobotWorkManager回调
```java
@Override
public void onAllTasksCompleted(String text) {
    updateFinish(1, true);
    
    // 【新增】检查是否需要执行"停止后还盘"
    if (shouldReturnTrayAfterStop && returnTrayIndex >= 0) {
        Log.w("ArmManager", "【停止后还盘】机械臂队列完成，开始执行还盘流程");
        shouldReturnTrayAfterStop = false;
        triggerReturnTrayAfterStop(returnTrayIndex);
        return;  // 【关键】return，不执行后续逻辑
    }
    
    // 正常的完成回调
    setcallback(true, text);
}
```

#### 修改方法：clearData
```java
public void clearData(Context context){
    // 【新增】重置停止后还盘标志
    shouldReturnTrayAfterStop = false;
    returnTrayIndex = -1;
    
    // 其他清理逻辑...
}
```

### TrayManageFragment 修改

#### 修改方法：stopTrayExecution
```java
private void stopTrayExecution(){
    int trayIndex = trayModel.index.get();
    
    // 1. 设置状态
    trayModel.flagstatus.set(ConstantData.FLAG_STOP);
    
    // 2. 标记为用户中断（阻止新任务，让队列继续）
    DispatchModel dm = TrayManager.getInstance().getCurrentDispatch(trayIndex);
    if (dm != null && dm.targettype.get() != null) {
        if (dm.targettype.get() == ConstantData.DEVICE_PCR) {
            PipePCRModel model = PCRManager.getInstance().getPipePCRModel(dm.target.get());
            if (model != null) {
                model.usererror.set(1);  // 【关键】只标记，不停止
            }
        } else if (dm.targettype.get() == ConstantData.DEVICE_RNA) {
            PipeRNAModel model = RNAManager.getInstance().getPipeRNAModel(dm.target.get());
            if (model != null) {
                model.usererror.set(1);  // 【关键】只标记，不停止
            }
        }
    }
    
    // 3. 【关键】注册停止后还盘
    ArmManager.getInstance().setStopAndReturnTray(trayIndex);
    
    // 4. 保存并提示
    TrayManager.getInstance().saveModel(trayIndex);
    showLongToast("托盘已停止，等待机械臂完成当前动作...");
}
```

---

## 测试场景

### 测试1：RNA门完整关闭

| 步骤 | 操作 | 预期 |
|------|------|------|
| 1 | 启动RNA实验 | RNA门打开 ✓ |
| 2 | 实验中点"停止" | 提示等待 ✓ |
| 3 | RNA门继续关闭 | 门关闭完成 ✓ |
| 4 | 机械臂还盘 | 托盘回到起始位 ✓ |
| 5 | 点"清盘" | 状态恢复正常 ✓ |

### 测试2：PCR盖子完整放回

| 步骤 | 操作 | 预期 |
|------|------|------|
| 1 | 启动PCR实验 | 盖子打开 ✓ |
| 2 | 实验中点"停止" | 提示等待 ✓ |
| 3 | 盖子继续放回 | 盖子放回完成 ✓ |
| 4 | 机械臂还盘 | 托盘回到起始位 ✓ |
| 5 | 点"清盘" | 状态恢复正常 ✓ |

### 测试3：普通运输中停止

| 步骤 | 操作 | 预期 |
|------|------|------|
| 1 | 启动实验 | 托盘移动中 ✓ |
| 2 | 移动中点"停止" | 提示等待 ✓ |
| 3 | 运输继续完成 | 托盘到达目标 ✓ |
| 4 | 机械臂还盘 | 托盘回到起始位 ✓ |
| 5 | 点"清盘" | 状态恢复正常 ✓ |

---

## 故障排查

### 问题1：停止后长时间无反应

**症状：** 点停止按钮后，界面一直显示"等待机械臂完成当前动作..."

**排查步骤：**
1. 查看日志中是否有 `【停止后还盘】机械臂队列完成` 的消息
2. 检查 `shouldReturnTrayAfterStop` 是否正确设置
3. 检查 `RobotWorkManager` 是否有队列堆积

**解决方案：**
- 检查是否有异常导致队列卡住
- 查看 `onAllTasksCompleted()` 是否被调用
- 重启应用重置状态

### 问题2：还盘失败

**症状：** 日志显示 `【执行还盘】失败` 错误

**排查步骤：**
1. 查看错误信息
2. 检查 `itemProperty_target` 是否正确
3. 检查机械臂是否卡住

**解决方案：**
- 手动复位机械臂：点"复位"按钮
- 重新启动应用
- 检查机械臂硬件连接

### 问题3：状态残留

**症状：** 多次停止后，状态混乱

**排查步骤：**
1. 检查 `shouldReturnTrayAfterStop` 是否被重置
2. 检查 `returnTrayIndex` 是否被重置
3. 查看 `clearData()` 是否被调用

**解决方案：**
- 确保在所有异常分支中都有状态重置
- 重启应用清空所有状态

### 问题4：回调被覆盖

**症状：** 还盘后原回调没有执行

**排查步骤：**
1. 检查是否保存了原回调
2. 查看是否在所有分支中恢复了原回调

**解决方案：**
- 代码已添加 `final ArmFinishCallback originalCallback` 保存原回调
- 确保在 `finish()` 和 `fail()` 中都恢复

---

## 注意事项

### ⚠️ 重要
1. **不要在中途调用 `cancelAllWork()`** - 会打断队列导致卡住
2. **不要覆盖 `armFinishCallback`** - 必须保存原回调
3. **所有异常分支都要重置状态** - 防止状态残留

### ✓ 最佳实践
1. 总是在最后重置 `shouldReturnTrayAfterStop` 和 `returnTrayIndex`
2. 在所有回调中检查原回调是否存在再调用
3. 添加详细的日志便于问题排查

---

## 修改历史

| 版本 | 日期 | 修改内容 |
|------|------|---------|
| 1.0 | 2026-11-23 | 初始版本，合并中断/停止，优化还盘流程 |
| 1.1 | 2026-11-23 | 修复clearData()状态残留问题 |
| 1.2 | 2026-11-23 | 修复回调覆盖和状态重置问题 |

