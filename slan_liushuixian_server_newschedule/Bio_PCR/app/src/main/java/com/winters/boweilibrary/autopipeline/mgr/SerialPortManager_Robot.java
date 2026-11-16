package com.winters.boweilibrary.autopipeline.mgr;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.gidi.bio_pcr.WintersUtils.AsyncTask.RNAAndPCRAsyncTask;
import com.gidi.bio_pcr.WintersUtils.NumUtils;
import com.gidi.bio_pcr.WintersUtils.entity.ByteBuilder;
import com.gidi.bio_pcr.WintersUtils.entity.MoveActionCmd;
import com.gidi.bio_pcr.WintersUtils.entity.RNARunningMessage;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexDebugEntity;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexSingleEntity;
import com.gidi.bio_pcr.WintersUtils.utils.AssetUtils;
import com.gidi.bio_pcr.WintersUtils.utils.DateUtil;
import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.gidi.bio_pcr.WintersUtils.utils.ListDataSaveUtil;
import com.gidi.bio_pcr.connect.ConnectCallback;
import com.gidi.bio_pcr.connect.ConnectManager;
import com.gidi.bio_pcr.connect.Connection;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.util.FileUtils;
import com.google.gson.reflect.TypeToken;
import com.winters.boweilibrary.autopipeline.model.item.ItemProperty;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.winters.boweilibrary.autopipeline.constant.Constants;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;
import static com.winters.boweilibrary.autopipeline.constant.Constants.ROBOT_ARMCONFIG_FILE_PATH;

public class SerialPortManager_Robot {
    private static final String TAG = "SerialPortManager_Robot";

    private static SerialPortManager_Robot mInstance;
    private Context mContext;
    private HandleDisconnectThread mHandleDisThread;

    private boolean is_ROBOT_connect = false;

    private boolean is_RecRobotRunningMesaage = false;

    private RNARunningMessage rNARunningMessage = new RNARunningMessage();

    private boolean robotState;

    public static List<ItemProperty> itemPropertyList;


    private RobotStatus robotStatus = new RobotStatus();

    private final Object robotStatusLock = new Object();

    public RobotStatus setRobotStatus() {
        synchronized (robotStatusLock) {
            robotStatus.setStatus(rNARunningMessage.getStatus());
            robotStatus.setDegree(rNARunningMessage.getDegree());
            robotStatus.setAlarm(rNARunningMessage.getAlarm());
            robotStatus.setNO(rNARunningMessage.getNO());
            robotStatus.setMotorID(rNARunningMessage.getMotorID());
            robotStatus.setROBOTconnect(is_ROBOT_connect);
            robotStatus.setROBOTTaskRunning(RNAAndPCRAsyncTask.getInstance().ismIsrunning());
            robotStatus.setSense(rNARunningMessage.getSense());
            robotStatus.createSenseMapList();
            return robotStatus;
            }
    }

    public RobotStatus setRobotStatusDisconnect() {
        synchronized (robotStatusLock) {
            robotStatus.setStatus("error");
            robotStatus.setDegree(-100);
            robotStatus.setAlarm("disconnect");
            robotStatus.setNO("error");
            robotStatus.setMotorID("disconnect");
            robotStatus.setROBOTconnect(is_ROBOT_connect);
            robotStatus.setROBOTTaskRunning(false);
            robotStatus.setSense(0);
            robotStatus.createSenseMapList();
            return robotStatus;
        }
    }


    public RobotStatus getRobotStatus() {
        synchronized (robotStatusLock) {
            return robotStatus;
            }
    }

    public boolean isIs_ROBOT_connect() {
        return is_ROBOT_connect;
    }

    public void setIs_ROBOT_connect(boolean is_ROBOT_connect) {
        this.is_ROBOT_connect = is_ROBOT_connect;
    }

    public boolean isIs_RecRobotRunningMesaage() {
        return is_RecRobotRunningMesaage;
    }

    public void setIs_RecRobotRunningMesaage(boolean is_RecRobotRunningMesaage) {
        this.is_RecRobotRunningMesaage = is_RecRobotRunningMesaage;
    }

    public RNARunningMessage getRobotRunningMessage() {
        return rNARunningMessage;
    }

    public void setRobotRunningMessage(RNARunningMessage robotRunningMessage) {
        this.rNARunningMessage = robotRunningMessage;
    }

    public List<ItemProperty> getItemPropertyList() {
        return itemPropertyList;
    }

    public void setItemPropertyList(List<ItemProperty> itemPropertyList) {
        this.itemPropertyList = itemPropertyList;
    }

    public static SerialPortManager_Robot getInstance(){

        if (null == mInstance) {
            synchronized (SerialPortManager_Robot.class){
                if (null == mInstance){
                    mInstance = new SerialPortManager_Robot();
                }
            }
        }
        return  mInstance;
    }

    private SerialPortManager_Robot(){


    }

    private void initConnection(Context context, String serial_port, final ConnectCallback connectCallback){
        this.mContext = context;

        Connection.getInstance().initConnection(context,serial_port,connectCallback);
        if(mHandleDisThread!= null ){
            mHandleDisThread.interrupt();
            mHandleDisThread = null;
        }
        mHandleDisThread = new HandleDisconnectThread();
        mHandleDisThread.start();
//        registerEventBus();
    }

//    protected void registerEventBus(){
//        if(!EventBus.getDefault().isRegistered(this)){
//            EventBus.getDefault().register(this);
//        }
//    }
//    protected void unRegisterEventBus(){
//        if(EventBus.getDefault().isRegistered(this)){
//            EventBus.getDefault().unregister(this);
//        }
//    }

