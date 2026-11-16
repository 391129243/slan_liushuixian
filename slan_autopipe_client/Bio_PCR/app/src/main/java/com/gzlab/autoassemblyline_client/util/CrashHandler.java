package com.gzlab.autoassemblyline_client.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.util.Log;

import com.gzlab.autoassemblyline_client.MainActivity;
import com.gzlab.autoassemblyline_client.constant.SharedConstant;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 全局崩溃处理器
 *
 * 职责：
 *  1. 捕获未处理异常，写入结构化崩溃日志到 SD 卡（FileUtils.LOG_PATH）
 *  2. 记录崩溃状态到 Preference，便于启动时识别"崩溃后重启"以及防崩溃循环
 *  3. 通过 AlarmManager + PendingIntent 在 1 秒后自动重启 MainActivity
 *  4. 防崩溃循环：5 分钟内连续崩溃 3 次则不再重启
 *  5. 处理完后把异常交回系统默认 handler，确保系统仍能弹出"应用已停止运行"对话框
 */
public class CrashHandler implements UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";

    private static final String FILE_NAME = "crash";
    private static final String FILE_NAME_SUFFIX = ".txt";

    //防崩溃循环：5 分钟内连续崩溃达到 MAX_CRASH_COUNT 次则不再自动重启
    private static final long CRASH_INTERVAL_MS = 5 * 60 * 1000L;
    private static final int MAX_CRASH_COUNT = 3;

    //重启延迟：留 1 秒缓冲，避免在当前进程尚未完全释放资源时启动新进程
    private static final long RESTART_DELAY_MS = 1000L;

    private static CrashHandler mInstance;

    private final Context mContext;
    private UncaughtExceptionHandler mDefaultCrashHandler;

    private CrashHandler(Context context) {
        this.mContext = context.getApplicationContext();
    }

    /**
     * 获取 CrashHandler 单例（线程安全）
     */
    public static synchronized CrashHandler getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new CrashHandler(context);
        }
        return mInstance;
    }

    /**
     * 初始化：将本 Handler 设为线程默认未捕获异常处理器，并缓存系统默认 handler
     * 重复调用安全
     */
    public synchronized void init() {
        if (mDefaultCrashHandler != null) {
            return;
        }
        mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 未捕获异常统一处理入口
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        boolean logged = false;
        try {
            //1. 同步写入崩溃日志到 SD 卡
            logged = dumpExceptionToSDCard(thread, ex);
        } catch (Throwable t) {
            Log.e(TAG, "dumpExceptionToSDCard failed: " + Log.getStackTraceString(t));
        }

        try {
            //2. 仅当日志成功落盘后才记录崩溃状态 + 尝试重启
            if (logged) {
                recordCrash();
                restartAppIfNeeded();
            }
        } catch (Throwable t) {
            Log.e(TAG, "recordCrash/restartApp failed: " + Log.getStackTraceString(t));
        }

        //3. 把异常交回系统默认 handler（KillProcess / 显示崩溃弹框）
        //   必须在最后调用：系统 handler 内部会调用 Process.killProcess 杀进程
        if (mDefaultCrashHandler != null && mDefaultCrashHandler != this) {
            mDefaultCrashHandler.uncaughtException(thread, ex);
        } else {
            //没有系统默认 handler：直接杀掉当前进程
            Process.killProcess(Process.myPid());
            System.exit(10);
        }
    }

    /**
     * 将崩溃日志写入 SD 卡
     *
     * @return true 表示日志写入成功
     */
    private boolean dumpExceptionToSDCard(Thread thread, Throwable ex) {
        if (ex == null) {
            return false;
        }
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.w(TAG, "外部存储不可用，无法写入崩溃日志");
            return false;
        }

        File dir = new File(FileUtils.LOG_PATH);
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "日志目录创建失败: " + FileUtils.LOG_PATH);
            return false;
        }

        long current = System.currentTimeMillis();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date(current));
        String fileName = DateFormatUtil.getFileName() + "_" + FILE_NAME + FILE_NAME_SUFFIX;
        File file = new File(FileUtils.LOG_PATH + fileName);

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(file, false)));
            pw.println(time);
            pw.println("Thread: " + thread.getName() + " (id=" + thread.getId() + ")");
            pw.println();
            dumpPhoneInfo(pw);
            pw.println();
            pw.println("Exception type: " + ex.getClass().getName());
            pw.println("Exception msg : " + ex.getMessage());
            pw.println();
            pw.println("Stack trace:");
            ex.printStackTrace(pw);
            pw.flush();
        } catch (Exception e) {
            Log.e(TAG, "写入崩溃日志失败: " + e.getMessage());
            return false;
        } finally {
            if (pw != null) {
                try {
                    pw.close();
                } catch (Exception ignore) {
                }
            }
        }

        Log.i(TAG, "崩溃日志已保存: " + file.getAbsolutePath());
        return true;
    }

    /**
     * 写入设备/应用基础信息
     */
    private void dumpPhoneInfo(PrintWriter pw) throws NameNotFoundException {
        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
            pw.print("App version: ");
            pw.print(pi.versionName);
            pw.print(" - ");
            pw.println(pi.versionCode);
        } catch (NameNotFoundException e) {
            pw.println("App version: unknown");
        }

        pw.print("OS version: ");
        pw.print(Build.VERSION.RELEASE);
        pw.print(" - SDK ");
        pw.println(Build.VERSION.SDK_INT);

        pw.print("Vendor: ");
        pw.println(Build.MANUFACTURER);

        pw.print("Model: ");
        pw.println(Build.MODEL);

        if (Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0) {
            pw.print("CPU ABI: ");
            StringBuilder abi = new StringBuilder();
            for (int i = 0; i < Build.SUPPORTED_ABIS.length; i++) {
                if (i > 0) {
                    abi.append(",");
                }
                abi.append(Build.SUPPORTED_ABIS[i]);
            }
            pw.println(abi.toString());
        } else {
            pw.println("CPU ABI: unknown");
        }
    }

    /**
     * 记录一次崩溃：累计短时间内的崩溃次数，用于判断是否进入防崩溃循环
     */
    private void recordCrash() {
        long now = System.currentTimeMillis();
        long lastCrashTime = PreferenceUtil.getInstance(mContext)
                .getLongValue(SharedConstant.LAST_CRASH_TIME, 0L);
        int crashCount = PreferenceUtil.getInstance(mContext)
                .getIntValue(SharedConstant.CRASH_COUNT, 0);

        if (now - lastCrashTime > CRASH_INTERVAL_MS) {
            //距离上次崩溃超过窗口，重置计数
            crashCount = 1;
        } else {
            crashCount += 1;
        }

        PreferenceUtil.getInstance(mContext)
                .setValueByName(SharedConstant.LAST_CRASH_TIME, now);
        PreferenceUtil.getInstance(mContext)
                .setValueByName(SharedConstant.CRASH_COUNT, crashCount);
        PreferenceUtil.getInstance(mContext)
                .setValueByName(SharedConstant.IS_CRASH, true);
    }

    /**
     * 短时间多次崩溃时不再重启，避免崩溃循环；
     * 其他情况下使用 AlarmManager 延迟启动 MainActivity
     */
    private void restartAppIfNeeded() {
        long now = System.currentTimeMillis();
        long lastCrashTime = PreferenceUtil.getInstance(mContext)
                .getLongValue(SharedConstant.LAST_CRASH_TIME, 0L);
        int crashCount = PreferenceUtil.getInstance(mContext)
                .getIntValue(SharedConstant.CRASH_COUNT, 0);

        if (crashCount >= MAX_CRASH_COUNT && (now - lastCrashTime) <= CRASH_INTERVAL_MS) {
            Log.e(TAG, "短时间连续崩溃 " + crashCount + " 次，停止自动重启");
            return;
        }

        try {
            Intent intent = new Intent(mContext, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            int requestCode = (int) (System.currentTimeMillis() & 0x7fffffff);
            int flags = PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT;
            //Android 12 (API 31) 强制要求 PendingIntent 指定 mutability
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent restartIntent = PendingIntent.getActivity(
                    mContext, requestCode, intent, flags);

            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.set(AlarmManager.RTC, now + RESTART_DELAY_MS, restartIntent);
                Log.i(TAG, "已调度重启任务，" + RESTART_DELAY_MS + "ms 后启动 MainActivity");
            } else {
                Log.w(TAG, "AlarmManager 不可用，跳过自动重启");
            }
        } catch (Exception e) {
            Log.e(TAG, "调度重启任务失败: " + e.getMessage());
        }
    }
}
