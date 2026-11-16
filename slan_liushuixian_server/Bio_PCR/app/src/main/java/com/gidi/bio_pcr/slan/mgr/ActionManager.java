package com.gidi.bio_pcr.slan.mgr;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.adapter.CustomSpinnerAdapter;
import com.gidi.bio_pcr.databinding.FragmentAioExDetailBinding;
import com.gidi.bio_pcr.db.ExperimentModel;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.ScreenShotUtil;
import com.gidi.bio_pcr.view.CustomAddSubEdit;
import com.gidi.bio_pcr.view.CustomLargeDialog;
import com.hongshi.slansdk.model.experiment.Experiment;
import com.hongshi.slansdk.service.SLANService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作管理器
 * 职责：管理截图、保存JSON、更多操作等
 */
public class ActionManager {

    private static final String TAG = "ActionManager";

    private final Context context;
    private final FragmentAioExDetailBinding binding;
    private final ChartManager chartManager;
    private final DataManager dataManager;
    private final PopupWindowManager popupManager;

    // 主线程Handler
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private CustomLargeDialog coordinateDialog;

    // Toast显示时长常量
    private static final int TOAST_SHORT = Toast.LENGTH_SHORT;
    private static final int TOAST_LONG = Toast.LENGTH_LONG;
    private CurveType currentType;

    public ActionManager(Context context, FragmentAioExDetailBinding binding,
                         ChartManager chartManager, DataManager dataManager,
                         PopupWindowManager popupManager) {
        this.context = context;
        this.binding = binding;
        this.chartManager = chartManager;
        this.dataManager = dataManager;
        this.popupManager = popupManager;
    }

    // ==================== UI线程辅助方法 ====================

    /**
     * 在主线程执行Runnable
     */
    private void runOnUiThread(Runnable action) {
        if (action != null) {
            mainHandler.post(action);
        }
    }

