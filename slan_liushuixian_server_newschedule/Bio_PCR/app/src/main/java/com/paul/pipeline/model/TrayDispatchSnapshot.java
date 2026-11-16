package com.paul.pipeline.model;

/**
 * 托盘“当前/错误调度项”的极小快照，供 databinding-free 的 SchedulingEngine 在需要同时
 * 判定 DispatchModel 多个字段时使用。由适配层从 DispatchModel 的 ObservableField 预计算，
 * *Known 标志忠实还原原代码里的各处空值守卫（dm.status.get()!=null 等），
 * 避免在引擎里出现 Integer 拆箱/空指针语义漂移。
 */
public class TrayDispatchSnapshot {
    public int dispatchIndex;       // dm.index.get()
    public int status;              // dm.status.get()：STATUS_FREE/RUN/FINISH/ERROR
    public boolean statusKnown;     // dm.status.get() != null
    public int targettype;          // dm.targettype.get()：TYPE_RNA/TYPE_PCR
    public boolean targettypeKnown; // dm.targettype.get() != null
    public int target;              // dm.target.get()，null 归 0
    public boolean targetKnown;     // dm.target.get() != null
    public int devicestatus;        // dm.devicestatus.get()，null 归 0
    public boolean devicestatusKnown; // dm.devicestatus.get() != null
}
