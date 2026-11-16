package com.paul.pipeline.fragment;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.databinding.FragmentUpdateLinuxBinding;
import com.paul.pipeline.adapter.ConnectListAdapter;
import com.paul.pipeline.adapter.DevicesListAdapter;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.fragment.model.PipePCRModel;
import com.paul.pipeline.fragment.model.PipeRNAModel;
import com.paul.pipeline.fragment.viewmodel.UpdateLinuxViewModel;
import com.paul.pipeline.manager.APLManager;
import com.paul.pipeline.manager.PCRManager;
import com.paul.pipeline.manager.RNAManager;
import com.paul.pipeline.model.LinuxDeviceModel;
import com.winters.boweilibrary.autopipeline.mgr.WSSModuleManager;
import com.winters.boweilibrary.autopipeline.model.item.PCRStatusItem;
import com.winters.boweilibrary.autopipeline.model.item.RNAStatusItem;
import com.winters.boweilibrary.autopipeline.model.item.WSSConnectionItem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class UpdateLinuxFragment extends Fragment {
    final static String TAG = "UpdateLinuxFragment";
    private UpdateLinuxViewModel mViewModel;

    public static UpdateLinuxFragment newInstance() {
        return new UpdateLinuxFragment();
    }

    FragmentUpdateLinuxBinding binding;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        //return inflater.inflate(R.layout.fragment_update_linux, container, false);
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_update_linux, container, false);
        initBinding();
        return binding.getRoot();
    }

    private void initBinding() {
        binding.btnSelectfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });
        binding.btnUpdatelinux.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateLinuxWork();
            }
        });
        binding.btnUpdatestop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelLinuxWork();
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(UpdateLinuxViewModel.class);
        if(mViewModel==null){
            mViewModel = new UpdateLinuxViewModel();
            Log.e(TAG, "new mViewModel");
        }
        targetList.clear();
