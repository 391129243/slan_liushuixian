# PCR Studio 模拟工作台

Vue 3 + TypeScript + Vite + ECharts。默认完全独立运行，不访问设备、Java API 或 C# 服务。

```powershell
cd E:\PCRinOne\framework\bowei_liushuixian\liushuixian_melody\data_server\frontend
npm.cmd ci --ignore-scripts
npm.cmd run dev
```

浏览器打开 http://127.0.0.1:5173 。无需启动 Java、数据库或设备。

## 功能

- 实验工作台：演示曲线、孔位选择、六通道显隐、初始数据对比、结果表及文件信息。
- 文件读取：选择/拖入非空 .pcr，最大 32 MiB，只读取浏览器提供的文件名和大小，然后加载模拟数据；不会解码或上传二进制。
- 模拟二次分析：自动/手动基线、阈值调整、交点插值 Ct；仅验证界面数据流，不是厂家算法或医学结果。
- 文件库：搜索、切换模拟实验。
- 分析记录：最近 50 个模拟版本保存到当前浏览器 localStorage，刷新后可回看。
- 导出 JSON：后缀为 `.analysis.json`，包含 mock 标记与模拟提示。不是 SDK 可读取的 .pcr。
- 保存 .pcr：预留入口，展示待 C# SDK 接入的说明，不制造伪文件、不覆盖源文件。

参考目录 `AIO_AAA_2026-11-16-11-01-01_07-自研试剂ab管多重测试_20261020` 中实际文件 `2026-11-16-13-04-40.pcr` 为 5,736,758 字节；文件名、大小和 SHA256 已核验，曲线、孔数、通道和 Ct 均为人工演示配置，不是从文件中解析的事实。原始文件没有复制到 Web 静态目录。

`src/services.ts` 定义 `PcrAnalysisService`，页面仍使用显式 mock 实现；`pcrApi` 为真实调用路径。现已收到宏石 SDK 资料，C# 桥接完成离线 HTTP 适配，但默认关闭，尚未验证厂商实际文件读取、分析和保存。替换页面服务前需完成真实文件及分析一致性验证，不能只删除“模拟”标签来充当真实接入。

## 构建与验证

```powershell
npm.cmd run build
npm.cmd run test:e2e
```

构建产物位于 `dist`。浏览器测试使用已安装 Chrome，默认路径 `C:/Program Files/Google/Chrome/Application/chrome.exe`；其他电脑可以设置环境变量 `PLAYWRIGHT_CHROME`。测试会在 5174 端口自动启动临时 Vite，完成后关闭，验证参数改变 Ct、孔位筛选、版本留存、JSON 导出、模拟文件读取、错误输入、移动端无横向溢出，并确认不发起后端 API 请求。

开发代理 `/api` 指向 `127.0.0.1:8088`，但 mock 服务不使用代理。生产静态页面需由独立 Web 服务承载，当前 Java JAR 未打包 Vue 静态资源。
