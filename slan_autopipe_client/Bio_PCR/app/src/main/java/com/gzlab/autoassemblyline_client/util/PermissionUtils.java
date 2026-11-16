package com.gzlab.autoassemblyline_client.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {
    private static final String TAG = "PermissionUtils";
    public static final int REQUEST_STORAGE_PERMISSION_CODE = 1001;
    public static final int REQUEST_MANAGE_ALL_FILES_CODE = 1002;

    /**
     * 判断是否已获得存储权限（适配 Android 11+ 的所有文件管理权限及低版本的读写权限）
     */
    public static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int writePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int readPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
            return writePermission == PackageManager.PERMISSION_GRANTED && readPermission == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * 自动检查并请求文件读写权限
     *
     * @param activity 当前 Activity
     * @return true 如果已经拥有权限，false 如果发起权限请求
     */
    public static boolean checkAndRequestStoragePermission(Activity activity) {
        if (hasStoragePermission(activity)) {
            return true;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, REQUEST_MANAGE_ALL_FILES_CODE);
            } catch (Exception e) {
                Log.e(TAG, "启动所有文件访问权限页面失败，尝试通用设置页", e);
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    activity.startActivityForResult(intent, REQUEST_MANAGE_ALL_FILES_CODE);
                } catch (Exception ex) {
                    Log.e(TAG, "启动通用设置页失败", ex);
                    Toast.makeText(activity, "请在系统设置中授予所有文件访问权限", Toast.LENGTH_LONG).show();
                }
            }
            return false;
        } else {
            List<String> permissionsNeeded = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (!permissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(
                        activity,
                        permissionsNeeded.toArray(new String[0]),
                        REQUEST_STORAGE_PERMISSION_CODE
                );
                return false;
            }
            return true;
        }
    }

    /**
     * 检查并请求所有必要的基础运行时权限（存储、位置等）
     */
    public static void checkAndRequestAllAppPermissions(Activity activity) {
        List<String> permissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(activity, permissions.toArray(new String[0]), REQUEST_STORAGE_PERMISSION_CODE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            checkAndRequestStoragePermission(activity);
        }
    }
}