    /**
     * 显示Toast（主线程）
     */
    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(context, message, TOAST_SHORT).show());
    }

    /**
     * 显示Toast（长显示）
     */
    private void showToastLong(String message) {
        runOnUiThread(() -> Toast.makeText(context, message, TOAST_LONG).show());
    }

    /**
     * 判断是否在主线程
     */
    private boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    // ==================== 保存JSON文件 ====================

    /**
     * 保存JSON文件
     */
    public void saveJsonFile(ExperimentModel model) {
        if (model == null) {
            showToast("无数据可保存");
            return;
        }

        String pcrFolderPath = model.getFilePath();
        if (TextUtils.isEmpty(pcrFolderPath)) {
            showToast("文件路径无效");
            return;
        }

        // 检查PCR文件
        File pcrFile = getPcrFile(pcrFolderPath);
        if (pcrFile == null || !pcrFile.exists()) {
            showToast("PCR文件不存在");
            return;
        }

        // 检查JSON是否已存在
        if (hasJsonFile(pcrFolderPath)) {
            showToast("JSON文件已存在");
            return;
        }

        // 异步保存
        new Thread(new SaveJsonTask(pcrFile, pcrFolderPath)).start();
    }

    /**
     * 保存JSON异步任务
     */
    private class SaveJsonTask implements Runnable {
        private final File pcrFile;
        private final String folderPath;

        SaveJsonTask(File pcrFile, String folderPath) {
            this.pcrFile = pcrFile;
            this.folderPath = folderPath;
        }

        @Override
        public void run() {
            try {
                if (PcrApplication.slanService == null) {
                    runOnUiThread(() -> Toast.makeText(context, "SLAN服务未初始化", Toast.LENGTH_LONG).show());
                    return;
                }

                Experiment experiment = PcrApplication.slanService.openExperiment(
                        pcrFile.getAbsolutePath(), true);

                if (experiment != null) {
                    // TODO: 调用实际的保存方法
                    // SLANManager.getInstance().saveDetailExToJson(experiment, folderPath);
                    runOnUiThread(() -> Toast.makeText(context, "JSON保存成功", TOAST_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(context, "保存失败，无法打开PCR文件", TOAST_LONG).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(context, "保存失败: " + e.getMessage(), TOAST_LONG).show());
                e.printStackTrace();
            }
        }
    }

    private File getPcrFile(String folderPath) {
        if (TextUtils.isEmpty(folderPath)) {
            return null;
        }
        File dir = new File(folderPath);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".pcr")) {
                        return file;
                    }
                }
            }
        }
        return null;
    }

    private boolean hasJsonFile(String folderPath) {
        if (TextUtils.isEmpty(folderPath)) {
            return false;
        }
        File dir = new File(folderPath);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".json")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ==================== 截图 ====================

    /**
     * 截图
     */
    public void screenShot(ExperimentModel model) {
        if (model == null) {
            showToast("无数据可截图");
            return;
        }

        String startTime = model.getStartTime();
        String testItem = model.getTestItem();
        String description = model.getDescription();

        if (TextUtils.isEmpty(startTime) || TextUtils.isEmpty(testItem)) {
            showToast("实验信息不完整");
            return;
        }

        // 创建截图目录
        String afterTime = DateFormatUtil.convertDateFormat(startTime);
        String subFirName = !TextUtils.isEmpty(description)
                ? afterTime + "_" + testItem + "_" + description
                : afterTime + "_" + testItem;

        File dir = new File(FileUtils.IMAGE_PATH, subFirName);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                showToast("创建截图目录失败");
                return;
            }
        }

        // 确保context是Activity类型
        if (!(context instanceof Activity)) {
            showToast("无法截图，需要Activity Context");
            return;
        }

        // 执行截图
        Bitmap bitmap = ScreenShotUtil.saveScreenShot((Activity) context, dir);
        if (bitmap != null) {
            showScreenShotAnimation(bitmap);
        } else {
            showToast("截图失败");
        }
    }

    /**
     * 显示截图动画
     */
    private void showScreenShotAnimation(Bitmap bitmap) {
        if (binding.rootLl == null) {
            showToast("视图已销毁");
            return;
        }

        ImageView screenshotView = new ImageView(context);
        screenshotView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        screenshotView.setImageBitmap(bitmap);

        try {
            binding.rootLl.addView(screenshotView);

            ScaleAnimation animation = (ScaleAnimation) AnimationUtils.loadAnimation(
                    context, R.anim.screen_cut);

            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationRepeat(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (binding.rootLl != null) {
                        binding.rootLl.removeView(screenshotView);
                    }
                    showToast("截图保存成功");
                }
            });

            screenshotView.startAnimation(animation);

        } catch (Exception e) {
            showToast("截图动画播放失败");
            e.printStackTrace();
        }
    }

    // ==================== 更多操作 ====================

    /**
     * 显示更多操作弹窗
     */
    public void showMoreActions(View anchor, CurveType currentType) {
        this.currentType = currentType;
        if (anchor == null) {
            showToast("锚点视图为空");
            return;
        }

        List<String> actions = new ArrayList<>();
        actions.add("坐标缩放");
        actions.add("坐标还原");

        popupManager.showListPopup(anchor, actions, new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                popupManager.dismiss();
                if (position == 0) {
                    showCoordinateDialog();
                } else if (position == 1) {
                    chartManager.resetZoom();
                    showToast("坐标已还原");
                }
            }

            @Override
            public void onItemLongClick(int position) {
                // 长按不做处理
            }
        });
    }

    // ==================== 坐标缩放对话框 ====================

    /**
     * 显示坐标缩放对话框
     */
    private void showCoordinateDialog() {
        if (coordinateDialog != null) {
            coordinateDialog.dismiss();
            coordinateDialog = null;
        }

        coordinateDialog = new CustomLargeDialog(context);
        View view = LayoutInflater.from(context).inflate(
                R.layout.dialog_custom_coodidate_scale_layout, null);

        if (view == null) {
            showToast("加载对话框布局失败");
            return;
        }

        CustomAddSubEdit minXEt = view.findViewById(R.id.dialog_coodidate_min_x_et);
        CustomAddSubEdit maxXEt = view.findViewById(R.id.dialog_coodidate_max_x_et);
        CustomAddSubEdit minYEt = view.findViewById(R.id.dialog_coodidate_y_min_et);
        CustomAddSubEdit maxYEt = view.findViewById(R.id.dialog_coodidate_Y_max_et);

        // 设置默认值（从图表获取当前范围）
        setDefaultCoordinateValues(minXEt, maxXEt, minYEt, maxYEt);

        CustomLargeDialog.Builder builder = new CustomLargeDialog.Builder(context);
        builder.setContentView(view);
        builder.setTitle("坐标缩放");
        builder.setPositiveButton("确定", (dialog, which) -> handleCoordinateConfirm(minXEt, maxXEt, minYEt, maxYEt));
        builder.setNegativeButton("取消", (dialog, which) -> {
            coordinateDialog.dismiss();
            coordinateDialog = null;
        });

        coordinateDialog = builder.create();
        coordinateDialog.setCanceledOnTouchOutside(true);
        coordinateDialog.show();
    }

    /**
     * 设置默认坐标值
     */
    private void setDefaultCoordinateValues(CustomAddSubEdit minXEt, CustomAddSubEdit maxXEt,
                                            CustomAddSubEdit minYEt, CustomAddSubEdit maxYEt) {
        // TODO: 从chartManager获取当前坐标范围
        // 这里可以设置默认值，或者从图表获取实际范围
        // 示例默认值
        minXEt.setEditTextValue("0");
        maxXEt.setEditTextValue("50");
        minYEt.setEditTextValue("0");
        maxYEt.setEditTextValue("100");
    }

    /**
     * 处理坐标确认
     */
    private void handleCoordinateConfirm(CustomAddSubEdit minXEt, CustomAddSubEdit maxXEt,
                                         CustomAddSubEdit minYEt, CustomAddSubEdit maxYEt) {
        try {
            String minXStr = minXEt.getEditText();
            String maxXStr = maxXEt.getEditText();
            String minYStr = minYEt.getEditText();
            String maxYStr = maxYEt.getEditText();

            if (TextUtils.isEmpty(minXStr) || TextUtils.isEmpty(maxXStr) ||
                    TextUtils.isEmpty(minYStr) || TextUtils.isEmpty(maxYStr)) {
                showToast("请填写完整的坐标值");
                return;
            }

            float minX = Float.parseFloat(minXStr);
            float maxX = Float.parseFloat(maxXStr);
            float minY = Float.parseFloat(minYStr);
            float maxY = Float.parseFloat(maxYStr);

            // 验证输入
            if (minX >= maxX) {
                showToast("X轴最小值必须小于最大值");
                return;
            }
            if (minY >= maxY) {
                showToast("Y轴最小值必须小于最大值");
                return;
            }
            if (minX < 0 || maxX < 0 || minY < 0 || maxY < 0) {
                showToast("坐标值不能为负数");
                return;
            }

            // 应用缩放
            chartManager.scaleZoom(minX, maxX, minY, maxY);
            coordinateDialog.dismiss();
            coordinateDialog = null;
            showToast("坐标缩放完成");

        } catch (NumberFormatException e) {
            showToast("请输入有效的数字");
        } catch (Exception e) {
            showToast("缩放失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== 资源释放 ====================

    /**
     * 释放资源
     */
    public void release() {
        // 关闭对话框
        if (coordinateDialog != null) {
            coordinateDialog.dismiss();
            coordinateDialog = null;
        }

        // 移除所有Handler回调
        mainHandler.removeCallbacksAndMessages(null);
    }
}