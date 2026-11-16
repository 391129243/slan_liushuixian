package com.paul.pipeline.scheduling;

import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.model.ArmStep;
import com.paul.pipeline.model.PcrSchedulingState;
import com.paul.pipeline.model.RnaSchedulingState;
import com.paul.pipeline.model.TrayDispatchSnapshot;

/**
 * databinding-free 调度引擎（第 3 步 RNA 单链路试点）。
 * <p>
 * 从 {@link APLManager#checkrnawork} 抽出的纯决策逻辑：只依赖 {@link SchedulingPort}
 * 与纯 POJO/枚举，可在纯 JVM 下单测。行为与原 switch 逐行等价，副作用（下发机械臂命令、
 * 改 Tray 状态、UI 回调）全部经 port 委托给适配层。诊断日志经 {@link SchedulingPort#log}
 * 保留，等价于原 android.util.Log.*（避免引擎依赖 Android 框架）。
 */
public class SchedulingEngine {

    private static final String TAG = "ArmTaskWorker";
    // 对齐 android.util.Log 的级别常量，避免 import android
    private static final int INFO = 4;
    private static final int WARN = 5;
    private static final int ERROR = 6;

    private final SchedulingPort port;

    public SchedulingEngine(SchedulingPort port) {
        this.port = port;
    }

