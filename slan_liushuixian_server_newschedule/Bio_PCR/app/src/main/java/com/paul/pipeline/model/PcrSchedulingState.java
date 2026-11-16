package com.paul.pipeline.model;

/**
 * PCR 单链路调度快照。与 {@link RnaSchedulingState} 结构基本一致，多一个
 * {@link #currentTrayKnown}：PCR 的外层托盘一致性守卫仅在 currentTray!=null 时才判定
 * （见原 checkpcrwork 首段），需忠实还原这一差异。
 * <p>
 * {@link #runtime} 与 PipePCRModel.runtime 是同一引用；引擎原地改运行态，
 * 适配层 applyPcr 仅回写 {@link #armStep}。
 */
public class PcrSchedulingState {
    public int index;                  // m.index.get()
    public boolean currentTrayKnown;   // m.currentTray.get() != null
    public int currentTray;            // m.currentTray.get()，null 归 0
    public boolean statusFree;         // status==FREE && linuxStatus==FREE（适配层预计算）
    public boolean exist;              // Boolean.TRUE.equals(m.exist.get())
    public ArmStep armStep;            // m.armStep.get()
    public DeviceRuntimeState runtime; // 与 model 共享同一引用
    public boolean isEnabled;          // 模组是否启用（从 ModuleEnableManager 读取）
}
