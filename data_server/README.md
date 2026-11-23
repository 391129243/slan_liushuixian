# dataserver

Vue 3 新增“数据中心”，通过登录访问服务器的报告、文件和分析版本，支持上传 PCR、读取实际 Ct/曲线、二次分析、保存及下载新 PCR。默认首页仍为无需后端的“真实结果验证”离线快照，模拟工作台作为独立入口保留。数据中心地址为 http://127.0.0.1:5173/?mode=server ，需要启动后端。详见 [前端说明](frontend/README.md)。非有限数值的 Android 安全序列化示例见 [samples/android](samples/android/README.md)，原设备工程未修改。

真实读取 `.pcr`、二次分析和保存接口采用 Java → C# 离线适配 → 厂商服务。正常模式使用 `SLAN.WCFServer.exe`；新增离线启动模式使用真实 SDK 的隔离文件宿主，不依赖 8186 或设备。二次分析结果自动写入分析版本表，导出的新 PCR 关联到对应版本。“数据中心”调用这些在线接口，离线快照与模拟工作台仍独立。详见 [C# 接口说明](csharp/README.md)。真实 SDK 的复算、保存、重新打开及浏览器/Java 全流程已通过离线验证。

Java 21 + Spring Boot HTTP 数据接收服务第一版。已实现设备实验结果上报、原文及曲线持久化、重复上报去重、PCR 文件存储、管理查询和下载。使用文件型 H2，不要求安装 MySQL 或 Redis；正常后端启动需要 Windows、厂商发行包和本机 .NET 运行环境。单独模拟前端不要求这些服务。

## 协议依据

本版依据 `E:/PCRinOne/framework/Bio_PCR_20230927/Bio_PCR/app/src/main/java/com/gidi/bio_pcr/hongshi/SLANManager.java` 的 `uploadSLANResult()`、`uploadFile()`，以及 `SLANExDetailActivity.uploadExResultByHttp()`。同时静态检查该项目 `slansdk V1.0.1 20240722.aar` 的模型字段。没有使用 WebSocket，也不要求 Type、SeqID、DeviceID 包装。

实际协议：

- `POST http://服务器:端口/upload`，`Content-Type: application/json`，body 直接为 Gson 序列化的 Experiment。
- 字段区分大小写：`Name`、`SerialNo`、`StartTime`、`EndTime`、`Block`、`WellResult`、`TargetStd`、`Temperatures`；孔内为 `TargetResults`，结果包含 `Ct`、`RawCurve`、`AMPCurve` 等；曲线点为 `X`、`Y`。未识别的扩展字段也完整保留。
- 原 SDK 的 `Block` 是 byte，不应照其他新版 SDK 强制要求 int 数组。本服务保留原值，不绑定厂商模型类。
- 原调用即使传入 File，`uploadSLANResult()` 也只上传 JSON，没有同步上传文件。独立 `uploadFile()` 才以 multipart 的 `file` 字段上传，且该方法的 URL 在原工程内硬编码，未发现其实际调用。
- 两次请求之间没有原生关联标识。本服务保存独立附件，通过管理员显式关联 ReportID/FileID；不会自动绑定到“最近的实验”。

成功应答示例：

```json
{"Result":"1","Message":"succeed","ReportID":"uuid","FileID":null,"Duplicate":false}
```

`Result` 字段沿用原代码注释中 `ResultMsg.Result == "1"` 的习惯；原代码实际没有校验该值，也没有检查 HTTP 状态码，所有 `onResponse` 都调用成功回调。这是设备端现有限制：服务端失败返回非 2xx 和 `Result:"0"`，后续设备端需一起修正判断。当前未修改参考 Android 工程。

## 运行

Windows 可直接双击 [start-dataserver.bat](start-dataserver.bat)，账号 `admin`、密码 `12345678`。脚本自动切换到项目目录并调用 `run.ps1`，默认自动启动或复用厂商程序和 C# 桥接；控制台保持显示日志。命令行执行 `start-dataserver.bat -Build` 可重新构建。关闭服务可按 Ctrl+C。此脚本只启动后端，Vue 前端需另行启动。

暂不接设备时，双击 [start-dataserver-offline.bat](start-dataserver-offline.bat)，再双击 [start-frontend.bat](start-frontend.bat)，打开 http://127.0.0.1:5173/?mode=server ，使用 `admin / 12345678` 登录。离线模式调用真实厂商文件 SDK，支持实际 Ct 复算及 PCR 保存，不启动或停止既有厂商 EXE。后端/C# 固定为 8088/5099，端口已被占用则拒绝启动，不替换其他进程。启动前自动准备 SDK 离线工具；`-Build` 可重新构建 Java/C#。Ctrl+C 关闭本启动器创建的进程。

离线模式数据库为 `runtime/offline-server/db/real-sdk.mv.db`，文件在 `runtime/offline-server/files`，工作副本在 `runtime/offline-server/work`，与正常模式的 `runtime/database`、`runtime/files` 分开。重启离线模式沿用原库；不自动向正常库导入。备份时需要同时备份对应数据库和文件目录。

已在本机准备好 Maven、现有 JDK 21，并生成 `target/dataserver-0.1.0.jar`。

```powershell
cd E:\PCRinOne\framework\bowei_liushuixian\liushuixian_melody\data_server
$env:DATASERVER_ADMIN_PASSWORD = '12345678'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\run.ps1
```

