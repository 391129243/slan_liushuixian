package com.gidi.bio_pcr.pcr;


public interface OnExperimentCallback {

    void onSaveExToDbResult(boolean result);

    void onParseFinish();

    void onSaveJsonResult(boolean result,String message);

    void onSecondParseFinish(boolean result);

    void onUpdateFinish(boolean result);

}
