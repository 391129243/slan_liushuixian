# WebSocketServer 消息处理队列分析与优化建议

## 当前实现分析

### 线程池配置

**位置：** `app/src/main/java/com/winters/boweilibrary/autopipeline/wssserver/WebSocketServer.java:26`

```java
private static final ExecutorService messageExecutor = Executors.newSingleThreadExecutor();
```

### 使用方式

**文本消息处理：**
```java
@Override
public void onMessage(WebSocket conn, String message) {
    messageExecutor.execute(() -> {
        EventBus.getDefault().post(new WSSMessageEvent(...));
    });
}
```

**二进制消息处理：**
```java
@Override
public void onMessage(WebSocket conn, ByteBuffer message) {
    ByteBuffer msgCopy = ByteBuffer.allocate(message.remaining());
    msgCopy.put(message);
    msgCopy.flip();
    
    messageExecutor.execute(() -> {
        BinBaseMessage binMsg = BinWssMessageUtils.parseBinMessage(msgCopy);
        EventBus.getDefault().post(new WSSMessageEvent(...));
    });
}
```

---

## 潜在问题分析

### 问题1：单线程 + 无界队列

**现象：**
- `Executors.newSingleThreadExecutor()` 使用 `LinkedBlockingQueue`（无界队列）
- 所有消息排队等待单线程串行处理
- 如果处理速度 < 消息到达速度，队列无限增长

**风险：**
1. **内存溢出 (OOM)**
   - 队列无限增长消耗内存
   - 特别是大的二进制消息（PCRFinish、日志文件）

2. **消息延迟增加**
   - 队列越长，消息等待时间越长
   - 实时性下降

3. **可能导致丢包**
   - 内存不足时，系统可能 crash
   - 连接可能超时断开

### 问题2：消息处理时间长

**可能的耗时操作：**
1. **EventBus.post()** - 同步发送到所有订阅者
2. **二进制消息解析** - `BinWssMessageUtils.parseBinMessage()`
3. **订阅者处理** - `WSSModuleManager.onEvent()`
   - SLAN_PCRFinishMessage 保存文件（耗时）
   - RNALogMessage/PCRLogMessage 保存日志（耗时）

**举例：**
```
消息到达速度：100条/秒
单条处理时间：50ms
处理能力：1000ms / 50ms = 20条/秒

积压速度：100 - 20 = 80条/秒
1分钟后：80 * 60 = 4800条 积压
```

### 问题3：ByteBuffer 拷贝开销

```java
ByteBuffer msgCopy = ByteBuffer.allocate(message.remaining());
msgCopy.put(message);
msgCopy.flip();
```

**问题：**
- 每个二进制消息都要完整拷贝一次
- 大消息（如 PCRFinish 几MB）拷贝开销大

---

## 优化方案

### 方案1：有界队列 + 拒绝策略（推荐）

**目标：** 防止内存溢出，牺牲部分消息

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy;

// 有界队列：最多缓存 1000 条消息
private static final int QUEUE_CAPACITY = 1000;

private static final ExecutorService messageExecutor = new ThreadPoolExecutor(
    1,                          // 核心线程数
    1,                          // 最大线程数（保持单线程）
    0L,                         // 空闲线程存活时间
    TimeUnit.MILLISECONDS,
    new ArrayBlockingQueue<>(QUEUE_CAPACITY),  // 有界队列
    new DiscardOldestPolicy()   // 队列满时，丢弃最旧的消息
);

// 或者使用自定义拒绝策略，记录日志
private static final RejectedExecutionHandler REJECT_HANDLER = new RejectedExecutionHandler() {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        Log.e("WebSocketServer", "【队列满】消息被丢弃！当前队列大小: " + executor.getQueue().size());
        // 可以选择记录到文件或上报
    }
};

private static final ExecutorService messageExecutor = new ThreadPoolExecutor(
    1, 1, 0L, TimeUnit.MILLISECONDS,
    new ArrayBlockingQueue<>(QUEUE_CAPACITY),
    REJECT_HANDLER
);
```

**优点：**
- 防止内存溢出
- 队列满时主动丢弃，而不是 crash
- 可以监控队列大小

**缺点：**
- 可能丢失消息

---

### 方案2：增加线程数（谨慎）

```java
// 改为多线程池，提高并发处理能力
private static final ExecutorService messageExecutor = Executors.newFixedThreadPool(4);
```

**优点：**
- 提高处理能力

**缺点：**
- **破坏消息顺序！**
- 可能导致业务逻辑错乱
- 需要业务层保证顺序性（如加锁）

**不推荐**，除非业务层不依赖消息顺序。

---

### 方案3：区分消息类型，分级处理

```java
// 关键消息（PCRFinish、RNAFinish）- 高优先级队列
private static final ExecutorService criticalExecutor = new ThreadPoolExecutor(
    1, 1, 0L, TimeUnit.MILLISECONDS,
    new ArrayBlockingQueue<>(100),
    new ThreadPoolExecutor.CallerRunsPolicy()  // 队列满时，由调用线程执行（阻塞）
);

