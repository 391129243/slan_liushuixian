package com.gidi.bio_pcr.mgr;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

public class AlarmUtils {
    private static final String TAG = "AlarmUtils";
    private static AlarmUtils mInstance = null;
    private Context context;

    public static AlarmUtils getInstance(Context context){
        if(null == mInstance){
            mInstance = new AlarmUtils(context);
        }
        return mInstance;
    }

    public AlarmUtils(Context context){
        this.context = context;
        initMediaPlayer();
    }

    public void destroyAlarm(){
        releasePlayer();
    }

    private boolean isSystemAlarm = false;

    private boolean isCountEnd = false;
    public void setSystemAlarm(boolean isSystemAlarm){
        this.isSystemAlarm = isSystemAlarm;
    }


    public boolean isSystemAlarm(){
        return isSystemAlarm;
    }

    public boolean isCountEnd() {
        return isCountEnd;
    }

    public void setCountEnd(boolean countEnd) {
        isCountEnd = countEnd;
    }

    private MediaPlayer mMediaPlayer;
    private boolean isPlaying = false;
    private static final int MODE_SINGLE_LOOP = 0;//循环播放
    private int curPlayAlarmRing = 0;//0:是没有播放
    private AssetFileDescriptor fd;
    private int currentMode = 0;


    private void initMediaPlayer(){
        Log.e(TAG,"initMediaPlayer");
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                // TODO Auto-generated method stub
                mMediaPlayer.start();
            }
        });

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                // TODO Auto-generated method stub
                if(isPlaying){
                    switch (currentMode) {
                        case MODE_SINGLE_LOOP:
//                            mMediaPlayer.start();
                            break;


                        default:
                            break;
                    }
                }

            }
        });

        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
                // TODO Auto-generated method stub
                return false;
            }
        });
        try {
            fd = context.getAssets().openFd("file/high.mp3");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public void startplay(int position){
        Log.e(TAG,"startplay");
        if(null != mMediaPlayer && mMediaPlayer.isPlaying()){
            mMediaPlayer.stop();
            mMediaPlayer.reset();
        }else{
            mMediaPlayer.reset();
        }
        try {
            if(null != fd){
                if(position == 1){
                    curPlayAlarmRing = 1;
                    fd = context.getAssets().openFd("low.mp3");
                }else if(position == 2){
                    curPlayAlarmRing = 2;
                    fd = context.getAssets().openFd("file/10217.mp3");
                }else if(position == 3){
                    curPlayAlarmRing = 3;
                    fd = context.getAssets().openFd("file/high.mp3");
                }
                // Log.i("xxxxs","curPlayAlarmRing---" + curPlayAlarmRing);
                mMediaPlayer.setDataSource(fd);
            }
            mMediaPlayer.setLooping(true);
            mMediaPlayer.prepareAsync();//prepare fail可能已经同步过了
            isPlaying = true;

        } catch (Exception e) {
            e.printStackTrace();
            //mMediaPlayer = null;
            Log.v(TAG, "play Exception " + e);
        }
        Log.v(TAG, "[Play] Start Preparing at ");

    }

    /***
     * 停止播放
     */
    public void stopPlay(){
        Log.e(TAG,"stopPlay");
        if(null != mMediaPlayer){
            mMediaPlayer.stop();
        }
        isPlaying = false;
        curPlayAlarmRing = 0;
    }

    private void releasePlayer(){
        if(null != mMediaPlayer){
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private boolean isPlay(){
        return isPlaying;
    }

    public int getPlayPosition(){
        return curPlayAlarmRing;
    }


}
