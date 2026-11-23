# 宏石 SLAN 离线分析桥接

已根据本次提供的 SLAN 9.0.1.260921 API 文档实现离线 HTTP 适配。Vue 页面继续模拟。正常启动 Java 后端时，Java 自动启动或复用 `SLAN.WCFServer.exe`，等待版本接口可用后再启动本适配服务，并设置 `PCR_VENDOR_ENABLED=true`。无需分别手动启动这两个程序。

## 已确认的接入方式

- 厂商 DLL `SLAN.API.dll` 的程序集版本是 1.0.0.0，TargetFramework 静态检查为 `.NETFramework,Version=v4.0`。不能把安装目录版本 9.0.1.260921 当成程序集版本，也不能据此直接加载到 .NET 10。
- 厂商 `SLAN.WCFServer.exe` 提供 HTTP JSON 接口，默认基址 `http://127.0.0.1:8186/SlanServerService`。
- 当前结构：Vue → Java → 本项目 C# HTTP 桥接（.NET 10）→ 厂商独立服务（.NET Framework）。桥接无需直接引用旧 DLL。
- 厂商资料从 `data_server/SLAN_9.0.1.260921_API_接口文档与完整程序_20261116` 中读取，原发行目录及参考 PCR 未改动。本桥接不复制厂商二进制到源码或输出包。

## 操作及对应厂商接口

| Java 管理接口 | 桥接接口 | 厂商离线操作 |
| --- | --- | --- |
| GET `/api/admin/pcr/capabilities` | GET `/v1/capabilities` | 启用时仅调用 GetVersion；禁用时无厂商调用 |
| POST `/api/admin/pcr/read` | POST `/v1/pcr/read` | 私有副本 → LoadExperimentResult |
| POST `/api/admin/pcr/reanalyze` | POST `/v1/pcr/reanalyze` | 检查实验名冲突 → OpenExperiment → GetExperimentParamters → SetExperimentParamter → GetExperimentData → SaveExperiment → CloseExperiment |
| POST `/api/admin/pcr/save` | POST `/v1/pcr/save` | 将已由 SDK 保存的分析副本导出为新文件 |
| 内部下载 | GET `/v1/pcr/files/{outputId}` | 返回已生成的文件，Java 核对 SHA256 后入库，返回 FileID |

Java 管理入口沿用管理员 Basic 认证。最终文件由已有 `/api/admin/files/{FileID}/download` 下载。没有设备枚举、运行、停止、释放、开关门或项目注册接口，也不自动调用厂家 Login；有权限要求时通过环境变量提供已有 Token。

Java 现已通过 Flyway V3 持久化分析历史。读取时登记输入文件/原报告/SDK 版本；二次分析成功后先存参数、完整结果、登录用户名和状态，再返回新增的 analysisRunId。保存时把新 FileID 关联到该版本，原始报告不覆盖。新增 GET `/api/admin/pcr/analyses`、`/analyses/{id}`、`/analyses/{id}/result` 查询数据库，无需 C# 在线。具体字段、requestId 重试和父版本关联见 [主说明](../README.md#二次分析版本持久化)。

## 单独启动适配服务的模拟模式

```powershell
cd E:\PCRinOne\framework\bowei_liushuixian\liushuixian_melody\data_server
# 无需运行厂商服务；PCR_VENDOR_ENABLED 缺省为 false
 dotnet run --project .\csharp\PcrSdkBridge
```

C# 单独运行默认监听 `http://127.0.0.1:5099`，默认禁用厂商调用，操作返回 501；由 Java 托管启动时则会自动启用调用。前端模拟按钮不调用它，因此独立页面不要求启动任何后端。

## 后续在独立离线环境启用

以下是绕过 Java 进程托管、单独调试适配层的配置。常规使用 `run.ps1` 即可。仍需确认厂商运行目录、依赖、权限、授权及独立服务实例已准备好。

