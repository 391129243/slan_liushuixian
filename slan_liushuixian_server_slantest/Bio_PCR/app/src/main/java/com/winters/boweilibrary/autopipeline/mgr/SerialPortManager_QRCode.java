package com.winters.boweilibrary.autopipeline.mgr;

import android.content.Context;
import android.util.Log;

import com.gidi.bio_pcr.util.NumberUtils;
import com.kongqw.serialportlibrary.Device;
import com.kongqw.serialportlibrary.SerialPortFinder;
import com.kongqw.serialportlibrary.SerialPortManager;
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener;
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener;
import com.winters.boweilibrary.BoWei.serialcom.BoWeiConnectManager;
import com.winters.boweilibrary.autopipeline.model.event.QRCodeMessageEvent;
import com.winters.boweilibrary.autopipeline.model.item.QRCodeItem;
import com.winters.boweilibrary.autopipeline.utils.RingBuffer;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SerialPortManager_QRCode {

    private static final String TAG = "SerialPortManager_QRCode";

    private static SerialPortManager_QRCode mInstance;

    private  String uartHumanQRCode = "ttyUSB";

    private  Device device_QRCode;

    private Context mContext;

    public boolean deviceQRCode;

    private SerialPortManager mSerialPortManager;

    private RingBuffer ringBuffer ;

    private byte[] processBuf = new byte[65536];

    private int length =0;

    private int index =0;

    private byte[] bytes;


    public static SerialPortManager_QRCode getInstance(){
        if (null == mInstance) {
            synchronized (SerialPortManager_QRCode.class){
                if (null == mInstance){
                    mInstance = new SerialPortManager_QRCode();
                }
            }
        }
        return  mInstance;
    }

    public SerialPortManager_QRCode() {
        String text = "start";
        bytes = text.getBytes();

    }


    public boolean sendStart(){
        if(deviceQRCode){
            return mSerialPortManager.sendBytes(bytes);
        }else{
            return false;
        }
    }

    /**
     * 识别ftdi芯片的USB驱动
     */

    public void GetTtyUsbPath() {

        synchronized (SerialPortManager_QRCode.this) {
            String devices = "/sys/bus/usb-serial/devices/";
            File file = new File(devices);
            File[] ttyUSBList = file.listFiles();
            uartHumanQRCode = "ttyUSB";

            if (ttyUSBList != null && ttyUSBList.length > 0) {
                for (int i = 0; i < ttyUSBList.length; i++) {
                    File temp = ttyUSBList[i];
                    if (temp.isDirectory() && temp.getName().contains("ttyUSB")) {
                        File usbserial = new File(temp.getPath() + "/uevent");
                        LineNumberReader readerProduct = null;
                        try {
                            readerProduct = new LineNumberReader(new FileReader(usbserial));
                            String info = readerProduct.readLine();
                            if (info.contains("ftdi")) {
                                //uartnamePCR = temp.getName();
                                uartHumanQRCode = temp.getName();
                                Log.i(TAG, "uartHumanQRCode==" + uartHumanQRCode);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "uartHumanQRCode : " + e.getMessage());
                        }

                    }

                }
            }
        }
    }



    public void openSerialPortManager_QRCode(Context context){

        this.mContext = context;

        GetTtyUsbPath();

        if (mSerialPortManager == null) {
            SerialPortFinder serialPortFinder = new SerialPortFinder();
            ArrayList<Device> devices = serialPortFinder.getDevices();
            device_QRCode = null;
            for (Device x : devices) {
                if (x.getName().equals(uartHumanQRCode)) {
                    device_QRCode = x;
                }
            }

            mSerialPortManager = new SerialPortManager();

            ringBuffer = new RingBuffer(65536);

            mSerialPortManager.setOnOpenSerialPortListener(new OnOpenSerialPortListener() {
                @Override
                public void onSuccess(File device) {
                    Log.e(TAG,"openSerialPortManager_QRCode:" + "onSuccess");
                    deviceQRCode = true;
                }

                @Override
                public void onFail(File device, Status status) {
                    Log.e(TAG,"openSerialPortManager_QRCode:" + "onFail");
                    deviceQRCode = false;
                }
            });

            mSerialPortManager.setOnSerialPortDataListener(new OnSerialPortDataListener() {
                @Override
                public void onDataReceived(byte[] bytes) {
                    try {
                        // 将接收到的数据写入环形缓冲区
                        ringBuffer.write(bytes);
                        // 处理缓冲区中的数据
                        processBufferData();
                        Log.i(TAG, "onDataReceived:" +  new String(bytes));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onDataSent(byte[] bytes) {
                    Log.i(TAG,"onDataSent:" + new String(bytes));
                }
            });
            if (device_QRCode != null) {
                boolean openSerialPort = mSerialPortManager.openSerialPort(device_QRCode.getFile(), 115200);
                if (!openSerialPort) {
                    mSerialPortManager = null;
                }else {

                }
            }

        }

    }

    /**
     * 从环形缓冲区取出所有数据并处理
     */
    private void processBufferData() {
        if(!ringBuffer.isEmpty()) {
            // 获取缓冲区中当前所有数据的大小
            int lengthToProcess = ringBuffer.getSize();

            // 获取缓冲区中的数据指针
            byte[] buffer = ringBuffer.getDataPointer(lengthToProcess);
            if (buffer != null) {
                // 直接处理数据
                int processedLength = unpackData(buffer, ringBuffer.getTail(), lengthToProcess);

                // 更新读取指针，标记已处理的数据
                ringBuffer.markProcessed(processedLength);
            }
        }
    }

    /**
     * 模拟解包逻辑
     * @param buffer 缓冲区数据
     * @param start 起始位置
     * @param length 数据长度
     * @return 已处理的字节数
     */
    private int unpackData(byte[] buffer, int start, int length) {

        String text = new String(buffer);

        Log.i(TAG,"Processed data: " + text);

        int processedLength = 0;

        while (true) {

            if (text.contains("##START##") && text.contains("##END##")) {

                int indexStart = text.indexOf("##START##");

                int indexEnd = text.indexOf("##END##");

                String QRCode_Text = text.substring(indexStart+ 0,indexEnd);

                List<QRCodeItem> qRCodeItems = new ArrayList<>();

                String regex = "#P#|#D#"; // 按照 "#P#" 和 "#D#" 分割

                // 使用正则表达式分割字符串
                String[] parts = text.split(regex);

                // 输出分割结果
                int i = 1;
                for (String part : parts) {
                    if (!part.isEmpty()) { // 过滤空字符串
                        // (438,140)0100614141999996901234567890
                        // 使用正则表达式提取括号内的数字和后续的字符串
                        String regex1 = "\\((\\d+),(\\d+)\\)(\\d+)";
                        Pattern pattern = Pattern.compile(regex1);
                        Matcher matcher = pattern.matcher(part);

                        if (matcher.find()) {
                            String x = matcher.group(1); // 提取第一个数字 438
                            String y = matcher.group(2); // 提取第二个数字 140
                            String qrcode = matcher.group(3); // 提取后续的字符串 0100614141999996901234567890

                            QRCodeItem qRCodeItem = new QRCodeItem();

                            qRCodeItem.setIndex(i++);
                            qRCodeItem.setX_coordinate(NumberUtils.StringtoInt(x));
                            qRCodeItem.setY_coordinate(NumberUtils.StringtoInt(y));
                            qRCodeItem.setQrcode(qrcode);
                            qRCodeItems.add(qRCodeItem);

                            Log.i(TAG,"Part 1: " + x);
                            Log.i(TAG,"Part 2: " + y);
                            Log.i(TAG,"Part 3: " + qrcode);
                        }
                    }
                }

                EventBus.getDefault().post(new QRCodeMessageEvent(QRCode_Text, qRCodeItems));

                processedLength = processedLength + indexEnd + 7;

                text = text.substring(indexEnd + 7);

            } else {
                return processedLength;
            }
        }
    }
}
