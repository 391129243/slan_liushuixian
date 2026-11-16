# 宏石 SLAN 离线分析桥接

已根据本次提供的 SLAN 9.0.1.260921 API 文档实现离线 HTTP 适配。默认仍关闭真实调用，Vue 页面继续模拟，不启动厂商程序，不连接设备。

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

## 默认模拟模式

```powershell
cd E:\PCRinOne\framework\bowei_liushuixian\liushuixian_melody\data_server
# 无需运行厂商服务；PCR_VENDOR_ENABLED 缺省为 false
 dotnet run --project .\csharp\PcrSdkBridge
```

C# 默认监听 `http://127.0.0.1:5099`。能力接口返回 `vendorAdapterImplemented=true`、`sdkReady=false`，操作返回 501。前端模拟按钮不调用它，因此独立页面不要求启动任何后端。

## 后续在独立离线环境启用

以下仅为配置说明，本次没有执行启用或启动厂商服务。需先由维护者确认厂商运行目录、依赖、权限、授权及独立服务实例已准备好；不要使用正在承载现场设备实验的共享服务。

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

读取：`{"fileId":"已上传文件的ID"}`。Java 解析其可信存储路径，再传递 `{fileId,fileName,sourcePath}`。C# 只允许 PCR_INPUT_ROOT 下的现有普通文件，拒绝软链接/重解析点；先复制为工作区 source.pcr，再交给厂商读取。输入大小限制 32 MiB，不需要依赖存储文件的 .bin 后缀判断内容。

二次分析：

```json
{"experimentId":"读取返回的句柄","parameters":{"autoBaseline":true,"baselineStart":3,"baselineEnd":15,"threshold":300}}
```

当前参数统一应用到实验的所有项目和靶标，尚未提供选孔或选通道修改接口。先获取原始完整 AnalyseParamter，保留 `__type`、BasicRule、滤波、交叉干扰、算法及熔解相关字段，只改：

- BeginBaseline / EndBaseline：基线起止。
- OptimizationMode：autoBaseline=true 映射 AUTO=1，false 映射 MANUAL=2；SDK 没有名为 AutoBaseline 的布尔字段。
- AutoThreshold=false、Threshold：使用传入手动阈值。

服务端保留现有演示请求的参数范围（基线 1–20，阈值 10–3000），这不是厂家支持范围的完整声明。HTTP 外层 `方法名Result` 会剥离，必须先检查 ErrorCode=0；保存、关闭和参数设置还检查 Result=true。

保存：`{"analysisId":"复算返回的句柄","fileName":"result.pcr"}`。厂商 SaveExperiment 只接受实验名，不能传输出路径。因此分析时在新的工作副本上 Open/Save；对外保存仅导出该副本。Java 拉取已保存文件，校验 SHA256，再生成下载 FileID。不会将 JSON 改后缀充当 PCR，也不会显式向厂商发送原文件路径。

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

已通过 C# 编译、Java 测试及协议替身测试：真实 Java/C# 进程调用模拟的厂商 HTTP 服务器，覆盖读取、全参数保留、复算、保存、Java 文件入库与下载、校验、源文件不变、错误时关闭自有实验、同名冲突及路径拒绝。测试文件为明确标记的假字节，只用于协议与存储验证。

本次未启动 SLAN.WCFServer.exe，未连接设备，未由厂家 SDK 解析真实 `.pcr`，未验证真实 Ct、曲线或保存文件能被厂家软件再次打开。原参考文件 SHA256 仍应为 `577afff986549e59d29f611358a776b5e1c0439bc34d4504cbda7889724031e2`。
