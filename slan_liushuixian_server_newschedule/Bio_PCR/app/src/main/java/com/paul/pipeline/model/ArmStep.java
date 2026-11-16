package com.paul.pipeline.model;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.paul.pipeline.data.ConstantData;

import java.io.IOException;

/**
 * 机械臂调度步骤枚举。
 * 取代原来散落在各处的裸 int（ConstantData.WORK_*）。
 * <p>
 * 每个枚举携带与旧常量一致的 int code，保证：
 * 1. 与 XML 绑定适配器（intStepString）、日志、旧逻辑的数值语义完全一致；
 * 2. Gson 序列化/反序列化仍按 int 存取（见 {@link Adapter}），
 * 老版本存档里的 {"mValue":2} 升级后可正确读回，不破坏 MMKV 存量数据。
 */
@JsonAdapter(ArmStep.Adapter.class)
public enum ArmStep {
    FREE(ConstantData.WORK_FREE),       // 0 无操作
    GET(ConstantData.WORK_GET),         // 1 取托盘
    RUN(ConstantData.WORK_RUN),         // 2 跑模板
    BACK(ConstantData.WORK_BACK),       // 3 送托盘
    MOVE(ConstantData.WORK_MOVE),       // 4 移托盘
    RECEIVE(ConstantData.WORK_RECEIVE); // 5 接托盘

    private final int code;

    ArmStep(int code) {
        this.code = code;
    }

    /** @return 与旧常量一致的 int 值（供 XML/日志/存档使用） */
    public int toInt() {
        return code;
    }

    /**
     * 由旧的 int 常量得到枚举，未知值一律回落到 {@link #FREE}，
     * 与旧 Converters.setIntStepString 的 default 分支行为一致。
     */
    public static ArmStep fromInt(int code) {
        for (ArmStep step : values()) {
            if (step.code == code) {
                return step;
            }
        }
        return FREE;
    }

    /** 兼容 Integer（可空）入参，null 视为 FREE。 */
    public static ArmStep fromInt(Integer code) {
        return code == null ? FREE : fromInt(code.intValue());
    }

    /**
     * Gson 适配器：把枚举当成 int 读写，保持与旧存档 {"mValue":<int>} 的二进制兼容。
     * 兼容容错：若历史数据写成了字符串数字也能解析。
     */
    public static final class Adapter extends TypeAdapter<ArmStep> {
        /** 序列化：写出枚举对应的 int（null 视为 FREE） */
        @Override
        public void write(JsonWriter out, ArmStep value) throws IOException {
            if (value == null) {
                out.value(FREE.code);
            } else {
                out.value(value.code);
            }
        }

        /** 反序列化：从 int 读回枚举；兼容 null 与历史字符串数字，异常一律回落 FREE */
        @Override
        public ArmStep read(JsonReader in) throws IOException {
            JsonToken token = in.peek();
            if (token == JsonToken.NULL) {
                in.nextNull();
                return FREE;
            }
            if (token == JsonToken.STRING) {
                String s = in.nextString();
                try {
                    return fromInt(Integer.parseInt(s.trim()));
                } catch (NumberFormatException e) {
                    return FREE;
                }
            }
            return fromInt(in.nextInt());
        }
    }
}
