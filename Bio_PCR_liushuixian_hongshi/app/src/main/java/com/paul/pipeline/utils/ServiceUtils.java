package com.paul.pipeline.utils;

import android.app.ActivityManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.List;

public class ServiceUtils {
    public static boolean isForegroundServiceRunning(Context context, Class<?> serviceClass) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
            if (jobInfo.getService().getClassName().equals(serviceClass.getName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isServiceDeclared(Context context, Class<?> serviceClass) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context, serviceClass));
        List<ResolveInfo> list = context.getPackageManager().queryIntentServices(intent, PackageManager.GET_SERVICES);
        return list != null && list.size() > 0;
    }



    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


}
