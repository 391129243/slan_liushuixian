# LogcatHelper 未存储本地日志问题修复说明

## 问题描述

**现象：**
- 本地 logcat 日志没有保存到 `/sdcard/Log/yyyyMMdd/logcat/` 目录
- 应用运行后没有生成 logcat 日志文件

---

## 根本原因

### LogcatHelper 初始化缺失

**问题：**
- `LogcatHelper.start()` 在应用启动 2 秒后被调用（PcrApplication.java:82）
- 但 **`LogcatHelper.init()` 从未被调用**
- 导致 `PATH_LOGCAT` 等路径变量为 `null`
- 日志无法保存

### 代码分析

**PcrApplication.java 原代码：**
```java
@Override
public void onCreate() {
    super.onCreate();
    mAppContext = this;
    mApplication = this;
    // ... 其他初始化
    mUIHandler = new UIHandler();
    //启动日志收集功能
    mUIHandler.postDelayed(logrunnable, 2000);  // 2秒后启动 LogcatHelper
}
```

**LogcatHelper.java 需要的初始化：**
```java
public void init(Context context) {
    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
        PATH_LOGCAT = Environment.getExternalStorageDirectory().getAbsolutePath()+ "/Log/";
        // ...
    } else {
        PATH_LOGCAT = context.getFilesDir().getPath() + File.separator + "Log/";
    }
}
```

**问题：**
- 如果 `init()` 没有被调用，`PATH_LOGCAT` 为 `null`
- `start()` 调用时无法创建 LogDumper，或者 LogDumper 保存路径为 `null`
- 日志无法写入文件

---

## 修复方案

### 在 PcrApplication.onCreate() 中添加 LogcatHelper 初始化

**位置：** `app/src/main/java/com/gidi/bio_pcr/PcrApplication.java:71-82`

**修改前：**
```java
@Override
public void onCreate() {
    super.onCreate();
    mAppContext = this;
    mApplication = this;
    String rootDir = MMKV.initialize(this);
    Log.i("MMKV", "MMKV root directory: " + rootDir);
    initSecure();
    mUIHandler = new UIHandler();
    //启动日志收集功能
    mUIHandler.postDelayed(logrunnable, 2000);
    // ... 其他代码
}
```

**修改后：**
```java
@Override
public void onCreate() {
    super.onCreate();
    mAppContext = this;
    mApplication = this;
    String rootDir = MMKV.initialize(this);
    Log.i("MMKV", "MMKV root directory: " + rootDir);
    initSecure();
    mUIHandler = new UIHandler();

    // 初始化 LogcatHelper（必须在 start() 之前调用）
    LogcatHelper.getInstance(getApplicationContext()).init(getApplicationContext());
    Log.i("PcrApplication", "LogcatHelper 初始化完成");

    //启动日志收集功能
    mUIHandler.postDelayed(logrunnable, 2000);
    // ... 其他代码
}
```

---

## 修复后的执行流程

```
1. 应用启动
   ↓
2. PcrApplication.onCreate()
   ↓
3. LogcatHelper.init(context)  ✅ 新增
   → 初始化 PATH_LOGCAT = "/sdcard/Log/"
   → 设置日志保存天数 = 14 天
   → 设置文件大小限制 = 20MB
   ↓
4. mUIHandler.postDelayed(logrunnable, 2000)
   ↓
5. 等待 2 秒
   ↓
6. logrunnable.run()
   → mUIHandler.sendEmptyMessage(MSG_START_LOG)
   ↓
7. UIHandler.handleMessage(MSG_START_LOG)
   → LogcatHelper.start()  ✅
   → 创建 LogDumper
   → 启动 logcat 进程
   → 开始保存日志到 /sdcard/Log/yyyyMMdd/logcat/xxx.log
   ↓
8. 日志持续保存
```

---

## 验证方法

### 1. 编译验证
```bash
./gradlew compileDebugJavaWithJavac
```
**结果：** ✅ BUILD SUCCESSFUL

### 2. 查看初始化日志
```bash
adb logcat | grep "LogcatHelper"
```

**预期输出：**
```
I/PcrApplication: LogcatHelper 初始化完成
I/LogcatHelper: Environment.getExternalStorageState()mounted
```

