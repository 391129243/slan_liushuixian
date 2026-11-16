package com.gzlab.autoassemblyline_client;

import android.app.Activity;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.gzlab.autoassemblyline_client.autopipeline.event.EnumMessageEvent;
import com.gzlab.autoassemblyline_client.autopipeline.event.WSSMessageEvent;
import com.gzlab.autoassemblyline_client.autopipeline.message.QueryDeviceMessage;
import com.gzlab.autoassemblyline_client.autopipeline.message.SLAN_PCRFinishMessage;
import com.gzlab.autoassemblyline_client.autopipeline.mgr.WSSModuleManager;
import com.gzlab.autoassemblyline_client.autopipeline.utils.BinWssMessageUtils;
import com.gzlab.autoassemblyline_client.event.MsgType;
import com.gzlab.autoassemblyline_client.event.PCRTemplateInfoUpdateEvent;
import com.gzlab.autoassemblyline_client.event.SLANDebugStatusEvent;
import com.gzlab.autoassemblyline_client.event.WSSStatusEvent;
import com.gzlab.autoassemblyline_client.util.FileUtils;
import com.gzlab.autoassemblyline_client.util.PermissionHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.nio.ByteBuffer;

public class MainActivity extends Activity {

    private TextView deviceIdTv;
    private TextView devicePosTv;
    private TextView pcrTemplateNameTv;
    private TextView pcrProjectNameTv;
    private TextView pcrResultPathTv;

