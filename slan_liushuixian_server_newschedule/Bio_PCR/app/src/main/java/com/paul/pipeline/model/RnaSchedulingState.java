package com.paul.pipeline.model;

/**
 * RNA 单链路调度快照。
 * <p>
 * 由适配层（APLManager）从 PipeRNAModel 的 ObservableField 预计算而来，供不依赖
 * databinding 的 SchedulingEngine 消费。引擎只读写这个纯 POJO 和其中的 runtime，
 * 不接触任何 ObservableField / Manager 单例。
 * <p>
 * 注意：{@link #runtime} 与 PipeRNAModel.runtime 是同一引用（本身已是纯 POJO），
 * 引擎对运行态布尔的修改直接生效；适配层 applyRna 仅需回写 {@link #armStep}。
 */
public class RnaSchedulingState {
    public int index;                  // m.index.get()
    public int currentTray;            // m.currentTray.get()，null 归 0
    public boolean statusFree;         // status==FREE && linuxStatus==FREE（适配层预计算）
    public boolean exist;              // Boolean.TRUE.equals(m.exist.get())
    public ArmStep armStep;            // m.armStep.get()
    public DeviceRuntimeState runtime; // 与 model 共享同一引用
    public boolean isEnabled;          // 模组是否启用（从 ModuleEnableManager 读取）
}
