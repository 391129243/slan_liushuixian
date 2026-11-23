# 真实 PCR 结果文件的 HTTP 上报模拟测试

测试结论：dataserver 的结果 JSON 接收、原始 PCR 附件接收、保存、查询、去重、下载与重启恢复通过。数据来自本机宏石厂商 DLL 对指定真实文件的离线读取，不使用 Vue 的演示曲线，不连接物理设备。

## 输入和读取依据

- 原文件：`AIO_AAA_2026-11-16-11-01-01_07-自研试剂ab管多重测试_20261020/2026-11-16-13-04-40.pcr`。
- 大小：5,736,758 字节。
- SHA256：`577afff986549e59d29f611358a776b5e1c0439bc34d4504cbda7889724031e2`，测试前后及上传下载结果一致。
- 厂商读取：所提供 9.0.1.260921 发行包的 `Service.SLANService.LoadExperimentResult`，在独立 .NET Framework 进程内直接调用同一离线实现。先复制输入，按厂商公开 `Images.DumpInvoke.Init()` 初始化其算法包；只补充离线模型要求的 UI 颜色资源，不修改算法。未启动新厂商服务，未调用设备查询、运行、停止或释放。
- 厂商进程存在，但本次 HTTP 8186 端口不可达，因此没有将 HTTP 服务不可用误报成文件无法解析，也没有冒充已完成厂商 HTTP 端到端验证。
- 新旧 API 的 `LoadExperimentResult` 返回完整 Experiment，不带 APIExperimentResult/ErrorCode 包装；已修复 C# 桥接此前按错误包装解读该返回值的问题，并通过协议替身回归测试。

## 真实数据概况

| 项目 | 数量或内容 |
| --- | --- |
| 实验类型 | Type=1，熔解实验 |
| 实验时间 | 2026-11-16 12:02:47 至 13:04:31 |
| 设备型号 | SLAN-16H |
| 孔位 | 8 |
| 靶标结果 | 24 |
| 通道编号 | 1、3、4 |
| 原始熔解曲线点 | 22,032 |
| 峰曲线点 | 12,024 |
| 峰记录 | 21 |
| 无有效 Ct 的靶标 | 24，SDK 返回 NaN，传输中保留缺失语义为 null |
| 测试上传 JSON 大小 | 1,596,505 字节 |

这是熔解数据。扩增 RawCurve/AMPCurve 为空并不表示该文件没有曲线；实际内容在 RawMeltCurve/PeakCurve/PeakValues 中。上述数字是 SDK 返回数据的统计，不是对真实检测结论的判定。

## 协议核对和数据适配

参考文件：

- `E:/PCRinOne/framework/Bio_PCR_20230927/Bio_PCR/app/src/main/java/com/gidi/bio_pcr/hongshi/SLANManager.java`：uploadSLANResult 已启用。
- `../slan_liushuixian_server_newschedule/Bio_PCR/app/src/main/java/com/gidi/bio_pcr/slan/SLANManager.java`：同类 HTTP 示例目前被注释，不能据此声称运行中的设备已经启用 HTTP 上报。

均采用 `POST /upload` + `Content-Type: application/json` + 完整 Experiment 根对象，不带 Type/SeqID 包装，不需要本测试额外增加设备认证头。`uploadSLANResult(Experiment,File,url)` 实际只发 JSON；PCR 附件另以 multipart 的 `file` 字段发送。原请求没有报告与附件的关联 ID，测试在上传后通过管理员接口显式关联，不靠时间猜测归属。

本测试以启用 HTTP 的 20230927 工程及其 20240722 SDK 模型作为 JSON 适配依据：

| C# SDK 字段 | Android 字段 |
| --- | --- |
| SerialNumber | SerialNo |
| UseModule | Block（旧模型为 byte） |
| Cn | CnValue |
| Rn | RnValue |
| PeakValues.Wm | PeakValues.Width |
| PeakValues.Ym | PeakValues.YValue |

