package com.gidi.bio_pcr.slan.fragment;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.gidi.bio_pcr.bean.QueryCondition;
import com.gidi.bio_pcr.db.LiteORMCtrlMgr;
import com.gidi.bio_pcr.db.SLANWellResult;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SLANExDetailViewModel extends ViewModel {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<SLANWellResult>> wellResults = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMsg = new MutableLiveData<>();

    public LiveData<List<SLANWellResult>> getWellResults() {
        return wellResults;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMsg() {
        return errorMsg;
    }

    /**
     * 在后台线程查询一次检测所有孔位记录，通过 LiveData 回传结果
     */
    public void queryExDetail(final QueryCondition queryCondition) {
        loading.setValue(true);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    List<SLANWellResult> result = LiteORMCtrlMgr.getInstance().searchSLANWellResult(queryCondition);
                    wellResults.postValue(result);
                } catch (Exception e) {
                    errorMsg.postValue(e.getMessage());
                } finally {
                    loading.postValue(false);
                }
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
}
