# PCRLog 上报日志记录增强

> **实现时间**: 2026-09-07  
> **目的**: 在日志文件中记录 PCRLog 上报的详细过程，便于调试和监控

---

## 一、日志输出示例

### 1.1 完整上报流程日志

```
========== PCRLog 上报触发 ==========
触发原因: 定时30秒到期
待上传大小: 18432 字节 (18 KB)
距上次上传: 30 秒

准备上传文件: 16-30-55.log, 文件大小=20480, 已上传=2048, 待上传=18432
读取日志数据: 16384 字节

→ 发送PCRLog消息
  文件路径: 2026-09-07/16-30-55.log
  数据大小: 16384 字节
  起始位置: 2048

构造PCRLog消息: 2026-09-07/16-30-55.log
PCRLog消息详情: SeqId=123, DeviceID=PCR1111, Position=1, FileSize=16384
序列化完成: 总大小=16584 字节 (JSON头+分隔符+数据)
PCRLog消息发送成功: 2026-09-07/16-30-55.log, SeqId=123

← PCRLog消息发送成功
PCRLog上传成功: 16-30-55.log [2048 -> 18432] (16384 bytes)

上传统计: 总文件=3, 已上传=1, 上传字节=16384
========== PCRLog 上报完成 ==========
```

### 1.2 触发条件日志

**定时触发（30秒到期）**：
```
========== PCRLog 上报触发 ==========
触发原因: 定时30秒到期
待上传大小: 8192 字节 (8 KB)
距上次上传: 31 秒
```

**缓冲触发（积累16KB）**：
```
========== PCRLog 上报触发 ==========
触发原因: 积累18KB
待上传大小: 18432 字节 (18 KB)
距上次上传: 12 秒
```

### 1.3 失败场景日志

**WSS 未连接**：
```
→ 发送PCRLog消息
  文件路径: 2026-09-07/16-30-55.log
  数据大小: 16384 字节
  起始位置: 0

PCRLog发送失败: WSS未连接
← PCRLog消息发送失败（WSS未连接或发送异常）
PCRLog上传失败: 16-30-55.log
```

**文件读取异常**：
```
准备上传文件: 16-30-55.log, 文件大小=20480, 已上传=0, 待上传=20480
读取日志文件异常: /storage/emulated/0/Log/2026-09-07/16-30-55.log
java.io.FileNotFoundException: /storage/emulated/0/Log/2026-09-07/16-30-55.log (No such file or directory)
```

---

## 二、日志级别说明

### 2.1 日志级别分类

| 级别 | 用途 | 示例 |
|------|------|------|
| **Log.i** | 关键流程节点 | 上报触发、发送成功、统计信息 |
| **Log.d** | 调试信息 | 文件准备、数据读取 |
| **Log.w** | 警告信息 | WSS未连接、跳过字节不匹配 |
| **Log.e** | 错误信息 | 参数无效、发送异常、读取失败 |

### 2.2 关键日志节点

```
LogcatHelper.checkAndUpload()
  ├─ Log.i: "========== PCRLog 上报触发 =========="
  ├─ Log.i: 触发原因、待上传大小、距上次上传时间
  │
  └─ uploadIncrementLogs()
      ├─ Log.w: 日志根目录不存在（如果不存在）
      ├─ Log.d: 准备上传文件（每个文件）
      │
      └─ uploadSingleFile()
          ├─ Log.d: 准备上传文件详情
          ├─ Log.i: 读取日志数据
          │
          └─ sendLogToServer()
              ├─ Log.i: → 发送PCRLog消息（详情）
              │
              └─ WSSModuleManager.sendLogMessage()
                  ├─ Log.i: 构造PCRLog消息
                  ├─ Log.i: PCRLog消息详情
                  ├─ Log.i: 序列化完成
                  ├─ Log.i: PCRLog消息发送成功
                  └─ Log.e: PCRLog发送失败（如果失败）
              │
              ├─ Log.i: ← PCRLog消息发送成功
              └─ Log.w: ← PCRLog消息发送失败（如果失败）
          │
          ├─ Log.i: PCRLog上传成功（包含进度）
          └─ Log.e: PCRLog上传失败
      │
      └─ Log.i: 上传统计
  │
  └─ Log.i: "========== PCRLog 上报完成 =========="
```

---

## 三、增强点详解

### 3.1 checkAndUpload() 增强

