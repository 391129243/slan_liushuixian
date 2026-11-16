# SLAN 9.0.1.260921 API 接口使用文档

整理日期：2026-11-16。适用对象：使用 C#/.NET 或 HTTP 集成宏石 SLAN 设备的软件开发人员。

## 1 版本与交付范围

本文所称“最新”是本机已找到的厂商发行包中的最新版本 **SLAN 9.0.1.260921_API**，不是对厂商所有线上发行版本的声明。依据为该发行包 Startup.exe 的产品版本、SLAN.API.dll 的实际公开类型和方法、同包 SLAN.API.xml；并参考厂商旧 Word 说明和现有 RNASeq 接入代码。

交付的 SLAN 文件夹是该厂商发行包的完整原样副本，共 101 个文件、52,880,593 字节。未把旧 260302 包、PCR 根目录的 260909 DLL 或 RNASeq 运行过程中产生的项目、日志、实验记录混入发行目录。发行目录原有空目录也保留。

SLAN.API.dll 的程序集版本和文件版本均为 1.0.0.0，因此不可用这个数字区分 260302、260909、260921。请同时核对 Startup.exe 版本及 SHA256。

| 文件 | SHA256 |
|---|---|
| SLAN.API.dll | 4EEAA9301A0137A546EAD7BEA1AB34B0A027937C72758B40ABC4A28700102C16 |
| SLAN.API.xml | 8AC8DD836186C5DBC1A6C30B0C20FB2D0F01FF7ED6B19007722ADDFA600FE7F0 |

本次重新反射比较 260909 与 260921：公开类型和成员签名未删除，新增设备枚举 `DeviceType.SLAN_16H = 26`。这只证明公开 API 表面的差异，不等同于所有固件、驱动和运行行为的兼容性验证。

压缩包中的主要内容：

- 本文的 Markdown 与纯文本两个版本，内容对应一致。
- `SLAN 9.0.1.260921_API/`：完整厂商发行目录。
- `原始参考资料/`：厂商 Word 说明、旧 Postman 集合，保留原件供溯源；其中过时示例以本文和当前 DLL 为准。
- `示例/`：C# 调用代码及 HTTP 请求示例。
- `SHA256SUMS.txt`：除清单自身之外的包内文件校验值。

## 2 接入方式与部署

### 2.1 两条调用路径

| 方式 | 入口 | 适用场景 |
|---|---|---|
| C#/.NET | SLAN.API.Client.SlanService、SlanDevice | Windows 上位机，使用厂商已封装的 WCF 客户端 |
| HTTP JSON | http://localhost:8186/SlanServerService/接口名 | Python、Java、其他进程或跨语言调用 |

后台服务由 `SLAN.WCFServer.exe` 提供；前台软件入口为 `Startup.exe`。厂商资料说明二者是独立程序，数据不共享。先确定要连接哪一个服务实例，避免误以为前台加载的项目已在后台服务中可用。

后台服务名是 `SlanServerService`；连接前台软件时服务名为 `SlanServerServiceSelf`。C# 使用 `SlanService.Initialize("SlanServerServiceSelf")` 切换。HTTP 相应替换 URL 中的服务名，并以该实例实际暴露的端点为准。

### 2.2 运行环境

1. 将整个 SLAN 发行目录解压到稳定、可写的位置，保留 DLL、Plugin、runtimes、配置和资源的相对布局。不要仅复制 SLAN.API.dll 就期待能驱动设备。
2. 设备驱动、连接方式与设备配置按现场安装环境准备。原始 API 发行包不包含当前 RNASeq 目录额外保存的 config.cfg、SQLite.Interop.dll、历史实验等；本次不把这些现场文件擅自移植到发行包。新机器若缺少所需依赖或配置，应使用相应完整安装环境补齐。
3. 启动服务时，把工作目录设为 SLAN.WCFServer.exe 所在目录。不要在不同版本目录各启动一份服务，再让客户端随机连接。
4. DLL 的 TargetFramework 为 `.NETFramework,Version=v4.0`，引用 System.ServiceModel、System.ServiceModel.Web、System.Runtime.Serialization 等 .NET Framework 程序集。建议在项目已验证的 .NET Framework 环境使用；本文不承诺直接兼容 .NET 6/8 等运行时。
5. C# 工程引用同一发行包的 SLAN.API.dll，旁置 SLAN.API.xml 可获得 IDE 注释提示。原生依赖的位数按设备软件实际安装环境匹配，不能由 API DLL 自身的程序集版本推断。
6. 厂商 Word 说明指出：251107 之后的后台程序进行 HTTP 访问时需以管理员身份运行，否则可能返回 404。遇到此问题时结合服务日志核查；本次未启动服务或修改本机权限设置。

示例启动命令，仅在需要启动该服务时执行：

```powershell
$slanDir = 'D:\SLAN\SLAN 9.0.1.260921_API'
Start-Process -FilePath (Join-Path $slanDir 'SLAN.WCFServer.exe') -WorkingDirectory $slanDir -WindowStyle Hidden
```

HTTP 服务端口为 8186。配置文件里的 2966、6088 是其他组件地址，不是本文 SLAN HTTP API 的请求端口。本机 C# 客户端通常使用 WCF 命名管道；`IPAddress` 默认为 localhost。远程部署需要按现场服务配置核查网络端点，不能把 HTTP URL 直接赋给 IPAddress。

## 3 必须分清的名称与返回值

| 场景 | 正确名称或规则 |
|---|---|
| C# 发现设备 | SlanService.GetAllDevices()，复数 |
| HTTP 发现设备 | POST /GetAllDevice，单数 |
| C# 熔解扫描温度 | device.GetMeltCurrentScanTemperature() |
| HTTP 熔解扫描温度 | POST /GetMeltScanTemperature |
| 项目增删查 | 属于 SlanService，不属于 SlanDevice |
| 当前实验结果 | device.GetExperimentResult() 返回 Experiment |
| CSV 路径 | 使用 PlateFile；PlateFlie 是保留的旧拼写 |
| 已加载实验名称 | GetAllExperimentName；不是旧说明的 GetOpenedExperimentName |
| 旧自动门状态 | GetAutoDoorStauts，历史拼写；新代码优先 GetAutoDoorState |
| 新自动门 HTTP 参数 | GetAutoDoorState 只接收 series，不接收 block |
| 分析参数 | Paramter、Paramters 是 SDK 实际拼写，不能自行纠正 |
| 项目字段 | CorssTalk、Experiment.Porjects 是 SDK 实际拼写 |

不能给所有调用套用同一套“成功码”。具体分为：

- `RunExperimentResponse`：判断 `Error == RunExperimentError.None`，即 0；`SavePath` 是服务端结果文件路径。当前响应对象只有 Error、SavePath，没有 XML 旧描述提到的“访问 ID”字段。
- `APIBaseResult` 及派生类型：先判断 `ErrorCode == APIError.None`，再读取 Result、Data 或 Token 等载荷。带 bool Result 的接口还应按业务含义检查 Result；例如 IsSameProgram 的 false 表示程序不一致，不能只凭 ErrorCode=0 当作相同。
- `RunningInformation`：`State` 是运行状态，`Code` 是 RunningError 错误枚举；`Code=0` 表示无运行错误，不是“接口调用失败”。
- bool、int、数组、Experiment 等直接返回类型：按对应契约处理；通信异常应单独捕获，不能一律当成成功或空结果。
- HTTP 外层采用 `接口名Result` 包装，例如 `GetVersionResult`、`RunExperimentExResult`。HTTP 200 仅说明请求得到响应，还需检查内部业务返回值。

## 4 标准实验流程

1. 启动目标版本服务，C# 显式 Initialize；读取 GetVersion 并记录日志。
2. 正常初次连接时枚举设备，按 SerialNumber 选定目标。不要长期使用“列表第一台”作为设备标识；异常恢复时先遵循 4.2 节，不能把重新枚举当作完全无副作用的探测。
3. 检查 ExperimentList，并读取相应模块的运行信息。先识别 Running、Crash 等已有会话，再决定继续、停止或新建实验。
4. 获取或注册项目。新集成优先 AddProjectEx，检查 ErrorCode 和 Result。同名项目会被覆盖。
5. 准备孔板 CSV，确保 TestPrj 对应服务端已存在的项目，孔号适配实际机型；若混用多个项目，先确认 PCR 程序一致，可用 IsSameProgram 辅助检查。
6. 创建 ExperimentSetting，明确赋值 Block，PlateFile 与 PlateContent 二选一，调用 RunExperimentEx。
7. 确认启动响应 Error=0，保存返回的 SavePath。保存设备序列号和本次模块掩码，后续状态、停止、读取和释放始终指向同一会话。
8. 周期调用 GetRunningInformation。工程上可先采用约 1 秒一次的串行轮询；这是接入建议，不是厂商保证的频率指标。设备运行期间不要在 UI 线程执行阻塞调用。
9. 荧光数量增加时读取 GetAQResult；熔解扫描温度变化时读取 GetMeltResult。避免无差别高频重复拉取全部曲线。
10. State=Finished 后读取 GetExperimentResult，并检查实际需要的孔、目标及曲线数据。数据尚未就绪时允许有限重试；不得把空结果直接当作最终结果成功交付。
11. 结果已保存或被应用可靠接收后，调用 ReleaseExperiment 释放设备实验会话。释放失败需要保留状态并允许重试。
12. 应用退出时调用 SlanService.Close 关闭客户端。Exit 会要求服务端退出，不应作为普通窗口关闭时的默认清理操作。

