package com.gidi.bio_pcr.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.config.FileBean;
import com.gidi.bio_pcr.config.UsbFileBean;
import com.gidi.bio_pcr.config.adapter.FilesListAdapter;
import com.gidi.bio_pcr.base.BaseActivity;
import com.gidi.bio_pcr.config.adapter.UsbFileAdapter;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.receiver.UsbBroadCastReceiver;
import com.gidi.bio_pcr.util.LoadingDialog;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.util.UsbCopySession;
import com.gidi.bio_pcr.util.UsbHelper;
import com.gidi.bio_pcr.view.CustomDialog;
import com.gidi.bio_pcr.view.NoticeDialog;
import com.gidi.bio_pcr.view.ProgressDialog;


import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import me.jahnen.libaums.core.UsbMassStorageDevice;
import me.jahnen.libaums.core.fs.UsbFile;

public class ResultExportActivity extends BaseActivity implements UsbBroadCastReceiver.UsbListener, View.OnClickListener{
    private RelativeLayout mTitleLayout;
    private TextView mTitleTv;
    private ImageView mBackImg;
    private ListView mLocalFileLv,mUdiskFileLv;
    private FilesListAdapter localAdapter;
    private UsbFileAdapter udiskAdapter;
    private List<FileBean> localFileList,checkLocalFiles;
    private List<UsbFileBean>usbFileList,checkUsbFiles;
    private UIHandler mUIHandler;
    private UsbHelper usbHelper;
    private TextView mLocalAllCheckTv,mLocalCancelTv,mLocalCopyTv,mLocalDelTv;
    private TextView mUsbAllCheckTv,mUsbCancelTv,mUsbCreateTv,mUsbDelTv,mUsbRenameTv;
    private TextView mLocalNumTv,mUsbNumTv;
    private boolean isLocalCheckShow = false;
    private boolean isLocalAllCheck = false;
    private boolean isUsbCheckShow = false;
    private boolean isUsbAllCheck = false;
    private NoticeDialog noticeDialog;
    private CustomDialog customDialog;
    private CustomDialog createDialog;//新建

    private Thread copyThread;

    private TextView mLocalBackTv,mUsbBackTv;
    private boolean isDetach = true;

    private boolean interruptCopyThread = false;

    private final String PCR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator
            +"PCR";
    private String localCurrentPath = PCR_PATH;
    //正在往 U 盘复制。复制期间不再响应插拔刷新,避免把正在写的连接打断
    private volatile boolean copying = false;
    //正在删除文件/文件夹。删除期间不再响应插拔刷新
    private volatile boolean deleting = false;
    //正在重命名文件/文件夹。重命名期间不再响应插拔刷新
    private volatile boolean renaming = false;
    //正在创建文件/文件夹。创建期间不再响应插拔刷新
    private volatile boolean creating = false;

    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_export_file;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        mTitleLayout = (RelativeLayout)findViewById(R.id.export_file_title_layout);
        mTitleTv = (TextView)mTitleLayout.findViewById(R.id.tab_title_tv);
        mTitleTv.setText(getResources().getString(R.string.result_export));
        mBackImg = (ImageView)mTitleLayout.findViewById(R.id.tab_back_img);
        mLocalBackTv = (TextView)findViewById(R.id.export_file_local_file_back_tv);
        mUsbBackTv = (TextView)findViewById(R.id.export_file_ufile_back_tv);

        mLocalFileLv= (ListView) findViewById(R.id.export_file_local_file_lv);
        localFileList = new CopyOnWriteArrayList<FileBean>();
        localAdapter = new FilesListAdapter(getApplicationContext());
        localAdapter.setFileList(localFileList);
        mLocalFileLv.setAdapter(localAdapter);

        mUdiskFileLv = (ListView) findViewById(R.id.export_file_udisk_file_lv);
        usbFileList = new CopyOnWriteArrayList<UsbFileBean>();
        udiskAdapter = new UsbFileAdapter(getApplicationContext());
        udiskAdapter.setFileList(usbFileList);
        mUdiskFileLv.setAdapter(udiskAdapter);

