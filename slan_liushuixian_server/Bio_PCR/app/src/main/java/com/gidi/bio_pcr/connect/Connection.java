package com.gidi.bio_pcr.connect;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import com.gidi.bio_pcr.WintersUtils.NumUtils;
import com.gidi.bio_pcr.WintersUtils.constant.ErrorType;
import com.gidi.bio_pcr.WintersUtils.entity.ActionDoneMessage;
import com.gidi.bio_pcr.WintersUtils.entity.ActionMcuAckMessage;
import com.gidi.bio_pcr.WintersUtils.entity.ByteBuilder;
import com.gidi.bio_pcr.WintersUtils.entity.RNARunningMessage;
import com.gidi.bio_pcr.WintersUtils.entity.event.SendCmdSuccessEvent;
import com.gidi.bio_pcr.WintersUtils.entity.event.SendCmdTimeoutEvent;
import com.gidi.bio_pcr.WintersUtils.entity.WarningMessage;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.ColdActionDoneMessage;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.DoorActionDoneMessage;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.LedActionDoneMessage;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.google.gson.Gson;
import com.kongqw.serialportlibrary.Device;
import com.kongqw.serialportlibrary.SerialPortFinder;
import com.kongqw.serialportlibrary.SerialPortManager;
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener;
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener;


import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.gidi.bio_pcr.constant.SharedConstant;


public class Connection {
    private static String TAG = "Connection";
    private String def_port_name = Config.SERIAL_PORT_rk3399_1;
    private int BaudRate = Config.BaudRate;
    private static Connection mInstance;
    private SerialPortManager mSerialPortManager;

    private static StringBuilder stringBuilder = new StringBuilder();

    List<Byte> byteList = new ArrayList<Byte>();

    /////重发次数
    private static final int SEND_RETRY_COUNT = 5;
    /////接收到ACK的时间
    private static final long SEND_TIME_OUT = 3000;

    private static final long SEND_POOL_TIME_OUT = 1000;

    private List<PacketEntity> mSendPool = new ArrayList<PacketEntity>();
    private Queue<Integer> mReceivePool = new LinkedList<Integer>();

    private ExecutorService mMainExecutor;
    private boolean mIsRunning = false;

    private Thread mSendThread;

    private  boolean init =false;

    private void addToSendPool(PacketEntity packetWrapper){
        synchronized (mSendPool) {
            mSendPool.add(packetWrapper);
            mSendPool.notify();
        }
    }


    private OnSerialConnectListener onSerialConnectListener;
    public void setOnSerialConnectListener(OnSerialConnectListener listener){
        onSerialConnectListener = listener;
    }



    /*******************************************************************************************/
    private Connection(){
        mSerialPortManager = new SerialPortManager();
    }

    public static Connection getInstance(){
        if (null == mInstance) {
            synchronized (Connection.class){
                if (null == mInstance){
                    mInstance = new Connection();
                }
            }
        }
        return mInstance;
    }

