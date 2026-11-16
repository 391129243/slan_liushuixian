package com.gidi.bio_pcr.slan.fragment;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.gidi.bio_pcr.bean.ExQueryCondition;
import com.gidi.bio_pcr.db.ExperimentModel;
import com.gidi.bio_pcr.db.LiteORMCtrlMgr;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResultExSLANViewModel extends ViewModel {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * 查询结果（实验列表 + 总记录数）
     */
    public static class ExQueryResult {
        public final List<ExperimentModel> results;
        public final long totalCount;

        public ExQueryResult(List<ExperimentModel> results, long totalCount) {
            this.results = results;
            this.totalCount = totalCount;
        }
    }

    private final MutableLiveData<ExQueryResult> queryResult = new MutableLiveData<>();
    private final MutableLiveData<String> errorMsg = new MutableLiveData<>();

    public LiveData<ExQueryResult> getQueryResult() {
        return queryResult;
    }

    public LiveData<String> getErrorMsg() {
        return errorMsg;
    }

    /**
     * 在后台线程查询实验结果（分页 + 总数），通过 LiveData 回传结果
     */
    public void queryExperiments(final ExQueryCondition queryCondition) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    long totalCount = LiteORMCtrlMgr.getInstance().getExperimentNumber(queryCondition);
                    List<ExperimentModel> results = LiteORMCtrlMgr.getInstance().searchExprement(queryCondition);
                    queryResult.postValue(new ExQueryResult(results, totalCount));
                } catch (Exception e) {
                    errorMsg.postValue(e.getMessage());
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