### 4.1 模块选择与设备差异

Block 是 byte 模块掩码，每位代表一个模块。不要用空闲时的 UseBlock=0 推导“本次不需要指定模块”；ExperimentSetting.Block 应独立赋值。`SlanDevice.UseBlock` 既暴露设备枚举时的占用信息，也可作为后续实例操作的模块选择；重新获取对象后应按目标会话确认或设置它。多模块程序宜使用明确的 series/block 服务端接口，或为每个会话维护独立对象和选择状态。

当前 RNASeq 工程中的适配值如下，仅说明现有接入约定，其他机型需独立验证：

| 机型 | DeviceType 数值 | 本工程实验 Block | 孔位 | UseHotBonnet |
|---|---:|---:|---|---|
| SLAN_08H | 11 | 0x01 | A1–A8 | true |
| SLAN_16H | 26 | 0x01 | A1–A8、B1–B8 | false |

16 个孔不表示应把 Block 写成 16，也不表示项目包含 16 个光学通道。现有工程对这两款设备使用 8 个项目通道，交叉干扰矩阵由适配层生成 8×8 的结构；实际检测通道及串色校准值应沿用已验证的项目配置。不能把 Postman 中其他机型的旧矩阵直接作为本机实验参数。

同包 ISLANService XML 对状态查询、停止、释放、曲线和自动门等带 block 参数的接口明确说明：0x01 为模块 A，0x02 为模块 B，0x03 为 A+B，默认 0x00 表示所有模块。因此旧示例中的 block=0 不是“第 0 个模块”，在多模块设备上可能扩大操作范围。本文 08H/16H 示例明确采用掩码 1；其他设备按 ExperimentList、对应机型配置及厂商要求选择。

### 4.2 运行状态与中断恢复

| RunningState | 值 | 处理建议 |
|---|---:|---|
| None | 0 | 无运行状态；刚启动短暂出现时可有限重试，持续出现需诊断，不代表完成 |
| Running | 1 | 继续轮询 |
| Error | 2 | 记录 Code、设备和会话信息，按故障处理 |
| Finished | 3 | 获取并核验最终结果，再释放 |
| Crash | 4 | 后台异常中断的实验；选择继续或停止并等待状态确认 |

ContinueExperiment 返回 true 表示找到断点并发起继续，仍须通过 GetRunningInformation 确认；false 表示未找到可继续实验。StopExperiment 是停止命令，没有 bool 成功返回；发出后要继续查询状态，不应直接把它等同于完成和释放。

**设备枚举和 CheckDeviceState 都不能作为无条件的重连探测。** 当前 XML 的 GetAllDevice 特别提示：设备中存在通信错误、超温等出错实验时，枚举与 COM 口通信可能使实验自动结束，之后无法 ContinueExperiment。后台异常中断时 CheckDeviceState 也有停止运行的特别提示；XML 同时注明 20250805 后会返回 false，应结合 ErrorCode/Result 判断，不能仅凭方法名把它视为无副作用的健康检查。

因此首次连接正常空闲设备时可以枚举；已知异常会话恢复时，应保留并使用原 SerialNumber、Block，通过 GetRunningInformation 定向确认状态，再决定继续或停止。不要用周期 GetAllDevice/CheckDeviceState 取代运行信息轮询。原标识丢失或状态无法确认时，先结合服务日志和现场仪器状态处理，不自动枚举并宣称可无损恢复。

API 调用抛出超时或连接异常时，“客户端没收到响应”不证明仪器未执行。尤其 RunExperimentEx 超时后不要立即自动重复启动，应先重新确认原设备、原模块和实验状态。取消应用线程或轮询任务也不等同于 StopExperiment。

## 5 项目与孔板

Project 描述检测项目：Name、Type、Reaction、Targets、Program、CorssTalk、规则等。Program.Sections 保存程序段；SectionAMP 包含 Cycles 和 Step[] Items；SectionMelt 描述熔解起止温度、间隔、速率等。Step 中 IsAcquisition 控制采光。具体温度、循环、阈值与反应体系应来自已验证方案，本文不提供可直接用于真实样本的默认实验程序。

目标 Target 的 TubeNo、ChannelNo、TargetIndex 在 XML 中规定从 1 开始。目标参数为多态对象：常规定性定量用 TargetParamter，熔解用 TargetParamterMelt。HTTP 传多态对象时应保留厂商 WCF 所需的类型信息，例如 `"__type":"SectionAMP:#SLAN.API.Model"`，不要随意把所有程序段扁平化成同一种对象。

建议从服务端 GetAllProject 返回的已验证项目，或本软件已验证模板出发进行修改，再用 AddProjectEx 注册。文件型模板并不等同于 API Project 对象，不能直接把任意 JSON 文件正文作为 project 发送。

CSV 示例展示格式，ExampleProject 必须先换成服务端已有项目名称：

```csv
WellNo,TestPrj,TubeNo,Type,Property,ReTubeNo,BarCode,PName,终端编号
A1,ExampleProject,,,,,,,
A2,ExampleProject,,,,,,,
```

完整 08H/16H 孔位由实验方案决定；示例只展示两行，不代表全部孔位已配置。项目名含逗号、引号或换行时需按 CSV 规则转义。使用 PlateContent 可减少客户端与服务端文件路径不一致的问题。使用 PlateFile 时应传服务端能访问的路径；原始 CSV 的中文编码须与软件导入要求一致，不能把出现乱码的文本直接作为模板。

## 6 ExperimentSetting 关键参数

| 字段 | 类型 | 使用说明 |
|---|---|---|
| PlateFile | string | 服务端可读的 CSV 文件路径；与 PlateContent 二选一 |
| PlateContent | string | CSV 原文；与 PlateFile 二选一 |
| PlateFlie | string | 已废弃的旧拼写，不用于新代码 |
| Block | byte | 明确选择本次运行模块；不要由空闲 UseBlock 直接复制 |
| Name | string | 实验文件名称，建议唯一且不含非法文件名字符 |
| FilePath | string | 实验保存路径；留空时依赖服务端默认配置，实际路径读取 SavePath |
| UseHotBonnet | bool | 使用热盖；结合机型设置 |
| ScanMode | ChannelScanMode | Project=0、All=1 |
| TemperatureControlMode | TemperatureControlMode | Standard=0、Fast=1；使用 Fast 前检查设备支持能力 |
| Operator、Auditor、Note | string | 操作者、审核者、备注 |
| PlateNo、BatchNo | string | 孔板号、批次号 |
| CustomizeData | string | 客户定制信息的 JSON 字符串；结构须按实际集成约定 |

## 7 C# 调用示例

完整代码见 `示例/SlanApiExample.cs`，已针对包内 DLL 编译验证，但未连接实体仪器执行。代码以“已经验证过的孔板内容”为输入，不自动生成 PCR 温度程序。

```csharp
using SLAN.API.Client;
using SLAN.API.Enum;
using SLAN.API.Model;

SlanService.Initialize(); // 默认后台 SlanServerService
try
{
    string version = SlanService.GetVersion();
    // 仅在正常初次连接、已确认无出错实验时枚举。
    SlanDevice[] devices = SlanService.GetAllDevices();
    // 按 SerialNumber 选择设备。先处理已有 Running/Crash 会话。
    // setting 应明确包含 Block 和 PlateFile/PlateContent。
    // RunExperimentResponse started = device.RunExperimentEx(setting);
    // 判断 started.Error == RunExperimentError.None 后才进入运行轮询。
}
finally
{
    SlanService.Close(); // 仅关闭客户端，不代表停止实验
}
```

同一客户端代理的 UI 操作、后台轮询、停止与结果读取建议由同一个锁或串行队列调度。多次 Initialize 会切换连接，应在会话管理层统一处理。不要用空 catch 吞掉通信异常，也不要在异常 finally 中无条件 ReleaseExperiment 或 Exit。

## 8 HTTP JSON 调用示例

当前 DLL 的 ISLANService 上带 WebInvoke 的方法已逐一列入后面的 HTTP 清单。请求使用 POST，参数名大小写按清单保持一致；请求头设置 `Content-Type: application/json`。返回结果的外层键为方法名加 Result，内部对象字段使用 SDK 的原名。

### 8.1 版本与设备

```powershell
$baseUrl = 'http://localhost:8186/SlanServerService'
$v = Invoke-RestMethod -Method Post -Uri ($baseUrl + '/GetVersion') -ContentType 'application/json' -Body '{}'
$v.GetVersionResult
# 以下枚举仅适用于正常连接，异常会话恢复时改用原 series/block 查询运行信息。
$d = Invoke-RestMethod -Method Post -Uri ($baseUrl + '/GetAllDevice') -ContentType 'application/json' -Body '{}'
$d.GetAllDeviceResult
```

### 8.2 启动实验请求

POST `/RunExperimentEx`，以下是结构示例。替换 series、项目名称及机型设置，并完成前述检查后再发送：

