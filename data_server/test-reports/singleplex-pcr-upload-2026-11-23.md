# 单重扩增 PCR 文件上报测试（2026-11-23）

结论：PASS。用厂商 SDK 离线读取真实扩增文件，按参考 Android 工程的 HTTP 协议模拟上传至实际 Java dataserver，验证结果与附件持久化。未接入设备、未修改原文件、未执行二次分析。

## 文件与数据

- 输入：`result/2026-10-29-16-23-17_06-1029-2-1022-单重-自研-定10-新冠-洗脱100-洗脱排气-21分钟-过转8度/2026-10-29-17-15-07.pcr`。
- 文件大小：240,240 字节。
- SHA-256：`e294c024e1a41a30cb747dd95d4742d3d448049561ea529a21c9bc19123021a7`。
- 厂商读取：9.0.1.260921 发行包的 `Service.SLANService.LoadExperimentResult`，独立 .NET Framework 进程读取私有副本，使用原厂公开初始化方法，没有设备控制调用。
- Type=0，型号 SLAN-8H，实验时间 2026-10-29 16:44:19 至 17:15:07。
- 8 孔 A1–A8，24 条通道结果，每孔 FAM/HEX/CY5 三条；目录名为“单重”，实际模型仍包含三个通道。
- 每条 RawCurve 和 AMPCurve 各 42 点，分别共 1,008 点，熔解曲线为空。
- 21 个有限 Ct，范围 30.904434157694627–39.873582710071155；另有 3 个 SDK Ct 为 Infinity。本次只验证数据传输及存储，不给出检测判定。
- 原文件 Operator 为空，operator_name/operator_user_id 均为空；upload_source=LEGACY_HTTP，没有冒填人员身份。

## Ct 核对

表格为便于阅读显示六位小数。实际上传、入库未舍入，所有有限 Ct 按解析后的数值逐项严格比较。“—”对应 SDK Infinity，传输及入库存 null，没有转为 0，也没有据此推断阴阳性。

| 孔位 | FAM | HEX | CY5 |
| --- | ---: | ---: | ---: |
| A1 | 37.865534 | 39.688512 | 36.472761 |
| A2 | — | — | 36.508819 |
| A3 | 37.348629 | — | 37.182343 |
| A4 | 39.873583 | 37.264670 | 37.176801 |
| A5 | 35.153660 | 35.042180 | 36.119922 |
| A6 | 34.268135 | 34.533863 | 35.722099 |
| A7 | 31.007679 | 31.495737 | 32.730643 |
| A8 | 30.904434 | 31.137310 | 32.512083 |

## 协议与兼容处理

JSON 使用 `POST /upload`、`Content-Type: application/json`，直接提交 Experiment 根对象。附件另以 multipart 的 `file` 字段上传，再通过管理接口显式关联 ReportID/FileID。

C# 模型适配旧 Android 字段：SerialNumber→SerialNo、UseModule→Block、Cn→CnValue、Rn→RnValue、Wm→Width、Ym→YValue。非有限数字字符串显式转换为 JSON null，本文件共转换 171 处，不仅包含 Ct。实际 JSON 为 41,896 字节，有限 Ct 和曲线点没有重算或替换。

未映射的 C# 结构及字符串 Infinity Ct 都被服务端以 HTTP 400 拒绝；规范化后 HTTP 200、Result=1。本轮没有运行原 Android 客户端，模拟通过不代表原客户端的序列化已修复。

## 检查结果

| 检查 | 结果 |
| --- | --- |
| 结果 JSON、孔数、通道结果数 | 通过 |
| 21 个有限 Ct、全部原始及扩增曲线逐项比较 | 通过 |
| 3 个 Infinity Ct 保存为 null | 通过 |
| 完整结果 JSON 深度相等、原文下载一致 | 通过 |
| JSON 重传及 4 个并发重传 | 同一 ReportID，库中仅一条结果 |
| PCR 附件重复上传 | 同一 FileID，库中仅一条文件记录 |
| 显式关联报告与附件 | 通过 |
| 附件下载 SHA-256 | 与原文件一致 |
| 终止测试服务后重启 | 结果、关联、去重及附件下载均通过 |
| 原文件测试前后 SHA-256 | 一致，未修改 |

原熔解文件也完成回归：8 孔、24 条结果、22,032 个原始熔解点、12,024 个峰曲线点，24 个非有限 Ct 保留为空；上报、下载及重启恢复通过。证据见 [熔解回归 JSON](melt-pcr-upload-regression-2026-11-23.json)。

## 测试范围与证据

测试使用随机本机端口与隔离 H2 文件库，不写入默认业务库。关闭 Java 自动托管厂商程序功能，文件提取在独立离线进程完成；未验证真实厂商 HTTP 桥接或设备通信。测试结束已关闭测试 Java 进程。

- ReportID：`e8e804e2-56d3-4cff-8420-a53976c8c403`。
- FileID：`3f2f7be6-a1da-420d-8478-4355f5dd5153`。
- 测试库：`target/singleplex-pcr-upload-20261123/http-1795399955611/db/upload-test.mv.db`。
- 原厂输出：`target/singleplex-pcr-upload-20261123/vendor-experiment.json`。
- 实际上报：`target/singleplex-pcr-upload-20261123/upload-experiment.json`。
- 完整检查及全精度 Ct：[测试 JSON](singleplex-pcr-upload-2026-11-23.json)。

## 复现

先运行 `scripts/read-pcr-offline.ps1`，通过 `-Source`、`-OutputDirectory`、`-VendorDirectory` 指定上述文件、独立输出目录和厂商程序目录，再执行：

```powershell
$sample = Get-ChildItem -LiteralPath result -Filter *.pcr -Recurse -File | Where-Object { $_.FullName -like '*单重*' }
node scripts/simulate-real-pcr-upload.mjs target/singleplex-pcr-upload-20261123 $sample.FullName
```

通用上报脚本现支持第二个位置参数指定原始 PCR 文件，同时兼容扩增与熔解数据。默认熔解样本支持位于项目根目录或新的 result 目录。`target` 为临时输出，Maven clean 会删除；测试概况、Ct 表和 JSON 证据已单独保存在本目录。
