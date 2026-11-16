package com.gzlab.autoassemblyline_client.util;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * USB 设备权限申请管理。
 * <p>
 * 解决 {@code UsbHostManager.openDevice} 因未授权抛出 RemoteException 的问题：
 * 打开设备前必须先 {@link UsbManager#hasPermission} 判断，未授权则
 * {@link UsbManager#requestPermission} 申请，授权成功后才能 openDevice。
 */
public class UsbPermissionManager {

    private static final String TAG = "UsbPermissionManager";

    /** 自定义 USB 授权结果广播 Action，需与项目内保持一致。 */
    public static final String ACTION_USB_PERMISSION =
            "com.gzlab.autoassemblyline_client.USB_PERMISSION";

    private static volatile boolean sReceiverRegistered = false;

    /**
     * 请求指定 USB 设备的权限。已授权则直接回调，未授权则弹系统授权框。
     */
    public static void requestPermission(Context context, UsbManager usbManager, UsbDevice device) {
        if (context == null || usbManager == null || device == null) {
            return;
        }
        if (usbManager.hasPermission(device)) {
            Log.i(TAG, "already has permission: " + device.getDeviceName());
            return;
        }
        // 系统签名 App 优先尝试静默授权（重启后无需再点授权框）
        if (grantPermissionSilently(context, usbManager, device)) {
            Log.i(TAG, "silently granted: " + device.getDeviceName());
            return;
        }
        registerReceiver(context.getApplicationContext());
        // 静默授权失败，回退到系统弹框（非系统签名或 ROM 不支持时才会走到这里）
        fallbackToDialog(context, usbManager, device);
    }

    /**
     * 仅静默授权，不弹框。用于开机 / SLAN SDK 打开设备之前的预授权，
     * 避免与 SDK 自己的授权弹框竞争。
     *
     * @return true 表示已拥有权限（本来就有，或本次静默授权成功）
     */
    public static boolean grantSilently(Context context, UsbManager usbManager, UsbDevice device) {
        if (context == null || usbManager == null || device == null) {
            return false;
        }
        if (usbManager.hasPermission(device)) {
            return true;
        }
        return grantPermissionSilently(context, usbManager, device);
    }

    private static void fallbackToDialog(Context context, UsbManager usbManager, UsbDevice device) {

        int flags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+ 必须显式指定可变性
            flags = PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                context, 0,
                new Intent(ACTION_USB_PERMISSION).setPackage(context.getPackageName()),
                flags);
        usbManager.requestPermission(device, permissionIntent);
        Log.i(TAG, "requestPermission: " + device.getDeviceName());
    }

    /**
     * 注册授权结果广播接收器（全局只注册一次）。
     */
    private static synchronized void registerReceiver(Context appContext) {
        if (sReceiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(sReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            appContext.registerReceiver(sReceiver, filter);
        }
        sReceiverRegistered = true;
    }

    private static final BroadcastReceiver sReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ACTION_USB_PERMISSION.equals(intent.getAction())) {
                return;
            }
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
            if (granted) {
                Log.i(TAG, "USB permission GRANTED: "
                        + (device != null ? device.getDeviceName() : "null"));
                // 已授权，后续可安全 openDevice。
                // 串口连接依赖底层库自行打开，这里授权即可。
            } else {
                Log.w(TAG, "USB permission DENIED: "
                        + (device != null ? device.getDeviceName() : "null"));
            }
        }
    };

    /**
     * 系统签名 App 静默授权：反射调用 UsbManager 隐藏 API
     * {@code grantPermission}，直接把设备权限授予本 App，无弹框。
     * <p>
     * 该权限会随 usb 权限数据库持久化，但开机后 usb 权限缓存会清空，
     * 因此需要在每次启动时对已连接设备重新调用（见
     * {@link PermissionHelper#requestConnectedUsbPermission(Context)}）。
     * <p>
     * 依赖 platform 签名 + AndroidManifest 中声明 {@code android.permission.MANAGE_USB}。
     * 非系统签名或 ROM 不支持时返回 false，回退到普通 requestPermission 弹框。
     *
     * @return true 表示静默授权成功
     */
    private static boolean grantPermissionSilently(Context context, UsbManager usbManager,
                                                   UsbDevice device) {
        // 依次尝试不同 Android 版本的 grantPermission 方法签名
        // 1) grantPermission(UsbDevice, int uid)           —— API 23+
        // 2) grantPermission(UsbDevice, String packageName) —— 部分 ROM
        // 3) grantPermission(UsbDevice)                     —— 旧版
        try {
            Method m = UsbManager.class.getMethod(
                    "grantPermission", UsbDevice.class, int.class);
            m.invoke(usbManager, device, Process.myUid());
            return usbManager.hasPermission(device);
        } catch (Throwable t) {
            Log.d(TAG, "grantPermission(UsbDevice,int) fail: " + t.getMessage());
        }
        try {
            Method m = UsbManager.class.getMethod(
                    "grantPermission", UsbDevice.class, String.class);
            m.invoke(usbManager, device, context.getPackageName());
            return usbManager.hasPermission(device);
        } catch (Throwable t) {
            Log.d(TAG, "grantPermission(UsbDevice,String) fail: " + t.getMessage());
        }
        try {
            Method m = UsbManager.class.getMethod("grantPermission", UsbDevice.class);
            m.invoke(usbManager, device);
            return usbManager.hasPermission(device);
        } catch (Throwable t) {
            Log.d(TAG, "grantPermission(UsbDevice) fail: " + t.getMessage());
        }
        return false;
    }
}
