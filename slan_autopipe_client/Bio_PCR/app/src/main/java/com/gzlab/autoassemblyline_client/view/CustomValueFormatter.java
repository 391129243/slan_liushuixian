package com.gzlab.autoassemblyline_client.view;

import com.github.mikephil.charting.formatter.ValueFormatter;
import com.hongshi.slansdk.common.HSUtils;

public class CustomValueFormatter extends ValueFormatter {
    @Override
    public String getFormattedValue(float value) {
        // 自定义格式化逻辑
        // 例如，将值乘以100并加上单位%
        long timeInMillis = Math.round(value); // 将float值转换为long类型的时间戳（毫秒）
       // Log.i("AAA","timeInMillis" + timeInMillis);
        return HSUtils.INSTANCE.INSTANCE.secondToHMSString(timeInMillis); // 格式化时间戳为字符串
    }
}
