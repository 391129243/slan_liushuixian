package com.winters.boweilibrary.BoWei.serialcom;

import android.content.Context;
import android.util.Log;

import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRStepEntity;
import com.winters.boweilibrary.BoWei.event.BoWeiRecvDataInfo;
import com.winters.boweilibrary.BoWei.event.BoWeiSendCmdFailedEvent;
import com.winters.boweilibrary.BoWei.OnSerialConnectListener;
import com.winters.boweilibrary.BoWei.status.BoWeiPCRStatus;
import com.gidi.bio_pcr.WintersUtils.NumUtils;

import com.gidi.bio_pcr.util.DateFormatUtil;
import com.kongqw.serialportlibrary.Device;
import com.kongqw.serialportlibrary.SerialPortFinder;
import com.kongqw.serialportlibrary.SerialPortManager;
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener;
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener;
import com.winters.boweilibrary.BoWei.utils.BoWeiUtils;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;


public class BoWeiConnection {
    private static String TAG = "BoWeiConnection";
    private int BaudRate = BoWeiConfig.BaudRate;
    private static BoWeiConnection mInstance;
    private SerialPortManager mSerialPortManager;

    private static StringBuilder stringBuilder = new StringBuilder();

    private Queue<Integer> mReceivePool = new LinkedList<Integer>();

    private  boolean init =false;

    private Object lock = new Object();

    private OnSerialConnectListener onSerialConnectListener;
    public void setOnSerialConnectListener(OnSerialConnectListener listener){
        onSerialConnectListener = listener;
    }



    /*******************************************************************************************/
    private BoWeiConnection(){
        mSerialPortManager = new SerialPortManager();
    }

    public static BoWeiConnection getInstance(){
        if (null == mInstance) {
            synchronized (BoWeiConnection.class){
                if (null == mInstance){
                    mInstance = new BoWeiConnection();
                }
            }
        }
        return mInstance;
    }

    public void initConnection(Context context,String serial_port,final BoWeiConnectCallback connectCallback){
        Log.i("PcrApplication", "333initConnection" );
        if(null != mSerialPortManager) {
            mSerialPortManager.closeSerialPort();
            mSerialPortManager = null;
        }
        if(null == mSerialPortManager){
            mSerialPortManager = new SerialPortManager();
        }
        //打开串口监听

        mSerialPortManager.setOnOpenSerialPortListener(new OnOpenSerialPortListener() {
            @Override
            public void onSuccess(File device) {
                Log.i(TAG, "openSerialSuccess" + device.getName());
                if(null != connectCallback){
                    Log.i("PcrApplication", "bowei_openSerialSuccess");
                    connectCallback.serialConnectSuccess(device.getName());
                }
            }

            @Override
            public void onFail(File device, Status status) {
                Log.i(TAG, "openSerialFail" + device.getName());
                Log.i("PcrApplication", "bowei_openSerialFail");
                if(null != connectCallback){
                    connectCallback.serialConnectFail(device.getName());
                }
            }
        });

        boolean result = openSerialPort(context,serial_port);
        if(!result){
            restartPort(context);
        }else{
            init =true;
        }

    }

    private boolean sendPacketMessage(BoWeiPCRStepEntity packet){
        Log.i(TAG, "send packet start");
        try {
            byte[] encodeBytes = NumUtils.StringList16To16HexList(packet.getPCRCmd());
            boolean status = sendSerialCmd(encodeBytes);
            Log.i(TAG, "send packet bytes" + Arrays.toString(encodeBytes) + " status: " + status);
            return status;
        } catch (Exception e) {
            Log.e(TAG, "send packet encode error " + e.getMessage());
            return false;
        }
    }

    public boolean openSerialPort(Context context,String serial_port){
        Log.i("PcrApplication", "444openSerialPort" );
        boolean result = openSerialPortDef(context,BaudRate,serial_port);
        //
        Log.i("PcrApplication", "555bowei_openSerialSuccess result" + result);

        if(mSerialPortManager == null){
            return false;
        }

        if(result){
            //打开串口成功，则接收数据
            mSerialPortManager.setOnSerialPortDataListener(new OnSerialPortDataListener() {
                @Override
                public void onDataReceived(byte[] bytes) {
                    //串口接收的数据
                 //   Log.i("PcrApplication", "666onDataReceived" + bytes.toString());
                    onReceiveMessage(bytes);
                }
                @Override
                public void onDataSent(byte[] bytes) {

                }
            });
        }
        return result;
    }

