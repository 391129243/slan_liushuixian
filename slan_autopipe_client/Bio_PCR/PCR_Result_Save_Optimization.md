# PCR 实验结果保存与上报优化文档

## 📅 修改日期
2026-09-26

## 🎯 优化目标

1. **在 `onSave` 回调中保存本地结果文件**
2. **文件保存到 `slanpcrresult` 目录，永久保留用于回溯**
3. **文件命名唯一性**：基于模板名和时间戳
4. **重试机制**：`getExperimentResult` 和 `saveExperiment` 各重试 3 次
5. **职责分离**：`handleOnSaveExperiment` 负责保存，`handleFinishExperiment` 负责上报

---

## 🔧 主要修改

### 1. 新增 `handleOnSaveExperiment` 方法

**位置**：`SLANModuleManager.java:579-714`

**功能**：在 SDK 的 `onSave` 回调中同步处理实验结果保存

#### 核心逻辑

```java
private void handleOnSaveExperiment(RunningInfo runningInfo) {
    // 1. 重试 3 次获取实验结果
    for (int attempt = 1; attempt <= 3; attempt++) {
        experiment = getExperimentResult(serialNo, mBlock);
        if (experiment != null) break;
        Thread.sleep(2000); // 失败后等待 2 秒
    }
    
    // 2. 生成唯一文件名
    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String fileName = templateName + "_" + timestamp + ".pcr";
    String saveFilePath = "/sdcard/PCR_CONTROL/slanpcrresult/" + fileName;
    
    // 3. 重试 3 次保存实验结果
    for (int attempt = 1; attempt <= 3; attempt++) {
        saveSuccess = saveExperiment(experiment.getName(), saveFilePath, false);
        if (saveSuccess) break;
        Thread.sleep(1000); // 保存失败后立即 sleep 1 秒
        if (attempt < 3) {
            Thread.sleep(2000); // 额外等待 2 秒后重试
        }
    }
    
    // 4. 保存成功后调用上报流程
    if (saveSuccess) {
        experimentFilePath = saveFilePath;
        handleFinishExperiment(experiment);
    } else {
        reportExperimentResultError(runningInfo, "保存实验结果失败");
    }
}
```

#### 重试时间线

```
第 1 次尝试 (t=0s)
  └─ 失败 → sleep 1s → sleep 2s

第 2 次尝试 (t=3s)
  └─ 失败 → sleep 1s → sleep 2s

第 3 次尝试 (t=6s)
  └─ 失败 → sleep 1s → 放弃

总计约 7 秒
```

---

### 2. 优化 `handleFinishExperiment` 方法

**位置**：`SLANModuleManager.java:823-862`

**功能**：只负责上报结果，不再保存文件

#### 优化前

```java
private void handleFinishExperiment(Experiment experiment) {
    // 1. 解析实验开始时间
    String startTimeStr = parseStartTime(experiment);
    
    // 2. 生成保存路径
    String savePath = FileUtils.RESULT_PATH + startTimeStr + "_" + templateName + "/";
    String saveFileName = DateFormatUtil.getFileName() + ".pcr";
    
    // 3. 保存到本地
    boolean savePcrResult = saveResultToPCR(experiment, savePath, saveFileName);
    String filePath = savePath + saveFileName;
    experimentFilePath = filePath;
    
    // 4. 保存成功后上报
    if (savePcrResult) {
        sendFinishMessage(finishInfo, filePath);
    }
    
    // 5. 清理模板目录
    deleteTemplateDir(currentTemplateDirPath);
}
```

#### 优化后

```java
private void handleFinishExperiment(Experiment experiment) {
    // 1. 校验 finishInfo
    if (finishInfo == null) {
        Log.w(TAG, "handleFinishExperiment: finishInfo 已为 null，跳过");
        return;
    }
    
    // 2. 校验文件路径（onSave 中已保存）
    String filePath = this.experimentFilePath;
    if (filePath == null || filePath.isEmpty()) {
        Log.e(TAG, "handleFinishExperiment: experimentFilePath 为空，无法上报");
        return;
    }
    
    // 3. 验证文件是否存在
    File resultFile = new File(filePath);
    if (!resultFile.exists()) {
        Log.e(TAG, "handleFinishExperiment: 结果文件不存在: " + filePath);
        return;
    }
    
    // 4. 上报实验结果给 server
    sendFinishMessage(finishInfo, filePath);
    
    // 5. 清理模板目录
    deleteTemplateDir(currentTemplateDirPath);
}
```