新流水线工程引用的 260916 SDK 的 Block 是 int[]，不同于旧模型。本轮验证的载荷采用旧工程启用的 HTTP 模型；没有假定模块掩码与新模块数组编号等价。dataserver 当前保留 Block 原文，不解释它的硬件含义。

### 发现的客户端序列化问题

以参考工程的 Gson 2.8.9 和实际 SDK 模型，在本机复现 SLANManager 的 Double 序列化配置：NaN 会写成裸 `NaN` 标记，而非标准 JSON。严格 JSON 解析失败，dataserver 返回 HTTP 400 / Result=0。SDK JSON 中的字符串 `"NaN"` 也不能作为正常 Ct 传入。

本测试显式把 216 个非有限数值编码为 JSON null，不转成 0；随后按相同 HTTP 路径、Content-Type 和 Experiment 根结构上报成功。因此“接收端通过”不意味着原 Android 程序不作修改就一定可上传此文件：设备端仍需在序列化时处理非有限数，并检查 HTTP 状态和业务 Result。原代码的 onResponse 无条件调用成功回调，本轮未修改 Android 工程。

## 检查结果

| 检查 | 结果 |
| --- | --- |
| 未映射的 C# 根结构 | 400，缺少 SerialNo，未误报成功 |
| 原参考 Gson 生成的裸 NaN JSON | 400，Result=0 |
| Ct 为字符串 NaN | 400，Result=0 |
| 规范化真实 Experiment JSON 上报 | 200，Result=1 |
| 相同 JSON 重传 | 返回相同 ReportID，Duplicate=true |
| 四个并发 JSON 重传 | 返回同一 ReportID，数据库仍只有一份报告 |
| 详情返回的完整 Experiment | 与发送 JSON 深度逐项相等，包含全部曲线、峰和 null Ct |
| 原 JSON 下载 | 字节级一致 |
| 真实 PCR multipart 上传与重传 | 返回相同 FileID，只有一份文件记录 |
| 显式报告附件关联 | 成功 |
| 下载 PCR 的 SHA256 | 与原始文件一致 |
| 立即终止测试 Java 进程后重启 | 报告、附件关联及再次重传去重均通过 |
| 原始参考文件 | 未修改 |

测试使用独立随机本机端口和 H2 文件库，不污染默认运行数据库；Java 托管启动开关显式关闭，不启动厂商程序或设备。结束后关闭测试 Java 进程。

## 证据和复现

本次最后一轮：ReportID `51154ea4-3575-453b-8669-c2d5e39a27ef`，FileID `f1cc75a4-1d09-443b-8a8e-39f22f6be431`。这些 ID 属于独立测试库，不是默认运行库中的记录。

- `target/real-pcr-upload-20261121/vendor-experiment.json`：原始厂商离线输出。
- `target/real-pcr-upload-20261121/upload-experiment.json`：实际发送的规范化 Experiment JSON。
- `target/real-pcr-upload-20261121/android-original-upload.json.txt`：原 Gson 裸 NaN 的反例，仅用于序列化缺陷复现，不是合法 JSON。
- `target/real-pcr-upload-20261121/android-gson-probe.txt`：本机序列化复现结论。
- `target/real-pcr-upload-20261121/upload-test-result.json`：本次各项断言、统计、测试库路径和 ID。

读取后可重复运行实际 HTTP 测试，每次创建独立测试库：

```powershell
node scripts/simulate-real-pcr-upload.mjs
```

重新提取文件使用 `scripts/read-pcr-offline.ps1`，提供 `-Source`、`-OutputDirectory`、`-VendorDirectory` 三个参数。需要 Windows .NET Framework 编译器和完整原厂包；脚本只在输出目录生成编译文件、算法包副本、PCR 输入副本及结果 JSON。`target` 是测试输出，执行 Maven clean 会清除，应先保留需要的测试证据。
