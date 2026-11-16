# 日志系统改造计划

## 目标

参考 6in1 项目的 v4.1 日志系统变更，对当前项目进行如下改造：

1. **目录结构调整**：将日志从分散目录统一到日期目录结构
2. **取消 ARM 日志存储**：因为 logcat 已包含相同内容，避免重复
3. **保留 PCR/RNA 日志**：继续支持但迁移到新的目录结构
4. **添加 14 天自动清理**：基于日期目录的过期清理机制
5. **优化 logcat 文件切换**：改进文件大小和日期切换逻辑

## 现状分析

### 当前目录结构
```
/sdcard/Log/
├── LOGCAT/                    # logcat 日志（独立目录）
│   └── 2026-09-06-14-30-00.log
├── ARM/                       # ARM 日志（独立目录，需删除）
│   └── 2026-09-06-14-30-00.log
├── PCR/                       # PCR 日志（独立目录）
│   └── 2026-09-06-14-30-00.log
└── RNA/                       # RNA 日志（独立目录）
    └── 2026-09-06-14-30-00.log
```

### 目标目录结构（参考 v4.1）
```
/sdcard/Log/
└── 20260906/                  # 日期作为最外层目录
    ├── logcat/                # logcat 归入子目录
    │   └── 20260906-143000.log
    ├── PCR/                   # PCR 日志归入日期下
    │   └── 20260906-143000.log
    └── RNA/                   # RNA 日志归入日期下
        └── 20260906-143000.log
    # ARM/ 不再存在 ✅
```

### 代码层面

**LogcatHelper.java** 包含：
- 主 logcat 收集（LogDumper 线程）
- PCR/RNA/ARM 三种日志的 appendlog 机制（LogAPL 线程）
- 旧的文件过期清理逻辑（基于单个文件，不是基于日期目录）

**调用方**：
- `APLService.java` - onEvent() 调用 appendlog
- `PipeMainActivity.java` - onEventMainThread() 调用 appendlog

**常量定义**：
- `ConstantData.java` - 定义 LOG_RNA、LOG_PCR、LOG_ARM

## 实施方案

### 阶段 1：LogcatHelper.java 改造

#### 1.1 目录结构调整

**变更点**：
- `PATH_LOGCAT`：从 `/Log/LOGCAT` 改为 `/Log/`
- 移除：`PATH_LOG_ARM`（整个变量）
- 调整：`PATH_LOG_PCR` 和 `PATH_LOG_RNA` 改为动态生成（基于日期）
- 新增：`getTodayLogDir(moduleName)` 方法，返回 `/Log/yyyyMMdd/moduleName/`

```java
// 改造前
PATH_LOGCAT = .../Log/LOGCAT
PATH_LOG_ARM = .../Log/ARM
PATH_LOG_PCR = .../Log/PCR  
PATH_LOG_RNA = .../Log/RNA

// 改造后
PATH_LOGCAT = .../Log/  // 根目录
// 不再需要静态的 PATH_LOG_ARM/PCR/RNA

private String getTodayLogDir(String moduleName) {
    String today = dayFormat.format(new Date());
    return PATH_LOGCAT + today + File.separator + moduleName + File.separator;
}
```

#### 1.2 Logcat 子目录调整

**LogDumper 线程改造**：
- `getTodayLogDir()` 方法：返回 `/Log/yyyyMMdd/logcat/`
- 文件切换逻辑：检查日期变化 + 文件大小（参考 6in1 的 20MB 限制）
- 文件名格式：从 `yyyy-MM-dd-HH-mm-ss.log` 改为 `yyyyMMdd-HHmmss.log`

```java
private String getTodayLogDir() {
    String today = dayFormat.format(new Date());
    return PATH_LOGCAT + today + File.separator + "logcat" + File.separator;
}
```

#### 1.3 移除 ARM 日志支持

**删除**：
- 常量：`LOG_ARM = 3`
- 字段：`PATH_LOG_ARM`
- 字段：`logAPL_arm`
- `appendlog()` 中的 `case LOG_ARM` 分支
- `init()` 中创建 ARM 目录的代码块