```json
{
  "series": "REPLACE_WITH_DEVICE_SERIAL",
  "experimentSetting": {
    "PlateFile": null,
    "PlateContent": "WellNo,TestPrj,TubeNo,Type,Property,ReTubeNo,BarCode,PName,终端编号\r\nA1,ExampleProject,,,,,,,\r\n",
    "Block": 1,
    "Name": "Example_20261116_001",
    "UseHotBonnet": true,
    "ScanMode": 0,
    "TemperatureControlMode": 0,
    "FilePath": null
  }
}
```

成功响应结构示意，路径和文件名仅为示例，不是本次实测结果：

```json
{
  "RunExperimentExResult": {
    "Error": 0,
    "SavePath": "D:\\SLAN\\Experiment\\Example_20261116_001.pcr"
  }
}
```

### 8.3 查询、停止、释放

GetRunningInformation、GetExperimentResult、GetAQResult、GetMeltResult、ContinueExperiment、StopExperiment 和 ReleaseExperiment 使用同样的目标参数形状：

```json
{"series":"REPLACE_WITH_DEVICE_SERIAL","block":1}
```

这些是不同操作，不能把同一请求连续发到所有接口。正常顺序为启动→轮询→完成后读取结果→释放；停止、继续仅在需要时单独执行。StopExperiment 的契约返回 void，不能要求响应一定具有 bool 的 StopExperimentResult。

### 8.4 HTTP 访问控制

厂商说明将登录列为可选功能，需要安装带权限的 SLAN 软件。当前契约为 `Login(string userName, string password, bool isencrypted)`；JSON 键应为 **userName**，不能照旧 Word 文本写成 username。成功响应中获取 Token，并在之后的请求头设置 `X-Auth-Token`。检查 Success 和错误信息后再保存会话状态。

若使用 isencrypted=true，需按厂商原始资料约定的 AES/CBC/PKCS7、UTF-8 和 Base64 实现；具体兼容参数见附带原始说明。本文不把带固定加密参数的协议包装成安全承诺。普通调试使用实际部署要求的认证配置，不假定所有安装都需要或支持登录。

## 9 结果读取与离线再分析

完整结果层次为 Experiment → WellResult[] → TargetResult[]。主要内容：

- Experiment：名称、时间、设备、孔板、操作者、WellResult、TargetStd 及 Porjects 等。
- WellResult：孔号 ID、样本、项目、患者信息、TargetResults 及结论。
- TargetResult：Ct、Cn、Rn，原始曲线 RawCurve、扩增曲线 AMPCurve、原始熔解 RawMeltCurve、峰曲线 PeakCurve、PeakValues 等。
- TargetStd：标准曲线相关数据。具体字段类型见模型附录。

保留原始测量结果，不要把缺失值或无效状态自行转换为 0，再当作正常 Ct。不同分析模式下字段含义和有效性应结合项目配置解释。GetMeltScanTemperature 在未执行熔解程序段时返回 NaN，不能把它视为有效温度。

只需读取已有 .pcr 文件时，使用 LoadExperimentResult(file)。需要修改实验信息或再分析时，使用以下流程，并逐次检查 APIResult：

1. OpenExperiment(file) 将服务端文件加载到内存。
2. 从返回 Experiment.Name 或 GetAllExperimentName 获取实际名称，不用客户端臆造的文件名替代。
3. GetExperimentBasicInfo / GetExperimentParamters 读取当前信息，修改后分别 SetExperimentBasicInfo / SetExperimentParamter。
4. 需要时 SetProjectRule 设置高级规则；rulePath 为空表示切换基本规则。
5. GetExperimentData 获取当前数据，SaveExperiment(name) 持久化修改。
6. CloseExperiment(name) 关闭此已加载实验，释放相应内存。

CloseExperiment(name) 管理按名称打开的实验；ReleaseExperiment(series, block) 或 device.ReleaseExperiment() 管理设备运行会话；SlanService.Close() 管理客户端连接，三者不可互相替代。

文件路径均应从服务端视角理解。HTTP DownloadFile(path) 返回 APIDownloadResult（Data 为 byte[]，另有 ErrorCode/Message），字节数组的 JSON 编码表现需以实际 WCF 响应为准；不能未经验证就假定是可直接下载的 URL 或 Base64 字符串。

## 10 自动门与常见故障

支持自动门的设备，在确认不存在运行中或待恢复实验后，可按 CheckDeviceState → GetAutoDoorState → OpenDoor/CloseDoor → 查询最终门状态的顺序操作。ErrorCode=CommandExecuting 时表示状态仍在查询，延迟后重查；DeviceNotSupport 表示设备不支持，不应循环重发命令。

GetAutoDoorState.Result 是 `List<AutoDoorState>`，各门状态按厂商设备定义对应。Unknown=0、Opened=1、Closed=2、ExperimentRunning=3、DoorRunning=4、CommandExecuting=5、Error=6。不要将旧 GetAutoDoorStauts 返回的 RunningState 当成这组门状态枚举。

| 现象 | 优先检查 |
|---|---|
| 连接拒绝或 WCF 端点不可用 | 服务是否启动、服务名是否匹配、实际运行路径是否为本包、网络配置 |
| HTTP 404 | 8186 端口和服务名、当前实例、厂商要求的管理员启动条件 |
| HTTP 400 | JSON 是否含注释/尾逗号、参数大小写、Content-Type、多态 __type |
| GetAllDevice 为空 | 设备连接、驱动、设备电源及服务端配置；服务连通不等于设备连通 |
| Plate_ProjectNotExist | CSV TestPrj 是否与该后台服务中的项目名称匹配 |
| Plate_PCRDifferent | 同板项目的 PCR 程序是否一致 |
| ExperimentIsRunning / HasCrashExperiment | 查询原模块状态，处理既有实验，避免重复启动 |
| ProjectParameterInvalid | Project、目标、程序段、串色矩阵及机型通道数是否完整匹配 |
| Finished 后结果为空 | 服务端最终结果可能尚未落定；有限重试并保留会话，检查服务日志 |
| 升级后机型识别错误 | 核对整个服务包及客户端 DLL；程序集版本 1.0.0.0 不能证明已升级 |

原 Postman 集合部分 raw body 内有 `//` 或 `///` 注释，它们不是合法的标准 JSON。发送前必须移除。该集合较旧，本文 HTTP 清单以 260921 DLL 的 WebInvoke 属性为准。

## 11 验证范围与资料优先级

本次完成：发行版本与哈希核对、实际 DLL 公开签名和枚举反射、HTTP 契约核对、260909→260921 公开表面对比、C# 示例编译、MD/TXT 内容一致性检查、压缩包文件集及逐文件 SHA256 检查。

本次未进行：启动厂商服务、设备连接、真实实验、温控/光学验证、HTTP 在线请求、驱动或全新机器安装验收。示例中的返回值仅作格式说明。

资料优先级：当前发行 DLL 的签名与属性 → 同包 XML 的语义说明 → 厂商旧 Word/Postman → 本地业务代码中的接入策略。XML 没有说明的取值范围、单位或默认语义不自行补全；附录中标为“原 XML 未说明”的项需要结合厂商确认或现场验证。

来源位置：

- 完整发行包：D:\Code\PCRinOne\SLAN.API\SLAN 9.0.1.260921_API
- 对照发行包：D:\Code\PCRinOne\SLAN.API\SLAN 9.0.1.260909_API
- 厂商参考：D:\Code\PCRinOne\SLAN.API\API使用方法251120.docx、SLANAPI.postman_collection260311.json
- 现有工程：D:\PCRinOne\framework\RNASeq\RNASeq\Services\Slan\ 下的 SlanClientService、SlanRuntimeManager、SlanDeviceProfile、SlanPlateBuilder、SlanExperimentRunner、SlanTemplateService。

以下附录自动从包内 DLL 的反射结果与同包 XML 注释合并生成。类型采用简写；字段继承关系单独标出。所有枚举数值均来自 DLL，方法参数顺序和拼写亦以 DLL 为准。

## 附录 A C# 客户端完整接口

### SLAN.API.Client.SlanService

| 属性 | 类型 | 说明 |
| --- | --- | --- |
| IPAddress | string | IP地址，不设置时为localhost |

#### .ctor

```csharp
SlanService()
```

原 XML 未说明

#### Initialize

```csharp
static void Initialize(string name = "SlanServerService")
```

初始化连接

#### Close

```csharp
static void Close()
```

关闭客户端，使服务器释放客户端资源；此接口与Exit不同，不会关闭服务器

#### GetVersion

```csharp
static string GetVersion()
```

获取服务版本号

#### GetAllDevices

```csharp
static SlanDevice[] GetAllDevices()
```

获取当前连接的所有设备

#### LoadExperiment

```csharp
static WellResult[] LoadExperiment(string file)
```

从数据文件获得实验结果数据

| 参数 | 原 XML 说明 |
| --- | --- |
| file | 数据文件路径 |

弃用标记：`[System.ObsoleteAttribute("Use LoadExperimentResult instead")]`。

#### LoadExperimentResult

```csharp
static Experiment LoadExperimentResult(string file)
```

从数据文件获得实验结果数据

| 参数 | 原 XML 说明 |
| --- | --- |
| file | 数据文件路径 |

#### GetAllProject

```csharp
static Project[] GetAllProject()
```

获取所有项目

#### AddProject

```csharp
static bool AddProject(Project project)
```

增加项目，如果项目已经存在，则进行覆盖

