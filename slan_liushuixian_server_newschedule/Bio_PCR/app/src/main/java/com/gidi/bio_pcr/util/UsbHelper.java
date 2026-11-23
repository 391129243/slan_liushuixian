package com.gidi.bio_pcr.util;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.gidi.bio_pcr.receiver.UsbBroadCastReceiver;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import me.jahnen.libaums.core.UsbMassStorageDevice;
import me.jahnen.libaums.core.fs.FileSystem;
import me.jahnen.libaums.core.fs.UsbFile;
import me.jahnen.libaums.core.fs.UsbFileInputStream;
import me.jahnen.libaums.core.fs.UsbFileOutputStream;
import me.jahnen.libaums.core.partition.Partition;


/**
 * Author ： BlackHao
 * Time : 2018/5/10 14:53
 * Description : USB 操作工具类
 * Logs :
 */

public class UsbHelper {

    //上下文对象
    private Context context;
    //USB 设备列表
    private UsbMassStorageDevice[] storageDevices;
    //USB 广播
    private UsbBroadCastReceiver mUsbReceiver;
    //回调
    private UsbBroadCastReceiver.UsbListener usbListener;
    //当前路径
    private UsbFile currentFolder = null;
    //currentFolder 所属的文件系统,readDevice 时记下,用于查询剩余空间
    private FileSystem currentFs = null;
    //TAG
    private static String TAG = "UsbHelper";
    //复制过程中被置位:用户点取消,或复制期间 U 盘被拔出。copyFile 的写入循环会检查它并中止
    private volatile boolean copyCancelled = false;

    public UsbHelper(Context context, UsbBroadCastReceiver.UsbListener usbListener) {
        this.context = context;
        this.usbListener = usbListener;
        //注册广播
        registerReceiver();
    }

    /**
     * 注册 USB 监听广播
     */
    private void registerReceiver() {
        mUsbReceiver = new UsbBroadCastReceiver();
      //  mUsbReceiver.setUsbListener(usbListener);
        mUsbReceiver.addUsbListener(usbListener);
        //监听otg插入 拔出
        IntentFilter usbDeviceStateFilter = new IntentFilter();
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
//        usbDeviceStateFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
//        usbDeviceStateFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        context.registerReceiver(mUsbReceiver, usbDeviceStateFilter);
        //注册监听自定义广播
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(context);
        boolean isRegister = isRegister(manager, UsbBroadCastReceiver.ACTION_USB_PERMISSION);
        if (!isRegister) {
            IntentFilter filter = new IntentFilter(UsbBroadCastReceiver.ACTION_USB_PERMISSION);
            context.registerReceiver(mUsbReceiver, filter);
        }
//        IntentFilter filter = new IntentFilter(UsbBroadCastReceiver.ACTION_USB_PERMISSION);
//        context.registerReceiver(mUsbReceiver, filter);
    }