    /**
     * 推进一台 RNA 的调度一步。
     *
     * @return true 表示空闲（机械臂可去调度别处）；false 仅在 GET/MOVE/BACK 成功下发命令时返回。
     */
    public boolean stepRna(RnaSchedulingState s, int tray) {
        // 快照为空（model 不存在）：视为空闲
        if (s == null) {
            return true;
        }
        // 托盘数据不一致：守卫日志由适配层在 snapshot 之外打印（与原实现一致），此处不重复
        if (s.currentTray != tray) {
            return true;
        }
        // 设备（本机 + Linux 侧）非空闲：本轮不推进
        if (!s.statusFree) {
            return true;
        }

        switch (s.armStep) {
            // FREE：无调度任务。仅在界面请求初始化(uiinit)且无托盘时，回调 UI 复位
            case FREE:
                if (s.runtime.isUiinit()) {
                    if (s.exist) {
                        port.log(WARN, TAG, "存在托盘，不可初始化 rna:" + s.index);
                    } else {
                        s.runtime.setUiinit(false);
                        // usererror 清零、uIinitCallback.finish、成功/信息日志都在 fireUiInit 内，
                        // 与原 checkrnawork 的 FREE 分支顺序一致
                        port.fireUiInit(s.index);
                    }
                }
                break;

            // RUN：托盘已到位，下发模板开始运行（未启动过才发）
            case RUN:
                if (!s.runtime.isExcutestart()) {
                    if (port.isWssStart() && port.isSelfCheck()) {
                        int di = port.currentDispatchIndex(s.currentTray);
                        if (di < 0) {
                            port.log(ERROR, TAG, "WORK_RUN 找不到调度数据 rna:" + s.index + " tray:" + s.currentTray);
                        } else {
                            if (port.startRunTemplateRna(s.index, tray, di)) {
                                s.runtime.setExcutestart(true);
                                s.runtime.setExcutefinish(false);
                                port.log(WARN, TAG, "startRunTemplate 成功 rna:" + s.index);
                            } else {
                                port.log(ERROR, TAG, "startRunTemplate 失败 rna:" + s.index);
                            }
                        }
                    } else {
                        port.log(WARN, TAG, "rna WORK_RUN WSSStart:" + port.isWssStart() + " SelfCheck:" + port.isSelfCheck());
                    }
                }
                break;

            // GET：调度机械臂把托盘从托盘架取到本设备（未启动过、且未开门、资源就绪、机械臂空闲）
            case GET:
                if (!s.runtime.isGettraystart()) {
                    if (!port.isDoorHold()) { // 开门事件优先级最高
                        if (port.isWssStart() && port.isSelfCheck()) {
                            if (port.isArmCanuse()) {
                                int di = port.currentDispatchIndex(s.currentTray);
                                if (di < 0) {
                                    port.log(ERROR, TAG, "WORK_GET 找不到调度数据 rna:" + s.index + " tray:" + s.currentTray);
                                } else {
                                    if (port.createGetTray(s.currentTray)) {
                                        s.runtime.setGettraystart(true);
                                        s.runtime.setGettrayfinish(false);
                                        port.log(WARN, TAG, "createArmTaskGetTray 成功 rna:" + s.index + " tray:" + s.currentTray);
                                        // 命令已下发、占用机械臂 → 返回 false 让本轮不再调度别处
                                        return false;
                                    } else {
                                        port.log(ERROR, TAG, "createArmTaskGetTray 失败 rna:" + s.index + " tray:" + s.currentTray);
                                    }
                                }
                            }
                        } else {
                            port.log(WARN, TAG, "rna WORK_GET WSSStart:" + port.isWssStart() + " SelfCheck:" + port.isSelfCheck());
                        }
                    }
                }
                break;

            // MOVE：RNA 完成后把托盘从 RNA 移到空闲 PCR（方向 RNA→PCR）
            case MOVE:
                if (!s.runtime.isMovetraystart()) {
                    if (!port.isDoorHold()) { // 开门事件优先级最高
                        if (port.isWssStart() && port.isSelfCheck()) {
                            if (port.isArmCanuse()) {
                                port.log(ERROR, TAG, "createArmMoveTray rna:" + s.index);
                                int di = port.currentDispatchIndex(s.currentTray);
                                if (di < 0) {
                                    port.log(ERROR, TAG, "WORK_MOVE 找不到调度数据 rna:" + s.index + " tray:" + s.currentTray);
                                } else {
                                    int pcr = port.findFreePcrIndex();
                                    if (pcr > 0) { // 找到空闲 PCR：移盘
                                        if (port.createMoveTray(s.index, pcr, s.currentTray, di)) {
                                            port.markDispatchDeviceFinish(s.currentTray, di);
                                            s.runtime.setMovetraystart(true);
                                            s.runtime.setMovetrayfinish(false);
                                            port.log(WARN, TAG, "createArmMoveTray 成功 rna:" + s.index + " pcr:" + pcr);
                                            // 命令已下发、占用机械臂 → 返回 false
                                            return false;
                                        } else {
                                            port.log(ERROR, TAG, "createArmMoveTray 失败 rna:" + s.index + " pcr:" + pcr);
                                        }
                                    } else {
                                        // 找不到空闲 PCR：不移盘，转为把托盘送回托盘架（WORK_BACK），本轮贯穿到 return true
                                        s.runtime.setBacktraystart(false);
                                        s.runtime.setBacktrayfinish(false);
                                        s.armStep = ArmStep.BACK;
                                        port.log(ERROR, TAG, "WORK_MOVE rna :" + s.index + " 找不到空闲pcr-还托盘WORK_BACK ");
                                    }
                                }
                            } else {
                                port.log(WARN, TAG, "rna WORK_MOVE arm busy");
                            }
                        } else {
                            port.log(WARN, TAG, "rna WORK_MOVE WSSStart:" + port.isWssStart() + " SelfCheck:" + port.isSelfCheck());
                        }
                    } else {
                        port.log(WARN, TAG, "rna WORK_MOVE opendoor");
                    }
                } else {
                    port.log(WARN, TAG, "rna WORK_MOVE movetraystart is true");
                }
                break;

            // BACK：把托盘从本设备送回托盘架（异常还盘 / 无空闲对侧设备 / 整列结束都走这里）
            case BACK:
                if (!s.runtime.isBacktraystart()) {
                    if (!port.isDoorHold()) { // 开门事件优先级最高
                        if (port.isWssStart()) { // 注意：BACK 只判 WSSStart，不含 SelfCheck（与原实现一致，勿归一化）
                            if (port.isArmCanuse()) {
                                port.log(ERROR, TAG, "createArmBackTray rna:" + s.index);
                                int di = port.currentDispatchIndex(s.currentTray);
                                if (di < 0) {
                                    port.log(ERROR, TAG, "WORK_BACK 找不到调度数据 rna:" + s.index + " tray:" + s.currentTray);
                                } else {
                                    if (port.createBackTray(s.index, s.currentTray, di)) {
                                        port.markDispatchDeviceFinish(s.currentTray, di);
                                        s.runtime.setBacktraystart(true);
                                        s.runtime.setBacktrayfinish(false);
                                        port.log(WARN, TAG, "checkrnawork createArmBackTray 成功 rna:" + s.index + " tray:" + s.currentTray);
                                        // 命令已下发、占用机械臂 → 返回 false
                                        return false;
                                    } else {
                                        port.log(ERROR, TAG, "checkrnawork createArmBackTray 失败 rna:" + s.index + " tray:" + s.currentTray);
                                    }
                                }
                            } else {
                                port.log(WARN, TAG, "rna WORK_BACK arm busy");
                            }
                        } else {
                            port.log(WARN, TAG, "rna WORK_BACK WSSStart:" + port.isWssStart() + " SelfCheck:" + port.isSelfCheck());
                        }
                    } else {
                        port.log(WARN, TAG, "rna WORK_BACK opendoor");
                    }
                } else {
                    port.log(WARN, TAG, "rna WORK_BACK start is true");
                }
                break;

            default:
                break;
        }
        return true;
    }

