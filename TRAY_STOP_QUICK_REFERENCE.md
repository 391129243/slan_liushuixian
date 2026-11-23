# 快速参考 - 托盘停止/还盘流程

## 核心概念（60秒理解）

```
❌ 原方案：停止 → 立即打断 → 还盘 → 卡住
✅ 新方案：停止 → 完成队列 → 还盘 → 安全

关键点：RobotWorkManager 的队列继续执行完毕
```

---

## 关键方法速查

### 用户点"停止"时
**位置：** `TrayManageFragment.java`  
**方法：** `stopTrayExecution()`

```java
// 步骤1：标记状态
trayModel.flagstatus.set(ConstantData.FLAG_STOP);

// 步骤2：标记中断（不立即停止PCR/RNA）
model.usererror.set(1);

// 步骤3：【关键】注册停止后还盘
ArmManager.getInstance().setStopAndReturnTray(trayIndex);
```

### 队列完成时（自动触发）
**位置：** `ArmManager.java`  
**方法：** `RobotWorkManager.onAllTasksCompleted()` 中的回调

```java
// 自动检查
if (shouldReturnTrayAfterStop && returnTrayIndex >= 0) {
    triggerReturnTrayAfterStop(returnTrayIndex);
}
```

### 还盘执行
**位置：** `ArmManager.java`  
**方法：** `triggerReturnTrayAfterStop(int trayIndex)`

```java
// 自动处理：
// 1. 检查itemProperty_target
// 2. 如果在PCR/RNA → 取回 + 放回
// 3. 否则 → 直接清理
// 4. 重置所有标志
```

---

## 日志关键字（快速定位问题）

| 关键字 | 含义 | 位置 |
|--------|------|------|
| `【用户停止】` | 用户点了停止 | ArmManager.setStopAndReturnTray() |
| `【停止后还盘】机械臂队列完成` | 队列完成，开始还盘 | ArmManager.init().onAllTasksCompleted() |
| `【执行还盘】开始处理` | 还盘流程开始 | ArmManager.triggerReturnTrayAfterStop() |
| `【执行还盘】机械臂持有物品` | 检测到持有物品 | ArmManager.triggerReturnTrayAfterStop() |
| `【执行还盘】放回完成` | 还盘成功 | ArmManager 回调 |
| `【执行还盘】失败` | 还盘失败 | ArmManager 回调 |

---

## 状态转换图

```
┌─────────────────────┐
│ FLAG_RUN            │
│ 实验运行中          │
└──────────┬──────────┘
           │
      用户点停止
           │
           ↓
┌─────────────────────┐
│ FLAG_STOP           │
│ shouldReturnTray=true
│ returnTrayIndex=1   │
└──────────┬──────────┘
           │
  RobotWorkManager
  继续完成队列
           │
           ↓
┌─────────────────────┐
│ onAllTasksCompleted │
│ 检查shouldReturnTray
│      ↓YES           │
│ 执行还盘            │
└──────────┬──────────┘
           │
           ↓
┌─────────────────────┐
│ 还盘完成            │
│ setFree()           │
│ 状态重置            │
└──────────┬──────────┘
           │
           ↓
┌─────────────────────┐
│ FLAG_FREE           │
│ 可清盘、重新开始    │
└─────────────────────┘
```

---

## 常见问题快速解决

| 问题 | 检查项 | 修复 |
|------|--------|------|
| 长时间无反应 | 日志中有 `【停止后还盘】` 吗 | 检查队列是否卡住 |
| 还盘失败 | itemProperty_target 是否正确 | 手动复位机械臂 |
| 状态残留 | clearData() 是否被调用 | 重启应用 |
| 回调问题 | 原回调是否被保存 | 检查 originalCallback |

---

## 文件修改清单

| 文件 | 修改内容 | 行数 |
|------|---------|------|
| `ArmManager.java` | 新增2个字段 + 修改init() + 新增2个方法 + 修改clearData() | +92 |
| `TrayManageFragment.java` | 修改stopTrayExecution() + 废弃triggerArmReturnTray() | +52 |

---

## 验证清单（上线前检查）

- [ ] 所有 `shouldReturnTrayAfterStop` 在异常分支中都被重置
- [ ] 所有 `returnTrayIndex` 在异常分支中都被重置
- [ ] 原 `armFinishCallback` 在所有分支中都被恢复
- [ ] `clearData()` 中重置了两个新字段
- [ ] RNA门完全关闭后才还盘
- [ ] PCR盖子完全放回后才还盘
- [ ] 日志输出完整（便于排查）
- [ ] 没有调用 `cancelAllWork()` 或 `robotStop()`

---

## 二级参考 - ArmDetailFragment 调试界面

**位置：** `ArmDetailFragment.java:179`  
**按钮：** "停止"按钮

⚠️ **注意：** 这是调试界面，直接调用 `ArmManager.getInstance().stop()`，会立即打断队列！

```java
binding.stopBtn.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        ArmManager.getInstance().stop();  // ⚠️ 调试用
        show("机械臂停止");
    }
});
```

**建议：** 添加注释说明这是调试界面，不建议生产环境使用。

---

## 完整流程动画

```
时间轴：

T0: 【运行中】
    RNA门：打开 ✓
    
T1: 【用户点停止】
    状态 → FLAG_STOP
    注册 → setStopAndReturnTray()
    消息 → "等待机械臂完成..."

T2-T10: 【队列继续】
    RNA门 → 继续关闭 ✓
    机械臂 → 完成所有worker
    
T11: 【队列完成】
    触发 → onAllTasksCompleted()
    检查 → shouldReturnTrayAfterStop == true
    
T12-T15: 【执行还盘】
    步骤1 → robotGet(RNA)  [取回]
    步骤2 → robotPut(PAN)  [放回]
    
T16: 【完成】
    重置 → 所有状态
    消息 → "可以清盘"
```

---

## 相关文件路径

| 文件 | 路径 |
|------|------|
| 完整文档 | `TRAY_STOP_DESIGN.md` |
| ArmManager | `slan_liushuixian_server/Bio_PCR/app/src/main/java/com/paul/pipeline/manager/ArmManager.java` |
| TrayManageFragment | `slan_liushuixian_server/Bio_PCR/app/src/main/java/com/paul/pipeline/fragment/TrayManageFragment.java` |
| ArmDetailFragment | `slan_liushuixian_server/Bio_PCR/app/src/main/java/com/paul/pipeline/fragment/ArmDetailFragment.java` |

