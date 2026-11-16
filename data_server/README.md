# dataserver

当前迭代已补齐 Vue 3 模拟工作台，暂不接入设备。进入 `frontend` 后执行 `npm.cmd run dev`，打开 http://127.0.0.1:5173 即可独立体验，无需启动下面的后端服务。详见 [前端说明](frontend/README.md)。

真实读取 `.pcr`、二次分析和保存接口采用 Java → C# 桥接 → 厂商独立 HTTP 服务，已按新提供的宏石 SLAN 9.0.1.260921 资料完成离线协议适配。默认关闭真实调用，操作返回 501；页面继续使用模拟数据。详见 [C# 接口说明](csharp/README.md)。本次未启动厂商服务或连接设备，也未验证真实文件解析，原参考 `.pcr` 保持不变。

Java 21 + Spring Boot HTTP 数据接收服务第一版。已实现设备实验结果上报、原文及曲线持久化、重复上报去重、PCR 文件存储、管理查询和下载。默认使用文件型 H2，开箱启动不依赖 MySQL、Redis 或厂商 SDK。

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

已在本机准备好 Maven、现有 JDK 21，并生成 `target/dataserver-0.1.0.jar`。

```powershell
cd E:\PCRinOne\framework\bowei_liushuixian\liushuixian_melody\data_server
$env:DATASERVER_ADMIN_PASSWORD = '设置至少12位的管理密码'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\run.ps1
```

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
| GET `/api/admin/files/{id}/download` | PCR 原文件下载，校验存储 SHA256 | 管理员 Basic |
| PUT `/api/admin/reports/{reportId}/file/{fileId}` | 显式关联原文件；不覆盖已有不同文件 | 管理员 Basic |

设备创建 body：

```json
{"deviceId":"SN001","deviceType":"PCR","position":0,"displayName":"一号设备"}
```

列表与详情的数据库元信息使用 snake_case，Experiment 内保持 SDK 原有大小写；时间 `received_at/last_seen` 是 Unix 毫秒，`StartTime/EndTime` 按设备原文保存。设备密钥只保存 SHA256，管理密码以 BCrypt 在内存认证。

## 数据与一致性

- 数据库：`runtime/database/dataserver.mv.db`；原始文件：`runtime/files/<hash前两位>/<sha256>.bin`。需要同时备份两者。
- 文件型 H2 显式设置 `WRITE_DELAY=0`，避免默认 500ms 延迟刷盘窗口导致立即终止进程后丢失已应答的数据。测试包含在关联成功后立即终止进程并恢复读取，不等同于验证所有硬件掉电场景。参考：https://h2database.com/html/commands.html#set_write_delay
- JSON 原文完整保存。指纹按递归字段排序并统一数值表示后计算，相同数据的格式化、字段顺序及整数/小数展示差异不会产生重复记录。数组顺序保留。数据发生改变则产生新结果记录，不覆盖原结果。
- 同设备行锁与唯一约束防止并发重复入库。成功响应在事务提交后才返回。附件先强制刷盘并原子移动，再记录数据库；中断最多留下未关联的文件，不会对未保存文件返回成功。尚未提供孤立文件清理任务。
- 文件名不参与本地路径，拒绝穿越路径；仅接收非空 `.pcr`，默认 JSON 与文件各最大 32 MiB。分页上限 100。
- 原设备上传的 Ct 为设备结果，状态 `DEVICE_RESULT_RECEIVED`；显式关联 PCR 后为 `WAITING_SDK`，不是“分析成功”。没有篡改 Ct 或运行 SDK 算法。

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

本次验证结果：`mvn verify` 成功，9 项 HTTP 集成测试全部通过；打包程序的 JSON/multipart 上报、立即终止后重启恢复、附件下载、重传去重及关闭旧入口测试全部通过。测试进程已关闭。MySQL 与真实设备联调尚未执行。

```powershell
# 构建、真实 HTTP 集成测试、打包
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 -Build
# 单独运行打包程序的落盘及重启恢复测试，无需启动生产服务
node .\scripts\smoke.mjs
```

`run.ps1 -Build` 测试通过后会启动服务。若只构建，直接用 Maven `clean verify`。Java 集成测试覆盖协议字段、原始 JSON/曲线保留、格式与数值归一去重、错误请求、大小限制、设备凭据和禁用、文件下载与显式关联、管理员鉴权、并发重传。`scripts/smoke.mjs` 另启动打包 JAR，验证 H2 文件数据库和附件在进程重启后仍可读取，结束时关闭测试进程。

## 当前边界

后端提供数据接收与查询；Vue 3 本轮以独立模拟模式运行，尚未与数据接收后台联动。小程序、档案绑定、代理商权限、短信微信通知、OTA、审核发布及后台分析队列仍未实现。按本轮确认，真实分析将接入 C# SDK，当前只预留接口。模拟 Ct 与曲线不代表真实检测或经过审核的医学报告。
