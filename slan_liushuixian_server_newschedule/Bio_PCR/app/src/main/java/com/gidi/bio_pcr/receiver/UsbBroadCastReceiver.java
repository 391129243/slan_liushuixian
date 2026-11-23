package com.gidi.bio_pcr.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.gidi.bio_pcr.PcrApplication;

import java.util.ArrayList;
import java.util.List;

/**
 * Author ： BlackHao
 * Time : 2018/5/10 10:07
 * Description : USB 广播
 * Logs :
 */

public class UsbBroadCastReceiver extends BroadcastReceiver {

    private List<UsbListener> usbListeners = new ArrayList<UsbListener>();

    public static final String ACTION_USB_PERMISSION = "android.permission.USB_PERMISSION";

    /**
     * 只关心 U 盘。USB 转串口、键盘等设备插拔也会发 ATTACHED/DETACHED,
     * 不过滤的话会把正在进行的导出刷新打断。
     * 与 libaums 的判定一致:接口类是 Mass Storage(0x08)。
     */
    private static boolean isMassStorage(UsbDevice device) {
        if (device == null) {
            return false;
        }
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface usbInterface = device.getInterface(i);
            if (usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_MASS_STORAGE) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case ACTION_USB_PERMISSION:
                //接受到自定义广播
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    //允许权限申请
                    if (usbDevice != null) {
                        //回调
                        if (null != usbListeners && usbListeners.size() > 0) {
                            for (UsbListener usbListener : usbListeners) {
                                usbListener.getReadUsbPermission(usbDevice);
                            }
                        }
                    }
                } else {

                    if (null != usbListeners && usbListeners.size() > 0) {
                        for (UsbListener usbListener : usbListeners) {
                            usbListener.failedReadUsb(usbDevice);
                        }
                    }
                }
                break;
            case UsbManager.ACTION_USB_DEVICE_ATTACHED://接收到存储设备插入广播
                UsbDevice device_add = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device_add != null && isMassStorage(device_add)) {
                   /* if (usbListener != null) {
                        usbListener.insertUsb(device_add);
                        PcrApplication.isUsbInsert = true;

                    }*/
                    Log.i("USB123","u盘插入触发");
                    if (null != usbListeners && usbListeners.size() > 0) {
                        for (UsbListener usbListener : usbListeners) {
                            usbListener.insertUsb(device_add);
                            PcrApplication.isUsbInsert = true;
                        }
                    }
                }
                break;
            case UsbManager.ACTION_USB_DEVICE_DETACHED:
                //接收到存储设备拔出广播
                Log.i("USB123","u盘拔出触发");
                UsbDevice device_remove = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                //拔出的不是 U 盘(比如 USB 转串口)时不通知,避免打断正在进行的导出
                if (isMassStorage(device_remove) && null != usbListeners && usbListeners.size()>0){
                        for(UsbListener usbListener :usbListeners){
                            usbListener.removeUsb(device_remove);
                            PcrApplication.isUsbInsert = false;
                        }
                    }

                break;
            case Intent.ACTION_MEDIA_MOUNTED:
                break;
            case Intent.ACTION_MEDIA_UNMOUNTED:
                break;
        }
    }

    public void addUsbListener(UsbListener usbListener) {
       // this.usbListener = usbListener;
        if(null != usbListeners){
           if(!usbListeners.contains(usbListener)){
               usbListeners.add(usbListener);
           }
        }
    }

    public void removeUsbListener(UsbListener usbListener){
        if(null != usbListeners){
            if(usbListeners.contains(usbListener)){
                usbListeners.remove(usbListener);
            }
        }
    }

    /**
     * USB 操作监听
     */
    public interface UsbListener {
        //USB 插入
        void insertUsb(UsbDevice device_add);

        //USB 移除
        void removeUsb(UsbDevice device_remove);

        //获取读取USB权限
        void getReadUsbPermission(UsbDevice usbDevice);

        //读取USB信息失败
        void failedReadUsb(UsbDevice usbDevice);
    }
}