#### 关键改进

- ✅ **不再调用 `saveResultToPCR`**：文件已在 `onSave` 中保存
- ✅ **使用 `experimentFilePath`**：直接使用已保存的文件路径
- ✅ **文件存在性验证**：确保文件真实存在才上报
- ✅ **简化逻辑**：职责单一，只负责上报

---

### 3. `onSave` 回调修改

**位置**：`SLANModuleManager.java:172-176`

#### 修改前

```java
@Override
public void onSave(@NonNull String s, @NonNull int[] ints, @NonNull RunningInfo runningInfo) {
    Log.i(TAG, "===PCR程序运行结束===");
    onExperimentFinish(runningInfo);
}
```

#### 修改后

```java
@Override
public void onSave(@NonNull String s, @NonNull int[] ints, @NonNull RunningInfo runningInfo) {
    // 在 onSave 回调中同步保存实验结果到本地
    handleOnSaveExperiment(runningInfo);
}
```

---

## 📂 文件保存位置

### 原保存位置（废弃）

```
/sdcard/PCR_CONTROL/Result/
└── 2026-09-24-00-07-08_模板名/
    └── 2026-09-24-00-07-08.pcr
```

**问题**：
- 每次实验创建新目录，管理复杂
- 文件名无唯一性保证
- 可能被后续流程删除

### 新保存位置（永久保留）

```
/sdcard/PCR_CONTROL/slanpcrresult/
├── first_20260924_000708.pcr
├── second_20260924_001523.pcr
├── COVID19_20260924_102345.pcr
├── first_20260924_100530.pcr
└── second_20260924_103215.pcr
```

**优势**：
- ✅ 统一目录，便于管理
- ✅ 文件名唯一（模板名 + 时间戳到秒）
- ✅ 永久保留，可用于历史数据回溯
- ✅ 根据 `slanpcrreq` 的模板名命名

---

## 🔄 完整流程图

```
┌─────────────────────────────────────────────────────────────┐
│                    SDK onSave 回调                           │
│                    (实验运行结束)                              │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│            handleOnSaveExperiment (同步处理)                 │
├─────────────────────────────────────────────────────────────┤
│  1. 重试 3 次获取 Experiment                                 │
│     • getExperimentResult(serialNo, mBlock)                 │
│     • 失败：sleep 2s 后重试                                  │
│     • 3 次都失败：报错并退出                                  │
│                                                              │
│  2. 生成唯一文件名                                            │
│     • 格式：模板名_yyyyMMdd_HHmmss.pcr                       │
│     • 例如：first_20260924_000708.pcr                       │
│                                                              │
│  3. 重试 3 次保存到本地                                       │
│     • saveExperiment(name, filePath, false)                 │
│     • 保存目录：/sdcard/PCR_CONTROL/slanpcrresult/          │
│     • 失败：sleep 1s + sleep 2s 后重试                       │
│     • 3 次都失败：报错并退出                                  │
│                                                              │
│  4. 保存成功                                                 │
│     • experimentFilePath = saveFilePath                     │
│     • 调用 handleFinishExperiment(experiment)               │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│         handleFinishExperiment (只上报，不保存)              │
├─────────────────────────────────────────────────────────────┤
│  1. 校验 finishInfo 和 experimentFilePath                    │
│  2. 验证文件是否存在                                          │
│  3. sendFinishMessage(finishInfo, filePath)                 │
│     • 上报实验结果给 server                                   │
│     • 包含结果数据和文件路径                                  │
│  4. deleteTemplateDir(currentTemplateDirPath)               │
│     • 清理临时模板目录                                        │
└─────────────────────────────────────────────────────────────┘
```

---

## 📝 日志输出示例

### 正常流程

```log
00:07:08.837  ===PCR程序运行结束===
00:07:08.837  ==handleOnSaveExperiment 开始处理==
00:07:08.837  handleOnSaveExperiment: 第 1 次尝试获取实验结果
00:07:08.850  handleOnSaveExperiment: 第 1 次获取实验结果成功
00:07:08.850  handleOnSaveExperiment: 准备保存实验结果到: /sdcard/PCR_CONTROL/slanpcrresult/first_20260924_000708.pcr
00:07:08.850  handleOnSaveExperiment: 第 1 次尝试保存实验结果
00:07:09.120  handleOnSaveExperiment: 第 1 次保存实验结果成功: /sdcard/PCR_CONTROL/slanpcrresult/first_20260924_000708.pcr
00:07:09.120  handleOnSaveExperiment: 实验结果已保存，开始处理上报
00:07:09.120  handleFinishExperiment: 使用已保存的结果文件: /sdcard/PCR_CONTROL/slanpcrresult/first_20260924_000708.pcr
00:07:09.120  handleFinishExperiment: 开始上报实验结果给 server
00:07:09.150  发送实验结束的信息和文件 :true
00:07:09.150  成功删除模板目录: /sdcard/PCR_CONTROL/PCR_MODULE_PATH/tempFolder/1727193628000/
```