    public void closeSerialPort(){
        if(null != mSerialPortManager){
            mSerialPortManager.closeSerialPort();
            init =false;
            mSerialPortManager = null;
        }
        synchronized (mReceivePool) {
            mReceivePool.clear();
        }

        stringBuilder.delete(0,stringBuilder.length());
    }


    public boolean restartPort(Context context) {
        Log.i(TAG, "restartPort");
        // closeSerialPort();
        return openSerialPort(context,null);
    }

    private boolean openSerialPortDef(Context context,int rate,String port){
        SerialPortFinder serialPortFinder = new SerialPortFinder();
        ArrayList<Device> devices = serialPortFinder.getDevices();
        boolean isOpenSerialPort = false;
        for(int i=0;i<devices.size();i++){
            if(devices.get(i).getName().equals(port)){
                isOpenSerialPort = mSerialPortManager.openSerialPort(devices.get(i).getFile(), rate);
            }
        }
        return isOpenSerialPort;
    }







    /**
     * 串口接收数据
     * @param bytes
     */
    private void onReceiveMessage(byte[] bytes){

        String newData = NumUtils.HexListToString16(bytes);

        stringBuilder.append(newData);

        while (true)
        {
            String processData = stringBuilder.toString().toUpperCase();

            if (processData.indexOf("5E ") == -1)////消息队列不存在5E，本消息是无效字节，抛弃
            {
                stringBuilder.setLength(0);
                break;
            }

            int indexstart = processData.indexOf("5E ");

            String substring = processData.substring(processData.indexOf("5E "));///5E字节头

            if (substring.length() >= 12) /////如果当前字节数大于4读取长度位，长度位在第4个字节
            {

                String string_length = substring.substring(9, 11);////长度位在第4个字节

                int length = NumUtils.String16ToByte(string_length) & 0xff;

                if (length == 0)
                {
                    stringBuilder.delete(0, indexstart + 3 + 1);/////长度不正确，抛弃此消息remove "5E",尝试处理下一个5E
                    continue;
                }

                if (substring.length() >= length * 3) ////判断本数据总字节数是否大于字节定义的长度
                {

                    substring = substring.substring(0, length * 3-1);

                    byte[] HexList = NumUtils.StringList16To16HexList(substring);

                    if (BoWeiUtils.checkCRC(HexList))///////CRC校验
                    {

                        byte function = HexList[1];

                        if ((function & 0x80) == 0x80) //////判断功能码消息最高位是否为0x80
                        {
                            BoWeiPCRStatus.heartBeatCount = 0;
                            if ((function & 0x7F) == 0x01) /////握手协议回复
                            {
                                //////握手协议的成功处理触发
                                if ((HexList[4] & 0xFF) == 0)
                                {
                                    String text = "recv(" + DateFormatUtil.getNowTime() + "):" + "握手成功" + "\r\n";
                                    EventBus.getDefault().postSticky(new BoWeiRecvDataInfo(text, substring,"set_heartbeat","OK"));
                                }
                                else
                                {
                                    String text = "recv(" + DateFormatUtil.getNowTime() + "):" + "握手失败" + "\r\n";
                                    EventBus.getDefault().postSticky(new BoWeiRecvDataInfo(text, substring,"set_heartbeat","ERROR"));
                                }
                            }
                            else if ((function & 0x7F) == 0x06) /////设置PCR温度回复
                            {
                                if ((HexList[4] & 0xFF) == 0)
                                {
                                    String text = "recv(" + DateFormatUtil.getNowTime() + "):" + "设置PCR温度成功" + "\r\n";
                                    EventBus.getDefault().postSticky(new BoWeiRecvDataInfo(text, substring, "pcr_temperature_set", "OK"));
                                }
                                else
                                {
                                    String text = "recv(" + DateFormatUtil.getNowTime() + "):" + "设置PCR温度失败" + "\r\n";
                                    EventBus.getDefault().postSticky(new BoWeiRecvDataInfo(text, substring, "pcr_temperature_set", "ERROR"));
                                }
                            }
                            else if ((function & 0x7F) == 0x07) /////设置电机控制回复
                            {
                                if ((HexList[4] & 0xFF) == 0)
                                {
                                    String text = "recv(" + DateFormatUtil.getNowTime() + "):" + "电机控制成功" + "\r\n";
                                    EventBus.getDefault().postSticky(new BoWeiRecvDataInfo(text, substring, "motor_", "OK"));
                                }
                                else
                                {
                                    String text = "recv(" + DateFormatUtil.getNowTime() + "):" + "电机控制失败" + "\r\n";
                                    EventBus.getDefault().postSticky(new BoWeiRecvDataInfo(text, substring, "motor_", "ERROR"));
                                }
                            }
                            else if ((function & 0x7F) == 0x09) /////获取温度指令回复(4 热盖平均值)
                            {
                                //////模块没有热盖
                                float temperature = ((int)(HexList[4] & 0xFF)) * 256 + ((int)HexList[5] & 0xFF);

                                temperature = temperature / 100;

                                int Hotlid_temperature_reach = HexList[6] & 0xFF;

                                if ((HexList[7] & 0xFF) != 0)
                                {
                                    //////操作结果（0：操作成功；X：在状态表中查询）
                                }

                                BoWeiPCRStatus.hotlid_temperature = temperature;

                                if (Hotlid_temperature_reach == 1)
                                {
                                    BoWeiPCRStatus.Hotlid_temperature_reach = true;
                                }
                                else
                                {
                                    BoWeiPCRStatus.Hotlid_temperature_reach = false;
                                }
                                //String text = "recv(" + DateFormatUtil.getNowTime() + "):" + "热盖温度" + temperature + "\r\n";
                                //EventBus.getDefault().postSticky(new RecvDataInfo(text, substring));

                            }
                            else if ((function & 0x7F) == 0x0A) /////设置热盖温度回复
                            {
                                //////没有热盖
                                if ((HexList[4] & 0xFF) == 0)
                                {
                                    //////操作结果（0：操作成功；X：在状态表中查询）
                                    String text = "recv(" + DateFormatUtil.getNowTime() + "):" + "设置热盖成功" + "\r\n";
                                    EventBus.getDefault().postSticky(new BoWeiRecvDataInfo(text, substring, "hotlid_temperature_set", "OK"));
                                }
                                else
                                {
                                    String text = "recv(" + DateFormatUtil.getNowTime() + "):" + "设置热盖成功" + "\r\n";
                                    EventBus.getDefault().postSticky(new BoWeiRecvDataInfo(text, substring, "hotlid_temperature_set", "ERROR"));
                                }
                            }
                            else if ((function & 0x7F) == 0x0C) /////获取PCR数据回复
                            {
                                for (int i = 0; i < 8; i++)
                                {
                                    float fluorescence_FAM = 0.0f;
                                    float fluorescence_CY5 = 0.0f;
                                    float fluorescence_HEX = 0.0f;
                                    float fluorescence_ROX = 0.0f;
                                    fluorescence_FAM = (float)((HexList[4 + i * 2] & 0xFF) + (HexList[4 + i * 2 + 1] & 0xFF) * 256);
                                    fluorescence_CY5 = (float)((HexList[4 + i * 2 + 32] & 0xFF) + (HexList[4 + i * 2 + 1 + 32] & 0xFF) * 256);
                                    fluorescence_HEX = (float)((HexList[4 + i * 2 + 64] & 0xFF) + (HexList[4 + i * 2 + 1 + 64] & 0xFF) * 256);
                                    fluorescence_ROX = (float)((HexList[4 + i * 2 + 96] & 0xFF) + (HexList[4 + i * 2 + 1 + 96] & 0xFF) * 256);
                                    BoWeiPCRStatus.pCRCurrentTupeDataList.get(i).setFluorescence_FAM(fluorescence_FAM);
                                    BoWeiPCRStatus.pCRCurrentTupeDataList.get(i).setFluorescence_CY5(fluorescence_CY5);
                                    BoWeiPCRStatus.pCRCurrentTupeDataList.get(i).setFluorescence_HEX(fluorescence_HEX);
                                    BoWeiPCRStatus.pCRCurrentTupeDataList.get(i).setFluorescence_ROX(fluorescence_ROX) ;
                                }
                                String text = "recv(" + DateFormatUtil.getNowTime() + "):" + "获取PCR数据成功" + "\r\n";
                                EventBus.getDefault().postSticky(new BoWeiRecvDataInfo(text, substring, "pcr_fluorescence_get", "OK", BoWeiPCRStatus.pCRCurrentTemperatureList, BoWeiPCRStatus.pCRCurrentTupeDataList));
                            }
                            else if ((function & 0x7F) == 0x0F) /////设置状态灯回复
                            {
                                if ((HexList[4] & 0xFF) == 0)
                                {
                                    String text = "recv(" + DateFormatUtil.getNowTime() + "):" + "设置状态灯成功" + "\r\n";
                                    EventBus.getDefault().postSticky(new BoWeiRecvDataInfo(text, substring, "statusled", "OK"));
                                }
                                else
                                {
                                    String text = "recv(" + DateFormatUtil.getNowTime() + "):" + "设置状态灯失败" + "\r\n";
                                    EventBus.getDefault().postSticky(new BoWeiRecvDataInfo(text, substring, "statusled", "ERROR"));
                                }
                            }
                            else if ((function & 0x7F) == 0x43) /////获取PCR温度回复
                            {

                                float temperature1 = ((int)(HexList[4] & 0xFF)) * 256 + ((int)HexList[5] & 0xFF);
                                float temperature2 = ((int)(HexList[6] & 0xFF)) * 256 + ((int)HexList[7] & 0xFF);
                                float temperature3 = ((int)(HexList[8] & 0xFF)) * 256 + ((int)HexList[9] & 0xFF);

                                temperature1 = temperature1 / 100.0f;
                                temperature2 = temperature2 / 100.0f;
                                temperature3 = temperature3 / 100.0f;

                                int PCR_temperature_reach = HexList[16] & 0xFF;

                                BoWeiPCRStatus.temperature1 = temperature1;
                                BoWeiPCRStatus.temperature2 = temperature2;
                                BoWeiPCRStatus.temperature3 = temperature3;

                                BoWeiPCRStatus.pCRCurrentTemperatureList.get(0).setTemperature(temperature1);
                                BoWeiPCRStatus.pCRCurrentTemperatureList.get(1).setTemperature(temperature1);
                                BoWeiPCRStatus.pCRCurrentTemperatureList.get(2).setTemperature(temperature1);

                                BoWeiPCRStatus.pCRCurrentTemperatureList.get(3).setTemperature(temperature2);
                                BoWeiPCRStatus.pCRCurrentTemperatureList.get(4).setTemperature(temperature2);
                                BoWeiPCRStatus.pCRCurrentTemperatureList.get(5).setTemperature(temperature2);

                                BoWeiPCRStatus.pCRCurrentTemperatureList.get(6).setTemperature(temperature3);
                                BoWeiPCRStatus.pCRCurrentTemperatureList.get(7).setTemperature(temperature3);

                                BoWeiPCRStatus.pCRCurrentTupeDataList.get(0).setTemperature_pre(temperature1);
                                BoWeiPCRStatus.pCRCurrentTupeDataList.get(1).setTemperature_pre(temperature1);
                                BoWeiPCRStatus.pCRCurrentTupeDataList.get(2).setTemperature_pre(temperature1);

                                BoWeiPCRStatus.pCRCurrentTupeDataList.get(3).setTemperature_pre(temperature2);
                                BoWeiPCRStatus.pCRCurrentTupeDataList.get(4).setTemperature_pre(temperature2);
                                BoWeiPCRStatus.pCRCurrentTupeDataList.get(5).setTemperature_pre(temperature2);

                                BoWeiPCRStatus.pCRCurrentTupeDataList.get(6).setTemperature_pre(temperature3);
                                BoWeiPCRStatus.pCRCurrentTupeDataList.get(7).setTemperature_pre(temperature3);

                                BoWeiPCRStatus.pCRCurrentTupeDataList.get(0).setTemperature_post(temperature1);
                                BoWeiPCRStatus.pCRCurrentTupeDataList.get(1).setTemperature_post(temperature1);
                                BoWeiPCRStatus.pCRCurrentTupeDataList.get(2).setTemperature_post(temperature1);

                                BoWeiPCRStatus.pCRCurrentTupeDataList.get(3).setTemperature_post(temperature2);
                                BoWeiPCRStatus.pCRCurrentTupeDataList.get(4).setTemperature_post(temperature2);
                                BoWeiPCRStatus.pCRCurrentTupeDataList.get(5).setTemperature_post(temperature2);

                                BoWeiPCRStatus.pCRCurrentTupeDataList.get(6).setTemperature_post(temperature3);
                                BoWeiPCRStatus.pCRCurrentTupeDataList.get(7).setTemperature_post(temperature3);

                                if (PCR_temperature_reach == 1)
                                {
                                    BoWeiPCRStatus.pCR_temperature_reach = true;
                                    String text = "recv(" + DateFormatUtil.getNowTime() + "):" + "Reach_PCR温度:" + temperature1 + ";" + temperature2 + ";" + temperature3 + "\r\n";
                                    EventBus.getDefault().postSticky(new BoWeiRecvDataInfo(text, substring, "get_current_temperature_reach", "OK", BoWeiPCRStatus.pCRCurrentTemperatureList, BoWeiPCRStatus.pCRCurrentTupeDataList,true));
                                }
                                else
                                {
                                    BoWeiPCRStatus.pCR_temperature_reach = false;
                                    String text = "recv(" + DateFormatUtil.getNowTime() + "):" + "unReach_PCR温度:" + temperature1 + ";" + temperature2 + ";" + temperature3 + "\r\n";
                                    EventBus.getDefault().postSticky(new BoWeiRecvDataInfo(text, substring, "get_current_temperature_unreach", "OK", BoWeiPCRStatus.pCRCurrentTemperatureList, BoWeiPCRStatus.pCRCurrentTupeDataList,false));
                                }
                            }
                            else if ((function & 0x7F) == 0x91) /////读EEPROM指令回复
                            {
                            }
                            else if ((function & 0x7F) == 0x92) /////写EEPROM指令回复
                            {
                            }
                            stringBuilder.delete(0, indexstart + length * 3);
                        }
                        else  //////错误消息
                        {
                            stringBuilder.delete(0, indexstart + 3);//////CRC校验不正确，可能是消息中附带5E，抛弃此消息remove "5E",尝试处理下一个5E
                            continue;
                        }
                    }
                    else
                    {
                        stringBuilder.delete(0, indexstart + 3);//////CRC校验不正确，可能是消息中附带5E，抛弃此消息remove "5E",尝试处理下一个5E
                        continue;
                    }
                }
                else
                {
                    break; ////目前数据的字节数不足，等待下下一条消息拼接
                }
            }
            else
            {
                break;  ////目前数据的字节数不足4字节，等待下下一条消息拼接
            }
        }
    }