        mLocalAllCheckTv = (TextView)findViewById(R.id.export_file_allcheck_local_tv);
        mLocalCancelTv = (TextView)findViewById(R.id.export_file_cancel_local_tv);
        mLocalCopyTv = (TextView)findViewById(R.id.export_file_copy_local_tv);
        mLocalDelTv= (TextView)findViewById(R.id.export_file_delete_local_tv);
        mUsbAllCheckTv = (TextView)findViewById(R.id.export_file_allcheck_usb_tv);
        mUsbCancelTv = (TextView)findViewById(R.id.export_file_cancel_usb_tv);
        mUsbCreateTv = (TextView)findViewById(R.id.export_file_create_usb_tv);
        mUsbDelTv= (TextView)findViewById(R.id.export_file_delete_usb_tv);
        mUsbRenameTv= (TextView)findViewById(R.id.export_file_rename_usb_tv);
        mUsbBackTv.setVisibility(View.INVISIBLE);
        mLocalBackTv.setVisibility(View.INVISIBLE);
        localButtomAction(false);
        usbButtomAction(false);
    }

    @Override
    protected void initListener() {
        mLocalCancelTv.setOnClickListener(this);
        mLocalAllCheckTv.setOnClickListener(this);
        mLocalCopyTv.setOnClickListener(this);
        mLocalDelTv.setOnClickListener(this);
        mUsbCancelTv.setOnClickListener(this);
        mUsbAllCheckTv.setOnClickListener(this);
        mUsbCreateTv.setOnClickListener(this);
        mUsbDelTv.setOnClickListener(this);
        mUsbRenameTv.setOnClickListener(this);
        mLocalBackTv.setOnClickListener(this);
        mUsbBackTv.setOnClickListener(this);

        mBackImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        localAdapter.setOnItemClickListener(new FilesListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                //点击是打开文件
                localAdapter.setSelectedPosition(position);
                File file = localFileList.get(position).getFile();
                openLocalFile(file);
            }

            @Override
            public void onItemLongClick(int position) {
                //长按是显示选中框
                if(!isLocalCheckShow){
                    isLocalCheckShow = true;
                    checkLocalFiles.clear();
                    for(FileBean bean : localFileList){
                        bean.setShow(true);
                    }
                    FileBean fileBean = localFileList.get(position);
                    fileBean.setCheck(true);
                    checkLocalFiles.add(fileBean);
                    localAdapter.setFileList(localFileList);
                    localButtomAction(true);
                }

            }

            @Override
            public void onItemCheck(int position) {
                FileBean fileBean1 = localFileList.get(position);
                if(fileBean1.isCheck()){
                    fileBean1.setCheck(false);
                }else {
                    fileBean1.setCheck(true);
                }
                if (fileBean1.isCheck() && !checkLocalFiles.contains(fileBean1)) {
                    checkLocalFiles.add(fileBean1);
                } else if (!fileBean1.isCheck() && checkLocalFiles.contains(fileBean1)) {
                    checkLocalFiles.remove(fileBean1);
                }
                localAdapter.setFileList(localFileList);
            }
        });
    }

    @Override
    protected void initData() {
        usbHelper = new UsbHelper(this.getApplicationContext(), this);

        checkLocalFiles = new CopyOnWriteArrayList<FileBean>();
        checkUsbFiles = new CopyOnWriteArrayList<UsbFileBean>();

        mUIHandler = new UIHandler(this);
        initLocalFiles();
        initUsbFiles();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();

    }

    protected void onDestroy() {
        super.onDestroy();
        if(null != usbHelper){
            usbHelper.removeReceiver(this);
            usbHelper.finishUsbHelper();
//            usbHelper = null;
        }
        if(null != localFileList){
            localFileList.clear();
            localAdapter.notifyDataSetChanged();
            localFileList = null;
        }

        if(null != usbFileList){
            usbFileList.clear();
            udiskAdapter.notifyDataSetChanged();
            usbFileList = null;
        }
        if(null != checkLocalFiles){
            checkLocalFiles.clear();
            checkLocalFiles = null;
        }

        if(null != checkUsbFiles){
            checkUsbFiles.clear();
            checkUsbFiles = null;
        }
        if(null != mUIHandler){
            mUIHandler.removeCallbacksAndMessages(null);
            mUIHandler = null;
        }
        if(null != noticeDialog){
            noticeDialog.dismiss();
            noticeDialog = null;
        }
        if(null != customDialog){
            customDialog.dismiss();
            customDialog = null;
        }
        if(null != progressDialog){
            progressDialog.dismiss();
            progressDialog = null;
        }
        hideLoadingDialog();
    }


    private void initLocalFiles(){
        localFileList.clear();
        localAdapter.notifyDataSetChanged();
        File[] files = new File(PCR_PATH).listFiles();//文件夹下的所有文件
        if(files != null && files.length>0) {
            for (int i = 0; i < files.length; i++) {
                if (null != localFileList) {
                    if(files[i].getName().equals("config")||files[i].getName().equals("result")
                    || files[i].getName().equals("image")
                            || files[i].getName().equals("result_report")){
                        localFileList.add(new FileBean(files[i],false,false));
                        Collections.sort(localFileList);
                    }
                }
            }
        }
        localAdapter.setFileList(localFileList);
        localAdapter.notifyDataSetChanged();

        mLocalBackTv.setVisibility(View.INVISIBLE);
    }

    private void initUsbFiles(){

        udiskAdapter.setFileList(usbFileList);
        udiskAdapter.setOnItemClickListener(new UsbFileAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                //点击打开u盘文件
                UsbFile usbFile = usbFileList.get(position).getUsbFile();
                if(usbFile.isDirectory()){
                    openUsbFile(usbFile);
                }else{
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.result_export_toast_file_cannot_open),Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onItemLongClick(int position) {
                //长按
                if(!isUsbCheckShow){
                    checkUsbFiles.clear();
                    isUsbCheckShow = true;
                    for(UsbFileBean bean : usbFileList){
                        bean.setShow(true);
                    }
                    UsbFileBean usbFileBean = usbFileList.get(position);
                    usbFileBean.setCheck(true);
                    checkUsbFiles.add(usbFileBean);
                    udiskAdapter.setFileList(usbFileList);
                    usbButtomAction(true);
                }
            }

            @Override
            public void onItemCheck(int position) {
                UsbFileBean usbFileBean = usbFileList.get(position);
                if(usbFileBean.isCheck()){
                    usbFileBean.setCheck(false);
                }else {
                    usbFileBean.setCheck(true);
                }
                if (usbFileBean.isCheck() && !checkUsbFiles.contains(usbFileBean)) {
                    checkUsbFiles.add(usbFileBean);
                } else if (!usbFileBean.isCheck() && checkUsbFiles.contains(usbFileBean)) {
                    checkUsbFiles.remove(usbFileBean);
                }
                udiskAdapter.setFileList(usbFileList);
            }
        });
        updateUsbFile(0);
    }

    private void updateUsbFile(int position) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //throw new RuntimeException(e);
                }
                if(null != usbHelper && usbFileList != null){
                    UsbMassStorageDevice[] usbMassStorageDevices = usbHelper.getDeviceList();
                    if (usbMassStorageDevices.length > 0) {
                        //存在USB
                        usbFileList.clear();
                        ResultExportActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                udiskAdapter.notifyDataSetChanged();
                            }
                        });

                        //usbList.addAll(usbHelper.readDevice(usbMassStorageDevices[position]));

                        ArrayList<UsbFile> allUsbFile = usbHelper.readDevice(usbMassStorageDevices[position]);
                        //////
                        if(allUsbFile == null){
                            return;
                        }
                        for (UsbFile usbFile : allUsbFile) {
                            /////activity死了，usbFileList=null, 可能出CRASH
                            if(null != usbFileList){
                                usbFileList.add(new UsbFileBean(usbFile, false, false));
                                isDetach = false;
                            }
                        }
                    }else {
                        usbFileList.clear();
                        ResultExportActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                udiskAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
                ResultExportActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        udiskAdapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.export_file_ufile_back_tv){
            usbFileBack();
        }else if(id == R.id.export_file_local_file_back_tv){
           localFilesBack();
        } else if(id == R.id.export_file_delete_local_tv){
            deleteLocalFile();
        }else if(id == R.id.export_file_copy_local_tv){
            if(checkLocalFiles != null && checkLocalFiles.size()>0){
                showNoticeDialog(getResources().getString(R.string.result_export_dialog_message_copy),2);
            }else{
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.result_export_toast_copy_file),Toast.LENGTH_SHORT).show();
            }

        }else if(id == R.id.export_file_allcheck_local_tv){
            localFileAllCheck();
        }else if(id == R.id.export_file_cancel_local_tv){
            cancelLocalCheck();
        }else if(id == R.id.export_file_allcheck_usb_tv){
            usbFileAllCheck();
        }else if(id == R.id.export_file_cancel_usb_tv){
            cancelUsbCheck();
        }else if(id == R.id.export_file_delete_usb_tv){
            if(checkUsbFiles != null && checkUsbFiles.size()>0){
                ProgressDialog dialog = new ProgressDialog(ResultExportActivity.this,
                        ResultExportActivity.this.getResources().getString(R.string.result_export_dialog_calculate_size));
                dialog.setCancelable(true);
                dialog.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        long length = getTotalLength();
                        if(null != ResultExportActivity.this){
                            new Thread(new Runnable() {
                                @Override
                                public void run() {

                                    ResultExportActivity.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            dialog.dismiss();
                                            if(length < 32 * 8  * 1024 * 1024L){
                                                showNoticeDialog(getResources().getString(R.string.result_export_dialog_message_delete),1);
                                            }else{
                                                //删除的文件太大（256M）,请减少删除数量
                                                Toast.makeText(getApplicationContext(),
                                                        getResources().getString(R.string.result_export_toast_file_to_large),Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });
                                }
                            }).start();
                        }
                    }
                }).start();

            }else{
                Toast.makeText(getApplicationContext()
                        ,getResources().getString(R.string.result_export_toast_delete_file),Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.export_file_rename_usb_tv){
            if(checkUsbFiles !=null && checkUsbFiles.size()>0){
                if(checkUsbFiles.size()>1){
                    //不能进行批量重命名
                    Toast.makeText(getApplicationContext()
                            ,getResources().getString(R.string.result_export_toast_batch_renaming),Toast.LENGTH_LONG).show();
                }else{
                    UsbFile usbFile = checkUsbFiles.get(0).getUsbFile();
                    //重命名
                    showCustomDialog(getResources().getString(R.string.result_export_rename),usbFile,1);
                }
            }
        }else if(id == R.id.export_file_create_usb_tv){
            //弹出对话框提示新建文件还是文件夹
            //新建
            showCreateDialog(getResources().getString(R.string.result_export_create));
        }
    }

    private void deleteLocalFile(){
      //  Log.i("AAA","选中删除部分个数" + checkLocalFiles.size());
    }

    private long getTotalLength(){
        long length = 0L;
        for(UsbFileBean usbFileBean :checkUsbFiles){
            UsbFile usbFile = usbFileBean.getUsbFile();
            length = length + getFileLength(usbFile);
        }
        return length;
    }

    private long getFileLength(UsbFile usbFile){
        long length = 0L;
        try{
            if(usbFile.isDirectory()){
                UsbFile[] usbFiles = usbFile.listFiles();
                if(null != usbFiles && usbFiles.length>0){
                    for(int i=0;i<usbFiles.length;i++){
                        if(usbFiles[i].isDirectory()){
                            length = length +getFileLength(usbFiles[i]);
                        }else{
                            length = length +usbFiles[i].getLength();
                        }
                    }
                }
            }else{
                length =+usbFile.getLength();
            }
        }catch (Exception e){
            e.getMessage();
        }
        return length;
    }

    /**
     * 统计待复制内容的总字节数,用于复制前和 U 盘剩余空间比较。
     * 目录读不到(listFiles 返回 null)时记为 -1,调用方据此放弃这次复制而不是照写。
     */
    private long totalSizeOf(List<FileBean> files) {
        long total = 0;
        for (FileBean bean : files) {
            long size = sizeOf(bean.getFile());
            if (size < 0) {
                return -1;
            }
            total += size;
        }
        return total;
    }

    private long sizeOf(File file) {
        if (file == null || !file.exists()) {
            return 0;
        }
        if (file.isFile()) {
            return file.length();
        }
        File[] children = file.listFiles();
        if (children == null) {
            return -1;
        }
        long total = 0;
        for (File child : children) {
            long size = sizeOf(child);
            if (size < 0) {
                return -1;
            }
            total += size;
        }
        return total;
    }

    /**
     * 本地文件复制到U盘。
     * 写到 U 盘当前目录下新建的时间戳目录里,不删除 U 盘上已有的内容。
     */
    private void localFileCopyToUsb(){
        if (copying) {
            //上一次复制还没结束,不再开第二个线程去抢同一个 U 盘
            hideFileProgressDialog();
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.result_export_dialog_message_copying_already), Toast.LENGTH_LONG).show();
            return;
        }
        if (checkLocalFiles == null || checkLocalFiles.size() == 0 || usbHelper == null) {
            hideFileProgressDialog();
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.result_export_toast_copy_file), Toast.LENGTH_LONG).show();
            return;
        }
        //复制期间的插拔广播不再刷新列表,见 insertUsb/removeUsb
        copying = true;
        usbHelper.resetCopyCancelled();
        final List<FileBean> toCopy = new ArrayList<>(checkLocalFiles);
        copyThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String failReason = null;
                UsbCopySession session = null;
                try {
                    UsbFile destParent = usbHelper.getCurrentFolder();
                    if (destParent == null) {
                        //U 盘没挂载上:没授权、不是 FAT32,或还没读到分区
                        failReason = getResources().getString(R.string.result_export_dialog_message_copy_not_fat32);
                    } else {
                        long need = totalSizeOf(toCopy);
                        long free = usbHelper.getFreeSpace();
                        //留 8MB 给 FAT 表和新目录项,避免写到最后一簇时失败
                        if (need < 0 || free < 0 || need + 8L * 1024 * 1024 > free) {
                            failReason = getResources().getString(R.string.result_export_dialog_message_copy_no_space);
                        }
                    }
                    if (failReason == null) {
                        List<File> sources = new ArrayList<>();
                        for (FileBean bean : toCopy) {
                            sources.add(bean.getFile());
                        }
                        session = new UsbCopySession(usbHelper, sources, destParent);
                        session.run(copyProgressListener);
                    }
                } catch (Exception e) {
                    Log.e("ResultExportActivity", "copy to usb failed: " + e.getMessage());
                    failReason = getResources().getString(R.string.result_export_dialog_message_copy_failed);
                }
                copying = false;
                if (mUIHandler != null) {
                    Message msg = Message.obtain();
                    msg.what = MsgType.MSG_END_COPY_LOCAL;
                    msg.obj = failReason == null ? session : failReason;
                    mUIHandler.sendMessage(msg);
                }
            }
        });
        copyThread.start();
    }

    /** 复制进度回调,由复制线程调用,转发到主线程更新进度框 */
    private final UsbHelper.CopyProgressListener copyProgressListener = new UsbHelper.CopyProgressListener() {
        //当前正在复制的文件名,随进度一起发给进度框
        private volatile String fileName = "";

        @Override
        public void copyStart(String name) {
            fileName = name;
            copyProgress(0);
        }

        @Override
        public void copyProgress(int progress) {
            if (mUIHandler != null) {
                Message msg = Message.obtain();
                msg.what = MsgType.MSG_COPY_PROGRESS;
                msg.obj = fileName;
                msg.arg1 = progress;
                mUIHandler.sendMessage(msg);
            }
        }

        @Override
        public void copyResult(boolean result) {
        }
    };


    /**
     * 本地文件全选
     */
    private void localFileAllCheck(){
        if(isLocalAllCheck){
            isLocalAllCheck = false;
            for(FileBean fileBean1 : localFileList){
                fileBean1.setCheck(false);
            }
            localAdapter.setFileList(localFileList);
            checkLocalFiles.clear();
        }else {
            isLocalAllCheck = true;
            for(FileBean fileBean1  : localFileList){
                fileBean1.setCheck(true);
            }
            localAdapter.setFileList(localFileList);
            checkLocalFiles.clear();
            checkLocalFiles.addAll(localFileList);
        }
    }

    /**
     * 删除本地
     */

    private void cancelLocalCheck(){
        for(FileBean fileBean1 : localFileList){
            fileBean1.setCheck(false);
            fileBean1.setShow(false);
        }
        localAdapter.setFileList(localFileList);
        localAdapter.notifyDataSetChanged();
        if(null != checkLocalFiles){
            checkLocalFiles.clear();
        }
        localButtomAction(false);
        isLocalCheckShow = false;
        isLocalAllCheck = false;
    }

    /**
     * Usb文件返回上一级
     */
    private void localFilesBack(){
        File file = new File(localCurrentPath).getParentFile();
        if(file != null){
            openLocalFile(new File(localCurrentPath).getParentFile());
        }
        isLocalCheckShow = false;
        isLocalAllCheck = false;
        localButtomAction(false);
    }

    /**
     * U盘文件返回上一级
     */
    private void usbFileBack(){
        if(usbHelper.getParentFolder() != null){
            if(StringUtil.isEmpty(usbHelper.getParentFolder().getName())){
                mUsbBackTv.setVisibility(View.INVISIBLE);
            }else{
                mUsbBackTv.setVisibility(View.VISIBLE);
            }
            openUsbFile(usbHelper.getParentFolder());
        }
        isUsbAllCheck =false;
        isUsbCheckShow = false;
        usbButtomAction(false);
    }

    private void cancelUsbCheck(){
        for(UsbFileBean usbFile : usbFileList){
            usbFile.setCheck(false);
            usbFile.setShow(false);
        }
        udiskAdapter.setFileList(usbFileList);
        udiskAdapter.notifyDataSetChanged();
        checkUsbFiles.clear();
        usbButtomAction(false);
        isUsbAllCheck = false;
        isUsbCheckShow = false;
    }

    private void usbFileAllCheck(){
        if(isUsbAllCheck){
            isUsbAllCheck = false;
            for(UsbFileBean usbFileBean : usbFileList){
                usbFileBean.setCheck(false);
            }
            udiskAdapter.setFileList(usbFileList);
            checkUsbFiles.clear();
        }else {
            isUsbAllCheck = true;
            for(UsbFileBean usbFileBean  : usbFileList){
                usbFileBean.setCheck(true);
            }
            udiskAdapter.setFileList(usbFileList);
            checkUsbFiles.clear();
            checkUsbFiles.addAll(usbFileList);
        }
    }

    private LoadingDialog loadingDialog;
    public void showLoadingDialog(String message){
        if(null == loadingDialog){
            loadingDialog = new LoadingDialog(this, message);
            loadingDialog.setCancelable(false);
            loadingDialog.show();
        }
    }

    /*
     * 隐藏提示加载
     */
    public void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }
    /**
     * 删除U盘文件
     */
    private void deleteUsbFileAction(){
        //删除后进行更新
        isUsbAllCheck = false;
        isUsbCheckShow = false;
        usbButtomAction(false);
        //正在删除,请等待
        showLoadingDialog(getResources().getString(R.string.result_export_dialog_message_deleting));
        new Thread(new Runnable() {
            @Override
            public void run() {
                deleting = true;
                try{
                    final boolean result = deleteCheckUsbFile();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (result) {
                                hideLoadingDialog();
                                try {
                                    openUsbFile(usbHelper.getCurrentFolder());
                                } catch (Exception e) {
                                    Log.e("ResultExportActivity", "open usb folder after delete failed: " + e.getMessage());
                                }
                                checkUsbFiles.clear();
                                //删除成功
                                Toast.makeText(getApplicationContext()
                                        , getResources().getString(R.string.result_export_toast_delete_success), Toast.LENGTH_SHORT).show();
                            } else {
                                hideLoadingDialog();
                                try {
                                    openUsbFile(usbHelper.getCurrentFolder());
                                } catch (Exception e) {
                                    Log.e("ResultExportActivity", "open usb folder after delete failed: " + e.getMessage());
                                }
                                checkUsbFiles.clear();
                                //删除失败
                                Toast.makeText(getApplicationContext()
                                        , getResources().getString(R.string.result_export_toast_delete_fail), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }catch (Exception E){
                    Log.e("ResultExportActivity", "delete usb file failed: " + E.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideLoadingDialog();
                            try {
                                openUsbFile(usbHelper.getCurrentFolder());
                            } catch (Exception e) {
                                Log.e("ResultExportActivity", "open usb folder after delete failed: " + e.getMessage());
                            }
                            checkUsbFiles.clear();
                            //删除失败
                            Toast.makeText(getApplicationContext()
                                    , getResources().getString(R.string.result_export_toast_delete_fail), Toast.LENGTH_SHORT).show();
                        }
                    });
                } finally {
                    deleting = false;
                }
            }
        }).start();
    }
    private boolean deleteCheckUsbFile(){
        try {
            for(UsbFileBean usbFileBean : checkUsbFiles){
                UsbFile usbFile = usbFileBean.getUsbFile();
                boolean result = removeUsbFile(usbFile);
                if(!result){
                    return false;
                }
            }
            return true;
        }catch (Exception e ){
            Log.e("ResultExportActivity",e.getMessage());
            return false;
        }
    }

    private boolean removeUsbFile(UsbFile usbFile){
        try{
            usbFile.delete();
            return true;
        }catch (Exception e){
            e.getMessage();
            Log.e("ResultExportActivity",e.getMessage());
            return false;
        }
    }




    /**
     * U盘文件重命名
     * @param usbFileName
     */
    private void renameUsbFile(String usbFileName, UsbFile usbFile){
        renaming = true;
        try{
            usbFile.setName(usbFileName);
            //重命名成功
            Toast.makeText(getApplicationContext()
                    ,getResources().getString(R.string.result_export_toast_rename_success),Toast.LENGTH_LONG).show();
            updateUsbFile(0);
        }catch (Exception e){
            Log.e("ResultExportActivity", "rename usb file failed: " + e.getMessage());
            //重命名失败
            Toast.makeText(getApplicationContext()
                    ,getResources().getString(R.string.result_export_toast_rename_fail),Toast.LENGTH_LONG).show();
        } finally {
            renaming = false;
        }
        isUsbCheckShow = false;
        isUsbAllCheck = false;
        usbButtomAction(false);
    }

    /**
     * 新建Usb文件
     */
    private void createUsbFile(String usbFileName, boolean isDirectory){
        creating = true;
        try {
            if(null != usbHelper){
                if(isDirectory){
                    try{
                        UsbFile usbFile = usbHelper.getCurrentFolder();
                        if (usbFile == null) {
                            Toast.makeText(getApplicationContext()
                                    ,getResources().getString(R.string.result_export_toast_create_foder_fail),Toast.LENGTH_LONG).show();
                            return;
                        }
                        UsbFile createDirectory = usbFile.createDirectory(usbFileName);
                        if(null != createDirectory){
                            //创建文件夹成功
                            Toast.makeText(getApplicationContext()
                                    ,getResources().getString(R.string.result_export_toast_create_foder_success),Toast.LENGTH_LONG).show();
                            updateUsbFile(0);
                        }else{
                            //创建文件夹失败
                            Toast.makeText(getApplicationContext()
                                    ,getResources().getString(R.string.result_export_toast_create_foder_fail),Toast.LENGTH_LONG).show();
                        }
                    }catch (Exception e){
                        Log.e("ResultExportActivity", "create usb directory failed: " + e.getMessage());
                        Toast.makeText(getApplicationContext()
                                ,getResources().getString(R.string.result_export_toast_create_foder_fail),Toast.LENGTH_LONG).show();
                    }
                }else{
                    try {
                        UsbFile usbFile = usbHelper.getCurrentFolder();
                        if (usbFile == null) {
                            Toast.makeText(getApplicationContext()
                                    ,getResources().getString(R.string.result_export_toast_create_file_fail),Toast.LENGTH_LONG).show();
                            return;
                        }
                        UsbFile createFile = usbFile.createFile(usbFileName);
                        if(createFile != null){
                            //创建文件成功
                            Toast.makeText(getApplicationContext()
                                    ,getResources().getString(R.string.result_export_toast_create_file_success),Toast.LENGTH_LONG).show();
                            updateUsbFile(0);
                        }else{
                            //创建文件失败
                            Toast.makeText(getApplicationContext()
                                    ,getResources().getString(R.string.result_export_toast_create_file_fail),Toast.LENGTH_LONG).show();
                        }
                    }catch (Exception e){
                        Log.e("ResultExportActivity", "create usb file failed: " + e.getMessage());
                        Toast.makeText(getApplicationContext()
                                ,getResources().getString(R.string.result_export_toast_create_file_fail),Toast.LENGTH_LONG).show();
                    }
                }
            }
        } finally {
            creating = false;
        }
    }

    /**
     * 打开本地文件
     *
     */
    private void openLocalFile(File file) {
        if(file.getAbsolutePath().equals(PCR_PATH)){
            initLocalFiles();
        }else {
            if (file.isDirectory()) {
                //文件夹更新列表
                localFileList.clear();
                localAdapter.notifyDataSetChanged();
                File[] files = file.listFiles();
                if (null != files) {
                    if (files.length > 0) {
                        for (int i = 0; i < files.length; i++) {
                            localFileList.add(new FileBean(files[i], false, false));
                        }
                    }
                    Collections.sort(localFileList);
                }
                localAdapter.notifyDataSetChanged();
                localCurrentPath = file.getAbsolutePath();
            }
            mLocalBackTv.setVisibility(View.VISIBLE);
        }
    }
    /**
     * 打开U盘中的文件
     *
     */
    private void  openUsbFile(UsbFile usbFile){
        if (usbFile.isDirectory()) {
            //文件夹更新列表
            if(null != usbFile){
                if(usbFile.getName().equals("/")){
                    mUsbBackTv.setVisibility(View.INVISIBLE);
                }else {
                    mUsbBackTv.setVisibility(View.VISIBLE);
                }
            }
            usbFileList.clear();
            udiskAdapter.notifyDataSetChanged();
            ArrayList<UsbFile>usbFiles = usbHelper.getUsbFolderFileList(usbFile);

            if(null != usbFiles && usbFiles.size()>0){
                for(UsbFile bean :usbFiles){
                    usbFileList.add(new UsbFileBean(bean,false,false));
                }
            }
            udiskAdapter.setFileList(usbFileList);
            udiskAdapter.notifyDataSetChanged();
        }
    }

    private void localButtomAction(boolean isEnable){
        if(isEnable){
            mLocalAllCheckTv.setEnabled(true);
            Drawable allCkImg = getResources().getDrawable(R.drawable.icon_allcheck_enable);
            allCkImg.setBounds(0,0,allCkImg.getMinimumWidth(),allCkImg.getMinimumHeight());
            mLocalAllCheckTv.setCompoundDrawables(null,allCkImg,null,null);
            mLocalAllCheckTv.setTextColor(getResources().getColor(R.color.deep_gray));

            mLocalCancelTv.setEnabled(true);
            Drawable cancelImg = getResources().getDrawable(R.drawable.icon_cancel_enable);
            cancelImg.setBounds(0,0,cancelImg.getMinimumWidth(),cancelImg.getMinimumHeight());
            mLocalCancelTv.setCompoundDrawables(null,cancelImg,null,null);
            mLocalCancelTv.setTextColor(getResources().getColor(R.color.deep_gray));

            mLocalCopyTv.setEnabled(true);
            Drawable copyImg = getResources().getDrawable(R.drawable.icon_copy_enanable);
            copyImg.setBounds(0,0,copyImg.getMinimumWidth(),copyImg.getMinimumHeight());
            mLocalCopyTv.setCompoundDrawables(null,copyImg,null,null);
            mLocalCopyTv.setTextColor(getResources().getColor(R.color.deep_gray));

            mLocalDelTv.setEnabled(true);
            Drawable deleteImg = getResources().getDrawable(R.drawable.icon_delete_enable);
            deleteImg.setBounds(0,0,deleteImg.getMinimumWidth(),deleteImg.getMinimumHeight());
            mLocalDelTv.setCompoundDrawables(null,deleteImg,null,null);
            mLocalDelTv.setTextColor(getResources().getColor(R.color.deep_gray));
        }else{
            mLocalAllCheckTv.setEnabled(false);
            Drawable allCkImg = getResources().getDrawable(R.drawable.icon_allcheck_no);
            allCkImg.setBounds(0,0,allCkImg.getMinimumWidth(),allCkImg.getMinimumHeight());
            mLocalAllCheckTv.setCompoundDrawables(null,allCkImg,null,null);
            mLocalAllCheckTv.setTextColor(getResources().getColor(R.color.light_gray));

            mLocalCancelTv.setEnabled(false);
            Drawable cancelImg = getResources().getDrawable(R.drawable.icon_cancel_no);
            cancelImg.setBounds(0,0,cancelImg.getMinimumWidth(),cancelImg.getMinimumHeight());
            mLocalCancelTv.setCompoundDrawables(null,cancelImg,null,null);
            mLocalCancelTv.setTextColor(getResources().getColor(R.color.light_gray));

            mLocalCopyTv.setEnabled(false);
            Drawable copyImg = getResources().getDrawable(R.drawable.icon_copy_file);
            copyImg.setBounds(0,0,copyImg.getMinimumWidth(),copyImg.getMinimumHeight());
            mLocalCopyTv.setCompoundDrawables(null,copyImg,null,null);
            mLocalCopyTv.setTextColor(getResources().getColor(R.color.light_gray));

            mLocalDelTv.setEnabled(false);
            Drawable deleteImg = getResources().getDrawable(R.drawable.icon_delete_no);
            deleteImg.setBounds(0,0,deleteImg.getMinimumWidth(),deleteImg.getMinimumHeight());
            mLocalDelTv.setCompoundDrawables(null,deleteImg,null,null);
            mLocalDelTv.setTextColor(getResources().getColor(R.color.light_gray));
        }

    }

    private void usbButtomAction(boolean isEnable){
        if(isEnable){
            mUsbAllCheckTv.setEnabled(true);
            Drawable allCkImg = getResources().getDrawable(R.drawable.icon_allcheck_enable);
            allCkImg.setBounds(0,0,allCkImg.getMinimumWidth(),allCkImg.getMinimumHeight());
            mUsbAllCheckTv.setCompoundDrawables(null,allCkImg,null,null);
            mUsbAllCheckTv.setTextColor(getResources().getColor(R.color.deep_gray));

            mUsbCancelTv.setEnabled(true);
            Drawable cancelImg = getResources().getDrawable(R.drawable.icon_cancel_enable);
            cancelImg.setBounds(0,0,cancelImg.getMinimumWidth(),cancelImg.getMinimumHeight());
            mUsbCancelTv.setCompoundDrawables(null,cancelImg,null,null);
            mUsbCancelTv.setTextColor(getResources().getColor(R.color.deep_gray));

            mUsbCreateTv.setEnabled(false);
            Drawable createImg = getResources().getDrawable(R.drawable.icon_create_no);
            createImg.setBounds(0,0,createImg.getMinimumWidth(),createImg.getMinimumHeight());
            mUsbCreateTv.setCompoundDrawables(null,createImg,null,null);
            mUsbCreateTv.setTextColor(getResources().getColor(R.color.light_gray));

            mUsbDelTv.setEnabled(true);
            Drawable deleteImg = getResources().getDrawable(R.drawable.icon_delete_enable);
            deleteImg.setBounds(0,0,deleteImg.getMinimumWidth(),deleteImg.getMinimumHeight());
            mUsbDelTv.setCompoundDrawables(null,deleteImg,null,null);
            mUsbDelTv.setTextColor(getResources().getColor(R.color.deep_gray));

            mUsbRenameTv.setEnabled(true);
            Drawable renameImg = getResources().getDrawable(R.drawable.icon_rename_enable);
            renameImg.setBounds(0,0,renameImg.getMinimumWidth(),deleteImg.getMinimumHeight());
            mUsbRenameTv.setCompoundDrawables(null,renameImg,null,null);
            mUsbRenameTv.setTextColor(getResources().getColor(R.color.deep_gray));


        }else{
            mUsbAllCheckTv.setEnabled(false);
            Drawable allCkImg = getResources().getDrawable(R.drawable.icon_allcheck_no);
            allCkImg.setBounds(0,0,allCkImg.getMinimumWidth(),allCkImg.getMinimumHeight());
            mUsbAllCheckTv.setCompoundDrawables(null,allCkImg,null,null);
            mUsbAllCheckTv.setTextColor(getResources().getColor(R.color.light_gray));

            mUsbCancelTv.setEnabled(false);
            Drawable cancelImg = getResources().getDrawable(R.drawable.icon_cancel_no);
            cancelImg.setBounds(0,0,cancelImg.getMinimumWidth(),cancelImg.getMinimumHeight());
            mUsbCancelTv.setCompoundDrawables(null,cancelImg,null,null);
            mUsbCancelTv.setTextColor(getResources().getColor(R.color.light_gray));

            mUsbCreateTv.setEnabled(true);
            Drawable createImg = getResources().getDrawable(R.drawable.icon_create_enable);
            createImg.setBounds(0,0,createImg.getMinimumWidth(),createImg.getMinimumHeight());
            mUsbCreateTv.setCompoundDrawables(null,createImg,null,null);
            mUsbCreateTv.setTextColor(getResources().getColor(R.color.deep_gray));

            mUsbDelTv.setEnabled(false);
            Drawable deleteImg = getResources().getDrawable(R.drawable.icon_delete_no);
            deleteImg.setBounds(0,0,deleteImg.getMinimumWidth(),deleteImg.getMinimumHeight());
            mUsbDelTv.setCompoundDrawables(null,deleteImg,null,null);
            mUsbDelTv.setTextColor(getResources().getColor(R.color.light_gray));

            mUsbRenameTv.setEnabled(false);
            Drawable renameImg = getResources().getDrawable(R.drawable.icon_rename_no);
            renameImg.setBounds(0,0,renameImg.getMinimumWidth(),deleteImg.getMinimumHeight());
            mUsbRenameTv.setCompoundDrawables(null,renameImg,null,null);
            mUsbRenameTv.setTextColor(getResources().getColor(R.color.light_gray));

        }

    }

    private boolean isSameNameUsbFile(String fileName){
        if(null != usbFileList && usbFileList.size()>0){
            for (UsbFileBean fileBean : usbFileList){
                if(fileBean.getUsbFile().getName().equals(fileName)){
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void insertUsb(UsbDevice device_add) {
        //复制、删除、重命名、创建过程中不刷新:刷新会重新打开设备并抢占 USB 接口,把正在操作的连接打断
        if (copying || deleting || renaming || creating) {
            return;
        }
        if(null != mUIHandler){
            Message msg = Message.obtain();
            msg.obj = device_add;
            msg.what = MsgType.MSG_UDISK_STATE;
            msg.arg1 = 1;
            mUIHandler.sendMessage(msg);
        }

    }

    @Override
    public void removeUsb(UsbDevice device_remove) {
        //正在复制、删除、重命名、创建时 U 盘被拔出:只请求中止,不在这里刷新或关闭设备
        if (copying) {
            usbHelper.cancelCopy();
            return;
        }
        if (deleting || renaming || creating) {
            //删除、重命名、创建也要中止
            return;
        }
//        updateUsbFile(0);
        usbHelper.closeUsbMassStorageDevice();
        usbFileList.clear();
        udiskAdapter.notifyDataSetChanged();
        usbButtomAction(false);
        isDetach = true;
    }

    @Override
    public void getReadUsbPermission(UsbDevice usbDevice) {

    }

    @Override
    public void failedReadUsb(UsbDevice usbDevice) {
        isDetach = true;
    }

    private static class UIHandler extends Handler {

        private final WeakReference<ResultExportActivity> mActivity;

        public UIHandler(ResultExportActivity activity) {
            mActivity = new WeakReference<ResultExportActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            if (null == mActivity.get()) {
                return;
            }
            switch (msg.what) {
                case MsgType.MSG_UDISK_STATE:
                    int state = msg.arg1;
                    Log.i("ResultExportActivity","udisk state" + state);
                    if(state == 1){
                        mActivity.get().isDetach = false;
                    }else {
                        mActivity.get().isDetach = true;
                    }
                    mActivity.get().updateUsbFile(0);
                    break;
                case MsgType.MSG_START_COPY_LOCAL:
                    Log.i("ResultExportActivity","MSG_START_COPY_LOCAL");
                    //复制进行中，请等待
                    mActivity.get().showFileProgressDialog(mActivity.get().getResources().getString(R.string.result_export_dialog_message_copying));
                    mActivity.get().localFileCopyToUsb();
                    break;
                case MsgType.MSG_END_COPY_LOCAL:
                    Log.i("ResultExportActivity","MSG_END_COPY_LOCAL");
                    mActivity.get().hideFileProgressDialog();
                    mActivity.get().showCopyResult(msg.obj);
                    //复制已结束,可以安全地重新读取 U 盘
                    mActivity.get().updateUsbFile(0);
                    break;
                case MsgType.MSG_COPY_PROGRESS:
                    int progress = (int)msg.arg1;
                    String name = (String) msg.obj;
                    if( mActivity.get().progressDialog != null &&  mActivity.get().progressDialog.isShowing()){
                        mActivity.get().progressDialog.setFileName(name);
                        mActivity.get().progressDialog.setProgress(progress);
                    }
                    break;
            }
        }
    }

    private void showNoticeDialog(String message,int dialogType){
        if(null != noticeDialog){
            noticeDialog.dismiss();
            noticeDialog =null;
        }
        if(null == noticeDialog){
            noticeDialog =new NoticeDialog(this,R.style.Dialog);
            if(dialogType == 1){
                noticeDialog.setMessage(message + "(" + checkUsbFiles.size()
                        +getResources().getString(R.string.result_export_dialog_message_items) + ")");
            }else if(dialogType == 2){
                noticeDialog.setMessage(message + "(" + checkLocalFiles.size()
                        +getResources().getString(R.string.result_export_dialog_message_items) + ")");
            }

            noticeDialog.setOnOkClickListener(new NoticeDialog.OnOkClickListener() {
                @Override
                public void onConfirmClick() {
                    noticeDialog.dismiss();
                    if(dialogType == 0){
                      deleteLocalFile();
                    }else if(dialogType == 1){
                        if(!isDetach){
                            deleteUsbFileAction();
                        }
                    }else if(dialogType == 2){
                        //复制本地文件去U盘
                        for(FileBean fileBean1 : localFileList){
                            fileBean1.setCheck(false);
                            fileBean1.setShow(false);
                        }
                        localAdapter.setFileList(localFileList);
                        localAdapter.notifyDataSetChanged();
                        localButtomAction(false);
                        isLocalCheckShow = false;
                        isLocalAllCheck = false;
                        if(null != mUIHandler){
                            mUIHandler.sendEmptyMessage(MsgType.MSG_START_COPY_LOCAL);
                        }
                    }
                }
            });

            noticeDialog.setOnCancelOnclickListener(new NoticeDialog.OnCancelClickListener() {
                @Override
                public void onCancelClick() {
                    noticeDialog.dismiss();
                }
            });
            noticeDialog.show();
        }
    }

    private void showCustomDialog(String title,UsbFile usbFile,int dialogType){
        if(null != customDialog){
            customDialog.dismiss();
            customDialog = null;
        }
        if(null == customDialog){
            customDialog = new CustomDialog(this);
            LayoutInflater mInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_export_template_layout, null);
            EditText editText = (EditText) view.findViewById(R.id.dialog_export_temp_name_et);
            if(usbFile != null){
                editText.setText(usbFile.getName());
            }else{
                if(dialogType == 2){
                    editText.setText(getResources().getString(R.string.result_export_dialog_file));
                }else if(dialogType == 3){
                    editText.setText(getResources().getString(R.string.result_export_dialog_directory));
                }
            }
            CustomDialog.Builder builder = new CustomDialog.Builder(this);
            builder.setContentView(view);
            builder.setTitle(title);
            builder.setPositiveButton(R.string.dialog_ok,  new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    String filename = editText.getText().toString();
                    if(StringUtil.isEmpty(filename)){
                        Toast.makeText(getApplicationContext()
                                ,getResources().getString(R.string.result_export_toast_file_name_null),Toast.LENGTH_LONG).show();
                        return;
                    }else if(isSameNameUsbFile(filename)){
                        Toast.makeText(getApplicationContext()
                                ,getResources().getString(R.string.result_export_toast_file_same_name),Toast.LENGTH_LONG).show();
                        return;
                    }
                    if(dialogType == 1){
                        renameUsbFile(filename,usbFile);
                    }else if(dialogType == 2){
                        //新建文件
                        createUsbFile(filename,false);
                    }else if(dialogType == 3){
                        //新建文件夹
                        createUsbFile(filename,true);
                    }

                    customDialog.dismiss();

                }
            });

            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    customDialog.dismiss();
                    customDialog = null;
                }
            });
            customDialog = builder.create();
            customDialog.show();
        }
    }

    private void showCreateDialog(String title){
        if(null != createDialog){
            createDialog.dismiss();
            createDialog = null;
        }
        if(null == createDialog){
            createDialog = new CustomDialog(this);
            LayoutInflater mInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_create_usbfile, null);
            TextView fileTv = (TextView)view.findViewById(R.id.create_file_tv);
            TextView directoryTv =  (TextView)view.findViewById(R.id.create_directory_tv);
            CustomDialog.Builder builder = new CustomDialog.Builder(this);
            builder.setContentView(view);
            builder.setTitle(title);
            fileTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    createDialog.dismiss();
                    createDialog = null;
                    //新建文件
                    showCustomDialog(getResources().getString(R.string.result_export_dialog_title_create_file)
                            ,null,2);
                }
            });

            directoryTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    createDialog.dismiss();
                    createDialog = null;
                    //新建文件夹
                    showCustomDialog(getResources().getString(R.string.result_export_dialog_title_create_folder)
                            ,null,3);
                }
            });

            createDialog = builder.create();
            createDialog.show();
        }
    }

    private ProgressDialog progressDialog;
    private void showFileProgressDialog(String title){
        if(null != progressDialog){
            progressDialog.dismiss();
            progressDialog = null;
        }
        if(progressDialog == null) {
            progressDialog = new ProgressDialog(this,title);
            progressDialog.setCanceledOnTouchOutside(true);
            progressDialog.setProgress(0);
            progressDialog.setOnCanlceListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //只请求中止,不在这里刷新:刷新会重新打开设备,和正在写的线程抢 USB 接口
                    usbHelper.cancelCopy();
                    progressDialog.dismiss();
                }
            });
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    usbHelper.cancelCopy();
                    progressDialog.dismiss();
                }
            });
            progressDialog.show();
        }
    }

    private void hideFileProgressDialog(){
        if(null != progressDialog){
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    /**
     * 复制结束后的提示。在主线程调用。
     *
     * @param result 失败原因(String),或复制过程(UsbCopySession)
     */
    private void showCopyResult(Object result) {
        String message;
        if (result instanceof String) {
            message = (String) result;
        } else if (result instanceof UsbCopySession) {
            UsbCopySession session = (UsbCopySession) result;
            if (session.isCancelled()) {
                message = getResources().getString(R.string.result_export_dialog_message_copy_cancelled);
            } else if (session.getFailedFiles().isEmpty()) {
                message = getResources().getString(R.string.result_export_dialog_message_copy_done,
                        countFiles(session.getDestDir()), session.getDestDir().getName());
            } else {
                message = getResources().getString(R.string.result_export_dialog_message_copy_partial,
                        countFiles(session.getDestDir()), session.getFailedFiles().size());
            }
        } else {
            message = getResources().getString(R.string.result_export_dialog_message_copy_failed);
        }
        showNoticeDialog(message, -1);
    }

    /** 统计一个 U 盘目录下(含子目录)的文件数,统计失败返回 0 */
    private int countFiles(UsbFile dir) {
        try {
            int count = 0;
            for (UsbFile file : dir.listFiles()) {
                count += file.isDirectory() ? countFiles(file) : 1;
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

}