```powershell
$env:PCR_VENDOR_ENABLED = 'true'
$env:PCR_VENDOR_URL = 'http://127.0.0.1:8186/SlanServerService'
$env:PCR_INPUT_ROOT = 'E:\PCRinOne\framework\bowei_liushuixian\liushuixian_melody\data_server\runtime\files'
$env:PCR_WORK_ROOT = 'E:\PCRinOne\framework\bowei_liushuixian\liushuixian_melody\data_server\runtime\slan-offline'
# 仅在厂商服务需要鉴权时设置 PCR_VENDOR_TOKEN；不在代码中写凭据。
dotnet run --project .\csharp\PcrSdkBridge
```

`PCR_BRIDGE_URL` 设置 C# 监听地址，Java 的 `DATASERVER_CSHARP_URL` 设置桥接访问地址。厂商 URL 当前限制为回环地址，两个服务与 Java 需要能访问相同文件系统；桥接本身无独立身份认证，只应置于受控本机进程环境，不能暴露公网。

能力查询成功只证明 GetVersion 可达，不证明参考文件兼容性、Ct 算法正确性或授权完整。厂商服务可能因缺失配置、项目或规则返回错误，本桥接不会擅自注册项目或复制现场配置来掩盖错误。

## 请求结构与数据保护

读取请求新增可选 attachments：每项为 `{fileName,sourcePath,sha256,role}`。Java 仅从已归档结果包生成这些参数，C# 按可信输入根目录、文件大小、文件名及哈希校验，再将原文件名复制到私有 PCR 工作目录。读取、复算、导出均保留同一份附件，返回附件摘要及 `applicationRulesApplied=false`，表示 C# 自身只负责 SDK 计算。随后 Java 使用 AndroidRuleEvaluator 执行 Android `.ct/.peak` 区间判读，将新结论及引擎版本写入 analysis_runs.result_json；最终 Java 响应包含 applicationRuleEvaluation 和实际 applicationRulesApplied。不将这些应用规则传入厂商 SetProjectRule。原始 `.conclusion`/judgeConclusion.result、SDK 计算结果、新应用判读分别保留。

读取：`{"fileId":"已上传文件的ID"}`。Java 先查询能力记录 SDK 版本，解析可信存储路径，再传递 `{fileId,fileName,sourcePath}`。Java 请求还可选择 reportId 或 parentAnalysisId，用于持久化归属，不会直接传给厂商。C# 只允许 PCR_INPUT_ROOT 下的现有普通文件，拒绝软链接/重解析点；先复制为工作区 source.pcr，再交给厂商读取，Java 核对源哈希。输入大小限制 32 MiB，不需要依赖存储文件的 .bin 后缀判断内容。

二次分析：

```json
{"experimentId":"读取返回的句柄","parameters":{"autoBaseline":true,"baselineStart":3,"baselineEnd":15,"threshold":300}}
```