    private boolean isValid(byte[] data) {
        int length = data[2]*256 + data[3];
        if(data.length - 10 != length){
            return false;
        }else{
            int sum = 0;
            for (int index = 5; index < data.length - 5; index++) {
                sum += (data[index] & 0xFF);
            }
            sum = sum & 0xFFFF;
            int crc = (data[data.length - 5]& 0xFF)* 256 + (data[data.length - 4]& 0xFF);
            if(crc != sum){
                return false;
            }
        }

        return true;
    }


    /**
     * 发送串口消息
     */
    private boolean sendSerialCmd(byte[] cmd){
        try {
            Log.i("sendSerialCmd", "sendSerialCmd:" + NumUtils.HexListToString16(cmd));
            if(cmd != null)
            {
                if (mSerialPortManager != null) {
                    mSerialPortManager.sendBytes(cmd);
                }
            }
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(TAG, "sendSerialCmd:" + ex.getMessage());
            return false;
        }
    }

    class PacketEntity {

        private BoWeiPCRStepEntity sendPacket = null;
        private boolean isNeedAck = false;
        private long lastSendTime = 0;
        private int retryCount = 0;
        private ActionListener callback = null;
        private boolean isHasAcked = false;

        public PacketEntity(BoWeiPCRStepEntity packet, ActionListener callback){
            this.sendPacket = packet;
            this.callback = callback;
            this.isNeedAck = true;
        }

