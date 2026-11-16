# 调度重构 · 真机回归 Checklist

适用范围：APLManager 调度决策重构（armStep 枚举化 + DeviceRuntimeState + SchedulingEngine 抽取 RNA/PCR/tray）。
单测已覆盖决策逻辑与调用顺序（RNA14+PCR12+Tray26=52 全绿）；本清单覆盖**只能在真机确认**的部分：
存档升级兼容、机械臂真实时序、UI 绑定刷新、门互锁。

> 记录约定：每条填 通过 / 失败 / 跳过 + 现象/日志关键字（TAG: `ArmTaskWorker`）。
> 建议全程 `adb logcat -s ArmTaskWorker` 跟日志。

---

## 0. 前置准备
- [ ] 用**旧版本 APK**（重构前）跑一轮完整调度，确保 MMKV 里已有存量数据：
      key 形如 `RNA1/RNA2`、`PCR1..6`、`Tray1..8`（见 RNAManager:85 / PCRManager:121 / TrayManager:83）。
- [ ] 不清数据，直接覆盖安装**重构后 APK**（关键：验证存档升级，不能卸载重装）。
- [ ] 记录测试环境：设备号、固件版本、APK commit。

---

## 1. armStep 存档升级兼容（最高优先级）
重构把 `armStep` 从 `ObservableField<Integer>` 改为 `ObservableField<ArmStep>`，并用 Gson `Adapter` 按 int 读写。
存量存档里是数字 `{"mValue":2}`，必须能正确读回枚举。

- [ ] 覆盖安装后**冷启动**，进主界面：RNA/PCR/托盘状态显示与升级前一致，无异常码、无“未初始化”错乱。
- [ ] 制造“中途态存档”再升级：旧版跑到某设备处于 `WORK_RUN/MOVE/BACK`（armStep=2/4/3）时强杀 App → 换新版启动 →
      该设备 armStep 文案（取托盘/跑模板/送托盘/移托盘）与强杀前一致，调度能继续而不是从头。
- [ ] 检查日志无 Gson 反序列化异常、无 `armStep` 为 null 导致的 NPE。
- [ ] 反向确认：新版运行后写回的存档再被新版读取正常（写=int，读=int，闭环）。

> 若此项失败：优先看 `ArmStep.Adapter.read/write` 与 MMKV `GSON` 配置；不要继续后面用例。

---

## 2. 界面绑定刷新（armStep → XML）
`app:intStepString="@{model.armStep}"` 现绑定到 `ArmStep`，经 `Converters.setIntStepString(TextView, ArmStep)` 重载显示。

- [ ] RNA/PCR 详情页与列表页，调度推进时状态文案实时变化：未调度→就绪→调度中→移盘/接盘→调度完成。
- [ ] 文案与旧版逐字一致（重载只是转 int 复用原分支，不应有新文案或丢文案）。
- [ ] `armStep=FREE` 且触发界面初始化（uiinit）时，UI 正确复位。

---

## 3. 加急优先级（checktrayurgent 4 趟顺序）
引擎复刻：关核提门(1/2) → 自检完成 → 加急RNA → 加急PCR → 普通RNA → 普通PCR，任一中止则整轮停。

- [ ] 同时投 1 个加急盘 + 1 个普通盘：**加急盘优先**被机械臂取走/推进，普通盘等待。
- [ ] 多加急盘：加急盘之间按托盘序推进，且加急整体先于普通。
- [ ] 关门前置：起始阶段核提处于关门态（`checkrnaclosedoor`）；若门未到位，调度不启动、日志出现 `rna: 1/2 closedoor`。
- [ ] 自检未完成（SelfCheck=false）时不调度，日志 `checktrayurgent ... SelfCheck:false`。
- [ ] 机械臂忙时中止本轮，下一周期（5s）重试，不串任务。

---

## 4. 分配空闲设备（checktrayRnawork/Pcrwork）
调度项 STATUS_FREE 且门开/WSS/自检/臂空闲齐备 → 分配空闲 RNA/PCR、置 STATUS_RUN、setCurrentTemplate、setCurrentDispatch。

