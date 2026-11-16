package com.gzlab.autoassemblyline_client.WintersUtils.utils;

import android.os.Environment;



import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    public static String FileSDPATH = getSDCardPath();

    /**
     * 获取到sd卡的根目录，并以String形式返回
     *
     * @return
     */
    public static String getSDCardPath() {
        FileSDPATH = Environment.getExternalStorageDirectory().toString() + "/";
        return FileSDPATH;
    }

    /**
     * 创建文件或文件夹
     *
     * @param fileName
     *            文件名或问文件夹名
     */
    public static void createFile(String fileName) {
        File file = new File(getSDCardPath() + fileName);
        if (file.exists()){
            System.out.println("文件存在");
            return;
        }
        // 判断SD卡是否存在，并且是否具有读写权限
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            System.out.println("SD卡不存在，或者没有读写权限");
            return;
        }
        if (fileName.indexOf(".") != -1) {
            // 说明包含，即使创建文件, 返回值为-1就说明不包含.,即使文件
            try {
                file.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println("创建了文件");
        } else {
            // 创建文件夹
            file.mkdir();
            System.out.println("创建了文件夹");
        }

    }


    public static String getDir(String dir) {

        String result = new String();

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {



            result = getSDCardPath() + dir + "/";

        }else{

            System.out.println("SD卡不存在，或者没有读写权限");

        }

        return result;

    }

    public static List<File> getFileList(String dir) {

        List<File> fileList = new ArrayList<File>();

        File file = new File(getSDCardPath() + dir);
        // 判断SD卡是否存在，并且是否具有读写权限
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            System.out.println("SD卡不存在，或者没有读写权限");
            return fileList;
        }
        if (!file.exists()){
            System.out.println("文件夹不存在");
            return fileList;
        }
        if (file.isFile()){
            System.out.println("不是文件夹");
            return fileList;
        }
        if (file.isDirectory()) {
            File[] result = file.listFiles();

            for (int i = 0; i < result.length;i++){
                if(result[i].isFile()){
                    fileList.add(result[i]);
                }
            }
            return fileList;
        }
        return fileList;
    }
}