弃用标记：`[System.ObsoleteAttribute("Use AddProjectEx instead")]`。

#### AddProjectEx

```csharp
static APIResult AddProjectEx(Project project)
```

增加项目，如果项目已经存在，则进行覆盖

#### RemoveProject

```csharp
static bool RemoveProject(string projectName)
```

删除项目

#### OpenExperiment

```csharp
static APIExperimentResult OpenExperiment(string file)
```

打开实验

| 参数 | 原 XML 说明 |
| --- | --- |
| file | 数据文件路径 |

原 XML 返回说明：实验

#### GetExperimentData

```csharp
static APIExperimentResult GetExperimentData(string name)
```

打开实验

| 参数 | 原 XML 说明 |
| --- | --- |
| name | 实验名称 |

原 XML 返回说明：实验

#### SaveExperiment

```csharp
static APIResult SaveExperiment(string name)
```

保存实验

| 参数 | 原 XML 说明 |
| --- | --- |
| name | 实验名称 |

#### SetExperimentBasicInfo

```csharp
static APIResult SetExperimentBasicInfo(string name, ExperimentBasicInfo info)
```

修改实验信息

| 参数 | 原 XML 说明 |
| --- | --- |
| name | 实验名称 |
| info | 实验信息 |

#### CloseExperiment

```csharp
static APIResult CloseExperiment(string name)
```

关闭实验

| 参数 | 原 XML 说明 |
| --- | --- |
| name | 实验名称 |

#### SetExperimentParamter

```csharp
static APIResult SetExperimentParamter(string name, AnalyseParamter paramter)
```

设置实验参数

| 参数 | 原 XML 说明 |
| --- | --- |
| name | 实验名称 |
| paramter | 修改的参数 |

#### GetExperimentParamters

```csharp
static APIExperimentParamtersResult GetExperimentParamters(string name)
```

获取实验所包含的参数

| 参数 | 原 XML 说明 |
| --- | --- |
| name | 实验名称 |

#### GetExperimentBasicInfo

```csharp
static APIExperimentBasicInfoResult GetExperimentBasicInfo(string name)
```

获取实验基本信息

| 参数 | 原 XML 说明 |
| --- | --- |
| name | 实验名称 |

#### SetProjectRule

```csharp
static APIResult SetProjectRule(string name, string projectName, string rulePath)
```

设置实验项目高级规则

| 参数 | 原 XML 说明 |
| --- | --- |
| name | 实验名称 |
| projectName | 项目名称 |
| rulePath | 高级规则文件路径 路径为空时设置为基本规则 |

#### GetAllExperimentName

```csharp
static APIOpenedExperimentNameResult GetAllExperimentName()
```

获取实验基本信息

#### GetExperimentRunTemperature

```csharp
static APIExperimentRunTemperatureResult GetExperimentRunTemperature(string name)
```

获取实验运行温度

#### IsSameProgram

```csharp
static APIResult IsSameProgram(string project1, string project2)
```

判断2个项目的实验程序是否一致，一致时可以同时运行

| 参数 | 原 XML 说明 |
| --- | --- |
| project1 | 项目名称1 |
| project2 | 项目名称2 |

#### SaveExperimentStd

```csharp
static APIResult SaveExperimentStd(string experimentName, string projectName, string stdName)
```

保存实验的标准曲线至外部标准曲线文件中

| 参数 | 原 XML 说明 |
| --- | --- |
| experimentName | 实验名称 |
| projectName | 项目名称 为空时保存实验中所有的标准曲线 |
| stdName | 保存的外标文件名 为空时默认项目名称(项目名称为空时,文件名不起作用) |

#### GetExternalStdData

```csharp
static APIExternalStdResult GetExternalStdData(string stdName)
```

查找外部标准曲线文件,获取标准曲线数据

| 参数 | 原 XML 说明 |
| --- | --- |
| stdName | 外部标准曲线文件名,为空时获取所有 |

#### SetExperimentStd

```csharp
static APIResult SetExperimentStd(string experimentName, string projectName, string stdName)
```

设置实验的外部标准曲线

| 参数 | 原 XML 说明 |
| --- | --- |
| experimentName | 实验名称 |
| projectName | 需要设置外标的项目名称 |
| stdName | 调用的外部标准曲线文件名称 |

#### Exit

```csharp
static bool Exit()
```

服务器退出

#### RefreshProject

```csharp
static APIResult RefreshProject()
```

刷新项目列表，重新加载所有项目

#### SetProjectBasicRule

```csharp
static APIResult SetProjectBasicRule(BasicRule basicRule)
```

设置项目的基本规则

#### GetProjectBasicRule

```csharp
static APIBasicRuleResult GetProjectBasicRule(string projectName)
```

获取项目的基础规则

#### ReleaseExperiment

```csharp
static bool ReleaseExperiment(string series, byte block)
```

释放实验

| 参数 | 原 XML 说明 |
| --- | --- |
| series | 仪器编号 |
| block | 模块信息 |

#### GetFluorescenceCount

```csharp
static int GetFluorescenceCount(string series, byte block)
```

获取扩增曲线荧光数量

#### GetMeltScanTemperature

```csharp
static double GetMeltScanTemperature(string series, byte block)
```

获取熔解曲线扫描温度

#### GetAQResult

```csharp
static WellResult[] GetAQResult(string series, byte block)
```

获取扩增程序段数据

#### GetMeltResult

```csharp
static WellResult[] GetMeltResult(string series, byte block)
```

获取熔解程序段数据

### SLAN.API.Client.SlanDevice

| 属性 | 类型 | 说明 |
| --- | --- | --- |
| SerialNumber | string | 设备编号 |
| DeviceType | DeviceType | 设备类型 |
| Fast | bool | 设备是否支持Fast控温 |
| UseBlock | byte | 设备模块使用状态，每个模块占1位(0：未使用， 1：已使用) |
| PcrFile | string | 数据文件路径（会存在多个模块一起运行，所以使用ExperimentList代替） |
| ExperimentList | List<RunningBlock> | 正在运行的实验信息 |

#### RunExperiment

```csharp
RunExperimentResponse RunExperiment(string plate)
```

运行实验

| 参数 | 原 XML 说明 |
| --- | --- |
| plate | 孔板文件 |

原 XML 返回说明：实验信息，包括数据文件和访问ID

弃用标记：`[System.ObsoleteAttribute("Use RunExperimentEx instead")]`。

#### RunExperiment

```csharp
RunExperimentResponse RunExperiment(string plate, ExperimentSetting experimentSetting)
```

运行实验

| 参数 | 原 XML 说明 |
| --- | --- |
| plate | 孔板文件 |
| experimentSetting | 设置信息，ExperimentSetting.Block和UseBlock无关，需要重新赋值 |

原 XML 返回说明：实验信息，包括数据文件和访问ID

弃用标记：`[System.ObsoleteAttribute("Use RunExperimentEx instead")]`。

#### RunExperimentEx

```csharp
RunExperimentResponse RunExperimentEx(ExperimentSetting experimentSetting)
```

运行实验

| 参数 | 原 XML 说明 |
| --- | --- |
| experimentSetting | 设置信息，ExperimentSetting.Block和UseBlock无关，需要重新赋值 设置信息中，PlateFlie和PlateContent两者必须选择其中一个进行赋值 |

原 XML 返回说明：实验信息，包括数据文件和访问ID

#### ContinueExperiment

```csharp
bool ContinueExperiment()
```

断点继续运行实验

原 XML 返回说明：true：找到断点实验，并且继续运行，然后通过GetRunningInformation获取实验状态 fakse：没有找到断点实验，无法继续运行

#### StopExperiment

```csharp
void StopExperiment()
```

停止实验

#### ReleaseExperiment

```csharp
bool ReleaseExperiment()
```

释放实验，当实验运行结束时，调用此方法，后台程序从内存中释放实验

#### GetRunningInformation

```csharp
RunningInformation GetRunningInformation()
```

获取运行信息

#### GetResult

```csharp
WellResult[] GetResult()
```

获取结果数据

弃用标记：`[System.ObsoleteAttribute("Use GetExperimentResult instead")]`。

#### GetExperimentResult

```csharp
Experiment GetExperimentResult()
```

获取结果数据

#### GetFluorescenceCount

```csharp
int GetFluorescenceCount()
```

获取当前荧光数量

#### OpenDoor

```csharp
bool OpenDoor()
```

打开自动门

#### CloseDoor

```csharp
bool CloseDoor()
```

关闭自动门

#### CanOperateDoor

```csharp
bool CanOperateDoor()
```

是否可以操作自动门

弃用标记：`[System.ObsoleteAttribute("Use GetAutoDoorState instead")]`。

#### GetAutoDoorStauts

```csharp
RunningState GetAutoDoorStauts()
```

获取自动门操作结果

弃用标记：`[System.ObsoleteAttribute("Use GetAutoDoorState instead")]`。

#### GetMeltCurrentScanTemperature

```csharp
double GetMeltCurrentScanTemperature()
```

获取熔解曲线当前扫描温度

#### GetAQResult

```csharp
WellResult[] GetAQResult()
```

获取扩增程序段数据

#### GetMeltResult

```csharp
WellResult[] GetMeltResult()
```

获取熔解程序段数据

#### CheckDeviceState

```csharp
APIResult CheckDeviceState()
```

