package com.winters.boweilibrary.BoWei.serialcom;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.winters.boweilibrary.BoWei.BoWeiService;
import com.winters.boweilibrary.BoWei.event.BoWeiProcedureFinishEvent;
import com.winters.boweilibrary.BoWei.event.BoweiConnectEvent;
import com.winters.boweilibrary.BoWei.status.BoWeiPCRStatus;
import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRStepEntity;
import com.winters.boweilibrary.BoWei.event.BoWeiRecvDataInfo;
import com.winters.boweilibrary.BoWei.OnSerialConnectListener;
import com.gidi.bio_pcr.WintersUtils.NumUtils;
import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRTupeData;
import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRTupeTempretureData;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;

public class BoWeiConnectManager implements OnSerialConnectListener {
    private static final String TAG = "BoWeiConnectManager";
    private static BoWeiConnectManager mInstance;
    private Context context;
    private HandleDisconnectThread mHandleDisThread;
    private boolean is_BoWei_connect = false;
    private  boolean isPCRRunning = false;

    public boolean isPCRRunning() {
        return isPCRRunning;
    }

    public void setPCRRunning(boolean PCRRunning) {
        isPCRRunning = PCRRunning;
    }

    public static BoWeiConnectManager getInstance(){
        if (null == mInstance) {
            synchronized (BoWeiConnectManager.class){
                if (null == mInstance){
                    mInstance = new BoWeiConnectManager();
                }
            }
        }
        return  mInstance;
    }

    private BoWeiConnectManager(){
    }

    public void initConnection(Context context, String serial_port, final BoWeiConnectCallback connectCallback){
        this.context = context;
        BoWeiConnection.getInstance().initConnection(context,serial_port,connectCallback);
        if(mHandleDisThread!= null ){
            mHandleDisThread.interrupt();
            mHandleDisThread = null;
        }
        mHandleDisThread = new HandleDisconnectThread();
        mHandleDisThread.start();
        BoWeiConnection.getInstance().setOnSerialConnectListener(this);
        registerEventBus();
    }

    protected void registerEventBus(){
        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
    }
    protected void unRegisterEventBus(){
        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
    }

    public void closeConnection(){
        if(null != mHandleDisThread){
            mHandleDisThread.interrupt();
            mHandleDisThread = null;
        }
        if(mStatusHandler != null){
            mStatusHandler.removeCallbacksAndMessages(null);
            mStatusHandler = null;
        }

        BoWeiConnection.getInstance().closeSerialPort();
        //unRegisterEventBus();
    }

    public boolean sendSerialCmdMsg_NoAck(BoWeiPCRStepEntity cmd){
        if(!is_BoWei_connect){
            return false;
        }
        int crc = 0;
        byte[] encodeBytes = NumUtils.StringList16To16HexList(cmd.getPCRCmd());

        for (int i = 0; i < encodeBytes.length - 1; i++)
        {
            crc = crc + encodeBytes[i] & 0xFF;
        }
        if (encodeBytes[encodeBytes.length - 1] == (byte)(crc & 0xFF))
        {
            BoWeiConnection.getInstance().sendMessage(cmd);
            return true;
        }
        else
        {
            //////CRC有问题
            return false;
        }
    }

    /**
     * 处理每一条数据
     * @param msg
     */
    private void onHandlerSerialMsg(String msg){

    }


    private Handler mStatusHandler;

    @Override
    public void onReceiveSerialMsg(String msg) {
        onHandlerSerialMsg(msg);
    }

    @Override
    public void onReceiveRNARunningMsg() {

    }


    /**
     * 丁智PCR检测连接
     * @author 80657
     *
     */
    /////////???????????????????????????????????
    public long count = 0;
    private void checkLineStatus() {
        BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("set_heartbeat", "握手", "5E 01 00 05 64", new ArrayList<Double>(), 1, 1, 0, 0, 0, 0, 0);
        BoWeiConnectManager.getInstance().sendSerialCmdMsg_NoAck(pcrDebugEntity);
    }
    /***
     *
     * @author 检查系统链路的线程
     *
     */

