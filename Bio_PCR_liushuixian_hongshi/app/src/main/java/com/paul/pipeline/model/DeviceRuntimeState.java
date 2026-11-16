package com.paul.pipeline.model;

/**
 * 设备（RNA/PCR）调度运行态。
 * <p>
 * 收拢原先散落在 PipeRNAModel / PipePCRModel 上的一组调度布尔标志
 * （excutestart / gettraystart / ... ）。这些字段只被调度逻辑读写，
 * 从不参与 XML databinding，因此从 ObservableField 降级为普通字段。
 * <p>
 * 仍作为 Model 的普通成员被 MMKV(Gson) 整体序列化，重启恢复行为与旧版一致。
 * 全部为原生 boolean，默认 false，与旧 initModel 中逐个 set(false) 的初值一致。
 */
public class DeviceRuntimeState {
    private boolean gettraystart;   // 取托盘启动
    private boolean gettrayfinish;  // 取托盘完成
    private boolean excutestart;    // 模板运行启动
    private boolean excutefinish;   // 模板运行结束
    private boolean movetraystart;  // 移动托盘启动
    private boolean movetrayfinish; // 移动托盘完成
    private boolean backtraystart;  // 送回托盘启动
    private boolean backtrayfinish; // 送回托盘完成
    private boolean opendoorfinish; // 开门完成（RNA）
    private boolean closedoor;      // 关门任务（RNA）
    private boolean uiinit;         // 界面初始化

    /** @return 取托盘是否已启动 */
    public boolean isGettraystart() {
        return gettraystart;
    }

    /** 设置取托盘是否已启动 */
    public void setGettraystart(boolean gettraystart) {
        this.gettraystart = gettraystart;
    }

    /** @return 取托盘是否已完成 */
    public boolean isGettrayfinish() {
        return gettrayfinish;
    }

    /** 设置取托盘是否已完成 */
    public void setGettrayfinish(boolean gettrayfinish) {
        this.gettrayfinish = gettrayfinish;
    }

    /** @return 模板运行是否已启动 */
    public boolean isExcutestart() {
        return excutestart;
    }

    /** 设置模板运行是否已启动 */
    public void setExcutestart(boolean excutestart) {
        this.excutestart = excutestart;
    }

    /** @return 模板运行是否已结束 */
    public boolean isExcutefinish() {
        return excutefinish;
    }

    /** 设置模板运行是否已结束 */
    public void setExcutefinish(boolean excutefinish) {
        this.excutefinish = excutefinish;
    }

    /** @return 移动托盘是否已启动 */
    public boolean isMovetraystart() {
        return movetraystart;
    }

    /** 设置移动托盘是否已启动 */
    public void setMovetraystart(boolean movetraystart) {
        this.movetraystart = movetraystart;
    }

    /** @return 移动托盘是否已完成 */
    public boolean isMovetrayfinish() {
        return movetrayfinish;
    }

    /** 设置移动托盘是否已完成 */
    public void setMovetrayfinish(boolean movetrayfinish) {
        this.movetrayfinish = movetrayfinish;
    }

    /** @return 送回托盘是否已启动 */
    public boolean isBacktraystart() {
        return backtraystart;
    }

    /** 设置送回托盘是否已启动 */
    public void setBacktraystart(boolean backtraystart) {
        this.backtraystart = backtraystart;
    }

    /** @return 送回托盘是否已完成 */
    public boolean isBacktrayfinish() {
        return backtrayfinish;
    }

    /** 设置送回托盘是否已完成 */
    public void setBacktrayfinish(boolean backtrayfinish) {
        this.backtrayfinish = backtrayfinish;
    }

    /** @return 开门是否已完成（RNA） */
    public boolean isOpendoorfinish() {
        return opendoorfinish;
    }

    /** 设置开门是否已完成（RNA） */
    public void setOpendoorfinish(boolean opendoorfinish) {
        this.opendoorfinish = opendoorfinish;
    }

    /** @return 是否有待执行的关门任务（RNA） */
    public boolean isClosedoor() {
        return closedoor;
    }

    /** 设置是否有待执行的关门任务（RNA） */
    public void setClosedoor(boolean closedoor) {
        this.closedoor = closedoor;
    }

    /** @return 是否请求界面初始化 */
    public boolean isUiinit() {
        return uiinit;
    }

    /** 设置是否请求界面初始化 */
    public void setUiinit(boolean uiinit) {
        this.uiinit = uiinit;
    }
}
