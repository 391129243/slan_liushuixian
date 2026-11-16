package com.gidi.bio_pcr.slan;

import com.gidi.bio_pcr.config.ConfigTarget;

import java.util.List;

/**
 * 参数更新毁掉接口
 */
public interface OnParameterUpdateListener {
    /**
     * 扩增参数更新
     */
    void onAmpParametersUpdated(List<ConfigTarget> configTargets, int ampAlgorithm);

    /**
     * 熔解参数更新
     */
    void onMeltParametersUpdated(List<ConfigTarget> configTargets);

}
