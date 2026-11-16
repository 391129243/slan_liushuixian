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

public class ResultWellSLANViewModel extends ViewModel {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * 查询结果（数据列表 + 总记录数）
     */
    public static class WellQueryResult {
        public final List<SLANWellResult> results;
        public final long totalCount;

        public WellQueryResult(List<SLANWellResult> results, long totalCount) {
            this.results = results;
            this.totalCount = totalCount;
        }
    }

    private final MutableLiveData<WellQueryResult> queryResult = new MutableLiveData<>();
    private final MutableLiveData<String> errorMsg = new MutableLiveData<>();

    public LiveData<WellQueryResult> getQueryResult() {
        return queryResult;
    }

    public LiveData<String> getErrorMsg() {
        return errorMsg;
    }

    /**
     * 在后台线程查询孔位结果（分页 + 总数），通过 LiveData 回传结果
     */
    public void queryWellResults(final QueryCondition queryCondition) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    long totalCount = LiteORMCtrlMgr.getInstance().getSLANWellResultCount(queryCondition);
                    List<SLANWellResult> results = LiteORMCtrlMgr.getInstance().searchSLANWellResultByLength(queryCondition);
                    queryResult.postValue(new WellQueryResult(results, totalCount));
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
