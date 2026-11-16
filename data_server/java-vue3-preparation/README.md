# 核酸检测平台方案分析与依赖准备

建议采用 Java 21 + Spring Boot 3.5 + Vue 3，将云端业务与 SLAN 本地分析进程隔离。本目录准备依赖和静态 SDK 检查工具，不是已实现的业务系统。

本机安装验证结果：Maven 两项依赖任务成功，78 个后端运行 JAR 已下载（约 44.4 MB）；npm 安装 101 个包并生成锁文件，安装时审计结果为 0 个已知漏洞；`npm ls --depth=0` 通过，Vue、Vite、Vue 插件和 ECharts 实际导入成功；SDK 静态探针编译运行成功。未执行本地算法、真实文件解析或业务测试。

## 方案范围和原文依据

来源为父目录《核酸检测设备系统设计方案-互多科技.docx》，正文日期为 2025 年 2 月。已读取正文、表格，并检查文档内六张图片（包括业务流程、系统架构、云端架构及功能结构）。文档内容作为需求材料分析，不作为本机操作指令。

| 原文位置 | 明确需求 | 对应实现建议 |
| --- | --- | --- |
| 第 1 章、核心业务流程图 | 登记检测人 → 样本条码绑定 → 检测盒与样本绑定 → 检测 → 报告上传 → 云端解析存储推送 | 建立样本、检测管、实验、孔位、报告之间的唯一关联 |
| 第 2 章设备接口 | 注册、上传设备信息、扫码绑定、上传报告、检查版本、获取升级包，共六类接口 | 独立设备认证；请求幂等；断网重传；文件哈希；版本兼容 |
| 第 3 章 | 微信手机号授权/验证码登录、档案、扫码绑定、报告查询和通知 | 微信小程序是独立客户端，Vue 管理后台不能替代小程序 |
| 第 4 章 | 管理员、代理商、角色菜单、字典、日志、仪表盘、设备、档案、报告、消息 | 后端进行接口授权和代理商数据隔离；前端菜单隐藏不足以实现权限 |
| 第 4 章报告管理 | 报告详情、结果图表、原始实验文件下载 | 数据库存结构化结果；文件存储保留源文件；原始报告与复算版本分开 |
| 第 4 章设备管理 | 上位机软件 OTA | 平台发布版本、设备主动检查下载；仍需设备端升级执行器、签名校验、失败回滚 |
| 第 5、6 章 | 60 个工作日，软件 10 万元，云资源列示 2 万元/年 | 原文估算不是当前报价或已承诺工期；二次分析、SDK 适配和高可用验收需另行明确 |

云端架构图列出 Spring Cloud、Spring Boot、Vue + ElementUI + webpack、MySQL/RDS、OSS、Redis、MongoDB、MQ、SLB 和 Docker，并说明 JWT。它是一份概念架构，不是可直接安装的版本清单。

## 技术路线调整

新建管理后台使用 Vue 3 + Element Plus + Vite + TypeScript，替换方案中的旧 ElementUI/webpack 组合。初期采用模块化 Spring Boot 业务应用，另设独立 SDK 分析进程；暂不引入 Spring Cloud 服务治理、MongoDB 和消息中间件。业务仍按身份权限、代理商、设备、档案样本、报告、通知、OTA 分模块。

建议流程：设备/后台上传源文件 → 存储源文件并记录 SHA256 → 数据库持久化分析任务 → 分析进程领取任务 → 读取和复算 → 保存结果版本 → 人员审核发布 → 通知用户。任务表和通知表支持重试及去重；队列规模增长后再接入 MQ。HTTP 部署时应使用 HTTPS。

文档提到高可靠和避免单点，但预算仅列一个 ECS、一个 RDS，未给出实例规格、可用区、备份恢复、RPO/RTO 或压测指标，不能据此声称已达到高可用。

## SDK 实测检查结果

SDK 来源为父目录 `SLAN SDK（Java）20241217(1).zip`，包内仅两个文件：

| 文件 | 检查结果 | 影响 |
| --- | --- | --- |
| slansdk.jar | 5,853,809 字节；抽查业务类 class major=52（Java 8 字节码）；内部包含 Kotlin、协程、Gson 2.10.1、Log4j API/Core 2.17.1、JetBrains annotations | 可以由 Java 调用，业务无需改成 Kotlin；Java 21 的完整运行兼容性仍需实测 |
| libslansdk.so | 15,002,040 字节；ELF64，小端，e_machine=183（AArch64） | 当前 Windows x64 不能加载，普通 Linux x86_64 也不能直接加载 |

