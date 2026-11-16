package com.gidi.bio_pcr.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class LogcatHelper {

    private static LogcatHelper INSTANCE = null;
    private static String PATH_LOGCAT;  // 日志根目录：/Log/
    private String PATH_TEMPLATE;
    private String PATH_DATABASE;
    private LogDumper mLogDumper = null;
    private int mPId;
	private static final String FILE_NAME_SUFFIX = ".log";
	private int SDCARD_LOG_FILE_SAVE_DAYS = 14;  // 保存14天（与参考工程 slan_liushuixian_server 对齐）
	private final int SDCARD_TYPE = 0;          //当前的日志记录类型为存储在SD卡下面
	private final int MEMORY_TYPE = 1;          //当前的日志记录类型为存储在内存中
	private int CURR_LOG_TYPE = SDCARD_TYPE;    //当前的日志记录类型
	private SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
	private SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA);
    private final long MAX_LOGCAT_FILE_SIZE = 20 * 1024 * 1024;  // 20MB for logcat
    private final long MAX_MODULE_FILE_SIZE = 100 * 1024 * 1024; // 100MB for PCR/RNA
    /**
     *
     * 初始化
     *
     * */
    public void init(Context context) {
        Log.i("LogcatHelper", "Environment.getExternalStorageState()" + Environment.getExternalStorageState());
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
        	PATH_LOGCAT = Environment.getExternalStorageDirectory().getAbsolutePath()+ "/Log/";
            PATH_TEMPLATE = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/Template/";
            PATH_DATABASE = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/database/";
            CURR_LOG_TYPE = SDCARD_TYPE;
        } else {
            PATH_LOGCAT = context.getFilesDir().getPath() + File.separator + "Log/";
            CURR_LOG_TYPE = MEMORY_TYPE;
        }
        Log.i("PcrApplication", "PATH_LOGCAT" + PATH_LOGCAT);
        File dir = new File(PATH_LOGCAT);
        if (!dir.exists()|| !dir.isDirectory()) {
        	dir.mkdirs();
        	Log.i("PcrApplication", "mkdirs logcatdir" + dir.getAbsolutePath());
        }

        File templateDir = new File(PATH_TEMPLATE);
        if (!templateDir.exists()|| !templateDir.isDirectory()) {
            templateDir.mkdirs();
        }

        File database = new File(PATH_DATABASE);
        if (!database.exists()|| !database.isDirectory()) {
            database.mkdirs();
        }

        // 清理超过14天的日志目录
        deleteExpiredLogDirs();
    }

    public static LogcatHelper getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new LogcatHelper(context);
        }
        return INSTANCE;
    }

    /**
     * 获取实例（无参，用于已初始化的情况）
     */
    public static LogcatHelper getInstance() {
        return INSTANCE;
    }

    private LogcatHelper(Context context) {
        init(context);
        //获取当前进程
        mPId = android.os.Process.myPid();
    }

    /**
     * 手动清理旧日志（提供给外部调用）
     */
    public void cleanOldLogs() {
        deleteExpiredLogDirs();
    }

    public void start() {
        if (mLogDumper == null){
        	mLogDumper = new LogDumper(String.valueOf(mPId), PATH_LOGCAT);
            mLogDumper.start();
        }
    }

    public void stop() {
    	Log.i("PcrApplication", "--LogcatHelper stop--");
        if (mLogDumper != null) {
            mLogDumper.stopLogs();
            mLogDumper = null;
        }
    }

    /**
     * 获取当前日期的日志目录（根据模块名）
     * 结构：/Log/yyyyMMdd/moduleName/  —— 日期作为最外层，模块日志归入对应子目录
     */
    private String getTodayLogDir(String moduleName) {
        String today = dayFormat.format(new Date());
        return PATH_LOGCAT + today + File.separator + moduleName + File.separator;
    }

    /**
     * 删除超过14天的日志目录
     */
    private void deleteExpiredLogDirs() {
        if (CURR_LOG_TYPE != SDCARD_TYPE) {
            return;
        }

        try {
            File logRoot = new File(PATH_LOGCAT);
            if (!logRoot.exists() || !logRoot.isDirectory()) {
                return;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -SDCARD_LOG_FILE_SAVE_DAYS);
            Date expireDate = calendar.getTime();

            File[] dayDirs = logRoot.listFiles();
            if (dayDirs == null) {
                return;
            }

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
                            Log.i("LogcatHelper", "删除过期日志目录: " + dirName);
                        }
                    } catch (ParseException e) {
                        Log.e("LogcatHelper", "解析日志目录日期失败: " + dirName);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("LogcatHelper", "删除过期日志目录失败: " + e.getMessage());
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

    private File createNewFile(String dir){
        try {
            File newFile = new File(dir);
            if(!newFile.exists()){
                if(!newFile.exists()){
                    newFile.getParentFile().mkdirs();
                    newFile.createNewFile();
                }
            }
            return newFile;
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    private class LogDumper extends Thread {

        private Process logcatProc;
        private BufferedReader mReader = null;
        private boolean mRunning = true;
        String cmds = null;
        private String mPID;
        private FileOutputStream out = null;
        private File currentLogFile = null;
        private String currentDayDir = null;

        public LogDumper(String pid, String dir) {
            mPID = pid;
            /**
             *
             * 日志级别：*:v , *:d , *:w , *:e , *:f , *:s
             *
             *
             * */

            // cmds = "logcat *:e *:w | grep \"(" + mPID + ")\"";
            // cmds = "logcat  | grep \"(" + mPID + ")\"";
            // cmds = "logcat -s way";
            cmds = "logcat *:e *:w *:i *:d | grep \"(" + mPID + ")\"";
            //cmds = "logcat *:w | grep \"(" + mPID + ")\"";

            try {
                // 创建当天的日志目录
                currentDayDir = getTodayLogDir("logcat");
                File dayDir = new File(currentDayDir);
                if (!dayDir.exists()) {
                    dayDir.mkdirs();
                }

                // 创建新的日志文件
                currentLogFile = createNewLogFile();

                if (currentLogFile != null) {
                    out = new FileOutputStream(currentLogFile, true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * 创建新的日志文件
         */
        private File createNewLogFile() {
            try {
                String fileName = fileNameFormat.format(new Date()) + FILE_NAME_SUFFIX;
                File file = new File(currentDayDir + fileName);
                if (!file.exists()) {
                    file.createNewFile();
                }
                Log.i("LogcatHelper", "创建新日志文件: " + file.getAbsolutePath());
                return file;
            } catch (IOException e) {
                Log.e("LogcatHelper", "创建日志文件失败: " + e.getMessage());
                return null;
            }
        }

        /**
         * 检查是否需要切换日志文件
         */
        private boolean needRotateFile() {
            if (currentLogFile == null) {
                return true;
            }

            // 检查日期是否变化
            String todayDir = getTodayLogDir("logcat");
            if (!todayDir.equals(currentDayDir)) {
                Log.i("LogcatHelper", "日期变化，需要切换日志目录");
                return true;
            }

            // 检查文件大小是否超过20MB
            if (currentLogFile.length() >= MAX_LOGCAT_FILE_SIZE) {
                Log.i("LogcatHelper", "文件大小超过20MB，需要切换日志文件");
                return true;
            }

            return false;
        }

        /**
         * 切换日志文件
         */
        private void rotateLogFile() {
            try {
                // 关闭当前文件
                if (out != null) {
                    out.close();
                    out = null;
                }

                // 更新日期目录
                currentDayDir = getTodayLogDir("logcat");
                File dayDir = new File(currentDayDir);
                if (!dayDir.exists()) {
                    dayDir.mkdirs();
                    // 新的一天，清理过期目录
                    deleteExpiredLogDirs();
                }

                // 创建新文件
                currentLogFile = createNewLogFile();
                if (currentLogFile != null) {
                    out = new FileOutputStream(currentLogFile, true);
                }
            } catch (Exception e) {
                Log.e("LogcatHelper", "切换日志文件失败: " + e.getMessage());
            }
        }

        public void stopLogs() {
            mRunning = false;
        }
        @Override
        public void run() {
            try {
                logcatProc = Runtime.getRuntime().exec(cmds);
                mReader = new BufferedReader(new InputStreamReader(logcatProc.getInputStream()), 512);
                String line = null;
                while (mRunning && (line = mReader.readLine()) != null) {
                    if (!mRunning) {
                        break;
                    }
                    if (line.length() == 0) {
                        continue;
                    }

                    // 检查是否需要切换文件
                    if (needRotateFile()) {
                        rotateLogFile();
                    }

                    if (out != null && line.contains(mPID)) {
                        out.write((DateFormatUtil.getSysFullTime() + "   " + line + "\n").getBytes());
                        out.flush();  // 定期刷新缓冲区
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
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
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                        out = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            }

        }
    }
    
    /**
     * 获取SD卡的Android/data/包名的路径
     *
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
        return cachePath ;
       // return new File(cachePath + File.separator + uniqueName);
    }  
  
    public final static int LOG_RNA = 1;//核提
    public final static int LOG_PCR = 2;//PCR
    // LOG_ARM 已删除 - ARM 日志不再单独落盘，因为 logcat 已包含相同内容（见 RobotWorkManager 改为 Log.i 直写 logcat）

    LogAPL logAPL_pcr = null;
    LogAPL logAPL_rna = null;

    public void appendlog(int type, String text) {
        switch(type) {
            case LOG_PCR:
                if (logAPL_pcr == null) {
                    logAPL_pcr = new LogAPL("PCR");
                    logAPL_pcr.start();
                }
                logAPL_pcr.appendContent(text+" \n");
                break;
            case LOG_RNA:
                if (logAPL_rna == null) {
                    logAPL_rna = new LogAPL("RNA");
                    logAPL_rna.start();
                }
                logAPL_rna.appendContent(text+" \n");
                break;
            // LOG_ARM case 已删除
        }
    }
    private class LogAPL extends Thread {
        boolean mRunning = true;
        StringBuffer content = new StringBuffer();
        String currentPath = "";
        String currentDir = "";
        String moduleName = "";  // 模块名称（PCR 或 RNA）
        FileOutputStream out = null;

        public LogAPL(String moduleName) {
            this.moduleName = moduleName;
            content.append(DateFormatUtil.getSysFullTime());
            updateCurrentDir();
            currentPath = currentDir + File.separator + fileNameFormat.format(System.currentTimeMillis()) + FILE_NAME_SUFFIX;
            initOutStreanm();
        }

        /**
         * 更新当前日期目录
         */
        private void updateCurrentDir() {
            currentDir = getTodayLogDir(moduleName);
        }

        public void initOutStreanm(){
            try {
                File newFile = createNewFile(currentPath);
                if(newFile!=null) {
                    sleep(1000);
                    deleteExpiredLogDirs();  // 使用新的过期清理方法
                    out = new FileOutputStream(newFile);
                }
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        public void checkFileSize(){
            long size = getFileSize(currentPath);
            if(size > MAX_MODULE_FILE_SIZE){
                // 检查日期是否变化
                String newDir = getTodayLogDir(moduleName);
                if (!newDir.equals(currentDir)) {
                    currentDir = newDir;
                    deleteExpiredLogDirs();  // 日期变化时清理过期目录
                }
                currentPath = currentDir + File.separator + fileNameFormat.format(System.currentTimeMillis()) + FILE_NAME_SUFFIX;
                initOutStreanm();
            }
        }

        public void appendContent(String text){
            if(content!=null){
                content.append(text);
            }
        }
        public void stopLogs() {
            mRunning = false;
        }

        @Override
        public void run() {
            try {
                String line = null;
                int length = 0;
                while (mRunning && content != null) {
                    if (!mRunning) {
                        break;
                    }
                    length = content.length();
                    line = content.toString();
                    if (length > 0) {
                        byte[] dataBytes = line.getBytes();
                        if (out != null && line!=null && dataBytes.length > 0) {
                            content.replace(0,length,"");
                            length = 0;
                            out.write(dataBytes);
                        }
                        checkFileSize();//检查大小超过100mb
                    }else{
                        sleep(1000);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                        out = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            }

        }
    }


    public long getFileSize(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            long fileSize = file.length();
            // 例如，将文件大小转换为KB
            double fileSizeInKB = fileSize / 1024.0;
//            Log.i("File Size", "File size is: " + fileSizeInKB + " KB");
            return fileSize;
        } else {
            Log.i("File Size", "File does not exist");
        }
        return -1;
    }
}
