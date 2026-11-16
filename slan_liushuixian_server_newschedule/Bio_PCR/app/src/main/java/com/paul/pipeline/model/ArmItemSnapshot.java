package com.paul.pipeline.model;

import com.winters.boweilibrary.autopipeline.model.item.ItemProperty;

/**
 * 机械臂回调里 ItemProperty 的纯 POJO 快照（仅取引擎路由需要的 type + position）。
 * <p>
 * ArmCallbackEngine 只依赖本 POJO + ConstantData 常量，不 import winters 库的 ItemProperty，
 * 也不碰 databinding，从而可在纯 JVM 下单测。适配层（APLManager）从真 ItemProperty 构建它。
 * <p>
 * 本 POJO 位于 model 包（非引擎包），可 import ItemProperty 提供 {@link #of} 工厂，保持适配层调用简洁。
 */
public class ArmItemSnapshot {
    /** 设备类型名：ConstantData.nameRNA / namePCR / nameTray（可能为 null）。 */
    public final String type;
    /** 设备/托盘位置序号。 */
    public final int position;

    public ArmItemSnapshot(String type, int position) {
        this.type = type;
        this.position = position;
    }

    /** 从真 ItemProperty 构建快照；入参为 null 时返回 null（照搬原回调的 null 检查语义）。 */
    public static ArmItemSnapshot of(ItemProperty item) {
        if (item == null) {
            return null;
        }
        return new ArmItemSnapshot(item.getType(), item.getPosition());
    }
}
