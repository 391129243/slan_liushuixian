package com.gidi.bio_pcr.connect;

import android.content.Context;
import android.os.Looper;
import android.os.Message;
import android.util.Log;


import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.WintersUtils.entity.ByteBuilder;
import com.gidi.bio_pcr.WintersUtils.entity.RNARunningMessage;

import android.os.Handler;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;

public class ConnectManager implements OnSerialConnectListener{
    private static final String TAG = "ConnectManager";
    private static ConnectManager mInstance;
    private Context context;
    private HandleDisconnectThread mHandleDisThread;


    public static ConnectManager getInstance(){
        if (null == mInstance) {
            synchronized (ConnectManager.class){
                if (null == mInstance){
                    mInstance = new ConnectManager();
                }
            }
        }
        return  mInstance;
    }

    private ConnectManager(){
    }

    public void initConnection(Context context, String serial_port, final  ConnectCallback connectCallback){
        this.context = context;
        Connection.getInstance().initConnection(context,serial_port,connectCallback);
        /*if(PcrApplication.isDewit) {
            if (mHandleDisThread == null) {
                mHandleDisThread = new HandleDisconnectThread();
                mHandleDisThread.start();
            } else {
                mHandleDisThread.interrupt();
                mHandleDisThread = null;
                mHandleDisThread = new HandleDisconnectThread();
                mHandleDisThread.start();
            }
        }*/
        Connection.getInstance().setOnSerialConnectListener(this);
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
//        ???????????
        Connection.getInstance().closeSerialPort();
    }

    public void sendSerialCmdMsg_NoAck(ByteBuilder cmd){
        Connection.getInstance().sendMessage(cmd);
    }

    public void sendSerialCmdMsg_NeedAck(ByteBuilder cmd, boolean isNeedAck){
        Connection.getInstance().sendMessage_NeedAck(cmd, isNeedAck);
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
    private long count = 0;
   /* private void checkLineStatus() {
        int pcrState = PCRManager.checkStatus();
        if(pcrState == 1){
            PcrApplication.is_PCR_connect = true;
            count = 0;
        }else{
            count++;
        }
        if(count > 3 ){
            PcrApplication.is_PCR_connect = false;
        }
        //PcrApplication.is_PCR_connect = true;
        MessageEvent stateMsg = new MessageEvent();
        stateMsg.setMsgType(MsgType.MSG_PCR_CONNECT_STATE);
        stateMsg.setData(PcrApplication.is_PCR_connect);
        EventBus.getDefault().post(stateMsg);
    }*/
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
                 //   checkLineStatus();
                    sendEmptyMessageDelayed(0,500);
                }
            };
            mStatusHandler.sendEmptyMessage(0);
            Looper.loop();
        }
    }


    /**************************************RNA处理*********************************/

    /**
     *定时监听RNA连接
     */
    private void checkRNAConnectStatus(){

    }

    /**
     *     初始化RNA的串口连接
     *     首先获取TTyusb的路径
     */

    private boolean connectingFlag = false;
    public void initRNAConnection(Context context){
        //
        if(connectingFlag == true){
            return;
        }
        connectingFlag = true;
        GetTtyUsbPath();
//        closeConnection();
        if(!uartnameRNA.equals("ttyUSB")) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //关闭前一个连接
                    closeConnection();
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
//                        //throw new RuntimeException(e);
                        return;
                    }
                    //新开一个连接
                    initConnection(context, uartnameRNA, new ConnectCallback() {
                        @Override
                        public void serialConnectSuccess(Object object) {
                            Log.i(TAG, "ConnectManager connect sucess");
                            PcrApplication.is_RNA_connect = true;
                            connectingFlag =false;
                        }

                        @Override
                        public void serialConnectFail(Object object) {
                            Log.i(TAG, "ConnectManager connect fail");
                            PcrApplication.is_RNA_connect = false;
                            connectingFlag =false;
                        }

                        @Override
                        public void serialRevData(RNARunningMessage rNARunningMessage) {

                        }

                        @Override
                        public void serialSendData() {

                        }
                    });}
            }).start();
        }else{
            connectingFlag =false;
            Log.i(TAG, "RNA_App not exist");
        }

    }


    /**
     * 识别CH340芯片的USB驱动
     */

    private String uartnameRNA = "ttyUSB";
    private String uartnamePCR = "ttyUSB";
    public void GetTtyUsbPath() {

        synchronized (ConnectManager.this) {
            String devices = "/sys/bus/usb-serial/devices/";
            File file = new File(devices);
            File[] ttyUSBList = file.listFiles();
            uartnameRNA = "ttyUSB";

            if (ttyUSBList != null && ttyUSBList.length > 0) {
                for (int i = 0; i < ttyUSBList.length; i++) {
                    File temp = ttyUSBList[i];
                    if (temp.isDirectory() && temp.getName().contains("ttyUSB")) {
                        File usbserial = new File(temp.getPath() + "/uevent");
                        LineNumberReader readerProduct = null;
                        try {
                            readerProduct = new LineNumberReader(new FileReader(usbserial));
                            String info = readerProduct.readLine();
                            if (info.contains("cp210x")||info.contains("pl2303")) {
                                uartnamePCR = temp.getName();
                                //uartnameRNA = temp.getName();
                                Log.i(TAG, "uartnamePCR" + uartnamePCR);
                                Log.i(TAG, "uartname" + uartnameRNA);
                            } else if (info.contains("ch34")) {
                                //uartnamePCR = temp.getName();
                                uartnameRNA = temp.getName();
                                Log.i(TAG, "uartnameRNA" + uartnameRNA);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "usbserial : " + e.getMessage());
                        }

                    }

                }
            }
        }
    }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


}
