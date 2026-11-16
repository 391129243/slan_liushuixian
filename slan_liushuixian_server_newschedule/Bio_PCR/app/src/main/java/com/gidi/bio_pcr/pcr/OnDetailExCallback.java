package com.gidi.bio_pcr.pcr;


public interface OnDetailExCallback {
    //保存JSON结果的回调
    void onSaveJsonResult(boolean result,String message);
    //二次分析结果回调
    void onSecondParseFinish(boolean result);
    //更新实验结果回调
    void onUpdateFinish(boolean result);

}
