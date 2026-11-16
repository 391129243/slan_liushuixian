# PCRLog 二进制消息格式修复

> **问题**: Server 端报错 "Header not match: 7b225479"  
> **原因**: Client 端序列化格式与 Server 端解析格式不匹配  
> **修复时间**: 2026-09-07

---

## 一、问题分析

### 1.1 错误信息

```
Header not match: 7b225479
```

**16进制解析**：
- `7b` = `{`
- `22` = `"`
- `54` = `T`
- `79` = `y`

合起来是 `{"Ty`，说明 Server 端收到了 JSON 开头，但期望的是魔数 `0x5AA5FCFF`。

### 1.2 原始格式（错误）

**Client 端发送**：
```
[JSON头部] + [\r\n\r\n分隔符] + [二进制数据]
```

**示例**：
```
{"Type":"PCRLog","SeqID":123,...}\r\n\r\n[binary data]
```

### 1.3 Server 端期望格式

**Server 端解析逻辑**（`BinWssMessageUtils.parseBinMessage`）：
```java
// 1. 读取前4字节作为魔数
byte[] headerBytes = new byte[4];
byteBuffer.get(headerBytes);
int header = 大端转int(headerBytes);
if (header != 0x5AA5FCFF) {
    Log.e("parseBinMessage", "Header not match: " + Integer.toHexString(header));
    return null;
}

// 2. 读取4字节JSON长度
int jsonLen = 读取大端int();

// 3. 读取JSON数据
byte[] jsonBytes = new byte[jsonLen];
String jsonStr = new String(jsonBytes, UTF-8);

// 4. 读取文件数据
int fileLen = jsonObj.optInt("FileSize");
byte[] fileBytes = new byte[fileLen];

// 5. 校验MD5
String checkSum = jsonObj.optString("CheckSum");
String fileMd5 = calculateMD5(fileBytes);
if (!checkSum.equals(fileMd5)) {
    // 校验失败
}
```

**正确格式**：
```
[4字节魔数: 0x5AA5FCFF] + [4字节JSON长度] + [JSON数据] + [二进制文件数据]
```

---

## 二、修复方案

### 2.1 修改序列化方法

**文件**: `WSSModuleManager.java`

**修复前**：
```java
private ByteBuffer serializeBinMessage(BinBaseMessage msg) {
    String jsonHeader = String.format(...);
    byte[] headerBytes = jsonHeader.getBytes("UTF-8");
    byte[] separator = "\r\n\r\n".getBytes("UTF-8");
    byte[] fileData = msg.getFileData();
    
    ByteBuffer buffer = ByteBuffer.allocate(headerBytes.length + separator.length + fileData.length);
    buffer.put(headerBytes);
    buffer.put(separator);
    buffer.put(fileData);
    buffer.flip();
    return buffer;
}
```

**修复后**：
```java
private ByteBuffer serializeBinMessage(BinBaseMessage msg) {
    // 0. 计算文件数据的 MD5
    byte[] fileData = msg.getFileData();
    String md5 = calculateMD5(fileData);
    msg.setCheckSum(md5);

    // 1. 构造JSON（包含CheckSum）
    String jsonHeader = String.format(
        "{\"Type\":\"%s\",\"SeqID\":%d,...,\"CheckSum\":\"%s\"}",
        ..., md5
    );
    byte[] jsonBytes = jsonHeader.getBytes("UTF-8");
    int jsonLen = jsonBytes.length;

    // 2. 魔数（大端序）: 0x5AA5FCFF
    byte[] magicBytes = new byte[]{
        (byte) 0x5A, (byte) 0xA5, (byte) 0xFC, (byte) 0xFF
    };

    // 3. JSON长度（大端序）
    byte[] jsonLenBytes = intToBigEndian(jsonLen);

    // 4. 组装：魔数 + JSON长度 + JSON + 文件数据
    ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + jsonLen + fileData.length);
    buffer.put(magicBytes);
    buffer.put(jsonLenBytes);
    buffer.put(jsonBytes);
    buffer.put(fileData);
    buffer.flip();
    
    return buffer;
}
```

### 2.2 新增辅助方法