检查仪器状态

#### GetAutoDoorState

```csharp
APIAutoDoorStateResult GetAutoDoorState()
```

获取自动门状态

原 XML 返回说明：ErrorCode = None 时可以通过APIAutoDoorStateResult的Result来正确的获取到各个自动门的状态 Result解释{未知,打开状态,关闭状态,实验正在运行,自动门正在运行,状态查询中,出错} Result { 0, 1, 2, 3, 4, 5 , 6}

## 附录 B HTTP 完整接口清单

全部端点来自当前 DLL 的 ISLANService.WebInvoke 属性。基地址为 `http://localhost:8186/SlanServerService`。参数列给出顶层 JSON 字段及类型；空参数用 `{}`。响应列是去掉外层 `方法名Result` 后的类型。返回 void 的方法不承诺有可读取的业务载荷。

### POST /GetVersion

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| {} | string | 获取服务版本号 |

原 XML 返回说明：服务版本号

### POST /GetAllDevice

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| {} | Device[] | 获取当前连接的所有设备 **特别提示** **当运行的设备中包含出错的实验(包括但不限于通信出错、超温出错等等)。** **此时调用此接口与COM口通信会导致实验自动结束，无法通过ContinueExperiment来继续运行此实验** |

### POST /Exit

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| {} | bool | 退出 如果有正在运行的实验，返回false，不执行退出 |

### POST /GetAllProject

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| {} | Project[] | 获取所有项目 |

### POST /AddProject

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| project: Project | bool | 增加项目（已弃用，参见当前替代接口） |

| 参数名 | 原 XML 约定 |
| --- | --- |
| project | 项目 |

### POST /AddProjectEx

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| project: Project | APIResult | 增加项目 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| project | 项目 |

### POST /RemoveProject

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| projectName: string | bool | 删除项目 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| projectName | 项目名称 |

### POST /RunExperiment

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| series: string, plate: string, block: byte | RunExperimentResponse | 运行实验（已弃用，参见当前替代接口） |

| 参数名 | 原 XML 约定 |
| --- | --- |
| series | 仪器序列号 |
| plate | 孔板文件绝对路径，确保Project文件夹中存在孔板文件中使用的项目 |
| block | 仪器模块(比如：0x01为使用模块A；0x02为使用模块B；0x03为使用模块A和模块B)，默认值0x00表示所有模块 |

### POST /RunExperimentWithSetting

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| series: string, plate: string, experimentSetting: ExperimentSetting | RunExperimentResponse | 运行实验（已弃用，参见当前替代接口） |

| 参数名 | 原 XML 约定 |
| --- | --- |
| series | 仪器序列号 |
| plate | 孔板文件绝对路径，确保Project文件夹中存在孔板文件中使用的项目 |
| experimentSetting | 实验设置信息 |

### POST /RunExperimentEx

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| series: string, experimentSetting: ExperimentSetting | RunExperimentResponse | 运行实验 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| series | 仪器序列号 |
| experimentSetting | 实验设置信息 设置信息中，PlateFlie和PlateContent两者必须选择其中一个进行赋值 |

### POST /ContinueExperiment

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| series: string, block: byte | bool | 断点继续运行实验 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| series | 仪器序列号 |
| block | 仪器模块 |

原 XML 返回说明：true：找到断点实验，并且继续运行，然后通过GetRunningInformation获取实验状态 false：没有找到断点实验，无法继续运行

### POST /StopExperiment

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| series: string, block: byte | void | 停止实验 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| series | 设备序列号 |
| block | 仪器模块(比如：0x01为使用模块A；0x02为使用模块B；0x03为使用模块A和模块B)，默认值0x00表示所有模块 |

### POST /ReleaseExperiment

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| series: string, block: byte | bool | 释放实验 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| series | 设备序列号 |
| block | 仪器模块(比如：0x01为使用模块A；0x02为使用模块B；0x03为使用模块A和模块B)，默认值0x00表示所有模块 |

### POST /GetRunningInformation

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| series: string, block: byte | RunningInformation | 获取运行信息 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| series | 设备序列号 |
| block | 仪器模块(比如：0x01为使用模块A；0x02为使用模块B；0x03为使用模块A和模块B)，默认值0x00表示所有模块 |

### POST /GetFluorescenceCount

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| series: string, block: byte | int | 获取扩增曲线当前荧光数量 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| series | 设备序列号 |
| block | 仪器模块(比如：0x01为使用模块A；0x02为使用模块B；0x03为使用模块A和模块B)，默认值0x00表示所有模块 |

### POST /GetMeltScanTemperature

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| series: string, block: byte | double | 获取熔解曲线当前扫描温度 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| series | 设备序列号 |
| block | 仪器模块(比如：0x01为使用模块A；0x02为使用模块B；0x03为使用模块A和模块B)，默认值0x00表示所有模块 |

原 XML 返回说明：正在执行熔解程序段时，返回当前温度，其他情况下，返回NaN

### POST /GetAQResult

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| series: string, block: byte | WellResult[] | 获取扩增程序段数据 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| series | 设备序列号 |
| block | 仪器模块(比如：0x01为使用模块A；0x02为使用模块B；0x03为使用模块A和模块B)，默认值0x00表示所有模块 |

### POST /GetMeltResult

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| series: string, block: byte | WellResult[] | 获取熔解程序段数据 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| series | 设备序列号 |
| block | 仪器模块(比如：0x01为使用模块A；0x02为使用模块B；0x03为使用模块A和模块B)，默认值0x00表示所有模块 |

### POST /GetResult

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| series: string, block: byte | WellResult[] | 获取所有结果数据(已过时,应使用GetExperimentResult作为替代)（已弃用，参见当前替代接口） |

| 参数名 | 原 XML 约定 |
| --- | --- |
| series | 设备序列号 |
| block | 仪器模块(比如：0x01为使用模块A；0x02为使用模块B；0x03为使用模块A和模块B)，默认值0x00表示所有模块 |

### POST /GetExperimentResult

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| series: string, block: byte | Experiment | 获取实验数据(包含结果数据) |

| 参数名 | 原 XML 约定 |
| --- | --- |
| series | 设备序列号 |
| block | 仪器模块(比如：0x01为使用模块A；0x02为使用模块B；0x03为使用模块A和模块B)，默认值0x00表示所有模块 |

### POST /OpenDoor

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| series: string, block: byte | bool | 打开自动门 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| series | 仪器序列号 |
| block | 仪器模块(比如：0x01为使用模块A；0x02为使用模块B；0x03为使用模块A和模块B)，默认值0x00表示所有模块 |

### POST /CloseDoor

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| series: string, block: byte | bool | 关闭自动门 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| series | 仪器序列号 |
| block | 仪器模块(比如：0x01为使用模块A；0x02为使用模块B；0x03为使用模块A和模块B)，默认值0x00表示所有模块 |

### POST /CanOperateDoor

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| series: string, block: byte | bool | 获取是否可以操作自动门（已弃用，参见当前替代接口） |

| 参数名 | 原 XML 约定 |
| --- | --- |
| series | 仪器序列号 |
| block | 仪器模块(比如：0x01为使用模块A；0x02为使用模块B；0x03为使用模块A和模块B)，默认值0x00表示所有模块 |

### POST /GetAutoDoorStauts

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| series: string, block: byte | RunningState | 获取自动门操作结果（已弃用，参见当前替代接口） |

| 参数名 | 原 XML 约定 |
| --- | --- |
| series | 仪器序列号 |
| block | 仪器模块(比如：0x01为使用模块A；0x02为使用模块B；0x03为使用模块A和模块B)，默认值0x00表示所有模块 |

### POST /GetAutoDoorState

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| series: string | APIAutoDoorStateResult | **特别提示** **调用此接口前需先调用CheckDeviceState来更新设备的状态** **当设备正在运行实验时状态会自动更新，无需频繁调用CheckDeviceState** 获取自动门状态结果 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| series | 仪器序列号 |

原 XML 返回说明：ErrorCode = CommandExecuting 正在获取最新的设备状态，稍后再调用此接口 ErrorCode = DeviceStateNotFound 未获取到仪器的状态 ErrorCode = DeviceNotFound 未找到该仪器 ErrorCode = DeviceNotSupport 设备不支持此功能 ErrorCode = None 时可以通过APIAutoDoorStateResult的Result来正确的获取到各个自动门的状态 Result解释{未知,打开状态,关闭状态,实验正在运行,自动门正在运行,状态查询中,出错} Result { 0, 1, 2, 3, 4, 5 , 6}

### POST /CheckDeviceState

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| series: string | APIResult | 检查设备状态 **特别提示** **当运行的设备中包含异常中断的实验 。** **此时调用此接口与COM口通信会导致仪器停止运行 20250805后会返回false** |

| 参数名 | 原 XML 约定 |
| --- | --- |
| series | 仪器序列号 |

原 XML 返回说明：ErrorCode = DeviceNotFound 未找到该仪器 ErrorCode = DeviceNotSupport 设备不支持此功能

### POST /LoadExperimentResult

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| file: string | Experiment | 加载实验结果，不需要释放内存 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| file | 数据文件路径 |

原 XML 返回说明：实验分析结果

