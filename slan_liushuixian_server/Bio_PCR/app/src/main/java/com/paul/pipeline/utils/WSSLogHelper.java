package com.paul.pipeline.utils;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.StorageMonitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * WSS模块日志存储助手 - 安全优化版
 * 改进：写入后立即关闭文件，避免文件句柄泄漏和数据丢失
 */
public class WSSLogHelper {

    private static final String TAG = "WSSLogHelper";
    private static WSSLogHelper INSTANCE = null;

    // 日志根目录：以日期作为最外层目录，模块日志与 logcat 日志同归属当天目录，方便按日期整体拷贝
    // 结构：/Log/yyyyMMdd/ModuleName/DeviceID/xxx.log
    private static final String LOG_ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Log/";
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB
    private static final int LOG_SAVE_DAYS = 14; // 保存14天
    private static final String FILE_SUFFIX = ".log";

    // ===== 轻量去重：防止客户端重传导致同一段日志重复落盘 =====
    private static final int DEDUP_MIN_CONTENT_SIZE = 256;        // 仅对 >=256 字节的大块内容去重，短状态行放行
    private static final int DEDUP_MAX_ENTRIES = 500;             // 最多缓存 500 条 hash，超出按 LRU 淘汰
    private static final long DEDUP_WINDOW_MS = 10 * 60 * 1000L;  // 10 分钟去重窗口，超窗视为新内容