- [ ] 单盘首个 RNA 步骤：正确分配到某台空闲 RNA，界面该 RNA 绑定该托盘/模板。
- [ ] RNA→PCR 移盘：MOVE 找空闲 PCR（方向 RNA→PCR）；PCR→RNA 反向亦然。
- [ ] **无空闲对侧设备**时回退：MOVE 找不到空闲 PCR/RNA → armStep 转 BACK（送回托盘），日志 `找不到空闲.*还托盘WORK_BACK`。
- [ ] 分配后 `setCurrentDispatch` 生效，`getCurrentDispatch` 能取回同一调度项（下发命令依赖它）。

---

## 5. 整盘结束（checktrayend）
推进 dispatchindex；整盘结束置 str_FINISH + flagstatus=FLAG_FREE + 关 LED + saveModel。

- [ ] 单列完成：设备回盘完成（backtrayfinish && armStep==BACK）→ 该设备 armStep 复位 FREE、uiinit=true、
      dm.status=FINISH、dispatchindex+1，进入下一列。
- [ ] 整盘所有列完成：tray.status=finish、flagstatus 复位、**LED 关闭**、存档已保存（重启后仍为 finish）。
- [ ] **时序确认（易回归点）**：advance（还有下一列）分支**不关 LED**；只有整盘 finish 才关 LED。
      观察 LED 灯：中途推进不应熄灯，整盘完才熄。
- [ ] dispatchindex 越界（>size）路径也能正确收尾为 finish + 关 LED + save。

---

## 6. 异常还盘（分自动 / 手动两条路径）
共同下游：托盘置 `str_ERROR`+记 `errorDispatchindex`（`TrayManager.errorDispatch`）→ 5s 周期 `checktrayerror()`
（已抽引擎）定位错误调度项（errorDispatchindex 或 dm.status/devicestatus==ERROR）→ 委托设备 → 回盘完成写回：
**先 flagstatus=FLAG_FREE，再设备 armStep=FREE + uiinit=true**（顺序单测已锁，真机确认硬件动作一致）。

### 6-前置. RNA/PCR 必须先 reset 才能还盘（关键前置）
异常后 RNA/PCR 停在 **error/alarm 状态**，`status/linuxStatus` 不是 FREE。而机械臂还盘走的是
`checkrnawork/checkpcrwork` 的 **WORK_BACK** 分支，其快照前置是 `statusFree = status==FREE && linuxStatus==FREE`
（见引擎 `stepRna/stepPcr` 顶部守卫）。**设备不 reset 清错 → statusFree=false → 引擎在 WORK_BACK 前直接 return，
机械臂永远不还盘。** 故 reset 是还盘的硬前置。reset 本身是下发 WSS 强制复位（`sendRNAResetWSSMessage(key,1)`），
设备复位后**异步回报 status→FREE**（下一 5s 轮询 `refreshModel` 回填）。

> **变更（最新）**：详情页“放回”按钮现在**自动替用户 reset 一次**，无需再先手点复位。
> `sendBackTray()` 内先 `RNAManager/PCRManager.reset(index)` 一次 → 成功才清 `usererror=0` → 再置 armStep=BACK。
> 只试一次；失败退回原行为（仍置 BACK，等用户稍后手动复位）。仅“放回”走自动 reset；
> **停止(stopBtn)、报警弹窗复位、自检复位、托盘管理页仍是各自原有（手动）路径。**

reset 的所有入口（区分自动 / 手动）：
- **自动（本次新增）**：详情页“放回” `sendoutBtn`→`sendBackTray` 内 `reset(index)`（RnaDetailFragment / PcrDetailFragment）。
- 手动：报警弹窗“复位”（`ArmErrorDialog.setResetButton`→`resetErrorRNA/PCR`→`reset()`+`clearAlarm`）；
  详情页“复位”按钮 `resetBtn`（RnaDetailFragment:89 `ExecuteResetForce` / PcrDetailFragment:109 `reset`）；
  自检流程逐台 `reset`（SelfcheckFragment:545/553）。

