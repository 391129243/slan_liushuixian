package com.paul.pipeline.scheduling;

import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.model.ArmItemSnapshot;

/**
 * databinding-free 机械臂命令回调引擎。
 * <p>
 * 从 APLManager 三个 startArmTask*（Get/Back/Move）的匿名 {@code ArmFinishCallback} 里抽出的
 * 纯路由决策 + 写回逻辑：只依赖 {@link ArmCallbackPort} + 纯 POJO {@link ArmItemSnapshot} + ConstantData 常量，
 * 可在纯 JVM 下单测。所有读/写/副作用（置标志、启模板、回收托盘、改调度状态、回传错误）均经 port 委托。
 * <p>
 * 行为与原匿名回调逐行等价，特别保留以下承重细节（单测锁定，禁止归一化）：
 * <ul>
 *   <li>Get.finish 用 equalsIgnoreCase 比较 type；Back/Move 用精确 switch（照搬原实现）。</li>
 *   <li>Back/Move.finish 里 updateDispatchStatus(FINISH) 在 null 检查之前无条件执行。</li>
 *   <li>三个 fail 写回顺序不一致：Get/Back 先 notifyArmError 再 updateDispatchStatus(ERROR)；
 *       Move 先 updateDispatchStatus(ERROR) 再 notifyArmError。</li>
 * </ul>
 */
public class ArmCallbackEngine {

    private static final String TAG = "ArmTaskWorker";
    // 对齐 android.util.Log 的级别常量，避免 import android
    private static final int ERROR = 6;

    private final ArmCallbackPort port;

    public ArmCallbackEngine(ArmCallbackPort port) {
        this.port = port;
    }

    // ================= 取托盘 Get =================

    /**
     * 取托盘完成：托盘送达目标 RNA/PCR 后，若该设备 armStep==GET 则置 gettrayfinish 并启动模板。
     * type 比较用 equalsIgnoreCase（照搬原实现）。
     */
    public void onGetFinish(ArmItemSnapshot aim, ArmItemSnapshot source) {
        if (source != null && aim != null) {
            port.log(ERROR, TAG, "机械臂回调 取托盘 source：" + source.type + source.position
                    + " 已送达目标位： " + aim.type + aim.position);
            if (ConstantData.nameRNA.equalsIgnoreCase(aim.type)) {
                if (port.deviceArmStepIsGet(ConstantData.DEVICE_RNA, aim.position)) {
                    port.setDeviceGettrayfinish(ConstantData.DEVICE_RNA, aim.position);
                    port.createRunTemplate(ConstantData.DEVICE_RNA, aim.position); // 临时措施，启动 rna 模板
                }
            } else if (ConstantData.namePCR.equalsIgnoreCase(aim.type)) {
                if (port.deviceArmStepIsGet(ConstantData.DEVICE_PCR, aim.position)) {
                    port.setDeviceGettrayfinish(ConstantData.DEVICE_PCR, aim.position);
                    port.createRunTemplate(ConstantData.DEVICE_PCR, aim.position); // 临时措施，启动 pcr 模板
                }
            }
        } else {
            port.log(ERROR, TAG, "机械臂回调数据错误 startArmTaskGetTray finish null");
        }
    }

    /**
     * 取托盘失败：回传错误；若 source==sourceitem.position 则把该调度项标 ERROR。
     * 顺序：先 notifyArmError，再 updateDispatchStatus(ERROR)。
     */
    public void onGetFail(ArmItemSnapshot source, int sourceIndex, int dispatchIndex, String error) {
        port.notifyArmError(error);
        if (source != null) {
            if (sourceIndex == source.position) {
                port.updateDispatchStatus(sourceIndex, dispatchIndex, ConstantData.STATUS_ERROR);
            } else {
                port.log(ERROR, TAG, "机械臂回调数据不一致 startArmTaskBackTray fail source tray:"
                        + sourceIndex + " sourceitem " + source.position);
            }
        } else {
            port.log(ERROR, TAG, "机械臂回调数据错误 startArmTaskGetTray fail null");
        }
    }

    // ================= 送回托盘 Back =================

