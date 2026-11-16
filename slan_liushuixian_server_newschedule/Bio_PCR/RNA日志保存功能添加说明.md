# RNA/PCR 二进制日志保存功能添加说明

## 问题描述

**现象：**
- RNALog(bin) 和 PCRLog(bin) 没有保存到本地文件
- 只有 EventBus 消息发送，但没有实际的文件写入逻辑

**影响：**
- RNA/PCR 模块的二进制日志无法保存到本地
- 无法追溯和排查 RNA/PCR 模块的历史问题
- 日志数据丢失

---

## 根本原因

### 当前代码分析

**位置1：** `app/src/main/java/com/winters/boweilibrary/autopipeline/mgr/WSSModuleManager.java:982-987`

**RNALog(bin) 问题代码：**
```java
} else if (binMsg instanceof RNALogMessage && item instanceof RNAStatusItem) {
    RNALogMessage msg = (RNALogMessage) binMsg;
    Log.e(TAG, "RNALog(bin): key=" + key + " file=" + msg.getFileName());
    EventBus.getDefault().post(new LogMessageEvent(
            ConstantData.LOG_RNA, binMsg.getFileData(),
            EnumMessageEvent.WSS_LOGMessage, (RNAStatusItem) item, msg));
}
```

**位置2：** `app/src/main/java/com/winters/boweilibrary/autopipeline/mgr/WSSModuleManager.java:994-1000`

**PCRLog(bin) 问题代码：**
```java
} else if (binMsg instanceof PCRLogMessage && item instanceof PCRStatusItem) {
    PCRLogMessage msg = (PCRLogMessage) binMsg;
    Log.e(TAG, "PCRLog(bin): key=" + key + " file=" + msg.getFileName());
    EventBus.getDefault().post(new LogMessageEvent(
            ConstantData.LOG_PCR, binMsg.getFileData(),
            EnumMessageEvent.WSS_LOGMessage, (PCRStatusItem) item, msg));
}
```

**问题分析：**
1. 只发送了 EventBus 消息
2. EventBus 的订阅者（APLService、PipeMainActivity）只是调用 `LogcatHelper.appendlog()`
3. `LogcatHelper.appendlog()` 只处理文本日志，写入 `/Log/yyyyMMdd/RNA/` 或 `/Log/yyyyMMdd/PCR/` 目录
4. **没有将二进制日志内容保存到独立的日志文件**

### 6in1 项目的实现

6in1 项目中有完整的实现：
- `saveRNALogToFile()` 方法
- `WSSLogHelper` 辅助类
- 异步保存到 `/Log/yyyyMMdd/RNA/DeviceID/` 目录

---

## 修复方案

### 方案：添加 RNA/PCR 二进制日志保存功能

**核心思路：**
1. 创建 `WSSLogHelper` 类，负责保存模块日志到文件
2. 在 `WSSModuleManager` 中添加 `saveRNALogToFile()` 和 `savePCRLogToFile()` 方法
3. 在接收到 RNALog(bin) 和 PCRLog(bin) 消息时，调用相应的保存方法

---

## 修复内容

### 1. 新增文件：WSSLogHelper.java

**路径：** `app/src/main/java/com/gidi/bio_pcr/util/WSSLogHelper.java`

**功能：**
- 保存 RNA/PCR 模块的二进制日志到本地文件
- 日志目录结构：`/Log/yyyyMMdd/ModuleName/DeviceID/yyyyMMdd-HHmmss.log`
- 文件大小限制：20MB 自动切换
- 线程安全的文件操作

**关键方法：**
```java
public synchronized void saveLog(String moduleName, String deviceID, String logContent)
```

**日志目录结构：**
```
/sdcard/Log/
└── 20260907/                      # 日期目录
    ├── logcat/                    # logcat 日志
    │   └── 20260907-150000.log
    ├── PCR/                       # PCR 模块日志
    │   ├── 20260907-150000.log    # 文本日志（LogcatHelper）
    │   └── PCR001/                # 设备ID目录（WSSLogHelper）
    │       └── 20260907-150000.log  # 二进制日志内容
    └── RNA/                       # RNA 模块日志
        ├── 20260907-150000.log    # 文本日志（LogcatHelper）
        └── RNA001/                # 设备ID目录（WSSLogHelper）
            └── 20260907-150000.log  # 二进制日志内容
```