- [ ] **点“放回”即自动 reset（正向，核心）**：异常设备详情页**直接点“放回”**（不先点复位）→
      日志出现 `ExecuteResetForce`/复位下发 → 设备下一轮回 FREE → `checktrayerror`→WORK_BACK 触发，机械臂取回该盘；
      toast 显示“已复位，等待调度放回”。**确认无需先手点复位即可完成还盘。**
- [ ] **reset 成功才清 usererror**：放回后若 reset 成功，`usererror=0`、UI 总状态脱离 ERROR；
      reset 失败则 usererror 不动、UI 仍显 ERROR（提示去确认设备）。
- [ ] **reset 失败降级**：断开 WSS / key=null 时点“放回”→ toast“复位发送失败，仍尝试放回，请确认设备状态”→
      仍置 armStep=BACK，但设备未回 FREE 故机械臂暂不取盘；恢复后手点复位或再次放回可继续。不崩、不卡死。
- [ ] **只试一次**：点一次“放回”只下发一次 reset，不重发刷屏。
- [ ] armStep 枚举改造交叉：reset 使设备回 FREE 后 `armStep` 仍为 BACK（等还盘），statusFree 变 true 才放行；
      确认 armStep 枚举值在 reset 前后读写正确（不因 reset 被误清）。
- [ ] 其它 reset 入口（手动复位按钮 / 弹窗 / 自检）行为与旧版一致，未被本次改动影响。
- [ ] reset 成功但设备迟迟不回 FREE：机械臂持续不还盘（不卡死、不误触），状态恢复后能继续。

### 6a. 异常自动还盘（设备/Linux 上报触发）
> 完整时序：设备报错 → 托盘 ERROR → **用户 reset 清错（见 §6-前置）** → checktrayerror 驱动还盘。
- [ ] 设备运行中真实报错（Linux 侧异常）→ 托盘自动转 ERROR，日志 `errorDispatch tray:` + `errorDispatchindex:`。
- [ ] **先 reset** 使设备回 FREE 后，`checktrayerror()` 驱动机械臂把该盘从出错设备**取回到托盘位**；
      取回后设备 armStep 复位 FREE、界面刷新。（未 reset 时不还盘属预期，见 §6-前置负向用例。）
- [ ] `exist` 传感器为空/false 时**不做**归位写回（守卫生效），不误判。
- [ ] 委托返回“忙”（false）本轮中止；`checktrayerror()` 无参在内层中止时返回 **true**，
      使 checkArmTask 本周期**跳过普通/加急调度**，下一周期重试（这条不对称是承重点）。

### 6b. 自检触发的批量异常还盘（SelfcheckFragment）
- [ ] 进自检：对每台 `exist` 的 RNA/PCR 批量 `NotifyXxxTrayError` + `checkXxxFinishError`
      （SelfcheckFragment:1238/1245）→ 所有带盘设备被标记异常并等待取回，自检期间不与调度打架。
- [ ] 自检把设备强制推到 `armStep=BACK`（`checkXxxFinishError` 直接改 armStep+清 backtray 标志），
      随后机械臂逐一还盘；确认无遗漏、无重复取盘。

### 6c. 异常手动还盘（详情页 / 托盘管理页 用户操作）
两条子路径都触及重构改过的字段，需分别测：
- [ ] **停止并交调度放回**：RNA/PCR 详情页或托盘管理页点“停止”→ `usererror=1` + `NotifyXxxTrayError`
      （RnaDetailFragment:77 / PcrDetailFragment:95 / TrayManageFragment:192/205）→ 走 errorDispatch → ERROR 态。
      此时设备仍 error，**需先 reset（§6-前置）** 使 statusFree 成立，`checktrayerror` 才取回；
      toast“停止成功，请等待”，reset 后机械臂还盘。