    private SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
    private SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA);

    // 日志写入队列
    private LinkedBlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>(1000);

    // 文件信息缓存：记录当前文件路径和大小（不缓存文件句柄）
    private ConcurrentHashMap<String, FileInfo> fileInfoCache = new ConcurrentHashMap<>();

    // 去重缓存：key = moduleName|deviceID|contentHash，value = 首次写入时间戳。
    // LinkedHashMap 按访问顺序实现 LRU，容量超限自动淘汰最久未用项。仅在写入线程访问，故用 synchronized 保护。
    private final LinkedHashMap<String, Long> dedupCache = new LinkedHashMap<String, Long>(
            DEDUP_MAX_ENTRIES + 1, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Entry<String, Long> eldest) {
            return size() > DEDUP_MAX_ENTRIES;
        }
    };

    // 后台写入线程
    private HandlerThread logWriteThread;
    private Handler logWriteHandler;
    private volatile boolean isRunning = false;

    private WSSLogHelper() {
        initLogDirectory();
        startLogWriteThread();
    }

    public static synchronized WSSLogHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WSSLogHelper();
        }
        return INSTANCE;
    }

    /**
     * 初始化日志目录并清理过期日志
     */
    private void initLogDirectory() {
        File logRoot = new File(LOG_ROOT_PATH);
        if (!logRoot.exists()) {
            logRoot.mkdirs();
            Log.i(TAG, "创建WSS日志根目录: " + LOG_ROOT_PATH);
        }

        // 清理过期日志
        deleteExpiredLogDirs();
    }

    /**
     * 启动后台日志写入线程
     */
    private void startLogWriteThread() {
        logWriteThread = new HandlerThread("WSSLogWriteThread");
        logWriteThread.start();
        logWriteHandler = new Handler(logWriteThread.getLooper());
        isRunning = true;

        // 启动日志写入任务
        logWriteHandler.post(logWriteRunnable);

        Log.i(TAG, "后台日志写入线程已启动");
    }

    /**
     * 日志写入任务
     */
    private Runnable logWriteRunnable = new Runnable() {
        @Override
        public void run() {
            while (isRunning) {
                try {
                    // 从队列取日志，阻塞等待
                    LogEntry entry = logQueue.take();
                    writeLogToFile(entry);
                } catch (InterruptedException e) {
                    Log.w(TAG, "日志写入线程被中断");
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "写入日志异常: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    };

    /**
     * 保存模块日志（异步）
     * @param moduleName 模块名称（如：PCR、RNA、AIO）
     * @param deviceID 设备ID
     * @param logContent 日志内容
     */
    public void saveLog(String moduleName, String deviceID, String logContent) {
        if (moduleName == null || moduleName.isEmpty()) {
            Log.e(TAG, "模块名称不能为空");
            return;
        }

        if (deviceID == null || deviceID.isEmpty()) {
            Log.e(TAG, "设备ID不能为空");
            return;
        }

        if (logContent == null || logContent.isEmpty()) {
            return;
        }

        // 创建日志条目
        LogEntry entry = new LogEntry(moduleName, deviceID, logContent);

        // 放入队列，异步写入
        boolean success = logQueue.offer(entry);
        if (!success) {
            Log.e(TAG, "日志队列已满，丢弃日志: " + moduleName + "/" + deviceID);
        }
    }

    /**
     * 批量保存日志（用于日志数组）
     */
    public void saveLogs(String moduleName, String deviceID, String[] logLines) {
        if (logLines == null || logLines.length == 0) {
            return;
        }

        for (String logLine : logLines) {
            if (logLine != null && !logLine.isEmpty()) {
                saveLog(moduleName, deviceID, logLine);
            }
        }
    }

    /**
     * 实际写入日志到文件（改进：写入后立即关闭 + 磁盘空间检查）
     */
    private void writeLogToFile(LogEntry entry) {
        FileOutputStream fos = null;
        try {
            // ✅ 磁盘空间检查 - 防止磁盘满
            if (!StorageMonitor.getInstance().canWriteLog()) {
                // 磁盘空间不足，拒绝写入（静默丢弃，避免日志爆炸）
                return;
            }

            // ✅ 轻量去重 - 防止客户端重传导致同一段日志重复落盘
            if (isDuplicate(entry)) {
                Log.w(TAG, "检测到重复日志块，丢弃: " + entry.moduleName + "/" + entry.deviceID
                        + " size=" + entry.logContent.length());
                return;
            }

            String key = entry.moduleName + "/" + entry.deviceID;
            String todayDir = getTodayModuleLogDir(entry.moduleName, entry.deviceID);

            // 创建目录
            File dir = new File(todayDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 获取或创建日志文件
            File logFile = getOrCreateLogFile(key, todayDir, entry.moduleName, entry.deviceID);
            if (logFile == null) {
                Log.e(TAG, "无法获取日志文件: " + key);
                return;
            }

            // 打开文件（追加模式）
            fos = new FileOutputStream(logFile, true);

            // 写入日志
            String timestamp = DateFormatUtil.getSysFullTime();
            String logLine = timestamp + " [" + entry.deviceID + "] " + entry.logContent + "\n";
            byte[] logBytes = logLine.getBytes();
            fos.write(logBytes);

            // 立即刷新到磁盘
            fos.flush();

            // 更新文件大小
            FileInfo fileInfo = fileInfoCache.get(key);
            if (fileInfo != null) {
                fileInfo.currentSize += logBytes.length;

                // 检查文件大小，超过20MB则标记切换
                if (fileInfo.currentSize >= MAX_FILE_SIZE) {
                    Log.i(TAG, "文件超过20MB，标记切换: " + key);
                    fileInfoCache.remove(key);  // 下次会创建新文件
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "写入日志失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关键改进：立即关闭文件句柄
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "关闭文件流失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 轻量去重判断：仅对较大的内容块（>=DEDUP_MIN_CONTENT_SIZE）做去重，短状态行直接放行。
     * 命中窗口内相同内容则判为重复（客户端重传），返回 true 表示应丢弃。
     * 仅由单一写入线程调用，dedupCache 的访问在此串行，无并发问题。
     */
    private boolean isDuplicate(LogEntry entry) {
        // 短内容不参与去重，避免误伤合法的重复日志行
        if (entry.logContent.length() < DEDUP_MIN_CONTENT_SIZE) {
            return false;
        }

        String dedupKey = entry.moduleName + "|" + entry.deviceID + "|" + entry.logContent.hashCode()
                + "|" + entry.logContent.length();
        long now = System.currentTimeMillis();

        Long firstSeen = dedupCache.get(dedupKey);
        if (firstSeen != null && (now - firstSeen) < DEDUP_WINDOW_MS) {
            // 窗口内已见过相同内容块，判为重传
            return true;
        }

        // 未见过或已超窗，记录（更新）本次时间戳
        dedupCache.put(dedupKey, now);
        return false;
    }

    /**
     * 获取或创建日志文件（不缓存文件句柄）
     */
    private synchronized File getOrCreateLogFile(String key, String dir, String moduleName, String deviceID) {
        // 检查是否需要切换日期目录
        String currentDir = getTodayModuleLogDir(moduleName, deviceID);
        FileInfo fileInfo = fileInfoCache.get(key);

        if (fileInfo != null && !fileInfo.directory.equals(currentDir)) {
            // 日期变化，清除旧文件信息
            Log.i(TAG, "日期变化，切换目录: " + key);
            fileInfoCache.remove(key);
            fileInfo = null;

            // 清理过期日志
            deleteExpiredLogDirs();
        }

        // 如果没有缓存信息或需要切换，查找或创建文件
        if (fileInfo == null) {
            File logFile = findLatestLogFile(dir, moduleName, deviceID);

            if (logFile == null || logFile.length() >= MAX_FILE_SIZE) {
                // 文件不存在或已超过20MB，创建新文件
                logFile = createNewLogFile(dir, moduleName, deviceID);
            }

            if (logFile != null) {
                // 缓存文件信息
                FileInfo info = new FileInfo();
                info.filePath = logFile.getAbsolutePath();
                info.directory = dir;
                info.currentSize = logFile.length();
                fileInfoCache.put(key, info);

                return logFile;
            }
        } else {
            // 使用缓存的文件信息
            return new File(fileInfo.filePath);
        }

        return null;
    }

    /**
     * 查找最新的日志文件
     */
    private File findLatestLogFile(String dir, String moduleName, String deviceID) {
        File directory = new File(dir);
        if (!directory.exists()) {
            return null;
        }

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            return null;
        }

        File latestFile = null;
        long latestTime = 0;

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(FILE_SUFFIX)) {
                if (file.lastModified() > latestTime) {
                    latestTime = file.lastModified();
                    latestFile = file;
                }
            }
        }

        return latestFile;
    }

    /**
     * 获取当天的模块日志目录
     * 格式：/Log/yyyyMMdd/ModuleName/DeviceID/
     */
    private String getTodayModuleLogDir(String moduleName, String deviceID) {
        String today = dayFormat.format(new Date());
        return LOG_ROOT_PATH + today + File.separator + moduleName + File.separator + deviceID + File.separator;
    }

    /**
     * 创建新的日志文件
     */
    private File createNewLogFile(String dir, String moduleName, String deviceID) {
        try {
            String fileName = fileNameFormat.format(new Date()) + FILE_SUFFIX;
            File file = new File(dir + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            Log.i(TAG, "创建新的WSS日志文件: " + file.getAbsolutePath());
            return file;
        } catch (IOException e) {
            Log.e(TAG, "创建日志文件失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 删除超过14天的日志目录
     */
    private void deleteExpiredLogDirs() {
        try {
            File logRoot = new File(LOG_ROOT_PATH);
            if (!logRoot.exists() || !logRoot.isDirectory()) {
                return;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -LOG_SAVE_DAYS);
            Date expireDate = calendar.getTime();

            File[] dayDirs = logRoot.listFiles();
            if (dayDirs == null) {
                return;
            }

            int deletedCount = 0;
            for (File dayDir : dayDirs) {
                if (!dayDir.isDirectory()) {
                    continue;
                }

                String dirName = dayDir.getName();
                // 检查目录名是否为日期格式 yyyyMMdd
                if (dirName.length() == 8 && dirName.matches("\\d{8}")) {
                    try {
                        Date dirDate = dayFormat.parse(dirName);
                        if (dirDate.before(expireDate)) {
                            deleteDirectory(dayDir);
                            deletedCount++;
                            Log.i(TAG, "删除过期WSS日志目录: " + dirName);
                        }
                    } catch (ParseException e) {
                        Log.e(TAG, "解析日志目录日期失败: " + dirName);
                    }
                }
            }

            if (deletedCount > 0) {
                Log.i(TAG, "共删除 " + deletedCount + " 个过期日志目录");
            }

        } catch (Exception e) {
            Log.e(TAG, "删除过期日志目录失败: " + e.getMessage());
        }
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) {
        if (directory == null || !directory.exists()) {
            return;
        }

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

    /**
     * 手动触发清理过期日志
     */
    public void cleanExpiredLogs() {
        deleteExpiredLogDirs();
    }

    /**
     * 关闭日志系统（简化版，不需要关闭文件句柄）
     */
    public void shutdown() {
        isRunning = false;

        // 停止线程
        if (logWriteThread != null) {
            logWriteThread.quitSafely();
        }

        // 清空缓存
        fileInfoCache.clear();

        Log.i(TAG, "WSS日志系统已关闭");
    }

    /**
     * 获取日志根路径
     */
    public String getLogRootPath() {
        return LOG_ROOT_PATH;
    }

    /**
     * 获取队列中待写入的日志数量
     */
    public int getPendingLogCount() {
        return logQueue.size();
    }

    /**
     * 日志条目类
     */
    private static class LogEntry {
        String moduleName;
        String deviceID;
        String logContent;
        long timestamp;

        LogEntry(String moduleName, String deviceID, String logContent) {
            this.moduleName = moduleName;
            this.deviceID = deviceID;
            this.logContent = logContent;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * 文件信息类（只缓存路径和大小，不缓存文件句柄）
     */
    private static class FileInfo {
        String filePath;
        String directory;
        long currentSize;
    }
}