### 2. 修改文件：WSSModuleManager.java

#### 修改1：添加导入
```java
import com.gidi.bio_pcr.util.WSSLogHelper;
```

#### 修改2：添加 `saveRNALogToFile()` 方法

**位置：** 在 `onSLANPCRFinish()` 方法之前

```java
/**
 * 保存 RNA 日志到本地文件（二进制消息）
 */
private void saveRNALogToFile(final RNALogMessage msg, final WSSConnectionItem item) {
    if (msg == null || item == null) {
        return;
    }

    pcrFinishExecutor.submit(new Runnable() {
        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            try {
                String deviceID = msg.getDeviceID();
                if (deviceID == null || deviceID.isEmpty()) {
                    deviceID = item.getDeviceID();
                }

                byte[] logData = msg.getFileData();

                if (logData != null && logData.length > 0) {
                    String logContent = new String(logData, "UTF-8");
                    WSSLogHelper.getInstance().saveLog("RNA", deviceID, logContent);
                    Log.i(TAG, "保存 RNA 日志成功: deviceID=" + deviceID +
                        ", size=" + logData.length +
                        " 耗时=" + (System.currentTimeMillis() - startTime) + "ms");
                }

            } catch (Exception e) {
                Log.e(TAG, "保存 RNA 日志失败: " + e.getMessage() +
                    " 耗时=" + (System.currentTimeMillis() - startTime) + "ms");
                e.printStackTrace();
            }
        }
    });
}
```

#### 修改3：添加 `savePCRLogToFile()` 方法

**位置：** 在 `saveRNALogToFile()` 方法之后

```java
/**
 * 保存 PCR 日志到本地文件（二进制消息）
 */
private void savePCRLogToFile(final PCRLogMessage msg, final WSSConnectionItem item) {
    if (msg == null || item == null) {
        return;
    }

    pcrFinishExecutor.submit(new Runnable() {
        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            try {
                String deviceID = msg.getDeviceID();
                if (deviceID == null || deviceID.isEmpty()) {
                    deviceID = item.getDeviceID();
                }

                byte[] logData = msg.getFileData();

                if (logData != null && logData.length > 0) {
                    String logContent = new String(logData, "UTF-8");
                    WSSLogHelper.getInstance().saveLog("PCR", deviceID, logContent);
                    Log.i(TAG, "保存 PCR 日志成功: deviceID=" + deviceID +
                        ", size=" + logData.length +
                        " 耗时=" + (System.currentTimeMillis() - startTime) + "ms");
                }

            } catch (Exception e) {
                Log.e(TAG, "保存 PCR 日志失败: " + e.getMessage() +
                    " 耗时=" + (System.currentTimeMillis() - startTime) + "ms");
                e.printStackTrace();
            }
        }
    });
}
```

#### 修改4：在 RNALog(bin) 处理中调用

**位置：** `WSSModuleManager.java:982-993`

**修改前：**
```java
} else if (binMsg instanceof RNALogMessage && item instanceof RNAStatusItem) {
    RNALogMessage msg = (RNALogMessage) binMsg;
    Log.e(TAG, "RNALog(bin): key=" + key + " file=" + msg.getFileName());
    EventBus.getDefault().post(new LogMessageEvent(
            ConstantData.LOG_RNA, binMsg.getFileData(),
            EnumMessageEvent.WSS_LOGMessage, (RNAStatusItem) item, msg));
}
```

**修改后：**
```java
} else if (binMsg instanceof RNALogMessage && item instanceof RNAStatusItem) {
    RNALogMessage msg = (RNALogMessage) binMsg;
    Log.e(TAG, "RNALog(bin): key=" + key + " file=" + msg.getFileName());

    // 保存 RNA 日志到本地文件
    saveRNALogToFile(msg, item);

    // 发送 EventBus 消息（用于界面显示等）
    EventBus.getDefault().post(new LogMessageEvent(
            ConstantData.LOG_RNA, binMsg.getFileData(),
            EnumMessageEvent.WSS_LOGMessage, (RNAStatusItem) item, msg));
}
```

#### 修改5：在 PCRLog(bin) 处理中调用

**位置：** `WSSModuleManager.java:994-1000`