`run.ps1` 自动构建缺失的 C# 适配程序，Java 启动时由生命周期管理器拉起厂商 EXE 和适配程序。直接 `java -jar target/dataserver-0.1.0.jar` 同样自动启动，但需先完成 C# 构建。相对路径均按 dataserver 工作目录解析。

- 厂商程序默认位置是本项目提供的 `SLAN_9.0.1.260921_API_接口文档与完整程序_20261116/.../SLAN 9.0.1.260921_API/SLAN.WCFServer.exe`，通过 `DATASERVER_SLAN_DIR` 覆盖目录。
- 厂商进程工作目录固定为 EXE 所在目录，隐藏启动，日志在 `runtime/managed/`。
- 复用同一路径的已运行厂商进程；未确认归属的端口占用会拒绝启动。正常关闭或启动失败时只终止本次创建的进程，不关闭原有进程，也不发送厂商 Exit/Release/Stop 命令。
- 等待 GetVersion 和适配能力查询就绪，默认每阶段最多 45 秒。缺失依赖、端口冲突、HTTP 权限或超时会明确使后端启动失败，查看 `runtime/managed/*.log`。
- 不自动提权或弹出 UAC。若厂商 HTTP 服务要求管理员权限，应从具有相应权限的终端启动后端。
- 纯模拟或后端测试设置 `$env:DATASERVER_MANAGED_ENABLED='false'`，不自动启动这两个进程。已有命令行脚本测试均显式关闭此开关。

仅正常关闭 JVM 时能执行托管清理；强制结束进程或掉电不保证子进程退出。再次启动会先识别已运行程序，避免重复启动。

默认监听 `127.0.0.1:8088`。设备联调时，将 `DATASERVER_BIND` 设为接收数据的本机局域网 IP；在设备系统设置中填写服务器 IP 和端口 `8088`，选择 HTTP 上报。不要把 URL 路径重复填进设备 IP 栏，设备会自行拼接 `/upload`。

```powershell
# 需设备联调时，在启动之前设置实际 IP
$env:DATASERVER_BIND = '192.168.1.104'
$env:DATASERVER_PORT = '8088'
```

旧设备不发送身份凭据，因此兼容 `/upload` 默认不要求额外认证，并按 `SerialNo` 自动登记旧设备。它不能证明硬件真实身份，须放在可信内网或由网关限制来源；不宜直接开放公网。配置 `DATASERVER_LEGACY_UPLOAD_ENABLED=false` 可完全关闭旧入口。管理接口始终需要 HTTP Basic，用户名 `admin`；跨不可信网络应由反向代理提供 HTTPS。

管理员新建的设备默认禁止无凭据的旧接口，仅可使用带 `X-Device-Id`、`X-Device-Key` 的 `/api/device/reports`，body 仍为相同 Experiment JSON，并要求 `SerialNo` 匹配身份。旧设备可被管理员禁用；不再上报不等于离线，设备列表只展示 `recent_upload`，没有伪造心跳在线状态。

手动上传样本：

```powershell
curl.exe -H 'Content-Type: application/json' --data-binary '@samples/experiment.json' http://127.0.0.1:8088/upload
curl.exe -F 'file=@D:/your/path/example.pcr' http://127.0.0.1:8088/upload
curl.exe -u 'admin:你的管理密码' http://127.0.0.1:8088/api/admin/reports
```

`samples/experiment.json` 是按实际 SDK 字段构造的演示数据，不是真实检测报告；附件仅作原样存储，不在当前平台校验 PCR 内部格式。

## 接口

| 方法与路径 | 用途 | 认证 |
| --- | --- | --- |
| POST `/upload` JSON | 老设备实验结果上报 | 兼容模式 |
| POST `/upload` multipart `file` | 老设备 PCR 文件独立上传 | 兼容模式 |
| POST `/api/device/reports` | 相同 JSON 的认证上报 | 设备 ID/Key 请求头 |
| GET `/actuator/health` | 服务存活 | 无 |
| GET `/api/admin/summary` | 设备、结果、附件计数及 SDK 可用状态 | 管理员 Basic |
| POST `/api/admin/devices` | 创建设备，返回一次性明文 Key | 管理员 Basic |
| GET `/api/admin/devices?offset=0&limit=20` | 设备列表，不暴露 Key 哈希 | 管理员 Basic |
| PATCH `/api/admin/devices/{id}` | `{"enabled":false}` 禁用设备 | 管理员 Basic |
| POST `/api/admin/devices/{id}/key` | 轮换设备 Key | 管理员 Basic |
| GET `/api/admin/reports?serial=SN&offset=0&limit=20` | 分页筛选；serial 可省略 | 管理员 Basic |
| GET `/api/admin/reports/{id}` | 完整 Experiment（含所有孔、Ct、曲线） | 管理员 Basic |
| GET `/api/admin/reports/{id}/source` | 原始 JSON 下载 | 管理员 Basic |
| GET `/api/admin/files?offset=0&limit=20` | 附件列表 | 管理员 Basic |
| POST `/api/admin/files` multipart `file` | 人员上传原始 PCR，独立于旧 `/upload` 开关 | 管理员 Basic |
| GET `/api/admin/files/{id}/download` | PCR 原文件下载，校验存储 SHA256 | 管理员 Basic |
| PUT `/api/admin/reports/{reportId}/file/{fileId}` | 显式关联原文件；不覆盖已有不同文件 | 管理员 Basic |
| GET `/api/admin/pcr/analyses?reportId=ID&inputFileId=ID&offset=0&limit=20` | 分析版本历史；两个筛选条件均可省略 | 管理员 Basic |
| GET `/api/admin/pcr/analyses/{analysisRunId}` | 参数、完整结果、操作者、状态、输入输出文件关联 | 管理员 Basic |
| GET `/api/admin/pcr/analyses/{analysisRunId}/result` | 已持久化的 C# 分析结果 JSON；不调用 SDK | 管理员 Basic |

