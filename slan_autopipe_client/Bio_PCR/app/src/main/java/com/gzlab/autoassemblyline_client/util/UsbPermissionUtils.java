package com.gzlab.autoassemblyline_client.util;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class UsbPermissionUtils {
    private static final String TAG = "UsbPermissionUtils";
    public static final String ACTION_USB_PERMISSION = "com.gidi.bio_pcr_6in1_client.USB_PERMISSION";

    private static volatile boolean receiverRegistered = false;
    // 记录已经发起过请求的设备，避免在轮询/重连定时器中反复弹窗
    private static final Set<String> requestedDevices = Collections.synchronizedSet(new HashSet<>());

    /**
     * 自动为系统内所有已连接的 USB 设备静默授权（不弹窗）
     */
    public static void grantOrRequestAllUsbPermissions(Context context) {
        grantOrRequestAllUsbPermissions(context, false);
    }

    /**
     * 为 USB 设备授权
     *
     * @param context 上下文
     * @param allowPopupFallback 是否在静默授权失败时允许弹窗（轮询重连时传 false，首次启动传 true）
     */
    public static void grantOrRequestAllUsbPermissions(Context context, boolean allowPopupFallback) {
        if (context == null) return;
        try {
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            if (usbManager == null) return;

            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            if (deviceList == null || deviceList.isEmpty()) {
                return;
            }

            registerUsbReceiverIfNeeded(context.getApplicationContext());

            for (UsbDevice device : deviceList.values()) {
                if (usbManager.hasPermission(device)) {
                    continue;
                }

                String deviceKey = device.getDeviceName();
                Log.i(TAG, "检测到未授权 USB 设备: " + deviceKey + ", VID: " + device.getVendorId() + ", PID: " + device.getProductId());

                // 1. 优先尝试系统反射静默赋权（不弹窗）
                boolean grantedSilently = trySilentGrantPermission(context, device);
                if (!grantedSilently) {
                    // 2. 尝试 root chmod 赋权
                    tryRootChmodUsb();
                }

                // 检查是否已获得权限
                if (usbManager.hasPermission(device)) {
                    Log.i(TAG, "USB 设备已成功静默授权: " + deviceKey);
                    requestedDevices.add(deviceKey);
                } else if (allowPopupFallback && !requestedDevices.contains(deviceKey)) {
                    // 3. 仅在允许且从未弹过窗的情况下发起一次标准授权
                    requestedDevices.add(deviceKey);
                    int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        flags |= PendingIntent.FLAG_MUTABLE;
                    }
                    Intent intent = new Intent(ACTION_USB_PERMISSION);
                    intent.setPackage(context.getPackageName());
                    PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, flags);
                    usbManager.requestPermission(device, pi);
                    Log.i(TAG, "已为 USB 设备发起一次系统权限请求: " + deviceKey);
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "grantOrRequestAllUsbPermissions 异常", e);
        }
    }

    /**
     * 尝试通过系统反射静默授予 USB 权限（适用于工控机/定制系统/系统签名）
     */
    public static boolean trySilentGrantPermission(Context context, UsbDevice device) {
        try {
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = serviceManagerClass.getMethod("getService", String.class);
            IBinder binder = (IBinder) getServiceMethod.invoke(null, Context.USB_SERVICE);

            Class<?> stubClass = Class.forName("android.hardware.usb.IUsbManager$Stub");
            Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
            Object service = asInterfaceMethod.invoke(null, binder);

            if (service != null) {
                int uid = android.os.Process.myUid();
                // 方式 1: grantDevicePermission(UsbDevice, int uid)
                try {
                    Method grantMethod = service.getClass().getMethod("grantDevicePermission", UsbDevice.class, int.class);
                    grantMethod.setAccessible(true);
                    grantMethod.invoke(service, device, uid);
                    Log.i(TAG, "反射 grantDevicePermission(uid) 成功");
                } catch (Throwable ignored) {
                }

                // 方式 2: setDevicePackage (设置为默认处理程序，不再弹窗)
                try {
                    Method setDevicePkgMethod = service.getClass().getMethod("setDevicePackage", UsbDevice.class, String.class, int.class);
                    setDevicePkgMethod.setAccessible(true);
                    setDevicePkgMethod.invoke(service, device, context.getPackageName(), 0);
                    Log.i(TAG, "反射 setDevicePackage 成功");
                } catch (Throwable ignored) {
                }

                // 方式 3: grantAccess(UsbDevice, String packageName)
                try {
                    Method grantAccessMethod = service.getClass().getMethod("grantAccess", UsbDevice.class, String.class);
                    grantAccessMethod.setAccessible(true);
                    grantAccessMethod.invoke(service, device, context.getPackageName());
                    Log.i(TAG, "反射 grantAccess 成功");
                } catch (Throwable ignored) {
                }

                // 方式 4: grantDevicePermission(UsbDevice, String packageName)
                try {
                    Method grantPkgMethod = service.getClass().getMethod("grantDevicePermission", UsbDevice.class, String.class);
                    grantPkgMethod.setAccessible(true);
                    grantPkgMethod.invoke(service, device, context.getPackageName());
                    Log.i(TAG, "反射 grantDevicePermission(pkg) 成功");
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "反射静默授予 USB 权限未成功: " + t.getMessage());
        }

        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        return usbManager != null && usbManager.hasPermission(device);
    }

    /**
     * 尝试对 /dev/bus/usb 设备节点进行 chmod 赋权（如果设备有 root 权限）
     */
    public static void tryRootChmodUsb() {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("chmod -R 777 /dev/bus/usb/\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            Log.i(TAG, "通过 root 赋权 /dev/bus/usb/ 完成");
        } catch (Throwable ignored) {
        } finally {
            try {
                if (os != null) os.close();
                if (process != null) process.destroy();
            } catch (Exception ignored) {
            }
        }
    }

    private static synchronized void registerUsbReceiverIfNeeded(Context context) {
        if (receiverRegistered) return;
        try {
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    String action = intent.getAction();
                    if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                        grantOrRequestAllUsbPermissions(ctx, false);
                    } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (device != null) {
                            requestedDevices.remove(device.getDeviceName());
                        }
                    } else if (ACTION_USB_PERMISSION.equals(action)) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (device != null) {
                            requestedDevices.add(device.getDeviceName());
                        }
                    }
                }
            };
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(receiver, filter);
            }
            receiverRegistered = true;
        } catch (Throwable e) {
            Log.e(TAG, "注册 USB 广播接收器失败", e);
        }
    }
}
