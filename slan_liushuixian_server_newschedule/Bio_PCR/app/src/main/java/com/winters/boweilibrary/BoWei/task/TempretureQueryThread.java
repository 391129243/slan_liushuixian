package com.winters.boweilibrary.BoWei.task;

import com.winters.boweilibrary.BoWei.serialcom.BoWeiConnectManager;
import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRStepEntity;

import java.util.ArrayList;

public class TempretureQueryThread extends Thread{

    private static TempretureQueryThread mInstance;

    private boolean queryable = true;

    private boolean interrupt = false;

    public static TempretureQueryThread getInstance(){
        if (null == mInstance) {
            synchronized (TempretureQueryThread.class){
                if (null == mInstance){
                    mInstance = new TempretureQueryThread();
                }
            }
        }
        return  mInstance;
    }


    public void setUPThread() {
        if(mInstance.isAlive()){////???
            queryable = false;
        }else{
            queryable = false;
            interrupt = false;
            mInstance.start();
        }
    }

    public void startQuery() {
        if(mInstance.isAlive()){////???
            queryable = true;
        }else{
            queryable = true;
            interrupt = false;
            mInstance.start();
        }
    }

    public void stopQuery() {
        queryable = false;
    }

    public void killQuery() {
        queryable = false;
        interrupt = true;
        //mInstance.interrupt();
        mInstance = null;
    }

    @Override
    public void run() {
        // 在这里写入你的线程执行代码
        // 例如：
        while(!interrupt){
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                //throw new RuntimeException(e);
            }
            if(queryable){
                BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("get_current_temperature", "获取PCR当前温度", "5E 43 00 06 00 A7", new ArrayList<Double>(), 1, 1, 0, 0, 0, 0, 0);
                BoWeiConnectManager.getInstance().sendSerialCmdMsg_NoAck(pcrDebugEntity);
            }
        }
    }
}
