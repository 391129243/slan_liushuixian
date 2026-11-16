package com.paul.pipeline.scheduling;

import com.paul.pipeline.model.PcrSchedulingState;
import com.paul.pipeline.model.RnaSchedulingState;
import com.paul.pipeline.model.TrayDispatchSnapshot;

/**
 * 调度引擎与外部世界（Manager 单例、门状态、机械臂、UI 回调、日志）之间的端口。
 * <p>
 * SchedulingEngine 只依赖本接口 + 纯 POJO/枚举，绝不 import databinding 或 Manager，
 * 从而可在纯 JVM 下单测。APLManager 提供本接口的具体实现（AplSchedulingPort），
 * 把这些调用适配到现有 TrayManager/RNAManager/PCRManager/ArmManager，行为保持不变。
 */
public interface SchedulingPort {

    // ---- 门控读 ----
    /** @return 是否处于开门保持态（getPipeDoorModel().doorhold） */
    boolean isDoorHold();
    /** @return WSS 资源是否就绪（APLManager.WSSStart） */
    boolean isWssStart();
    /** @return 自检是否完成（APLManager.SelfCheck） */
    boolean isSelfCheck();
    /** @return 机械臂是否可用（ArmManager.armCanuse） */
    boolean isArmCanuse();

    // ---- 查询 ----
    /** 当前托盘对应的调度序号；getCurrentDispatch 为 null 时返回 -1。 */
    int currentDispatchIndex(int currentTray);

    /** 空闲 PCR 序号（>0）；无空闲时返回 -1。 */
    int findFreePcrIndex();

    /** 空闲 RNA 序号（>0）；无空闲时返回 -1（供 PCR MOVE 使用）。 */
    int findFreeRnaIndex();

    // ---- 命令（副作用留在实现里）----
    /** usererror 清零 + uIinitCallback.finish(DEVICE_RNA, rnaIndex) + 日志。 */
    void fireUiInit(int rnaIndex);

    /** startRunTemplate(rnaIndex, DEVICE_RNA, tray, dispatchIndex)。 */
    boolean startRunTemplateRna(int rnaIndex, int tray, int dispatchIndex);

    /** 依据当前托盘解析调度并 createArmTaskGetTray。 */
    boolean createGetTray(int currentTray);

    /** createArmMoveTray(rnaIndex, DEVICE_RNA, pcrIndex, DEVICE_PCR, tray, dispatchIndex)。 */
    boolean createMoveTray(int rnaIndex, int pcrIndex, int tray, int dispatchIndex);

    /** updateDsiapatchDeviceStatus(tray, dispatchIndex, STATUS_FINISH)。 */
    void markDispatchDeviceFinish(int tray, int dispatchIndex);

    /** createArmBackTray(rnaIndex, DEVICE_RNA, tray, dispatchIndex)。 */
    boolean createBackTray(int rnaIndex, int tray, int dispatchIndex);

    // ---- PCR 命令 ----
    /** usererror 清零 + uIinitCallback.finish(DEVICE_PCR, pcrIndex) + 日志。 */
    void fireUiInitPcr(int pcrIndex);

    /** startRunTemplate(pcrIndex, DEVICE_PCR, tray, dispatchIndex)。 */
    boolean startRunTemplatePcr(int pcrIndex, int tray, int dispatchIndex);

    /** createArmMoveTray(pcrIndex, DEVICE_PCR, rnaIndex, DEVICE_RNA, tray, dispatchIndex)（PCR→RNA）。 */
    boolean createMovePcr(int pcrIndex, int rnaIndex, int tray, int dispatchIndex);

    /** createArmBackTray(pcrIndex, DEVICE_PCR, tray, dispatchIndex)。 */
    boolean createBackPcr(int pcrIndex, int tray, int dispatchIndex);

    // ---- 快照 / 回写 ----
    /** 从 PipeRNAModel 构建快照；model 为 null 返回 null。 */
    RnaSchedulingState snapshotRna(int rnaIndex);

    /** 把引擎改动（当前仅 armStep）回写到 PipeRNAModel。 */
    void applyRna(int rnaIndex, RnaSchedulingState state);

    /** 从 PipePCRModel 构建快照；model 为 null 返回 null。 */
    PcrSchedulingState snapshotPcr(int pcrIndex);

    /** 把引擎改动（当前仅 armStep）回写到 PipePCRModel。 */
    void applyPcr(int pcrIndex, PcrSchedulingState state);

    // ===== 第 4 步：tray 编排层端口 =====

