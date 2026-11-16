package com.gidi.bio_pcr.util;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WSS模块日志存储助手 - 简化版
 * 用于保存 RNA/PCR 二进制日志到本地文件
 */
public class WSSLogHelper {

    private static final String TAG = "WSSLogHelper";
    private static WSSLogHelper INSTANCE = null;

    // 日志根目录：/Log/yyyyMMdd/ModuleName/DeviceID/
    private static final String LOG_ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Log/";
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB
    private static final String FILE_SUFFIX = ".log";

    private SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
    private SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA);

    // 文件信息缓存
    private ConcurrentHashMap<String, FileInfo> fileInfoCache = new ConcurrentHashMap<>();

    private WSSLogHelper() {
        initLogDirectory();
    }

    public static synchronized WSSLogHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WSSLogHelper();
        }
        return INSTANCE;
    }

    /**
     * 初始化日志目录
     */
    private void initLogDirectory() {
        File logRoot = new File(LOG_ROOT_PATH);
        if (!logRoot.exists()) {
            logRoot.mkdirs();
            Log.i(TAG, "创建WSS日志根目录: " + LOG_ROOT_PATH);
        }
    }

    /**
     * 保存模块日志（同步）
     * @param moduleName 模块名称（如：PCR、RNA）
     * @param deviceID 设备ID
     * @param logContent 日志内容
     */
    public synchronized void saveLog(String moduleName, String deviceID, String logContent) {
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

        FileOutputStream fos = null;
        try {
            String key = moduleName + "/" + deviceID;
            String todayDir = getTodayModuleLogDir(moduleName, deviceID);

            // 创建目录
            File dir = new File(todayDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 获取或创建日志文件
            File logFile = getOrCreateLogFile(key, todayDir, moduleName, deviceID);
            if (logFile == null) {
                Log.e(TAG, "无法获取日志文件: " + key);
                return;
            }

            // 打开文件（追加模式）
            fos = new FileOutputStream(logFile, true);

            // 写入日志
            String timestamp = DateFormatUtil.getSysFullTime();
            String logLine = timestamp + " [" + deviceID + "] " + logContent + "\n";
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
            // 关闭文件句柄
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
     * 获取或创建日志文件
     */
    private File getOrCreateLogFile(String key, String dir, String moduleName, String deviceID) {
        // 检查是否需要切换日期目录
        String currentDir = getTodayModuleLogDir(moduleName, deviceID);
        FileInfo fileInfo = fileInfoCache.get(key);

        if (fileInfo != null && !fileInfo.directory.equals(currentDir)) {
            // 日期变化，清除旧文件信息
            Log.i(TAG, "日期变化，切换目录: " + key);
            fileInfoCache.remove(key);
            fileInfo = null;
        }

        // 如果没有缓存信息或需要切换，查找或创建文件
        if (fileInfo == null) {
            File logFile = findLatestLogFile(dir);

            if (logFile == null || logFile.length() >= MAX_FILE_SIZE) {
                // 文件不存在或已超过20MB，创建新文件
                logFile = createNewLogFile(dir);
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
    private File findLatestLogFile(String dir) {
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
    private File createNewLogFile(String dir) {
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
     * 文件信息类
     */
    private static class FileInfo {
        String filePath;
        String directory;
        long currentSize;
    }
}