设备创建 body：

```json
{"deviceId":"SN001","deviceType":"PCR","position":0,"displayName":"一号设备"}
```

列表与详情的数据库元信息使用 snake_case，Experiment 内保持 SDK 原有大小写；时间 `received_at/last_seen` 是 Unix 毫秒，`StartTime/EndTime` 按设备原文保存。设备密钥只保存 SHA256，管理密码以 BCrypt 在内存认证。

## 数据与一致性

- 结果包上报采用 Flyway V5：`result_bundles` 保存不可变实验快照及清单，`result_bundle_attachments` 保存附件名、类型、SHA-256 和大小。规则及结论原始字节复用 `runtime/files` 内容寻址存储；报告通过 bundle_id 关联结果包，读取会话和分析版本固定引用当时的 bundle_id/规则清单。备份仍需包含数据库和对应 files 目录。
- Flyway V4 增加文件 `uploaded_by_username`、`upload_source` 及分析版本 `saved_by_username`。人员上传记录 ADMIN_UPLOAD，分析输出记录 SDK_ANALYSIS，人员身份均取自登录认证；旧协议上传记录 LEGACY_HTTP，历史文件默认 LEGACY_OR_UNKNOWN 且账号为空。同名同内容重复上传仍复用原记录，保留首次登记信息。
- `result` 目录下的真实单重扩增文件已完成 SDK 离线读取及 HTTP 上报验证：8 孔、24 条通道结果、21 个有效 Ct、3 个 Infinity Ct 显式转为 null，扩增曲线、文件下载及重启恢复一致。详见 [单重扩增上报测试](test-reports/singleplex-pcr-upload-2026-11-23.md)。这是隔离测试库验证，前端和默认业务库不会自动增加该实验。
- Flyway V2 为 `reports` 增加操作者及上传来源，列表与详情均返回：`operator_name` 从设备 JSON 的可选 `Operator` 字段提取（最长 160 字符，缺失/null/纯空白存 NULL），仅是设备声明的姓名；`operator_user_id` 预留平台人员账号关联，目前保持 NULL，尚无用户表及外键，不能按姓名自动匹配账号。设备上报不能设置该用户 ID。
- `upload_source` 由服务端记录为 `LEGACY_HTTP`（旧接口）或 `DEVICE_AUTH`（设备密钥认证）；`uploaded_by_device_id` 仅在设备认证成功时填写，旧接口为 NULL。这些字段不能从上传 JSON 赋值；设备编号不作为人员 ID。重复结果保留首次入库的来源信息。
- V2 升级已有记录时不猜测操作者或认证身份：新增人员字段为 NULL，来源为 `LEGACY_OR_UNKNOWN`，历史 JSON 中的 `Operator` 仍可通过详情的 `Experiment` 查阅，暂不自动回填。二次分析操作者现已单独记录在分析版本中，不覆盖原始实验操作者；完整用户系统、审核及通用操作审计表尚未实现。
- 数据库：`runtime/database/dataserver.mv.db`；原始文件：`runtime/files/<hash前两位>/<sha256>.bin`。需要同时备份两者。
- 文件型 H2 显式设置 `WRITE_DELAY=0`，避免默认 500ms 延迟刷盘窗口导致立即终止进程后丢失已应答的数据。测试包含在关联成功后立即终止进程并恢复读取，不等同于验证所有硬件掉电场景。参考：https://h2database.com/html/commands.html#set_write_delay
- JSON 原文完整保存。指纹按递归字段排序并统一数值表示后计算，相同数据的格式化、字段顺序及整数/小数展示差异不会产生重复记录。数组顺序保留。数据发生改变则产生新结果记录，不覆盖原结果。
- 同设备行锁与唯一约束防止并发重复入库。成功响应在事务提交后才返回。附件先强制刷盘并原子移动，再记录数据库；中断最多留下未关联的文件，不会对未保存文件返回成功。尚未提供孤立文件清理任务。
- 文件名不参与本地路径，拒绝穿越路径；仅接收非空 `.pcr`，默认 JSON 与文件各最大 32 MiB。分页上限 100。
- 原设备上传的 Ct 为设备结果，状态 `DEVICE_RESULT_RECEIVED`；显式关联 PCR 后为 `WAITING_SDK`，不是“分析成功”。接收流程不重算 Ct；二次分析由独立管理接口触发，结果保存为新版本。

## 二次分析版本持久化

Flyway V3 新增 `pcr_read_sessions` 和 `analysis_runs`，不修改已有原始报告。前者记录读取句柄与可信输入文件、原报告、父分析版本的关系；后者记录每次分析的参数、完整结果 JSON、SDK 版本、登录用户名、输入/输出文件、起止时间及失败原因。当前登录用户为 admin，因此 `operator_username=admin`；不接受请求体自报操作者，也不将原始设备的 Operator 当作平台用户。