### 获取失败重试

```log
00:07:08.837  handleOnSaveExperiment: 第 1 次尝试获取实验结果
00:07:08.850  handleOnSaveExperiment: 第 1 次获取实验结果为空
00:07:10.850  handleOnSaveExperiment: 第 2 次尝试获取实验结果
00:07:10.865  handleOnSaveExperiment: 第 2 次获取实验结果成功
...
```

### 保存失败重试

```log
00:07:08.850  handleOnSaveExperiment: 第 1 次尝试保存实验结果
00:07:09.120  handleOnSaveExperiment: 第 1 次保存实验结果失败
00:07:10.120  (sleep 1s)
00:07:12.120  handleOnSaveExperiment: 第 2 次尝试保存实验结果
00:07:12.350  handleOnSaveExperiment: 第 2 次保存实验结果成功
...
```

### 失败上报

```log
00:07:08.837  handleOnSaveExperiment: 第 1 次尝试获取实验结果为空
00:07:10.837  handleOnSaveExperiment: 第 2 次尝试获取实验结果为空
00:07:12.837  handleOnSaveExperiment: 第 3 次尝试获取实验结果为空
00:07:12.837  handleOnSaveExperiment: 重试 3 次后仍无法获取实验结果
00:07:12.837  上报实验结果错误: 获取 Experiment 失败
```

---

## ⚙️ 重试机制详解

### 获取 Experiment 重试

- **重试次数**：3 次
- **重试间隔**：2000ms (2 秒)
- **失败条件**：`experiment == null` 或抛出异常
- **总耗时**：最多 6 秒（3 次尝试，每次间隔 2 秒）

### 保存 Experiment 重试

- **重试次数**：3 次
- **立即延迟**：1000ms (1 秒) - 保存失败后立即 sleep
- **重试间隔**：2000ms (2 秒) - 额外等待后重试
- **失败条件**：`saveSuccess == false` 或抛出异常
- **总耗时**：最多 7 秒
  - 第 1 次失败：sleep 1s + sleep 2s = 3s
  - 第 2 次失败：sleep 1s + sleep 2s = 3s
  - 第 3 次失败：sleep 1s = 1s

---

## 🎯 关键优势

### 1. 职责分离

| 方法 | 职责 | 线程 |
|------|------|------|
| `handleOnSaveExperiment` | 获取和保存实验结果到本地 | SDK 回调线程（同步） |
| `handleFinishExperiment` | 上报结果给 server | SDK 回调线程（同步） |

### 2. 文件管理优化

- **唯一性**：`模板名_时间戳.pcr` 格式，时间戳精确到秒
- **可回溯**：所有结果文件永久保留在 `slanpcrresult` 目录
- **易管理**：统一目录，不再创建多级子目录
- **命名规范**：基于 `slanpcrreq` 的模板名，便于识别

### 3. 容错性增强

- **双重重试**：获取和保存各重试 3 次
- **缓冲机制**：保存失败后 sleep 1 秒缓冲
- **详细日志**：每次尝试都有日志输出，便于排查问题
- **异常捕获**：使用 `Throwable` 捕获 JNI 侧异常

### 4. 性能优化

- **同步处理**：在 `onSave` 回调中完成，避免异步调度开销
- **避免重复保存**：`handleFinishExperiment` 不再保存文件
- **文件验证**：上报前验证文件存在性，避免无效上报

---

## 🔍 问题修复

### 问题 1：多重 PCR 模板混淆

**原因**：所有模板文件保存到同一个 `tempFolder` 目录，`getProjects()` 总是返回第一个文件

**修复**：为每个模板创建带时间戳的独立目录

