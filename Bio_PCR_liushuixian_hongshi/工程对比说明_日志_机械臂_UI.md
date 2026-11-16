# 工程对比说明：日志 / 机械臂调度 / UI 数据分离

> 对比对象
> - **工程 A（本工程）**：`Bio_PCR_liushuixian_hongshi`
> - **工程 B（参考工程）**：`slan_liushuixian_server/Bio_PCR`
>
> 目的：记录两工程在三大子系统上的差异，作为后续互相同步/优化的依据。
> 结论摘要：**日志系统 B 更新（已移植到 A）；机械臂调度 A 更先进；UI 后台化 B 有专用执行器。**

---

## 0. 总览

| 维度 | 工程 A（本工程） | 工程 B（参考） | 谁更优 |
|------|-----------------|---------------|--------|
| 日志目录结构 | ~~分散目录~~ → 已移植为日期目录 | 日期目录 `/Log/yyyyMMdd/模块/` | 移植后持平，A 额外修了双写 |
| 日志去重 | 单写入方（APLService） | **双写入方，bin 日志写两遍** | **A** |
| 机械臂调度 | 抽出 `SchedulingEngine`/`SchedulingPort` + 单元测试 | 调度逻辑内联在 `APLManager` | **A** |
| 安全门禁 | 手臂可用/传感器校验**生效** | 大量 `//TODO fordebugtest` `if(true)` 绕过 | **A** |
| SLAN 落库线程 | 走 `PipeMainActivity` Handler 消息 | 专用 `PCRFinishTaskExecutor` 后台队列 | **B** |
| WSS 阻塞优化 | 无专用执行器 | `onUpgradeAck`/落库全后台化 | **B** |
| MVVM/DataBinding | 与 B 基本一致（viewmodel 包字节级相同） | 同左 | 持平 |

---

## 1. 日志系统

### 1.1 变更前后（A 已移植 B 的设计）
- 详见本工程 `日志系统变更说明.md`。
- 关键点：`/Log/模块` → `/Log/yyyyMMdd/模块/`；10 天单文件清理 → 14 天日期目录递归清理；
  logcat 20MB / PCR·RNA 100MB 切换；ARM 取消独立文件改直写 logcat。

### 1.2 A 相对 B 的额外优势：修复 bin 日志双写
- **B 的现状（Bug）**：`APLService.onEvent(LogMessageEvent)`（无类型门控）与
  `PipeMainActivity.onEvent(LogMessageEvent)`（仅 `WSS_LOGMessage`）**同时注册**，
  且都通过同一个 `LogcatHelper` 单例写文件。
  → `WSSModuleManager.handleBinMessage` 发出的 bin PCR/RNA 日志被**写入两次**。
- **A 的处理**：移植时删除 `PipeMainActivity` 的该订阅，`APLService` 作为唯一写入方。
- **后续建议**：反向同步到 B 时，一并移除 B 的 `PipeMainActivity.onEvent(LogMessageEvent)`。

### 1.3 涉及文件
- A：`com/gidi/bio_pcr/util/LogcatHelper.java`、`com/paul/pipeline/data/ConstantData.java`、
  `com/paul/pipeline/service/APLService.java`、`com/paul/pipeline/activity/PipeMainActivity.java`、
  `com/winters/boweilibrary/autopipeline/worker/RobotWorkManager.java`
- B：同名文件（B 尚未修双写；B 仍保留 `/Log/ARM` 之前版本已在其 `日志系统变更说明.md` 取消）

---

## 2. 机械臂调度

### 2.1 相同点（两工程一致）
- **传输架构相同**：机械臂经**串口**直控（`SerialPortManager_Robot.robotGet/robotPut/robotReset`
  ← `RobotWorker.doWork()` ← WorkManager 链），PCR/RNA 分析模块经 **WSS** 远程下发模板。
  两者都是"串口手臂 + WSS 分析仪"混合，手臂从不走 WSS。
- **调度轮询**：`APLService` Timer 每 **5000ms**（`ConstantData.ARMTASK_PERIOD`）触发
  `APLManager.checkArmTask()`。
