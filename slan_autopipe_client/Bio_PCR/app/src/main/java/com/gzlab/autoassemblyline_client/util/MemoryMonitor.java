package com.gzlab.autoassemblyline_client.util;

import android.os.Debug;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 内存监控：周期性把进程内存按类别打印到日志（会被 LogcatHelper 以 *:I 抓取落盘）。
 *
 * 设计目标：能分清"哪一部分内存在涨、涨了多少、是否不回落"。
 * 每次快照都与上一次对比，输出 Δ 增量：
 *   - JavaHeap  : Dalvik/ART 堆（Java 对象泄漏会在这里涨，受 maxMemory 限制，涨满即 OOM）
 *   - NativeHeap: native 堆（SDK native 层、旧版本 Bitmap、so 库分配）
 *   - PSS 分类  : java / native / code / graphics / stack / system / total（整体物理占用）
 *   - Process   : VmRSS（进程实际占用物理内存）、VmSwap（被换出）、VmSize（虚拟地址空间）
 *   - Threads   : 线程总数 + 按线程名归类的明细（某类线程只增不减 → 线程泄漏）
 *   - FD        : 文件描述符总数 + 按类型归类（socket/pipe/文件，某类只增 → 对应资源未关闭）
 *
 * 说明：无法统计"单个线程占用多少堆内存"——线程共享同一进程地址空间（同一 Java 堆、
 * 同一 native 堆），内存按进程计量，不存在按线程拆分的堆占用。每线程唯一独占的是栈
 * （固定几百 KB）。因此这里给出的是线程"数量与分类 + 每线程 CPU 时间"，这才是定位
 * 线程泄漏/空转的有效信息。
 *
 * 用 Log.i(TAG) 输出，便于用 "MemoryMonitor" 关键字过滤查看增长趋势。
 */
public final class MemoryMonitor {

    public static final String TAG = "MemoryMonitor";

    /** 默认采样周期：60 秒 */
    private static final long DEFAULT_INTERVAL_MS = 60 * 1000L;
    private static final long KB = 1024L;

    private static volatile MemoryMonitor sInstance;

    private ScheduledExecutorService mExecutor;
    private ScheduledFuture<?> mFuture;
    private volatile boolean mRunning = false;

    private int mSnapshotIndex = 0;
    private final long mStartElapsedMs = SystemClock.elapsedRealtime();

    // 上一次快照，用于计算 Δ 增量（单位：KB / 个）
    private long mPrevJavaUsedKb = -1;
    private long mPrevNativeAllocKb = -1;
    private long mPrevTotalPssKb = -1;
    private long mPrevVmRssKb = -1;
    private int mPrevThreadCount = -1;
    private int mPrevFdCount = -1;

    // 上一次各类线程数量（key=线程名归一化前缀），用于标出哪类线程在增长
    private final Map<String, Integer> mPrevThreadGroup = new HashMap<>();

    private MemoryMonitor() {
    }

    public static MemoryMonitor getInstance() {
        if (sInstance == null) {
            synchronized (MemoryMonitor.class) {
                if (sInstance == null) {
                    sInstance = new MemoryMonitor();
                }
            }
        }
        return sInstance;
    }

    public void start() {
        start(DEFAULT_INTERVAL_MS);
    }