#### intToBigEndian（大端转换）

```java
private byte[] intToBigEndian(int value) {
    return new byte[]{
        (byte) ((value >> 24) & 0xFF),
        (byte) ((value >> 16) & 0xFF),
        (byte) ((value >> 8) & 0xFF),
        (byte) (value & 0xFF)
    };
}
```

**作用**：将 int 转换为大端字节数组（网络字节序）

#### calculateMD5（MD5计算）

```java
private String calculateMD5(byte[] data) {
    try {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    } catch (Exception e) {
        Log.e(TAG, "计算MD5异常", e);
        return "";
    }
}
```

**作用**：计算字节数组的 MD5 校验和（小写16进制）

---

## 三、格式对比

### 3.1 修复前后格式对比

| 组成部分 | 修复前 | 修复后 |
|---------|-------|--------|
| **魔数** | ❌ 无 | ✅ 0x5AA5FCFF（4字节） |
| **JSON长度** | ❌ 无 | ✅ 大端int（4字节） |
| **JSON数据** | ✅ 有（但无CheckSum） | ✅ 有（含CheckSum） |
| **分隔符** | ❌ `\r\n\r\n`（4字节） | ✅ 无 |
| **文件数据** | ✅ 有 | ✅ 有 |

### 3.2 实际字节示例

**假设**：
- JSON: `{"Type":"PCRLog",...,"CheckSum":"abc123"}`（100字节）
- 文件数据: 16384字节

**修复前**：
```
Position  Content
0-99      {"Type":"PCRLog",...}
100-103   \r\n\r\n
104-16487 [binary data]
```
**总计**: 16488字节

**修复后**：
```
Position  Content
0-3       0x5A 0xA5 0xFC 0xFF (魔数)
4-7       0x00 0x00 0x00 0x64 (JSON长度=100，大端)
8-107     {"Type":"PCRLog",...,"CheckSum":"abc123"}
108-16491 [binary data]
```
**总计**: 16492字节

---

## 四、MD5 校验

### 4.1 为什么需要 MD5

Server 端代码（line 257-261）：
```java
String checkSum = jsonObj.optString("CheckSum", null);
String fileMd5 = getBytesMD5(fileBytes);
boolean md5Match = checkSum != null && checkSum.equalsIgnoreCase(fileMd5);

if (md5Match && Type.equals("PCRLog")) {
    // 解析成功
}
```

**必须校验通过**，否则 Server 端返回 null，不处理消息。

### 4.2 计算时机

```java
// 序列化时自动计算
String md5 = calculateMD5(fileData);
msg.setCheckSum(md5);
```

**优势**：
- 保证 JSON 中的 CheckSum 与实际文件数据一致
- 自动更新，无需手动调用

### 4.3 校验流程

```
Client                          Server
  ↓                               ↓
计算 MD5                      收到消息
  ↓                               ↓
放入 JSON.CheckSum            读取 JSON.CheckSum
  ↓                               ↓
序列化发送                     计算文件数据 MD5
  ↓                               ↓
                              比较两个 MD5
                                  ↓
                            相同 → 解析成功
                            不同 → 返回 null
```

---

## 五、日志输出增强

### 5.1 新增 MD5 日志

**修复后**：
```
PCRLog消息详情: SeqId=123, DeviceID=PCR1111, Position=1, FileSize=16384, CheckSum=abc123...
序列化完成: 总大小=16492 字节 (魔数4B+JSON长度4B+JSON+数据)
序列化: 魔数=4B, JSON长度=4B, JSON=100B, 文件=16384B, 总计=16492B
```

### 5.2 调试建议

**验证魔数**：
```bash
# 抓取 WSS 发送的数据
adb shell tcpdump -i wlan0 -s 0 -w /sdcard/wss.pcap

# 用 Wireshark 打开，查看前4字节
# 期望: 5A A5 FC FF
```

**验证 MD5**：
```bash
# Server 端日志
grep "fileMd5" server.log
# 期望: md5Match=true
```

---

## 六、兼容性说明

### 6.1 与 Finish 消息的关系

