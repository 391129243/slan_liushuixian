# Client OOM 问题分析与修复总结

> 日期：2026-10-28
> 模块：`com.gzlab.autoassemblyline_client`（PCR client）
> 关键词：OutOfMemoryError / 日志上报 / WSS 重连 / 线程池泄漏 / 内存监控

## 一、现象

2026-10-12 client 崩溃，`tombstone` 与 `PCR_CONTROL/*_crash.txt` 一致：

```
java.lang.OutOfMemoryError: Failed to allocate a 55304 byte allocation
  with 776 free bytes and 776B until OOM, target footprint 268435456, growth limit 268435456
  at com.google.gson.Gson.toJson(...)
  at ...WSSModuleManager.sendLogMessage(WSSModuleManager.java:532)
  at ...LogcatHelper.uploadSingleFile(LogcatHelper.java:555)
  at ...LogcatHelper$1.run(LogcatHelper.java:393)   // LogUploadTimer 线程
```

Java 堆打满 256MB（`255MB/256MB, 0% free`），连 `Forcing collection of SoftReferences` 都回收不到内存 —— 说明堆里是**强引用可达的存活对象**，真实泄漏，不是瞬时 GC 压力。崩溃栈里的 Gson 序列化（55KB）只是压垮点，不是根因。

## 二、诊断依据（两份 log 交叉验证）

| 来源 | 内容 |
|---|---|
| tombstone（10-12） | OOM 现场 + 全线程栈。线程名编号涨到 89000+（`pool-89550-thread-1`、`Timer-13`、`WebSocketWriteThread-89669`），说明有对象被反复创建却从不释放 |
| 运行日志（10-27, pid 3099, 20:48~23:59） | 录下了发病过程 |

运行日志关键统计（约 3.2 小时、60MB）：

- `Temperature:` **468,318 行**、`E EventBus:` **29,224 行** —— 占全部日志 95%+
- 日志生成 ~177 行/秒，上传每次只发 16KB，**永远追不上**
- WSS 于 **22:08:44 断开后 1h51m 零重连**，之后 3335 次 `PCRLog发送失败: WSS未连接`
- 待上传积压一路涨到 **56MB** 从不回落

**死亡链**：日志生产 ≫ 上传消费 + WSS 断连不恢复 → 待上传数据在堆里无限积压 → 序列化时 OOM。

## 三、四处修复

### 修复 1 —— Temperature 日志刷屏（O(n²)）

**位置**：`SLANModuleManager.getBioWayTempertures()`

**问题**：循环内对整条实验历史的温度数组逐条打印三行 `Log.i("Temperature", ...)`。该方法被曲线上报（每 30 秒）调用，每次都重建整条历史数组并全量重打 —— 日志量随实验时长按 O(n²) 增长。

**修复**：删除循环内三行打印，保留温度数组构建逻辑。

```java
// 修复前
for (Temperature temperature : slanTemperatures) {
    Log.i("Temperature","==getBlockTemperature=="+ temperature.getBlockTemperature());
    Log.i("Temperature","==getTubeTemperature=="+ temperature.getTubeTemperature());
    Log.i("Temperature","==getHotlidTemperature=="+ temperature.getHotlidTemperature());
    biowayTemperatures.add(new Temperature(...));
}
// 修复后：去掉三行 Log.i，仅保留 biowayTemperatures.add(...)
```

### 修复 2 —— MainActivity 每 2 秒 NPE

**位置**：`MainActivity.onSLANDebugStatusUpdate()`

**问题**：对 `event.pcrTemplateName` / `runningPcrName` / `currentTemplateDirPath` / `experimentFilePath` 直接调 `.isEmpty()`，字段为 null 时抛 NPE，被 EventBus 每 2 秒（调试状态发布周期）捕获并打印整条堆栈。

**修复**：4 处改为 `TextUtils.isEmpty(...)`（新增 `import android.text.TextUtils;`）。

### 修复 3 —— WSSClient 生命周期泄漏 + 断连不恢复

**位置**：`WSSModuleManager`

**问题 A（线程池泄漏）**：`safeCloseWssClient()` / `closeWssConnection()` 原来只调 `wssClient.close()`，不释放每个 `WSSClient` 内部的 `messageExecutor`（`newSingleThreadExecutor`）。连接检查器每 2 秒重连一次、每次 `new` 一个新 WSSClient，旧实例的线程池永久泄漏（线程号涨到 89000+），最终吃光内存。
**修复 A**：改为 `wssClient.shutdown()`（内部 `messageExecutor.shutdownNow()` + `close()`）。

**问题 B（断连不恢复）**：`startWssConnection()` / `performReconnectTask()` 原来异步 `connect()` 后立即乐观地置 `isWssOpen=true`。若 TCP 连接静默卡住（`onOpen`/`onError` 都不触发），状态永远停在"已连接"，检查器再也不重试 —— 断连后永不恢复。
**修复 B**：改为 `connectBlocking(CONNECT_TIMEOUT_SEC=5s, TimeUnit.SECONDS)`，按真实握手结果驱动状态；失败走错误/重试分支。

**新增**：`isWssConnected()`（`isWssOpen.get() && wssClient != null && wssClient.isOpen()`），供日志上传模块查询。

> 注：Java-WebSocket 1.5.2 客户端单次使用，close 后不可复用。两处调用点都在 `resetWssClient()`/`performReconnectTask()` 先 shutdown 旧实例再 `getInstance()` 取新实例后执行，满足单次使用约束。

