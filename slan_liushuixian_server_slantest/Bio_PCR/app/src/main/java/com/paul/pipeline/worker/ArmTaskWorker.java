package com.paul.pipeline.worker;

import android.content.Context;
import android.util.Log;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.paul.pipeline.service.APLService;
import com.paul.pipeline.utils.ServiceUtils;

public class ArmTaskWorker  extends Worker {
    Context mcontext;
    public ArmTaskWorker(Context appContext, WorkerParameters workerParams) {
        super(appContext, workerParams);
        mcontext = appContext;
    }

    @Override
    public Result doWork() {
        // 在这里执行你的后台任务
        try{
            if(ServiceUtils.isServiceRunning(mcontext, APLService.class)){
                Log.i("ArmTaskWorker", "执行service检查:已启动");
                return Result.success();
            }else{
                Log.w("ArmTaskWorker", "aplservice检查:未启动");
                return Result.failure();
            }
        } catch (Exception e) {
            Log.e("ArmTaskWorker", e.getMessage());
            return Result.retry();
        }

    }
}
