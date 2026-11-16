package com.gidi.bio_pcr.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * 模组启用/禁用管理工具类
 * 使用 SharedPreferences 持久化存储各模组的启用状态
 * 设置后需要重启应用才能生效
 */
public class ModuleEnableManager {
    private static final String TAG = "ModuleEnableManager";

    private static final String PREF_NAME = "module_enable_settings";
    private static final String KEY_PREFIX = "module_enabled_";

    private static ModuleEnableManager instance;
    private SharedPreferences preferences;
    private Context context;

    // 内存缓存，避免频繁读取 SharedPreferences
    private Map<String, Boolean> enableCache;

    private ModuleEnableManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.enableCache = new HashMap<>();
        loadAllToCache();
    }

    /**
     * 获取单例实例
     * 注意：必须在 Application.onCreate() 中初始化
     */
    public static ModuleEnableManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ModuleEnableManager.class) {
                if (instance == null) {
                    instance = new ModuleEnableManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * 获取单例实例（已初始化后使用）
     */
    public static ModuleEnableManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ModuleEnableManager not initialized. Call getInstance(Context) first.");
        }
        return instance;
    }

    /**
     * 加载所有已保存的启用状态到缓存
     */
    private void loadAllToCache() {
        Map<String, ?> allPrefs = preferences.getAll();
        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            if (entry.getKey().startsWith(KEY_PREFIX)) {
                String deviceId = entry.getKey().substring(KEY_PREFIX.length());
                Boolean enabled = (Boolean) entry.getValue();
                if (enabled != null) {
                    enableCache.put(deviceId, enabled);
                }
            }
        }
        Log.d(TAG, "Loaded " + enableCache.size() + " module states to cache");
    }

    /**
     * 检查指定模组是否启用
     * @param deviceId 设备ID（如 SLANPCR1111）
     * @return true=启用, false=禁用
     */
    public boolean isModuleEnabled(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return true;  // 无效ID默认启用
        }

        // 先从缓存读取
        if (enableCache.containsKey(deviceId)) {
            return enableCache.get(deviceId);
        }

        // 缓存未命中，从 SharedPreferences 读取
        String key = KEY_PREFIX + deviceId;
        boolean enabled = preferences.getBoolean(key, true);  // 默认启用
        enableCache.put(deviceId, enabled);
        return enabled;
    }

    /**
     * 设置指定模组的启用状态
     * 注意：设置后需要重启应用才能生效
     * @param deviceId 设备ID
     * @param enabled true=启用, false=禁用
     * @return 保存是否成功
     */
    public boolean setModuleEnabled(String deviceId, boolean enabled) {
        if (deviceId == null || deviceId.isEmpty()) {
            Log.w(TAG, "setModuleEnabled: invalid deviceId");
            return false;
        }

        String key = KEY_PREFIX + deviceId;
        boolean success = preferences.edit().putBoolean(key, enabled).commit();

        if (success) {
            enableCache.put(deviceId, enabled);
            Log.i(TAG, "Module " + deviceId + " enabled=" + enabled + " (需要重启生效)");
        } else {
            Log.e(TAG, "Failed to save module state: " + deviceId);
        }

        return success;
    }

    /**
     * 批量设置多个模组的启用状态
     * @param enableStates Map<deviceId, enabled>
     * @return 保存是否成功
     */
    public boolean setModulesEnabled(Map<String, Boolean> enableStates) {
        if (enableStates == null || enableStates.isEmpty()) {
            return false;
        }

        SharedPreferences.Editor editor = preferences.edit();
        for (Map.Entry<String, Boolean> entry : enableStates.entrySet()) {
            String deviceId = entry.getKey();
            Boolean enabled = entry.getValue();
            if (deviceId != null && !deviceId.isEmpty() && enabled != null) {
                String key = KEY_PREFIX + deviceId;
                editor.putBoolean(key, enabled);
            }
        }

        boolean success = editor.commit();
        if (success) {
            enableCache.putAll(enableStates);
            Log.i(TAG, "Batch updated " + enableStates.size() + " modules (需要重启生效)");
        } else {
            Log.e(TAG, "Failed to batch update modules");
        }

        return success;
    }

    /**
     * 获取所有模组的启用状态
     * @return Map<deviceId, enabled>
     */
    public Map<String, Boolean> getAllModuleStates() {
        return new HashMap<>(enableCache);
    }

    /**
     * 重置指定模组为默认状态（启用）
     * @param deviceId 设备ID
     * @return 保存是否成功
     */
    public boolean resetModule(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return false;
        }

        String key = KEY_PREFIX + deviceId;
        boolean success = preferences.edit().remove(key).commit();

        if (success) {
            enableCache.put(deviceId, true);  // 默认启用
            Log.i(TAG, "Reset module " + deviceId + " to default (enabled)");
        }

        return success;
    }

    /**
     * 重置所有模组为默认状态（启用）
     * @return 保存是否成功
     */
    public boolean resetAll() {
        boolean success = preferences.edit().clear().commit();

        if (success) {
            enableCache.clear();
            Log.i(TAG, "Reset all modules to default (enabled)");
        }

        return success;
    }

    /**
     * 获取已禁用的模组数量
     */
    public int getDisabledModuleCount() {
        int count = 0;
        for (Boolean enabled : enableCache.values()) {
            if (!enabled) {
                count++;
            }
        }
        return count;
    }

    /**
     * 检查是否至少有一个模组启用（安全检查）
     * @return true=有至少一个启用的模组
     */
    public boolean hasAnyModuleEnabled() {
        for (Boolean enabled : enableCache.values()) {
            if (enabled) {
                return true;
            }
        }
        // 如果缓存为空或全部禁用，返回true（默认状态）
        return enableCache.isEmpty();
    }
}
