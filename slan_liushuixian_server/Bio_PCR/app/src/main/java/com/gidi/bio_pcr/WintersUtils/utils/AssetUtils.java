package com.gidi.bio_pcr.WintersUtils.utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.gidi.bio_pcr.service.MyWindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AssetUtils {

    /**
     * 将Asset下的文件复制到/data/data/.../files/目录下
     * @param context
     * @param fileName
     */
    public static boolean copyFromAsset(Context context, String fileName, boolean recreate) {
        byte[] buf = new byte[2048];
        try {
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/" + fileName);
            String dir = file.getParent();
            File fileDir = new File(dir);
            if(!fileDir.exists()){
                fileDir.mkdirs();
            }
            String destFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/" + fileName;
            File destFile = new File(destFilePath);
            if(!destFile.exists() || recreate){
                destFile.createNewFile();
            }else{
                return true;
            }
            FileOutputStream os = new FileOutputStream(destFilePath);// 得到数据库文件的写入流
            InputStream is = context.getAssets().open(fileName);// 得到数据库文件的数据流
            int cnt = -1;
            while ((cnt = is.read(buf)) != -1) {
                os.write(buf, 0, cnt);
            }
            os.flush();
            is.close();
            os.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            buf = null;
        }
    }

    public static String getDataFileFullPath(Context context, String fileName){
        File fileDir = context.getFilesDir();
        String destFilePath = fileDir.getAbsolutePath()+File.separator+fileName;
        return destFilePath;
    }


    public static File copyFile(String file, Context mContext,boolean recreate){
        File existedFile = null;
        String path ;
        try {
            copyFromAsset(mContext, file, recreate);
        } catch (Exception e) {
            Log.e("AssetUtils",e.getMessage());
        }
        path = getDataFileFullPath(mContext,file);
        existedFile = new File( path );
        if(existedFile.exists()){
            return existedFile;
        }
        return null;
    }


    /**
     * 遍历 assets 文件夹
     * @param path assets下的文件目录
     */
    public static void copyAssets(String path,Context context,boolean recreate){
        AssetManager am = context.getAssets();
        try {
            // 获取目录里全部的文件，包括文件夹
            String[] list = am.list(path);
            for (int i = 0; i < list.length; i++) {
                String file = path + "/" + list[i];    // 如果当前目录不是根目录时，创建子目录
                copyFile(file,context,recreate);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void installAssetApk(Context context , String apkPath) {
        try {
            // 读取 APK 文件
            InputStream inputStream = context.getAssets().open(apkPath);

            // 获取文件输出流
            FileOutputStream outputStream = context.openFileOutput("temp.apk", Context.MODE_PRIVATE);

            // 创建临时文件
            File file = new File(context.getFilesDir(), "temp.apk");

            // 将 APK 写入临时文件
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            // 关闭文件输出流
            outputStream.close();

            // 创建 Intent
            Intent intent = new Intent(Intent.ACTION_VIEW);

            // 设置 Intent 执行动作
            intent.setAction(Intent.ACTION_VIEW);

            // 设置 Intent 数据类型
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");

            // 设置 Intent 安装标志位
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // 启动安装 Intent
            context.startActivity(intent);

            if (!MyWindowManager.isWindowShowing()) {
                MyWindowManager.createSmallWindow(context);
            }


        } catch (IOException e) {

            e.printStackTrace();

        }
    }

}
