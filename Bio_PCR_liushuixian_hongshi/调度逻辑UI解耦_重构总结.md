# 调度逻辑与 UI（databinding）解耦 · 重构总结

> 范围：RNA / PCR / Tray / Arm 的调度决策从 `APLManager` 中抽出，脱离 Android databinding，
> 成为可在纯 JVM 下单测的 `SchedulingEngine`。全程行为逐行等价、可编译、单测覆盖。
> 单测结果：RNA 14 + PCR 12 + Tray 26 = **52 用例全绿**。

---

## 1. 为什么重构（问题）
`APLManager` 是调度总管，但调度状态机（`checkrnawork` / `checkpcrwork` / `checktrayend` 等）
**直接读写 databinding 的 UI 模型**（`ObservableField`），带来三个问题：
1. 编译期强依赖 `androidx.databinding`，调度核心**无法在纯 JVM 下单测**；
2. 调度状态（`armStep`、一堆运行态布尔）散落在 UI 模型上，**UI 可随意改调度状态**，边界不清；
3. 状态机跑在后台线程（APLService Timer 每 5s），`ObservableField.set()` 又触发 UI 线程回调，读写归属混乱。

**目标**：把调度决策抽到一个不依赖 databinding 的引擎，UI 模型退化为只读投影 + 命令端口，
让整条调度链可单测、可维护。

---

## 2. 解耦后的分层

```
UI 层 (Fragment / XML databinding)
        │ 观察（只读投影）
        ▼
UI 模型 PipeRNAModel/PipePCRModel/PipeTrayModel/DispatchModel（仍是 ObservableField）
        │ 适配层从 ObservableField 预计算成纯 POJO 快照 / 把命令落回真模型
        ▼
SchedulingPort（接口）  ←—— APLManager.AplSchedulingPort 实现，适配到现有 Manager
        │ 只依赖接口 + 纯 POJO/枚举
        ▼
SchedulingEngine（纯决策，无 android/androidx/manager import，可纯 JVM 单测）
```

- **引擎只发意图**（取盘/移盘/送盘/起模板/分配设备/关门/UI 回调），
  **副作用全部留在适配层**（下发机械臂命令、改 Tray 状态、`saveModel`、LED、`uIinitCallback`）。
- 调度循环入口（`APLService` Timer → `APLManager.checkArmTask`）与门控包裹**保持不变**。

---

## 3. 分四步实施

### 第 1 步：消灭裸 int / 布尔（类型化）
- 新增枚举 `model/ArmStep`（FREE/GET/RUN/BACK/MOVE/RECEIVE），取代裸 int `WORK_*`；
  内嵌 Gson `TypeAdapter` **按 int 读写**，保证 MMKV 存量存档 `{"mValue":2}` 升级后仍可读回（存档兼容）。
- 新增 `model/DeviceRuntimeState`（纯 POJO）收拢一组调度运行态布尔
  （excutestart/gettraystart/movetraystart/backtraystart/…/uiinit/closedoor），
  从 `ObservableField` 降级为普通字段（仍随 Model 被 MMKV 序列化，重启恢复行为不变）。
- `PipeRNAModel`/`PipePCRModel`：`armStep` 改 `ObservableField<ArmStep>`，运行态布尔迁入 `runtime`。
- `Converters` 新增 `setIntStepString(TextView, ArmStep)` 绑定适配器重载，**XML 表达式 `@{model.armStep}` 不改**。

### 第 2 步：RNA 单链路试点（建立引擎骨架）
- 新增 `scheduling/SchedulingPort`（端口接口）、`scheduling/SchedulingEngine`、`model/RnaSchedulingState`（快照 POJO）。
- 把 `checkrnawork` 决策逻辑抽为 `SchedulingEngine.stepRna(RnaSchedulingState, tray)`；
  `APLManager.checkrnawork` 退化为 `snapshot → engine.stepRna → apply`。
- 引擎诊断日志经 `port.log(level,tag,msg)` 钩子保留（适配层转真 `android.util.Log`，测试里 no-op），
  从而引擎不 import `android.util.Log`、保持 JVM 纯净。
- 引入 `testImplementation org.mockito:mockito-core:5.11.0`，写 `SchedulingEngineRnaTest`。

### 第 3 步：PCR 链路（并行 stepPcr）
- 新增 `model/PcrSchedulingState`（多一个 `currentTrayKnown` 还原 PCR 的空 tray 守卫差异）。
- `SchedulingEngine.stepPcr` 与 `stepRna` 并列，复用同一 `SchedulingPort`；
  PCR 差异忠实保留：MOVE 找空闲 **RNA**（方向 PCR→RNA）、命令走 `DEVICE_PCR`。
- `checkpcrwork` 退化为委托；`SchedulingEnginePcrTest` 覆盖。

### 第 4 步：Tray 编排层（全量抽到引擎）
- 新增 `model/TrayDispatchSnapshot`（当前/错误调度项的极小快照 + 空值位）。
- 6 个 tray 方法抽到引擎：`checkTrayUrgent` / `checkTrayError()` / `checkTrayError(int)` /
  `checkTrayRnawork` / `checkTrayPcrwork` / `checkTrayEnd`；`APLManager` 对应方法退化为委托。