1. 上传原始 PCR，获得 FileID；需要归属某报告时，先显式关联该报告与原始文件。
2. `POST /api/admin/pcr/read`：`{"fileId":"输入文件ID","reportId":"原报告ID"}`。reportId 可省略：文件恰好关联一个报告时沿用该报告，未关联时允许仅文件分析，关联多个报告时返回 409，要求明确选择。返回原 C# `experimentId`，另加持久化的 `readSessionId` 和 SDK 版本。
3. `POST /api/admin/pcr/reanalyze`：

```json
{
  "experimentId":"读取返回的句柄",
  "requestId":"客户端为本次操作生成的唯一编号",
  "parameters":{"autoBaseline":false,"baselineStart":3,"baselineEnd":15,"threshold":500}
}
```

调用 SDK 前写入 RUNNING 记录，成功后将完整 C# 返回结果（包含 experiment、Ct、曲线及扩展字段）保存到 `result_json`，状态为 SUCCEEDED，随后才向前端返回成功。响应保留 C# `analysisId`，新增持久化版本 ID `analysisRunId`。此时即使尚未导出新 PCR，结果 JSON 也已入库。原报告 JSON、原报告操作者及原始文件关联均不覆盖。

`requestId` 可选，建议客户端提供。同一登录用户、相同 requestId 的成功重试直接读取数据库，不再调用 SDK；参数或读取句柄不一致、仍在执行或之前失败时返回 409。省略 requestId 则每次请求创建新版本。SDK 失败记录 FAILED 及原因，不伪造结果；超时后服务端失败不代表厂商一定停止执行，不自动重发。

4. `POST /api/admin/pcr/save`：`{"analysisId":"analysisRunId","fileName":"二次分析结果.pcr"}`。兼容使用原 C# analysisId。Java 下载输出、校验 SHA-256，在同一个数据库事务中登记 `uploaded_files` 并更新该版本的 `output_file_id`。文件状态从 NOT_SAVED → SAVING → SAVED，失败则为 FAILED，并保留成功的分析 JSON。相同版本及文件名重复保存返回已有 FileID，无需再次调用 C#；换文件名返回 409。
5. 按 reportId 或 inputFileId 查询分析历史；详情返回 `parameters`、`result`，列表只返回轻量元数据。已存结果及已保存文件不依赖 C# 在线，服务重启后仍可读取。

对上一次输出再次分析时，读取请求传入 `{"fileId":"上次输出FileID","parentAnalysisId":"父analysisRunId"}`；服务端校验输入确实为父版本输出，并继承原报告，形成版本链。不能随意将无关文件归入某个父版本。

读取和未导出文件的 SDK 句柄仍是 C# 进程内状态：桥接重启后要重新读取、建立新分析版本，持久化的历史 JSON 不受影响。进程异常中断时可能留下 RUNNING/SAVING 状态，当前需人工核对，尚无自动恢复或重放任务。磁盘文件与数据库不是统一事务，事务回滚可能留下未引用的磁盘内容，尚无自动清理。独立任务队列、审批、选孔分析和 Vue 在线历史界面不在本次实现中。

## 设备结果包：PCR、规则与结论一起上报

`E:/PCRinOne/framework/Bio_PCR_20230927/Bio_PCR` 的 HTTP 上传已改为调用 `ResultBundleUploader`，从选中 PCR 所在结果目录制作私有缓存快照，携带实验 JSON、PCR、全部 `.ct`、`.peak`、`.conclusion` 和 `judgeConclusion.result`。不读取当前模板作为历史实验的替代规则，不上传无关 HEX 或其他 JSON。结果保存时原有规则复制逻辑增加 `.conclusion`，二次保存清理结果目录时保留这三类规则附件。

设备配置的 `/upload` URL 保持不变，新协议为同一个 multipart 请求：

| 字段 | 内容 |
| --- | --- |
| experiment | 安全序列化后的完整旧 Android Experiment JSON；NaN/Infinity 转为 null，不转成 0 |
| manifest | JSON 字符串，version=1，files 列出每个文件的 fileName、role、size、sha256 |
| file | 唯一主 `.pcr` 二进制文件 |
| attachments | 重复 multipart 字段，逐个携带规则或原始结论文件 |

role 为 PCR、CT_RULE、PEAK_RULE、CONCLUSION_ARCHIVE、DEVICE_CONCLUSION，由服务端按文件名核对。原工程实际输出的结论文件是 `judgeConclusion.result`；`.conclusion` 文件如存在也归档，不假定其内部格式。文件名保留，服务端实际路径仍为哈希 `.bin`。PCR/实验 JSON 各最大 32 MiB，最多 64 个附件、每个最大 2 MiB、所有二进制合计最大 64 MiB；HTTP 整体上限 100 MiB 以容纳 JSON 与 multipart 开销。路径穿越、Windows 保留名/非法字符、重名、清单缺项和字节哈希不符均拒收，不留下部分业务记录。

成功响应保留 Result=1、ReportID、FileID，新增 BundleID、RuleFileCount、ConclusionFileCount。Android 同时检查 HTTP、业务结果、三个 ID 和附件数量确认，失败回调会通知界面，不再把所有 onResponse 当作成功。快照只写入应用缓存，发送结束后清理；用户结果目录不变。旧服务若不支持结果包，不会被误判为附件上传成功，应先更新 dataserver 再更新设备 APK。