        public PacketEntity(BoWeiPCRStepEntity packet){
            this.sendPacket = packet;
            this.isNeedAck = false;
        }

        public PacketEntity(BoWeiPCRStepEntity packet, boolean isNeedAck){
            this.sendPacket = packet;
            this.isNeedAck = isNeedAck;
        }

        public BoWeiPCRStepEntity getPacket(){
            return sendPacket;
        }

        public boolean isNeedAck(){
            return isNeedAck;
        }

        public boolean isHasAcked(){
            return isHasAcked;
        }

        public void setAcked(){
            isHasAcked = true;
        }

        public long getLastSendTime(){
            return lastSendTime;
        }

        public void setLastSendTime(long lastSendTime){
            this.lastSendTime = lastSendTime;
        }

        public void increaseRetryCoutn(){
            retryCount += 1;
        }

        public void resetRetryCount(){
            retryCount = 0;
        }

        public int getRetryCount(){
            return retryCount;
        }


        public void onSuccess(){
            if(callback != null){
                callback.onSuccess();
            }
        }

        public void onFailed(int error){
            if(callback != null) {
                callback.onFailed(error);
            }
        }

    }

    public interface ActionListener {
        public void onSuccess();
        public void onFailed(int errorCode);
    }

    public void reset(){
        synchronized (mReceivePool) {
            mReceivePool.clear();
        }
    }


    public void sendMessage(BoWeiPCRStepEntity packet) {
        synchronized (lock) {
//            try {
//                Thread.sleep(300);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
            boolean result = sendPacketMessage(packet);
            if (!result) {
                EventBus.getDefault().post(new BoWeiSendCmdFailedEvent(packet));
            }

        }
    }

}