- 采用**端口回调式**（不做整盘快照）：引擎驱动遍历，所有读/写/分配/委托/副作用经 port 回调，
  设备完成写回、LED、`saveModel` 拆成**独立原子 port 方法**按原顺序编排，杜绝时序漂移。
- 设备链路委托走 `port.delegateRna/delegatePcr`（调已引擎化的 `checkrnawork/checkpcrwork`，各自建快照，无双快照）。
- `SchedulingEngineTrayTest` 覆盖。

### 附加需求：手动"放回"时自动 reset
- RNA/PCR 详情页"放回"按钮（`sendBackTray`）在置 BACK 前**自动 `reset(index)` 一次**；
  reset 成功才清 `usererror`；失败退回原行为。省掉"先手点复位再放回"。仅"放回"走自动 reset。

---

## 4. 关键设计约束（都被单测锁定）

| 约束 | 说明 |
|---|---|
| 返回值语义 | `stepRna/stepPcr` 返回 `true`=空闲；仅 GET/MOVE/BACK **成功下发命令**时返回 `false`（占用机械臂、短路整轮） |
| BACK 门控不对称 | BACK 只判 `WSSStart`，不含 `SelfCheck`（与原实现一致，勿归一化） |
| MOVE 回退 | 找不到空闲对侧设备 → `armStep=BACK`、贯穿 `return true` |
| Tray 返回不对称 | `checktrayurgent` 中止 return false；`checktrayerror()` 无参内层中止 return **true**、异常 try/catch return false |
| 写回顺序 | 错误路径：先 `setTrayFlagFree` 再 `markDeviceScheduleDone`；checktrayend：advance 分支不发 LED、`save` 落在 if/else 之后 |
| 存档兼容 | `ArmStep` 按 int 序列化，兼容旧 MMKV 数字存档；运行态仍随 Model 持久化 |
| 引擎纯净 | `SchedulingEngine/SchedulingPort/*Snapshot` 无 `android`/`androidx`/`manager` import（`ConstantData` 为纯常量类，允许） |

---

## 5. 文件清单

### 新增（生产代码）
| 文件 | 作用 |
|---|---|
| `model/ArmStep.java` | 调度步骤枚举 + Gson int 适配器 |
| `model/DeviceRuntimeState.java` | 设备调度运行态 POJO |
| `model/RnaSchedulingState.java` | RNA 调度快照 |
| `model/PcrSchedulingState.java` | PCR 调度快照（含 currentTrayKnown） |
| `model/TrayDispatchSnapshot.java` | 托盘当前/错误调度项快照 |
| `scheduling/SchedulingPort.java` | 引擎↔外界端口接口（门控/查询/命令/快照/tray/log） |
| `scheduling/SchedulingEngine.java` | databinding-free 调度引擎（stepRna/stepPcr + 6 个 tray 方法） |

### 新增（测试）
| 文件 | 用例数 |
|---|---|
| `test/.../SchedulingEngineRnaTest.java` | 14 |
| `test/.../SchedulingEnginePcrTest.java` | 12 |
| `test/.../SchedulingEngineTrayTest.java` | 26 |

### 修改
| 文件 | 改动 |
|---|---|
| `manager/APLManager.java` | 新增内部类 `AplSchedulingPort`（约 57 个适配方法）；`checkrnawork/checkpcrwork/checktray*` 退化为委托；持有 engine/port |
| `manager/RNAManager.java` / `PCRManager.java` | armStep/运行态读写点改用枚举 + `runtime` |
| `fragment/model/PipeRNAModel.java` / `PipePCRModel.java` | armStep 改枚举、运行态迁入 `runtime` |
| `adapter/Converters.java` | 新增 `setIntStepString(TextView, ArmStep)` 重载 |
| `fragment/RnaDetailFragment.java` / `PcrDetailFragment.java` | `sendBackTray` 放回前自动 reset |
| `app/build.gradle` | 加 `testImplementation mockito-core:5.11.0` |

---

## 6. 验证方式
- 编译：`./gradlew :app:compileDebugJavaWithJavac`（offline）成功。
- 单测：`./gradlew :app:testDebugUnitTest --tests "com.paul.pipeline.scheduling.*"` → 52 全绿。
- 纯净性：grep 确认引擎侧文件无 `android`/`androidx`/`manager` import。
- 真机回归：见 `调度重构_真机回归checklist.md`（armStep 存档兼容、加急优先级、整盘结束、异常还盘含 reset 前置、门互锁）。

---

## 7. 后续可做（未包含在本次）
- 把设备侧状态回流（模板跑完 Linux 回报 finish、断线重连 status 同步）也纳入引擎可测范围。
- `AplSchedulingPort` 目前是 `APLManager` 内部类；后续可抽成独立类进一步减小 APLManager 体量。
- UI 模型的展示字段与调度字段可进一步物理分离（本次仅逻辑解耦，UI 模型仍是 BaseObservable）。