**Finish 消息**也使用相同的二进制格式，需要检查是否也需要修复。

查找 Finish 消息序列化：
```bash
grep -n "sendFinishResultWithAck" WSSModuleManager.java
```

**建议**：统一使用 `serializeBinMessage` 方法。

### 6.2 向后兼容

**旧 Server 端**（如果存在）：
- 无法解析新格式（因为期望魔数）

**新 Server 端**：
- 只接受新格式（有魔数校验）

**结论**：Client 和 Server 必须同时更新。

---

## 七、测试验证

### 7.1 单元测试（建议）

```java
@Test
public void testSerializeBinMessage() {
    PCRLogMessage msg = new PCRLogMessage();
    msg.setType("PCRLog");
    msg.setSeqId(123);
    msg.setFileName("test.log");
    msg.setFileSize(10);
    msg.setFileData("0123456789".getBytes());
    
    ByteBuffer buffer = serializeBinMessage(msg);
    
    // 验证魔数
    byte[] magic = new byte[4];
    buffer.get(magic);
    assertEquals(0x5A, magic[0] & 0xFF);
    assertEquals(0xA5, magic[1] & 0xFF);
    assertEquals(0xFC, magic[2] & 0xFF);
    assertEquals(0xFF, magic[3] & 0xFF);
    
    // 验证 JSON 长度
    byte[] jsonLenBytes = new byte[4];
    buffer.get(jsonLenBytes);
    int jsonLen = bigEndianToInt(jsonLenBytes);
    assertTrue(jsonLen > 0);
    
    // 验证 JSON 包含 CheckSum
    byte[] jsonBytes = new byte[jsonLen];
    buffer.get(jsonBytes);
    String json = new String(jsonBytes, "UTF-8");
    assertTrue(json.contains("CheckSum"));
}
```

### 7.2 集成测试

**步骤**：
1. 启动 Client 应用
2. 触发日志上报（等待30秒或产生16KB日志）
3. 观察 Client 日志：`PCRLog消息发送成功`
4. 观察 Server 日志：`parseBinMessage succeed, md5Match=true`

**期望**：
- Client: 无 "发送失败" 日志
- Server: 无 "Header not match" 日志

---

## 八、排查指南

### 8.1 如果仍然报 "Header not match"

**可能原因**：
1. 魔数字节序错误（大端 vs 小端）
2. 数据被截断或损坏
3. WebSocket 分包问题

**排查**：
```bash
# Client 日志
grep "序列化完成" client.log
# 期望: 魔数=4B

# Server 日志
grep "Header not match" server.log
# 查看错误的16进制值
```

### 8.2 如果报 "md5Match=false"

**可能原因**：
1. 文件数据传输损坏
2. MD5 计算算法不一致

**排查**：
```bash
# Client 日志
grep "CheckSum=" client.log
# 记录 Client 计算的 MD5

# Server 日志
grep "fileMd5=" server.log
# 对比 Server 计算的 MD5
```

---

## 九、总结

### 9.1 修复内容

| 项目 | 修复前 | 修复后 |
|------|-------|--------|
| **消息格式** | JSON + `\r\n\r\n` + 数据 | 魔数 + JSON长度 + JSON + 数据 |
| **魔数** | ❌ 无 | ✅ 0x5AA5FCFF |
| **JSON长度** | ❌ 无 | ✅ 大端int（4字节） |
| **MD5校验** | ❌ 无 | ✅ 计算并添加到JSON |
| **字节序** | - | ✅ 大端（网络字节序） |

### 9.2 新增方法

- `intToBigEndian(int)` — int转大端字节数组
- `calculateMD5(byte[])` — 计算MD5校验和

### 9.3 编译验证

```bash
./gradlew :app:compileDebugJavaWithJavac --no-daemon

# 结果
BUILD SUCCESSFUL in 35s
```

✅ 编译通过

---

**修复完成时间**: 2026-09-07  
**问题根因**: 序列化格式不匹配  
**解决方案**: 匹配 Server 端解析格式（魔数 + JSON长度 + JSON + 数据 + MD5）  
**待实机验证**: Server 端不再报 "Header not match"，md5Match=true
