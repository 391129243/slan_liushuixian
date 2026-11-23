package com.gidi.bio_pcr.fragment.setting;

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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.activity.ResultExportActivity;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.config.FileBean;
import com.gidi.bio_pcr.config.UsbFileBean;
import com.gidi.bio_pcr.config.adapter.FilesListAdapter;
import com.gidi.bio_pcr.config.adapter.UsbFileAdapter;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.receiver.UsbBroadCastReceiver;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.LoadingDialog;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.util.UsbCopySession;
import com.gidi.bio_pcr.util.UsbHelper;
import com.gidi.bio_pcr.util.UsbToLocalCopySession;
import com.gidi.bio_pcr.view.CustomDialog;
import com.gidi.bio_pcr.view.DividerDecoration;
import com.gidi.bio_pcr.view.NoticeDialog;
import com.gidi.bio_pcr.view.ProgressDialog;
import com.gidi.bio_pcr.view.WrapContentLinearLayoutManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import me.jahnen.libaums.core.UsbMassStorageDevice;
import me.jahnen.libaums.core.fs.UsbFile;

/**
 * 将U盘的配置文件复制进PCR目录
 */
public class FileOperateFragment extends BaseFragment implements View.OnClickListener, UsbBroadCastReceiver.UsbListener{
    private final String PCR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator;
//            +"PCR";
    private String localCurrentPath = PCR_PATH;
    private ListView mLocalFileLv,mUdiskFileLv;
    private FilesListAdapter localAdapter;
    private UsbFileAdapter udiskAdapter;
    private List<FileBean> localFileList,checkLocalFiles;
    private List<UsbFileBean>usbFileList,checkUsbFiles;
    private UsbHelper usbHelper;
    private UIHandler mUIHandler;
    private TextView mLocalAllCheckTv,mLocalCancelTv,mLocalRefleshTv,mLocalDelTv,mLocalCreateTv;
    private TextView mLocalCopyTv,mLocalRenameTv;
    private TextView mUsbCancelTv,mUsbCreateTv,mUsbDelTv,mUsbRenameTv,mUsbCopyTv,mUsbAllCheckTv;
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
    //正在往 U 盘复制。复制期间不再响应插拔刷新,避免把正在写的连接打断
    private volatile boolean copying = false;
    //正在删除文件/文件夹。删除期间不再响应插拔刷新
    private volatile boolean deleting = false;
    //正在重命名文件/文件夹。重命名期间不再响应插拔刷新
    private volatile boolean renaming = false;
    //正在创建文件/文件夹。创建期间不再响应插拔刷新
    private volatile boolean creating = false;
    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_file_operate;
    }

    @Override
    protected void initView(View rootView) {

        mLocalBackTv = (TextView)rootView.findViewById(R.id.file_operate_local_file_back_tv);
        mUsbBackTv = (TextView)rootView.findViewById(R.id.file_operate_ufile_back_tv);
        mLocalFileLv= (ListView) rootView.findViewById(R.id.file_operate_local_file_lv);
        localAdapter = new FilesListAdapter(getActivity().getApplicationContext());
        localFileList = new CopyOnWriteArrayList<FileBean>();
        localAdapter.setFileList(localFileList);
        mLocalFileLv.setAdapter(localAdapter);

        mUdiskFileLv = (ListView)rootView.findViewById(R.id.file_operate_udisk_file_lv);
        udiskAdapter = new UsbFileAdapter(getActivity().getApplicationContext());
        usbFileList = new CopyOnWriteArrayList<UsbFileBean>();
        udiskAdapter.setFileList(usbFileList);
        mUdiskFileLv.setAdapter(udiskAdapter);


        mLocalCreateTv = (TextView)rootView.findViewById(R.id.file_operate_create_local_tv) ;
        mLocalAllCheckTv = (TextView)rootView.findViewById(R.id.file_operate_allcheck_local_tv);
        mLocalCancelTv = (TextView)rootView.findViewById(R.id.file_operate_cancel_local_tv);
        mLocalDelTv= (TextView)rootView.findViewById(R.id.file_operate_delete_local_tv);
        mLocalRefleshTv = (TextView)rootView.findViewById(R.id.file_operate_reflesh_local_tv);
        mLocalCopyTv = (TextView)rootView.findViewById(R.id.file_operate_copy_local_tv);

        mUsbCopyTv = (TextView)rootView.findViewById(R.id.file_operate_copy_usb_tv);
        mUsbAllCheckTv = (TextView)rootView.findViewById(R.id.file_operate_allcheck_usb_tv);
        mUsbCancelTv = (TextView)rootView.findViewById(R.id.file_operate_cancel_usb_tv);
        mUsbCreateTv = (TextView)rootView.findViewById(R.id.file_operate_create_usb_tv);
        mUsbDelTv= (TextView)rootView.findViewById(R.id.file_operate_delete_usb_tv);
        mUsbRenameTv= (TextView)rootView.findViewById(R.id.file_operate_rename_usb_tv);
        mUsbBackTv.setVisibility(View.INVISIBLE);
        mLocalBackTv.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void initListener() {
        mLocalCreateTv.setOnClickListener(this);
        mLocalCancelTv.setOnClickListener(this);
        mLocalAllCheckTv.setOnClickListener(this);
        mLocalDelTv.setOnClickListener(this);
        mLocalCopyTv.setOnClickListener(this);

        mUsbAllCheckTv.setOnClickListener(this);
        mUsbCancelTv.setOnClickListener(this);
        mUsbCreateTv.setOnClickListener(this);
        mUsbDelTv.setOnClickListener(this);
        mUsbRenameTv.setOnClickListener(this);
        mLocalBackTv.setOnClickListener(this);
        mUsbBackTv.setOnClickListener(this);
        mUsbCopyTv.setOnClickListener(this);
        initLocalFiles();
        initUsbFiles();

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
                    localAdapter.setSelectedPosition(position);
                    localAdapter.setFileList(localFileList);
                    localButtomAction(true);
                }

            }

            @Override
            public void onItemCheck(int position) {
                FileBean fileBean = localFileList.get(position);
                if(fileBean.isCheck()){
                    fileBean.setCheck(false);
                }else {
                    fileBean.setCheck(true);
                }
                if (fileBean.isCheck() && !checkLocalFiles.contains(fileBean)) {
                    checkLocalFiles.add(fileBean);
                } else if (!fileBean.isCheck() && checkLocalFiles.contains(fileBean)) {
                    checkLocalFiles.remove(fileBean);
                }
                localAdapter.setSelectedPosition(position);
                localAdapter.setFileList(localFileList);
            }
        });

    }

    protected void initData() {
        usbHelper = new UsbHelper(getActivity().getApplicationContext(), this);
        localFileList = new CopyOnWriteArrayList<FileBean>();
        checkLocalFiles = new CopyOnWriteArrayList<FileBean>();
        checkUsbFiles = new CopyOnWriteArrayList<UsbFileBean>();
        usbFileList = new CopyOnWriteArrayList<UsbFileBean>();
        mUIHandler = new UIHandler(this);
    }

    private void initLocalFiles(){
        localFileList.clear();
        File[] files = new File(PCR_PATH).listFiles();//文件夹下的所有文件
        if(files != null && files.length>0) {
            for (int i = 0; i < files.length; i++) {
                if (null != localFileList) {
                    /*if(files[i].getName().equals("config")||files[i].getName().equals("commonHex_v4")
                            || files[i].getName().equals("Template")
                            || files[i].getName().equals("commonHex_v5")
                            || files[i].getName().equals("file")
                            || files[i].getName().equals("image")
                            || files[i].getName().equals("result")
                        ){
                        localFileList.add(new FileBean(files[i],false,false));
                        Collections.sort(localFileList);
                    }*/
                    localFileList.add(new FileBean(files[i],false,false));
                    Collections.sort(localFileList);
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
                //关键修复：列表被清空/越界时直接忽略,避免 U 盘拔出/切换目录的空窗期点击导致崩溃
                if (position < 0 || position >= usbFileList.size()) {
                    return;
                }
                //点击打开u盘文件
                UsbFile usbFile = usbFileList.get(position).getUsbFile();
                if (usbFile == null) {
                    return;
                }
                if(usbFile.isDirectory()){
                    openUsbFile(usbFile);
                }else{
                    Toast.makeText(getActivity().getApplicationContext(),
                            getActivity().getResources().getString(R.string.result_export_toast_file_cannot_open),Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onItemLongClick(int position) {
                //关键修复：列表被清空/越界时直接忽略
                if (position < 0 || position >= usbFileList.size()) {
                    return;
                }
                //长按
                if(!isUsbCheckShow){
                    isUsbCheckShow = true;
                    checkUsbFiles.clear();
                    for(UsbFileBean bean : usbFileList){
                        bean.setShow(true);
                    }
                    UsbFileBean fileBean = usbFileList.get(position);
                    fileBean.setCheck(true);
                    checkUsbFiles.add(fileBean);
                    udiskAdapter.setFileList(usbFileList);
                    usbButtomAction(true);
                }
            }

            @Override
            public void onItemCheck(int position) {
                //关键修复：列表被清空/越界时直接忽略
                if (position < 0 || position >= usbFileList.size()) {
                    return;
                }
                UsbFileBean fileBean = usbFileList.get(position);
                if (fileBean == null) {
                    return;
                }
                if(fileBean.isCheck()){
                    fileBean.setCheck(false);
                }else {
                    fileBean.setCheck(true);
                }
                if (fileBean.isCheck() && !checkUsbFiles.contains(fileBean)) {
                    checkUsbFiles.add(fileBean);
                } else if (!fileBean.isCheck() && checkUsbFiles.contains(fileBean)) {
                    checkUsbFiles.remove(fileBean);
                }
                udiskAdapter.setFileList(usbFileList);
            }
        });
        updateUsbFile(0);
    }



    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(null != usbHelper){
            usbHelper.removeReceiver(this);
            usbHelper.finishUsbHelper();
//            usbHelper = null;
        }
        if(null != localFileList){
            localFileList.clear();
//            localFileList = null;
        }

        if(null != usbFileList){
            usbFileList.clear();
//            usbFileList = null;
        }
        if(null != checkLocalFiles){
            checkLocalFiles.clear();
//            checkLocalFiles = null;
        }
        if(null != checkUsbFiles){
            checkUsbFiles.clear();
//            checkUsbFiles = null;
        }

        if(null != mUIHandler){
            mUIHandler.removeCallbacksAndMessages(null);
//            mUIHandler = null;
        }
        if(null != noticeDialog){
            noticeDialog.dismiss();
//            noticeDialog = null;
        }
        if(null != customDialog){
            customDialog.dismiss();
//            customDialog = null;
        }
        hideLoadingDialog();
        hideProgressDialog();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.file_operate_rename_usb_tv){
            //重命名Usb的文件或文件夹
            if(checkUsbFiles !=null && checkUsbFiles.size()>0){
                if(checkUsbFiles.size()>1){
                    //不能批量重命名
                    Toast.makeText(getActivity().getApplicationContext(),
                            getActivity().getResources().getString(R.string.result_export_toast_batch_renaming),Toast.LENGTH_LONG).show();
                }else{
                    UsbFile usbFile = checkUsbFiles.get(0).getUsbFile();
                    showCustomDialog(getActivity().getResources().getString(R.string.result_export_rename),usbFile,1);
                }
            }
        }else if(id == R.id.file_operate_create_usb_tv){
            //新建Usb文件
            showCreateDialog(getActivity().getResources().getString(R.string.result_export_create),0);
        }else if(id == R.id.file_operate_cancel_usb_tv){
            //取消Usb文件处理
            cancelUsbCheck();
        }else if(id == R.id.file_operate_allcheck_usb_tv){
            //usb文件全选
            usbFileAllCheck();
        }else if(id == R.id.file_operate_delete_usb_tv){
            //删除Usb文件
            if(checkUsbFiles != null && checkUsbFiles.size()>0){
                ProgressDialog dialog = new ProgressDialog(getActivity(),
                        getActivity().getResources().getString(R.string.result_export_dialog_calculate_size));
                dialog.setCancelable(true);
                dialog.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        long length = getTotalLength();
                        if(null != getActivity()){
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                dialog.dismiss();
                                                if (length < 32 * 8 * 1024 * 1024L) {
                                                    showNoticeDialog(getActivity().getResources()
                                                            .getString(R.string.result_export_dialog_message_delete), 1);
                                                } else {
                                                    Toast.makeText(getActivity().getApplicationContext(),
                                                            getActivity().getResources().getString(R.string.result_export_toast_file_to_large), Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        });
                                    }
                                }
                            }).start();
                        }
                    }
                }).start();

