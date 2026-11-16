package com.paul.pipeline.model;

/**
 * 一对孔位:A 行 + B 行 同列配对
 * 例如: PairHoleModel(A8, B8)、PairHoleModel(A7, B7)...
 */
public class PairHoleModel {
    /** A 列孔位 (A1~A8) */
    public final SampleHoleModel aHole;
    /** B 列孔位 (B1~B8) */
    public final SampleHoleModel bHole;

    public PairHoleModel(SampleHoleModel aHole, SampleHoleModel bHole) {
        this.aHole = aHole;
        this.bHole = bHole;
    }

    /** 稳定 ID:基于 A 列的 index,保证 8 个 pair 的 ID 唯一且稳定 */
    public long stableId() {
        return aHole == null || aHole.index.get() == null
                ? 0L
                : aHole.index.get();
    }
}