    /**
     * 推进一台 PCR 的调度一步。与 {@link #stepRna} 逐行等价，差异：
     * 1) 外层托盘一致性守卫仅在 currentTrayKnown 时才判（原 checkpcrwork）；
     * 2) MOVE 找空闲 RNA（PCR→RNA，方向相反）；
     * 3) 命令走 PCR 端口方法。
     *
     * @return true 表示空闲；false 仅在 GET/MOVE/BACK 成功下发命令时返回。
     */
    public boolean stepPcr(PcrSchedulingState s, int tray) {
        // 快照为空：视为空闲
        if (s == null) {
            return true;
        }
        // 差异点：仅当 currentTray 已知时才做托盘一致性早退（PCR 的 currentTray 可能为 null）
        if (s.currentTrayKnown && s.currentTray != tray) {
            return true;
        }
        // 设备非空闲：本轮不推进
        if (!s.statusFree) {
            return true;
        }

        port.log(INFO, TAG, " pcr: " + s.index + " armStep:" + s.armStep);
        switch (s.armStep) {
            // FREE：无任务，仅按需回调 UI 初始化
            case FREE:
                if (s.runtime.isUiinit()) {
                    if (s.exist) {
                        port.log(WARN, TAG, "存在托盘，不可初始化 pcr:" + s.index);
                    } else {
                        s.runtime.setUiinit(false);
                        port.fireUiInitPcr(s.index);
                    }
                }
                break;

            // GET：机械臂把托盘取到本 PCR
            case GET:
                if (!s.runtime.isGettraystart()) {
                    if (!port.isDoorHold()) { // 开门事件优先级最高
                        if (port.isWssStart() && port.isSelfCheck()) {
                            if (port.isArmCanuse()) {
                                int di = port.currentDispatchIndex(s.currentTray);
                                if (di < 0) {
                                    port.log(ERROR, TAG, "WORK_GET 找不到调度数据 pcr:" + s.index + " tray:" + s.currentTray);
                                } else {
                                    if (port.createGetTray(s.currentTray)) {
                                        s.runtime.setGettraystart(true);
                                        s.runtime.setGettrayfinish(false);
                                        port.log(WARN, TAG, "createArmTaskGetTray 成功 pcr:" + s.index + " tray:" + s.currentTray);
                                        // 命令已下发、占用机械臂 → 返回 false
                                        return false;
                                    } else {
                                        port.log(ERROR, TAG, "createArmTaskGetTray 失败 pcr:" + s.index + " tray:" + s.currentTray);
                                    }
                                }
                            }
                        } else {
                            port.log(WARN, TAG, "PCR WORK_GET WSSStart:" + port.isWssStart() + " SelfCheck:" + port.isSelfCheck());
                        }
                    }
                }
                break;

            // RUN：下发 PCR 模板开始运行
            case RUN:
                if (!s.runtime.isExcutestart()) {
                    if (port.isWssStart() && port.isSelfCheck()) {
                        int di = port.currentDispatchIndex(s.currentTray);
                        if (di < 0) {
                            port.log(ERROR, TAG, "WORK_RUN 找不到调度数据 pcr:" + s.index + " tray:" + s.currentTray);
                        } else {
                            if (port.startRunTemplatePcr(s.index, tray, di)) {
                                s.runtime.setExcutestart(true);
                                s.runtime.setExcutefinish(false);
                                port.log(WARN, TAG, "startRunTemplate 成功 pcr:" + s.index);
                            } else {
                                port.log(ERROR, TAG, "startRunTemplate 失败 pcr:" + s.index);
                            }
                        }
                    } else {
                        port.log(WARN, TAG, "PCR WORK_RUN WSSStart:" + port.isWssStart() + " SelfCheck:" + port.isSelfCheck());
                    }
                }
                break;

            // MOVE：PCR 完成后把托盘移到空闲 RNA（方向 PCR→RNA，与 RNA 侧相反）
            case MOVE:
                if (!s.runtime.isMovetraystart()) {
                    if (!port.isDoorHold()) { // 开门事件优先级最高
                        if (port.isWssStart() && port.isSelfCheck()) {
                            if (port.isArmCanuse()) {
                                port.log(ERROR, TAG, "WORK_MOVE pcr:" + s.index);
                                int di = port.currentDispatchIndex(s.currentTray);
                                if (di < 0) {
                                    port.log(ERROR, TAG, "WORK_MOVE 找不到调度数据 pcr:" + s.index + " tray:" + s.currentTray);
                                } else {
                                    int rna = port.findFreeRnaIndex();
                                    if (rna > 0) { // 找到空闲 RNA：移盘
                                        port.log(ERROR, TAG, "找到空闲rna: " + rna);
                                        if (port.createMovePcr(s.index, rna, s.currentTray, di)) {
                                            port.markDispatchDeviceFinish(s.currentTray, di);
                                            s.runtime.setMovetraystart(true);
                                            s.runtime.setMovetrayfinish(false);
                                            port.log(WARN, TAG, "createArmMoveTray 成功 pcr:" + s.index + " rna:" + rna);
                                            // 命令已下发、占用机械臂 → 返回 false
                                            return false;
                                        } else {
                                            port.log(ERROR, TAG, "createArmMoveTray 失败 pcr:" + s.index + " rna:" + rna);
                                        }
                                    } else {
                                        // 找不到空闲 RNA：转为送回托盘架（WORK_BACK）
                                        s.runtime.setBacktraystart(false);
                                        s.runtime.setBacktrayfinish(false);
                                        s.armStep = ArmStep.BACK;
                                        port.log(ERROR, TAG, "WORK_MOVE pcr :" + s.index + " 找不到空闲rna-还托盘WORK_BACK ");
                                    }
                                }
                            } else {
                                port.log(WARN, TAG, "PCR WORK_MOVE arm busy");
                            }
                        } else {
                            port.log(WARN, TAG, "PCR WORK_MOVE WSSStart:" + port.isWssStart() + " SelfCheck:" + port.isSelfCheck());
                        }
                    } else {
                        port.log(WARN, TAG, "PCR WORK_MOVE opendoor");
                    }
                } else {
                    port.log(WARN, TAG, "PCR WORK_MOVE movetraystart is true");
                }
                break;

            // BACK：把托盘从本 PCR 送回托盘架（异常还盘 / 无空闲 RNA / 整列结束）
            case BACK:
                if (!s.runtime.isBacktraystart()) {
                    if (!port.isDoorHold()) { // 开门事件优先级最高
                        if (port.isWssStart()) { // 注意：BACK 只判 WSSStart，不含 SelfCheck（与原实现一致，勿归一化）
                            if (port.isArmCanuse()) {
                                port.log(ERROR, TAG, "createArmBackTray pcr:" + s.index);
                                int di = port.currentDispatchIndex(s.currentTray);
                                if (di < 0) {
                                    port.log(ERROR, TAG, "WORK_BACK 找不到调度数据 pcr:" + s.index + " tray:" + s.currentTray);
                                } else {
                                    if (port.createBackPcr(s.index, s.currentTray, di)) {
                                        port.markDispatchDeviceFinish(s.currentTray, di);
                                        s.runtime.setBacktraystart(true);
                                        s.runtime.setBacktrayfinish(false);
                                        port.log(WARN, TAG, "createArmBackTray 成功 pcr:" + s.index + " tray:" + s.currentTray);
                                        // 命令已下发、占用机械臂 → 返回 false
                                        return false;
                                    } else {
                                        port.log(ERROR, TAG, "createArmBackTray 失败 pcr:" + s.index + " tray:" + s.currentTray);
                                    }
                                }
                            } else {
                                port.log(WARN, TAG, "PCR WORK_BACK arm busy");
                            }
                        } else {
                            port.log(WARN, TAG, "PCR WORK_BACK WSSStart:" + port.isWssStart() + " SelfCheck:" + port.isSelfCheck());
                        }
                    } else {
                        port.log(WARN, TAG, "PCR WORK_BACK opendoor");
                    }
                } else {
                    port.log(WARN, TAG, "PCR WORK_BACK start is true");
                }
                break;

            default:
                break;
        }
        return true;
    }

