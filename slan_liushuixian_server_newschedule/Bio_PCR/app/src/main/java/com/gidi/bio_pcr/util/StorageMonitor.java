package com.gidi.bio_pcr.util;

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.paul.pipeline.utils.WSSLogHelper;

import java.io.File;

/**
 * 磁盘空间监控器 - 防止磁盘空间耗尽
 */
public class StorageMonitor {

    private static final String TAG = "StorageMonitor";

    // 最小可用空间 (500MB)
    private static final long MIN_FREE_SPACE = 500 * 1024 * 1024L;

    // 警告可用空间 (1GB)
    private static final long WARNING_FREE_SPACE = 1024 * 1024 * 1024L;

    // 日志目录最大占用空间 (2GB)
    private static final long MAX_LOG_SPACE = 2L * 1024 * 1024 * 1024;

    private static StorageMonitor INSTANCE;

    // 熔断标志：当磁盘空间不足时，停止写入日志
    private volatile boolean logWriteCircuitBreakerOpen = false;

    // 上次检查时间
    private long lastCheckTime = 0;
    private static final long CHECK_INTERVAL = 30 * 1000; // 30秒检查一次

    private StorageMonitor() {
    }

    public static synchronized StorageMonitor getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new StorageMonitor();
        }
        return INSTANCE;
    }

    /**
     * 检查是否可以写入日志
     * @return true=可以写入，false=磁盘空间不足，拒绝写入
     */
    public boolean canWriteLog() {
        // 如果熔断器已打开，直接拒绝
        if (logWriteCircuitBreakerOpen) {
            long now = System.currentTimeMillis();
            // 每30秒尝试恢复一次
            if (now - lastCheckTime > CHECK_INTERVAL) {
                checkAndUpdateStatus();
            }
            return !logWriteCircuitBreakerOpen;
        }

        // 定期检查磁盘状态
        long now = System.currentTimeMillis();
        if (now - lastCheckTime > CHECK_INTERVAL) {
            checkAndUpdateStatus();
        }

        return !logWriteCircuitBreakerOpen;
    }

    /**
     * 检查并更新磁盘状态
     */
    private void checkAndUpdateStatus() {
        lastCheckTime = System.currentTimeMillis();

        long freeSpace = getAvailableSpace();

        if (freeSpace < MIN_FREE_SPACE) {
            // 磁盘空间严重不足，打开熔断器
            if (!logWriteCircuitBreakerOpen) {
                logWriteCircuitBreakerOpen = true;
                Log.e(TAG, "⚠️ 磁盘空间严重不足！可用空间: " + formatSize(freeSpace) +
                    "，最小要求: " + formatSize(MIN_FREE_SPACE) + "，停止写入日志！");

                // 尝试紧急清理
                emergencyCleanup();
            }
        } else if (freeSpace < WARNING_FREE_SPACE) {
            // 磁盘空间警告
            Log.w(TAG, "⚠️ 磁盘空间不足警告！可用空间: " + formatSize(freeSpace));

            // 尝试清理
            performCleanup();

            // 如果熔断器打开，且清理后空间充足，关闭熔断器
            if (logWriteCircuitBreakerOpen) {
                logWriteCircuitBreakerOpen = false;
                Log.i(TAG, "✅ 磁盘空间恢复，恢复日志写入");
            }
        } else {
            // 磁盘空间充足
            if (logWriteCircuitBreakerOpen) {
                logWriteCircuitBreakerOpen = false;
                Log.i(TAG, "✅ 磁盘空间恢复正常: " + formatSize(freeSpace));
            }
        }
    }

    /**
     * 获取可用磁盘空间（字节）
     */
    public long getAvailableSpace() {
        try {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long availableBytes = stat.getAvailableBytes();
            return availableBytes;
        } catch (Exception e) {
            Log.e(TAG, "获取磁盘空间失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 获取日志目录总大小
     */
    public long getLogDirectorySize() {
        try {
            String logRootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Log/";
            File logRoot = new File(logRootPath);
            return getDirSize(logRoot);
        } catch (Exception e) {
            Log.e(TAG, "获取日志目录大小失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 检查日志目录是否超过限制
     */
    public boolean isLogDirectoryOverLimit() {
        long logSize = getLogDirectorySize();
        return logSize > MAX_LOG_SPACE;
    }

    /**
     * 常规清理
     */
    private void performCleanup() {
        Log.i(TAG, "开始清理日志...");

        // 1. 清理 WSS 日志
        WSSLogHelper.getInstance().cleanExpiredLogs();

        // 2. 清理 Logcat 日志
        //LogcatHelper.getInstance().deleteSDcardExpiredLog();

        // 3. 清理其他旧日志
        cleanOldBinaryLogs();

        long logSize = getLogDirectorySize();
        Log.i(TAG, "清理完成，当前日志目录大小: " + formatSize(logSize));
    }

    /**
     * 紧急清理（磁盘空间严重不足）
     */
    private void emergencyCleanup() {
        Log.e(TAG, "⚠️ 开始紧急清理...");

        try {
            // 1. 清理所有日志目录
            String logRootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Log/";
            File logRoot = new File(logRootPath);

            if (logRoot.exists() && logRoot.isDirectory()) {
                // 删除所有超过3天的日志
                deleteOldFiles(logRoot, 3);
            }

            // 2. 如果还是不够，删除所有超过1天的日志
            long freeSpace = getAvailableSpace();
            if (freeSpace < MIN_FREE_SPACE * 2) {
                deleteOldFiles(logRoot, 1);
            }

            long afterCleanup = getAvailableSpace();
            Log.i(TAG, "紧急清理完成，释放空间: " + formatSize(afterCleanup - freeSpace));

        } catch (Exception e) {
            Log.e(TAG, "紧急清理失败: " + e.getMessage());
        }
    }

    /**
     * 删除超过指定天数的文件
     */
    private void deleteOldFiles(File directory, int days) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        long cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);
        deleteOldFilesRecursive(directory, cutoffTime);
    }

    /**
     * 递归删除旧文件
     */
    private void deleteOldFilesRecursive(File directory, long cutoffTime) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                deleteOldFilesRecursive(file, cutoffTime);
                // 如果目录为空，删除目录
                if (file.listFiles() == null || file.listFiles().length == 0) {
                    file.delete();
                }
            } else {
                if (file.lastModified() < cutoffTime) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        Log.d(TAG, "删除旧文件: " + file.getName());
                    }
                }
            }
        }
    }

    /**
     * 清理旧的二进制日志
     */
    private void cleanOldBinaryLogs() {
        try {
            String binLogPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Log/BinLog/";
            File binLogDir = new File(binLogPath);

            if (binLogDir.exists() && binLogDir.isDirectory()) {
                // 删除超过7天的二进制日志
                deleteOldFiles(binLogDir, 7);
            }
        } catch (Exception e) {
            Log.e(TAG, "清理二进制日志失败: " + e.getMessage());
        }
    }

    /**
     * 递归计算目录大小
     */
    private long getDirSize(File dir) {
        if (dir == null || !dir.exists()) {
            return 0;
        }

        if (!dir.isDirectory()) {
            return dir.length();
        }

        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    size += getDirSize(file);
                } else {
                    size += file.length();
                }
            }
        }
        return size;
    }

    /**
     * 格式化大小显示
     */
    public static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2fKB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2fMB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2fGB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 检查是否可以写入指定大小的文件
     * @param fileSize 文件大小（字节）
     * @return true=可以写入，false=空间不足
     */
    public boolean canWriteFile(long fileSize) {
        if (!canWriteLog()) {
            return false;
        }

        long availableSpace = getAvailableSpace();
        // 保留至少 MIN_FREE_SPACE 的空间
        return availableSpace - fileSize > MIN_FREE_SPACE;
    }

    /**
     * 手动触发检查
     */
    public void checkNow() {
        checkAndUpdateStatus();
    }

    /**
     * 获取磁盘状态信息
     */
    public String getStorageInfo() {
        long available = getAvailableSpace();
        long logSize = getLogDirectorySize();
        boolean circuitBreakerStatus = logWriteCircuitBreakerOpen;

        return String.format(
            "可用空间: %s\n" +
            "日志占用: %s\n" +
            "日志限制: %s\n" +
            "熔断状态: %s",
            formatSize(available),
            formatSize(logSize),
            formatSize(MAX_LOG_SPACE),
            circuitBreakerStatus ? "❌ 已打开（停止写入）" : "✅ 正常"
        );
    }
}