#### 1.4 PCR/RNA 日志迁移到日期目录

**LogAPL 线程改造**：
- 构造函数接收 `moduleName` 参数（"PCR" 或 "RNA"）
- 使用 `getTodayLogDir(moduleName)` 动态获取目录
- 日期切换时自动切换到新日期目录

```java
// appendlog() 改造
case LOG_PCR:
    if (logAPL_pcr == null) {
        logAPL_pcr = new LogAPL("PCR");  // 传入模块名
        logAPL_pcr.start();
    }
    logAPL_pcr.appendContent(text + " \n");
    break;
case LOG_RNA:
    if (logAPL_rna == null) {
        logAPL_rna = new LogAPL("RNA");
        logAPL_rna.start();
    }
    logAPL_rna.appendContent(text + " \n");
    break;
// LOG_ARM 分支删除
```

#### 1.5 日期目录清理机制

**参考 6in1 实现**：
- `deleteExpiredLogDirs()` 方法：扫描 `/Log/` 下的 `yyyyMMdd` 目录
- 删除超过 14 天的整个日期目录（包含其下所有子目录）
- 在日期切换时触发清理
- 提供 `cleanOldLogs()` 公开方法供外部调用

```java
private void deleteExpiredLogDirs() {
    try {
        File logRoot = new File(PATH_LOGCAT);
        if (!logRoot.exists() || !logRoot.isDirectory()) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -SDCARD_LOG_FILE_SAVE_DAYS);
        Date expireDate = calendar.getTime();

        File[] dayDirs = logRoot.listFiles();
        if (dayDirs == null) return;

        for (File dayDir : dayDirs) {
            if (!dayDir.isDirectory()) continue;
            
            String dirName = dayDir.getName();
            // 检查目录名是否为日期格式 yyyyMMdd
            if (dirName.length() == 8 && dirName.matches("\\d{8}")) {
                try {
                    Date dirDate = dayFormat.parse(dirName);
                    if (dirDate.before(expireDate)) {
                        deleteDirectory(dayDir);
                        Log.i("LogcatHelper", "删除过期日志目录: " + dirName);
                    }
                } catch (ParseException e) {
                    Log.e("LogcatHelper", "解析日志目录日期失败: " + dirName);
                }
            }
        }
    } catch (Exception e) {
        Log.e("LogcatHelper", "删除过期日志目录失败: " + e.getMessage());
    }
}

private void deleteDirectory(File directory) {
    if (directory == null || !directory.exists()) return;
    
    if (directory.isDirectory()) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteDirectory(file);
            }
        }
    }
    directory.delete();
}

public void cleanOldLogs() {
    deleteExpiredLogDirs();
}
```

#### 1.6 文件大小管理优化

**当前问题**：
- 文件大小限制 100MB 太大
- LogDumper 和 LogAPL 都使用相同的 MAXFileSize

**改进**：
- logcat 文件限制：20MB（参考 6in1）
- PCR/RNA 文件限制：保持 100MB（考虑到模块日志可能较大）
- 使用不同的常量区分

```java
private final long MAX_LOGCAT_FILE_SIZE = 20 * 1024 * 1024;  // 20MB for logcat
private final long MAX_MODULE_FILE_SIZE = 100 * 1024 * 1024; // 100MB for PCR/RNA
```

### 阶段 2：调用方调整

#### 2.1 ConstantData.java

**删除**：
```java
public final static int LOG_ARM = 3;  // 整行删除
```

**保留**：
```java
public final static int LOG_RNA = 1;  // 保留
public final static int LOG_PCR = 2;  // 保留
```

#### 2.2 APLService.java

**删除** `case LOG_ARM` 分支：
```java
@Subscribe(threadMode = ThreadMode.MAIN)
public void onEvent(LogMessageEvent messageEvent) {
    if(messageEvent!=null){
        switch(messageEvent.getLogtype()){
            case ConstantData.LOG_PCR:
                LogcatHelper.getInstance(getApplicationContext())
                        .appendlog(messageEvent.getLogtype(), toLogString(messageEvent.getLogtext()));
                break;
            case ConstantData.LOG_RNA:
                LogcatHelper.getInstance(getApplicationContext())
                        .appendlog(messageEvent.getLogtype(), toLogString(messageEvent.getLogtext()));
                break;
            // case ConstantData.LOG_ARM:  ← 删除这整个分支
        }
    }
}
```