完全相同的 JSON、PCR 和附件清单重传返回同一 BundleID/ReportID。PCR 或规则/结论内容变化时产生新结果包与报告，不覆盖旧记录；同内容 PCR 仍复用物理存储。旧的 JSON-only 与 file-only `/upload` 继续兼容。旧目录缺规则时记录缺失状态、设备提示未包含规则，不自动套用当前模板。

接口与管理：

| 接口 | 用途 |
| --- | --- |
| POST `/upload` multipart | 旧设备兼容入口，受原兼容开关和设备策略控制 |
| POST `/api/device/bundles` multipart | X-Device-Id / X-Device-Key 认证上报 |
| POST `/api/admin/bundles` multipart | 管理员导入相同协议，记录登录账号 |
| GET `/api/admin/reports/{reportId}/bundles` | 查询原报告的快照 |
| GET `/api/admin/bundles/{bundleId}` | 完整清单与归属 |
| GET `/api/admin/bundles/{bundleId}/attachments/{fileName}` | 下载单个原始附件，校验存储哈希 |
| GET `/api/admin/bundles/{bundleId}/download` | 原始结果 ZIP：manifest.json、experiment.json、原 PCR、规则和原结论 |
| GET `/api/admin/pcr/analyses/{analysisRunId}/bundle` | 保存后导出新 PCR、analysis.json 和固定规则/原结论附件，附来源清单 |

Vue 数据中心的报告/分析详情展示“规则与结论快照”，支持单文件和原结果包下载；已保存分析可下载分析结果包。对有规则来源的派生 PCR 再分析时必须传 parentAnalysisId，不能仅按文件 ID 丢弃其规则关联；历史界面的“基于此结果再分析”会正确传递父版本。

**二次分析中的含义：** Java 将归档附件的可信路径和哈希传给 C#；C# 校验后将原文件名恢复到 source.pcr/analysis.pcr/导出副本旁。C# 负责 SDK 的 Ct/曲线/熔解峰重算；Java 再执行下面的 Android 应用层判读。参数、SDK 结果、规则快照、新判读一并持久化到该分析版本。旧 `judgeConclusion.result` 和 `.conclusion` 只作为原始附件，不会冒充新结论，也不传入 SDK 的 SetProjectRule。

### 按 Android 源码执行 CT/PEAK 判读

依据 `Bio_PCR_20230927/Bio_PCR/app/src/main/java/com/gidi/bio_pcr/hongshi/SLANManager.java` 的 `judgeCTFile`（552 行）、`judgePEAKFile`（665 行）和 `secondSLANExAnalyse`（1070 行附近）。文件 JSON 结构依据 `WintersUtils/entity/judgeEntity/CTRuleEntity.java`、`PEAKRuleEntity.java`、`PEAKSingleRuleEntity.java`。Android 的调用顺序是 `setExperimentParamter` 重算 → `judgeCTFile/judgePEAKFile` → 更新结论。服务端遵循同样的先重算、后判读顺序。

| 文件 | Android JSON 结构 | 判读公式 |
| --- | --- | --- |
| `.ct` | 数组，每项 Channel、Category、CTvalue、CTvalueDown、CTvalueUP、CTvalueUP_Suspicious | L=CTvalue−CTvalueDown，U=CTvalue+CTvalueUP，S=U+CTvalueUP_Suspicious。L≤Ct≤U 阳性；U<Ct≤S 可疑样本；其余有限 Ct 阴性 |
| `.peak` | 数组，每项 Channel、PEAKSingleRuleEntityList；子项 Category、PEAKvalue、PEAKvalueDown、PEAKvalueUP、PEAKvalueRM | 任意一个 SDK 峰满足 PEAKvalue−PEAKvalueDown≤Tm≤PEAKvalue+PEAKvalueUP 且 Rm≥PEAKvalueRM，即该子项阳性；完整峰列表中无命中为阴性 |

上下界均包含。UP/Down/Suspicious 字段存的是偏移量，不能当作绝对边界；Rm 是 SDK 峰的 `Rm`，不能直接拿曲线 Y 值替代。按 Android 的通道名大小写不敏感包含匹配；`IsSelected` 是编辑界面状态，原判读方法没有据此过滤。现有真实模板 `temp/Template/01-3_flu_repeat_fam/single_judge.ct` 也已核对，包含上述完整字段；不会将别的模板规则自动补到缺规则的实验中。

服务端引擎为 [AndroidRuleEvaluator.java](src/main/java/com/pcr/dataserver/AndroidRuleEvaluator.java)，版本 `android-slan-rules-v1`。返回 `applicationRuleEvaluation`：来源文件名/哈希、引擎版本、逐孔逐通道逐项目的边界、实际 Ct、所有观测峰及命中峰、结论和缺失原因。每个文件和每条规则分别保留，不合并成未经定义的样本总判定，也不添加代码中没有的内参/QC/复合结论规则。

对 Android 原方法中发现的边界问题做了明确处理：

