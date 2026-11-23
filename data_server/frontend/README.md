# PCR Studio 数据中心与分析工作台

Vue 3 + TypeScript + Vite + ECharts。新增“数据中心”连接 Java API，实现服务器文件、报告、实际分析结果和版本管理。默认打开的“真实结果验证”仍为离线快照，模拟工作台独立保留。

## 数据中心

在 dataserver 根目录双击 `start-dataserver-offline.bat` 启动真实 SDK 离线后端，再双击 `start-frontend.bat`，访问 http://127.0.0.1:5173/?mode=server 。默认账号 `admin`，密码 `12345678`。如使用正常 SLAN 厂商服务，可改用 `start-dataserver.bat` 启动后端。前端 Vite 将 `/api` 代理到本机 8088。

- 登录身份只保存在页面模块内存，不写 localStorage/sessionStorage、URL 或静态文件，刷新后重新登录。
- 人员上传通过受认证的 POST `/api/admin/files`，最大 32 MiB；文件和实际上传账号保存在服务器。
- 查询报告并明确关联原始 PCR；读取文件后展示 SDK 返回的 Ct、扩增/原始扩增/熔解/峰曲线，按通道筛选。非有限 Ct 显示“—”，不转成 0；坐标点不平滑、不补造。
- 兼容 C# SDK 扩增曲线的 `double[]`（按循环序号绘制）及 Android 的 `X/Y` 对象数组。熔解/峰曲线使用明确温度坐标，不能把缺少温度的数值数组猜成温度曲线。实际文件已验证 24 条扩增曲线，每条 42 点。
- 二次分析支持小数阈值（如 0.24），成功后参数、操作者、结果自动入库。网络异常重试复用 requestId，避免重复分析；未确认的请求可先查询历史。
- 保存新 PCR、下载结果、导出版本 JSON、按原报告查询历史，并可基于已保存输出创建子版本；历史 JSON 和已保存文件不依赖 SDK 在线。
- SDK 会话在桥接重启后失效；历史版本仍可查看，重新分析需再次读取输入文件。
- 设备结果包的 `.ct/.peak/.conclusion` 与 `judgeConclusion.result` 以原始规则快照展示；SDK 重算后由 Java 执行 CT/PEAK 规则，界面分别显示原始附件、逐项新判读和综合结论。
- “按项目和通道配置高级参数”读取真实 SDK 原值，支持扩增算法、基线、自动阈值、定性分析、滤波、熔解峰高/温度范围/算法/峰型。仅修改所选靶标的改动字段，未选靶标保留原值；不支持的熔解字段不显示。
- 规则编辑支持 CT/PEAK 文件、通道、项目及各项边界，先保存独立版本再分析；综合策略可选择 CT 与 PEAK 全部命中或任一命中，缺失数据均未判定。历史版本与原始规则不被覆盖，分析结果包包含新 PCR、有效规则、新判读、综合策略和原始附件。

离线启动器的实际库和文件在 `runtime/offline-server/`，与正常服务库隔离。模拟工作台的浏览器数据不会自动上传。

```powershell
cd E:\PCRinOne\framework\bowei_liushuixian\liushuixian_melody\data_server\frontend
npm.cmd ci --ignore-scripts
npm.cmd run dev
```

浏览器打开 http://127.0.0.1:5173 。仅查看离线快照与模拟工作台时无需启动 Java、数据库或设备。

## 真实结果验证

首页展示 8 孔、24 靶标、22,032 个原始熔解曲线点、12,024 个峰曲线点和 21 个峰；支持孔位/通道筛选、原始信号/峰曲线切换、Tm 表格、验证清单、来源哈希和筛选结果 JSON 导出。Ct 无有效值时显示“—”，不显示为 0。

`public/reference/melt-result.json` 由 `scripts/prepare-reference.mjs` 从真实文件上报测试产物生成。只保留孔位、通道、曲线和峰值，不包含患者、样本身份、服务器路径或账号信息；所有曲线坐标完整保留。它是离线快照，不是实时数据库查询，也不表示已提供真实在线复算功能。

有本机测试产物时可从 dataserver 目录重新生成：`node frontend/scripts/prepare-reference.mjs`。生成前会核对测试 PASS 状态和曲线点数。

直接打开 `http://127.0.0.1:5173/?mode=mock` 可进入原模拟工作台。

## 模拟工作台功能

- 实验工作台：演示曲线、孔位选择、六通道显隐、初始数据对比、结果表及文件信息。
- 文件读取：选择/拖入非空 .pcr，最大 32 MiB，只读取浏览器提供的文件名和大小，然后加载模拟数据；不会解码或上传二进制。
- 模拟二次分析：自动/手动基线、阈值调整、交点插值 Ct；仅验证界面数据流，不是厂家算法或医学结果。
- 文件库：搜索、切换模拟实验。
- 分析记录：最近 50 个模拟版本保存到当前浏览器 localStorage，刷新后可回看。
- 导出 JSON：后缀为 `.analysis.json`，包含 mock 标记与模拟提示。不是 SDK 可读取的 .pcr。
- 保存 .pcr：预留入口，展示待 C# SDK 接入的说明，不制造伪文件、不覆盖源文件。

模拟工作台中引用同名文件的演示曲线仍为人工配置，不代表真实熔解实验。真实文件的 SDK 读取结果只在独立的“真实结果验证”入口展示。原始 PCR 二进制没有复制到 Web 静态目录。

`src/services.ts` 继续负责独立模拟工作台；`src/server-api.ts` 和 `DataCenterView.vue` 负责数据中心的实际 API。二者不共用结果缓存，不能将模拟记录当作服务器版本。实际 SDK 离线读取、复算、保存、重新打开和完整浏览器流程已验证，证据见 `test-reports/data-center-live-2026-11-23.json`。

## 构建与验证

```powershell
npm.cmd run build
npm.cmd run test:e2e
```

构建产物位于 `dist`。浏览器测试使用已安装 Chrome，默认路径 `C:/Program Files/Google/Chrome/Application/chrome.exe`；其他电脑可以设置环境变量 `PLAYWRIGHT_CHROME`。11 项测试在 5174 自动启动临时 Vite，涵盖登录、上传、规则编辑/版本/综合结论、高级参数、保存下载、requestId 重试、密码不落浏览器存储和移动端布局；其中数据中心 API 使用测试夹具。原模拟/快照测试另确认不发起后端请求。实际 Java/C#/SDK 验收时，在离线后端和 5173 前端启动后，从 dataserver 根目录运行 `node scripts/verify-result-bundle-ui.mjs --advanced`。

开发代理 `/api` 指向 `127.0.0.1:8088`，但 mock 服务不使用代理。生产静态页面需由独立 Web 服务承载，当前 Java JAR 未打包 Vue 静态资源。
