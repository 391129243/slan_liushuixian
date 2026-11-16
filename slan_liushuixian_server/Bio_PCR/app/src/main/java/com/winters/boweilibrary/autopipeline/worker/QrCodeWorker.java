package com.winters.boweilibrary.autopipeline.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.gidi.bio_pcr.WintersUtils.entity.event.ProcedureFinishEvent;
import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.gidi.bio_pcr.util.NumberUtils;
import com.google.gson.reflect.TypeToken;
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener;
import com.winters.boweilibrary.autopipeline.constant.Constants;
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_QRCode;
import com.winters.boweilibrary.autopipeline.model.event.QRCodeMessageEvent;
import com.winters.boweilibrary.autopipeline.model.item.QRCodeItem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.winters.boweilibrary.autopipeline.constant.Constants.ACTION;
import static com.winters.boweilibrary.autopipeline.constant.Constants.KEY_INPUT_DATA;
import static com.winters.boweilibrary.autopipeline.constant.Constants.KEY_OUTPUT_DATA;
import static com.winters.boweilibrary.autopipeline.constant.Constants.KEY_RESULT_DATA;

public class QrCodeWorker extends Worker {

    private final static String TAG = "QrCodeWorker";

    private List<QRCodeItem> qRCodeItems1 = new ArrayList<>();

    private List<QRCodeItem> qRCodeItems2 = new ArrayList<>();

    //private List<QRCodeItem> qRCodeItems3 = new ArrayList<>();



    private int index =0;

    private int count =0;

    private Object lock = new Object();

    private ProcedureFinishEvent procedureFinishEvent;



    public QrCodeWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        // 方式二：获取 JSON 字符串并反序列化
        String configJson = getInputData().getString(KEY_INPUT_DATA);
        String action = getInputData().getString(ACTION);

        int panNum = getInputData().getInt(Constants.KEY_PANNUM_DATA,-1);

        List<QRCodeItem> qRCodeItems = GsonUtil.fromJson(configJson,new TypeToken<List<QRCodeItem>>(){});
        qRCodeItems.sort((a, b) -> Integer.compare(a.getX_coordinate(), b.getX_coordinate()));

        registerEventBus();

        int retrycount =0;

        while (true){
            if(index == 2 ){
                break;
            }
            boolean result = SerialPortManager_QRCode.getInstance().sendStart();

            if(!result){
//不管对不对，保证检测完成(保证测试成功)
//                Log.i(TAG, "SerialPortManager_QRCode send error");
//                if(count++ == 10){
//                    Data errorData = new Data.Builder()
//                            .putString(KEY_RESULT_DATA, "error")
//                            .build();
//                    unRegisterEventBus();
//                    return Result.failure(errorData);
//                }
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    Data errorData = new Data.Builder()
//                            .putString(KEY_RESULT_DATA, "error")
//                            .build();
//                    unRegisterEventBus();
//                    return Result.failure(errorData);
//                }
                Log.i(TAG, "SerialPortManager_QRCode scan  warn: " + configJson);
                Data succeedData = new Data.Builder()
                        .putString(KEY_RESULT_DATA, "succeed")
                        .putString(KEY_OUTPUT_DATA, GsonUtil.toJson(qRCodeItems1,false))
                        .build();
                return Result.success(succeedData);
            }else{
                if(retrycount++ == 2){
                    break;
                }
                synchronized(lock) {
                    try {
                        lock.wait(200);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        Boolean result =  compareQRCodeLists();

        //Todo  对比configJson的患者信息，确认是否为同一个盘
        result = true;

        //Todo  对比configJson的患者信息，确认是否为同一个盘
        unRegisterEventBus();

        if(result){
            Log.i(TAG, "finish succeed: " + configJson);
            Data succeedData = new Data.Builder()
                    .putString(KEY_RESULT_DATA, "succeed")
                    .putString(KEY_OUTPUT_DATA, GsonUtil.toJson(qRCodeItems1,false))
                    .build();
            Log.i(TAG, "Robot_get running -2 QRCODE finish");
            return Result.success(succeedData);
        }else{
            Log.i(TAG, "finish warn: " + configJson);
            Data errorData = new Data.Builder()
                    .putString(KEY_RESULT_DATA, "warn")
                    .putString(KEY_OUTPUT_DATA, GsonUtil.toJson(qRCodeItems1,false))
                    .build();
            return Result.success(errorData);
        }
    }

    /**
     * 比较三个二维码队列的元素数量和qrcode内容是否一致
     * @return true: 数量和内容都一致，false: 不一致
     */
    public boolean compareQRCodeLists() {
        // 先按 x_coordinate 升序排序
        qRCodeItems1.sort((a, b) -> Integer.compare(a.getX_coordinate(), b.getX_coordinate()));
        qRCodeItems2.sort((a, b) -> Integer.compare(a.getX_coordinate(), b.getX_coordinate()));
        //qRCodeItems3.sort((a, b) -> Integer.compare(a.getX_coordinate(), b.getX_coordinate()));

        if (qRCodeItems1.size() != qRCodeItems2.size() /*|| qRCodeItems1.size() != qRCodeItems3.size()*/) {
            Log.i(TAG, "二维码队列数量不一致");
            return false;
        }

        for (int i = 0; i < qRCodeItems1.size(); i++) {
            String code1 = qRCodeItems1.get(i).getQrcode();
            String code2 = qRCodeItems2.get(i).getQrcode();
            //String code3 = qRCodeItems3.get(i).getQrcode();
            if (!(code1 != null && code1.equals(code2) /* && code1.equals(code3)*/)) {
                Log.i(TAG, "第" + (i+1) + "个二维码内容不一致: " + code1 + ", " + code2 /* + ", " + code3*/);
                return false;
            }
        }
        Log.i(TAG, "二维码队列数量和内容都一致");
        return true;
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



    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEventMainThread(QRCodeMessageEvent qRCodeMessageEvent) {

        List<QRCodeItem> qRCodeItems = null;

        if(index == 0){
            qRCodeItems = qRCodeItems1;
            index++;
        }else if(index == 1){
            qRCodeItems = qRCodeItems2;
            index++;
        }
//        else if(index == 2){
//            qRCodeItems = qRCodeItems3;
//            index++;
//        }

        String text = qRCodeMessageEvent.getQRCodeMessage();

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

        synchronized (lock) {
            try {
                lock.notify();
            } catch (Exception e) {
                Log.i(TAG, "error:" + e.getMessage());
            }
        }
    }



}