**问题 C（重连无退避 + 双入口语义重叠，2026-10-28 补充修复）**：重连节奏原为固定 2 秒 —— 长断网时每 2 秒 `new WSSClient` + `connectBlocking(5s)` 一轮，开销高；且 `scheduleReconnect()`（延时解锁器）与每 2 秒检查器两个入口语义重叠、可读性差。
**修复 C**：
- 加**指数退避**：失败后等待 2s→4s→8s→16s→30s 封顶（`RECONNECT_BACKOFF_MIN_MS=2000` / `RECONNECT_BACKOFF_MAX_MS=30000`），连接成功复位回 2s。删除旧常量 `RECONNECT_DELAY`。
- 状态字段 `mNextReconnectAtMs`（下次允许重连时刻，`SystemClock.elapsedRealtime` 基准）、`mCurrentBackoffMs`（当前退避时长）。
- 检查器节拍不变（每 2 秒），但 `performConnectionCheck()` 加 `isBackoffElapsed()` 闸门，未到时刻跳过本轮。
- `scheduleReconnect()` 改为**统一失败入口**：只做「解锁 `isReconnecting` + 武装退避闸门」，不再自起延时任务；真正重连始终由那一个 2 秒检查器在闸门到期后发起。
- 路径对齐：6 条失败路径（无网/连接异常/重连超时 `postReconnectFailed`/重连异常/初次连接失败）全部走 `scheduleReconnect()` 武装退避；2 条成功路径（`postReconnectSuccess`/`handleWssConnect`，后者覆盖初次连接的 `WSS_Connect` 事件）调 `resetReconnectBackoff()` 复位。
- 效果：长断网重连开销降一个量级（最慢 30 秒一轮），网络恢复后退避立即复位、不牺牲恢复速度。日志打 `重连退避：本次等待 Xms，下次退避 Yms`。

### 修复 4 —— 断连时暂停上传（不丢弃任何日志）

**位置**：`LogcatHelper.checkAndUpload()`

**问题**：WSS 断连期间日志仍在疯涨，上传却每轮照样读大文件、做 Gson 序列化，持续给堆施压。

**修复**：方法开头查询 `WSSModuleManager.isWssConnected()`，未连接直接 return —— 不读文件、不序列化。日志照常由 `LogDumper` 落盘，WSS 恢复后从已保存的进度指针继续增量上传，**一条不丢**。

```java
private void checkAndUpload() {
    WSSModuleManager wssManager = WSSModuleManager.getInstance(appContext);
    if (!wssManager.isWssConnected()) {
        Log.d(TAG, "WSS未连接，跳过本轮日志上传");
        return;
    }
    // ... 原有逻辑
}
```

> **设计决策**：最初实现过"单文件积压超 2MB 则快进读取位置、丢弃最旧增量"的封顶策略，后按需求改为**断连期间日志一条都不能丢**，已移除丢弃逻辑及 `MAX_PENDING_PER_FILE` 常量。内存安全由两层保证：① 上传恒为 16KB 分块固定分配，无大块尖峰；② 断连时整轮跳过。

**日志完整性边界**：补传依赖 `LogDumper` 落盘策略 —— 单文件 20MB 滚动、过期目录保留 `SDCARD_LOG_FILE_SAVE_DAYS = 10` 天。若断连超过 10 天、或积压文件被过期清理删除，则补传不完整。断连可能长于 10 天时需调高保留天数。

## 四、内存监控（MemoryMonitor）

从 `D:\PCRinOne\framework\bio_pcr_6in1\6in1_Client\Bio_PCR` 移植 `util/MemoryMonitor.java`（仅改包名为 `com.gzlab.autoassemblyline_client.util`）。

- 单例 + 守护线程，默认 **60 秒**一次，用 `Log.i("MemoryMonitor", ...)` 输出（随 LogDumper 落盘、随 WSS 上传）。
- 每项带 **Δ 增量**对比上次快照，用来判断"哪块内存在涨、是否只涨不回落"：
  - `[JavaHeap]` 已用/上限 + 百分比 —— 对应本次 OOM（堆打满）
  - `[Native]` native 堆
  - `[PSS total]` / `[PSS 分类]` java/native/code/graphics/stack/other/system
  - `[Process]` VmRSS/VmSwap/VmSize
  - `[Threads]` 总数 + `[Thread明细]` 按线程名归类计数 + CPU jiffies —— 对应 WSSClient 线程泄漏
  - `[FD明细]` 按 socket/pipe/anon/file/logfile 归类 —— 对应资源未关闭
- **接入点**：`PcrApplication.onCreate` 日志收集线程启动后 `MemoryMonitor.getInstance().start();`
- 排查用 `MemoryMonitor` 关键字过滤日志；`dumpOnce()` 为 public，可在实验开始/结束等关键节点手动调用做前后对比。

## 五、验证

`./gradlew :app:compileDebugJavaWithJavac` 退出码 0，仅工程原有的 deprecation/unchecked/varargs 警告，无新增错误。

## 六、涉及文件

| 文件 | 改动 |
|---|---|
| `SLANModuleManager.java` | 修复 1：删除温度逐条打印 |
| `MainActivity.java` | 修复 2：4 处 `TextUtils.isEmpty`；新增 import |
| `WSSModuleManager.java` | 修复 3：shutdown 释放线程池、connectBlocking、新增 isWssConnected |
| `LogcatHelper.java` | 修复 4：断连跳过上传；移除积压封顶丢弃逻辑 |
| `util/MemoryMonitor.java` | 新增：周期内存监控 |
| `PcrApplication.java` | 接入 MemoryMonitor.start()；新增 import |