旧的四参数请求仍统一应用到所有项目和靶标。新增 `scope:"SELECTED"`、projects、targets 后，可按项目/靶标索引精确修改扩增算法、自动阈值、定性开关、优化模式、滤波、基线，以及熔解峰高/温度范围/算法/峰型。完整字段与枚举见 [高级参数说明](../README.md#高级参数可编辑规则版本与综合结论)。先获取原始完整 AnalyseParamter，保留 `__type`、BasicRule、交叉干扰和未修改的字段；旧四项映射为：

- BeginBaseline / EndBaseline：基线起止。
- OptimizationMode：autoBaseline=true 映射 AUTO=1，false 映射 MANUAL=2；SDK 没有名为 AutoBaseline 的布尔字段。
- AutoThreshold=false、Threshold：使用传入手动阈值。

GetExperimentParamters 的部分 JSON 传输没有返回派生类型标识；完整项目回传前，对所有熔解靶标补齐 `__type:"TargetParamterMelt:#SLAN.API.Model"`，包括没有被选择修改的熔解靶标。隔离 SDK 宿主按这个已知类型还原，避免 JSON 将 TargetParamterMelt 降为 TargetParamter 后发生 SDK 强制转换异常；不加载任意外部类型。读取响应返回原参数集，复算响应返回请求参数集和 SDK 实际回读参数集。

基线范围仍为 1–20；阈值接受正的有限数，包括真实扩增文件使用的 0.12、0.24，不再强制套用演示 UI 的 10–3000 范围。HTTP 外层 `方法名Result` 会剥离，必须先检查 ErrorCode=0；保存、关闭和参数设置还检查 Result=true。

保存：Java 接口使用 `{"analysisId":"analysisRunId或复算返回的旧句柄","fileName":"result.pcr"}`，服务端查出对应 C# 句柄后转发。厂商 SaveExperiment 只接受实验名，不能传输出路径。因此分析时在新的工作副本上 Open/Save；对外保存仅导出该副本。Java 拉取文件，校验 SHA256，在事务中生成 FileID 并关联分析版本。相同版本/文件名重试直接返回已存文件。不会将 JSON 改后缀充当 PCR，也不会显式向厂商发送原文件路径。

同一桥接内串行调用。打开前检查同名实验，发现冲突返回 409，不覆盖或关闭其他客户端的实验。只关闭本桥接确认打开的实验，不调用 ReleaseExperiment。失败或超时不重发原操作；超时可能意味着厂商仍在处理，需维护者确认服务状态。

桥接句柄暂存内存，重启后失效，需要重新读取。当前最多 128 个读取/分析句柄；失败工作副本和过期目录尚无自动清理，需在停止桥接并确认无使用后维护。原始输入、分析工作区和下载存储应分开管理。

厂商结果字段为 `SerialNumber`、`UseModule`、`Porjects`，和旧 Android SDK 的 `SerialNo`、`Block` 不同。桥接保留原始厂商 JSON，未将它直接送入旧设备 ExperimentParser；未来真实页面适配需显式处理这些差异。

## 验证与未验证范围

```powershell
dotnet build .\csharp\PcrSdkBridge -o .\target\csharp-bridge
# 先完成 Java mvn verify：
node .\scripts\slan-offline-contract.mjs
# 默认禁用路径验证：
node .\scripts\bridge-smoke.mjs
```

协议替身测试使用真实 Java/C# 进程调用模拟厂商 HTTP 服务器，覆盖读取、全参数保留、复算、保存、分析历史入库、失败记录、原始报告不变、幂等重试，以及停止 C# 后重启 Java 仍可读取历史结果和下载已保存文件。测试文件为明确标记的假字节，只用于协议与存储验证。

上述协议测试未启动厂商发行目录的 SLAN.WCFServer.exe（Windows 进程托管测试使用替身 EXE），未连接设备。另已通过独立 .NET Framework 读取工具调用真实厂商 DLL，验证扩增/熔解 PCR 的离线读取及 Java 上报，见 [真实单重扩增测试](../test-reports/singleplex-pcr-upload-2026-11-23.md)。

`scripts/simulate-real-sdk-reanalysis.mjs` 进一步以真实厂商 `Service.SLANService` 方法执行单重文件的调参、复算、保存和重新打开校验，经现有 C# 桥接与 Java 自动入库，并核对重启恢复。测试 HTTP 入口仅作实际 SDK 的传输封装，算法和文件保存由厂商 DLL 执行；SDK 宿主调用公开 RefreshStatus 按真实数据完整性更新状态，不直接指定 Completed。Ct 从原阈值 0.12 改为 0.24 后实际发生变化，新文件重新读取一致。详见 [实际 SDK 证据](../test-reports/real-sdk-reanalysis-2026-11-23.json)。厂商 EXE 自带的 8186 HTTP 端点未在该模拟中启动；本机原有厂商进程未改动。