- 缺失通道、Ct 为 null/NaN、缺失峰数据或通道匹配不唯一时返回 `UNDETERMINED/未判定`；不默认阴性，也不沿用上一孔数据。SDK 明确返回 Infinity Ct 时，按 Android 的区间比较结果记阴性，并注明无有限 Ct。峰列表 `[]` 表示无检出峰；null 表示未提供峰数据。
- Android 同通道 CT 规则使用 Map.put 会覆盖前项；服务端保留所有规则，逐条记录来源和索引。
- Android 的峰标记可命中前一个峰、实际 TM/RM 却来自最后一个峰；服务端分别保存 observedPeaks、matchedPeaks，避免结论证据错位。
- 缺字段、非有限数、负偏移、格式无效的规则明确记错误，不按 Gson 默认 0 执行；其他合法文件可继续判读。原始 SDK 分析成功与规则判读状态分别展示。

`ruleContext`/`ruleSnapshot` 仅描述输入归档，仍用 ARCHIVED_NOT_EVALUATED；本版本实际判读状态看 `applicationRuleEvaluation.status`（EVALUATED、PARTIAL、INVALID_RULES、INVALID_DATA、MISSING_RULES）。顶层 `applicationRulesApplied` 表示 Java 是否完成至少一项有效判读，C# 返回 false 仅表示其自身未执行应用层规则。原始报告不改写，历史版本不追溯重判。Vue 展示“本版本规则判读”，分析结果 ZIP 新增 `application-conclusions.json`，与原结论附件分开。

### 高级参数、可编辑规则版本与综合结论

读取文件现在返回 `parameterSets`：通过 SDK 在私有副本上 Open → GetExperimentParamters → Close 取得完整参数。Vue 的“按项目和通道配置高级参数”显示每个项目/靶标的原值，只发送勾选靶标中修改的字段；项目算法可单独修改，未选择项目/靶标的参数保留。复算结果同时保存请求的 `requestedParameterSets` 与 SDK 回读的有效 `parameterSets`，自动阈值由 SDK 计算。

| 参数 | 范围 / 选项 | 层级 |
| --- | --- | --- |
| 扩增算法 ampAlgorithm | 0 相对荧光值；1 绝对荧光值；2 相对荧光值（线性） | 项目 |
| baselineStart / baselineEnd | 修改时满足 1 ≤ 起点 < 终点 ≤ 20 | 靶标 |
| optimizationMode | 0 不优化、1 自动优化、2 手动优化 | 靶标 |
| autoThreshold / threshold | 自动阈值开关；手动阈值为正有限数，开启自动时页面不覆盖阈值数值 | 靶标 |
| qualitativeAnalysis | true 定性、false 定量 | 靶标 |
| digitalFilter | 0 不滤波、1 轻度、2 重度 | 靶标 |
| peakHeightThreshold | 非负有限数，对应 SDK 原字段 PeekHightThreshold | 熔解靶标 |
| minTemperature / maxTemperature | 有限数，最低温度 < 最高温度；SDK 负责实际文件适用性校验 | 熔解靶标 |
| meltPeakAlgorithm | 0 标准算法、1 高级算法 | 熔解靶标 |
| peakType | 0 正峰、1 负峰、2 正负峰 | 熔解靶标 |

枚举值通过本机所供 SLAN.API.dll 的 EAMPAlgorithm、EOptionzationMode、EDigitalFilter、EPeekType、EMeltPeakAlgorithm 反射核实，中文含义与同目录 SLAN.API.xml 一致。仅允许编辑 SDK 实际返回的字段，扩增靶标不允许附加熔解字段。旧全局四参数请求保持兼容。高级请求仍包含旧四项基础参数，额外指定 `scope:"SELECTED"`、`projects:[{projectIndex,ampAlgorithm}]`、`targets:[{projectIndex,targetIndex,settings:{...}}]`；索引从 0 开始，以读取返回的 parameterSets 定位，支持同通道名出现在不同项目。`scope:"ALL"` 则应用全局基础参数，可用可选 defaults 和逐靶标 settings 进一步覆盖。

“编辑判读规则与 CT / PEAK 综合结论”支持 CT 中心/上下偏移/可疑扩展，PEAK Tm 中心/上下偏移/Rm 下限，以及通道、项目名称的编辑，支持增删文件、规则和峰项目。保存新版本后才允许用于分析，不改写原始上传文件；每版记录名称、创建账号、父版本、文件内容/哈希及综合策略。切换已有版本可复用，已保存的分析不会被后来的规则修改影响。

新增管理接口（均需管理员认证）：

| 接口 | 用途 |
| --- | --- |
| GET `/api/admin/pcr/rules?bundleId=...` | 列出该原始结果包的规则版本；省略 bundleId 列出独立规则版本 |
| GET `/api/admin/pcr/rules/draft?bundleId=...` | 从原始 CT/PEAK 构造编辑草稿，不创建版本 |
| GET `/api/admin/pcr/rules/draft?versionId=...` | 读取版本编辑草稿 |
| GET `/api/admin/pcr/rules/{id}` | 完整版本及来源/创建账号 |
| POST `/api/admin/pcr/rules` | `{baseBundleId,parentVersionId,label,files:[{fileName,rules}],combinationRules}` 创建不可变版本 |

