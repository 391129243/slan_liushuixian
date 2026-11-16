package com.gzlab.autoassemblyline_client.mgr;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;


public class FtpUploadManager {
    public static final String TAG = "FtpUploadManager" ;
    public static String ftpUrL = "120.79.243.146";
    public static int ftpPort = 22;
    public static String ftpUserName = "winters";
    public static String ftpPassWord = "123456";
    static FtpUploadManager mInstance;

    public static FtpUploadManager getInstance() {
        if (mInstance == null) {
            mInstance = new FtpUploadManager();
        }
        return mInstance;
    }

    public static String getFtpDir(){
        return "accessControl";
    }

    public static String getFileName(String path){
        String[] patharr = path.split("/");
        return patharr[patharr.length-1] ;
    }

    public static String getLocalPath(){
        return Environment.getExternalStorageDirectory().getPath() + "/mcs/accessControl" ;
    }

    public void upLoadFile(String filePath, String fileName, String fileFtpPath, Context mContext) {
        new Thread(new Runnable() {
            @Override
            public void run() {
            try{
                URL url = new URL("//www.baidu.com");
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                urlc.setConnectTimeout(3000);
                urlc.connect();
                if (urlc.getResponseCode() == 200) {
                }else{
                    ((Activity)mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext,"上报PCR文件至FTP失败，请检查网络",Toast.LENGTH_LONG);
                        }
                    });
                    return;
                }
                Thread.sleep(2000);
            } catch (Exception e) {
                ((Activity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext,"上报PCR文件至FTP失败，请检查网络",Toast.LENGTH_LONG);
                    }
                });
                Log.e(TAG, e.getMessage());
            }

            boolean isSuccess = false;
            FTPClient ftpClient = new FTPClient();
            try {
                ftpClientLogin(ftpClient);
//            ftpClient.makeDirectory(picFtpPath);
                CreateDirecroty(fileFtpPath,ftpClient);
//            ftpClient.changeWorkingDirectory(picFtpPath);
                ftpClient.setBufferSize(1024);
                ftpClient.setControlEncoding("UTF-8");
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                FileInputStream in = new FileInputStream(filePath);
                ftpClient.storeFile(fileName, in);
                in.close();
//            if(ftpClient.completePendingCommand()){
                isSuccess = true;
                ftpClient.logout();
                ((Activity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext,"上报PCR文件至FTP成功",Toast.LENGTH_LONG);
                    }
                });
//            }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }catch (Exception e) {
                Log.e(TAG, e.getMessage());
                ((Activity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext,"上报PCR文件至FTP失败，请检查网络",Toast.LENGTH_LONG);
                    }
                });
            } finally {
                if (ftpClient.isConnected()) {
                    try {
                        ftpClient.disconnect();
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
        }
    }).start();
    }


    public void upLoadFile(String filePath, String fileName, String fileFtpPath) {
        try{
            URL url = new URL("//www.baidu.com");
            HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
            urlc.setConnectTimeout(3000);
            urlc.connect();
            if (urlc.getResponseCode() == 200) {
            }else{
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isSuccess = false;
                FTPClient ftpClient = new FTPClient();
                try {
                    ftpClientLogin(ftpClient);
//            ftpClient.makeDirectory(picFtpPath);
                    CreateDirecroty(fileFtpPath,ftpClient);
//            ftpClient.changeWorkingDirectory(picFtpPath);
                    ftpClient.setBufferSize(1024);
                    ftpClient.setControlEncoding("UTF-8");
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                    FileInputStream in = new FileInputStream(filePath);
                    ftpClient.storeFile(fileName, in);
                    in.close();
//            if(ftpClient.completePendingCommand()){
                    isSuccess = true;
                    ftpClient.logout();
//            }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                } finally {
                    if (ftpClient.isConnected()) {
                        try {
                            ftpClient.disconnect();
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                }
            }
        }).start();
    }

    private void ftpClientLogin(FTPClient ftpClient) throws Exception {
        if (ftpPort != -1)
            ftpClient.connect(ftpUrL, ftpPort);
        else
            ftpClient.connect(ftpUrL);
        ftpClient.login(ftpUserName, ftpPassWord);
        int reply = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftpClient.disconnect();
            throw new Exception("FTP服务器连接失败");
        }
        ftpClient.enterLocalPassiveMode();
    }

    public void setServer(String[] server) {
        if(server==null){
            return;
        }
        if(server.length>3){
            try {
                ftpUrL = server[0];
                ftpPort = Integer.parseInt(server[1]);
                ftpUserName = server[2];
                ftpPassWord = server[3];
            }catch (Exception e){

            }
        }
    }

    //改变目录路径
    public boolean changeWorkingDirectory(String directory, FTPClient ftpClient) {
        boolean flag = true;
        try {
            flag = ftpClient.changeWorkingDirectory(directory);
            if (flag) {
                Log.d(TAG , "进入文件夹" + directory + " 成功！");

            } else {
                Log.d(TAG , "进入文件夹" + directory + " 失败！");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return flag;
    }

    //创建目录
    public boolean makeDirectory(String dir, FTPClient ftpClient) {
        boolean flag = true;
        try {
            flag = ftpClient.makeDirectory(dir);
            if (flag) {
                Log.i(TAG ,"创建文件夹" + dir + " 成功！");

            } else {
                Log.i(TAG ,"创建文件夹" + dir + " 失败！");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }


    //创建多层目录文件，如果有ftp服务器已存在该文件，则不创建，如果无，则创建
    public boolean CreateDirecroty(String remote, FTPClient ftpClient) throws IOException {
        boolean success = true;
        String directory = remote + "/";
//        String directory = remote.substring(0, remote.lastIndexOf("/") + 1);
        // 如果远程目录不存在，则递归创建远程服务器目录
        if (!directory.equalsIgnoreCase("/") && !changeWorkingDirectory(new String(directory),ftpClient)) {
            int start = 0;
            int end = 0;
            if (directory.startsWith("/")) {
                start = 1;
            } else {
                start = 0;
            }
            end = directory.indexOf("/", start);
            String path = "";
            String paths = "";
            while (true) {

                String subDirectory = new String(remote.substring(start, end).getBytes("utf-8"), "utf-8");
                path = path + "/" + subDirectory;
                if (!existFile(path,ftpClient)) {
                    if (makeDirectory(subDirectory,ftpClient)) {
                        changeWorkingDirectory(subDirectory,ftpClient);
                    } else {
                        Log.d(TAG ,"创建目录[" + subDirectory + "]失败");
                        changeWorkingDirectory(subDirectory,ftpClient);
                    }
                } else {
                    changeWorkingDirectory(subDirectory,ftpClient);
                }

                paths = paths + "/" + subDirectory;
                start = end + 1;
                end = directory.indexOf("/", start);
                // 检查所有目录是否创建完毕
                if (end <= start) {
                    break;
                }
            }
        }
        return success;
    }

    //判断ftp服务器文件是否存在
    public boolean existFile(String path, FTPClient ftpClient) throws IOException {
        boolean flag = false;
        FTPFile[] ftpFileArr = ftpClient.listFiles(path);
        if (ftpFileArr.length > 0) {
            flag = true;
        }
        return flag;
    }
}
