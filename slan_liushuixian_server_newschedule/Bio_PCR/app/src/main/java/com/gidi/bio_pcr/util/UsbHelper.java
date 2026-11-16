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
    //TAG
    private static String TAG = "UsbHelper";

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
                Partition partition = device.getPartitions().get(0);
                FileSystem currentFs = partition.getFileSystem();
                //获取根目录
                UsbFile root = currentFs.getRootDirectory();
                currentFolder = root;
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
        boolean result;
        try{
            //开始写入
            FileInputStream fis = new FileInputStream(targetFile);//读取选择的文件的
//            BufferedInputStream fis = new BufferedInputStream(new FileInputStream(targetFile.getAbsolutePath()));;//读取选择的文件的
            long avi = fis.available();
            UsbFileOutputStream uos = new UsbFileOutputStream(saveFile);
//            BufferedOutputStream uos = new BufferedOutputStream(new UsbFileOutputStream(saveFile));
            int bytesRead;
//            byte[] buffer = new byte[1024 * 1024];
            long writeCount = 0;
            while ((bytesRead = fis.read(buffer)) != -1) {
                uos.write(buffer, 0, bytesRead);
                writeCount += bytesRead;
                if (progressListener != null) {
                    //回调下载进度
                    progressListener.copyProgress((int) (writeCount * 100 / avi));
                }
            }
            uos.flush();
            fis.close();
            uos.close();
            if(progressListener != null){
                Log.i(TAG,"progressListener.copyResult(true);\n" );
                progressListener.copyResult(true);
            }
            result = true;
            Thread.sleep(200);
        } catch (Exception e) {
            Log.i(TAG,"progressListener.copyResult(false);" );
            Log.e(TAG,e.getMessage() );
            if(progressListener != null){
                progressListener.copyResult(false);
            }
            result = false;
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

        File saveFile = new File(savePath, saveName);
       // Log.i("AAA", "saveFile" + saveFile.getAbsolutePath());

        boolean result;
        try {
            //开始写入
            //1.targetFile是模板文件夹
            UsbFileInputStream uis = new UsbFileInputStream(targetFile);//读取选择的文件的
            FileOutputStream fos = new FileOutputStream(saveFile);
            //这里uis.available一直为0
            long avi = targetFile.getLength();
            long writeCount = 0;
            int bytesRead;
//            byte[] buffer = new byte[1024*1024];
            while ((bytesRead = uis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                writeCount += bytesRead;
//                Log.e(TAG, "Progress : write : " + writeCount + " All : " + avi);
                if (progressListener != null) {
                    //回调下载进度
                    progressListener.copyProgress((int) (writeCount * 100 / avi));
                }
            }
            fos.flush();
            uis.close();
            fos.close();

            if(null != progressListener){
                progressListener.copyResult(true);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG,e.getMessage() );
            if(null != progressListener){
                progressListener.copyResult(false);
            }
            return false;
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
        void copyProgress(int progress);
        void copyResult(boolean result);
    }
}