    public void initConnection(Context context,String serial_port,final  ConnectCallback connectCallback){
        if(null == mSerialPortManager){
            mSerialPortManager = new SerialPortManager();
        }
        //打开串口监听

        mSerialPortManager.setOnOpenSerialPortListener(new OnOpenSerialPortListener() {
            @Override
            public void onSuccess(File device) {
                Log.i(TAG, "openSerialSuccess" + device.getName());
                if(null != connectCallback){
                    connectCallback.serialConnectSuccess(device.getName());
                }
            }

            @Override
            public void onFail(File device, Status status) {
                Log.i(TAG, "openSerialFail" + device.getName());
                if(null != connectCallback){
                    connectCallback.serialConnectFail(device.getName());
                }
            }
        });

        boolean result = openSerialPort(context,serial_port,connectCallback);
        if(!result){
            return;
        }else{
            init =true;
            setPortName(context,serial_port);
            mIsRunning = true;
            mSendThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    processSendPool();
                }
            }, "UART_Manager_Write_Thread");
            mSendThread.setDaemon(true);
            mSendThread.start();
            mMainExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread ret = new Thread(r, "UART_Main_Thread");
                    ret.setDaemon(true);
                    return ret;
                }
            });
        }

    }

    private void processSendPool(){
        while(mIsRunning){
            synchronized (mSendPool) {
                long minSleepTime = SEND_POOL_TIME_OUT;
                long currentTime = System.currentTimeMillis();
                Iterator<PacketEntity> iterator = mSendPool.iterator();
                while(iterator.hasNext()){
                    PacketEntity entity = iterator.next();
                    //已收到确认的消息
                    if(entity.isHasAcked()){
                        entity.onSuccess();
                        iterator.remove();
                        continue;
                    }

                    //重试次数超过的消息
                    if(entity.getRetryCount() >= SEND_RETRY_COUNT){
                        entity.onFailed(ErrorType.NO_ACK);
                        iterator.remove();
                        EventBus.getDefault().post(new SendCmdTimeoutEvent(entity.getPacket()));
                        continue;
                    }

                    //未收到确认重试次数也未超过的消息
                    if(currentTime - entity.getLastSendTime() >= SEND_TIME_OUT){
                        /////////发送串口信息

                        boolean result = sendPacketMessage(entity.getPacket());

                        EventBus.getDefault().post(new SendCmdSuccessEvent(entity.getPacket()));

                        if(entity.isNeedAck()){
                            entity.increaseRetryCoutn();;
                            entity.setLastSendTime(currentTime);
                        }else{
                            iterator.remove();
                        }
                    }else{
                        long sendInterval = SEND_TIME_OUT - (currentTime - entity.getLastSendTime());
                        if(sendInterval < minSleepTime){
                            //minSleepTime = sendInterval;
                        }
                    }
                }

                try {
                    mSendPool.wait(minSleepTime);
                } catch (InterruptedException e) {
                    //ignore
                    break;
                }
            }
        }

    }

    private void processAckPacket(final ByteBuilder msgPacket) {
        mMainExecutor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (mSendPool) {
                    Iterator<PacketEntity> iterator = mSendPool.iterator();
                    while(iterator.hasNext()){
                        PacketEntity entity = iterator.next();
                        if(entity.getPacket().getMsdID() == msgPacket.getMsdID()){
                            entity.setAcked();
                            EventBus.getDefault().post(entity.getPacket());
                            break;
                        }
                    }
                }
            }
        });
    }

