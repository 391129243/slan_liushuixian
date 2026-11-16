package com.winters.boweilibrary.BoWei.event;

import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRStepEntity;

public class BoWeiPCRProcedureInfoEvent
{
    private String result;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    private long remainTime;//程序剩余时间

    private long runingTime;//运行时间
    private String holdTime;//步骤保持时间。步骤倒计时


    public long getRemainTime() {
        return remainTime;
    }

    public void setRemainTime(long remainTime) {
        this.remainTime = remainTime;
    }

    private BoWeiPCRStepEntity currentBoWeiPCRStepEntity;

    public BoWeiPCRStepEntity getCurrentBoWeiPCRStepEntity() {
        return currentBoWeiPCRStepEntity;
    }

    public void setCurrentBoWeiPCRStepEntity(BoWeiPCRStepEntity currentBoWeiPCRStepEntity) {
        this.currentBoWeiPCRStepEntity = currentBoWeiPCRStepEntity;
    }



    public BoWeiPCRProcedureInfoEvent(String result) {
        this.result = result;
    }

    public long getRuningTime() {
        return runingTime;
    }

    public String getHoldTime() {
        return holdTime;
    }

    public void setHoldTime(String holdTime) {
        this.holdTime = holdTime;
    }

    public void setRuningTime(long runingTime) {
        this.runingTime = runingTime;
    }

    public BoWeiPCRProcedureInfoEvent(long runningTime, long remainTime,String holdTime, BoWeiPCRStepEntity currentBoWeiPCRStepEntity) {
        this.runingTime = runningTime;//已经运行时间
        this.remainTime = remainTime;
        this.holdTime = holdTime;//步骤倒计时
        this.currentBoWeiPCRStepEntity = currentBoWeiPCRStepEntity;
    }
}