    public void closeConnection(){
        if(null != mHandleDisThread){
            mHandleDisThread.interrupt();
            mHandleDisThread = null;
        }
        if(mStatusHandler != null){
            mStatusHandler.removeCallbacksAndMessages(null);
            mStatusHandler = null;
        }

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

    /**
     * 丁智PCR检测连接
     * @author 80657
     *
     */
    private long count = 0;
    private void checkLineStatus() {

        if(robotState){
            robotState = false;
            is_ROBOT_connect = true;
            count = 0;
        }else{
            count++;
        }
        if(count > 5 ){
            is_ROBOT_connect = false;
            robotStatus.setROBOTconnect(is_ROBOT_connect);
            setRobotStatusDisconnect();
            initRobotConnection(mContext);
            count = 0;
        }
        MessageEvent stateMsg = new MessageEvent();
        stateMsg.setMsgType(MsgType.MSG_ROBOT_CONNECT_STATE);
        stateMsg.setData(is_ROBOT_connect);
        EventBus.getDefault().post(stateMsg);
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
                    checkLineStatus();
                    sendEmptyMessageDelayed(0,4000);
                }
            };
            mStatusHandler.sendEmptyMessageDelayed(0,4000);
            Looper.loop();
        }
    }

    /**
     * 开机时初始化
     * @param context
     */
    public void init(Context context){
        mContext = context;

        if(itemPropertyList == null) {
            try {
                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/" + "module.itemproperty");

                if (!file.exists()) {

                    AssetUtils.copyFromAsset(mContext, "module.itemproperty", true);
                }

                FileInputStream inputStream = new FileInputStream(Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/" + "module.itemproperty");

                String strJson = FileUtils.fileToStr(inputStream);

                itemPropertyList = GsonUtil.fromJson(strJson, new TypeToken<List<ItemProperty>>() {});

                inputStream.close();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     *     初始化RNA的串口连接
     *     首先获取TTyusb的路径
     */
    private boolean connectingFlag = false;
    public void initRobotConnection(Context context){

        if(connectingFlag == true){
            return;
        }

        mContext = context;

//        if(itemPropertyList == null) {
//            try {
//                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
//                        + "/PCR/" + "module.itemproperty");
//
//                if (!file.exists()) {
//
//                    AssetUtils.copyFromAsset(mContext, "module.itemproperty", true);
//                }
//
//                FileInputStream inputStream = new FileInputStream(Environment.getExternalStorageDirectory().getAbsolutePath()
//                        + "/PCR/" + "module.itemproperty");
//
//                String strJson = FileUtils.fileToStr(inputStream);
//
//                itemPropertyList = GsonUtil.fromJson(strJson, new TypeToken<List<ItemProperty>>() {});
//
//                inputStream.close();
//
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }

        connectingFlag = true;
        GetTtyUsbPath();
//      closeConnection();
        if(!uartnameRobot.equals("ttyUSB")) {
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
                    initConnection(context, uartnameRobot, new ConnectCallback() {
                        @Override
                        public void serialConnectSuccess(Object object) {
                            Log.i(TAG, "SerialPortManager_Robot connect sucess");
                            //is_ROBOT_connect = true;
                            connectingFlag =false;
                        }

                        @Override
                        public void serialConnectFail(Object object) {
                            Log.i(TAG, "SerialPortManager_Robot connect fail");
                            is_ROBOT_connect = false;
                            connectingFlag =false;
//                            setRobotStatus();
                            setRobotStatusDisconnect();
                        }

                        @Override
                        public void serialRevData(RNARunningMessage message) {
                            Log.i(TAG, "serialRevData RNARunningMessage");
                            robotState = true;
                            rNARunningMessage = message;
                            setRobotStatus();
                        }

                        @Override
                        public void serialSendData() {

                        }
                    });
                }
            }).start();
        }else{
            connectingFlag =false;
            Log.e(TAG, "initRobotConnection failed");
        }

    }


    /**
     * 识别CH340芯片的USB驱动
     */

    private String uartnameRobot = "ttyUSB";
    public void GetTtyUsbPath() {

        synchronized (SerialPortManager_Robot.this) {
            String devices = "/sys/bus/usb-serial/devices/";
            File file = new File(devices);
            File[] ttyUSBList = file.listFiles();
            uartnameRobot = "ttyUSB";

            if (ttyUSBList != null && ttyUSBList.length > 0) {
                for (int i = 0; i < ttyUSBList.length; i++) {
                    File temp = ttyUSBList[i];
                    if (temp.isDirectory() && temp.getName().contains("ttyUSB")) {
                        File usbserial = new File(temp.getPath() + "/uevent");
                        LineNumberReader readerProduct = null;
                        try {
                            readerProduct = new LineNumberReader(new FileReader(usbserial));
                            String info = readerProduct.readLine();
                            if (info.contains("ch34")) {
                                //uartnamePCR = temp.getName();
                                uartnameRobot = temp.getName();
                                Log.i(TAG, "uartnameRobot==" + uartnameRobot);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "usbserial : " + e.getMessage());
                        }

                    }

                }
            }
        }
    }


    protected String getNewHexByPosition(String hex, int positionInt){

        byte[] hexList = NumUtils.StringList16To16HexList(hex);

        hexList[6] = (byte)((positionInt >> 24) & 0xff);
        hexList[7] = (byte)((positionInt >> 16) & 0xff);
        hexList[8] = (byte)((positionInt >> 8) & 0xff);
        hexList[9] = (byte)(positionInt & 0xff);

        int crc = 0;

        for (int i = 2; i <= 9; i++)
        {
            crc = crc + (int)(hexList[i]&0xFF);
        }

        hexList[10] = (byte)((crc >> 8) & 0xff);
        hexList[11] = (byte)(crc & 0xff);

        String hexcommand = NumUtils.HexListToString16(hexList).toUpperCase();

        return hexcommand;
    }



    protected void setParm(String template, List<HexDebugEntity> HexDebugEntityList,ItemProperty itemProperty){
        if(HexDebugEntityList == null || HexDebugEntityList.isEmpty()){
            return;
        }

        for (int i = 0; i < HexDebugEntityList.size(); i++) {
            if (HexDebugEntityList.get(i).getDescription().contains(Constants.CONFIG_GET_XY)) {
                for (int j = 0; j < HexDebugEntityList.get(i).getHexActionList().size(); j++) {
                    if(HexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(Constants.CONFIG_GET_X)){

                        String newHex = getNewHexByPosition(HexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) (itemProperty.getX()*1000.0f));

                        HexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        HexDebugEntityList.get(i).setDescription
                                (HexDebugEntityList.get(i).getDescription() + ":" + itemProperty.getX());

                    }else if(HexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(Constants.CONFIG_GET_Y)){

                        String newHex = getNewHexByPosition(HexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) (itemProperty.getY()*1000.0f));

                        HexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        HexDebugEntityList.get(i).setDescription
                                (HexDebugEntityList.get(i).getDescription() + ":" + itemProperty.getY());

                    }
                }
            }else if (HexDebugEntityList.get(i).getDescription().contains(Constants.CONFIG_GET_Z)) {
                for (int j = 0; j < HexDebugEntityList.get(i).getHexActionList().size(); j++) {
                    if(HexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(Constants.CONFIG_GET_Z)){

//                        String newHex = getNewHexByPosition(HexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) (itemProperty.getZ()*1000.0f));
//
//                        HexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        if(template.contains("put") && template.contains("cover") && itemProperty.getType().equals("PCR_COVER")  ){
                            HexDebugEntityList.get(i).setDescription
                                    (HexDebugEntityList.get(i).getDescription() + ":" + itemProperty.getZ1());
                            String newHex = getNewHexByPosition(HexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) (itemProperty.getZ1()*1000.0f));

                            HexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);
                        }else{
                            HexDebugEntityList.get(i).setDescription
                                    (HexDebugEntityList.get(i).getDescription() + ":" + itemProperty.getZ());
                            String newHex = getNewHexByPosition(HexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) (itemProperty.getZ()*1000.0f));

                            HexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);
                        }
                    }
                }
            }
        }
    }

    private int countReset =0;
    public boolean robotGet(ItemProperty itemProperty){
        Log.i(TAG, "Robot_get running 0.5" + itemProperty.getType() + itemProperty.getPosition());
        countReset++;
        if(itemProperty.getType().equals("RNA")){
            return robot_Running(Constants.ROBOT_GET_RNA_FILE_PATH, itemProperty);
        }else if(itemProperty.getType().equals("PCR_COVER")){
            return robot_Running(Constants.ROBOT_GET_PCR_COVER_FILE_PATH, itemProperty);
        }else{
            if(countReset >= 10 && itemProperty.getType().equals("PAN")){
                countReset = 0;
                return robot_Running(Constants.ROBOT_GET_RESET_FILE_PATH, itemProperty);
            }
            return robot_Running(Constants.ROBOT_GET_FILE_PATH, itemProperty);
        }
    }

    public boolean robotPut(ItemProperty itemProperty){
        Log.i(TAG, "Robot_get put running 0.5" + itemProperty.getType() + itemProperty.getPosition());
        countReset++;
        if(itemProperty.getType().equals("RNA")){
            return robot_Running(Constants.ROBOT_PUT_RNA_FILE_PATH, itemProperty);
        }else if(itemProperty.getType().equals("PCR_COVER")){
            return robot_Running(Constants.ROBOT_PUT_PCR_COVER_FILE_PATH, itemProperty);
        }else{
            return robot_Running(Constants.ROBOT_PUT_FILE_PATH, itemProperty);
        }
    }

    public boolean robotArmConfig(){
        if(SerialPortManager_Robot.getInstance().isIs_ROBOT_connect()){
            ///TODO 处理坐标换算
            File robotConfigFile = new File(ROBOT_ARMCONFIG_FILE_PATH);
            List<HexDebugEntity> HexDebugEntityList =  ListDataSaveUtil.getHexDebugEntityList(robotConfigFile);

            if(HexDebugEntityList == null){
                Log.e(TAG, "robotArmConfig template error:" + ROBOT_ARMCONFIG_FILE_PATH);
                return false;
            }

            boolean result = startAysnTask(HexDebugEntityList,robotConfigFile.getName());
            if(result){
                Log.i(TAG, "robotArmConfig");
                return true;
            }else{
                Log.i(TAG, "robotArmConfig falied");
                return false;
            }

        }else{
            //ToastUtils.showToast(this, "RNA串口断连");
            Log.i(TAG, "Robot串口断连");
            return false;
        }
    }

    public boolean robotReset(){
        if(SerialPortManager_Robot.getInstance().isIs_ROBOT_connect()){
            ///TODO 处理坐标换算
            boolean ARMSensor =  SerialPortManager_Robot.getInstance().getRobotStatus().getSenseByKey("ARM");

            String fileName = "";

            Log.i(TAG, "robotReset ARMSensor:" + ARMSensor);

            if(ARMSensor){
                fileName = Constants.ROBOT_RESET_FILE_PATH;
            }else{
                fileName = Constants.ROBOT_ARMCONFIG_FILE_PATH;
            }

            File robotResetFile = new File(fileName);

            List<HexDebugEntity> HexDebugEntityList =  ListDataSaveUtil.getHexDebugEntityList(robotResetFile);

            if(HexDebugEntityList == null || HexDebugEntityList.size() == 0){
                Log.e(TAG, "robotReset template error:" + fileName);
                return false;
            }

            boolean result = startAysnTask(HexDebugEntityList,robotResetFile.getName());
            if(result){
                Log.i(TAG, "robotReset succeed:" + fileName);
                return true;
            }else{
                Log.i(TAG, "robotReset falied" + fileName);
                return false;
            }

        }else{
            //ToastUtils.showToast(this, "RNA串口断连");
            Log.i(TAG, "Robot串口断连");
            return false;
        }
    }

    public boolean finishCMD(){
        if(SerialPortManager_Robot.getInstance().isIs_ROBOT_connect()){
            ///TODO 处理坐标换算
            File robotFinishFile = new File(Constants.ROBOT_FINISH_FILE_PATH);
            List<HexDebugEntity> HexDebugEntityList =  ListDataSaveUtil.getHexDebugEntityList(robotFinishFile);

            if(HexDebugEntityList == null){
                Log.e(TAG, "robotFinish template error:" + Constants.ROBOT_RESET_FILE_PATH);
                return false;
            }

            boolean result = startAysnTask(HexDebugEntityList,robotFinishFile.getName());
            if(result){
                Log.i(TAG, "robotFinish");
                return true;
            }else{
                Log.i(TAG, "robotFinish falied");
                return false;
            }
        }else{
            Log.i(TAG, "Robot串口断连");
            return false;
        }
    }

    public boolean setLed(int pan, int color){
        if (!SerialPortManager_Robot.getInstance().isIs_ROBOT_connect()) {
            Log.w(TAG, "operateDoor: Robot serial port disconnected");
            return false;
        }
        File setCoolerFile = new File(Constants.ROBOT_LED_FILE_PATH);
        if (!setCoolerFile.exists() || !setCoolerFile.canRead()) {
            Log.e(TAG, "setCoolerFile: template file not exists or unreadable: " + Constants.ROBOT_LED_FILE_PATH);
            return false;
        }
        try{
            ///TODO 处理坐标换算
            List<HexDebugEntity> HexDebugEntityList =  ListDataSaveUtil.getHexDebugEntityList(setCoolerFile);
            if (HexDebugEntityList == null || HexDebugEntityList.isEmpty()) {
                Log.e(TAG, "operateDoor: empty or null entity list from file: " + Constants.ROBOT_OPERATE_DOOR_FILE_PATH);
                return false;
            }
            HexDebugEntity ledHexDebugEntity = null;
            for(int i = 0;i < HexDebugEntityList.size();i++){
                HexDebugEntity hexDebugEntity= HexDebugEntityList.get(i);
                if(hexDebugEntity.getName().toLowerCase().equals("led")){
                    List<HexSingleEntity> HexActionList = hexDebugEntity.getHexActionList();
                    HexSingleEntity hexSingleEntity = new HexSingleEntity("", 0, "", true);
//                    DecimalFormat df = new DecimalFormat("0");
                    if(HexActionList.size()>0){
                        hexSingleEntity = HexActionList.get(0);
                        hexSingleEntity.setHex("" + pan  +  color);
                    }else{
                        hexSingleEntity.setHex("" + pan  +  color);
                        HexActionList.add(hexSingleEntity);
                    }
                    ledHexDebugEntity = hexDebugEntity;
                }
            }

            if(ledHexDebugEntity == null){
                Log.e(TAG, "setCooler template error:" + Constants.ROBOT_RESET_FILE_PATH);
                return false;
            }

            //boolean result = startAysnTask(HexDebugEntityList,setCoolerFile.getName());
            MoveActionCmd moveActionCmd = new MoveActionCmd();
            moveActionCmd.setID(1);
            moveActionCmd.setCmd(ledHexDebugEntity.getName());
            //moveActionCmd.setNO(/*current_template + */"" + currentHexDebugEntity.getNO() + "" + "s"  + (currentHexDebugEntity.getLoop()-i) + "");
            //currentHexDebugEntity.setName(current_template + "" + currentHexDebugEntity.getName() + "" + "s" + (currentHexDebugEntity.getLoop()-i)  + "");

            String stringDate = DateUtil.getStringDate();

            moveActionCmd.setNO(ledHexDebugEntity.getNO() + "|" + stringDate);

//                    Log.i("RNA", "下发指令" +current_template + "" + currentHexDebugEntity.getNO() + "" + "s" + (currentHexDebugEntity.getLoop()-i) + "");
            List<String> hexList = new ArrayList<String>();
            for(int j = 0 ; j < ledHexDebugEntity.getHexActionList().size();j++){

                hexList.add(ledHexDebugEntity.getHexActionList().get(j).getHex());

            }
            moveActionCmd.setHex(hexList);
            ConnectManager.getInstance().sendSerialCmdMsg_NoAck(moveActionCmd);
            return true;
        }catch (Exception e){
            Log.i(TAG, "Robot串口断连");
            return false;
        }

    }

    public boolean setCooler(int pan, int degree){
        if(SerialPortManager_Robot.getInstance().isIs_ROBOT_connect()){
            ///TODO 处理坐标换算
            File setCoolerFile = new File(Constants.ROBOT_SET_COOLER_FILE_PATH);
            List<HexDebugEntity> HexDebugEntityList =  ListDataSaveUtil.getHexDebugEntityList(setCoolerFile);
            HexDebugEntity coolHexDebugEntity = null;
            for(int i = 0;i < HexDebugEntityList.size();i++){
                HexDebugEntity hexDebugEntity= HexDebugEntityList.get(i);
                if(hexDebugEntity.getName().toLowerCase().equals("cold")){
                    List<HexSingleEntity> HexActionList = hexDebugEntity.getHexActionList();
                    HexSingleEntity hexSingleEntity = new HexSingleEntity("", 0, "", true);
                    DecimalFormat df = new DecimalFormat("000");
                    if(HexActionList.size()>0){
                        hexSingleEntity = HexActionList.get(0);
                        hexSingleEntity.setHex("" + pan + df.format(degree * 10));
                    }else{
                        hexSingleEntity.setHex("" + pan + df.format(degree * 10));
                        HexActionList.add(hexSingleEntity);
                    }
                    coolHexDebugEntity = hexDebugEntity;
                }
            }

            if(coolHexDebugEntity == null){
                Log.e(TAG, "setCooler template error:" + Constants.ROBOT_RESET_FILE_PATH);
                return false;
            }

            //boolean result = startAysnTask(HexDebugEntityList,setCoolerFile.getName());
            MoveActionCmd moveActionCmd = new MoveActionCmd();
            moveActionCmd.setID(1);
            moveActionCmd.setCmd(coolHexDebugEntity.getName());
            //moveActionCmd.setNO(/*current_template + */"" + currentHexDebugEntity.getNO() + "" + "s"  + (currentHexDebugEntity.getLoop()-i) + "");
            //currentHexDebugEntity.setName(current_template + "" + currentHexDebugEntity.getName() + "" + "s" + (currentHexDebugEntity.getLoop()-i)  + "");

            String stringDate = DateUtil.getStringDate();

            moveActionCmd.setNO(coolHexDebugEntity.getNO() + "|" + stringDate);

//                    Log.i("RNA", "下发指令" +current_template + "" + currentHexDebugEntity.getNO() + "" + "s" + (currentHexDebugEntity.getLoop()-i) + "");
            List<String> hexList = new ArrayList<String>();
            for(int j = 0 ; j < coolHexDebugEntity.getHexActionList().size();j++){

                hexList.add(coolHexDebugEntity.getHexActionList().get(j).getHex());

            }
            moveActionCmd.setHex(hexList);
            ConnectManager.getInstance().sendSerialCmdMsg_NoAck(moveActionCmd);
            return true;
        }else{
            Log.i(TAG, "Robot串口断连");
            return false;
        }
    }


    public boolean operateDoor(int operation){
        if (!SerialPortManager_Robot.getInstance().isIs_ROBOT_connect()) {
            Log.w(TAG, "operateDoor: Robot serial port disconnected");
            return false;
        }
        File operateDoorFile = new File(Constants.ROBOT_OPERATE_DOOR_FILE_PATH);
        if (!operateDoorFile.exists() || !operateDoorFile.canRead()) {
            Log.e(TAG, "operateDoor: template file not exists or unreadable: " + Constants.ROBOT_OPERATE_DOOR_FILE_PATH);
            return false;
        }
        try {
            // 4. 读取配置，增加空保护
            List<HexDebugEntity> hexDebugEntityList = ListDataSaveUtil.getHexDebugEntityList(operateDoorFile);
            if (hexDebugEntityList == null || hexDebugEntityList.isEmpty()) {
                Log.e(TAG, "operateDoor: empty or null entity list from file: " + Constants.ROBOT_OPERATE_DOOR_FILE_PATH);
                return false;
            }
            HexDebugEntity doorHexDebugEntity = null;
            for(int i = 0;i < hexDebugEntityList.size();i++){
                HexDebugEntity hexDebugEntity= hexDebugEntityList.get(i);
                if(hexDebugEntity.getName().toLowerCase().equals("door")){
                    List<HexSingleEntity> HexActionList = hexDebugEntity.getHexActionList();
                    HexSingleEntity hexSingleEntity = new HexSingleEntity("", 0, "", true);
                    if(HexActionList.size()>0){
                        hexSingleEntity = HexActionList.get(0);
                        hexSingleEntity.setHex("" + operation);
                    }else{
                        hexSingleEntity.setHex("" + operation);
                        HexActionList.add(hexSingleEntity);
                    }
                    doorHexDebugEntity = hexDebugEntity;
                }
            }

            if(doorHexDebugEntity == null){
                Log.e(TAG, "operateDoor template error:" + Constants.ROBOT_OPERATE_DOOR_FILE_PATH);
                return false;
            }

            //boolean result = startAysnTask(HexDebugEntityList,operateDoorFile.getName());
            MoveActionCmd moveActionCmd = new MoveActionCmd();
            moveActionCmd.setID(1);
            moveActionCmd.setCmd(doorHexDebugEntity.getName());
            //moveActionCmd.setNO(/*current_template + */"" + currentHexDebugEntity.getNO() + "" + "s"  + (currentHexDebugEntity.getLoop()-i) + "");
            //currentHexDebugEntity.setName(current_template + "" + currentHexDebugEntity.getName() + "" + "s" + (currentHexDebugEntity.getLoop()-i)  + "");

            String stringDate = DateUtil.getStringDate();


            moveActionCmd.setNO(doorHexDebugEntity.getNO() + "|" + stringDate);

//                    Log.i("RNA", "下发指令" +current_template + "" + currentHexDebugEntity.getNO() + "" + "s" + (currentHexDebugEntity.getLoop()-i) + "");
            List<String> hexList = new ArrayList<String>();
            for(int j = 0 ; j < doorHexDebugEntity.getHexActionList().size();j++){

                hexList.add(doorHexDebugEntity.getHexActionList().get(j).getHex());

            }
            moveActionCmd.setHex(hexList);
            ConnectManager.getInstance().sendSerialCmdMsg_NoAck(moveActionCmd);
            return true;

        }catch (Exception e){
            Log.i(TAG, "Robot串口断连");
            return false;
        }

    }


    public boolean robot_Running(String template, ItemProperty itemProperty){
        if(SerialPortManager_Robot.getInstance().isIs_ROBOT_connect()){
            ///TODO 处理坐标换算

            File robotgetfile = new File(template);
            List<HexDebugEntity> HexDebugEntityList =  ListDataSaveUtil.getHexDebugEntityList(robotgetfile);
            List<HexDebugEntity> HexDebugEntityList_new;
            if(HexDebugEntityList == null){
                Log.e(TAG, "robot_Running template error:" + template);
                return false;
            }

            setParm(template, HexDebugEntityList,itemProperty);

            HexDebugEntity finish = new HexDebugEntity();
            finish.setDescription("finish");
            finish.setName("finish");
            finish.setNO("");
            finish.setHexActionList(new ArrayList<HexSingleEntity>());
            finish.setSelected(true);
            finish.setLoop(1);

//            HexDebugEntityList.add(finish);

            /// //增加盖子的磁吸操作
            if(template.contains("get") && template.contains("cover") && itemProperty.getType().equals("PCR_COVER") ){
                for (int i = 0; i < HexDebugEntityList.size(); i++) {
                    HexDebugEntity hexDebugEntity= HexDebugEntityList.get(i);
                    if (HexDebugEntityList.get(i).getName().equals("cover")) {
                        List<HexSingleEntity> HexActionList = hexDebugEntity.getHexActionList();
                        HexSingleEntity hexSingleEntity = new HexSingleEntity("", 0, "", true);
                        if(HexActionList.size()>0){
                            hexSingleEntity = HexActionList.get(0);
                            hexSingleEntity.setHex("" + itemProperty.getPosition());
                        }else{
                            hexSingleEntity.setHex("" + itemProperty.getPosition());
                            HexActionList.add(hexSingleEntity);
                        }
                        break;
                    }
                }
            }

            boolean ARMSensor =  SerialPortManager_Robot.getInstance().getRobotStatus().getSenseByKey("ARM");

            Log.i(TAG, "robot_Running ARMSensor:" + ARMSensor);
            HexDebugEntityList_new = new ArrayList<>();
            if(template.contains("get")) {
                if (ARMSensor) {
                    int index = 0;
                    for (int i = 0; i < HexDebugEntityList.size(); i++) {
                        if (HexDebugEntityList.get(i).getDescription().equals("hand_flag")) {
                            Log.i(TAG, "robot_Running get ARMSensor:" + ARMSensor + ":" + template);
                            Log.i(TAG, "robot_Running get index:" + i);
                            index = i;
                            break;
                        }
                    }
                    for (int i = index + 1; i < HexDebugEntityList.size(); i++) {
                        HexDebugEntityList_new.add(HexDebugEntityList.get(i));
                    }
                } else {
                    HexDebugEntityList_new = HexDebugEntityList;
                }
            }else if(template.contains("put")) {
                if (!ARMSensor) {
                    HexDebugEntityList_new = new ArrayList<>();
                    int index = 0;
                    for (int i = 0; i < HexDebugEntityList.size(); i++) {
                        if (HexDebugEntityList.get(i).getDescription().equals("hand_flag")) {
                            Log.i(TAG, "robot_Running put ARMSensor:" + ARMSensor + ":" + template);
                            Log.i(TAG, "robot_Running put index:" + i);
                            index = i;
                            break;
                        }
                    }
                    for (int i = index + 1; i < HexDebugEntityList.size(); i++) {
                        HexDebugEntityList_new.add(HexDebugEntityList.get(i));
                    }
                } else {
                    HexDebugEntityList_new = HexDebugEntityList;
                }
            }else{
                HexDebugEntityList_new = HexDebugEntityList;
            }

            boolean result = startAysnTask(HexDebugEntityList_new,robotgetfile.getName());
            if(result){
                Log.i(TAG, "Robot_get running 1");
                return true;
            }else{
                Log.i(TAG, "Robot_get run error");
                return false;
            }

        }else{
            //ToastUtils.showToast(this, "RNA串口断连");
            Log.i(TAG, "Robot串口断连");
            return false;
        }
    }

    /**
     * 机械臂停止
     */
    public void robotStop(){
        if(RNAAndPCRAsyncTask.getInstance().ismIsrunning()){
            RNAAndPCRAsyncTask.getInstance().setIsinteruppt(true);
            RNAAndPCRAsyncTask.getInstance().setmIsrunning(false);
        }
    }

    /**
     * 机械臂安全停止：当前步骤执行完（收到 ack）后停止，不再下发后续步骤。
     * 与 robotStop() 不同，不会立即打断等待，避免上一步的迟到 ack 被下一个任务的第一步误收。
     * 任务真正退出后 RNAAndPCRAsyncTask.isIdle() 才会变为 true。
     */
    public void robotStopAfterCurrentStep(){
        RNAAndPCRAsyncTask.requestStopAfterCurrentStep();
    }

    /** 当前线程最近一次 startAysnTask 成功启动的运行序号/模板，供 RobotWorker 绑定 finish 事件 */
    private static final ThreadLocal<Long> lastStartedRunId = new ThreadLocal<>();
    private static final ThreadLocal<String> lastStartedTemplate = new ThreadLocal<>();

    public static void clearLastStartedOnThisThread(){
        lastStartedRunId.remove();
        lastStartedTemplate.remove();
    }

    /** @return 运行序号，未在本线程启动过返回 0 */
    public static long getLastStartedRunIdOnThisThread(){
        Long v = lastStartedRunId.get();
        return v == null ? 0 : v;
    }

    public static String getLastStartedTemplateOnThisThread(){
        return lastStartedTemplate.get();
    }

    /** 兜底：等待上一个任务释放 mIsrunning 的最长时间(ms)与轮询间隔(ms) */
    private static final long START_TASK_WAIT_TIMEOUT_MS = 5000;
    private static final long START_TASK_WAIT_INTERVAL_MS = 100;

    /** 只保护"判空闲 + 启动"这一小段，等待过程不持锁，避免主线程调用方被长时间阻塞 */
    private final Object startTaskLock = new Object();

    public boolean startAysnTask(List<HexDebugEntity> HexDebugEntityList,String Template){
        try {
            if (HexDebugEntityList == null || HexDebugEntityList.isEmpty()) {
                Log.e(TAG, "startAysnTask template error:" + Template);
                return false;
            }
            // 兜底：上一个任务可能因 worker 被取消等异常还未真正退出（或其 finish 事件还未发出），
            // 这里有界等待其真正空闲，避免两个模板同时运行；超时才判失败。
            long waitStart = System.currentTimeMillis();
            boolean waited = false;
            while (true) {
                synchronized (startTaskLock) {
                    if (RNAAndPCRAsyncTask.isIdle()) {
                        if (waited) {
                            Log.i(TAG, "RNAAndPCRAsyncTask released after wait "
                                    + (System.currentTimeMillis() - waitStart) + "ms: continue start " + Template);
                        }
                        RNAAndPCRAsyncTask task = RNAAndPCRAsyncTask.getInstance();
                        if (task.getStatus() != android.os.AsyncTask.Status.PENDING) {
                            // 已结束但实例未清理（如被 cancel），AsyncTask 不能重复 execute，换新实例
                            RNAAndPCRAsyncTask.clearInstance();
                            task = RNAAndPCRAsyncTask.getInstance();
                        }
                        RNAAndPCRAsyncTask.current_template = Template;
                        RNAAndPCRAsyncTask.current_template_phase = 0;
                        RNAAndPCRAsyncTask.current_template_phase_step = 0;
                        task.setmContext(mContext);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            //根据不同的api采用并行模式进行开启
                            //////启动流程的API
                            task.executeOnExecutor(THREAD_POOL_EXECUTOR, HexDebugEntityList);
                        } else {
                            task.execute(HexDebugEntityList);
                        }
                        // onPreExecute 已在本线程同步执行，runId/模板已确定
                        lastStartedRunId.set(task.getRunId());
                        lastStartedTemplate.set(task.getStartedTemplate());
                        Log.i(TAG, "startAysnTask start " + Template + " runId:" + task.getRunId());
                        return true;
                    }
                }
                if (System.currentTimeMillis() - waitStart >= START_TASK_WAIT_TIMEOUT_MS) {
                    Log.e(TAG, "RNAAndPCRAsyncTask still running after wait "
                            + (System.currentTimeMillis() - waitStart) + "ms: when start " + Template);
                    return false;
                }
                waited = true;
                try {
                    Thread.sleep(START_TASK_WAIT_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    Log.e(TAG, "startAysnTask wait interrupted: when start " + Template);
                    return false;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "startAysnTask template error:" + e.getMessage());
            return false;
        }
    }


    public class RobotStatus{ /// //机械臂的状态结构

        private String status;

        private String NO;

        private int degree;

        private String alarm;

        private int Sense;

        private String motorID;

        private boolean isROBOTconnect = false;

        private boolean isROBOTTaskRunning = false;

        private LinkedHashMap<String,Boolean> SenseMapList= new LinkedHashMap<>();

        private final Object senseMapLock = new Object();

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getNO() {
            return NO;
        }

        public void setNO(String NO) {
            this.NO = NO;
        }

        public int getDegree() {
            return degree;
        }

        public void setDegree(int degree) {
            this.degree = degree;
        }

        public String getAlarm() {
            return alarm;
        }

        public void setAlarm(String alarm) {
            this.alarm = alarm;
        }

        public String getMotorID() {
            return motorID;
        }

        public void setMotorID(String motorID) {
            this.motorID = motorID;
        }

        public boolean isROBOTconnect() {
            return isROBOTconnect;
        }

        public void setROBOTconnect(boolean ROBOTconnect) {
            isROBOTconnect = ROBOTconnect;
        }

        public boolean isROBOTTaskRunning() {
            return isROBOTTaskRunning;
        }

        public void setROBOTTaskRunning(boolean ROBOTTaskRunning) {
            isROBOTTaskRunning = ROBOTTaskRunning;
        }

        public int getSense() {
            return Sense;
        }

        public void setSense(int sense) {
            Sense = sense;
        }
        //Sense 里面是个unsigned long数据类型 整数 转换成二进制数位最高给17位
        // 按位顺序由高到低是 夹爪/PCR6/PCR5/PCR4/PCR3/PCR2/PCR1/核提2/核提1/托盘8/托盘7/托盘6/托盘5/托盘4/托盘3/托盘2/托盘1

        public LinkedHashMap<String, Boolean> createSenseMapList(){
            synchronized (senseMapLock) {
                SenseMapList.clear();
                SenseMapList.put("PAN1", (Sense & (1<<0)) != 0);
                SenseMapList.put("PAN2", (Sense & (1<<1)) != 0);
                SenseMapList.put("PAN3", (Sense & (1<<2)) != 0);
                SenseMapList.put("PAN4", (Sense & (1<<3)) != 0);
                SenseMapList.put("PAN5", (Sense & (1<<4)) != 0);
                SenseMapList.put("PAN6", (Sense & (1<<5)) != 0);
                SenseMapList.put("PAN7", (Sense & (1<<6)) != 0);
                SenseMapList.put("PAN8", (Sense & (1<<7)) != 0);
                SenseMapList.put("RNA1", (Sense & (1<<8)) != 0);
                SenseMapList.put("RNA2", (Sense & (1<<9)) != 0);
                SenseMapList.put("PCR1", (Sense & (1<<10)) != 0);
                SenseMapList.put("PCR2", (Sense & (1<<11)) != 0);
                SenseMapList.put("PCR3", (Sense & (1<<12)) != 0);
                SenseMapList.put("PCR4", (Sense & (1<<13)) != 0);
                SenseMapList.put("PCR5", (Sense & (1<<14)) != 0);
                SenseMapList.put("PCR6", (Sense & (1<<15)) != 0);
                SenseMapList.put("ARM", (Sense & (1<<16)) != 0);
                SenseMapList.put("PCR_COVER1", (Sense & (1<<17)) != 0);
                SenseMapList.put("PCR_COVER2", (Sense & (1<<18)) != 0);
                SenseMapList.put("PCR_COVER3", (Sense & (1<<19)) != 0);
                SenseMapList.put("PCR_COVER4", (Sense & (1<<20)) != 0);
                SenseMapList.put("PCR_COVER5", (Sense & (1<<21)) != 0);
                SenseMapList.put("PCR_COVER6", (Sense & (1<<22)) != 0);
                SenseMapList.put("PCR_COVER0", (Sense & (1<<7)) != 0);
                SenseMapList.put("DOOR", (Sense & (1<<23)) != 0);
                return SenseMapList;
            }
        }

        public LinkedHashMap<String, Boolean> getSenseMapList() {
            synchronized (senseMapLock) {
                SenseMapList.clear();
                SenseMapList.put("PAN1", (Sense & (1<<0)) != 0);
                SenseMapList.put("PAN2", (Sense & (1<<1)) != 0);
                SenseMapList.put("PAN3", (Sense & (1<<2)) != 0);
                SenseMapList.put("PAN4", (Sense & (1<<3)) != 0);
                SenseMapList.put("PAN5", (Sense & (1<<4)) != 0);
                SenseMapList.put("PAN6", (Sense & (1<<5)) != 0);
                SenseMapList.put("PAN7", (Sense & (1<<6)) != 0);
                SenseMapList.put("PAN8", (Sense & (1<<7)) != 0);
                SenseMapList.put("RNA1", (Sense & (1<<8)) != 0);
                SenseMapList.put("RNA2", (Sense & (1<<9)) != 0);
                SenseMapList.put("PCR1", (Sense & (1<<10)) != 0);
                SenseMapList.put("PCR2", (Sense & (1<<11)) != 0);
                SenseMapList.put("PCR3", (Sense & (1<<12)) != 0);
                SenseMapList.put("PCR4", (Sense & (1<<13)) != 0);
                SenseMapList.put("PCR5", (Sense & (1<<14)) != 0);
                SenseMapList.put("PCR6", (Sense & (1<<15)) != 0);
                SenseMapList.put("ARM", (Sense & (1<<16)) != 0);
                SenseMapList.put("PCR_COVER1", (Sense & (1<<17)) != 0);
                SenseMapList.put("PCR_COVER2", (Sense & (1<<18)) != 0);
                SenseMapList.put("PCR_COVER3", (Sense & (1<<19)) != 0);
                SenseMapList.put("PCR_COVER4", (Sense & (1<<20)) != 0);
                SenseMapList.put("PCR_COVER5", (Sense & (1<<21)) != 0);
                SenseMapList.put("PCR_COVER6", (Sense & (1<<22)) != 0);
                SenseMapList.put("PCR_COVER0", (Sense & (1<<7)) != 0);
                SenseMapList.put("DOOR", (Sense & (1<<23)) != 0);
                return SenseMapList;
            }
        }

        public Boolean getSenseByKey(String key){
            synchronized (senseMapLock) {
                if(SenseMapList.containsKey(key)){
                    return SenseMapList.get(key);
                }else{
                    return false;
                }
            }
        }

    }

}
