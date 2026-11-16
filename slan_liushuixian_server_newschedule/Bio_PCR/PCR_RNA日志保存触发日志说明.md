# PCR/RNA 日志保存触发日志说明

## 日志输出位置

在 `WSSModuleManager.java` 中接收到 PCRLog(bin) 或 RNALog(bin) 消息时，会输出详细的日志信息。

---

## 日志格式

### RNALog(bin) 日志

**触发位置：** 接收到 RNA 模块的二进制日志消息时

**日志输出：**
```
E/WSSModuleManager: 【RNALog(bin)】收到日志: key=RNA_001, file=rna_log_20260907.bin, deviceID=RNA001, size=2048 bytes
I/WSSModuleManager: 【RNALog(bin)】开始触发保存到本地文件...
I/WSSModuleManager: 【RNALog(bin)】保存任务已提交到异步队列
I/WSSModuleManager: 保存 RNA 日志成功: deviceID=RNA001, size=2048 耗时=15ms
```

### PCRLog(bin) 日志

**触发位置：** 接收到 PCR 模块的二进制日志消息时

**日志输出：**
```
E/WSSModuleManager: 【PCRLog(bin)】收到日志: key=PCR_001, file=pcr_log_20260907.bin, deviceID=PCR001, size=4096 bytes
I/WSSModuleManager: 【PCRLog(bin)】开始触发保存到本地文件...
I/WSSModuleManager: 【PCRLog(bin)】保存任务已提交到异步队列
I/WSSModuleManager: 保存 PCR 日志成功: deviceID=PCR001, size=4096 耗时=20ms
```

---

## 日志字段说明

| 字段 | 说明 | 示例值 |
|------|------|--------|
| **key** | 消息唯一标识 | RNA_001, PCR_001 |
| **file** | 日志文件名 | rna_log_20260907.bin |
| **deviceID** | 设备ID | RNA001, PCR001 |
| **size** | 日志数据大小（字节） | 2048 bytes |
| **耗时** | 保存操作耗时（毫秒） | 15ms |

---

## 日志级别说明

| 级别 | 标签 | 用途 |
|------|------|------|
| **ERROR** | `E/WSSModuleManager` | 接收到日志消息（方便过滤和定位） |
| **INFO** | `I/WSSModuleManager` | 保存过程的关键步骤 |
| **WARN** | `W/WSSModuleManager` | 异常情况（如保存失败） |

---

## 查看日志命令

### 1. 查看所有 RNA/PCR 日志
```bash
adb logcat | grep "Log(bin)"
```

**输出示例：**
```
E/WSSModuleManager: 【RNALog(bin)】收到日志: key=RNA_001, file=rna_log_20260907.bin, deviceID=RNA001, size=2048 bytes
I/WSSModuleManager: 【RNALog(bin)】开始触发保存到本地文件...
I/WSSModuleManager: 【RNALog(bin)】保存任务已提交到异步队列
E/WSSModuleManager: 【PCRLog(bin)】收到日志: key=PCR_001, file=pcr_log_20260907.bin, deviceID=PCR001, size=4096 bytes
I/WSSModuleManager: 【PCRLog(bin)】开始触发保存到本地文件...
```

### 2. 只看 RNA 日志
```bash
adb logcat | grep "RNALog(bin)"
```

### 3. 只看 PCR 日志
```bash
adb logcat | grep "PCRLog(bin)"
```

### 4. 查看保存结果
```bash
adb logcat | grep "保存.*日志成功\|保存.*日志失败"
```

**输出示例：**
```
I/WSSModuleManager: 保存 RNA 日志成功: deviceID=RNA001, size=2048 耗时=15ms
I/WSSModuleManager: 保存 PCR 日志成功: deviceID=PCR001, size=4096 耗时=20ms
```

### 5. 查看完整的保存流程
```bash
adb logcat | grep -E "Log\(bin\)|保存.*日志"
```

---

## 日志流程

### 完整的日志保存流程

```
1. 接收到二进制消息
   ↓
   E/WSSModuleManager: 【RNALog(bin)】收到日志: key=RNA_001, file=xxx, deviceID=RNA001, size=2048 bytes
   
2. 触发保存
   ↓
   I/WSSModuleManager: 【RNALog(bin)】开始触发保存到本地文件...
   
3. 提交到异步队列
   ↓
   I/WSSModuleManager: 【RNALog(bin)】保存任务已提交到异步队列
   
4. 异步线程执行保存
   ↓
   （后台执行）
   
5. 保存完成
   ↓
   I/WSSModuleManager: 保存 RNA 日志成功: deviceID=RNA001, size=2048 耗时=15ms
   
   或（如果失败）
   
   E/WSSModuleManager: 保存 RNA 日志失败: 错误信息 耗时=10ms
```

---

## 异常情况日志

### 1. 设备ID为空
```
E/WSSModuleManager: 【RNALog(bin)】收到日志: key=RNA_001, file=xxx, deviceID=null, size=2048 bytes
```
**说明：** 设备ID从 item 中获取