// 日志消息（PCRLog、RNALog）- 低优先级队列，允许丢弃
private static final ExecutorService logExecutor = new ThreadPoolExecutor(
    1, 1, 0L, TimeUnit.MILLISECONDS,
    new ArrayBlockingQueue<>(1000),
    new ThreadPoolExecutor.DiscardOldestPolicy()  // 队列满时，丢弃旧日志
);

@Override
public void onMessage(WebSocket conn, ByteBuffer message) {
    ByteBuffer msgCopy = ByteBuffer.allocate(message.remaining());
    msgCopy.put(message);
    msgCopy.flip();
    
    // 先快速判断消息类型（peek 头部字节）
    byte msgType = msgCopy.get(0);
    
    if (isCriticalMessage(msgType)) {
        // 关键消息用 criticalExecutor
        criticalExecutor.execute(() -> processMessage(msgCopy, conn));
    } else {
        // 日志消息用 logExecutor
        logExecutor.execute(() -> processMessage(msgCopy, conn));
    }
}
```

**优点：**
- 保证关键消息不丢失
- 日志消息可以适度丢弃

**缺点：**
- 实现复杂
- 需要快速识别消息类型

---

### 方案4：添加监控和告警

```java
// 定期检查队列大小
private static final ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);

static {
    monitor.scheduleAtFixedRate(() -> {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) messageExecutor;
        int queueSize = executor.getQueue().size();
        int activeCount = executor.getActiveCount();
        long completedTaskCount = executor.getCompletedTaskCount();
        
        Log.i("WebSocketServer", "【队列监控】队列大小: " + queueSize +
            ", 活跃线程: " + activeCount +
            ", 已完成任务: " + completedTaskCount);
        
        // 告警：队列超过 80% 容量
        if (queueSize > QUEUE_CAPACITY * 0.8) {
            Log.w("WebSocketServer", "【队列告警】队列接近满载: " + queueSize + "/" + QUEUE_CAPACITY);
        }
        
        // 告警：队列持续增长
        if (queueSize > lastQueueSize && queueSize > 100) {
            Log.w("WebSocketServer", "【队列告警】队列持续增长，可能处理不过来！");
        }
        
        lastQueueSize = queueSize;
    }, 10, 10, TimeUnit.SECONDS);  // 每 10 秒检查一次
}

private static int lastQueueSize = 0;
```

**优点：**
- 可以提前发现问题
- 便于调试和性能分析

---

### 方案5：优化 EventBus 处理（异步化）

当前 `EventBus.post()` 是**同步**的，会阻塞 messageExecutor 线程。

```java
// 将耗时的订阅者改为异步处理
@Subscribe(threadMode = ThreadMode.ASYNC)
public void onEvent(WSSMessageEvent messageEvent) {
    // 处理消息
}
```

**或者在 WSSModuleManager 中使用线程池：**
```java
@Subscribe(threadMode = ThreadMode.MAIN)
public void onEvent(WSSMessageEvent messageEvent) {
    // 快速提交到线程池，立即返回
    asyncExecutor.execute(() -> {
        // 实际处理逻辑
        processMessage(messageEvent);
    });
}
```

**优点：**
- 减少 messageExecutor 线程阻塞
- 提高吞吐量

**缺点：**
- 可能破坏消息顺序

---

## 推荐实施方案

### 第一阶段：监控（低风险）

1. 添加队列监控代码（方案4）
2. 观察队列大小变化
3. 统计消息处理时间
4. 评估是否真的有积压问题

### 第二阶段：优化（如果确认有问题）

**推荐组合：**
- **方案1（有界队列）** + **方案4（监控）** + **方案5（异步化耗时操作）**

```java
// 1. 有界队列
private static final int QUEUE_CAPACITY = 1000;
private static final ExecutorService messageExecutor = new ThreadPoolExecutor(
    1, 1, 0L, TimeUnit.MILLISECONDS,
    new ArrayBlockingQueue<>(QUEUE_CAPACITY),
    new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            Log.e("WebSocketServer", "【队列满】消息被丢弃！当前队列: " + executor.getQueue().size());
            rejectedCount.incrementAndGet();  // 统计丢弃数量
        }
    }
);

private static final AtomicInteger rejectedCount = new AtomicInteger(0);