    /**
     *
     * action IntentFilter中的某一个action，因为获取到的是IntentFilter的所有action，所以只要匹配一个就可以
     *
     */
    private boolean isRegister(LocalBroadcastManager manager, String action) {
        boolean isRegister = false;
        try {
            Field mReceiversField = manager.getClass().getDeclaredField("mReceivers");
            mReceiversField.setAccessible(true);
//            String name = mReceiversField.getName();
            HashMap<BroadcastReceiver, ArrayList<IntentFilter>> mReceivers = (HashMap<BroadcastReceiver, ArrayList<IntentFilter>>) mReceiversField.get(manager);

            for (BroadcastReceiver key : mReceivers.keySet()) {
                ArrayList<IntentFilter> intentFilters = mReceivers.get(key);
                for (int i = 0; i < intentFilters.size(); i++) {
                    IntentFilter intentFilter = intentFilters.get(i);
                    Field mActionsField = intentFilter.getClass().getDeclaredField("mActions");
                    mActionsField.setAccessible(true);
                    ArrayList<String> mActions = (ArrayList<String>) mActionsField.get(intentFilter);
                    for (int j = 0; j < mActions.size(); j++) {
                        if (mActions.get(i).equals(action)) {
                            isRegister = true;
                            break;
                        }
                    }
                }
            }

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return isRegister;
    }



    public void removeReceiver( UsbBroadCastReceiver.UsbListener usbListener){
        mUsbReceiver.removeUsbListener(usbListener);

    }



    /**
     * 读取 USB设备列表
     *
     * @return USB设备列表
     */
    public UsbMassStorageDevice[] getDeviceList() {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        //获取存储设备
        storageDevices = UsbMassStorageDevice.getMassStorageDevices(context);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(UsbBroadCastReceiver.ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        //可能有几个 一般只有一个 因为大部分手机只有1个otg插口
        for (UsbMassStorageDevice device : storageDevices) {
//            Log.e(TAG, device.getUsbDevice().getDeviceName());
            //有就直接读取设备是否有权限
            if (!usbManager.hasPermission(device.getUsbDevice())) {
                //没有权限请求权限
                usbManager.requestPermission(device.getUsbDevice(), pendingIntent);
            }
        }
        return storageDevices;
    }

    /**
     * 获取device 根目录文件
     *
     * @param device USB 存储设备
     * @return 设备根目录下文件列表
     */
    public ArrayList<UsbFile> readDevice(UsbMassStorageDevice device) {
        ArrayList<UsbFile> usbFiles = new ArrayList<>();

        try {

            Thread.sleep(200);
            //初始化
            device.init();
            //
            //获取partition
            List<Partition> mPartitions = device.getPartitions();
            if(null != mPartitions && mPartitions.size()>0){
                Partition partition = mPartitions.get(0);
                FileSystem fs = partition.getFileSystem();
                //获取根目录
                UsbFile root = fs.getRootDirectory();
                currentFolder = root;
                currentFs = fs;
                //将文件列表添加到ArrayList中
                UsbFile[] files = root.listFiles();
                Arrays.sort(files, new Comparator<UsbFile>() {
                    @Override
                    public int compare(UsbFile file1, UsbFile file2) {
                        if(file1.isDirectory() && !file2.isDirectory()){
                            return -1;
                        }else if(!file1.isDirectory() && file2.isDirectory()) {
                            return 1;
                        }else{
//                            long lastModified1 = file1.lastModified();
//                            long lastModified2 = file2.lastModified();
//                            return Long.compare(lastModified2, lastModified1);
                            return file2.getName().compareTo(file1.getName());
                        }
                    }
                });
                Collections.addAll(usbFiles, files);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
            Log.e(TAG, e.getMessage());
        } finally {

            return usbFiles;

        }
    }

    /**
     * 读取 USB 内文件夹下文件列表
     *
     * @param usbFolder usb文件夹
     * @return 文件列表
     */
    public ArrayList<UsbFile> getUsbFolderFileList(UsbFile usbFolder) {
        //更换当前目录
        currentFolder = usbFolder;
        ArrayList<UsbFile> usbFiles = new ArrayList<>();
        try {
            UsbFile[] files = usbFolder.listFiles();
            Arrays.sort(files, new Comparator<UsbFile>() {
                @Override
                public int compare(UsbFile file1, UsbFile file2) {
                    if(file1.isDirectory() && !file2.isDirectory()){
                        return -1;
                    }else if(!file1.isDirectory() && file2.isDirectory()) {
                        return 1;
                    }else{
//                        long lastModified1 = file1.lastModified();
//                        long lastModified2 = file2.lastModified();
//                        return Long.compare(lastModified2, lastModified1);
                        return file2.getName().compareTo(file1.getName());
                    }
                }
            });
            Collections.addAll(usbFiles, files);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return usbFiles;
    }


    /**
     * 复制文件到 USB
     *
     * @param targetFile       需要复制的文件
     * @param saveFolder       复制的目标文件夹
     * @param progressListener 下载进度回调
     * @return 复制结果
     */
    public boolean saveSDFileToUsb(File targetFile, UsbFile saveFolder, CopyProgressListener progressListener) {
        boolean result;
        try {
            //USB文件是否存在
            boolean isExist = false;
            UsbFile saveFile = null;
            for (UsbFile usbFile : saveFolder.listFiles()) {
                if (usbFile.getName().equals(targetFile.getName())) {
                    isExist = true;
                    saveFile = usbFile;
                }
            }
            if (isExist) {
                //文件已存在，删除文件
                saveFile.delete();
            }
            //创建新文件
            saveFile = saveFolder.createFile(targetFile.getName());
            //开始写入
            FileInputStream fis = new FileInputStream(targetFile);//读取选择的文件的
            long avi = fis.available();
            UsbFileOutputStream uos = new UsbFileOutputStream(saveFile);
            int bytesRead;
//            byte[] buffer = new byte[1024 * 1024];
            long writeCount = 0;
            while ((bytesRead = fis.read(buffer)) != -1) {
                uos.write(buffer, 0, bytesRead);
                writeCount += bytesRead;
//                Log.e(TAG, "Progress : " + (writeCount * 100 / avi));
                if (progressListener != null) {
                    //回调下载进度
                    progressListener.copyProgress((int) (writeCount * 100 / avi));
                }
            }
            uos.flush();
            fis.close();
            uos.close();
            result = true;
        } catch (final Exception e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }


    private byte[] buffer = new byte[1024 * 1024];
    public boolean copyFile(File targetFile,UsbFile saveFile ,CopyProgressListener progressListener){
        boolean result = false;
        FileInputStream fis = null;
        UsbFileOutputStream uos = null;
        try{
            //开始写入
            fis = new FileInputStream(targetFile);//读取选择的文件的
            //用文件实际长度,不用 available():它只表示"当前能不阻塞读到多少",大文件上进度会跳
            long avi = targetFile.length();
            uos = new UsbFileOutputStream(saveFile);
            int bytesRead;
            long writeCount = 0;
            while (!copyCancelled && (bytesRead = fis.read(buffer)) != -1) {
                uos.write(buffer, 0, bytesRead);
                writeCount += bytesRead;
                if (progressListener != null && avi > 0) {
                    //回调下载进度
                    progressListener.copyProgress((int) (writeCount * 100 / avi));
                }
            }
            if (copyCancelled) {
                //写到一半被取消,返回失败,由调用方删掉这个写了一半的文件
                Log.i(TAG, "copyFile cancelled: " + targetFile.getName());
                result = false;
            } else {
                if(progressListener != null){
                    Log.i(TAG,"progressListener.copyResult(true);\n" );
                    progressListener.copyResult(true);
                }
                result = true;
            }
        } catch (Exception e) {
            Log.i(TAG,"progressListener.copyResult(false);" );
            Log.e(TAG, "copyFile failed: " + e.getMessage());
            if(progressListener != null){
                progressListener.copyResult(false);
            }
            result = false;
        } finally {
            //无论成功失败都关掉,避免文件描述符泄漏、U 盘上留下占着簇的空文件
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception ignored) {
                }
            }
            if (uos != null) {
                try {
                    uos.close();
                } catch (Exception ignored) {
                }
            }
        }
        return result;
    }

    /**
     * 复制 USB文件到本地
     *
     * @param targetFile       需要复制的文件
     * @param savePath         复制的目标文件路径
     * @param progressListener 下载进度回调
     * @return 复制结果
     */
    public boolean saveUSbFileToLocal(UsbFile targetFile, String savePath,String saveName,
                                      CopyProgressListener progressListener) {

        //注意:这里会直接覆盖同名文件,失败时还会把它删掉。要保留原文件的调用方应先写临时文件再替换,
        //见 UsbToLocalCopySession.copyOneFile
        File saveFile = new File(savePath, saveName);

        boolean result = false;
        UsbFileInputStream uis = null;
        FileOutputStream fos = null;
        try {
            uis = new UsbFileInputStream(targetFile);
            fos = new FileOutputStream(saveFile);
            //uis.available 一直为 0,用 U 盘文件长度作为进度分母;0 字节文件不回调进度,避免除零
            long avi = targetFile.getLength();
            long writeCount = 0;
            int bytesRead;
            //用局部缓冲区,不和其他复制方法共用成员变量 buffer
            byte[] localBuffer = new byte[1024 * 1024];
            while (!copyCancelled && (bytesRead = uis.read(localBuffer)) != -1) {
                fos.write(localBuffer, 0, bytesRead);
                writeCount += bytesRead;
                if (progressListener != null && avi > 0) {
                    progressListener.copyProgress((int) (writeCount * 100 / avi));
                }
            }
            if (copyCancelled) {
                //写到一半被取消,走 finally 里的失败分支,删掉写了一半的文件
                Log.i(TAG, "saveUSbFileToLocal cancelled: " + saveName);
                return false;
            }
            fos.flush();
            result = true;
            if(null != progressListener){
                progressListener.copyResult(true);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "saveUSbFileToLocal error, file=" + saveName, e);
            if(null != progressListener){
                progressListener.copyResult(false);
            }
            return false;
        } finally {
            //无论成功失败都关闭流
            try { if (uis != null) uis.close(); } catch (Exception ignore) {}
            try { if (fos != null) fos.close(); } catch (Exception ignore) {}
            //失败或取消时删除写了一半的文件
            if (!result && saveFile.exists()) {
                try { saveFile.delete(); } catch (Exception ignore) {}
            }
        }
    }

    /**
     * 获取上层目录文件夹
     *
     * @return usbFile : 父目录文件 / null ：无父目录
     */
    public UsbFile getParentFolder() {
        if (currentFolder != null && !currentFolder.isRoot()) {
            return currentFolder.getParent();
        } else {
            return null;
        }
    }


    /**
     * 获取当前 USBFolder
     */
    public UsbFile getCurrentFolder() {
        return currentFolder;
    }

    /**
     * 当前 U 盘文件系统的剩余空间,单位字节。
     * 读不到(没有挂载、或不是 libaums 支持的 FAT32)时返回 -1,调用方应提示用户而不是继续写。
     */
    public long getFreeSpace() {
        try {
            if (currentFs == null) {
                return -1;
            }
            return currentFs.getFreeSpace();
        } catch (Exception e) {
            Log.e(TAG, "getFreeSpace failed: " + e.getMessage());
            return -1;
        }
    }

    /** 开始一次复制前调用,清除上一次的取消标记 */
    public void resetCopyCancelled() {
        copyCancelled = false;
    }

    /** 请求中止正在进行的复制。写入循环会在下一次读之前停下来 */
    public void cancelCopy() {
        copyCancelled = true;
    }

    public boolean isCopyCancelled() {
        return copyCancelled;
    }

    /**
     * 退出 UsbHelper
     */
    public void finishUsbHelper() {
        context.unregisterReceiver(mUsbReceiver);
        for (UsbMassStorageDevice device : storageDevices) {
            device.close();
        }
    }

    public void closeUsbMassStorageDevice() {
        for (UsbMassStorageDevice device : storageDevices) {
            device.close();
        }
    }

    /**
     * 下载进度回调
     */
    public interface CopyProgressListener {
        /** 开始复制某个文件时回调,用于进度框显示当前文件名。默认不处理,已有的实现不受影响 */
        default void copyStart(String fileName) {
        }
        void copyProgress(int progress);
        void copyResult(boolean result);
    }
}