    // ================= 第 4 步：tray 编排层 =================

    /**
     * 复刻 checktrayurgent()：先关核提门，再（自检完成时）按 加急RNA→加急PCR→普通RNA→普通PCR
     * 四趟遍历，任一趟返回 false 立即中止并 return false。
     */
    public boolean checkTrayUrgent() {
        boolean result;
        // 起始阶段先确认两台核提处于关门态，防止机械臂干涉调度关门；任一未关到位则本轮不调度
        result = port.closeRnaDoor(1);
        if (!result) {
            port.log(ERROR, TAG, "rna: 1  closedoor");
            return false;
        }
        result = port.closeRnaDoor(2);
        if (!result) {
            port.log(ERROR, TAG, "rna: 2  closedoor");
            return false;
        }
        // 仅自检完成后才调度（保证机械臂/RNA/PCR/托盘空闲，不干涉自检）
        if (port.isSelfCheck()) {
            int n = port.trayCount();
            // 加急 RNA
            for (int i = 0; i < n; i++) {
                if (port.trayUrgentAt(i)) {
                    if (!checkTrayRnawork(port.trayIndexAt(i))) {
                        port.log(ERROR, TAG, "urgent rna tray:" + port.trayIndexAt(i));
                        return false;
                    }
                }
            }
            // 加急 PCR
            for (int i = 0; i < n; i++) {
                if (port.trayUrgentAt(i)) {
                    if (!checkTrayPcrwork(port.trayIndexAt(i))) {
                        port.log(ERROR, TAG, "urgent pcr tray:" + port.trayIndexAt(i));
                        return false;
                    }
                }
            }
            // 普通 RNA
            for (int i = 0; i < n; i++) {
                if (!checkTrayRnawork(port.trayIndexAt(i))) {
                    port.log(ERROR, TAG, "normal rna tray:" + port.trayIndexAt(i));
                    return false;
                }
            }
            // 普通 PCR
            for (int i = 0; i < n; i++) {
                if (!checkTrayPcrwork(port.trayIndexAt(i))) {
                    port.log(ERROR, TAG, "normal pcr tray:" + port.trayIndexAt(i));
                    return false;
                }
            }
        } else {
            port.log(WARN, TAG, "checktrayurgent WSSStart/SelfCheck:" + port.isWssStart() + " " + port.isSelfCheck());
        }
        return result;
    }