**修改前：**
```java
} else if (binMsg instanceof PCRLogMessage && item instanceof PCRStatusItem) {
    PCRLogMessage msg = (PCRLogMessage) binMsg;
    Log.e(TAG, "PCRLog(bin): key=" + key + " file=" + msg.getFileName());
    EventBus.getDefault().post(new LogMessageEvent(
            ConstantData.LOG_PCR, binMsg.getFileData(),
            EnumMessageEvent.WSS_LOGMessage, (PCRStatusItem) item, msg));
}
```

**修改后：**
```java
} else if (binMsg instanceof PCRLogMessage && item instanceof PCRStatusItem) {
    PCRLogMessage msg = (PCRLogMessage) binMsg;
    Log.e(TAG, "PCRLog(bin): key=" + key + " file=" + msg.getFileName());

    // 保存 PCR 日志到本地文件
    savePCRLogToFile(msg, item);

    // 发送 EventBus 消息（用于界面显示等）
    EventBus.getDefault().post(new LogMessageEvent(
            ConstantData.LOG_PCR, binMsg.getFileData(),
            EnumMessageEvent.WSS_LOGMessage, (PCRStatusItem) item, msg));
}
```

---

## 工作流程

### 修复前流程

```
1. 接收 RNALog(bin) 消息
   ↓
2. 解析 RNALogMessage
   ↓
3. 发送 EventBus 消息 ✅
   ↓
4. APLService/PipeMainActivity 订阅
   ↓
5. 调用 LogcatHelper.appendlog() ✅
   写入 /Log/yyyyMMdd/RNA/xxx.log（文本日志）
   ↓
6. ❌ 二进制日志内容未保存到独立文件
```

### 修复后流程

```
1. 接收 RNALog(bin) 消息
   ↓
2. 解析 RNALogMessage
   ↓
3. 调用 saveRNALogToFile() ✅ (新增)
   ↓
   3.1 提交到 pcrFinishExecutor 异步执行
   ↓
   3.2 转换 byte[] 为 String
   ↓
   3.3 调用 WSSLogHelper.saveLog("RNA", deviceID, logContent)
   ↓
   3.4 保存到 /Log/yyyyMMdd/RNA/DeviceID/xxx.log ✅
   ↓
4. 发送 EventBus 消息 ✅
   ↓
5. APLService/PipeMainActivity 订阅
   ↓
6. 调用 LogcatHelper.appendlog() ✅
   写入 /Log/yyyyMMdd/RNA/xxx.log（文本日志）
```

---

## 关键改进点

### 1. 异步保存
- 使用 `pcrFinishExecutor` 线程池异步执行
- 不阻塞主线程和网络接收线程
- 提高系统响应性能

### 2. 文件管理
- 自动创建日期目录：`/Log/yyyyMMdd/`
- 按设备ID分目录：`RNA/DeviceID/`
- 文件大小超过 20MB 自动切换
- 日期变化自动切换目录

### 3. 错误处理
- 完善的异常捕获和日志记录
- 文件操作失败不影响其他功能
- 记录保存耗时，便于性能分析

### 4. 日志格式
```
2026-09-07 15:16:24 [RNA001] 日志内容
```
- 包含时间戳
- 包含设备ID
- 便于追溯和排查问题

---

## 验证方法

### 1. 编译验证
```bash
./gradlew compileDebugJavaWithJavac
```
**结果：** ✅ BUILD SUCCESSFUL

### 2. 功能测试

**测试步骤：**
1. 启动 RNA 模块实验
2. 运行过程中，RNA 模块会发送 RNALog(bin) 消息
3. 检查日志输出：
   ```bash
   adb logcat | grep "保存 RNA 日志"
   ```
   预期输出：
   ```
   I/WSSModuleManager: 保存 RNA 日志成功: deviceID=RNA001, size=2048 耗时=15ms
   ```

4. 检查文件系统：
   ```bash
   adb shell ls -lh /sdcard/Log/20260907/RNA/
   ```
   预期结果：
   - 有 `RNA001/`（或其他设备ID）目录
   - 目录下有 `.log` 文件
   - 文件大小 > 0

5. 查看日志内容：
   ```bash
   adb shell cat /sdcard/Log/20260907/RNA/RNA001/20260907-150000.log
   ```
   预期结果：
   - 每行包含时间戳和设备ID
   - 内容为 RNA 模块的日志信息