### POST /OpenExperiment

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| file: string | APIExperimentResult | 打开本地实验文件，并加载到内存中，不使用时需要调用CloseExperiment关闭实验，释放内存 1、OpenExperiment 2、GetExperimentParamter、GetExperimentBasicInfo (修改分析参数、实验基本信息前需先获取此参数后再进行修改) 3、SetExperimentParamter、SetExperimentBasicInfo、SetProjectRule 4、SaveExperiment 5、CloseExperiment |

| 参数名 | 原 XML 约定 |
| --- | --- |
| file | 数据文件路径 |

原 XML 返回说明：实验

### POST /GetExperimentData

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| name: string | APIExperimentResult | 打开实验 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| name | 实验名称 |

原 XML 返回说明：实验

### POST /SaveExperiment

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| name: string | APIResult | 保存实验 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| name | 实验名称 |

### POST /CloseExperiment

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| name: string | APIResult | 关闭实验 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| name | 实验名称 |

### POST /GetExperimentParamters

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| name: string | APIExperimentParamtersResult | 获取实验所包含的参数 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| name | 实验名称 |

### POST /SetExperimentParamter

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| name: string, paramter: AnalyseParamter | APIResult | 设置分析参数 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| name | 实验名称 |
| paramter | 分析参数 |

### POST /SetProjectRule

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| name: string, projectName: string, rulePath: string | APIResult | 设置实验项目高级规则 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| name | 实验名称 |
| projectName | 项目名称 |
| rulePath | 高级规则文件路径,路径为空时将设置为基本规则 |

### POST /GetExperimentBasicInfo

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| name: string | APIExperimentBasicInfoResult | 获取实验基本信息 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| name | 实验名称 |

### POST /SetExperimentBasicInfo

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| name: string, info: ExperimentBasicInfo | APIResult | 设置实验基本信息 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| name | 实验名称 |
| info | 实验基本信息 |

### POST /GetAllExperimentName

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| {} | APIOpenedExperimentNameResult | 获取所有服务端已加载的实验名称 |

### POST /GetExperimentRunTemperature

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| name: string | APIExperimentRunTemperatureResult | 获取实验运行温度 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| name | 实验名称 |

原 XML 返回说明：实验运行温度

### POST /DownloadFile

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| path: string | APIDownloadResult | 下载文件 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| path | 文件路径 |

原 XML 返回说明：文件二进制数据

### POST /IsSameProgram

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| project1: string, project2: string | APIResult | 判断2个项目的实验程序是否一致，一致时可以同时运行 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| project1 | 项目名称1 |
| project2 | 项目名称2 |

### POST /SaveExperimentStd

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| experimentName: string, projectName: string, stdName: string | APIResult | 保存实验的标准曲线至外部标准曲线文件中 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| experimentName | 实验名称 |
| projectName | 项目名称 为空时保存实验中所有的标准曲线 |
| stdName | 保存的外标文件名 为空时默认项目名称(项目名称为空时,文件名不起作用) |

### POST /GetExternalStdData

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| stdName: string | APIExternalStdResult | 查找外部标准曲线文件,获取标准曲线数据 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| stdName | 外部标准曲线文件名,为空时获取所有 |

### POST /SetExperimentStd

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| experimentName: string, projectName: string, stdName: string | APIResult | 设置实验的外部标准曲线 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| experimentName | 实验名称 |
| projectName | 需要设置外标的项目名称 |
| stdName | 调用的外部标准曲线文件名称 |

### POST /RefreshProject

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| {} | APIResult | 刷新项目 |

### POST /SetProjectBasicRule

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| basicRule: BasicRule | APIResult | 设置项目基础规则 |

### POST /GetProjectBasicRule

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| projectName: string | APIBasicRuleResult | 获取项目基础规则的接口 |

### POST /Login

| 参数 | 响应载荷 | 说明 |
| --- | --- | --- |
| userName: string, password: string, isencrypted: bool | APIUserResult | 登录 |

| 参数名 | 原 XML 约定 |
| --- | --- |
| userName | 用户名 |
| password | 密码 |

`LoadExperiment` 虽是 WCF OperationContract，但当前 DLL 未为其声明 WebInvoke；本文不把它列为已核实的 HTTP POST 端点。使用 HTTP 时优先 LoadExperimentResult。SlanService.Initialize/Close 是客户端生命周期方法，不能据此拼出同名 HTTP URL。

## 附录 C 全部枚举和错误码

枚举名称与数值均来自当前 DLL；说明来自同包 XML。不要跨枚举比较同一个整数的含义。

### APIError

| 名称 | 数值 | 说明 |
| --- | --- | --- |
| None | 0 | None |
| ExperimentNotFound | 1 | 未找到实验 |
| ExperimentLoadError | 2 | 文件加载失败 |
| ProjectNotFound | 3 | 未找到项目 |
| ProjectParameterInvalid | 4 | 未找到该项目的参数或规则 |
| RulejudgeInvalid | 5 | 设置规则错误,高级规则文件无效 |
| RulejudgeNotFound | 6 | 未找到规则文件 |
| ExperimentCanNotSave | 7 | 实验无法保存,实验未完成或数据不够 |
| ExperimentSaveError | 8 | 文件保存出错 |
| ExperimentIsRunning | 9 | 实验正在运行 |
| ParamterInvalid | 10 | 设置分析参数,参数验证失败 |
| UnKnowError | 11 | 未知错误 |
| FileNotExist | 12 | 文件不存在 |
| SaveStdError | 13 | 保存标准曲线错误 |
| ProjectNotMatch | 14 | 项目不匹配 |
| DeviceNotFound | 15 | 设备未找到 |
| DeviceNotSupport | 16 | 设备不支持 |
| DeviceStateNotFound | 17 | 未查询到设备状态 |
| CommandExecuting | 18 | 正在执行命令 |
| BasicRuleFail | 19 | 基础规则设置失败 |
| HasCrashExperiment | 20 | 设备包含异常中断实验，无法刷新门状态 |
| Unauthorized | 21 | 登录验证失败 |

### AutoDoorState

| 名称 | 数值 | 说明 |
| --- | --- | --- |
| Unknown | 0 | 未知 |
| Opened | 1 | 打开状态 |
| Closed | 2 | 关闭状态 |
| ExperimentRunning | 3 | 实验正在运行 |
| DoorRunning | 4 | 自动门正在运行 |
| CommandExecuting | 5 | 状态查询中 |
| Error | 6 | 出错 |

### ChannelScanMode

| 名称 | 数值 | 说明 |
| --- | --- | --- |
| Project | 0 | 按项目 |
| All | 1 | 全通道 |

### DeviceType

| 名称 | 数值 | 说明 |
| --- | --- | --- |
| SLAN_96P | 1 | SLAN-96P |
| SLAN_96S | 2 | SLAN-96S |
| SLAN_48P | 3 | SLAN-48P |
| SLAN_24P | 6 | SLAN-24P |
| SLAN_96H | 7 | SLAN-96H |
| SLAN_96D | 8 | SLAN-96D |
| SLAN_48H | 9 | SLAN-48H |
| SLAN_24H | 10 | SLAN-24H |
| SLAN_08H | 11 | SLAN-08H |
| SLAN_04H | 12 | SLAN-04H |
| SLAN_24S | 13 | SLAN-24S |
| SLAN_48S | 14 | SLAN-48S |
| SLAN_32D | 15 | SLAN-32D |
| SLAN_32DV | 16 | SLAN-32DV |
| SLAN_48SC | 17 | SLAN-48SC |
| SLAN_96R | 20 | SLAN-96R |
| SLAN_16R | 21 | SLAN-16R |
| SLAN_16H | 26 | SLAN_16H |

### EAMPAlgorithm

| 名称 | 数值 | 说明 |
| --- | --- | --- |
| Default | 0 | 相对荧光值法 |
| Subtraction | 1 | 绝对荧光值法 |
| RelativeLinear | 2 | 相对荧光值法 - 线性 |

### ECalcResult

| 名称 | 数值 | 说明 |
| --- | --- | --- |
| None | 0 | None |
| Ct | 1 | Ct |
| Cn | 2 | Cn |
| Rn | 3 | Rn |
| r | 4 | r |

### EDigitalFilter

| 名称 | 数值 | 说明 |
| --- | --- | --- |
| None | 0 | 不滤波 |
| Normal | 1 | 轻度滤波 |
| High | 2 | 重度滤波 |

### EMeltPeakAlgorithm

| 名称 | 数值 | 说明 |
| --- | --- | --- |
| Standard | 0 | 标准算法（以最大Y值为Tm） |
| Advance | 1 | 高级算法 |

### EOperator

| 名称 | 数值 | 说明 |
| --- | --- | --- |
| Null | 0 | Null |
| Equal | 1 | 等于 |
| UnEqual | 2 | 不等于 |
| LessThan | 3 | 小于 |
| EqualLessThan | 4 | 小于等于 |

### EOptionzationMode

| 名称 | 数值 | 说明 |
| --- | --- | --- |
| NA | 0 | 不优化 |
| AUTO | 1 | 自动优化 |
| MANUAL | 2 | 手动优化 |

### EPeekType

| 名称 | 数值 | 说明 |
| --- | --- | --- |
| Positive | 0 | 正峰 |
| Negative | 1 | 负峰 |
| Positive_Negative | 2 | 正负峰 |

### EScanMode