    // 调试状态显示控件
    private TextView debugConnectionStatus;
    private TextView debugRunningStatus;
    private TextView debugExperimentInfo;
    private TextView debugAlarmInfo;
    private TextView debugErrorCount;
    private TextView debugGuardStatus;
    private TextView debugFilePaths;
    private TextView debugWssStatus;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mudele_new_main_layout);
        // 启动时自动申请存储、定位、USB 等权限
        PermissionHelper.requestAllPermissions(this);

        // 注册 EventBus
        EventBus.getDefault().register(this);

        Button sendBtn = (Button) findViewById(R.id.send_pcr_file_btn);
        deviceIdTv = (TextView)findViewById(R.id.device_id_tv) ;
        deviceIdTv.setText(PcrApplication.deviceId+"");
        devicePosTv = (TextView)findViewById(R.id.device_pos_tv);
        devicePosTv.setText(PcrApplication.devicePos+"");

        // 显示版本信息
        TextView versionTv = (TextView)findViewById(R.id.version_tv);
        String versionName = com.gzlab.autoassemblyline_client.util.CommonUtil.getAppVersionName(this);
        versionTv.setText("v" + versionName);

        // 初始化 PCR 信息显示控件
        pcrTemplateNameTv = findViewById(R.id.pcr_template_name_tv);
        pcrProjectNameTv = findViewById(R.id.pcr_project_name_tv);
        pcrResultPathTv = findViewById(R.id.pcr_result_path_tv);

        // 初始化调试状态显示控件
        debugConnectionStatus = findViewById(R.id.debug_connection_status);
        debugRunningStatus = findViewById(R.id.debug_running_status);
        debugExperimentInfo = findViewById(R.id.debug_experiment_info);
        debugAlarmInfo = findViewById(R.id.debug_alarm_info);
        debugErrorCount = findViewById(R.id.debug_error_count);
        debugGuardStatus = findViewById(R.id.debug_guard_status);
        debugFilePaths = findViewById(R.id.debug_file_paths);
        debugWssStatus = findViewById(R.id.debug_wss_status);

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SLAN_PCRFinishMessage slan_pcrFinishMsg = new SLAN_PCRFinishMessage();
                slan_pcrFinishMsg.setTemplate("templateName");
                slan_pcrFinishMsg.setStatus("Finish");
                slan_pcrFinishMsg.setAlarm("");
                slan_pcrFinishMsg.setCurrentSection("0");
                slan_pcrFinishMsg.setCurrentStep("0");
                slan_pcrFinishMsg.setCurrentCycle(0);
                slan_pcrFinishMsg.setRunningTime(0);
                slan_pcrFinishMsg.setLeftTime(0);
                slan_pcrFinishMsg.setWaitingTime(0);
                slan_pcrFinishMsg.setTotalTime(0);
                slan_pcrFinishMsg.setDegree(39);
                slan_pcrFinishMsg.setAlarm("");
                slan_pcrFinishMsg.setInfo("");
                slan_pcrFinishMsg.setType(EnumMessageEvent.WSS_PCR_SLAN_Finish.getType());
                slan_pcrFinishMsg.setDeviceID(PcrApplication.deviceId);
                slan_pcrFinishMsg.setPosition(PcrApplication.devicePos);
                slan_pcrFinishMsg.setDeviceType("PCR");
                slan_pcrFinishMsg.setSeqID(10);

                String experiment_filePath = FileUtils.RESULT_PATH+"2025-11-07-11-25-35_13~新冠"+"/"+"2025-11-07-11-56-13.pcr";

                ByteBuffer experiment_byteBuffer = BinWssMessageUtils.packetBinMessage(slan_pcrFinishMsg,experiment_filePath);
                Log.i("WSSClient","发送实验结束的信息和文件" );
                WSSModuleManager.getInstance(getApplicationContext()).sendFinishResultWithAck(experiment_byteBuffer);
            }
        });

      //  Intent service = new Intent(this, WebSocketService.class);
       // startService(service);

        // 处理 App 已在前台时的 USB 插入事件
        handleUsbAttached(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // singleTask 模式下 USB 插入会走 onNewIntent
        handleUsbAttached(intent);
    }

    /**
     * 处理 USB 设备插入：对未授权的设备补一次权限申请。
     * device_filter.xml 命中的设备系统会自动授权，这里兜底覆盖未命中的场景。
     */
    private void handleUsbAttached(Intent intent) {
        if (intent == null
                || !UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
            return;
        }
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device == null) {
            return;
        }
        UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
        Log.i("MainActivity", "USB attached: " + device.getDeviceName());
        com.gzlab.autoassemblyline_client.util.UsbPermissionManager
                .requestPermission(this, usbManager, device);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销 EventBus
        EventBus.getDefault().unregister(this);
    }

    /**
     * 接收 PCR 模板信息更新事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPCRTemplateInfoUpdate(PCRTemplateInfoUpdateEvent event) {
        if (event == null) {
            return;
        }

        // 更新界面显示
        if (pcrTemplateNameTv != null) {
            pcrTemplateNameTv.setText(event.getTemplateName());
        }
        if (pcrProjectNameTv != null) {
            pcrProjectNameTv.setText(event.getProjectName());
        }
        if (pcrResultPathTv != null) {
            pcrResultPathTv.setText(event.getResultPath());
        }

        Log.i("MainActivity", "更新PCR信息显示 - 模板:" + event.getTemplateName()
                + " 文件:" + event.getProjectName()
                + " 路径:" + event.getResultPath());
    }

    /**
     * 接收 SLAN 调试状态更新事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSLANDebugStatusUpdate(SLANDebugStatusEvent event) {
        if (event == null) {
            return;
        }

        // 更新连接状态
        if (debugConnectionStatus != null) {
            String connStatus = String.format("连接: %s | 设备: %s",
                    event.isPcrConnected ? "✅已连接" : "❌未连接",
                    event.deviceState);
            debugConnectionStatus.setText(connStatus);
        }

        // 更新运行状态
        if (debugRunningStatus != null) {
            String runStatus = String.format("运行: %s | 状态: %s | Linux: %s",
                    event.isRunning ? "▶️运行中" : "⏸️空闲",
                    event.status,
                    event.linuxStatus);
            debugRunningStatus.setText(runStatus);
        }

        // 更新当前实验
        if (debugExperimentInfo != null) {
            String expInfo = String.format("模板: %s | 文件: %s",
                    TextUtils.isEmpty(event.pcrTemplateName) ? "--" : event.pcrTemplateName,
                    TextUtils.isEmpty(event.runningPcrName) ? "--" : event.runningPcrName);
            debugExperimentInfo.setText(expInfo);
        }

        // 更新告警信息
        if (debugAlarmInfo != null) {
            String alarmInfo = String.format("告警: %s | Linux告警: %s",
                    event.alarm,
                    event.linuxAlarm);
            debugAlarmInfo.setText(alarmInfo);
        }

        // 更新错误计数
        if (debugErrorCount != null) {
            String errorInfo = String.format("SLAN错误: %d | 重连次数: %d",
                    event.slanErrorCount,
                    event.reconnectCount);
            debugErrorCount.setText(errorInfo);
        }

        // 更新守卫状态
        if (debugGuardStatus != null) {
            String guardInfo = String.format("已上报: %s | 全局token: %d | 当前会话: %d",
                    event.finishReported ? "是" : "否",
                    event.finishSessionToken,
                    event.currentSessionToken);
            debugGuardStatus.setText(guardInfo);
        }

        // 更新文件路径
        if (debugFilePaths != null) {
            String pathInfo = String.format("模板目录: %s\n结果文件: %s",
                    TextUtils.isEmpty(event.currentTemplateDirPath) ? "--" : event.currentTemplateDirPath,
                    TextUtils.isEmpty(event.experimentFilePath) ? "--" : event.experimentFilePath);
            debugFilePaths.setText(pathInfo);
        }
    }

    /**
     * 接收主控 WSS 连接状态（WSSModuleManager 检查器每 2 秒发布）
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWSSStatusUpdate(WSSStatusEvent event) {
        if (event == null || debugWssStatus == null) {
            return;
        }
        String state;
        if (event.connected) {
            state = "✅已连接";
        } else if (event.reconnecting) {
            state = "🔄重连中";
        } else {
            state = "❌已断开，" + (event.nextRetryMs / 1000) + "s 后重连";
        }
        debugWssStatus.setText("WSS: " + state + " | " + event.serverUri);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(WSSMessageEvent messageEvent) {
        if (messageEvent == null) {
            return;
        }
        EnumMessageEvent eme = messageEvent.getMsgType();

        switch (eme) {
            case WSSManager_PCR_QUERYDEVICE:
                QueryDeviceMessage queryDeviceMessage = (QueryDeviceMessage) messageEvent.getData();
                deviceIdTv.setText(PcrApplication.deviceId+"");
                devicePosTv.setText(PcrApplication.devicePos+"");
                break;
        }
    }
}