复算请求增加 `ruleVersionId`。省略时继承读取会话（派生 PCR 继承父分析）；`"ORIGINAL"` 明确使用原始结果包规则。独立文件可创建 baseBundleId=null 的独立规则；绑定结果包的规则不能用于另一结果包。rule_version_id 写入 pcr_read_sessions 和 analysis_runs，完整规则内容及组合策略写入 rule_snapshot_json。V6 自动创建 rule_versions 表及关联字段。规则文件每个最多 2 MiB、总计 4 MiB，1–64 个，规则/引用/哈希均校验。

综合策略由用户显式配置，按同一孔将一条 CT 与一条 PEAK 规则组合，引用格式 `{fileName,ruleIndex}`（文件内索引从 0 起，PEAK 按通道/子项顺序展开）。不根据同名项目自动推断组合。

| 两项状态 | ALL：全部命中 | ANY：任一命中 |
| --- | --- | --- |
| 阳性 + 阳性 | 阳性 | 阳性 |
| 阳性 + 阴性 | 阴性 | 阳性 |
| 可疑 + 阳性 | 可疑 | 阳性 |
| 可疑 + 阴性 | 阴性 | 可疑 |
| 阴性 + 阴性 | 阴性 | 阴性 |
| 任一项未判定/缺失 | 未判定 | 未判定 |

综合结果存于 `combinedConclusions`，保留 CT/PEAK 各自状态和组合方式。新结果包包含新 PCR、有效 `.ct/.peak`、rule-version.json、application-conclusions.json、combined-conclusions.json 和 analysis.json；选用编辑版本时，原始规则/结论放入 original-attachments/，避免与有效规则混淆。C# 在私有工作目录中使用所选版本的附件快照，保存新 PCR 时同样保留。应用判读规则/综合策略存于附属文件及数据库，不伪装成 SDK 原生规则。

验证命令：`dotnet run --project csharp/PcrSdkBridge.Tests`（SDK 参数编辑校验），`mvn verify`（规则版本/组合/幂等/继承/导出），前端 `npm.cmd run test:e2e`。`node scripts/simulate-real-sdk-reanalysis.mjs --advanced` 已使用真实单重与熔解文件验证按靶标高级参数、保存后重开、CT/PEAK 规则版本、综合策略及重启恢复，见 [真实 SDK 高级参数测试](test-reports/real-sdk-advanced-2026-11-23.json)。实际熔解文件存在无有效 Ct 的项目，峰判读正常、综合项按缺测返回未判定；完整状态组合由固定输入的规则测试覆盖。规则均明确标注为软件测试规则，不视为原实验真实规则。读取中补齐并还原熔解参数的已知派生类型，已覆盖未选中熔解靶标的类型保持回归。

验证：Android Debug APK 已构建，4 项快照/序列化/HTTP 确认单测通过；后端结果包测试覆盖重传、规则变更新快照、原附件字节、父子版本、导出、哈希/路径/缺项拒收、认证及事务回滚；前端 10 项测试通过。真实联测用 `node scripts/simulate-real-sdk-reanalysis.mjs --bundle`，会编译并执行该 Android 工程的同一套上传代码，串通 Java/C#/真实 SDK 的读取、复算、保存、原字节校验及重启恢复。证据见 [实际结果包联测](test-reports/real-sdk-bundle-2026-11-23.json)。现有参考结果目录缺少真实 CT/PEAK/conclusion 规则，测试在私有目录补充明确的测试附件，原 judgeConclusion.result/PCR 不变；不将测试规则视为原实验真实规则。

规则引擎另覆盖区间边界、可疑区间、选择标志、无 Ct、缺通道、多通道歧义、峰高/温度双条件、多峰证据、格式/哈希错误及同通道多规则；实际多重文件的 SDK 输出确认峰字段为 Tm/Rm/Wm/Ym，本判读使用 Rm。真实单重文件阈值改为 0.24 后，A1 FAM Ct=39.053621906411，明确的测试规则将其判为可疑；该文件没有熔解峰数据，峰规则为未判定，结果与来源规则随版本重启恢复。已有分析版本不会自动补判。`node scripts/verify-result-bundle-ui.mjs` 用这组测试规则验证在线 Vue → Java → C# → 实际 SDK 的判读显示、保存、历史及移动端布局，见 [在线规则界面验收](test-reports/result-bundle-ui-2026-11-23.json)；它会在离线模拟库中建立标为 SIMULATION 的报告。

## MySQL 部署

MySQL 配置已提供，但本机未安装 MySQL 服务，也未执行 MySQL 实例集成测试。推荐使用新建的 MySQL 8.4/RDS 数据库：

```powershell
$env:SPRING_PROFILES_ACTIVE = 'mysql'
$env:DATASERVER_DB_URL = 'jdbc:mysql://127.0.0.1:3306/dataserver?useUnicode=true&characterEncoding=UTF-8'
$env:DATASERVER_DB_USER = 'dataserver'
$env:DATASERVER_DB_PASSWORD = '你的数据库密码'
```

由 Flyway 建表。切换数据源不会自动迁移 H2 已有记录。本服务接收层不依赖 Redis，后续增加验证码、会话或任务调度时再接入。

## 验证

进程托管新增测试覆盖路径带空格、隐藏启动、正确工作目录、就绪探测、重复启动、端口冲突及所有权清理。Windows 实际进程测试使用专用替身 EXE，未启动或停止厂商发行目录中的真实程序：