| 名称 | 数值 | 说明 |
| --- | --- | --- |
| Continue | 0 | Continue模式 |
| Step | 1 | Step模式 |

### ExperimentType

| 名称 | 数值 | 说明 |
| --- | --- | --- |
| AQ | 0 | 定性/定量 |
| Melt | 1 | 标准熔解曲线 |
| AD | 2 | 等位基因鉴定 |
| HRM | 3 | HRM |
| RQ | 4 | 相对定量 |
| IA | 5 | 等温扩增 Isothermal amplification |
| TQ | 6 | 反式定性定量 Qualitative and quantitative trans |
| FastCal | 7 | 实验运行过程中，实时计算结果 |

### RunExperimentError

| 名称 | 数值 | 说明 |
| --- | --- | --- |
| None | 0 | None |
| ExperimentIsRunning | 1 | 实验正在运行 |
| DeviceNotExist | 2 | 没有找到设备 |
| Plate_FileNotExist | 3 | 孔板文件不存在 |
| Plate_NoData | 4 | 孔板文件没有数据 |
| Plate_FormatError | 5 | 孔板文件格式错误 |
| Plate_ProjectNotExist | 6 | 孔板文件中的项目不存在 |
| Plate_WellNoNotExist | 7 | 孔板文件中的孔号不存在 |
| Plate_PCRDifferent | 8 | 孔板文件引用的项目的PCR程序不一致 |
| Customize_IncorrectFormat | 9 | 定制信息格式错误 |

### RunningError

| 名称 | 数值 | 说明 |
| --- | --- | --- |
| None | 0 | None |
| DeviceConnectError | 1 | 设备连接出错 |
| CommunicationError | 2 | 通信出错 |
| CloseHotlidError | 3 | 关闭热盖出错 |
| SliderNotClose | 4 | 滑盖未到位 |
| OpenHotlidError | 5 | 打开热盖出错 |
| SendCommandError | 6 | 发送命令出错 |
| BlockOverTemperatureError | 7 | 模块超温出错 |
| HotlidOverTemperatureError | 8 | 热盖超温出错 |
| BlockTemperatureUpDownError | 9 | 模块升降温出错 |
| HotlidTemperatureRiseError | 10 | 热盖升温出错 |
| FluorescenceResetError | 11 | 设备信息错误 |
| DeviceInfoError | 12 | 设备信息错误 |

### RunningState

| 名称 | 数值 | 说明 |
| --- | --- | --- |
| None | 0 | None |
| Running | 1 | 正在运行 |
| Error | 2 | 出错 |
| Finished | 3 | 完成 |
| Crash | 4 | 非正常退出时，上一次实验状态 |

### SampleType

| 名称 | 数值 | 说明 |
| --- | --- | --- |
| Unknown | 0 | 样品 |
| Standard | 1 | 标准品 |
| HighPositive | 2 | 阳性对照 |
| LowPositive | 3 | 临界阳性对照 |
| Negative | 4 | 阴性对照 |
| NTC | 5 | 无模板对照 |
| QC | 6 | 校准品（质控品） |
| RetestSample | 7 | 重测样品 |
| Calibrator | 8 | 参比样品 |
| Last | 9 | 最后（只用作标识） |
| Custom | 1000 | 自定义类型 |

### TemperatureControlMode

| 名称 | 数值 | 说明 |
| --- | --- | --- |
| Standard | 0 | 标准控温 |
| Fast | 1 | 快速控温 |

## 附录 D 数据模型和辅助类型

仅列各类型自己声明的公开字段、属性和方法；继承成员需同时参看父类。例如 APIResult 系列继承 APIBaseResult.ErrorCode 与 Message；SectionAMP、SectionMelt 继承 Section 的字段。无 XML 注释的字段只保证名称与类型已反射确认，不推测语义。

### EnumManagement

枚举管理器

| 方法签名 | 说明 |
| --- | --- |
| static IList<SampleType> GetAllSampleType() | 获取所有样本类型 |
| static IList<SampleType> GetAllExperimentICSampleType() | 获取所有实验质控类型 |
| static IList<SampleType> GetAllSampleICSampleType() | 获取样本类型 |

### AnalyseParamter

分析参数

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| ProjectName | string | 项目名称 |
| AMPAlgorithm | EAMPAlgorithm | 扩增曲线算法 |
| Targets | Target[] | 目标列表 |
| CorssTalk | double[][] | 交叉干扰 |
| BasicRule | BasicRule | 基础规则 |

### APIAutoDoorStateResult

自动门状态返回结果

父类：`APIBaseResult`。

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Result | List<AutoDoorState> | 结果 |

### APIBaseResult

返回值

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| ErrorCode | APIError | 返回值 0 成功 其他失败 |
| Message | string | 错误信息 |

### APIBasicRuleResult

基础规则结果

父类：`APIBaseResult`。

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Result | BasicRule | 基本规则 |

### APIDownloadResult

APIDownloadResult

父类：`APIBaseResult`。

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Data | byte[] | 结果 |

### APIExperimentBasicInfoResult

APIExperimentBasicInfoResult

父类：`APIBaseResult`。

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Result | ExperimentBasicInfo | 结果 |

### APIExperimentParamtersResult

返回值

父类：`APIBaseResult`。

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Result | List<AnalyseParamter> | 结果 |

### APIExperimentResult

GetExperimentBasicInfo返回值

父类：`APIBaseResult`。

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Result | Experiment | 结果 |

### APIExperimentRunTemperatureResult

返回值

父类：`APIBaseResult`。

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Result | List<TemperatureDot> | 结果 |

### APIExternalStdResult

APIExternalStdResult

父类：`APIBaseResult`。

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Result | ExternalStd[] | 结果 |

### APIOpenedExperimentNameResult

APIOpenedExperimentNameResult返回值

父类：`APIBaseResult`。

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Result | List<string> | 结果 |

### APIResult

返回值

父类：`APIBaseResult`。

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Result | bool | 结果 |

### APIUserResult

返回值

父类：`APIBaseResult`。

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Token | string | Token |
| Success | bool | 结果 |

### BasicRule

基础规则

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Name | string | 项目名称 |
| UseBasicRule | bool | 是否使用基本规则 |
| ExperimentQC | SimpleRule | 实验质控 |
| SampleQC | SimpleRule | 样品质控 |
| ResultRule | SimpleRule | 结果判定 |

### BasicRuleStructure

BasicRuleStructure

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| basicRule | BasicRule | 基本规则 |

### Device

设备

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| SerialNumber | string | 设备编号 |
| DeviceType | DeviceType | 设备类型 |
| ExperimentList | List<RunningBlock> | 正在运行的实验信息 |
| Fast | bool | 设备是否支持Fast控温 |
| UseBlock | byte | （已废弃，转移到RunningBlockList） 设备模块使用状态，每个模块占1位(0：未使用， 1：已使用) |
| PcrFile | string | （已废弃，转移到RunningBlockList） 设备结果保存路径 |

### Dot

点

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| X | double | X |
| Y | double | Y |

### Experiment

实验

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| TargetStd | TargetStd[] | 标准曲线数据 |
| Porjects | Project[] | 实验中的项目列表 |
| Name | string | 实验名称 |
| StartTime | string | 实验开始时间 |
| EndTime | string | 实验开始结束时间 |
| Operator | string | 操作者 |
| Auditor | string | 审核者 |
| Note | string | 备注 |
| PlateNo | string | 版号 |
| BatchNo | string | 批次号 |
| SerialNumber | string | 设备编号 |
| DeviceModel | string | 设备型号 |
| Type | int | 实验类型 |
| UseModule | byte | 使用模块 |
| UseHotBonnet | bool | 是否使用热盖 |
| TemperatureControlMode | int | 控温方式 |
| ScanMode | int | 荧光扫描方式 |
| CustomColumns | string[] | 自定义列名称(与患者信息中的CustomData对应) |
| WellResult | WellResult[] | 反应孔结果 |

### ExperimentBasicInfo

实验基本信息

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Operator | string | 操作者 |
| Auditor | string | 审核者 |
| Note | string | 备注 |
| PlateNo | string | 版号 |
| BatchNo | string | 批次号 |
| CustomizeData | string | 定制信息 |

### ExperimentSetting

实验设置；属性不设置时，采用默认值。

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| PlateFile | string | 孔板导入文件路径（CSV文件），与PlateContent两者必须选择一项赋值 |
| PlateFlie | string | 孔板导入文件路径（CSV文件），与PlateContent两者必须选择一项赋值 已废弃使用PlateFile替代 |
| PlateContent | string | 孔板导入文件内容（CSV文件内容），与PlateFile两者必须选择一项赋值 |
| Operator | string | 操作者 |
| Auditor | string | 审核者 |
| Note | string | 备注 |
| PlateNo | string | 孔板编号 |
| BatchNo | string | 批次号 |
| UseHotBonnet | bool | 使用热盖 |
| Block | byte | 使用模块 |
| Name | string | 文件名称 |
| ScanMode | ChannelScanMode | 通道扫描模式 |
| TemperatureControlMode | TemperatureControlMode | 温度控制方式 |
| FilePath | string | 实验保存路径 |
| CustomizeData | string | 客户定制信息json字符串 |

### ExternalStd

外部标准曲线

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| FileNmae | string | 外部标准曲线文件名称 |
| TargetStd | TargetStd | 标准曲线数据 |