//    private void sendPacket(ByteBuilder packet){
//        int retry_count = 0;
//        do {
//            Log.i(TAG, "send packet start");
//            try {
//                byte[] encodeBytes = packet.encodeBytes();
//                boolean status = sendSerialCmd(encodeBytes);
//                Log.i(TAG, "send packet bytes" + Arrays.toString(encodeBytes) + " status: " + status);
//                if (status) {
//                    break;
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "send packet encode error " + e.getMessage());
//                break;
//            }
//
//            if(retry_count ==3 ){
//
//                init = false;
//            }
//        } while (retry_count++ < 3);
//    }

    private boolean sendPacketMessage(ByteBuilder packet){

        Log.i(TAG, "send packet start");
        try {
            byte[] encodeBytes = packet.encodeBytes();
            boolean status = sendSerialCmd(encodeBytes);
            Log.i(TAG, "send packet bytes" + Arrays.toString(encodeBytes) + " status: " + status);
            return status;
        } catch (Exception e) {
            Log.e(TAG, "send packet encode error " + e.getMessage());
            return false;
        }

    }

    public boolean openSerialPort(Context context,String serial_port,ConnectCallback connectCallback){

        if(StringUtil.isEmpty(serial_port)){
            serial_port = getSerialport(context);
        }
        boolean result = openSerialPortDef(context,BaudRate,serial_port);
        //
        if(result){
            //打开串口成功，则接收数据
            Log.i(TAG, "打开串口成功，则接收数据");
            mSerialPortManager.setOnSerialPortDataListener(new OnSerialPortDataListener() {
                @Override
                public void onDataReceived(byte[] bytes) {
                    //串口接收的数据
                    //Log.i(TAG, "处理接收数据");
                    onReceiveMessage(bytes,connectCallback);
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
        }
        //////////////??????????
        if(mSendThread !=null && mSendThread.isAlive()){
            mSendThread.interrupt();
            mIsRunning = false;
        }
        synchronized (mReceivePool) {
            mReceivePool.clear();
        }
        synchronized (mSendPool) {
            mSendPool.clear();
        }

        if(mMainExecutor != null){
            mMainExecutor.shutdown();
        }

        stringBuilder.delete(0,stringBuilder.length());
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


    public boolean setPortName(Context context,String name) {
        if(StringUtil.isEmpty(name)){
            return false;
        }else {

            setSerialport(context, name);
            return true;
        }
    }

    private String getSerialport(Context context) {
        return PreferenceUtil.getInstance(context)
                .getStringValue(SharedConstant.SERIAL_PORT, def_port_name);
    }

    private void setSerialport(Context context,String port) {
        PreferenceUtil.getInstance(context).setValueByName(SharedConstant.SERIAL_PORT, port);
    }


    /**
     * 串口接收数据
     * @param bytes
     */
    private void onReceiveMessage(byte[] bytes, ConnectCallback connectCallback){

        String newData = NumUtils.HexListToString16(bytes);

        stringBuilder.append(newData);

        List<String> cmdList = new ArrayList<String>();

        //Log.i(TAG, "处理接收数据" + newData);

        while (true)
        {
            String processData = stringBuilder.toString().toUpperCase();

            if (processData.contains("5A A5") && processData.contains("FF FC FF"))
            {
                int indexHead = processData.indexOf("5A A5");

                int indexTail = processData.indexOf("FF FC FF");

                if (indexTail > indexHead)
                {
                    String str1 = processData.substring(indexHead, indexTail - indexHead + 8);

                    cmdList.add(str1);

                    stringBuilder.delete(0, indexTail + 8);
                }
                else
                {
                    stringBuilder.delete(0, indexTail + 8);

                }
            }
            else if(processData.contains("5A A5"))
            {

                int indexHead = processData.indexOf("5A A5");


                stringBuilder.delete(0, indexHead);

                break;
            }else{
                /////drop
                break;
            }



        }

        onHandlerSerialMsgArray(cmdList, connectCallback);

    }

    private boolean isValid(byte[] data) {
        int unsignedByte1 = data[2] & 0xFF;
        int unsignedByte2 = data[3] & 0xFF;

        int length = unsignedByte1*256 + unsignedByte2;
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

    private void onHandlerSerialMsgArray(List<String> msgArray, ConnectCallback connectCallback){
        for(String msg : msgArray){
            //Log.i("onHandlerSerialMsgArray","msg-"  + msg + "  len:" +msg.length());
            if(null != msg) {

                byte[] byteList = NumUtils.StringList16To16HexList(msg);

                if( byteList == null || byteList.length < 15){
                    return;
                }
                if(!isValid(byteList)){
                    Log.i("onHandlerSerialMsgArray","CRC ERROR");
                    return;
                }
                if((byteList[4]&0xFF) == 0xA4){

                    ActionMcuAckMessage actionMcuAckMessage = new ActionMcuAckMessage();

                    actionMcuAckMessage = (ActionMcuAckMessage) actionMcuAckMessage.decodeBytes(byteList);

                    //EventBus.getDefault().post(actionMcuAckMessage);

                    if(actionMcuAckMessage.getAlarm() == null){

                        processAckPacket(actionMcuAckMessage);

                    }else if(actionMcuAckMessage.getAlarm().trim().equals("")){

                        processAckPacket(actionMcuAckMessage);

                    }else{
                        Log.i("actionMcuAckMessage", "alarm" + actionMcuAckMessage.getAlarm());

                        WarningMessage warningMessage = new WarningMessage();

                        Gson gson = new Gson();

                        String text = gson.toJson(actionMcuAckMessage);

                        warningMessage.setAlarm("actionMcuAckMessage alarm: " + text);

                        EventBus.getDefault().post(warningMessage);
                    }

                }else if((byteList[4]&0xFF)  == 0xA2){

                    ActionDoneMessage actionDoneMessage = new ActionDoneMessage();

                    actionDoneMessage = (ActionDoneMessage) actionDoneMessage.decodeBytes(byteList);

                    EventBus.getDefault().post(actionDoneMessage);

                }else if((byteList[4]&0xFF)  == 0xA1){

                    RNARunningMessage rNARunningMessage = new RNARunningMessage();

                    rNARunningMessage = (RNARunningMessage) rNARunningMessage.decodeBytes(byteList);

                    Log.i("RNARunningMessage", "recv");

                    EventBus.getDefault().post(rNARunningMessage);

                    connectCallback.serialRevData(rNARunningMessage);

                }else if((byteList[4]&0xFF)  == 0xA3){

                    WarningMessage warningMessage = new WarningMessage();

                    warningMessage = (WarningMessage) warningMessage.decodeBytes(byteList);

                    EventBus.getDefault().post(warningMessage);

                }else if((byteList[4]&0xFF)  == 0xB1){
                    //door
                    DoorActionDoneMessage dooractionDoneMessage = new DoorActionDoneMessage();

                    dooractionDoneMessage = (DoorActionDoneMessage) dooractionDoneMessage.decodeBytes(byteList);

                    EventBus.getDefault().post(dooractionDoneMessage);

                }else if((byteList[4]&0xFF)  == 0xB2){
                    /// cold
                    ColdActionDoneMessage coldactionDoneMessage = new ColdActionDoneMessage();

                    coldactionDoneMessage = (ColdActionDoneMessage) coldactionDoneMessage.decodeBytes(byteList);

                    EventBus.getDefault().post(coldactionDoneMessage);

                }else if((byteList[4]&0xFF)  == 0xB6){
                    /// cold
                    LedActionDoneMessage ledActionDoneMessage = new LedActionDoneMessage();

                    ledActionDoneMessage = (LedActionDoneMessage) ledActionDoneMessage.decodeBytes(byteList);

                    EventBus.getDefault().post(ledActionDoneMessage);

                }


                if(null != onSerialConnectListener){
                    onSerialConnectListener.onReceiveSerialMsg(msg);
                }
            }
        }
    }


    public boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
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

        private ByteBuilder sendPacket = null;
        private boolean isNeedAck = false;
        private long lastSendTime = 0;
        private int retryCount = 0;
        private ActionListener callback = null;
        private boolean isHasAcked = false;

        public PacketEntity(ByteBuilder packet, ActionListener callback){
            this.sendPacket = packet;
            this.callback = callback;
            this.isNeedAck = true;
        }

        public PacketEntity(ByteBuilder packet){
            this.sendPacket = packet;
            this.isNeedAck = false;
        }

        public PacketEntity(ByteBuilder packet,boolean isNeedAck){
            this.sendPacket = packet;
            this.isNeedAck = isNeedAck;
        }

        public ByteBuilder getPacket(){
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
        synchronized (mSendPool) {
            for(PacketEntity entity : mSendPool){
                if(entity.isHasAcked()){
                    entity.onSuccess();
                    continue;
                }

                entity.onFailed(ErrorType.NO_ACK);
            }
            mSendPool.clear();
        }

        synchronized (mReceivePool) {
            mReceivePool.clear();
        }
    }

    public void sendMessage_NeedAck(final ByteBuilder packet,boolean isNeedAck) {
        if(!mIsRunning) return;
        mMainExecutor.execute(new Runnable() {
            @Override
            public void run() {
                PacketEntity entity = new PacketEntity(packet,isNeedAck);
                addToSendPool(entity);
            }
        });
    }

    public void sendMessage(final ByteBuilder packet) {
        if(!mIsRunning) return;
        mMainExecutor.execute(new Runnable() {

            @Override
            public void run() {
                PacketEntity entity = new PacketEntity(packet);
                addToSendPool(entity);
            }
        });
    }

}