```powershell
dotnet build .\scripts\fixtures\VendorProcessStub -o .\target\managed-fixture
dotnet build .\csharp\PcrSdkBridge -o .\target\csharp-bridge
$env:RUN_MANAGED_ACCEPTANCE = 'true'
# 然后执行 mvn verify；该开关额外启用真实 Windows 替身进程测试。
```

HTTP 测试覆盖上报、原始结果保留、分析 JSON 与参数入库、操作者来源、版本链、失败记录、保存校验与事务回滚、并发 requestId 去重及权限校验。MySQL 实例与真实厂商 HTTP/设备联调尚未执行。

2026-11-23：`mvn verify` 共 28 项，27 项通过、1 项 Windows 替身进程测试因未启用开关而跳过（含实际文件发现的小数阈值校验回归）。`node scripts/slan-offline-contract.mjs` 与 `node scripts/bridge-smoke.mjs` 通过。前者使用真实 Java/C# 进程和厂商协议替身，验证分析结果在导出文件前已入库、参数/账号/SDK 版本/文件关联、失败留痕、幂等重试，以及停止 C# 后重启 Java 仍可查询和下载；证据见 [分析历史测试 JSON](test-reports/analysis-history-2026-11-23.json)。该协议替身测试本身不验证真实厂商算法；实际 SDK 的复算、保存和重新打开校验见下文离线模拟。

```powershell
# 构建、真实 HTTP 集成测试、打包
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 -Build
# 单独运行打包程序的落盘及重启恢复测试，无需启动生产服务
node .\scripts\smoke.mjs
```

`run.ps1 -Build` 测试通过后会启动服务。若只构建，直接用 Maven `clean verify`。Java 集成测试覆盖协议字段、原始 JSON/曲线保留、格式与数值归一去重、错误请求、大小限制、设备凭据和禁用、文件下载与显式关联、管理员鉴权、并发重传。`scripts/smoke.mjs` 另启动打包 JAR，验证 H2 文件数据库和附件在进程重启后仍可读取，结束时关闭测试进程。

## 当前边界

后端和 Vue 数据中心已接通实际文件及分析历史；模拟工作台与离线快照仍独立。小程序、档案绑定、多用户角色权限、短信微信通知、OTA、审核发布及后台分析队列仍未实现。C# 离线 HTTP 适配及真实 SDK 的离线复算/保存已验证；实际部署的厂商 EXE HTTP 端点需单独确认。分析结果尚未经审核发布。

2026-11-23 数据中心验证：Java 共 29 项测试，28 项通过、1 项未启用的 Windows 替身测试跳过；前端构建及 9 项浏览器测试通过。另以实际浏览器访问运行中的 Java/C#/真实 SDK，验证真实文件上传、Ct 变化、结果保存下载、历史回看、刷新重新登录及移动端布局，见 [实际页面验证 JSON](test-reports/data-center-live-2026-11-23.json)。账号密码仅存在页面内存，未写入浏览器存储；页面刷新后需要重新登录。

## 真实 SDK 二次分析离线模拟

`scripts/simulate-real-sdk-reanalysis.mjs` 使用真实单重文件、真实厂商 DLL、真实 Java 与 C# 桥接，HTTP 测试入口通过标准输入转发给隔离 .NET Framework 进程里的 `Service.SLANService`，只开放离线方法。不连接仪器，不启动或终止原有厂商进程，不修改输入文件。此模式调用真实算法与保存方法，但不等同于运行厂商 EXE 自带的 HTTP 服务器。

先提取并编译离线工具，再执行测试：

```powershell
$sample = Get-ChildItem -LiteralPath result -Filter *.pcr -Recurse -File | Where-Object { $_.FullName -like '*单重*' }
$vendor = Join-Path (Get-Location) 'SLAN_9.0.1.260921_API_接口文档与完整程序_20261116\SLAN_9.0.1.260921_API_接口文档与完整程序\SLAN 9.0.1.260921_API'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/read-pcr-offline.ps1 -Source $sample.FullName -OutputDirectory target/real-sdk-reanalysis-20261123 -VendorDirectory $vendor
node scripts/simulate-real-sdk-reanalysis.mjs
```

需要先构建 Java JAR 和 C# 桥接。测试将原阈值 0.12 改为 0.24，基线保持 6–12，由 SDK 实际复算；核对 21 个有限 Ct 的变化、原始曲线不变、保存文件再次由 SDK 打开后 Ct/扩增曲线及参数一致，以及停止 SDK/C# 后重启 Java 的入库记录和下载完整性。新 PCR 副本保存在 `runtime/simulations/`，不受 Maven clean 影响；测试数据库和详细输出位于 `target/real-sdk-reanalysis-20261123/`，使用隔离端口与库。

独立 SDK 宿主没有厂商 UI 的模型状态刷新流程：对已打开副本调用 SDK 公开的 `RefreshStatus()`，由其 `HasEnoughData()` 自行判定状态，再遵守 `CanSave()`。没有直接赋值 Completed 或跳过 SDK 保存检查。真实样本暴露的阈值限制也已修复：Java/C# 接受大于 0 的有限阈值，包括 0.12、0.24，不再限制为演示用的 10–3000。

真实测试证据见 [SDK 复算与保存 JSON](test-reports/real-sdk-reanalysis-2026-11-23.json)，含全精度逐孔 Ct 对比、输入输出哈希、分析版本及文件 ID。此处仅验证软件行为，不给出检测判定。