- [ ] **直接放回（现自动 reset）**：详情页点“放回”→ `sendBackTray` **先自动 `reset(index)` 一次**（成功清 usererror）
      → 再 `checkRnaFinishError/checkPcrFinishError`（RnaDetailFragment:188 / PcrDetailFragment:195）**置 armStep=BACK**、
      `excutefinish=true`、清 backtray 标志 → toast“已复位，等待调度放回” → 设备回 FREE 后机械臂送回托盘。
      重点确认：armStep 枚举改造后这条直达路径写回正确、界面文案切到“送托盘”。**详见 §6-前置的自动 reset 用例。**
- [ ] **手动恢复正常**：托盘管理/详情页对 ERROR 盘“重新下发/恢复”→ `errorDispatchindex=0`、`dispatchindex=1`
      （TrayDetailFragment:200-201）→ 托盘脱离 ERROR、可重新从头调度；确认存档保存后重启仍为恢复态。

### 6d. 交叉
- [ ] 异常还盘期间点”开门”：开门优先，机械臂完成当前原子动作后暂停，不把盘丢在半途。
- [ ] 手动停止与自动异常同时发生（并发）：不重复取盘、不状态错乱。

### 6e. 放回/复位后托盘异常停止（本次修复，阻断项）
**修复内容**：`放回`/`复位` 补调 `NotifyXxxTrayError` 标托盘 `str_ERROR`，使 `checkTrayEnd` 不再推进 dispatchindex。

- [ ] **核心正向**：RNA/PCR 异常后，点`放回`（sendBackTray）→ 托盘变 ERROR（UI 红色）→ 机械臂取回后**不再推进 dispatchindex 到下一步**，后续流程停止。
- [ ] **复位正向**：异常后点`复位`（resetBtn）→ 同样置托盘 ERROR → 不再推进。（复位时无托盘则 no-op，不误标。）
- [ ] **停止按钮/自检/ack-error 行为不变**：上述三条原有路径与放回/复位结果一致，不重复取盘、不状态错乱。
- [ ] **恢复流程**：托盘异常状态下，手动在 TrayDetailFragment 重新下发（`errorDispatchindex=0`、`dispatchindex=1`）后，调度恢复正常推进。
- [ ] **无托盘时复位 no-op**：设备上无托盘时点复位，`NotifyXxxTrayError` 因 `currentTray==null` 守卫自动 no-op，不误标其他托盘。
- [ ] **与 §5 整盘结束不干扰**：整盘所有列正常完成（STATUS_FINISH）的推进逻辑不受影响，仅异常路径被拦截。

---

## 7. 门互锁（开门优先级最高）
所有 GET/MOVE/BACK 分支前置 `!doorhold`；开门事件优先。

- [ ] 调度进行中点“开门”：机械臂完成当前原子动作后**暂停新调度**，门解锁，日志 `已开门，不调度`。
- [ ] 关门回位后调度自动恢复，不丢步、不重复取盘。
- [ ] 开门态下强制不下发 GET/MOVE/BACK（日志 `...opendoor`）。
- [ ] 门未到位/上锁失败的提示与旧版一致（openDoor/closeDoor 文案未改）。

---

## 8. 稳定性 / 回归面
- [ ] 连续跑 ≥3 个完整整盘（含 RNA→PCR 多列模板），无卡死、无状态机死锁、无重复下发。
- [ ] 5s 周期任务（APLService Timer）长时间运行无内存增长异常（关注 MMKV 频繁 saveObject）。
- [ ] 与旧版对照：同一批样品、同一模板，端到端时间与产物一致。
- [ ] logcat 无新增 `NullPointerException` / Gson 异常 / `找不到调度数据` 的非预期爆发。

---

## 回归结论
- [ ] 全部通过 → 可发布。
- [ ] 存档兼容（§1）、异常还盘（§6，含 **§6-前置 reset 后才能还盘** 与手动放回 `checkXxxFinishError` 直达路径）
      或门互锁（§7）任一失败 → **阻断发布**，回到对应代码定位。
- [ ] 仅日志文案差异（非行为）→ 记录，不阻断。

失败项请附：用例编号、现象、logcat 片段、复现步骤。