    /**
     * 复刻 checktrayerror() 无参：遍历 ERROR 托盘，逐个 checkTrayError(idx)。
     * 注意不对称：内层中止（返回 false）时本方法 return **true**；异常被 try/catch 吞掉 return false。
     */
    public boolean checkTrayError() {
        try {
            int n = port.trayCount();
            for (int i = 0; i < n; i++) {
                if (port.trayIndexKnownAt(i) && port.trayStatusIs(port.trayIndexAt(i), ConstantData.str_ERROR)) {
                    boolean result = checkTrayError(port.trayIndexAt(i));
                    if (!result) {
                        port.log(ERROR, TAG, "error tray :" + port.trayIndexAt(i));
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 复刻 checktrayerror(int)：处理异常托盘，取回托盘。
     * 写回顺序：delegate 后读 deviceBackFinishedInBack；命中则先 setTrayFlagFree 再 markDeviceScheduleDone。
     */
    public boolean checkTrayError(int trayIndex) {
        boolean result = true; // 默认空闲
        if (port.trayDispatchSize(trayIndex) >= 0 && port.trayHasErrorDispatchIndex(trayIndex)) {
            int edindex = port.trayErrorDispatchIndex(trayIndex);
            int size = port.trayDispatchSize(trayIndex);
            port.log(INFO, TAG, "tray" + trayIndex + " error_dispatchindex " + edindex + " dispatchlistsize " + size);
            if (port.trayHasStatus(trayIndex)) {
                if (port.trayStatusIs(trayIndex, ConstantData.str_ERROR)) {
                    if (edindex > 0 && size > 0 && edindex <= size) {
                        TrayDispatchSnapshot d = port.findErrorDispatch(trayIndex);
                        if (d != null) {
                            port.log(INFO, TAG, "tray" + trayIndex + " error_DispatchModel_index " + d.dispatchIndex + " status " + d.status);
                            if (d.targettype == ConstantData.TYPE_RNA) {
                                port.log(WARN, TAG, "调度单列error tray " + trayIndex + " rna :" + d.target);
                                if (d.targetKnown) {
                                    // 先委托设备推进（可能把设备推到 BACK 完成态）；再读回盘完成状态
                                    boolean rnab = port.delegateRna(d.target, trayIndex);
                                    // 设备已回盘完成 + 托盘在位：写回顺序必须先 flagFree 再设备(armStep=FREE+uiinit)，与原实现一致
                                    if (port.deviceBackFinishedInBack(ConstantData.DEVICE_RNA, d.target)
                                            && port.trayExistFlag(trayIndex)) {
                                        port.setTrayFlagFree(trayIndex);
                                        port.markDeviceScheduleDone(ConstantData.DEVICE_RNA, d.target);
                                        port.log(WARN, TAG, "tray " + trayIndex + " rna " + d.target + " 调度完成");
                                    }
                                    port.log(WARN, TAG, "tray error " + trayIndex + " rna" + d.target + " 调度 是否空闲:" + rnab);
                                    if (!rnab) {
                                        port.log(WARN, TAG, "rna " + d.target + " ERROR work ");
                                        return false;
                                    }
                                }
                            } else if (d.targettype == ConstantData.TYPE_PCR) {
                                port.log(WARN, TAG, "调度单列error tray " + trayIndex + " pcr :" + d.target);
                                if (d.targetKnown) {
                                    boolean pcrb = port.delegatePcr(d.target, trayIndex);
                                    if (port.deviceBackFinishedInBack(ConstantData.DEVICE_PCR, d.target)
                                            && port.trayExistFlag(trayIndex)) {
                                        port.setTrayFlagFree(trayIndex);
                                        port.markDeviceScheduleDone(ConstantData.DEVICE_PCR, d.target);
                                        port.log(WARN, TAG, "tray " + trayIndex + " pcr " + d.target + " error work 调度完成");
                                    }
                                    port.log(WARN, TAG, "tray error " + trayIndex + " pcr" + d.target + " 调度 是否空闲:" + pcrb);
                                    if (!pcrb) {
                                        port.log(WARN, TAG, "pcr " + d.target + " ERROR work ");
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                port.log(WARN, TAG, "checktrayerror tray " + trayIndex + " 状态为空！");
            }
        } else {
            port.log(WARN, TAG, "checktrayerror tray " + trayIndex + " 调度表为空！");
        }
        return result;
    }

    /** 复刻 checktrayRnawork(int)：分配盘位和调度机械臂（RNA）。 */
    public boolean checkTrayRnawork(int trayIndex) {
        int size = port.trayDispatchSize(trayIndex);
        if (size >= 0) {
            int di = port.trayDispatchIndex(trayIndex);
            port.log(INFO, TAG, "tray" + trayIndex + " dispatchindex " + di + " dispatchlistsize " + size);
            if (port.trayStatusIs(trayIndex, ConstantData.str_RUN)) {
                if (di > 0 && di <= size) {
                    TrayDispatchSnapshot d = port.currentTrayDispatch(trayIndex);
                    if (d != null) {
                        port.log(INFO, TAG, "checktrayRnawork  tray:" + trayIndex + " DispatchModel " + d.dispatchIndex + " status " + d.status);
                        if (d.status == ConstantData.STATUS_RUN) {
                            if (d.targettype == ConstantData.TYPE_RNA) {
                                boolean rnab = port.delegateRna(d.target, trayIndex);
                                port.log(INFO, TAG, "rna " + d.target + " work " + rnab);
                                if (!rnab) {
                                    return false;
                                }
                            }
                        } else if (d.status == ConstantData.STATUS_FREE) {
                            port.log(INFO, TAG, "tray DispatchModel type " + d.targettype);
                            if (!port.isDoorHold()) {
                                if (port.isWssStart() && port.isSelfCheck()) {
                                    if (port.isArmCanuse()) {
                                        if (d.targettype == ConstantData.DEVICE_RNA) {
                                            port.allocateFreeRnaToDispatch(trayIndex, di);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                port.log(WARN, TAG, "checktrayRnawork tray " + trayIndex + " 状态为空！");
            }
        } else {
            port.log(WARN, TAG, "checktrayRnawork tray " + trayIndex + " 调度表为空！");
        }
        return true;
    }

    /** 复刻 checktrayPcrwork(int)：分配盘位和调度机械臂（PCR）。 */
    public boolean checkTrayPcrwork(int trayIndex) {
        int size = port.trayDispatchSize(trayIndex);
        if (size >= 0) {
            int di = port.trayDispatchIndex(trayIndex);
            port.log(INFO, TAG, "tray" + trayIndex + " dispatchindex " + di + " dispatchlistsize " + size);
            if (port.trayStatusIs(trayIndex, ConstantData.str_RUN)) {
                if (di > 0 && di <= size) {
                    TrayDispatchSnapshot d = port.currentTrayDispatch(trayIndex);
                    if (d != null) {
                        port.log(INFO, TAG, "checktrayPcrwork tray:" + trayIndex + " DispatchModel " + d.dispatchIndex + " status " + d.status);
                        if (d.status == ConstantData.STATUS_RUN) {
                            port.log(INFO, TAG, " dmtargettype: " + d.targettype);
                            if (d.targettype == ConstantData.TYPE_PCR) {
                                boolean pcrb = port.delegatePcr(d.target, trayIndex);
                                port.log(INFO, TAG, "pcr " + d.target + " work " + pcrb);
                                if (!pcrb) {
                                    return false;
                                }
                            }
                        } else if (d.status == ConstantData.STATUS_FREE) {
                            port.log(INFO, TAG, "checktrayPcrwork tray:" + trayIndex + " DispatchModel " + d.dispatchIndex + " free ");
                            if (!port.isDoorHold()) {
                                if (port.isWssStart() && port.isSelfCheck()) {
                                    if (port.isArmCanuse()) {
                                        if (d.targettype == ConstantData.DEVICE_PCR) {
                                            port.allocateFreePcrToDispatch(trayIndex, di);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                port.log(WARN, TAG, "checktrayPcrwork tray " + trayIndex + " 状态为空！");
            }
        } else {
            port.log(WARN, TAG, "checktrayPcrwork tray " + trayIndex + " 调度表为空！");
        }
        return true;
    }

    /**
     * 复刻 checktrayend()：推进整盘调度进度、判定整盘结束、处理设备回盘完成写回。
     * LED/save/status 的调用顺序严格照搬（advance 分支不发 LED；save 落在 if/else 之后）。
     */
    public void checkTrayEnd() {
        int n = port.trayCount();
        for (int i = 0; i < n; i++) {
            if (!port.trayExistsAt(i)) {
                continue;
            }
            int trayIndex = port.trayIndexAt(i);
            int size = port.trayDispatchSize(trayIndex);
            if (size < 0) {
                port.log(WARN, TAG, "checktrayend tray " + trayIndex + " 调度表为空！");
                continue;
            }
            int di = port.trayDispatchIndex(trayIndex);
            port.log(INFO, TAG, "tray" + trayIndex + " dispatchindex " + di + " dispatchlistsize " + size);
            if (!port.trayHasStatus(trayIndex)) {
                port.log(WARN, TAG, "checktrayend tray " + trayIndex + " 状态为空！");
                continue;
            }
            if (!port.trayStatusIs(trayIndex, ConstantData.str_RUN)) {
                continue;
            }
            // dispatchindex 已越过末列：整盘结束（flagFree→statusFinish→关LED→存档）
            if (di > 0 && di > size) {
                port.setTrayFlagFree(trayIndex);
                port.setTrayStatusFinish(trayIndex);
                port.createLedTask(trayIndex, 0);
                port.log(WARN, TAG, "tray " + trayIndex + " 调度结束！");
                port.saveTray(trayIndex);
            } else if (di <= 0) {
                port.log(INFO, TAG, "未开始 dispatchindex 0 tray:" + trayIndex);
            } else { // di > 0 && di <= size：正在跑某一列
                TrayDispatchSnapshot d = port.currentTrayDispatch(trayIndex);
                if (d != null) {
                    port.log(INFO, TAG, "checktrayend tray " + trayIndex + " DispatchModel " + d.dispatchIndex + " status " + d.status);
                    if (d.status == ConstantData.STATUS_FINISH) {
                        // 当前列已完成：还有下一列则推进(不关LED)，否则整盘结束(关LED)
                        if ((di + 1) <= size) {
                            port.advanceTrayDispatch(trayIndex, di + 1);
                        } else {
                            port.setTrayFlagFree(trayIndex);
                            port.setTrayStatusFinish(trayIndex);
                            port.log(WARN, TAG, "tray " + trayIndex + " 整盘结束调度完成");
                            port.createLedTask(trayIndex, 0);
                            port.saveTray(trayIndex);
                        }
                    } else if (d.status == ConstantData.STATUS_RUN) {
                        // 当前列运行中：等设备回盘完成后，标该列 FINISH 并推进/整盘结束
                        if (d.targettypeKnown) {
                            if (d.targettype == ConstantData.TYPE_RNA) {
                                if (port.deviceBackFinishedInBack(ConstantData.DEVICE_RNA, d.target)) {
                                    // 顺序照搬：设备写回(FREE+uiinit)→标该列FINISH→推进或整盘finish→存档(在 if/else 之后)
                                    port.markDeviceScheduleDone(ConstantData.DEVICE_RNA, d.target);
                                    port.log(WARN, TAG, "调度单列完成 tray " + trayIndex + " rna :" + d.target);
                                    port.setDispatchStatusFinish(trayIndex, d.dispatchIndex);
                                    if ((di + 1) <= size) {
                                        port.advanceTrayDispatch(trayIndex, di + 1); // 推进分支不关 LED
                                    } else {
                                        port.setTrayFlagFree(trayIndex);
                                        port.setTrayStatusFinish(trayIndex);
                                        port.log(WARN, TAG, "tray " + trayIndex + " 整盘调度完成");
                                        port.createLedTask(trayIndex, 0);
                                    }
                                    port.saveTray(trayIndex);
                                }
                            } else if (d.targettype == ConstantData.TYPE_PCR) {
                                if (port.deviceBackFinishedInBack(ConstantData.DEVICE_PCR, d.target)) {
                                    port.markDeviceScheduleDone(ConstantData.DEVICE_PCR, d.target);
                                    port.log(WARN, TAG, "调度单列完成 tray " + trayIndex + " pcr :" + d.target);
                                    port.setDispatchStatusFinish(trayIndex, d.dispatchIndex);
                                    if ((di + 1) <= size) {
                                        port.advanceTrayDispatch(trayIndex, di + 1);
                                    } else {
                                        port.setTrayFlagFree(trayIndex);
                                        port.setTrayStatusFinish(trayIndex);
                                        port.log(WARN, TAG, "tray " + trayIndex + " 整盘调度完成");
                                        port.createLedTask(trayIndex, 0);
                                    }
                                    port.saveTray(trayIndex);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