    /**
     * 送回托盘完成：先无条件标该调度项 FINISH；再按源设备类型回收（finishBacktray）。
     */
    public void onBackFinish(ArmItemSnapshot aim, ArmItemSnapshot source, int target, int dispatchIndex) {
        port.updateDispatchStatus(target, dispatchIndex, ConstantData.STATUS_FINISH);
        if (source != null && aim != null) {
            port.log(ERROR, TAG, "机械臂 送回托盘 source:" + source.position + " sourcetype:" + source.type
                    + " target:" + aim.position + " targettype:" + aim.type);
            switch (source.type) {
                case ConstantData.nameRNA:
                    port.finishBacktray(ConstantData.DEVICE_RNA, source.position, aim.position);
                    break;
                case ConstantData.namePCR:
                    port.finishBacktray(ConstantData.DEVICE_PCR, source.position, aim.position);
                    break;
                case ConstantData.nameTray:
                    break;
            }
        } else {
            port.log(ERROR, TAG, "机械臂回调数据错误 startArmTaskBackTray finish null");
        }
    }

    /**
     * 送回托盘失败：先 notifyArmError；若 target==aimitem.position 则标该调度项 ERROR。
     */
    public void onBackFail(ArmItemSnapshot aim, int target, int dispatchIndex, String error) {
        port.notifyArmError(error);
        if (aim != null) {
            if (target == aim.position) {
                port.updateDispatchStatus(target, dispatchIndex, ConstantData.STATUS_ERROR);
            } else {
                port.log(ERROR, TAG, "机械臂回调数据不一致 startArmTaskBackTray fail target tray:"
                        + target + " aimitem" + aim.position);
            }
        } else {
            port.log(ERROR, TAG, "机械臂回调数据错误 startArmTaskBackTray fail null");
        }
    }

    // ================= 移动托盘 Move =================

    /**
     * 移动托盘完成：先无条件标该调度项 FINISH；源设备移出(finishMoveouttray)，
     * 目标设备更新下一步调度(updateDispatchNext)并启动模板。两个 switch 顺序照搬。
     */
    public void onMoveFinish(ArmItemSnapshot aim, ArmItemSnapshot source, int trayIndex, int dispatchIndex) {
        port.updateDispatchStatus(trayIndex, dispatchIndex, ConstantData.STATUS_FINISH);
        if (source != null && aim != null) {
            port.log(ERROR, TAG, "机械臂移动托盘 source:" + source.position + " sourcetype:" + source.type
                    + " target:" + aim.position + " targettype:" + aim.type);
            switch (source.type) {
                case ConstantData.nameRNA:
                    port.finishMoveouttray(ConstantData.DEVICE_RNA, source.position, aim.position);
                    break;
                case ConstantData.namePCR:
                    port.finishMoveouttray(ConstantData.DEVICE_PCR, source.position, aim.position);
                    break;
                case ConstantData.nameTray:
                    break;
            }
            switch (aim.type) {
                case ConstantData.nameRNA:
                    port.updateDispatchNext(trayIndex, dispatchIndex, ConstantData.DEVICE_RNA, aim.position);
                    port.createRunTemplate(ConstantData.DEVICE_RNA, aim.position); // 临时措施，启动 rna 模板
                    break;
                case ConstantData.namePCR:
                    port.updateDispatchNext(trayIndex, dispatchIndex, ConstantData.DEVICE_PCR, aim.position);
                    port.createRunTemplate(ConstantData.DEVICE_PCR, aim.position); // 临时措施，启动 pcr 模板
                    break;
                case ConstantData.nameTray:
                    break;
            }
        } else {
            port.log(ERROR, TAG, "机械臂回调数据错误 startArmTaskMoveTray fail null");
        }
    }

    /**
     * 移动托盘失败：先标该调度项 ERROR，再 notifyArmError（与 Get/Back 相反，刻意保留）。
     */
    public void onMoveFail(int trayIndex, int dispatchIndex, String error) {
        port.updateDispatchStatus(trayIndex, dispatchIndex, ConstantData.STATUS_ERROR);
        port.notifyArmError(error);
    }
}