//        refreshDevicelist();
        initVM();
        refresh();
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUIHandler();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        removeUIHandler();
    }
    Timer timer;
    TimerTask timerTask;
    private void initUIHandler() {
        handler = new UIHandler();
        initTimer();
    }
    private void initTimer(){
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                // 在这里执行你的后台任务
                try{
                    if(handler!=null) {
                        handler.sendEmptyMessage(MSG_REFRESH);
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
                //  Log.i(TAG, "执行主页刷新");
            }
        };
        timer.schedule(timerTask, 1000L, 1000L);
    }
    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }
    private void removeUIHandler(){
        stopTimer();
        if (null != handler) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }
    final static int MSG_REFRESH = 1;
    UIHandler handler;
    class UIHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MSG_REFRESH:
                    refreshDevicelist();
                    refreshWork();
                    refresh();
                    break;
            }
        }
    }
    private void refresh(){
        binding.setUlvm(mViewModel);
        binding.executePendingBindings();
    }
    DevicesListAdapter devicesListAdapter;
    private void initVM(){
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL,false);
        //1.设置LinearLayoutManager ListView
        binding.devicesRv.setLayoutManager(layoutManager);
        binding.devicesRv.setHasFixedSize(true);
        //3.设置Adapter
        devicesListAdapter = new DevicesListAdapter(getContext(), mViewModel.devicelist.getValue(),
                (view, position) -> checkDeviceSelected(position));
        binding.devicesRv.setAdapter(devicesListAdapter);
        devicesListAdapter.notifyDataSetChanged();
    }
    private void refreshDevicelist(){
        if(mViewModel!=null){
//            Log.e(TAG, "  "+ mViewModel.fileversion.getValue());
            mViewModel.devicelist.getValue().clear();
            ConcurrentHashMap<String, WSSConnectionItem> items = WSSModuleManager.getInstance().getWssConnectionItems();
            if (items != null) {
                for (String key : items.keySet()) {
                    WSSConnectionItem item = items.get(key);
                    LinuxDeviceModel model = new LinuxDeviceModel();
                    model.ipport.setValue(key);
                    model.index.setValue(item.getPosition());
                    model.position.setValue(item.getPosition()+"");
                    model.type.setValue(item.getDeviceType());
                    model.deviceid.setValue(item.getDeviceID());
                    model.version.setValue(item.getVersion());
                    if (item instanceof PCRStatusItem) {
                        PCRStatusItem statusItem = (PCRStatusItem) item;
                    } else if(item instanceof RNAStatusItem){
                        RNAStatusItem statusItem = (RNAStatusItem) item;
                    }else{
                        Log.e(TAG, "TemplateWSSMessage: " + key + " is not PCR/RNA device");
                    }
                    model.selected.setValue(false);
                    if(targetList!=null) {
                        Log.e(TAG, "targetList  "+ targetList.size());
                        for (int i = 0; i < targetList.size(); i++) {
                            LinuxDeviceModel tm = targetList.get(i);
                            if(tm!=null){
                                if (model.deviceid.getValue() != null && tm.deviceid.getValue()!=null && model.deviceid.getValue().equalsIgnoreCase(tm.deviceid.getValue())) {
                                    model.selected.setValue(true);
                                    Log.e(TAG, "tm  "+ tm.deviceid.getValue() +"  "+ tm.version.getValue());
                                    if (tm.updatestatus.getValue() != null && tm.updatestatus.getValue() == UP_START) {
                                        Log.e(TAG, "model "+model.deviceid.getValue() +"  "+ model.version.getValue());
                                        if (model.version.getValue() != null && mViewModel.fileversion.getValue() != null) {
                                            boolean comp = model.version.getValue().equalsIgnoreCase(mViewModel.fileversion.getValue());
                                            if (comp) {
                                                Log.e(TAG, model.deviceid.getValue() +"  equalsIgnoreCase ok");
                                                tm.updatestatus.setValue(UP_FINISH);
                                                tm.result.setValue("已升级");
                                                mViewModel.filealert.setValue(model.deviceid.getValue()+" 已升级！");
                                            }else{
                                                Log.e(TAG, model.deviceid.getValue() +"  equalsIgnoreCase fail");
                                            }
                                        }
                                    }
                                    model.updatestatus.setValue(tm.updatestatus.getValue());
                                    model.result.setValue(tm.result.getValue());
                                    break;
                                }

                            }
                        }
                    }
                    mViewModel.devicelist.getValue().add(model);
                }
            }
            if(devicesListAdapter!=null) {
                devicesListAdapter.notifyDataSetChanged();
            }
        }
    }
    private boolean checkDeviceSelected(LinuxDeviceModel ldm){
        ArrayList<LinuxDeviceModel> list = mViewModel.devicelist.getValue();
        if(ldm.selected.getValue()) {
            if(ldm.type==null && mViewModel.filetype==null|| !ldm.type.getValue().equalsIgnoreCase(mViewModel.filetype.getValue())){
                mViewModel.filealert.setValue("文件类型与设备类型不同！");
                Toast.makeText(getContext(), "文件类型与设备类型不同！", Toast.LENGTH_LONG).show();
                refresh();
                return false;
            }
            if (targetList.size() > 0) {
                if (!targetList.get(0).type.getValue().equalsIgnoreCase(ldm.type.getValue())) {
                    mViewModel.filealert.setValue("必须选择同类型设备！");
                    Toast.makeText(getContext(), "必须选择同类型设备！", Toast.LENGTH_LONG).show();
                    for(int i=0;i<list.size();i++) {
                        LinuxDeviceModel model = list.get(i);
                        if(model.position.getValue()==ldm.position.getValue()&&model.type.getValue().equalsIgnoreCase(ldm.type.getValue())) {
                            model.selected.setValue(!ldm.selected.getValue());
                            Toast.makeText(getContext(), "取消选择:"+ldm.deviceid.getValue(), Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }
                    refresh();
                    return false;
                }
            }
        }
        for(int i=0;i<list.size();i++) {
            LinuxDeviceModel model = list.get(i);
            if(model.deviceid.getValue()!=null&&ldm.deviceid.getValue()!=null&&model.deviceid.getValue()==ldm.deviceid.getValue()) {
                model.selected.setValue(ldm.selected.getValue());
                // Toast.makeText(getContext(), "选择:"+ldm.deviceid.getValue(), Toast.LENGTH_SHORT).show();
                break;
            }
        }
        if(ldm.selected.getValue()) {
            boolean ishas = false;
            for (int i = 0; i < targetList.size(); i++) {
                LinuxDeviceModel model = targetList.get(i);
                if(model.deviceid.getValue()!=null&&ldm.deviceid.getValue()!=null&&model.deviceid.getValue().equalsIgnoreCase(ldm.deviceid.getValue())) {
                    ishas = true;
                    break;
                }
            }
            if(!ishas){
                targetList.add(ldm);
            }
        }else{
            for (int i = 0; i < targetList.size(); i++) {
                LinuxDeviceModel model = targetList.get(i);
                if(model.deviceid.getValue()!=null&&ldm.deviceid.getValue()!=null&&model.deviceid.getValue().equalsIgnoreCase(ldm.deviceid.getValue())) {
                    targetList.remove(model);
                    break;
                }
            }
        }
        String selecteddev = "";
        for(LinuxDeviceModel mo: targetList){
            if(mo.selected.getValue()){
                selecteddev+=" "+ mo.deviceid.getValue();
            }
        }
        mViewModel.filealert.setValue(selecteddev);
        refresh();
        return true;
    }
    ArrayList<LinuxDeviceModel> targetList = new ArrayList<>();

    private void openFileChooser() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*"); // 选择所有类型的文件
            startActivityForResult(Intent.createChooser(intent, "请选择升级文件！"), FILE_SELECT_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，可以安全地继续操作了。
            } else {
                // 权限被拒绝，处理这个情况，例如提示用户手动开启权限。
                Log.e(TAG, "权限被拒绝");
            }
        }
    }
    final static int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 112;
    final static int FILE_SELECT_CODE = 111;
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECT_CODE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            // 处理文件URI，例如读取文件内容等
            try {
                if(uri!=null) {
                    getFileInfo(uri);
                }
//                InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
                // 使用inputStream读取文件内容等操作
//                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void getFileInfo(Uri uri){
        String path = getFilePathByUri(getContext(),uri);
        String filename = getFileNameFromContentUri(getContext(),uri);
        //filename  = "rna_app_rna_01_00_18_20251009.tar.gz";
        mViewModel.filepath.setValue(path);
        mViewModel.filename.setValue(filename);
        String version = "";
        String upversion = "";
        if(filename.contains(".tar.gz")){
            if(filename.contains("rna_app")) {
                mViewModel.filetype.setValue("RNA");
                upversion = filename.replace("rna_app_", "").replace(".tar.gz", "");
                String temp = upversion.replace("rna_","");
                version = dropLastnumber(temp);
            }else if(filename.contains("pcr_app")){
                mViewModel.filetype.setValue("PCR");
                upversion = filename.replace("pcr_app_", "").replace(".tar.gz", "");
                String temp = upversion.replace("pcr_","");
                version = dropLastnumber(temp);
            }else{
                mViewModel.filetype.setValue("");
                mViewModel.filealert.setValue("未知类型，文件名不符合要求！");
            }
        }else{
            Log.e(TAG, "updateRna filename 不合规则:"+filename);
            mViewModel.filealert.setValue("文件后缀不符合要求!");
        }
        mViewModel.fileversion.setValue(version);
        mViewModel.updateversion.setValue(upversion);
        refresh();
    }
    public String dropLastnumber(String text){
        String result = text;
        if(text.contains("_")) {
            int lastindex = text.lastIndexOf("_");
            result = text.substring(0,lastindex);
        }
        return result;
    }
    public String getFileNameFromContentUri(Context context, Uri contentUri) {
        String fileName = null;
        String[] projection = { MediaStore.MediaColumns.DISPLAY_NAME };
        try (Cursor cursor = context.getContentResolver().query(contentUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                fileName = cursor.getString(nameIndex);
            }
        }
        return fileName;
    }
    public String getFilePathByUri(Context context, Uri uri) {
        String path = null;
        // 以 file:// 开头的
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            path = uri.getPath();
            return path;
        }
        // 以 content:// 开头的，比如 content://media/extenral/images/media/17766
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    if (columnIndex > -1) {
                        path = cursor.getString(columnIndex);
                    }
                }
                cursor.close();
            }
            return path;
        }
        // 4.4及之后的 是以 content:// 开头的，比如 content://com.android.providers.media.documents/document/image%3A235700
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                if (isExternalStorageDocument(uri)) {
                    // ExternalStorageProvider
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        path = Environment.getExternalStorageDirectory() + "/" + split[1];
                        return path;
                    }
                } else if (isDownloadsDocument(uri)) {
                    // DownloadsProvider
                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                            Long.valueOf(id));
                    path = getDataColumn(context, contentUri, null, null);
                    return path;
                } else if (isMediaDocument(uri)) {
                    // MediaProvider
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{split[1]};
                    path = getDataColumn(context, contentUri, selection, selectionArgs);
                    return path;
                }
            }
        }
        return null;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }


    private void updateLinuxWork() {
        if(mViewModel.filepath.getValue()!=null){
            File upfile = new File(mViewModel.filepath.getValue());
            if(!upfile.isFile() || !upfile.exists()){
                mViewModel.filealert.setValue("请选择升级文件！");
                refresh();
                return;
            }
        }else{
            mViewModel.filealert.setValue("请选择升级文件！");
            refresh();
            return;
        }
        if(targetList.size()==0){
            mViewModel.filealert.setValue("请选择设备！");
            refresh();
            return;
        }
        mViewModel.updatestatus.setValue(UP_START);
        mViewModel.updateall.setValue(targetList.size()+"");
        mViewModel.updatefinish.setValue(0+"");
        mViewModel.filealert.setValue(getString(R.string.file_updating));
        if(mViewModel!=null){
            if(targetList!=null&&targetList.size()>0){
                for(int i=0;i<targetList.size();i++) {
                    LinuxDeviceModel model = targetList.get(i);
                    if (model.selected.getValue()) {
                        if(updateDevice(model.type.getValue(),model.index.getValue(),mViewModel.updateversion.getValue(),mViewModel.filepath.getValue())){
                            model.updatestatus.setValue(UP_START);
                            model.result.setValue("升级中");
                        }else{
                            model.updatestatus.setValue(UP_ERROR);
                            model.result.setValue("发送失败");
                        }
                    }
                }
            }
        }
    }
    private void cancelLinuxWork() {
        mViewModel.updatestatus.setValue(UP_FREE);
        mViewModel.updateall.setValue("");
        mViewModel.updatefinish.setValue("");
        mViewModel.filealert.setValue("已清空！");
        if(targetList!=null){
            targetList.clear();
        }
    }
    final static int UP_FREE = 0;//0free 1start 2working 3finish
    final static int UP_START = 1;
    final static int UP_FINISH = 2;
    final static int UP_ERROR = -1;
    private void refreshWork(){
        if(mViewModel!=null){
            if(mViewModel.updatestatus.getValue() == UP_START){
                int countfinish = 0;
//                int countfail = 0;
                for(int j=0;j<targetList.size();j++){
                    LinuxDeviceModel model = targetList.get(j);
                    switch(model.updatestatus.getValue()){
                        case UP_FREE:
                            break;
                        case UP_START:
                            break;
                        case UP_FINISH:
                            countfinish++;
                            break;
                        case UP_ERROR:
//                            countfail++;
                            break;
                    }
                }
                mViewModel.updatefinish.setValue(countfinish+"");
//                mViewModel.updatefail.setValue(countfail+"");
//                if(countfinish+countfail>=targetList.size()) {
                if(countfinish>=targetList.size()) {
                    mViewModel.updatestatus.setValue(UP_FINISH);
                    mViewModel.filealert.setValue("所选设备升级完成！");
                }
            }
        }
    }
    private boolean updateDevice(String type,int index,String version,String filepath){
        boolean result = false;
        if(type.equalsIgnoreCase(ConstantData.nameRNA)){
            String key = APLManager.getInstance().getKey(ConstantData.DEVICE_RNA,index);
            result = RNAManager.getInstance().updateRna(key,version,filepath);
        }else if(type.equalsIgnoreCase(ConstantData.namePCR)){
            String key = APLManager.getInstance().getKey(ConstantData.DEVICE_PCR,index);
            result = PCRManager.getInstance().updatePcr(key,version,filepath);
        }
        return  result;
    }
}