### 3. 查看日志文件是否生成
```bash
# 等待应用启动 2 秒后
adb shell ls -lh /sdcard/Log/$(date +%Y%m%d)/logcat/
```

**预期输出：**
```
-rw-rw---- 1 u0_a123 sdcard_rw 1234567 2026-09-07 15:00 20260907-150000.log
```

### 4. 查看日志内容
```bash
adb shell head -20 /sdcard/Log/$(date +%Y%m%d)/logcat/*.log
```

**预期输出：**
```
09-07 15:00:01.123  1234  1234 I PcrApplication: onCreate
09-07 15:00:01.125  1234  1234 I MMKV: MMKV root directory: /data/user/0/com.gidi.bio_pcr/files/mmkv
09-07 15:00:01.127  1234  1234 I PcrApplication: LogcatHelper 初始化完成
...
```

### 5. 查看日志目录结构
```bash
adb shell tree /sdcard/Log/ -L 3
```

**预期输出：**
```
/sdcard/Log/
└── 20260907/
    ├── logcat/
    │   └── 20260907-150000.log
    ├── PCR/
    │   ├── 20260907-150000.log
    │   └── SLAN_PCR1111/
    └── RNA/
        ├── 20260907-150000.log
        └── RNA001/
```

---

## 相关配置

### LogcatHelper 配置项

| 配置项 | 值 | 说明 |
|-------|-----|------|
| **日志根目录** | `/sdcard/Log/` | 外部存储挂载时 |
| **备用目录** | `/data/data/com.gidi.bio_pcr/files/Log/` | 外部存储未挂载时 |
| **日志保存天数** | 14 天 | 自动删除超过14天的日志 |
| **logcat 文件大小限制** | 20 MB | 单个 logcat 文件达到 20MB 自动切换 |
| **模块文件大小限制** | 100 MB | PCR/RNA 模块日志文件限制 |
| **启动延迟** | 2 秒 | 应用启动后延迟 2 秒启动日志收集 |

---

## 注意事项

1. **初始化顺序很重要**
   - 必须先调用 `init()` 初始化路径
   - 再调用 `start()` 启动日志收集
   - 否则日志无法保存

2. **存储权限**
   - 应用需要有外部存储权限（`WRITE_EXTERNAL_STORAGE`）
   - Android 6.0+ 需要动态申请权限
   - 如果没有权限，会使用内部存储

3. **延迟启动的原因**
   - 等待 2 秒确保应用初始化完成
   - 避免在初始化过程中启动 logcat 进程导致冲突

4. **日志文件命名**
   - 格式：`yyyyMMdd-HHmmss.log`
   - 示例：`20260907-150000.log`

5. **自动清理**
   - 每次启动时自动删除超过 14 天的日志目录
   - 节省存储空间

---

## 相关文件

### 修改文件（1个）
1. `app/src/main/java/com/gidi/bio_pcr/PcrApplication.java`
   - 添加 `LogcatHelper.init(context)` 调用

### 相关文件（未修改）
1. `app/src/main/java/com/gidi/bio_pcr/util/LogcatHelper.java`
   - `init()` - 初始化路径和配置
   - `start()` - 启动日志收集
   - `stop()` - 停止日志收集

---

## 故障排查

### 问题：日志文件仍然没有生成

**可能原因：**
1. **存储权限未授予**
   ```bash
   adb shell dumpsys package com.gidi.bio_pcr | grep "android.permission.WRITE_EXTERNAL_STORAGE"
   ```
   
2. **外部存储未挂载**
   ```bash
   adb shell mount | grep sdcard
   ```

3. **LogcatHelper 启动失败**
   ```bash
   adb logcat | grep -E "LogcatHelper|LogDumper"
   ```

4. **logcat 进程启动失败**
   ```bash
   adb shell ps | grep logcat
   ```

### 问题：日志文件很小或为空

**可能原因：**
1. **日志级别过滤**
   - 检查 LogDumper 中的日志级别过滤条件

2. **logcat 缓冲区被清空**
   - 应用启动前执行了 `logcat -c`

3. **写入权限问题**
   - 检查文件权限和目录权限

---

**修复完成时间：** 2026-09-07  
**修复文件数：** 1个  
**编译状态：** ✅ BUILD SUCCESSFUL  
**建议部署：** ✅ 可以部署，需验证日志文件生成