    private class HandleDisconnectThread extends Thread{
        @Override
        public void run() {
            // TODO Auto-generated method stub
            super.run();
            Looper.prepare();
            mStatusHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if(true) {
                      //if(!isPCRRunning) {
                      Log.i("BoWeiConnectManager", "count" + count);
                      Log.i("BoWeiConnectManager", "connectingFlag" + connectingFlag);
                        if (count++ >= 5) {
                            count = 0;
                            initBoWeiConnection(context);
                            is_BoWei_connect = false;
                            PcrApplication.is_SLAN_PCR_connect = false;
                        }else{
                            if(!isPCRRunning) {
                                checkLineStatus();
                            }
                        }
                    }
//                    if (count++ >= 5) {
//                        count = 0;
//                        initBoWeiConnection(context);
//                        is_BoWei_connect = false;
//                        PcrApplication.is_SLAN_PCR_connect = false;
//                    }
//                    checkLineStatus();
                    checkConnectState();
                    sendEmptyMessageDelayed(0,2000);
                }
            };
            if(null != mStatusHandler){
                mStatusHandler.sendEmptyMessageDelayed(0,2000);
            }
            Looper.loop();
        }
    }

    private void checkConnectState() {
        EventBus.getDefault().post(new BoweiConnectEvent(is_BoWei_connect));
    }


    /**************************************RNA处理*********************************/



    /**
     *     初始化RNA的串口连接
     *     首先获取TTyusb的路径
     */

    private boolean connectingFlag = false;
    public void initBoWeiConnection(Context context){
        Log.i("BoWeiConnectManager", "connectingFlag---" + connectingFlag);
        if(connectingFlag == true){
            return;
        }
        connectingFlag = true;
        GetTtyUsbPath();
        Log.i("BoWeiConnectManager", "222uartnameBoWei" + uartnameBoWei);
        if(!uartnameBoWei.equals("ttyUSB")) {
            BoWeiPCRStatus.pCRCurrentTupeDataList.clear();
            BoWeiPCRStatus.pCRCurrentTupeDataList.add(new BoWeiPCRTupeData(1, 1, 1, 1, 1, 1, 1));
            BoWeiPCRStatus.pCRCurrentTupeDataList.add(new BoWeiPCRTupeData(2, 2, 2, 2, 2, 2, 2));
            BoWeiPCRStatus.pCRCurrentTupeDataList.add(new BoWeiPCRTupeData(3, 3, 3, 3, 3, 3, 3));
            BoWeiPCRStatus.pCRCurrentTupeDataList.add(new BoWeiPCRTupeData(4, 4, 4, 4, 4, 4, 4));
            BoWeiPCRStatus.pCRCurrentTupeDataList.add(new BoWeiPCRTupeData(5, 5, 5, 5, 5, 5, 5));
            BoWeiPCRStatus.pCRCurrentTupeDataList.add(new BoWeiPCRTupeData(6, 6, 6, 6, 6, 6, 6));
            BoWeiPCRStatus.pCRCurrentTupeDataList.add(new BoWeiPCRTupeData(7, 7, 7, 7, 7, 7, 7));
            BoWeiPCRStatus.pCRCurrentTupeDataList.add(new BoWeiPCRTupeData(8, 8, 8, 8, 8, 8, 8));

            BoWeiPCRStatus.pCRCurrentTemperatureList.clear();
            BoWeiPCRStatus.pCRCurrentTemperatureList.add(new BoWeiPCRTupeTempretureData(1, 1));
            BoWeiPCRStatus.pCRCurrentTemperatureList.add(new BoWeiPCRTupeTempretureData(2, 2));
            BoWeiPCRStatus.pCRCurrentTemperatureList.add(new BoWeiPCRTupeTempretureData(3, 3));
            BoWeiPCRStatus.pCRCurrentTemperatureList.add(new BoWeiPCRTupeTempretureData(4, 4));
            BoWeiPCRStatus.pCRCurrentTemperatureList.add(new BoWeiPCRTupeTempretureData(5, 5));
            BoWeiPCRStatus.pCRCurrentTemperatureList.add(new BoWeiPCRTupeTempretureData(6, 6));
            BoWeiPCRStatus.pCRCurrentTemperatureList.add(new BoWeiPCRTupeTempretureData(7, 7));
            BoWeiPCRStatus.pCRCurrentTemperatureList.add(new BoWeiPCRTupeTempretureData(8, 8));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //关闭前一个连接
                    closeConnection();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
//                        connectingFlag = false;
//                        return;
                    }
                    //新开一个连接
                    initConnection(context, uartnameBoWei, new BoWeiConnectCallback() {
                        @Override
                        public void serialConnectSuccess(Object object) {

                            is_BoWei_connect = true;
                            connectingFlag = false;
                            Log.i("BoWeiConnectManager", "BoWeiConnectManager connect sucess" + "connectingFlag" + connectingFlag);
                        }
                        @Override
                        public void serialConnectFail(Object object) {
                            Log.i("BoWeiConnectManager", "BoWeiConnectManager connect fail");
                            is_BoWei_connect = false;
                            connectingFlag = false;
                        }});
                }
            }).start();
        }else{
            Log.i(TAG, "BoWei uart not exist");
            is_BoWei_connect = false;
            connectingFlag = false;
            if(mHandleDisThread== null ){
                mHandleDisThread = new HandleDisconnectThread();
                mHandleDisThread.start();
            }

        }

    }


    /**
     * 识别ftdi芯片的USB驱动
     */
    private String uartnameBoWei = "ttyUSB";
    public void GetTtyUsbPath() {

        synchronized (BoWeiConnectManager.this) {
            String devices = "/sys/bus/usb-serial/devices/";
            File file = new File(devices);
            File[] ttyUSBList = file.listFiles();
            uartnameBoWei = "ttyUSB";

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
                                uartnameBoWei = temp.getName();
                                Log.i("BoweiConnecttion", "uartnameBowei" + uartnameBoWei);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "usbserial : " + e.getMessage());
                        }
                    }
                }
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(BoWeiRecvDataInfo boWeiRecvDataInfo) {
        is_BoWei_connect = true;
        PcrApplication.is_SLAN_PCR_connect = true;
        count = 0;
        Log.e(TAG, "heartbreak rece" + boWeiRecvDataInfo.getRecvData() + boWeiRecvDataInfo.getCmd());
    }
}