### 3. 性能测试

**测试指标：**
- 保存耗时 < 100ms（大部分情况下）
- 不影响网络接收性能
- 不阻塞主线程

**测试方法：**
查看日志中的耗时记录：
```
I/WSSModuleManager: 保存 RNA 日志成功: deviceID=RNA001, size=2048 耗时=15ms
```

---

## 与 LogcatHelper 的区别

| 特性 | LogcatHelper | WSSLogHelper |
|------|-------------|--------------|
| **用途** | 保存应用日志 | 保存模块二进制日志 |
| **目录结构** | `/Log/yyyyMMdd/RNA/` | `/Log/yyyyMMdd/RNA/DeviceID/` |
| **文件命名** | `yyyyMMdd-HHmmss.log` | `yyyyMMdd-HHmmss.log` |
| **文件大小** | 100MB | 20MB |
| **内容来源** | `appendlog()` API | RNALog(bin) 消息 |
| **日志格式** | 纯文本 | 时间戳 + 设备ID + 内容 |
| **设备区分** | 无 | 按设备ID分目录 |

**说明：**
- LogcatHelper 用于应用级别的文本日志（RNA、PCR 模块调用 `appendlog()` 的内容）
- WSSLogHelper 用于设备级别的二进制日志（从 WebSocket 接收的 RNALog(bin) 消息）
- 两者互补，共同构成完整的日志系统

---

## 后续优化建议

### 1. 添加 PCRLog(bin) 保存
```java
} else if (binMsg instanceof PCRLogMessage && item instanceof PCRStatusItem) {
    PCRLogMessage msg = (PCRLogMessage) binMsg;
    Log.e(TAG, "PCRLog(bin): key=" + key + " file=" + msg.getFileName());
    
    // 保存 PCR 日志到本地文件
    savePCRLogToFile(msg, item);
    
    EventBus.getDefault().post(new LogMessageEvent(
            ConstantData.LOG_PCR, binMsg.getFileData(),
            EnumMessageEvent.WSS_LOGMessage, (PCRStatusItem) item, msg));
}
```

### 2. 添加日志查询功能
- 按设备ID查询日志
- 按日期范围查询日志
- 导出日志文件

### 3. 添加日志统计
- 统计各设备的日志数量
- 统计日志文件大小
- 监控磁盘占用

### 4. 优化性能
- 使用缓冲写入减少磁盘IO
- 批量写入日志
- 压缩历史日志

---

## 注意事项

1. **磁盘空间**
   - 确保设备有足够的磁盘空间
   - 建议定期清理旧日志
   - 可以添加磁盘空间检查

2. **权限**
   - 确保应用有外部存储写入权限
   - Android 6.0+ 需要动态申请权限

3. **性能影响**
   - 使用异步保存，不影响主线程
   - 大量日志时可能影响磁盘IO
   - 建议监控保存耗时

4. **字符编码**
   - 使用 UTF-8 编码
   - 确保日志内容正确解析

---

## 涉及文件清单

### 新增文件（1个）
1. `app/src/main/java/com/gidi/bio_pcr/util/WSSLogHelper.java` - WSS日志保存辅助类

### 修改文件（1个）
1. `app/src/main/java/com/winters/boweilibrary/autopipeline/mgr/WSSModuleManager.java`
   - 添加 `import com.gidi.bio_pcr.util.WSSLogHelper;`
   - 添加 `saveRNALogToFile()` 方法
   - 修改 RNALog(bin) 处理逻辑

---

## 参考文档

- `D:\PCRinOne\framework\bio_pcr_6in1\6in1_Server\Bio_PCR\app\src\main\java\com\gidi\bio_pcr_6in1_server\util\WSSLogHelper.java`
- `D:\PCRinOne\framework\bio_pcr_6in1\6in1_Server\Bio_PCR\app\src\main\java\com\winters\boweilibrary\autopipeline\mgr\WSSModuleManager.java`

---

**修复完成时间：** 2026-09-07  
**修复文件数：** 2个（1个新增，1个修改）  
**编译状态：** ✅ BUILD SUCCESSFUL  
**建议部署：** ✅ 可以部署，需进行功能测试验证
