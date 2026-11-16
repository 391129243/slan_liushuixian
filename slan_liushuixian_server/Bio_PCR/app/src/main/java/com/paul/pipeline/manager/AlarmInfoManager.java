package com.paul.pipeline.manager;

import com.paul.pipeline.listener.AlarmListener;

public class AlarmInfoManager {
    private static final String TAG = "AlarmInfoManager";
    private static AlarmInfoManager mInstance;
    public static AlarmInfoManager getInstance(){
        if (null == mInstance) {
            synchronized (AlarmInfoManager.class){
                if (null == mInstance){
                    mInstance = new AlarmInfoManager();
                }
            }
        }
        return  mInstance;
    }
    AlarmListener alarmListener;

    public void setAlarmListener(AlarmListener alarmListener) {
        this.alarmListener = alarmListener;
    }

    String alarmMsgTv="";
    boolean sound = false;

    public void setText(String value){
        alarmMsgTv = value;
        if(alarmMsgTv==null||alarmMsgTv.isBlank()||alarmMsgTv.isEmpty()){
            sound = false;
        }else{
            sound = true;
        }
        if(alarmListener!=null){
            alarmListener.alarm(alarmMsgTv,sound);
        }
    }
}
