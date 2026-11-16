package com.gzlab.autoassemblyline_client.connect;

import android.content.Context;
import android.util.Log;


import com.gzlab.autoassemblyline_client.PcrApplication;
import com.gzlab.autoassemblyline_client.WintersUtils.entity.ByteBuilder;
import com.gzlab.autoassemblyline_client.event.MessageEvent;
import com.gzlab.autoassemblyline_client.event.MsgType;
import com.hongshi.slansdk.listener.DeviceStateListener;
import com.hongshi.slansdk.slanenum.DeviceState;


import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;

public class ConnectManager implements OnSerialConnectListener{
    private static final String TAG = "ConnectManager";
    private static ConnectManager mInstance;
    private Context context;

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
       /* if(PcrApplication.isDewit) {
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
       /* if(null != mHandleDisThread){
            mHandleDisThread.interrupt();
            mHandleDisThread = null;
        }*/
       /* if(mStatusHandler != null){
            mStatusHandler.removeCallbacksAndMessages(null);
            mStatusHandler = null;
        }*/
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


    //private Handler mStatusHandler;

    @Override
    public void onReceiveSerialMsg(String msg) {
        onHandlerSerialMsg(msg);
    }

    @Override
    public void onReceiveRNARunningMsg() {

    }



    /***
     *
     * @author 检查系统链路的线程
     *
     */
/*    private class HandleDisconnectThread extends Thread{

        @Override
        public void run() {
            // TODO Auto-generated method stub
            super.run();
            Looper.prepare();
            mStatusHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {

                    sendEmptyMessageDelayed(0,500);
                }
            };
            mStatusHandler.sendEmptyMessage(0);
            Looper.loop();
        }
    }*/


    /**************************************RNA处理*********************************/

    /**
     *定时监听RNA连接
     */
    private void checkRNAConnectStatus(){

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
                                uartnameRNA = temp.getName();
                                Log.i("Connecttion", "uartnamePCR" + uartnamePCR);
                                Log.i("Connecttion", "uartname" + uartnameRNA);
                            } else if (info.contains("ch34")) {
                                uartnamePCR = temp.getName();
                                uartnameRNA = temp.getName();
                                Log.i("Connecttion", "uartnamePCR" + uartnamePCR);
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

    private int slanConnect_count = 0;
    /**
     * 初始化SLAN连接
     */
    public void initSLANPcrConnection(){

//        if(PcrApplication.currentDevices != null){
//            PcrApplication.currentDevices.reConnect();
//            return;
//        }
        Log.e(TAG, "PCR重连222");
        PcrApplication.devices =  PcrApplication.slanService.getDevice();
        Log.e(TAG, "PCR重连222 PcrApplication.devices.size() = " + PcrApplication.devices.size());
        if(PcrApplication.devices == null || PcrApplication.devices.isEmpty()){
            PcrApplication.is_SLAN_PCR_connect = false;//0331改f
        }else{
            PcrApplication.is_SLAN_PCR_connect = true;
            PcrApplication.currentDevices = PcrApplication.devices.get(0);
            if (null != PcrApplication.slanService) {
                PcrApplication.slanService.setDeviceStateListener(new DeviceStateListener() {
                    @Override
                    public void getDeviceInfo(@NonNull String s, @NonNull DeviceState deviceState) {
                        Log.i("ABCD", "getDeviceInfo： " + deviceState);
                        Log.i("ABCD", "s： " + s);

                        if (deviceState == DeviceState.Error) {
                            //通信出错
                            slanConnect_count ++;
                            if(slanConnect_count > 3) {
                                PcrApplication.is_SLAN_PCR_connect = false;
                            }
                        } else {
                            PcrApplication.is_SLAN_PCR_connect = true;
                            slanConnect_count = 0;
                        }
                        MessageEvent stateMsg = new MessageEvent();
                        stateMsg.setMsgType(MsgType.MSG_PCR_CONNECT_STATE);
                        EventBus.getDefault().postSticky(stateMsg);
                    }
                });
            }
        }
    }

    /**
     * 监听PCR连接
     */



}