- **优先级顺序**：关门 → 加急 RNA → 加急 PCR → 普通 RNA → 普通 PCR。
- **手臂步进状态机**：`FREE → GET → RUN → MOVE/BACK → FREE`。
- **并发模型**：调度层单轮询线程（布尔门控 `gettraystart/movetraystart/backtraystart/excutestart`
  互斥单臂）；执行层 WorkManager Worker + EventBus `lock.wait()/notify()`
  （`RobotWorker` 阻塞等待 `ProcedureFinishEvent`）。
- **传感器**：`SerialPortManager_Robot` 把位打包的 `Sense` int 解析为 ~24 个布尔量
  （PAN1-8/RNA/PCR/ARM/DOOR），**串口 RX 推送**而非轮询查询；`RobotWorker` get/put 后
  轮询 ARM 与目标位传感器校验（8×500ms=4s 超时）。

### 2.2 差异点

| 项 | 工程 A | 工程 B |
|----|--------|--------|
| 调度代码组织 | 抽到 `com.paul.pipeline.scheduling`：`SchedulingEngine`、`SchedulingPort`、`ArmCallbackEngine`、`ArmCallbackPort`，`checkArmTask` 委托给 `SchedulingEngine.checkTrayUrgent()`；**有单元测试**（`SchedulingEnginePcrTest/RnaTest/TrayTest`） | 调度逻辑**内联**在 `APLManager`（`checktrayurgent`/`checkpcrwork`/`checkrnawork`），**无 scheduling 包、无测试** |
| 安全门禁 | `armCanuse()`、`RobotWorker` get/put 传感器校验**生效** | 大量 `//TODO fordebugtest` 的 `if(true)`/`if(false)` 绕过手臂忙、门锁、托盘存在、`armCanuse()`；`RobotWorker` 传感器校验结果被 `if(true)` 覆盖（`RobotWorker.java:179-180`） |
| `getFreeModel` 物理占位检查 | `!exist.get()` **启用** | `!exist` 被**注释掉**（`PCRManager.java:366`），门控更弱 |
| 重试 | `RobotWorker` 可用 WorkManager 自动重试 | `RobotWorker` 返回 `Result.failure` 而非 `retry`，自动重试实际未用 |

> 说明：`pcrFinishPending`/`checkPcrFinishPending`（finish 后等状态变 free 的重试队列）
> **两工程都没有**。B 的 `MODIFICATION_HISTORY.md` 记录过该机制，但"已于 2026-08-15 完全回退"。

### 2.3 后续优化建议
- **B 应吸收 A 的 `SchedulingEngine` 抽象 + 单元测试**，降低调度回归风险。
- **B 上线前必须清理 `//TODO fordebugtest` 的 `if(true)` 门禁绕过**，尤其
  `RobotWorker.java:179-180` 覆盖传感器校验结果 —— 存在物理误动作风险。
- A 可评估是否需要 B `MODIFICATION_HISTORY.md` 中"finish pending 重试"思路来兜底
  finish 事件早于 status 变 free 的时序问题（当前 A/B 都靠即时处理）。

---

## 3. UI / 数据分离

### 3.1 相同点
- **MVVM 骨架一致**：`fragment/viewmodel` 包（17 个 ViewModel）在两工程**字节级相同**
  （`ArmDetailViewModel`…`WellDetailViewModel`），另有 `activity/viewmodel/PipeMainViewModel`。
- **DataBinding + ViewModelProvider + LiveData + ObservableField** 用法一致
  （如 `PcrDetailFragment` 的 `DataBindingUtil.inflate` / `ViewModelProvider` / `.observe`）。
- **权威领域状态在管理器单例**：`APLManager`/`PCRManager`/`RNAManager`/`TrayManager`/`ArmManager`
  与 `PipePCRModel`/`PipeRNAModel`/`PCRStatusItem` 等；UI 直接读写单例。
- **EventBus → Handler/MSG**：业务事件经 EventBus 进入 `PipeMainActivity`，再用 `MSG_*` 分发更新 UI。
  ViewModel 主要服务于 DataBinding，而非唯一状态源。两工程耦合度相当。

### 3.2 差异点（关键在"重活的线程模型"）

