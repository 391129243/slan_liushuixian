package com.gzlab.autoassemblyline_client.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class LogcatHelper {

    private static final String TAG = "LogcatHelper";
    private static LogcatHelper INSTANCE = null;
    private static String PATH_LOGCAT;
    private String PATH_TEMPLATE;
    private String PATH_DATABASE;
    private LogDumper mLogDumper = null;
    private int mPId;
    private Context appContext;

    // ==================== 存储策略配置 ====================
    private static final String FILE_NAME_SUFFIX = ".log";
    private static final int SDCARD_LOG_FILE_SAVE_DAYS = 10;  // 保存14天
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 单文件最大20MB
    private static final long FLUSH_INTERVAL = 5 * 1024; // 每5KB flush一次

    // ==================== 上报策略配置 ====================
    private static final long UPLOAD_INTERVAL_MS = 30 * 1000;      // 定时30秒
    private static final long UPLOAD_BUFFER_THRESHOLD = 16 * 1024; // 积累16KB立即发送
    private static final String PREF_NAME = "LogUploadProgress";
    private static final String PREF_KEY_PREFIX = "log_pos_";

    private final int SDCARD_TYPE = 0;          //当前的日志记录类型为存储在SD卡下面
    private final int MEMORY_TYPE = 1;          //当前的日志记录类型为存储在内存中
    private int CURR_LOG_TYPE = SDCARD_TYPE;    //当前的日志记录类型

    // 日期格式：目录用 yyyy-MM-dd，文件名用 HH-mm-ss
    private SimpleDateFormat dateDirSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
    private SimpleDateFormat fileNameSdf = new SimpleDateFormat("HH-mm-ss", Locale.CHINA);

    // ==================== 上报进度管理 ====================
    private final Map<String, Long> mUploadProgressMap = new HashMap<>();
    private SharedPreferences mUploadProgressPref;
    private long mLastUploadTime = 0;
    private Timer mUploadTimer;
    private volatile long mPendingUploadSize = 0;
  
    /**
     *
     * 初始化
     *
     * */
    public void init(Context context) {
        Log.i(TAG, "Environment.getExternalStorageState()" + Environment.getExternalStorageState());
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
        	PATH_LOGCAT = Environment.getExternalStorageDirectory().getAbsolutePath()+ "/Log/";
            PATH_TEMPLATE = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/Template/";
            PATH_DATABASE = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/database/";
            CURR_LOG_TYPE = SDCARD_TYPE;
        } else {
            PATH_LOGCAT = context.getFilesDir().getPath() + File.separator + "Log/";
            CURR_LOG_TYPE = MEMORY_TYPE;
        }
        Log.i(TAG, "PATH_LOGCAT: " + PATH_LOGCAT);
        File dir = new File(PATH_LOGCAT);
        if (!dir.exists() || !dir.isDirectory()) {
            Log.i(TAG, "创建日志根目录");
        	dir.mkdirs();
        	Log.i(TAG, "日志目录: " + dir.getAbsolutePath());
        }

        File templateDir = new File(PATH_TEMPLATE);
        if (!templateDir.exists() || !templateDir.isDirectory()) {
            templateDir.mkdirs();
        }

        File database = new File(PATH_DATABASE);
        if (!database.exists() || !database.isDirectory()) {
            database.mkdirs();
        }
    }  
  
    public static LogcatHelper getInstance(Context context) {  
        if (INSTANCE == null) {  
            INSTANCE = new LogcatHelper(context);  
        }  
        return INSTANCE;  
    }  

    private LogcatHelper(Context context) {
        this.appContext = context.getApplicationContext();
        init(context);
        //获取当前进程
        mPId = android.os.Process.myPid();
        // 初始化上传进度持久化
        mUploadProgressPref = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadUploadProgress();
    }

    public void start() {
        if (mLogDumper == null){
        	mLogDumper = new LogDumper(String.valueOf(mPId), PATH_LOGCAT);
            mLogDumper.start();
        }
        // 启动增量上传定时器
        startUploadTimer();
    }

    public void stop() {
    	Log.i(TAG, "--LogcatHelper stop--");
        if (mLogDumper != null) {
            mLogDumper.stopLogs();
            mLogDumper = null;
        }
        // 停止上传定时器
        stopUploadTimer();
    }

    private class LogDumper extends Thread {

        private Process logcatProc;
        private BufferedReader mReader = null;
        private boolean mRunning = true;
        String cmds = null;
        private String mPID;
        private BufferedOutputStream out = null;

        // 文件滚动相关
        private String mCurrentFileDate = ""; // 当前文件所属日期 yyyy-MM-dd
        private volatile long mCurrentFileSize = 0; // 当前文件大小
        private volatile long mLastFlushSize = 0; // 上次flush时的文件大小
        private final Object mLogLock = new Object();

        public LogDumper(String pid, String dir) {
            mPID = pid;
            try {
                // 初始打开日志文件
                openLogFile(dir);
                sleep(1000);
                // 删除过期日志
                deleteExpiredLogs();
            } catch (IOException e) {
                Log.e(TAG, "初始化日志文件异常", e);
            } catch (InterruptedException e) {
                Log.e(TAG, "初始化延迟异常", e);
            }

            /**
             * 日志级别：*:v , *:d , *:w , *:e , *:f , *:s
             * */
            cmds = "logcat *:e *:i | grep \"(" + mPID + ")\"";
        }  
  
        public void stopLogs() {
            mRunning = false;
        }

        /**
         * 打开日志文件（按天分目录）
         */
        private void openLogFile(String rootDir) throws IOException {
            Date now = new Date();
            String dateDir = dateDirSdf.format(now);
            String fileName = fileNameSdf.format(now);

            // 创建日期子目录：/Log/yyyy-MM-dd/
            File dateDirFile = new File(rootDir, dateDir);
            if (!dateDirFile.exists()) {
                dateDirFile.mkdirs();
            }

            // 创建日志文件：/Log/yyyy-MM-dd/HH-mm-ss.log
            File logFile = new File(dateDirFile, fileName + FILE_NAME_SUFFIX);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }

            out = new BufferedOutputStream(new FileOutputStream(logFile, true), 8192);
            mCurrentFileDate = dateDir;
            mCurrentFileSize = logFile.length();
            mLastFlushSize = mCurrentFileSize;
            Log.i(TAG, "日志文件已打开: " + logFile.getAbsolutePath() + ", 大小: " + mCurrentFileSize);
        }

        /**
         * 检查是否需要滚动日志文件
         * 条件1：跨天
         * 条件2：文件超过20MB
         */
        private void rollLogFileIfNeeded() {
            String today = dateDirSdf.format(new Date());
            boolean needRoll = false;
            String reason = "";

            // 条件1：跨天
            if (!today.equals(mCurrentFileDate)) {
                needRoll = true;
                reason = "跨天";
            }
            // 条件2：超过20MB
            else if (mCurrentFileSize >= MAX_FILE_SIZE) {
                needRoll = true;
                reason = "超过20MB";
            }

            if (needRoll) {
                synchronized (mLogLock) {
                    try {
                        if (out != null) {
                            out.flush();
                            out.close();
                        }
                        Log.i(TAG, "日志文件滚动: " + reason);
                        openLogFile(PATH_LOGCAT);
                        mLastFlushSize = 0;
                    } catch (IOException e) {
                        Log.e(TAG, "日志文件滚动异常", e);
                    }
                }
            }
        }  
  
        @Override
        public void run() {
            try {
                logcatProc = Runtime.getRuntime().exec(cmds);
                mReader = new BufferedReader(new InputStreamReader(
                        logcatProc.getInputStream()), 512);
                String line = null;
                while (mRunning && (line = mReader.readLine()) != null) {
                    if (!mRunning) {
                        break;
                    }
                    if (line.length() == 0) {
                        continue;
                    }
                    if (out != null && line.contains(mPID)) {
                        synchronized (mLogLock) {
                            // 检查是否需要滚动
                            rollLogFileIfNeeded();

                            // 写入日志
                            byte[] logBytes = (DateFormatUtil.getSysFullTime() + "   " + line + "\n").getBytes();
                            out.write(logBytes);
                            mCurrentFileSize += logBytes.length;

                            // 智能flush：每积累5KB触发一次
                            if (mCurrentFileSize - mLastFlushSize >= FLUSH_INTERVAL) {
                                out.flush();
                                mLastFlushSize = mCurrentFileSize;
                            }
                        }
                    }
                }

            } catch (IOException e) {
                Log.e(TAG, "日志读取异常", e);
            } finally {
                if (logcatProc != null) {
                    logcatProc.destroy();
                    logcatProc = null;
                }
                if (mReader != null) {
                    try {
                        mReader.close();
                        mReader = null;
                    } catch (IOException e) {
                        Log.e(TAG, "关闭Reader异常", e);
                    }
                }
                if (out != null) {
                    try {
                        out.flush();
                        out.close();
                        out = null;
                    } catch (IOException e) {
                        Log.e(TAG, "关闭输出流异常", e);
                    }
                }
            }
        }

        /**
         * 删除过期日志（按日期目录）
         * 保护crash文件不被删除
         */
        private void deleteExpiredLogs() {
            if (CURR_LOG_TYPE != SDCARD_TYPE) {
                return;
            }

            File logRootDir = new File(PATH_LOGCAT);
            if (!logRootDir.isDirectory()) {
                return;
            }

            // 计算过期日期
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -SDCARD_LOG_FILE_SAVE_DAYS);
            String expireDateStr = dateDirSdf.format(calendar.getTime());

            File[] files = logRootDir.listFiles();
            if (files == null) {
                return;
            }

            for (File file : files) {
                // 只处理目录，跳过所有文件（包括crash文件）
                if (!file.isDirectory()) {
                    if (file.getName().contains("crash") || file.getName().endsWith(".txt")) {
                        Log.d(TAG, "跳过crash文件，不自动删除: " + file.getName());
                    }
                    continue;
                }

                String dirName = file.getName();
                // 只删除符合 yyyy-MM-dd 格式的目录
                if (dirName.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    // 字符串比较：yyyy-MM-dd 格式可以直接按字典序比较
                    if (dirName.compareTo(expireDateStr) < 0) {
                        if (deleteDirectory(file)) {
                            Log.i(TAG, "删除过期日志目录: " + dirName);
                        }
                    }
                }
            }
        }

        /**
         * 递归删除目录
         */
        private boolean deleteDirectory(File dir) {
            if (dir.isDirectory()) {
                File[] children = dir.listFiles();
                if (children != null) {
                    for (File child : children) {
                        if (!deleteDirectory(child)) {
                            return false;
                        }
                    }
                }
            }
            return dir.delete();
        }
    }

    /**
     * 获取SD卡的Android/data/包名的路径
     * 保留方法以防其他地方调用
     ***/
    private static String getDiskCacheDir(Context context) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return cachePath;
    }

    // ==================== 日志上报功能 ====================

    /**
     * 启动增量上传定时器
     * 每10秒检查一次，满足条件时上传
     */
    private void startUploadTimer() {
        if (mUploadTimer != null) {
            return;
        }
        mUploadTimer = new Timer("LogUploadTimer", true);
        mUploadTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkAndUpload();
            }
        }, 3000, 10 * 1000); // 首次延迟3秒，之后每10秒检查
        Log.i(TAG, "日志增量上传定时器已启动：定时30秒 OR 积累16KB");
    }

    /**
     * 停止上传定时器
     */
    private void stopUploadTimer() {
        if (mUploadTimer != null) {
            mUploadTimer.cancel();
            mUploadTimer = null;
            Log.i(TAG, "日志上传定时器已停止");
        }
    }

    /**
     * 混合策略核心：检查是否满足上传条件
     * 条件1：距离上次上传 >= 30秒
     * 条件2：待上传字节数 >= 16KB
     */
    private void checkAndUpload() {
        // WSS 未连接时直接跳过本轮：不读文件、不做 Gson 序列化。
        // 断连期间日志照常落盘，连接恢复后再从已保存的进度继续增量上传；
        // 避免断连时反复读取大文件并序列化，持续给堆施压导致 OOM。
        com.gzlab.autoassemblyline_client.autopipeline.mgr.WSSModuleManager wssManager =
                com.gzlab.autoassemblyline_client.autopipeline.mgr.WSSModuleManager.getInstance(appContext);
        if (!wssManager.isWssConnected()) {
            Log.d(TAG, "WSS未连接，跳过本轮日志上传");
            return;
        }

        long now = System.currentTimeMillis();
        long elapsedTime = now - mLastUploadTime;

        // 计算待上传字节数
        long pendingSize = calculatePendingUploadSize();
        mPendingUploadSize = pendingSize;

        boolean shouldUpload = false;
        String reason = "";

        // 条件1：定时30秒到期
        if (elapsedTime >= UPLOAD_INTERVAL_MS) {
            shouldUpload = true;
            reason = "定时30秒到期";
        }
        // 条件2：积累超过16KB
        else if (pendingSize >= UPLOAD_BUFFER_THRESHOLD) {
            shouldUpload = true;
            reason = "积累" + (pendingSize / 1024) + "KB";
        }

        if (shouldUpload) {
            Log.i(TAG, "========== PCRLog 上报触发 ==========");
            Log.i(TAG, "触发原因: " + reason);
            Log.i(TAG, "待上传大小: " + pendingSize + " 字节 (" + (pendingSize / 1024) + " KB)");
            Log.i(TAG, "距上次上传: " + (elapsedTime / 1000) + " 秒");
            uploadIncrementLogs();
            mLastUploadTime = System.currentTimeMillis();
            Log.i(TAG, "========== PCRLog 上报完成 ==========");
        }
    }

    /**
     * 计算所有文件的待上传字节数总和
     */
    private long calculatePendingUploadSize() {
        long totalPending = 0;

        File logRootDir = new File(PATH_LOGCAT);
        if (!logRootDir.exists() || !logRootDir.isDirectory()) {
            return 0;
        }

        File[] dateDirs = logRootDir.listFiles();
        if (dateDirs == null) return 0;

        for (File dateDir : dateDirs) {
            if (!dateDir.isDirectory()) continue;
            File[] logFiles = dateDir.listFiles();
            if (logFiles == null) continue;

            for (File logFile : logFiles) {
                if (!logFile.isFile() || !logFile.getName().endsWith(FILE_NAME_SUFFIX)) {
                    continue;
                }
                String path = logFile.getAbsolutePath();
                long readPos = mUploadProgressMap.getOrDefault(path, 0L);
                long fileLength = logFile.length();
                if (fileLength > readPos) {
                    totalPending += (fileLength - readPos);
                }
            }
        }

        return totalPending;
    }

    /**
     * 上传增量日志
     */
    private void uploadIncrementLogs() {
        File logRootDir = new File(PATH_LOGCAT);
        if (!logRootDir.exists() || !logRootDir.isDirectory()) {
            Log.w(TAG, "日志根目录不存在: " + PATH_LOGCAT);
            return;
        }

        int totalFiles = 0;
        int uploadedFiles = 0;
        long totalBytes = 0;

        File[] dateDirs = logRootDir.listFiles();
        if (dateDirs == null) return;

        for (File dateDir : dateDirs) {
            if (!dateDir.isDirectory()) continue;
            File[] logFiles = dateDir.listFiles();
            if (logFiles == null) continue;

            for (File logFile : logFiles) {
                if (!logFile.isFile() || !logFile.getName().endsWith(FILE_NAME_SUFFIX)) {
                    continue;
                }
                totalFiles++;
                long beforePos = mUploadProgressMap.getOrDefault(logFile.getAbsolutePath(), 0L);
                uploadSingleFile(logFile);
                long afterPos = mUploadProgressMap.getOrDefault(logFile.getAbsolutePath(), 0L);
                if (afterPos > beforePos) {
                    uploadedFiles++;
                    totalBytes += (afterPos - beforePos);
                }
            }
        }

        Log.i(TAG, "上传统计: 总文件=" + totalFiles + ", 已上传=" + uploadedFiles + ", 上传字节=" + totalBytes);
    }

    /**
     * 上传单个文件的增量内容
     */
    private void uploadSingleFile(File logFile) {
        String filePath = logFile.getAbsolutePath();
        long readPos = mUploadProgressMap.getOrDefault(filePath, 0L);
        long fileLength = logFile.length();

        if (fileLength <= readPos) {
            return; // 无新内容
        }

        Log.d(TAG, "准备上传文件: " + logFile.getName() + ", 文件大小=" + fileLength + ", 已上传=" + readPos + ", 待上传=" + (fileLength - readPos));

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(logFile);
            // 跳过已读取的部分
            long skipped = fis.skip(readPos);
            if (skipped != readPos) {
                Log.w(TAG, "跳过字节数不匹配: 期望=" + readPos + ", 实际=" + skipped);
                return;
            }

            // 读取增量内容（最多16KB）
            int chunkSize = (int) Math.min(fileLength - readPos, 16 * 1024);
            byte[] buffer = new byte[chunkSize];
            int bytesRead = fis.read(buffer);

            if (bytesRead > 0) {
                Log.i(TAG, "读取日志数据: " + bytesRead + " 字节");
                // 发送到主控
                boolean success = sendLogToServer(logFile, buffer, bytesRead, readPos);
                if (success) {
                    // 更新进度
                    long newPos = readPos + bytesRead;
                    mUploadProgressMap.put(filePath, newPos);
                    saveUploadProgress(filePath, newPos);
                    Log.i(TAG, "PCRLog上传成功: " + logFile.getName() + " [" + readPos + " -> " + newPos + "] (" + bytesRead + " bytes)");
                } else {
                    Log.e(TAG, "PCRLog上传失败: " + logFile.getName());
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "读取日志文件异常: " + filePath, e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    Log.e(TAG, "关闭文件流异常", e);
                }
            }
        }
    }

    /**
     * 发送日志到服务器（通过 WSSModuleManager）
     */
    private boolean sendLogToServer(File logFile, byte[] data, int length, long startPos) {
        try {
            // 构造相对路径：yyyy-MM-dd/HH-mm-ss.log
            String relativePath = getRelativePath(logFile);

            Log.i(TAG, "→ 发送PCRLog消息");
            Log.i(TAG, "  文件路径: " + relativePath);
            Log.i(TAG, "  数据大小: " + length + " 字节");
            Log.i(TAG, "  起始位置: " + startPos);

            // 调用 WSSModuleManager 发送二进制日志消息
            com.gzlab.autoassemblyline_client.autopipeline.mgr.WSSModuleManager wssManager =
                    com.gzlab.autoassemblyline_client.autopipeline.mgr.WSSModuleManager.getInstance(appContext);

            boolean success = wssManager.sendLogMessage(relativePath, data, length);
            if (success) {
                Log.i(TAG, "← PCRLog消息发送成功");
            } else {
                Log.w(TAG, "← PCRLog消息发送失败（WSS未连接或发送异常）");
            }
            return success;
        } catch (Exception e) {
            Log.e(TAG, "← PCRLog消息发送异常", e);
            return false;
        }
    }

    /**
     * 获取日志文件的相对路径
     * /Log/2026-09-07/14-30-15.log -> 2026-09-07/14-30-15.log
     */
    private String getRelativePath(File logFile) {
        String fullPath = logFile.getAbsolutePath();
        int logDirIndex = fullPath.indexOf("/Log/");
        if (logDirIndex >= 0) {
            return fullPath.substring(logDirIndex + 5); // 跳过 "/Log/"
        }
        return logFile.getName();
    }

    /**
     * 加载上传进度
     */
    private void loadUploadProgress() {
        Map<String, ?> allPrefs = mUploadProgressPref.getAll();
        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(PREF_KEY_PREFIX)) {
                String filePath = key.substring(PREF_KEY_PREFIX.length());
                Long pos = (Long) entry.getValue();
                if (pos != null) {
                    mUploadProgressMap.put(filePath, pos);
                }
            }
        }
        Log.i(TAG, "加载上传进度: " + mUploadProgressMap.size() + " 个文件");
    }

    /**
     * 保存单个文件的上传进度
     */
    private void saveUploadProgress(String filePath, long position) {
        mUploadProgressPref.edit()
                .putLong(PREF_KEY_PREFIX + filePath, position)
                .apply();
    }
}