    /**
     * 启动周期性采样。重复调用安全（已运行则忽略）。
     *
     * @param intervalMs 采样周期（毫秒）
     */
    public synchronized void start(long intervalMs) {
        if (mRunning) {
            Log.w(TAG, "内存监控已在运行，忽略重复启动");
            return;
        }
        if (intervalMs <= 0) {
            intervalMs = DEFAULT_INTERVAL_MS;
        }
        mRunning = true;
        mExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable r) {
                Thread t = new Thread(r, "MemoryMonitor");
                t.setDaemon(true);
                return t;
            }
        });
        final long periodMs = intervalMs;
        // 延迟 5 秒开始，给启动阶段的内存分配一点稳定时间
        mFuture = mExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    dumpOnce();
                } catch (Throwable t) {
                    // 监控本身绝不能把 APP 拖崩
                    Log.e(TAG, "采样异常", t);
                }
            }
        }, 5000, periodMs, TimeUnit.MILLISECONDS);
        Log.i(TAG, "内存监控已启动，采样周期=" + (periodMs / 1000) + "秒");
    }

    public synchronized void stop() {
        mRunning = false;
        if (mFuture != null) {
            mFuture.cancel(false);
            mFuture = null;
        }
        if (mExecutor != null) {
            mExecutor.shutdownNow();
            mExecutor = null;
        }
        Log.i(TAG, "内存监控已停止");
    }
    /** 立即采样一次并打印（也可在关键节点手动调用，比如实验开始/结束前后对比）。 */
    public void dumpOnce() {
        mSnapshotIndex++;
        long runningMin = (SystemClock.elapsedRealtime() - mStartElapsedMs) / 60000L;

        // ---------- Java 堆（ART/Dalvik）----------
        Runtime rt = Runtime.getRuntime();
        long javaMaxKb = rt.maxMemory() / KB;          // 堆上限，逼近即 OOM
        long javaTotalKb = rt.totalMemory() / KB;      // 已向系统申请的堆
        long javaFreeKb = rt.freeMemory() / KB;        // 堆内空闲
        long javaUsedKb = javaTotalKb - javaFreeKb;    // 实际使用的 Java 对象内存
        int javaUsedPct = javaMaxKb > 0 ? (int) (javaUsedKb * 100 / javaMaxKb) : 0;

        // ---------- Native 堆 ----------
        long nativeTotalKb = Debug.getNativeHeapSize() / KB;
        long nativeAllocKb = Debug.getNativeHeapAllocatedSize() / KB;
        long nativeFreeKb = Debug.getNativeHeapFreeSize() / KB;

        // ---------- PSS 分类（整体物理内存占用，相对昂贵，随采样一起做）----------
        Debug.MemoryInfo mi = new Debug.MemoryInfo();
        Debug.getMemoryInfo(mi);
        long totalPssKb = mi.getTotalPss();
        long javaPssKb = parseStat(mi, "summary.java-heap");
        long nativePssKb = parseStat(mi, "summary.native-heap");
        long codePssKb = parseStat(mi, "summary.code");
        long stackPssKb = parseStat(mi, "summary.stack");
        long graphicsPssKb = parseStat(mi, "summary.graphics");
        long privateOtherKb = parseStat(mi, "summary.private-other");
        long systemPssKb = parseStat(mi, "summary.system");

        // ---------- 进程级内存（/proc/self/status，更贴近真实物理占用）----------
        long vmRssKb = readProcStatusKb("VmRSS");     // 实际占用的物理内存
        long vmSwapKb = readProcStatusKb("VmSwap");   // 被换出到 swap 的部分
        long vmSizeKb = readProcStatusKb("VmSize");   // 虚拟地址空间（native 泄漏时也会涨）

        // ---------- 线程 / 文件描述符 ----------
        int threadCount = countProcThreads();         // 以 /proc/self/task 为准，比 Thread.activeCount() 全面
        int fdCount = countOpenFileDescriptors();

        // ---------- 打印：先总览，再分类，带 Δ 增量 ----------
        Log.i(TAG, String.format(Locale.US,
                "===== 内存快照 #%d | 运行%d分钟 =====", mSnapshotIndex, runningMin));

        Log.i(TAG, String.format(Locale.US,
                "[JavaHeap ] used=%dKB/%dKB (%d%%)  total=%dKB free=%dKB %s",
                javaUsedKb, javaMaxKb, javaUsedPct, javaTotalKb, javaFreeKb,
                delta(javaUsedKb, mPrevJavaUsedKb, "KB")));

        Log.i(TAG, String.format(Locale.US,
                "[Native   ] alloc=%dKB  total=%dKB free=%dKB %s",
                nativeAllocKb, nativeTotalKb, nativeFreeKb,
                delta(nativeAllocKb, mPrevNativeAllocKb, "KB")));

        Log.i(TAG, String.format(Locale.US,
                "[PSS total] %dKB(%.1fMB) %s",
                totalPssKb, totalPssKb / 1024f,
                delta(totalPssKb, mPrevTotalPssKb, "KB")));

        Log.i(TAG, String.format(Locale.US,
                "[PSS 分类 ] java=%dKB native=%dKB code=%dKB graphics=%dKB stack=%dKB other=%dKB system=%dKB",
                javaPssKb, nativePssKb, codePssKb, graphicsPssKb, stackPssKb, privateOtherKb, systemPssKb));

        Log.i(TAG, String.format(Locale.US,
                "[Process  ] VmRSS=%dKB(%.1fMB) %s  VmSwap=%dKB  VmSize=%dKB(%.1fMB)",
                vmRssKb, vmRssKb / 1024f, delta(vmRssKb, mPrevVmRssKb, "KB"),
                vmSwapKb, vmSizeKb, vmSizeKb / 1024f));

        Log.i(TAG, String.format(Locale.US,
                "[Threads  ] count=%d %s   [FD] count=%d %s",
                threadCount, delta(threadCount, mPrevThreadCount, "个"),
                fdCount, delta(fdCount, mPrevFdCount, "个")));

        // 线程按名字归类打印：某类线程只增不减即线程泄漏。附每类累计 CPU 时间(jiffies)，可看谁在空转/狂跑。
        dumpThreadBreakdown();
        // FD 按类型归类：socket / pipe / 普通文件 / anon，某类只增不减 → 对应资源未关闭
        dumpFdBreakdown();

        // 刷新上一次快照
        mPrevJavaUsedKb = javaUsedKb;
        mPrevNativeAllocKb = nativeAllocKb;
        mPrevTotalPssKb = totalPssKb;
        mPrevVmRssKb = vmRssKb;
        mPrevThreadCount = threadCount;
        mPrevFdCount = fdCount;
    }

    /** 格式化 Δ 增量；首次无基线时返回空串。涨用 +、跌用 -，便于一眼看出是否"只涨不回落"。 */
    private String delta(long cur, long prev, String unit) {
        if (prev < 0) {
            return "(基线)";
        }
        long d = cur - prev;
        if (d == 0) {
            return "(Δ 0" + unit + ")";
        }
        return String.format(Locale.US, "(Δ %+d%s)", d, unit);
    }

    /** 读取 getMemoryStats 中的某一项，解析失败返回 -1。 */
    private long parseStat(Debug.MemoryInfo mi, String key) {
        try {
            String v = mi.getMemoryStat(key);
            if (v == null) {
                return -1;
            }
            return Long.parseLong(v.trim());
        } catch (Throwable t) {
            return -1;
        }
    }
    /** 统计当前进程打开的文件描述符数量（/proc/self/fd）。失败返回 -1。 */
    private int countOpenFileDescriptors() {
        try {
            File fdDir = new File("/proc/self/fd");
            String[] fds = fdDir.list();
            return fds != null ? fds.length : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    /** 以 /proc/self/task 统计线程数，比 Thread.activeCount()（只统计当前 ThreadGroup）更全面。 */
    private int countProcThreads() {
        try {
            File taskDir = new File("/proc/self/task");
            String[] tasks = taskDir.list();
            if (tasks != null) {
                return tasks.length;
            }
        } catch (Throwable ignore) {
        }
        return Thread.activeCount();
    }

    /** 读取 /proc/self/status 中某字段的 KB 值（如 VmRSS / VmSwap / VmSize）。失败返回 -1。 */
    private long readProcStatusKb(String field) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/self/status"));
            String line;
            String prefix = field + ":";
            while ((line = br.readLine()) != null) {
                if (line.startsWith(prefix)) {
                    // 形如 "VmRSS:   123456 kB"
                    String digits = line.replaceAll("[^0-9]", "");
                    if (digits.isEmpty()) {
                        return -1;
                    }
                    return Long.parseLong(digits);
                }
            }
        } catch (Throwable ignore) {
        } finally {
            closeQuietly(br);
        }
        return -1;
    }

    /**
     * 按线程名归类打印：同名/同前缀的线程聚合计数，并与上次对比标出 (Δ+n)。
     * 线程名里的数字后缀（pool-12-thread-3 等）会被归一化，便于聚合同类线程。
     * 附每类累计 CPU jiffies，突增的类说明该类线程在忙/空转。
     */
    private void dumpThreadBreakdown() {
        try {
            File taskDir = new File("/proc/self/task");
            String[] tids = taskDir.list();
            if (tids == null || tids.length == 0) {
                return;
            }
            // key = 归一化线程名，value = [数量, 累计CPU jiffies]
            Map<String, long[]> groups = new TreeMap<>();
            for (String tid : tids) {
                String name = readThreadName(tid);
                long cpu = readThreadCpuJiffies(tid);
                String key = normalizeThreadName(name);
                long[] v = groups.get(key);
                if (v == null) {
                    v = new long[]{0, 0};
                    groups.put(key, v);
                }
                v[0]++;
                if (cpu > 0) {
                    v[1] += cpu;
                }
            }

            StringBuilder sb = new StringBuilder("[Thread明细] ");
            Map<String, Integer> curGroup = new HashMap<>();
            for (Map.Entry<String, long[]> e : groups.entrySet()) {
                int count = (int) e.getValue()[0];
                long cpu = e.getValue()[1];
                curGroup.put(e.getKey(), count);
                Integer prev = mPrevThreadGroup.get(e.getKey());
                String mark = "";
                if (prev != null && count != prev) {
                    mark = String.format(Locale.US, "(Δ%+d)", count - prev);
                }
                sb.append(e.getKey()).append("×").append(count).append(mark)
                        .append("[cpu").append(cpu).append("] ");
            }
            Log.i(TAG, sb.toString());

            mPrevThreadGroup.clear();
            mPrevThreadGroup.putAll(curGroup);
        } catch (Throwable t) {
            Log.w(TAG, "dumpThreadBreakdown 失败: " + t.getMessage());
        }
    }

    private String readThreadName(String tid) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/self/task/" + tid + "/comm"));
            String name = br.readLine();
            return name != null ? name.trim() : "?";
        } catch (Throwable ignore) {
            return "?";
        } finally {
            closeQuietly(br);
        }
    }

    /** 读取线程累计 CPU 时间(utime+stime，单位 jiffies)，取自 /proc/self/task/<tid>/stat。 */
    private long readThreadCpuJiffies(String tid) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/self/task/" + tid + "/stat"));
            String line = br.readLine();
            if (line == null) {
                return -1;
            }
            // stat 的 comm 字段用括号包裹且可能含空格，从最后一个 ')' 之后开始切分
            int idx = line.lastIndexOf(')');
            if (idx < 0 || idx + 2 >= line.length()) {
                return -1;
            }
            String[] f = line.substring(idx + 2).split(" ");
            // 从 ')' 后算起：state(0) ... utime 是第 12 项、stime 第 13 项（0-based：11、12）
            if (f.length > 12) {
                return Long.parseLong(f[11]) + Long.parseLong(f[12]);
            }
        } catch (Throwable ignore) {
        } finally {
            closeQuietly(br);
        }
        return -1;
    }

    /** 归一化线程名：去掉数字后缀，使 pool-1-thread-2 / pool-1-thread-3 聚为一类。 */
    private String normalizeThreadName(String name) {
        if (name == null || name.isEmpty()) {
            return "?";
        }
        // 把连续数字替换为 #，合并同类线程
        return name.replaceAll("\\d+", "#");
    }
    /**
     * FD 按类型归类打印：读取 /proc/self/fd 下每个符号链接的目标，归类为
     * socket / pipe / anon_inode / 普通文件（按目录），某类只增不减 → 对应资源未关闭。
     */
    private void dumpFdBreakdown() {
        try {
            File fdDir = new File("/proc/self/fd");
            String[] fds = fdDir.list();
            if (fds == null) {
                return;
            }
            Map<String, Integer> types = new TreeMap<>();
            for (String fd : fds) {
                String target = readLinkTarget("/proc/self/fd/" + fd);
                String type = classifyFd(target);
                Integer c = types.get(type);
                types.put(type, c == null ? 1 : c + 1);
            }
            StringBuilder sb = new StringBuilder("[FD明细   ] ");
            for (Map.Entry<String, Integer> e : types.entrySet()) {
                sb.append(e.getKey()).append("×").append(e.getValue()).append(" ");
            }
            Log.i(TAG, sb.toString());
        } catch (Throwable t) {
            Log.w(TAG, "dumpFdBreakdown 失败: " + t.getMessage());
        }
    }

    private String readLinkTarget(String path) {
        try {
            File f = new File(path);
            // getCanonicalPath 会解析符号链接目标
            return f.getCanonicalPath();
        } catch (Throwable ignore) {
            return "?";
        }
    }

    private String classifyFd(String target) {
        if (target == null) {
            return "?";
        }
        if (target.contains("socket:")) {
            return "socket";
        }
        if (target.contains("pipe:")) {
            return "pipe";
        }
        if (target.contains("anon_inode:")) {
            return "anon";
        }
        if (target.startsWith("/dev/")) {
            return "dev";
        }
        if (target.endsWith(".jar") || target.endsWith(".apk") || target.endsWith(".dex")
                || target.endsWith(".so") || target.endsWith(".oat") || target.endsWith(".art")
                || target.endsWith(".vdex") || target.endsWith(".odex")) {
            return "lib/code";
        }
        if (target.contains("/Log/") || target.endsWith(".log")) {
            return "logfile";
        }
        return "file";
    }

    private void closeQuietly(BufferedReader br) {
        if (br != null) {
            try {
                br.close();
            } catch (Throwable ignore) {
            }
        }
    }
}