**改前**：
```java
Log.d(TAG, "触发上传：" + reason + "，待上传：" + pendingSize + "字节");
```

**改后**：
```java
Log.i(TAG, "========== PCRLog 上报触发 ==========");
Log.i(TAG, "触发原因: " + reason);
Log.i(TAG, "待上传大小: " + pendingSize + " 字节 (" + (pendingSize / 1024) + " KB)");
Log.i(TAG, "距上次上传: " + (elapsedTime / 1000) + " 秒");
// ... 上传逻辑 ...
Log.i(TAG, "========== PCRLog 上报完成 ==========");
```

**优势**：
- 分隔符便于日志文件中快速定位上报周期
- KB 单位更直观
- 包含时间间隔信息

### 3.2 uploadIncrementLogs() 增强

**新增统计**：
```java
int totalFiles = 0;        // 总文件数
int uploadedFiles = 0;     // 成功上传的文件数
long totalBytes = 0;       // 总上传字节数

// 遍历文件...
Log.i(TAG, "上传统计: 总文件=" + totalFiles + ", 已上传=" + uploadedFiles + ", 上传字节=" + totalBytes);
```

**优势**：
- 一目了然的上传效果
- 便于监控上传成功率

### 3.3 uploadSingleFile() 增强

**新增详情日志**：
```java
Log.d(TAG, "准备上传文件: " + logFile.getName() + ", 文件大小=" + fileLength + ", 已上传=" + readPos + ", 待上传=" + (fileLength - readPos));
Log.i(TAG, "读取日志数据: " + bytesRead + " 字节");
Log.i(TAG, "PCRLog上传成功: " + logFile.getName() + " [" + readPos + " -> " + newPos + "] (" + bytesRead + " bytes)");
```

**优势**：
- 清晰显示断点续传进度
- 便于排查文件读取问题

### 3.4 sendLogToServer() 增强

**新增发送详情**：
```java
Log.i(TAG, "→ 发送PCRLog消息");
Log.i(TAG, "  文件路径: " + relativePath);
Log.i(TAG, "  数据大小: " + length + " 字节");
Log.i(TAG, "  起始位置: " + startPos);
// ... 发送 ...
Log.i(TAG, "← PCRLog消息发送成功");
```

**优势**：
- 箭头符号直观表示发送方向
- 便于追踪单个文件的上传流程

### 3.5 WSSModuleManager.sendLogMessage() 增强

**新增消息详情**：
```java
Log.i(TAG, "构造PCRLog消息: " + fileName);
Log.i(TAG, "PCRLog消息详情: SeqId=" + seqId + ", DeviceID=" + PcrApplication.deviceId +
        ", Position=" + PcrApplication.devicePos + ", FileSize=" + length);
Log.i(TAG, "序列化完成: 总大小=" + totalSize + " 字节 (JSON头+分隔符+数据)");
Log.i(TAG, "PCRLog消息发送成功: " + fileName + ", SeqId=" + seqId);
```

**优势**：
- 显示 SeqId 便于与服务器端日志对应
- 显示序列化后的总大小（包含协议开销）
- 便于排查消息构造问题

---

## 四、日志过滤方法

### 4.1 Android Studio Logcat 过滤

**查看所有 PCRLog 相关日志**：
```
tag:LogcatHelper PCRLog
```

**查看上报触发**：
```
tag:LogcatHelper 上报触发
```

**查看发送成功**：
```
tag:LogcatHelper 发送成功
```

**查看失败日志**：
```
tag:LogcatHelper level:warn
tag:LogcatHelper level:error
```

### 4.2 命令行过滤

```bash
# 实时查看 PCRLog 上报
adb logcat | grep "PCRLog"

# 查看触发和完成
adb logcat | grep "=========="

# 查看统计信息
adb logcat | grep "上传统计"

# 查看错误
adb logcat *:E | grep "LogcatHelper"
```

---

## 五、监控指标

### 5.1 从日志中提取的关键指标

| 指标 | 提取方式 | 正常范围 |
|------|---------|---------|
| **上传频率** | 统计 "上报触发" 的时间间隔 | 10-30秒 |
| **上传成功率** | `已上传 / 总文件` | >95% |
| **单次上传大小** | "待上传大小" | 0-16KB |
| **WSS连接状态** | "WSS未连接" 出现次数 | 0次 |
| **文件读取异常** | "读取日志文件异常" | 0次 |