// 2. 监控
private static final ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
static {
    monitor.scheduleAtFixedRate(() -> {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) messageExecutor;
        int queueSize = executor.getQueue().size();
        int rejected = rejectedCount.getAndSet(0);
        
        Log.i("WebSocketServer", "【队列监控】队列: " + queueSize + "/" + QUEUE_CAPACITY +
            ", 丢弃消息: " + rejected);
    }, 10, 10, TimeUnit.SECONDS);
}

// 3. WSSModuleManager 中异步化耗时操作（已经做了，pcrFinishExecutor）
```

---

## 代码实现示例

完整的优化代码：

```java
package com.winters.boweilibrary.autopipeline.wssserver;

import android.util.Log;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketServer extends org.java_websocket.server.WebSocketServer {

    private static final String TAG = "WebSocketServer";
    private static WebSocketServer websocketServer;
    
    // 队列容量：根据实际情况调整
    private static final int QUEUE_CAPACITY = 1000;
    
    // 统计被拒绝的消息数量
    private static final AtomicInteger rejectedCount = new AtomicInteger(0);
    private static int lastQueueSize = 0;
    
    // 有界队列 + 拒绝策略
    private static final ExecutorService messageExecutor = new ThreadPoolExecutor(
        1,                          // 单线程，保证消息顺序
        1,
        0L,
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(QUEUE_CAPACITY),  // 有界队列
        new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                int rejected = rejectedCount.incrementAndGet();
                Log.e(TAG, "【队列满】消息被丢弃！当前队列: " + executor.getQueue().size() +
                    ", 累计丢弃: " + rejected);
            }
        }
    );
    
    // 监控线程
    private static final ScheduledExecutorService monitorExecutor = Executors.newScheduledThreadPool(1);
    
    static {
        // 启动队列监控
        monitorExecutor.scheduleAtFixedRate(() -> {
            try {
                ThreadPoolExecutor executor = (ThreadPoolExecutor) messageExecutor;
                int queueSize = executor.getQueue().size();
                int activeCount = executor.getActiveCount();
                long completedTaskCount = executor.getCompletedTaskCount();
                int rejected = rejectedCount.get();
                
                // 定期输出监控信息
                Log.i(TAG, "【队列监控】队列: " + queueSize + "/" + QUEUE_CAPACITY +
                    ", 活跃: " + activeCount +
                    ", 完成: " + completedTaskCount +
                    ", 丢弃: " + rejected);
                
                // 告警：队列超过 80% 容量
                if (queueSize > QUEUE_CAPACITY * 0.8) {
                    Log.w(TAG, "【队列告警】队列接近满载: " + queueSize + "/" + QUEUE_CAPACITY);
                }
                
                // 告警：队列持续增长
                if (queueSize > lastQueueSize && queueSize > 100) {
                    Log.w(TAG, "【队列告警】队列持续增长，可能处理不过来！当前: " + queueSize + ", 上次: " + lastQueueSize);
                }
                
                lastQueueSize = queueSize;
                
            } catch (Exception e) {
                Log.e(TAG, "监控线程异常: " + e.getMessage());
            }
        }, 10, 10, TimeUnit.SECONDS);  // 每 10 秒检查一次
    }
    
    // ... 其他代码保持不变
}
```

---

## 验证方法

### 1. 查看队列监控日志
```bash
adb logcat | grep "队列监控"
```

**预期输出：**
```
I/WebSocketServer: 【队列监控】队列: 5/1000, 活跃: 1, 完成: 12345, 丢弃: 0
I/WebSocketServer: 【队列监控】队列: 8/1000, 活跃: 1, 完成: 12350, 丢弃: 0
```

### 2. 查看队列告警
```bash
adb logcat | grep "队列告警"
```

**如果出现：**
```
W/WebSocketServer: 【队列告警】队列接近满载: 850/1000
W/WebSocketServer: 【队列告警】队列持续增长，可能处理不过来！当前: 850, 上次: 800
```
说明处理能力不足，需要进一步优化。

### 3. 查看消息丢弃
```bash
adb logcat | grep "队列满"
```

**如果出现：**
```
E/WebSocketServer: 【队列满】消息被丢弃！当前队列: 1000, 累计丢弃: 1
```
说明队列已满，开始丢弃消息。

---

## 总结

### 当前风险

- ✅ **单线程 + 无界队列**：可能导致内存溢出
- ⚠️ **无监控**：不知道是否有积压问题
- ⚠️ **无拒绝策略**：内存不足时可能 crash

### 推荐方案

1. **立即实施**：添加队列监控（低风险，高收益）
2. **观察一段时间**：确认是否真的有积压问题
3. **如果有问题**：实施有界队列 + 拒绝策略

### 注意事项

- **保持单线程**很重要，不要轻易改为多线程
- **丢消息总比 crash 好**
- **监控是关键**，能提前发现问题

---

**创建时间：** 2026-09-07  
**相关文件：** WebSocketServer.java, WSSModuleManager.java