//                long length = getTotalLength();
//                if(length < 32 * 1024 * 1024L){
//                    showNoticeDialog("确认要删除",1);
//                }else{
//                    Toast.makeText(getActivity().getApplicationContext(),"删除的文件太大,请减少删除数量",Toast.LENGTH_LONG).show();
//                }

            }else{
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.result_export_toast_delete_file),Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.file_operate_ufile_back_tv){
            usbFileBack();
        } else if(id == R.id.file_operate_delete_local_tv){
            //删除本地文件
            if(null != checkLocalFiles && checkLocalFiles.size()>0){
                if(checkLocalFiles.get(0).getFile().getParentFile().getAbsolutePath()
                        .equals(PCR_PATH)){
                    Toast.makeText(getActivity().getApplicationContext(),
                            getActivity().getResources().getString(R.string.result_export_toast_can_not_delete_root_directory),Toast.LENGTH_LONG).show();
                }else{
                    showNoticeDialog(getActivity().getResources().getString(R.string.result_export_dialog_delete_local_file),0);
                }
            }else{
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.result_export_toast_delete_file),Toast.LENGTH_LONG).show();
            }

        }else if(id == R.id.file_operate_reflesh_local_tv){
            //刷新本地文件
            refleshLocalFile();
        }else if(id == R.id.file_operate_cancel_local_tv){
            //取消本地选项
            cancelLocalCheck();
        }else if(id == R.id.file_operate_allcheck_local_tv){
            //全选
            localFileAllCheck();
        }else if(id == R.id.file_operate_local_file_back_tv){
            localFileBack();
        }else if(id == R.id.file_operate_delete_local_tv){
            if(checkLocalFiles != null && checkLocalFiles.size()>0){
                showNoticeDialog(getActivity().getResources().getString(R.string.result_export_dialog_message_delete),0);

            }else{
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.result_export_toast_delete_file),Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.file_operate_create_local_tv){
            showCreateDialog(getActivity().getResources().getString(R.string.result_export_create),1);
        }else if(id == R.id.file_operate_copy_usb_tv){
            if(checkUsbFiles != null && checkUsbFiles.size()>0){
                showNoticeDialog(getActivity().getResources().getString(R.string.result_export_dialog_message_copy),2);

            }else{
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.result_export_toast_copy_file),Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.file_operate_copy_local_tv){
            //复制本地
            isUsbAllCheck = false;
            isUsbCheckShow = false;
            usbButtomAction(false);
            if(checkLocalFiles != null && checkLocalFiles.size()>0){
                showNoticeDialog(getActivity().getResources().getString(R.string.result_export_dialog_message_copy),3);
            }else{
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.result_export_toast_copy_file),Toast.LENGTH_LONG).show();
            }
        }

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
        if(null != mUIHandler){
            Message msg = Message.obtain();
            msg.obj = device_remove;
            msg.what = MsgType.MSG_UDISK_STATE;
            msg.arg1 = 0;
            mUIHandler.sendMessage(msg);
        }
    }

    @Override
    public void getReadUsbPermission(UsbDevice usbDevice) {

    }

    @Override
    public void failedReadUsb(UsbDevice usbDevice) {

    }


    private void updateUsbFile(int position) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    //throw new RuntimeException(e);
                }
                //子线程只做耗时的 USB 设备读取,不再触碰 usbFileList / Adapter
                final ArrayList<UsbFile>[] allUsbFileRef = new ArrayList[]{null};
                final boolean[] usbExistsRef = {false};
                if (null != usbHelper) {
                    UsbMassStorageDevice[] usbMassStorageDevices = usbHelper.getDeviceList();
                    if (usbMassStorageDevices.length > 0) {
                        //存在USB
                        usbExistsRef[0] = true;
                        ArrayList<UsbFile> allUsbFile = usbHelper.readDevice(usbMassStorageDevices[position]);
                        //  Log.e("AAA", "Usb allUsbFile" + allUsbFile.size());

                        if (allUsbFile == null || allUsbFile.size() == 0) {
                            Log.e("FileOperateFragment", "usbMassStorageDevices[0] 空");
                        }
                        allUsbFileRef[0] = allUsbFile;
//                        Log.i("FileOperateFragment", usbMassStorageDevices[position].getUsbDevice().getDeviceName());
//                        Log.i("FileOperateFragment", usbMassStorageDevices[position].getUsbDevice().getSerialNumber());
                    } else {
                        Log.e("FileOperateFragment", "No Usb Device");
                    }
                }
                //统一在主线程上原子地完成 clear + add + notifyDataSetChanged
                //从源头消除"列表已清空但 Adapter 尚未刷新"的可点击窗口期
                if (getActivity() != null){
                    final boolean usbExists = usbExistsRef[0];
                    final ArrayList<UsbFile> allUsbFile = allUsbFileRef[0];
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            usbFileList.clear();
                            if (usbExists && allUsbFile != null) {
                                for (UsbFile usbFile : allUsbFile) {
                                    usbFileList.add(new UsbFileBean(usbFile, false, false));
                                    isDetach = false;
                                }
                            }
                            udiskAdapter.setFileList(usbFileList);
                            udiskAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        }).start();
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
     * 打开 USB File
     * @param usbFile USB File
     */
    private void openUsbFile(UsbFile usbFile) {
        if (usbFile.isDirectory()) {
            if(null != usbFile){
                if(usbFile.getName().equals("/")){
                    mUsbBackTv.setVisibility(View.INVISIBLE);
                }else {
                    mUsbBackTv.setVisibility(View.VISIBLE);
                }
            }
            //文件夹更新列表
            usbFileList.clear();
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
///////////////////////////////////////////////////U盘文件操作///////////////////////////////////////////////////
    /**
     * 获取U盘文件的大小
     * @return
     */
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


    private int copyUsbCount = 0;

    /**
     * 1-新建Usb文件
     */
    private void createUsbFile(String usbFileName, boolean isDirectory) {
        if (null != usbHelper) {
            creating = true;
            try {
                if (isDirectory) {
                    try {
                        UsbFile usbFile = usbHelper.getCurrentFolder();
                        if (usbFile == null) {
                            Toast.makeText(getActivity().getApplicationContext(),
                                    getActivity().getResources().getString(R.string.result_export_toast_create_foder_fail), Toast.LENGTH_LONG).show();
                            return;
                        }
                        UsbFile createDirectoty = usbFile.createDirectory(usbFileName);
                        if (null != createDirectoty) {
                            Toast.makeText(getActivity().getApplicationContext(),
                                    getActivity().getResources().getString(R.string.result_export_toast_create_foder_success),
                                    Toast.LENGTH_LONG).show();
                            updateUsbFile(0);
                        } else {
                            Toast.makeText(getActivity().getApplicationContext(),
                                    getActivity().getResources().getString(R.string.result_export_toast_create_foder_fail),
                                    Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e("FileOperateFragment", "create usb directory failed: " + e.getMessage());
                        Toast.makeText(getActivity().getApplicationContext(),
                                getActivity().getResources().getString(R.string.result_export_toast_create_foder_fail), Toast.LENGTH_LONG).show();
                    }
                } else {
                    try {
                        UsbFile usbFile = usbHelper.getCurrentFolder();
                        if (usbFile == null) {
                            Toast.makeText(getActivity().getApplicationContext(),
                                    getActivity().getResources().getString(R.string.result_export_toast_create_file_fail), Toast.LENGTH_LONG).show();
                            return;
                        }
                        UsbFile createFile = usbFile.createFile(usbFileName);
                        if (createFile != null) {
                            Toast.makeText(getActivity().getApplicationContext(),
                                    getActivity().getResources().getString(R.string.result_export_toast_create_file_success),
                                    Toast.LENGTH_LONG).show();
                            updateUsbFile(0);
                        } else {
                            Toast.makeText(getActivity().getApplicationContext(),
                                    getActivity().getResources().getString(R.string.result_export_toast_create_file_fail),
                                    Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e("FileOperateFragment", "create usb file failed: " + e.getMessage());
                        Toast.makeText(getActivity().getApplicationContext(),
                                getActivity().getResources().getString(R.string.result_export_toast_create_file_fail),
                                Toast.LENGTH_LONG).show();
                    }
                }
            } finally {
                creating = false;
            }
        }
    }

    /**
     * 2-复制U盘文件
     */

    private void copyUsbFileAction(){
        isUsbCheckShow = false;
        isUsbAllCheck = false;
        usbButtomAction(false);
        for(UsbFileBean usbFileBean : usbFileList){
            usbFileBean.setCheck(false);
            usbFileBean.setShow(false);
        }
        udiskAdapter.setFileList(usbFileList);
        udiskAdapter.notifyDataSetChanged();
        if(isDetach){
            Log.i("FileOperateFragment","111-取消" + isDetach);
            if(null != mUIHandler){
                Log.i("FileOperateFragment","111-取消");
                mUIHandler.sendEmptyMessage(MsgType.MSG_END_COPY_USB);
            }

        }else{
            if(null != mUIHandler){
                mUIHandler.sendEmptyMessage(MsgType.MSG_START_COPY_USB);
            }
        }

    }

    /**
     * U 盘文件复制到当前本地目录。
     * 本地有同名文件时覆盖(先写临时文件再替换,失败时原文件不变),同名目录合并;结果在结束时汇总提示。
     */
    private void copyUsbFileToLocal(){
        if (copying) {
            //上一次复制还没结束,不再开第二个线程去抢同一个 U 盘
            hideProgressDialog();
            Toast.makeText(getActivity().getApplicationContext(),
                    getResources().getString(R.string.result_export_dialog_message_copying_already), Toast.LENGTH_LONG).show();
            return;
        }
        if (checkUsbFiles == null || checkUsbFiles.size() == 0 || usbHelper == null || isDetach) {
            if (mUIHandler != null) {
                mUIHandler.sendEmptyMessage(MsgType.MSG_END_COPY_USB);
            }
            return;
        }
        //复制期间的插拔广播不再刷新列表,见 insertUsb/removeUsb
        copying = true;
        usbHelper.resetCopyCancelled();
        final List<UsbFile> sources = new ArrayList<>();
        for (UsbFileBean bean : checkUsbFiles) {
            if (bean.getUsbFile() != null) {
                sources.add(bean.getUsbFile());
            }
        }
        final File destDir = new File(localCurrentPath);
        copyThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //失败原因用资源 id 传回主线程再取字符串:后台线程里 fragment 可能已经 detach
                int failRes = 0;
                UsbToLocalCopySession session = null;
                try {
                    if (!destDir.isDirectory()) {
                        failRes = R.string.result_export_dialog_message_copy_to_local_failed;
                    } else {
                        long need = UsbToLocalCopySession.totalSizeOf(sources);
                        long free = destDir.getUsableSpace();
                        if (need < 0) {
                            //U 盘目录读不出来
                            failRes = R.string.result_export_dialog_message_copy_to_local_failed;
                        } else if (need + 8L * 1024 * 1024 > free) {
                            failRes = R.string.result_export_dialog_message_copy_to_local_no_space;
                        }
                    }
                    if (failRes == 0) {
                        session = new UsbToLocalCopySession(usbHelper, sources, destDir);
                        session.run(copyProgressListener);
                    }
                } catch (Exception e) {
                    Log.e("FileOperateFragment", "copy to local failed: " + e.getMessage());
                    failRes = R.string.result_export_dialog_message_copy_to_local_failed;
                    session = null;
                }
                copying = false;
                if (mUIHandler != null) {
                    Message msg = Message.obtain();
                    msg.what = MsgType.MSG_END_COPY_USB;
                    msg.obj = failRes == 0 ? session : Integer.valueOf(failRes);
                    mUIHandler.sendMessage(msg);
                }
            }
        });
        copyThread.start();
    }

    /**
     * 3-删除Usb文件夹/文件
     * @return
     */
    private void deleteUsbFileAction(){
        //删除后进行更新
        isUsbCheckShow = false;
        usbButtomAction(false);
        showLoadingDialog(getActivity().getResources().getString(R.string.result_export_dialog_message_deleting));
        new Thread(new Runnable() {
            @Override
            public void run() {
                deleting = true;
                try {
                    final boolean result = deleteCheckUsbFile();
                    if (getActivity() != null){
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (result) {
                                    hideLoadingDialog();
                                    try {
                                        openUsbFile(usbHelper.getCurrentFolder());
                                    } catch (Exception e) {
                                        Log.e("FileOperateFragment", "open usb folder after delete failed: " + e.getMessage());
                                    }
                                    checkUsbFiles.clear();
                                    Toast.makeText(getActivity().getApplicationContext(),
                                            getActivity().getResources().getString(R.string.result_export_toast_delete_success), Toast.LENGTH_SHORT).show();
                                } else {
                                    hideLoadingDialog();
                                    try {
                                        openUsbFile(usbHelper.getCurrentFolder());
                                    } catch (Exception e) {
                                        Log.e("FileOperateFragment", "open usb folder after delete failed: " + e.getMessage());
                                    }
                                    checkUsbFiles.clear();
                                    Toast.makeText(getActivity().getApplicationContext(),
                                            getActivity().getResources().getString(R.string.result_export_toast_delete_fail), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                } finally {
                    deleting = false;
                }
            }
        }).start();
    }

    /**
     * 3-删除选中的Usb文件
     * @return
     */
    private boolean deleteCheckUsbFile(){
        for(UsbFileBean usbFileBean : checkUsbFiles){
            UsbFile usbFile = usbFileBean.getUsbFile();
            boolean result = removeUsbFile(usbFile);
            if(!result){
                return false;
            }
        }
        return true;
    }

    private boolean removeUsbFile(UsbFile usbFile){
        try{
            usbFile.delete();
            return true;
        }catch (Exception e){
            e.getMessage();
            return false;
        }
    }

    /**
     * 4-U盘文件重命名
     * @param usbFileName
     */
    private void renameUsbFile(String usbFileName,UsbFile usbFile){
        try{
            usbFile.setName(usbFileName);
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.result_export_toast_rename_success),
                    Toast.LENGTH_LONG).show();
            updateUsbFile(0);
        }catch (Exception e){
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.result_export_toast_rename_fail),
                    Toast.LENGTH_LONG).show();
        }
        isUsbCheckShow = false;
        isUsbAllCheck = false;
        usbButtomAction(false);
    }



    /**
     * 5-取消Usb选项
     */
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

    /**
     * usb文件全选
     */
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
            for(UsbFileBean usbFileBean : usbFileList){
                usbFileBean.setCheck(true);
            }
            udiskAdapter.setFileList(usbFileList);
            checkUsbFiles.clear();
            checkUsbFiles.addAll(usbFileList);
        }
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





   /* private void copyUsbFileThread( final UsbFile file, final String savefilePath,final String saveName){

        new Thread(new Runnable() {
            @Override
            public void run() {
                //复制结果
                //1.先在Template文件夹中创建子文件

                //先创建文件夹再复制文件
                final boolean result = usbHelper.saveUSbFileToLocal(file, savefilePath, saveName,new UsbHelper.CopyProgressListener() {
                    @Override
                    public void copyProgress(final int progress) {
                        if(null != mUIHandler){
                            Message msg = Message.obtain();
                            msg.what = MSG_COPY_PROGRESS;
                            msg.obj = file.getName();
                            msg.arg1 = progress;
                            mUIHandler.sendMessage(msg);
                        }

                    }

                    @Override
                    public void copyResult(boolean result) {
                        if(result){
                            if(null != mUIHandler){
                                mUIHandler.sendEmptyMessage(MsgType.MSG_COPY_USB_SUCCESS);
                            }
                        }else{
                            if(null != mUIHandler){
                                mUIHandler.sendEmptyMessage(MSG_COPY_USB_FAIL);
                            }
                        }
                    }
                });

            }
        }).start();
    }*/
////////////////////////////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////////////本地文件操作//////////////////////////////////////////////////
    /**
     * 1.复制本地文件
     */
    private void copyLocalFileAction(){
        for(FileBean fileBean1 : localFileList){
            fileBean1.setCheck(false);
            fileBean1.setShow(false);
        }
        localAdapter.setFileList(localFileList);
        localAdapter.notifyDataSetChanged();
        localButtomAction(false);
        isLocalCheckShow = false;
        isLocalAllCheck = false;
        if(!isDetach){
            if(null != mUIHandler){
                mUIHandler.sendEmptyMessage(MsgType.MSG_START_COPY_LOCAL);
            }
        }else{
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.result_export_toast_udisk_no_inserted),Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 统计待复制内容的总字节数,用于复制前和 U 盘剩余空间比较。
     * 目录读不到时记为 -1,调用方据此放弃这次复制而不是照写。
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
     * 本地文件复制到 U 盘。
     * 写到 U 盘当前目录下新建的时间戳目录里,不删除 U 盘上已有的内容。
     */
    private void copyLocalFileToUsb(){
        if (copying) {
            //上一次复制还没结束,不再开第二个线程去抢同一个 U 盘
            hideProgressDialog();
            Toast.makeText(getActivity().getApplicationContext(),
                    getResources().getString(R.string.result_export_dialog_message_copying_already), Toast.LENGTH_LONG).show();
            return;
        }
        if (checkLocalFiles == null || checkLocalFiles.size() == 0 || usbHelper == null) {
            hideProgressDialog();
            Toast.makeText(getActivity().getApplicationContext(),
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
                    Log.e("FileOperateFragment", "copy to usb failed: " + e.getMessage());
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
     * 2-删除本地文件
     * @return
     */
    private void deleteLocalFileAction(){
        //根目录不能删除
        isLocalAllCheck = false;
        isLocalCheckShow = false;
        localButtomAction(false);
        showLoadingDialog(getActivity().getResources().getString(R.string.result_export_dialog_message_deleting));
        new Thread(new Runnable() {
            @Override
            public void run() {
               final boolean result = deleteLocalFile();
               Log.i("FileOperateFragment","deleteLocalFileAction--" + result);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (result) {
                                hideLoadingDialog();
                                openLocalFile(new File(localCurrentPath));
                                checkLocalFiles.clear();
                                Toast.makeText(getActivity().getApplicationContext(),
                                        getActivity().getResources().getString(R.string.result_export_toast_delete_success), Toast.LENGTH_SHORT).show();
                            } else {
                                hideLoadingDialog();
                                openLocalFile(new File(localCurrentPath));
                                checkLocalFiles.clear();
                                Toast.makeText(getActivity().getApplicationContext(),
                                        getActivity().getResources().getString(R.string.result_export_toast_delete_fail), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        }).start();
    }

    private boolean  deleteLocalFile(){
        for(FileBean fileBean :checkLocalFiles){
            File localFile = fileBean.getFile();
            boolean result = removeLocalFile(localFile);
            if(!result){
                return false;
            }
        }
        return true;
    }

    private boolean removeLocalFile(File del_local_file){
        try{
            if(del_local_file.isFile()){
                return del_local_file.delete();
            }else {
                return FileUtils.deleteDirByFile(del_local_file);
            }
        }catch (Exception e){
            return false;
        }
    }

    /**
     * 3-刷新本地文件
     */
    private void  refleshLocalFile(){
        //刷新当前path的列表
        openLocalFile(new File(localCurrentPath));

    }

    /**
     * 4-取消本地选项
     */
    private void cancelLocalCheck(){
        if(null != localFileList && localFileList.size()>0){
            for(FileBean fileBean : localFileList){
                fileBean.setCheck(false);
                fileBean.setShow(false);
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

    }

    /**
    *
     * 5-本地文件全选
     */
    private void localFileAllCheck(){
        if(isLocalAllCheck){
            isLocalAllCheck = false;
            for(FileBean fileBean : localFileList){
                fileBean.setCheck(false);
            }
            localAdapter.setFileList(localFileList);
            checkLocalFiles.clear();
        }else {
            isLocalAllCheck = true;
            for(FileBean fileBean : localFileList){
                fileBean.setCheck(true);
            }
            localAdapter.setFileList(localFileList);
            checkLocalFiles.clear();
            checkLocalFiles.addAll(localFileList);
        }
    }

    /**
     * 本地文件返回
     * @return
     */
    private void localFileBack(){
        File file = new File(localCurrentPath).getParentFile();
        if(file != null){
            openLocalFile(new File(localCurrentPath).getParentFile());
        }
        isLocalCheckShow = false;
        isLocalAllCheck = false;
        localButtomAction(false);
    }

    /**
     * 创建本地文件夹
     * @param fileName
     * @return
     */
    private void createLocalDir(String fileName){
        String path = localCurrentPath + File.separator + fileName;
        File file = new File(path);
        if(file.exists()){
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.result_export_toast_directory_exist),Toast.LENGTH_LONG).show();
        }else {
            file.mkdirs();
        }
        openLocalFile(new File(localCurrentPath));
    }

    private void localButtomAction(boolean isEnable){
        if(isEnable){

            mLocalCopyTv.setEnabled(true);
            Drawable copyImg = getResources().getDrawable(R.drawable.icon_copy_enanable);
            copyImg.setBounds(0,0,copyImg.getMinimumWidth(),copyImg.getMinimumHeight());
            mLocalCopyTv.setCompoundDrawables(null,copyImg,null,null);
            mLocalCopyTv.setTextColor(getResources().getColor(R.color.deep_gray));

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

            mLocalRefleshTv.setEnabled(false);
            Drawable refleshImg = getResources().getDrawable(R.drawable.icon_reflesh_no);
            refleshImg.setBounds(0,0,refleshImg.getMinimumWidth(),refleshImg.getMinimumHeight());
            mLocalRefleshTv.setCompoundDrawables(null,refleshImg,null,null);
            mLocalRefleshTv.setTextColor(getResources().getColor(R.color.light_gray));

            mLocalDelTv.setEnabled(true);
            Drawable deleteImg = getResources().getDrawable(R.drawable.icon_delete_enable);
            deleteImg.setBounds(0,0,deleteImg.getMinimumWidth(),deleteImg.getMinimumHeight());
            mLocalDelTv.setCompoundDrawables(null,deleteImg,null,null);
            mLocalDelTv.setTextColor(getResources().getColor(R.color.deep_gray));

            mLocalCreateTv.setEnabled(false);
            Drawable createImg = getResources().getDrawable(R.drawable.icon_create_no);
            createImg.setBounds(0,0,createImg.getMinimumWidth(),createImg.getMinimumHeight());
            mLocalCreateTv.setCompoundDrawables(null,createImg,null,null);
            mLocalCreateTv.setTextColor(getResources().getColor(R.color.light_gray));
        }else{

            mLocalCopyTv.setEnabled(false);
            Drawable copyImg = getResources().getDrawable(R.drawable.icon_copy_file);
            copyImg.setBounds(0,0,copyImg.getMinimumWidth(),copyImg.getMinimumHeight());
            mLocalCopyTv.setCompoundDrawables(null,copyImg,null,null);
            mLocalCopyTv.setTextColor(getResources().getColor(R.color.light_gray));

            mLocalCreateTv.setEnabled(true);
            Drawable createImg = getResources().getDrawable(R.drawable.icon_create_enable);
            createImg.setBounds(0,0,createImg.getMinimumWidth(),createImg.getMinimumHeight());
            mLocalCreateTv.setCompoundDrawables(null,createImg,null,null);
            mLocalCreateTv.setTextColor(getResources().getColor(R.color.deep_gray));

            mLocalAllCheckTv.setEnabled(false);
            Drawable allCkImg = getResources().getDrawable(R.drawable.icon_allcheck_no);
            allCkImg.setBounds(0,0,allCkImg.getMinimumWidth(),allCkImg.getMinimumHeight());
            mLocalAllCheckTv.setCompoundDrawables(null,allCkImg,null,null);
            mLocalAllCheckTv.setTextColor(getResources().getColor(R.color.light_gray));

            mLocalRefleshTv.setEnabled(true);
            Drawable refleshImg = getResources().getDrawable(R.drawable.icon_reflesh_enable);
            refleshImg.setBounds(0,0,refleshImg.getMinimumWidth(),refleshImg.getMinimumHeight());
            mLocalRefleshTv.setCompoundDrawables(null,refleshImg,null,null);
            mLocalRefleshTv.setTextColor(getResources().getColor(R.color.deep_gray));

            mLocalCancelTv.setEnabled(false);
            Drawable cancelImg = getResources().getDrawable(R.drawable.icon_cancel_no);
            cancelImg.setBounds(0,0,cancelImg.getMinimumWidth(),cancelImg.getMinimumHeight());
            mLocalCancelTv.setCompoundDrawables(null,cancelImg,null,null);
            mLocalCancelTv.setTextColor(getResources().getColor(R.color.light_gray));

            mLocalDelTv.setEnabled(false);
            Drawable deleteImg = getResources().getDrawable(R.drawable.icon_delete_no);
            deleteImg.setBounds(0,0,deleteImg.getMinimumWidth(),deleteImg.getMinimumHeight());
            mLocalDelTv.setCompoundDrawables(null,deleteImg,null,null);
            mLocalDelTv.setTextColor(getResources().getColor(R.color.light_gray));
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
            renameImg.setBounds(0,0,renameImg.getMinimumWidth(),renameImg.getMinimumHeight());
            mUsbRenameTv.setCompoundDrawables(null,renameImg,null,null);
            mUsbRenameTv.setTextColor(getResources().getColor(R.color.deep_gray));

            mUsbCopyTv.setEnabled(true);
            Drawable copyImg = getResources().getDrawable(R.drawable.icon_copy_enanable);
            copyImg.setBounds(0,0,copyImg.getMinimumWidth(),copyImg.getMinimumHeight());
            mUsbCopyTv.setCompoundDrawables(null,copyImg,null,null);
            mUsbCopyTv.setTextColor(getResources().getColor(R.color.deep_gray));

        }else{

            mUsbCopyTv.setEnabled(false);
            Drawable copyImg = getResources().getDrawable(R.drawable.icon_copy_file);
            copyImg.setBounds(0,0,copyImg.getMinimumWidth(),copyImg.getMinimumHeight());
            mUsbCopyTv.setCompoundDrawables(null,copyImg,null,null);
            mUsbCopyTv.setTextColor(getResources().getColor(R.color.detect_gray));

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
            mUsbCreateTv.setTextColor(getResources().getColor(R.color.light_gray));

            mUsbDelTv.setEnabled(false);
            Drawable deleteImg = getResources().getDrawable(R.drawable.icon_delete_no);
            deleteImg.setBounds(0,0,deleteImg.getMinimumWidth(),deleteImg.getMinimumHeight());
            mUsbDelTv.setCompoundDrawables(null,deleteImg,null,null);
            mUsbDelTv.setTextColor(getResources().getColor(R.color.light_gray));

            mUsbRenameTv.setEnabled(false);
            Drawable renameImg = getResources().getDrawable(R.drawable.icon_rename_no);
            renameImg.setBounds(0,0,renameImg.getMinimumWidth(),renameImg.getMinimumHeight());
            mUsbRenameTv.setCompoundDrawables(null,renameImg,null,null);
            mUsbRenameTv.setTextColor(getResources().getColor(R.color.light_gray));

        }

    }



    /**
     *
     * @param title
     * @param usbFile
     * @param dialogType
     */
    private void showCustomDialog(String title,UsbFile usbFile,int dialogType){
        if(null != customDialog){
            customDialog.dismiss();
            customDialog = null;
        }
        if(null == customDialog){
            customDialog = new CustomDialog(getActivity());
            LayoutInflater mInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_export_template_layout, null);
            EditText editText = (EditText) view.findViewById(R.id.dialog_export_temp_name_et);
            if(usbFile != null){
                editText.setText(usbFile.getName());
            }else{
                if(dialogType == 2){
                    editText.setText(getActivity().getResources().getString(R.string.result_export_dialog_file));
                }else if(dialogType == 3){
                    editText.setText(getActivity().getResources().getString(R.string.result_export_dialog_directory));
                }else if(dialogType == 4){
                    editText.setText(getActivity().getResources().getString(R.string.result_export_dialog_directory));
                }
            }
            CustomDialog.Builder builder = new CustomDialog.Builder(getActivity());
            builder.setContentView(view);
            builder.setTitle(title);
            builder.setPositiveButton(R.string.dialog_ok,  new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    String filename = editText.getText().toString();
                    if(StringUtil.isEmpty(filename)){
                        Toast.makeText(getActivity().getApplicationContext(),
                                getActivity().getResources().getString(R.string.judge_ct_toast_file_name),Toast.LENGTH_LONG).show();
                        return;
                    }else if(isSameNameUsbFile(filename)){
                        Toast.makeText(getActivity().getApplicationContext(),
                                getActivity().getResources().getString(R.string.result_export_toast_same_name_file),Toast.LENGTH_LONG).show();
                        return;
                    }
                    if(dialogType == 1){
                        renameUsbFile(filename,usbFile);
                    }else if(dialogType == 2){
                        //新建文件
                        createUsbFile(filename,false);
                    }else if(dialogType == 3){
                        //新建usb文件夹
                        createUsbFile(filename,true);
                    }else if(dialogType == 4){
                        createLocalDir(filename);
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

    /**
     *
     * @param title
     * @param dialogType 0:属于新建usb文件，1新建本地文件夹
     */
    private void showCreateDialog(String title,int dialogType){
        if(null != createDialog){
            createDialog.dismiss();
            createDialog = null;
        }
        if(null == createDialog){
            createDialog = new CustomDialog(getActivity());
            LayoutInflater mInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_create_usbfile, null);
            TextView fileTv = (TextView)view.findViewById(R.id.create_file_tv);
            TextView directoryTv =  (TextView)view.findViewById(R.id.create_directory_tv);
            if(dialogType == 1){
                fileTv.setVisibility(View.INVISIBLE);
            }else{
                fileTv.setVisibility(View.VISIBLE);
            }
            CustomDialog.Builder builder = new CustomDialog.Builder(getActivity());
            builder.setContentView(view);
            builder.setTitle(title);
            fileTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    createDialog.dismiss();
                    createDialog = null;
                    showCustomDialog(getActivity().getResources().getString(R.string.result_export_dialog_title_create_file)
                            ,null,2);
                }
            });

            directoryTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    createDialog.dismiss();
                    createDialog = null;
                    if(dialogType == 0){
                        //usb
                        showCustomDialog(getActivity().getResources().getString(R.string.result_export_dialog_title_create_folder),null,3);
                    }else{
                        showCustomDialog(getActivity().getResources().getString(R.string.result_export_dialog_title_create_folder),null,4);
                    }

                }
            });

            createDialog = builder.create();
            createDialog.setCanceledOnTouchOutside(true);
            createDialog.show();
        }
    }

    /**
     *
     * @param message
     * @param dialogType
     * 0:删除本地的文件
     * 1:删除Usb文件
     * 2:复制U盘文件
     * 3:复制本地文件到U盘
     */
    private void showNoticeDialog(String message,int dialogType){
        if(null != noticeDialog){
            noticeDialog.dismiss();
            noticeDialog =null;
        }
        if(null == noticeDialog){
            noticeDialog =new NoticeDialog(getActivity(),R.style.Dialog);
            if (dialogType == 0){
                noticeDialog.setMessage(message + "(" + checkLocalFiles.size()
                        +getActivity().getResources().getString(R.string.result_export_dialog_message_items)
                        + ")");
            }else if(dialogType == 1){
                noticeDialog.setMessage(message + "(" + checkUsbFiles.size()
                        +getActivity().getResources().getString(R.string.result_export_dialog_message_items)
                        + ")");
            }else if(dialogType == 2){
                noticeDialog.setMessage(message + "(" + checkUsbFiles.size()
                        +getActivity().getResources().getString(R.string.result_export_dialog_message_items)
                        + ")");
            }else if(dialogType ==3){
                noticeDialog.setMessage(message + "(" + checkLocalFiles.size()
                        +getActivity().getResources().getString(R.string.result_export_dialog_message_items)
                        + ")");
            }
            noticeDialog.setOnOkClickListener(new NoticeDialog.OnOkClickListener() {
                @Override
                public void onConfirmClick() {
                    noticeDialog.dismiss();
                    if(dialogType == 0){
                        deleteLocalFileAction();
                    }else if(dialogType == 1){
                        if(!isDetach){
                            deleteUsbFileAction();
                        }else {
                            Toast.makeText(getActivity().getApplicationContext(),
                                    getActivity().getResources().getString(R.string.result_export_toast_udisk_no_inserted),Toast.LENGTH_LONG).show();
                        }
                    }else if(dialogType == 2){
                        copyUsbFileAction();
                    }else if(dialogType == 3){
                        copyLocalFileAction();
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

            // 复制完成提示（dialogType == -1）只显示确定按钮，隐藏取消按钮
            // 必须在 show() 之后调用，确保控件已初始化
            if (dialogType == -1) {
                noticeDialog.setCancelButtonVisible(false);
            }
        }
    }

    private LoadingDialog loadingDialog;
    public void showLoadingDialog(String message){
        if(null == loadingDialog){
            loadingDialog = new LoadingDialog(getActivity(), message);
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

    private void hideProgressDialog(){
        if(null != progressDialog && progressDialog.isShowing()){
            progressDialog.dismiss();
            progressDialog = null;
        }
    }



    /////////////////////


    private int suc_count = 0;
    private static class UIHandler extends Handler {
        private final WeakReference<FileOperateFragment> mFragReference;

        public UIHandler(FileOperateFragment fragment) {
            mFragReference = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            // 只获取一次，复用局部变量
            FileOperateFragment fragment = mFragReference.get();
            // 统一生命周期校验：页面销毁/脱离Activity直接退出，不再执行任何逻辑
            if (fragment == null || fragment.isDetached() || !fragment.isAdded() || fragment.getActivity() == null) {
                return;
            }

            switch (msg.what) {
                case MsgType.MSG_UDISK_STATE:
                    int state = msg.arg1;
                    if (state == 0) {
                        fragment.isDetach = true;
                    } else if (state == 1) {
                        fragment.isDetach = false;
                    }
                    fragment.updateUsbFile(0);
                    fragment.usbButtomAction(false);
                    break;

                case MsgType.MSG_START_COPY_LOCAL:
                    String copyTip = fragment.getResources().getString(R.string.result_export_dialog_message_copying);
                    fragment.showProgressDialog(copyTip);
                    fragment.copyLocalFileToUsb();
                    break;

                case MsgType.MSG_COPY_PROGRESS:
                    int progress = msg.arg1;
                    String name = (String) msg.obj;
                    if (fragment.progressDialog != null && fragment.progressDialog.isShowing()) {
                        fragment.progressDialog.setFileName(name);
                        fragment.progressDialog.setProgress(progress);
                    }
                    break;

                case MsgType.MSG_START_COPY_USB:
                    String usbCopyTip = fragment.getResources().getString(R.string.result_export_dialog_message_copying);
                    fragment.showProgressDialog(usbCopyTip);
                    fragment.copyUsbFileToLocal();
                    break;

                case MsgType.MSG_END_COPY_USB:
                    fragment.suc_count = 0;
                    fragment.copyUsbCount = 0;
                    fragment.checkUsbFiles.clear();
                    fragment.hideProgressDialog();
                    //obj 为空表示没有真正开始复制(没选文件、U 盘已拔出),不提示
                    if (msg.obj != null) {
                        fragment.showUsbToLocalResult(msg.obj);
                    }
                    fragment.openLocalFile(new File(fragment.localCurrentPath));
                    break;

                case MsgType.MSG_COPY_USB_SUCCESS:
                    fragment.suc_count++;
                    Log.i("FileOperateFragment", "复制U盘至本地文件数量成功" + fragment.suc_count);
                    if (fragment.suc_count == fragment.copyUsbCount) {
                        Log.i("FileOperateFragment", "复制成功 suc_count" + fragment.suc_count);
                        if (fragment.mUIHandler != null) {
                            fragment.mUIHandler.sendEmptyMessageDelayed(MsgType.MSG_END_COPY_USB, 2000);
                        }
                    }
                    break;

                case MsgType.MSG_COPY_USB_FAIL:
                    Log.i("FileOperateFragment", "复制Usb至本地 文件失败");
                    Log.i("FileOperateFragment", "复制Usb至本地数量失败--" + fragment.suc_count);
                    Log.i("FileOperateFragment", "复制Usb至本地数量--" + fragment.copyUsbCount);
                    fragment.hideProgressDialog();
                    fragment.interruptCopyThread = true;
                    fragment.copyThread = null;
                    String failUsbLocal = fragment.getResources().getString(R.string.result_export_dialog_message_copy_to_local_failed);
                    fragment.showNoticeDialog(failUsbLocal, -1);
                    fragment.suc_count = 0;
                    fragment.copyUsbCount = 0;
                    fragment.checkUsbFiles.clear();
                    fragment.openLocalFile(new File(fragment.localCurrentPath));
                    break;

                case MsgType.MSG_END_COPY_USB_FAIL:
                    Log.i("FileOperateFragment", "结束复制Usb至本地 文件失败");
                    Log.i("FileOperateFragment", "结束复制Usb至本地数量失败--" + fragment.suc_count);
                    Log.i("FileOperateFragment", "结束复制Usb至本地数量--" + fragment.copyUsbCount);
                    fragment.hideProgressDialog();
                    fragment.interruptCopyThread = true;
                    fragment.copyThread = null;
                    String endFailUsbLocal = fragment.getResources().getString(R.string.result_export_dialog_message_copy_to_local_failed);
                    fragment.showNoticeDialog(endFailUsbLocal, -1);
                    fragment.suc_count = 0;
                    fragment.copyUsbCount = 0;
                    fragment.checkUsbFiles.clear();
                    fragment.openLocalFile(new File(fragment.localCurrentPath));
                    break;

                case MsgType.MSG_END_COPY_LOCAL:
                    //和原来一样,复制结束后清空本次的选择
                    fragment.checkLocalFiles.clear();
                    fragment.hideProgressDialog();
                    fragment.showCopyResult(msg.obj);
                    //复制已结束,可以安全地重新读取 U 盘
                    fragment.openUsbFile(fragment.usbHelper.getCurrentFolder());
                    break;
            }
        }
    }
    private ProgressDialog progressDialog;
    private void showProgressDialog(String title){
        hideProgressDialog();
        if(progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity(),title);
            progressDialog.setCanceledOnTouchOutside(true);
            progressDialog.setProgress(0);
            progressDialog.setOnCanlceListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //只请求中止,不在这里刷新:刷新会重新打开设备,和正在写的线程抢 USB 接口
                    //进度框两个方向共用:cancelCopy 管本地→U盘,interruptCopyThread 管U盘→本地
                    usbHelper.cancelCopy();
                    interruptCopyThread = true;
                    progressDialog.dismiss();
                }
            });
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    usbHelper.cancelCopy();
                    interruptCopyThread = true;
                    progressDialog.dismiss();
                }
            });
            progressDialog.show();
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

    /**
     * U 盘复制到本地结束后的提示。在主线程调用。
     *
     * @param result 失败原因的字符串资源 id(Integer),或复制过程(UsbToLocalCopySession)
     */
    private void showUsbToLocalResult(Object result) {
        String message;
        if (result instanceof Integer) {
            message = getResources().getString((Integer) result);
        } else if (result instanceof UsbToLocalCopySession) {
            UsbToLocalCopySession session = (UsbToLocalCopySession) result;
            if (session.isCancelled()) {
                message = getResources().getString(R.string.result_export_dialog_message_copy_cancelled);
            } else if (session.getFailedFiles().isEmpty()) {
                message = getResources().getString(R.string.result_export_dialog_message_copy_to_local_done,
                        session.getCopiedCount(), session.getDestDir().getName());
            } else {
                message = getResources().getString(R.string.result_export_dialog_message_copy_to_local_partial,
                        session.getCopiedCount(), session.getFailedFiles().size());
            }
        } else {
            message = getResources().getString(R.string.result_export_dialog_message_copy_to_local_failed);
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