### MidValue

中间结果

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Name | string | 名称 |
| Value | string | 内容 |

### PatientInfo

患者信息

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| ID | string | 唯一标示 |
| Name | string | 姓名 |
| Sex | string | 性别 |
| Age | string | 年龄 |
| Specimen | string | 标本类型 |
| SpecimenDate | string | 采样日期 |
| Doctor | string | 送检医生 |
| Office | string | 送检科室 |
| Diagnosis | string | 诊断 |
| CaseID | string | 病历号 |
| BedID | string | 病床号 |
| InPatientID | string | 住院号 |
| OutPatientID | string | 门诊号 |
| Remark | string | 备注 |
| CustomData | string[] | 自定义 |

### Peak

熔解峰

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Tm | double | 熔解峰Tm |
| Rm | double | 熔解峰相对高度Rm |
| Wm | double | 熔解峰宽度Wm |
| Ym | double | 熔解峰绝对高度Ym |

### Program

PCR程序

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Sections | Section[] | 程序段列表，需要根据实际情况创建不同的程序段 扩增程序段：SectionAMP 熔解程序段：SectionMelt |

### Project

项目

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Name | string | 项目名称 |
| Type | ExperimentType | 实验类型 |
| AMPAlgorithm | EAMPAlgorithm | 扩增曲线算法 |
| Reaction | int | 反应体系 |
| Unit | string | 单位 |
| Note | string | 备注 |
| TubeSortByColumn | bool | 排序方式 True：竖排 False：横排 |
| Targets | Target[] | 目标列表 |
| Program | Program | PCR程序段 |
| CorssTalk | double[][] | 交叉干扰 |
| Rule | string | 高级规则文件路径 |
| BasicRule | BasicRule | 基础规则 |

### RuleItem

规则项

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| SampleType | SampleType | 样本类型 |
| IsAnyTarget | bool | 是否为任一目标 |
| TargetName | string | 检测目标 |
| LeftValue | double | 左端数值 |
| LeftOperator | EOperator | 左端比较符 |
| CalcResult | ECalcResult | 数据类型 |
| RightOperator | EOperator | 右端比较符 |
| RightValue | double | 右端数值 |
| Content | string | 内容 |
| Color | string | 显示颜色 默认黑色 |

### RunExperimentResponse

运行实验响应

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Error | RunExperimentError | 错误代码 |
| SavePath | string | 文件保存路径 |

### RunningBlock

正在运行的模块

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Block | byte | 设备运行模块，每个模块占1位(0：未使用， 1：已使用) |
| PcrFile | string | 设备运行模块结果保存路径 |

### RunningInformation

运行信息

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| State | RunningState | 运行状态 |
| Code | RunningError | 运行错误信息 |
| RemainTime | int | 剩余时间 |
| RunningTime | int | 已经运行时间 |
| Running | string | 正在运行的信息 |
| HotlidTemperature | double | 热盖温度 |
| BlockTemperature | double | 模块温度 |
| TubeTemperature | double | 试管温度 |
| SetTemperature | double | 当前设置温度 |
| Cycle | int | 当前程序段已经运行到第几个循环 |
| HoldTime | int | 当前步骤保持时间 |

### Section

PCR程序-程序段

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Name | string | 名称 |

### SectionAMP

PCR程序-程序段 扩增程序段

父类：`Section`。

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Cycles | int | 循环数 |
| Items | Step[] | 步骤列表 |

### SectionMelt

PCR程序-程序段 熔解程序段

父类：`Section`。

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| ScanMode | EScanMode | 扫描方式 |
| Rate | double | 速率（°C/s） |
| Interval | double | 扫描间隔（°C） |
| HoldTime | int | 保持时间（s） |
| StartTemp | double | 起始温度（°C） |
| StartTempHoldTime | int | 起始温度保持时间 |
| EndTemp | double | 结束温度（°C） |

### SimpleRule

简单规则

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Use | bool | 是否使用 |
| Items | RuleItem[] | 规则项 |

### Step

PCR程序-扩增程序段-步骤

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| HoldTime | int | 保持时间(单位：秒) |
| Name | string | 名称 |
| Temperature | double | 设置温度(°C) |
| IsAcquisition | bool | 是否荧光检测 |
| IsAdvProgram | bool | 是否高级编程 |
| StartCycle | int | 高级编程的起始循环 |
| TempVarValue | double | 温度变化值（°C）（大于0：递增。小于0：递减） |
| TimeVarValue | int | 时间变化值（单位：秒）（大于0：递增。小于0：递减） |
| Ramp | double | 升降温速率(°C/s)（等于0：忽视。大于0：使用） |

### Target

目标

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Name | string | 目标名称 |
| TubeNo | int | 试管编号(从1开始) |
| ChannelNo | int | 通道编号(从1开始) |
| TargetIndex | int | 编号(从1开始) |
| Dye | string | 染料名称 |
| Paramter | TargetParamter | 目标参数，根据实验类型创建不同的参数对象 定性定量：TargetParamter 熔解曲线：TargetParamterMelt |
| Color | string | 颜色 |

### TargetParamter

目标参数

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| QualitativeAnalysis | bool | True：定性分析 False：定量分析 |
| BeginBaseline | int | 基线起点 |
| EndBaseline | int | 基线终点 |
| AutoThreshold | bool | 自动阀值 |
| Threshold | double | 非自动阈值时，手动阀值设置值 |
| OptimizationMode | EOptionzationMode | 基线优化方式 |
| DigitalFilter | EDigitalFilter | 数字滤波 |
| DetectionMin | double | 检测下限(定量分析时有效) |
| DetectionMax | double | 检测上限(定量分析时有效) |

### TargetParamterMelt

目标参数-熔解曲线

父类：`TargetParamter`。

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| MinTemperature | double | 最小温度 |
| MaxTemperature | double | 最大温度 |
| NoiseThreshold | double | 噪声阈值 |
| PeekWidthThreshold | double | 峰宽阈值 |
| PeekHightThreshold | double | 峰高阈值 |
| PeekType | EPeekType | 熔解峰型 |
| MeltPeakAlgorithm | EMeltPeakAlgorithm | 熔解曲线寻峰算法(实验类型为熔解曲线时，设定) |

### TargetResult

目标结果

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Channel | int | 检测通道 |
| Name | string | 检测目标，比如HBV |
| Ct | double | Ct值 |
| CtMean | double | CtMean |
| CtSD | double | CtSD |
| CtCV | double | CtCV |
| Cn | double | Cn |
| CnMean | double | CnMean |
| CnSD | double | CnSD |
| CnCV | double | CnCV |
| Rn | double | Rn |
| RawCurve | double[] | 原始曲线数据 |
| AMPCurve | double[] | 扩增曲线数据 |
| RawMeltCurve | Dot[] | 原始熔解曲线数据 |
| PeakCurve | Dot[] | 峰值曲线数据 |
| PeakValues | Peak[] | 熔解峰数据 |
| TubeName | string | 试管名称 |
| TubeNo | int | 试管编号 |
| Dye | string | 染料名称 |
| Property | string | 属性 |
| ConclusionList | string[] | 目标结论 |
| MidValue | MidValue[] | 中间结果 |
| Color | string | 显示颜色 |

### TargetStd

目标标准曲线

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| ProjectName | string | 检测项目名称 |
| TargetName | string | 检测目标名称 |
| Slope | string | 标准曲线斜率 |
| Intercept | string | 标准曲线截距 |
| r_Value | string | 标准曲线相关系数 |
| Efficiency | string | 扩增效率 |
| StdDot | Dot[] | 标准品数据点列表（X为浓度标称值的LOG值，Y为Ct值） |

### TemperatureDot

温度点

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| RunTemperature | double | 运行温度数据 |
| HotlidTemperature | double | 热盖温度数据 |
| Time | double | 时间(秒) |

### WellResult

反应孔结果

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| ID | string | 反应孔标识，比如：A1、A2等等 |
| SampleWellNames | string[] | 样本所在反应空名称，比如：三管实验[A1,A2,A3] |
| SampleType | SampleType | 样本类型 |
| SampleTypeName | string | 样本类型名称 |
| SampleName | string | 样本名称 |
| CompoundTubeNo | string | 复管编号 |
| PatientInfo | PatientInfo | 样本患者信息 |
| TargetResults | TargetResult[] | 反应孔包含的目标结果 |
| SampleConclusion | string[] | 样本结论 |
| SampleMidValue | MidValue[] | 样本中间结果 |
| WellConclusion | string[] | 孔结论 |
| WellMidValue | MidValue[] | 样本中间结果 |
| ProjectName | string | 项目名称 |

### SlanServerFault

异常信息 运行出现错误时候，要抛出SlanServerFault异常

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| Message | string | 信息 |

## 附录 E 本次静态核验记录

- 当前 DLL 导出类型：65 个。
- 带 WebInvoke 的 HTTP 操作：47 个。
- 260909 → 260921：按公开类型/成员名称、签名及枚举常量比较，新增 1 项、删除 0 项。
- 唯一新增项：`DeviceType.SLAN_16H = 26`。
- C# 示例已使用 .NET Framework 编译器对本包 DLL 进行编译检查；未执行仪器操作。
- 压缩包内厂商发行文件逐一核对 SHA256；结果见包内校验清单。
