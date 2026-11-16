package com.gzlab.autoassemblyline_client.service;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.gzlab.autoassemblyline_client.autopipeline.mgr.WSSClient;

public class WebSocketService extends Service {

    private WSSClient wssClient;
    private final static int GRAY_SERVICE_ID = 1001;
    private WebSocketClientBinder mBinder = new WebSocketClientBinder();
    @Override
    public void onCreate() {
        super.onCreate();

    }

    public class WebSocketClientBinder extends Binder {
        public WebSocketService getService() {
            return WebSocketService.this;
        }
    }

    public static class GrayInnerService extends Service {

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startForeground(GRAY_SERVICE_ID, new Notification());
            stopForeground(true);
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i("WSSClient", "WebSocketService onBind");
        return mBinder;
    }
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("WSSClient","onStartCommand");
        //初始化Websocket
        initWebSocketClient();
        //设置service为前台服务，提高优先级
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            //Android4.3以下 ，隐藏Notification上的图标
            startForeground(GRAY_SERVICE_ID, new Notification());
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            //Android4.3 - Android8.0，隐藏Notification上的图标
            Intent innerIntent = new Intent(this, GrayInnerService.class);
            startService(innerIntent);
            startForeground(GRAY_SERVICE_ID, new Notification());
        } else {
            //Android8.0以上app启动后通知栏会出现一条"正在运行"的通知
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "前台服务通道",
                        NotificationManager.IMPORTANCE_LOW
                );
                NotificationManager manager = getSystemService(NotificationManager.class);
                manager.createNotificationChannel(channel);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                    Notification notification = new Notification.Builder(getApplicationContext(), CHANNEL_ID).build();
                    startForeground(GRAY_SERVICE_ID, notification);
                }
            }
         /*   NotificationChannel channel = new NotificationChannel(NotificationUtil.channel_id, NotificationUtil.channel_name,
                    NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Notification notification = new Notification.Builder(getApplicationContext(), NotificationUtil.channel_id_tai_bang).build();
                startForeground(GRAY_SERVICE_ID, notification);
            }*/
        }
        return START_STICKY;
    }

    private void initWebSocketClient(){
        Log.e("WSSClient","initWebSocketClient");
        wssClient = WSSClient.getInstance();
        wssClient.connect();
    }

    private void connect() {
        new Thread() {
            @Override
            public void run() {
                try {
                    //connectBlocking多出一个等待操作，会先连接再发送，否则未连接发送会报错
                    wssClient.connectBlocking();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