#### 2.3 PipeMainActivity.java

**删除** `case LOG_ARM` 分支（类似 APLService.java）

### 阶段 3：日期格式统一

**当前格式不一致问题**：
- logcat 文件名：`yyyy-MM-dd-HH-mm-ss.log`
- 日期目录应为：`yyyyMMdd`

**统一改造**：
- `SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);`
- `SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA);`
- 移除旧的 `myLogSdf`，统一使用 `fileNameFormat`

## 风险评估

### 低风险
- ✅ 目录结构调整：只影响新产生的日志，旧日志保持不动
- ✅ 删除 ARM 日志：只是停止写入，不影响功能
- ✅ 日期格式统一：纯格式变更

### 中等风险
- ⚠️ 文件大小限制调整：logcat 从 100MB 降到 20MB，可能增加文件切换频率
- ⚠️ 清理逻辑变更：从单文件清理改为目录清理，需测试边界情况

### 注意事项
- 旧目录不会自动迁移：`/Log/LOGCAT/`、`/Log/ARM/`、`/Log/PCR/`、`/Log/RNA/` 的旧日志会保留原位
- 手动清理或等待自然过期（14天后用新清理逻辑也不会删它们，因为它们不是 yyyyMMdd 格式目录）
- 升级后立即生效，新日志写入新结构

## 测试验证

### 单元测试点
1. **目录创建**：验证 `/Log/20260906/logcat/`、`/Log/20260906/PCR/`、`/Log/20260906/RNA/` 正确创建
2. **文件命名**：验证文件名格式为 `yyyyMMdd-HHmmss.log`
3. **日期切换**：跨天测试，验证自动切换到新日期目录
4. **文件大小切换**：logcat 达到 20MB 时自动创建新文件
5. **过期清理**：14 天前的日期目录被正确删除
6. **ARM 日志不再产生**：确认发送 LOG_ARM 类型消息不会创建文件

### 集成测试
1. 运行完整流程，产生 PCR、RNA 日志
2. 确认 `/Log/yyyyMMdd/` 下有 `logcat/`、`PCR/`、`RNA/` 子目录
3. 确认没有 `ARM/` 子目录
4. 手动调整系统日期到 15 天后，验证清理逻辑

## 实施步骤

1. ✅ **备份当前代码**：提交当前版本到 git
2. ✅ **修改 LogcatHelper.java**：应用所有目录、清理、文件管理改造
3. ✅ **修改 ConstantData.java**：删除 LOG_ARM 常量
4. ✅ **修改 APLService.java**：删除 LOG_ARM 分支
5. ✅ **修改 PipeMainActivity.java**：删除 LOG_ARM 分支
6. ✅ **编译测试**：确保编译通过
7. ✅ **功能测试**：验证日志正常写入新目录结构
8. ✅ **清理测试**：验证过期目录清理功能
9. ✅ **提交代码**：提交所有改动

## 回退方案

如果出现问题需要回退：
1. Git revert 到改造前的提交
2. 旧日志仍在旧目录，可以访问
3. 新日志会重新写入旧目录结构

## 文件清单

**需要修改的文件**：
1. `app/src/main/java/com/gidi/bio_pcr/util/LogcatHelper.java` - 主要改造
2. `app/src/main/java/com/paul/pipeline/data/ConstantData.java` - 删除 LOG_ARM
3. `app/src/main/java/com/paul/pipeline/service/APLService.java` - 删除 LOG_ARM 分支
4. `app/src/main/java/com/paul/pipeline/activity/PipeMainActivity.java` - 删除 LOG_ARM 分支

**无需修改的文件**：
- 其他任何引用 LogcatHelper 的文件（API 保持兼容）
- 数据库相关文件
- 网络通信相关文件