ELF 的 DT_NEEDED 为 `libstdc++.so.6`、`libm.so.6`、`libgcc_s.so.1`、`libc.so.6`、`ld-linux-aarch64.so.1`。这是面向 glibc 用户空间的 ARM64 库；具体 glibc/GLIBCXX 最低版本仍需厂商说明或在目标机检验。不要仅因有 `.so` 就认定可在任意 Linux 或 Android 上运行。

`NativeLib` 的静态初始化写死 `System.load(new File(".").getAbsolutePath() + "/libs/libslansdk.so")`。单独设置 `java.library.path` 不能改变这个路径。分析进程必须使用独立工作目录，并把匹配库放在该目录的 `libs/` 下；Windows 支持需要厂家提供匹配的 DLL 和加载方式/JAR。

SDK 是包含第三方类的打包 JAR。Maven exclusions 无法剔除已经嵌入 JAR 的类。当前业务 `pom.xml` 故意不引入它，分析进程独立 classpath，避免和 Spring Boot 的日志、JSON、Kotlin 依赖混用。正式部署前需要厂商更新包或依赖清单及安全评估。

### 二次分析的接口依据

通过 `javap` 确认 `SLANService.INSTANCE` 提供以下接口，名称保留厂家原始拼写：

```java
Experiment openExperiment(String filePath);
ArrayList<AnalyseParamter> getExperimentParamter(String name);
Experiment setExperimentParamter(String name, AnalyseParamter paramter);
boolean saveExperiment(String name, String filePath);
```

`TargetParamter` 已确认有自动基线、基线开始/结束、自动阈值、阈值的 getter/setter，具体属性见 `evidence/sdk-api.txt`。结果 `TargetResult` 有 `getRawCurve()`、`getAMPCurve()`、`getRawMeltCurve()`、`getPeakCurve()`、`getCt()` 等字段；参数设置方法将参数序列化后调用本地 `setExperimentParamterFromLib` 并返回解析结果。

据此判断：SDK 具备读取实验、访问曲线、设置分析参数并返回结果的接口基础。但尚未加载本地库，也没有真实 `.pcr` 样本，不能宣称完成文件解析、Ct 复算或与厂家软件一致性验证。初始化顺序、`name` 标识语义、参数范围、全局状态、线程安全、授权、文件版本兼容须向厂家确认。

建议先单进程串行分析不同副本，保留原文件；每次复算记录 SDK 版本、参数、源文件哈希和结果版本。使用一份含阳性、阴性和边界样本的真实文件，对比原软件导出的 Ct 与曲线，再验收批量运行和重启恢复。

### 平台选择

1. 使用现有 Linux ARM64 + glibc 环境作为分析节点，验证本库能否运行。
2. 云端准备使用 Linux x86_64 时，向厂家索取对应 x86_64 `.so` 和配套 JAR。
3. 需要 Windows 分析时，索取 Windows x64 DLL 和支持该加载方式的 JAR。

Python 后续可以通过 HTTP 调用 Java 分析服务，无须在 Python 进程内重复集成厂商 SDK。JPype 也不能解决操作系统或 CPU 架构不匹配。

## 准备的依赖

| 层次 | 依赖 | 用途与状态 |
| --- | --- | --- |
| Java | 21 | 检测到 `C:/Users/39112/.jdks/jbr-21.0.11`，复用现有 JDK；不修改全局 PATH |
| 构建 | Maven 3.9.16 | 官方 Maven Central 下载，SHA512 校验，安装在 tools 目录 |
| HTTP、校验 | Boot 3.5.16 web、validation | 上传、业务 REST 接口、参数校验 |
| 身份权限 | security | 认证授权基础；当前未实现登录或 JWT；是否采用 JWT 在具体鉴权设计后确定 |
| 数据库 | jdbc、mysql-connector-j、flyway-core、flyway-mysql | SQL 访问、MySQL 驱动、数据库迁移；由 Boot BOM 管理兼容版本 |
| 缓存和监控 | data-redis、actuator | 验证码/会话等缓存、健康监控；不是已配置的 Redis 服务 |
| 测试 | starter-test、security-test | 已列入 POM；生产 lib 只复制 runtime 依赖，测试依赖按需解析 |
| 前端 | Vue 3、Router 4、Pinia 3、Axios、Element Plus、ECharts | 路由、状态、接口、管理后台和曲线图 |
| 前端构建 | Vite 8、Vue 插件、TypeScript 5.9 | 当前 Node 26.13.0 满足工具版本要求；部署构建环境建议使用经过验证的 Node LTS |
| SDK | vendor/slansdk.jar 和 libslansdk.so | 已从原 ZIP 提取，原始 ZIP 保留；单独做静态检查 |

