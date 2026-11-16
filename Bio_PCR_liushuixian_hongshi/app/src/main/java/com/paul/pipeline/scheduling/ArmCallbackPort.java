package com.paul.pipeline.scheduling;

/**
 * 机械臂命令回调引擎（ArmCallbackEngine）与外部世界之间的端口。
 * <p>
 * ArmCallbackEngine 只依赖本接口 + 纯 POJO（ArmItemSnapshot）+ ConstantData 常量，
 * 绝不 import databinding / android / Manager，从而可在纯 JVM 下单测。
 * APLManager 为每次 startArmTask* 调用新建一个实现（AplArmCallbackPort），
 * 闭包捕获当前 ArmworkMessage + armErrorCallback，把这些回调 1:1 适配到现有 Manager，行为保持不变。
 */
public interface ArmCallbackPort {

    /** Get.finish 守卫：目标设备 armStep 是否为 GET（读 model.armStep==ArmStep.GET）。 */
    boolean deviceArmStepIsGet(int deviceType, int index);

    /** 置目标设备 runtime.gettrayfinish = true。 */
    void setDeviceGettrayfinish(int deviceType, int index);

    /** 启动对应设备模板：createRnaRunTemplate / createPcrRunTemplate。 */
    void createRunTemplate(int deviceType, int index);

    /** 送回托盘完成：RNAManager/PCRManager.finishBacktray(srcPos, aimPos)。 */
    void finishBacktray(int deviceType, int srcPos, int aimPos);

    /** 移出托盘完成：RNAManager/PCRManager.finishMoveouttray(srcPos, aimPos)。 */
    void finishMoveouttray(int deviceType, int srcPos, int aimPos);

    /** 更新调度项的下一步目标设备：TrayManager.updateDsiapatchNext(tray, dispatchIndex, deviceType, aimPos)。 */
    void updateDispatchNext(int trayIndex, int dispatchIndex, int deviceType, int aimPos);

    /** 更新调度项状态：TrayManager.updateDsiapatchStatus(trayIndex, dispatchIndex, status)。 */
    void updateDispatchStatus(int trayIndex, int dispatchIndex, int status);

    /** 回传机械臂错误：msg.setArmerrortext(text) + armErrorCallback.error(msg)。 */
    void notifyArmError(String errorText);

    /** 日志钩子：level 复用 android.util.Log 常量，实现里转真 Log，测试里 no-op。 */
    void log(int level, String tag, String msg);
}