| 项 | 工程 A | 工程 B |
|----|--------|--------|
| SLAN 落库执行器 | **无** `PCRFinishTaskExecutor` | 有 `manager/PCRFinishTaskExecutor.java`（单线程 + 无界队列，非守护） |
| SLAN 结束保存路径 | 走 `PipeMainActivity` Handler 消息（`MSG_FIRST_SLAN_PCR_DONE` 等），`PipePcrFragment.saveExperiment()` → `curveFragment.saveExperiment()` | `PcrCurveFragment.openAndSaveSLANExperiment()` 全程提交到 `PCRFinishTaskExecutor` 后台，仅 Toast 回 `runOnUiThread` |
| 落库线程 | —（路径不同） | 曾在 `runOnUiThread` 里落库（`.broken` 备份可见），优化后移出主线程 |
| WSS 阻塞优化 | 无专用后台队列 | `WSSModuleManager.onUpgradeAck` 的 `Thread.sleep(1000)` 移后台；PCR 结束**先还盘再落库** |
| 冗余 LiveData | 无 | B `PipeMainViewModel` 有未被使用的 `pcrSLANWellResult`（死代码） |
| chart 刷新线程 | 两者都有 `refreshExecutor`（`PcrCurveFragment`），一致 | 同左 |

### 3.3 后续优化建议
- **A 可评估引入类似 `PCRFinishTaskExecutor` 的单线程后台队列**，把 SLAN 文件打开/落库
  与心跳/EventBus 主线程解耦（B 的 `WSS_OPTIMIZATION_SUMMARY.md` 已验证收益：
  心跳不再超时、还盘响应快 2-5s）。当前 A 走 Handler 消息，需确认落库是否也在主线程。
- **B 应删除死代码** `pcrSLANWellResult` 及 `PcrCurveFragment.java.broken` 备份。
- 两工程都可考虑把权威状态逐步收敛到 ViewModel/LiveData，减少 UI 对管理器单例的直接依赖
  （当前耦合较重，属长期重构项）。

---

## 4. 同步优先级建议（供排期）

| 优先级 | 事项 | 方向 | 风险 |
|--------|------|------|------|
| P0 | B 清理 `//TODO fordebugtest` 门禁绕过（含传感器校验覆盖） | 修 B | 高（物理安全） |
| P0 | B 移除 `PipeMainActivity` 日志重复订阅 | 修 B | 低 |
| P1 | A 引入 SLAN 落库后台执行器（参考 B 的 `PCRFinishTaskExecutor`） | 补 A | 中 |
| P1 | B 吸收 A 的 `SchedulingEngine` 抽象与单元测试 | 修 B | 中 |
| P2 | B 删除死代码 / `.broken` 备份 | 修 B | 低 |
| P2 | 两工程状态向 ViewModel 收敛（长期） | 双向 | 中 |

---

## 5. 关键文件索引

**工程 A**
- 日志：`app/src/main/java/com/gidi/bio_pcr/util/LogcatHelper.java`
- 调度：`app/src/main/java/com/paul/pipeline/scheduling/SchedulingEngine.java`、`SchedulingPort.java`
- 调度入口：`app/src/main/java/com/paul/pipeline/manager/APLManager.java`
- 手臂：`app/src/main/java/com/winters/boweilibrary/autopipeline/worker/RobotWorker.java`、`RobotWorkManager.java`、`mgr/SerialPortManager_Robot.java`
- UI：`app/src/main/java/com/paul/pipeline/activity/PipeMainActivity.java`、`fragment/PcrCurveFragment.java`

**工程 B**
- 日志：`app/src/main/java/com/gidi/bio_pcr/util/LogcatHelper.java`（+ `日志系统变更说明.md`）
- 调度：`app/src/main/java/com/paul/pipeline/manager/APLManager.java`（内联）
- 后台队列：`app/src/main/java/com/paul/pipeline/manager/PCRFinishTaskExecutor.java`
- UI：`app/src/main/java/com/paul/pipeline/fragment/PcrCurveFragment.java`（+ `.broken` 备份）
- 参考文档：`DISPATCH_ISSUE_ANALYSIS.md`、`WSS_OPTIMIZATION_SUMMARY.md`、`MODIFICATION_HISTORY.md`

---

**文档生成时间**：2026-09-06
**依据**：两工程源码交叉走查（含 grep / 单元测试目录 / `.broken` 备份对比）