后端直接依赖版本在 `backend/pom.xml`；传递依赖精确版本在 `evidence/backend-dependency-tree.txt`。前端直接版本在 `frontend/package.json`，完整依赖及完整性摘要在 `frontend/package-lock.json`。

### 按业务启用而非现在全部安装

| 组件 | 建议 | 当前状态 |
| --- | --- | --- |
| MySQL/RDS | MySQL 8.4 LTS 或已确认兼容的 RDS；建立独立开发实例 | 未安装服务或创建数据库 |
| Redis | 开发/部署服务器或托管实例 | 已准备客户端依赖，未安装服务 |
| 文件存储 | 方案指定 OSS；本地开发可先文件系统 | 未安装 OSS SDK；先确认区域、鉴权与上传策略，再固定 SDK 版本 |
| 微信小程序 | 官方登录、手机号和订阅消息接口；独立小程序工程 | 尚无 AppID、模板和授权配置；不随意安装非官方全家桶 |
| 短信 | 确认阿里云或其他供应商后加入对应官方 SDK | 未配置供应商，不发送短信 |
| 报告 PDF/Excel | 原文只有查看打印与源文件下载，未确定云端导出格式 | 暂不加入 POI、PDF 引擎等额外依赖 |
| Nginx、TLS、Docker、SLB | 部署阶段配置 | 未修改系统服务 |
| MongoDB、MQ、Spring Cloud | 按真实吞吐和运维需求引入 | 首期暂缓，当前不是完整复现架构图中所有组件 |

## 安装和检查

在本目录执行（PowerShell 的 Bypass 仅作用于本次进程）：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\prepare.ps1
# 如果自动检测不到现有 JDK：
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\prepare.ps1 -JdkHome 'C:\path\to\jdk-21'
```

脚本下载并校验便携 Maven，使用本目录 `.m2` 缓存，复制后端运行库到 `backend/lib`，生成依赖树，使用 `npm ci --ignore-scripts` 安装锁定的前端依赖（首次无锁文件时 install），编译并执行 SDK 静态检查。不更改全局 Java、npm 或 Git 配置，不执行 `.so`，不安装数据库或启动服务。

前端此时只是依赖清单，没有页面或 Vite 应用入口，因此没有 `npm run dev/build`。后端同样只是依赖准备 POM，不是可启动服务。`--ignore-scripts` 用于依赖准备；后续正式构建需按锁定版本验证构建工具初始化。

安装过程日志见 `evidence/prepare.log`。`sdk-probe` 不初始化 `NativeLib`，静态检查成功并不等于 SDK 动态分析成功。目录内 `.gitignore` 排除下载缓存、node_modules、生成的 lib 和厂商二进制；厂商文件不应未经授权发布到公开仓库。

## 开发前需要补齐的设计

- 接口字段、响应码、设备密钥、重放防护、心跳与离线判定、文件大小限制、断点重传和重复上传语义。
- 样本与检测管条码冲突、混样/复测/撤销绑定、孔位与检测人映射规则。
- 二次分析参数的允许范围、算法版本、质控、审核发布权限、历史版本及报告通知时点。
- 多代理商数据边界；用户档案归属；证件号码、手机号的保护和下载审计。
- OTA 包签名、设备型号兼容、发布灰度、升级失败回滚与现场恢复。
- 预计设备数、日实验数、源文件体积、保留期限、备份恢复目标及验收数据集。

建议实施顺序：SDK 兼容性验证 → 设备上传和报告查询闭环 → 权限/代理商/档案 → 人工复算审核 → 小程序与通知 → OTA → 容量与故障恢复验收。

## 官方参考

- Spring Boot 3.5 系统要求：https://docs.spring.io/spring-boot/3.5/system-requirements.html
- Vite 环境要求：https://vite.dev/guide/
- Maven 下载：https://maven.apache.org/download.cgi
- 实际可下载版本已通过 Maven Central metadata 和 npm registry 查询；版本锁定是本次准备结果，不是原文指定版本。