```java
// 修改前
String saveDirPath = FileUtils.PCR_MODULE_PATH + "/tempFolder/";

// 修改后
String timestamp = String.valueOf(System.currentTimeMillis());
String saveDirPath = FileUtils.PCR_MODULE_PATH + "/tempFolder/" + timestamp + "/";
```

### 问题 2：实验结果获取日志不完整

**原因**：第 1 次尝试没有日志输出

**修复**：增加第 1 次尝试的日志

```java
Log.i(TAG, "handleOnSaveExperiment: 第 " + attempt + " 次尝试获取实验结果");
if (experiment == null) {
    Log.w(TAG, "handleOnSaveExperiment: 第 " + attempt + " 次获取实验结果为空");
}
```

### 问题 3：设备状态检查过于严格

**原因**：`DeviceState.None` 是正常空闲状态，不应视为异常

**修复**：移除对 `DeviceState.None` 的检查（在 `handleOnSaveExperiment` 中不检查设备状态）

---

## 📋 修改文件清单

| 文件 | 修改内容 |
|------|---------|
| `SLANModuleManager.java:172-176` | 修改 `onSave` 回调，调用 `handleOnSaveExperiment` |
| `SLANModuleManager.java:579-714` | 新增 `handleOnSaveExperiment` 方法 |
| `SLANModuleManager.java:823-862` | 优化 `handleFinishExperiment` 方法 |
| `WSSClient.java:208-234` | 为每个模板创建独立目录（修复模板混淆问题） |

---

## 🧪 测试建议

### 1. 正常流程测试

```
1. 发送 PCR 模板请求（first.prj）
2. 启动实验
3. 等待实验结束
4. 检查日志：
   - handleOnSaveExperiment 开始处理
   - 获取实验结果成功
   - 保存实验结果成功
   - 上报实验结果给 server
5. 验证文件：
   - /sdcard/PCR_CONTROL/slanpcrresult/first_yyyyMMdd_HHmmss.pcr 存在
```

### 2. 重试机制测试

```
模拟场景：
- 第 1 次获取失败（返回 null）
- 第 2 次获取成功

检查日志：
- 第 1 次尝试获取实验结果为空
- (等待 2 秒)
- 第 2 次尝试获取实验结果成功
```

### 3. 多重 PCR 测试

```
1. 连续发送不同模板（first.prj, second.prj）
2. 依次启动实验
3. 检查日志：
   - 每个模板保存到独立目录
   - 模板名称正确
4. 验证文件：
   - first_xxx.pcr 和 second_xxx.pcr 文件不同
```

### 4. 文件回溯测试

```
1. 执行多次实验
2. 检查 slanpcrresult 目录
3. 验证：
   - 所有结果文件都保留
   - 文件名唯一（时间戳不同）
   - 可以根据文件名回溯实验记录
```

### 5. 失败上报测试

```
模拟场景：
- 3 次获取都失败或 3 次保存都失败

检查日志：
- 重试 3 次后仍无法获取/保存实验结果
- 上报实验结果错误
```

---

## 📚 相关 API

### SDK API

```java
// 获取实验结果
Experiment getExperimentResult(String serialNo, int[] block)

// 保存实验结果
boolean saveExperiment(String name, String filePath, boolean autoSave)
```

### 参数说明

| 参数 | 类型 | 说明 |
|------|------|------|
| `serialNo` | String | 设备序列号 |
| `block` | int[] | 区块数组 |
| `name` | String | 实验名称（从 Experiment 获取） |
| `filePath` | String | 完整文件路径（含文件名） |
| `autoSave` | boolean | 是否自动保存（传 false） |

---

## 🔗 相关文档

- [SLAN SDK 开发文档](../docs/SLAN_SDK.md)
- [WSS 协议文档](../docs/WSS_Protocol.md)
- [PCR 模板管理](./PCR_Template_Management.md)

---

## 👥 维护者

- **修改人**：Claude Code
- **修改日期**：2026-09-26
- **版本**：v1.0

---

## 📝 备注

1. **兼容性**：此修改向后兼容，不影响其他模块
2. **性能影响**：同步处理增加了 `onSave` 回调的执行时间（最多约 7 秒），但避免了异步调度的复杂性
3. **文件清理**：`slanpcrresult` 目录的文件不会自动清理，需要定期手动清理或增加定时清理任务
4. **后续优化**：
   - 可考虑增加文件大小限制，避免磁盘占满
   - 可考虑增加定时清理任务，删除 N 天前的旧文件
   - 可考虑增加文件压缩功能，节省存储空间

---

**文档结束**