    // ---- 托盘遍历 + 标量读 ----
    /** @return 托盘总数（getTraylist().size()） */
    int trayCount();
    /** @param i 列表下标 @return 该托盘的 index（trays.get(i).index） */
    int trayIndexAt(int i);
    /** @param i 列表下标 @return 该托盘及其 index 是否均非空 */
    boolean trayIndexKnownAt(int i);
    /** @param i 列表下标 @return 该列表项是否非空（trays.get(i)!=null） */
    boolean trayExistsAt(int i);
    /** @param i 列表下标 @return 该托盘是否加急（urgent!=null && TRUE.equals(urgent)） */
    boolean trayUrgentAt(int i);
    /** @return 托盘状态是否等于给定值（忽略大小写） */
    boolean trayStatusIs(int trayIndex, String status);
    /** @return 托盘状态是否非空（status.get()!=null） */
    boolean trayHasStatus(int trayIndex);
    /** @return 托盘当前调度序号（dispatchindex.get()，原为拆箱读取、恒有值） */
    int trayDispatchIndex(int trayIndex);
    /** @return 调度表大小；dispatchlist==null 时返回 -1 */
    int trayDispatchSize(int trayIndex);
    /** @return 托盘存在传感器是否为真（exist!=null && exist，写回守卫用） */
    boolean trayExistFlag(int trayIndex);
    /** @return 错误调度序号是否非空（errorDispatchindex.get()!=null） */
    boolean trayHasErrorDispatchIndex(int trayIndex);
    /** @return 错误调度序号（errorDispatchindex.get()） */
    int trayErrorDispatchIndex(int trayIndex);

    // ---- 当前 / 错误调度项快照 ----
    /** 当前调度项（dm.index==tray.dispatchindex）；无匹配返回 null。 */
    TrayDispatchSnapshot currentTrayDispatch(int trayIndex);
    /** 错误调度项：edindex==dm.index || dm.status==ERROR || dm.devicestatus==ERROR（含空值守卫）；无返回 null。 */
    TrayDispatchSnapshot findErrorDispatch(int trayIndex);

    // ---- 调度项 / 托盘写 ----
    /** 分配空闲 RNA 给当前调度项：set target/status=RUN + setCurrentTemplate + setCurrentDispatch */
    void allocateFreeRnaToDispatch(int trayIndex, int dispatchIndex);
    /** 分配空闲 PCR 给当前调度项（与 RNA 对称） */
    void allocateFreePcrToDispatch(int trayIndex, int dispatchIndex);
    /** 推进托盘调度序号（tray.dispatchindex.set(newDispatchIndex)） */
    void advanceTrayDispatch(int trayIndex, int newDispatchIndex);
    /** 托盘标记复位为空闲（flagstatus.set(FLAG_FREE)） */
    void setTrayFlagFree(int trayIndex);
    /** 托盘状态置完成（status.set(str_FINISH)） */
    void setTrayStatusFinish(int trayIndex);
    /** 当前调度项状态置完成（dm.status.set(STATUS_FINISH)） */
    void setDispatchStatusFinish(int trayIndex, int dispatchIndex);
    /** 下发 LED 任务（APLManager.createLedTask(trayIndex, arg)，arg=0 关灯） */
    void createLedTask(int trayIndex, int arg);
    /** 持久化托盘（TrayManager.saveModel(trayIndex)） */
    void saveTray(int trayIndex);

    // ---- 设备完成写回（checktrayend RUN 分支 & checktrayerror 共用）----
    /** @return 设备是否已回盘完成（runtime.isBacktrayfinish() && armStep==BACK） */
    boolean deviceBackFinishedInBack(int deviceType, int deviceIndex);
    /** 标记设备调度完成：armStep=FREE + runtime.uiinit=true */
    void markDeviceScheduleDone(int deviceType, int deviceIndex);

    // ---- 委托到已引擎化的设备链路 ----
    /** 委托 RNA 设备链路一步（APLManager.checkrnawork），@return 是否空闲 */
    boolean delegateRna(int deviceIndex, int trayIndex);
    /** 委托 PCR 设备链路一步（APLManager.checkpcrwork），@return 是否空闲 */
    boolean delegatePcr(int deviceIndex, int trayIndex);

    // ---- 关门前置 ----
    /** 关核提门（APLManager.checkrnaclosedoor），@return 是否已到关门态 */
    boolean closeRnaDoor(int rnaIndex);

    // ---- 日志钩子 ----
    /** level 复用 android.util.Log 的常量（VERBOSE=2..ERROR=6）；实现里转真 Log，测试里 no-op。 */
    void log(int level, String tag, String msg);
}