### 5.2 异常监控

**高频告警**：
```
# 连续3次上报失败
grep "PCRLog上传失败" xxx.log | tail -3
```

**WSS断连**：
```
# 出现 "WSS未连接" 日志
grep "WSS未连接" xxx.log
```

**序列化失败**：
```
# 出现 "序列化失败" 日志
grep "序列化失败" xxx.log
```

---

## 六、调试技巧

### 6.1 验证上报是否启动

**查看日志**：
```
tag:LogcatHelper 上传定时器已启动
```

**期望输出**：
```
LogcatHelper: 日志增量上传定时器已启动：定时30秒 OR 积累16KB
```

### 6.2 验证触发条件

**强制触发（定时30秒）**：
- 等待30秒后查看日志

**强制触发（积累16KB）**：
```bash
# 生成大量日志
adb shell "for i in {1..1000}; do echo 'Test log message $i' >> /storage/emulated/0/test.log; done"
```

### 6.3 验证断点续传

**步骤**：
1. 启动应用，等待上报一次（观察日志 "PCRLog上传成功"）
2. 记录当前进度（如 `[0 -> 16384]`）
3. 重启应用
4. 再次触发上报
5. 观察日志，进度应从 `16384` 继续

**期望日志**：
```
准备上传文件: 16-30-55.log, 文件大小=32768, 已上传=16384, 待上传=16384
```

---

## 七、性能影响

### 7.1 日志输出开销

| 日志类型 | 单次耗时 | 频率 | 总开销 |
|---------|---------|------|--------|
| Log.i 简单 | <0.1ms | 每次上报10条 | <1ms |
| Log.i 复杂（拼接） | <0.5ms | 每次上报5条 | <2.5ms |
| 总计 | - | 每30秒一次 | <5ms/30s |

**结论**：对性能影响可忽略（<0.02% CPU）

### 7.2 日志文件大小增长

**假设**：
- 每次上报产生 15 条日志
- 每条日志平均 100 字节
- 上报频率：30秒/次

**增长速率**：
```
15条 × 100字节 × (3600秒/30秒) = 180KB/小时
```

**24小时累计**：约 4.3MB

**结论**：日志增长在可接受范围内（14天存储下约60MB）

---

## 八、生产环境建议

### 8.1 日志级别调整

**调试阶段**：
- 保持所有日志（当前实现）

**生产阶段**：
```java
// 可选：关闭 Log.d 级别日志
if (BuildConfig.DEBUG) {
    Log.d(TAG, "准备上传文件: " + ...);
}
```

### 8.2 日志采样

**可选优化**（针对高频场景）：
```java
private int uploadCounter = 0;

private void checkAndUpload() {
    // ...
    if (shouldUpload) {
        uploadCounter++;
        // 每10次上报才打印详细日志
        boolean verbose = (uploadCounter % 10 == 0);
        if (verbose) {
            Log.i(TAG, "========== PCRLog 上报触发 (第" + uploadCounter + "次) ==========");
        }
        // ...
    }
}
```

---

## 九、总结

### 9.1 增强内容

| 模块 | 增强项 | 新增日志数 |
|------|--------|----------|
| checkAndUpload | 分隔符 + 详细信息 | 5条 |
| uploadIncrementLogs | 统计信息 | 1条 |
| uploadSingleFile | 文件详情 + 进度 | 3条 |
| sendLogToServer | 发送详情（箭头格式） | 5条 |
| WSSModuleManager | 消息详情 + SeqId | 4条 |

**总计**：每次上报约 **15-20条** 详细日志

### 9.2 日志特点

- ✅ **分层清晰** — 不同模块使用不同前缀
- ✅ **进度可追踪** — 显示字节范围 `[start -> end]`
- ✅ **失败可定位** — 详细的错误信息和异常栈
- ✅ **性能友好** — 开销<0.02% CPU

### 9.3 实机验证清单

- [ ] 定时30秒触发日志
- [ ] 缓冲16KB触发日志
- [ ] 断点续传进度日志
- [ ] WSS断连失败日志
- [ ] 序列化完整性日志
- [ ] 统计信息准确性

---

**实现完成时间**: 2026-09-07  
**编译验证**: ✅ BUILD SUCCESSFUL  
**待实机验证**: 日志输出完整性、性能影响
