package com.gzlab.autoassemblyline_client.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限自动申请工具类。
 * <p>
 * 负责：
 * 1. 运行时危险权限（存储、定位）批量申请；
 * 2. Android 11+ 的“所有文件访问”({@link Settings#ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION})；
 * 3. 已连接 USB 设备的权限自动申请（配合 device_filter.xml 使用）。
 */
public class PermissionHelper {

    private static final String TAG = "PermissionHelper";

    public static final int REQUEST_CODE_PERMISSIONS = 0x1001;
    public static final int REQUEST_CODE_MANAGE_STORAGE = 0x1002;

    /** 需要在运行时申请的危险权限。 */
    private static final String[] RUNTIME_PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };

    /** USB 管理权限（系统签名 App 静默授权 USB 所需）。 */
    private static final String PERMISSION_MANAGE_USB = "android.permission.MANAGE_USB";

    private PermissionHelper() {
    }

    /**
     * 启动诊断：打印本 App 是否已拿到 MANAGE_USB 权限。
     * <p>
     * 用于区分“系统重启后仍弹 USB 授权框”的根因：
     * <ul>
     *   <li>granted=true  → 权限已到手，若仍弹框则是<b>时机问题</b>
     *       （设备枚举晚于 SDK 打开设备），应加大 waitAndGrantUsbSilently 超时；</li>
     *   <li>granted=false → <b>签名/权限问题</b>，APK 未用设备固件 platform key 签名，
     *       或 ROM 未授予 MANAGE_USB，静默授权必然失败、只能弹框。</li>
     * </ul>
     * 过滤日志：{@code adb logcat -s PermissionHelper}
     */
    public static void logUsbPermissionStatus(Context context) {
        try {
            int state = context.checkPermission(PERMISSION_MANAGE_USB,
                    android.os.Process.myPid(), android.os.Process.myUid());
            boolean granted = state == PackageManager.PERMISSION_GRANTED;
            Log.i(TAG, "==== USB 权限诊断 ====");
            Log.i(TAG, "package = " + context.getPackageName());
            Log.i(TAG, "MANAGE_USB granted = " + granted
                    + (granted ? "  (静默授权可用，若仍弹框请排查时机/超时)"
                               : "  (无此权限：APK 需用设备 platform key 签名，否则只能弹框)"));

            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            if (usbManager != null) {
                java.util.Map<String, UsbDevice> list = usbManager.getDeviceList();
                Log.i(TAG, "当前已枚举 USB 设备数 = " + list.size());
                for (UsbDevice d : list.values()) {
                    Log.i(TAG, String.format(
                            "  device=%s VID=0x%04X PID=0x%04X hasPermission=%b",
                            d.getDeviceName(), d.getVendorId(), d.getProductId(),
                            usbManager.hasPermission(d)));
                }
            }
            Log.i(TAG, "======================");
        } catch (Exception e) {
            Log.e(TAG, "logUsbPermissionStatus: " + e.getMessage());
        }
    }

    /**
     * 一次性申请所有需要的权限。建议在启动 Activity 的 onCreate 中调用。
     */
    public static void requestAllPermissions(Activity activity) {
        requestManageExternalStorage(activity);
        requestRuntimePermissions(activity);
        requestConnectedUsbPermission(activity);
    }

    /**
     * 申请运行时危险权限（仅申请尚未授予的）。
     */
    public static void requestRuntimePermissions(Activity activity) {
        List<String> needRequest = new ArrayList<>();
        for (String permission : RUNTIME_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                needRequest.add(permission);
            }
        }
        if (!needRequest.isEmpty()) {
            ActivityCompat.requestPermissions(activity,
                    needRequest.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
        }
    }

    /**
     * Android 11 (API 30) 及以上申请“所有文件访问”权限，跳转系统设置页。
     * 低版本无需处理，靠上面的读写权限即可。
     */
    public static void requestManageExternalStorage(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
                } catch (Exception e) {
                    Log.e(TAG, "requestManageExternalStorage: " + e.getMessage());
                    // 部分定制 ROM 不支持带 package 的 Action，退回到通用设置页
                    try {
                        Intent intent = new Intent(
                                Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
                    } catch (Exception ex) {
                        Log.e(TAG, "requestManageExternalStorage fallback: " + ex.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 是否已获得完整存储权限。
     */
    public static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 对当前已连接、但尚未授权的 USB 设备逐个申请权限。
     * 配合 AndroidManifest 中 USB_DEVICE_ATTACHED + device_filter.xml，
     * 插入时会自动弹框；此方法覆盖“App 启动时设备已经插着”的场景。
     */
    public static void requestConnectedUsbPermission(Context context) {
        try {
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            if (usbManager == null) {
                return;
            }
            for (UsbDevice device : usbManager.getDeviceList().values()) {
                if (!usbManager.hasPermission(device)) {
                    UsbPermissionManager.requestPermission(context, usbManager, device);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "requestConnectedUsbPermission: " + e.getMessage());
        }
    }

    /**
     * 开机 / SLAN SDK 打开设备之前调用：在有限时间内等待 USB 串口设备枚举完成，
     * 并对其静默授权（不弹框）。
     * <p>
     * 解决“系统重启后仍弹出 FT232 授权框”的问题：kiosk 启动很早，
     * onCreate 时 USB 往往还没枚举完，getDeviceList() 为空；这里做有限轮询，
     * 一旦设备出现就静默授权，保证 SLAN SDK 的 100ms 重连定时器
     * 在 hasPermission()==true 之后才打开设备，从而不弹框。
     * <p>
     * 注意：本方法会阻塞当前线程，最长约 {@code timeoutMs} 毫秒。
     * 由于运行在 Application.onCreate（无 Activity），超时上限设小以避免卡启动。
     *
     * @param timeoutMs 最长等待毫秒数，建议 3000~4000
     * @return 已静默授权的设备数量
     */
    public static int waitAndGrantUsbSilently(Context context, long timeoutMs) {
        int granted = 0;
        try {
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            if (usbManager == null) {
                return 0;
            }
            long deadline = System.currentTimeMillis() + timeoutMs;
            boolean anyDeviceSeen = false;
            do {
                java.util.Collection<UsbDevice> devices = usbManager.getDeviceList().values();
                if (!devices.isEmpty()) {
                    anyDeviceSeen = true;
                    boolean allGranted = true;
                    for (UsbDevice device : devices) {
                        if (UsbPermissionManager.grantSilently(context, usbManager, device)) {
                            granted++;
                        } else {
                            allGranted = false;
                        }
                    }
                    // 设备已出现且全部授权成功，无需继续等待
                    if (allGranted) {
                        break;
                    }
                }
                if (System.currentTimeMillis() >= deadline) {
                    break;
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } while (true);

            Log.i(TAG, "waitAndGrantUsbSilently: seen=" + anyDeviceSeen
                    + ", granted=" + granted);
        } catch (Exception e) {
            Log.e(TAG, "waitAndGrantUsbSilently: " + e.getMessage());
        }
        return granted;
    }
}