### 2. 数据为空
```
E/WSSModuleManager: 【RNALog(bin)】收到日志: key=RNA_001, file=xxx, deviceID=RNA001, size=0 bytes
```
**说明：** 没有日志数据，不会触发保存

### 3. 保存失败
```
E/WSSModuleManager: 保存 RNA 日志失败: java.io.IOException: No space left on device 耗时=5ms
```
**可能原因：**
- 磁盘空间不足
- 权限不足
- 文件路径错误

---

## 日志调试技巧

### 1. 实时监控日志保存
```bash
adb logcat -c && adb logcat | grep -E "Log\(bin\)|保存.*日志"
```

### 2. 统计保存成功率
```bash
adb logcat | grep "保存.*日志" | tee log.txt
# 然后统计成功和失败的数量
grep "成功" log.txt | wc -l
grep "失败" log.txt | wc -l
```

### 3. 查看保存耗时分布
```bash
adb logcat | grep "保存.*日志成功" | grep -oP "耗时=\d+ms"
```

### 4. 过滤特定设备的日志
```bash
adb logcat | grep "deviceID=RNA001"
```

---

## 性能指标

### 正常情况下的指标

| 指标 | 预期值 | 说明 |
|------|--------|------|
| **保存耗时** | < 50ms | 小文件（<10KB） |
| **保存耗时** | < 100ms | 中文件（10KB-100KB） |
| **保存耗时** | < 500ms | 大文件（100KB-1MB） |
| **成功率** | > 99% | 正常情况下 |

### 异常情况

如果出现以下情况，需要排查：
- 保存耗时 > 1000ms
- 成功率 < 95%
- 频繁出现 "保存失败"

---

## 示例日志

### 正常流程
```
14:23:15.123  E/WSSModuleManager: 【RNALog(bin)】收到日志: key=RNA_001, file=rna_20260907_142315.bin, deviceID=RNA001, size=2048 bytes
14:23:15.125  I/WSSModuleManager: 【RNALog(bin)】开始触发保存到本地文件...
14:23:15.126  I/WSSModuleManager: 【RNALog(bin)】保存任务已提交到异步队列
14:23:15.142  I/WSSModuleManager: 保存 RNA 日志成功: deviceID=RNA001, size=2048 耗时=16ms
14:23:15.145  I/WSSLogHelper: 创建新的WSS日志文件: /sdcard/Log/20260907/RNA/RNA001/20260907-142315.log

14:23:20.456  E/WSSModuleManager: 【PCRLog(bin)】收到日志: key=PCR_001, file=pcr_20260907_142320.bin, deviceID=PCR001, size=4096 bytes
14:23:20.458  I/WSSModuleManager: 【PCRLog(bin)】开始触发保存到本地文件...
14:23:20.459  I/WSSModuleManager: 【PCRLog(bin)】保存任务已提交到异步队列
14:23:20.479  I/WSSModuleManager: 保存 PCR 日志成功: deviceID=PCR001, size=4096 耗时=20ms
14:23:20.482  I/WSSLogHelper: 创建新的WSS日志文件: /sdcard/Log/20260907/PCR/PCR001/20260907-142320.log
```

### 异常流程（保存失败）
```
14:25:10.123  E/WSSModuleManager: 【RNALog(bin)】收到日志: key=RNA_002, file=rna_20260907_142510.bin, deviceID=RNA001, size=2048 bytes
14:25:10.125  I/WSSModuleManager: 【RNALog(bin)】开始触发保存到本地文件...
14:25:10.126  I/WSSModuleManager: 【RNALog(bin)】保存任务已提交到异步队列
14:25:10.135  E/WSSModuleManager: 保存 RNA 日志失败: java.io.IOException: Permission denied 耗时=9ms
14:25:10.136  E/WSSLogHelper: 写入日志失败: java.io.IOException: Permission denied
```

---

## 故障排查

### 问题：没有看到日志
**可能原因：**
1. 日志级别被过滤
2. 没有接收到二进制消息
3. TAG 不匹配

**解决方法：**
```bash
# 降低日志级别
adb logcat *:V | grep WSSModuleManager

# 查看所有 WebSocket 消息
adb logcat | grep -i "wss\|websocket"
```

### 问题：保存任务提交了但没有"成功"日志
**可能原因：**
1. 异步任务还在执行（等待一会）
2. 异步任务抛出异常（查看完整日志）
3. 线程池满了（任务被拒绝）

**解决方法：**
```bash
# 查看完整异常堆栈
adb logcat | grep -A 10 "保存.*日志失败"
```

---

## 文件验证

保存成功后，可以验证文件是否存在：

```bash
# 查看 RNA 日志文件
adb shell ls -lh /sdcard/Log/20260907/RNA/RNA001/

# 查看 PCR 日志文件
adb shell ls -lh /sdcard/Log/20260907/PCR/PCR001/

# 查看文件内容（前几行）
adb shell head -20 /sdcard/Log/20260907/RNA/RNA001/20260907-142315.log
```

---

**创建时间：** 2026-09-07  
**相关文件：** WSSModuleManager.java, WSSLogHelper.java
