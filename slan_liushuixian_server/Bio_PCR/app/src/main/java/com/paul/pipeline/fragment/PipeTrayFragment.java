package com.paul.pipeline.fragment;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.databinding.FragmentPipeTrayBinding;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.fragment.viewmodel.PipeTrayViewModel;
import com.paul.pipeline.fragment.model.PipeTrayModel;
import com.paul.pipeline.manager.TrayManager;
import com.paul.pipeline.model.PairHoleModel;
import com.paul.pipeline.model.SampleHoleModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PipeTrayFragment extends Fragment {
    private static final String ARG_PARAM2 = "index";

    public PipeTrayFragment() {
        // Required empty public constructor
    }

    public static PipeTrayFragment newInstance(int index) {
        PipeTrayFragment fragment = new PipeTrayFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM2, index);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            trayindex = getArguments().getInt(ARG_PARAM2);
        }
    }
    PipeTrayViewModel mViewModel;
    FragmentPipeTrayBinding binding;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_pipe_tray, container, false);
        initBinding();
        return binding.getRoot();
//        return inflater.inflate(R.layout.fragment_pipe_tray, container, false);
    }

    private void initBinding() {
        binding.mainbgLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(trayindex==8){
                    mainViewModel.setToastInfo("该位置已调整为盖板位！");
                }else {
                    openTrayDetail(trayindex);
                }
            }
        });
    }
    private void openTrayDetail(int id){
        if(id>=1&&id<=8) {
            mainViewModel.onNavClicked(ConstantData.NAV_TRAYMANAGE);
            mainViewModel.onTrayClicked(id);
        }
    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(PipeTrayViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);
        mainViewModel.getTrayChanged().observe(requireActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if(integer==trayindex){
                    initModel();
                }
            }
        });
        initModel();
        initUIHandler();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        removeUIHandler();
    }

    int trayindex;
    PipeMainViewModel mainViewModel;
    PipeTrayModel model;

    public void initModel(){
        if(trayindex>=1&&trayindex<=8) {
            if(mainViewModel!=null) {
                model = mainViewModel.findPipeTrayModel(getContext(), trayindex);
                Log.i("PipeTrayFragment", "initModel tray=" + trayindex
                        + ", model=" + model
                        + ", holes=" + (model == null ? "null" : model.holes)
                        + ", holes.size=" + (model == null || model.holes == null ? "null" : model.holes.size()));
                binding.setModel(model);
                binding.executePendingBindings();
                initSampleHoleListCycleView();
            }
        }
    }
    public void clearModel(){
        if(mainViewModel!=null) {
            mainViewModel.clearPipeTrayModel(getContext(), trayindex);
        }
    }
    // 不再使用 RecyclerView,改为直接向 LinearLayout 动态添加 view
    // 避免 LayoutManager 方向、wrap_content 高度、stableId 等 RecyclerView 特有坑
    private void initSampleHoleListCycleView(){
        if(model == null || getContext() == null) {
            Log.w("PipeTrayFragment", "initSampleHoleListCycleView skipped: model=" + model + ", ctx=" + getContext());
            return;
        }
        // 防御:若 holes 数据缺失或不足 16 个 (例如 MMKV 反序列化异常),重新 init
        if (model.holes == null || model.holes.size() < 16) {
            Log.w("PipeTrayFragment", "holes 数据异常 (size="
                    + (model.holes == null ? "null" : model.holes.size())
                    + "),重新调用 initModel 重建");
            model.initModel(trayindex, getString(R.string.pipe_main_trayname) + trayindex);
            binding.setModel(model);
            binding.executePendingBindings();
        }
        if (model.holes == null || model.holes.isEmpty()) {
            Log.w("PipeTrayFragment", "init 后 holes 仍为空,跳过");
            return;
        }

//        Log.i("PipeTrayFragment", "initSampleHoleListCycleView holes.size=" + model.holes.size());
        // 把 16 个孔位 (A1-A8, B1-B8) 配对成 8 对,并倒序为 A8/B8 -> A1/B1
        final List<PairHoleModel> pairs = buildPairsReversed(model.holes);
//        Log.i("PipeTrayFragment", "pairs.size=" + pairs.size());

        // 预缓存 Drawable 与像素值
        final Drawable bgStroke = ContextCompat.getDrawable(getContext(), R.drawable.shape_oval_blue_stroke_bg);
        final Drawable bgSolid  = ContextCompat.getDrawable(getContext(), R.drawable.shape_oval_blue_bg);
        final float density = getContext().getResources().getDisplayMetrics().density;
        final int holeSize  = (int)(22 * density);   // 22dp
        final int gap       = (int)(3  * density);   // 3dp
        final int cellMarginEnd = (int)(6 * density);

        // 清空再添加,定时刷新时也会走这里
        binding.sampleholeContainer.removeAllViews();
        int idx = 0;
        for (PairHoleModel pair : pairs) {
            String aText = "?", bText = "?";

            // 纯代码动态创建 cell:LinearLayout(vertical) 内含 B 和 A 两个 TextView
            android.widget.LinearLayout cell = new android.widget.LinearLayout(getContext());
            cell.setOrientation(android.widget.LinearLayout.VERTICAL);
            cell.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            cell.setBackgroundColor(0x22FF0000); // 浅红底,便于看到 cell 边界
            android.widget.LinearLayout.LayoutParams cellLp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            cellLp.setMarginEnd(cellMarginEnd);
            cell.setLayoutParams(cellLp);

            // B TextView (上)
            TextView bTv = new TextView(getContext());
            bTv.setGravity(android.view.Gravity.CENTER);
            bTv.setTextColor(ContextCompat.getColor(getContext(), R.color.content_text));
            bTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                    getContext().getResources().getDimensionPixelSize(R.dimen.pipemain_hole_text));
            if (pair.bHole != null) {
                bText = pair.bHole.name.get();
                bTv.setText(bText);
                bTv.setBackground(pair.bHole.status.get() != null && pair.bHole.status.get() == 0
                        ? bgStroke : bgSolid);
            } else {
                bTv.setText("");
            }
            cell.addView(bTv, new android.widget.LinearLayout.LayoutParams(holeSize, holeSize));

            // A TextView (下)
            TextView aTv = new TextView(getContext());
            aTv.setGravity(android.view.Gravity.CENTER);
            aTv.setTextColor(ContextCompat.getColor(getContext(), R.color.content_text));
            aTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                    getContext().getResources().getDimensionPixelSize(R.dimen.pipemain_hole_text));
            if (pair.aHole != null) {
                aText = pair.aHole.name.get();
                aTv.setText(aText);
                aTv.setBackground(pair.aHole.status.get() != null && pair.aHole.status.get() == 0
                        ? bgStroke : bgSolid);
            } else {
                aTv.setText("");
            }
            android.widget.LinearLayout.LayoutParams aLp = new android.widget.LinearLayout.LayoutParams(holeSize, holeSize);
            aLp.topMargin = gap;
            cell.addView(aTv, aLp);

            //Log.i("PipeTrayFragment", "  pair[" + (idx++) + "] = (" + bText + "/" + aText + ") childCount=" + cell.getChildCount());`
            binding.sampleholeContainer.addView(cell);
        }
//        Log.i("PipeTrayFragment", "sampleholeContainer child count=" + binding.sampleholeContainer.getChildCount()
//                + ", container orientation=" + binding.sampleholeContainer.getOrientation()
//                + ", width=" + binding.sampleholeContainer.getWidth()
//                + ", height=" + binding.sampleholeContainer.getHeight());
    }

    /**
     * 根据孔位的 name (A1~A8, B1~B8) 配对,得到倒序的 8 对 PairHoleModel
     * 顺序: A8+B8, A7+B7, ..., A1+B1
     * 不依赖 holes 列表的实际顺序,避免因 holes 顺序异常导致配对错乱
     */
    private List<PairHoleModel> buildPairsReversed(List<SampleHoleModel> holes) {
        List<PairHoleModel> pairs = new ArrayList<>();
        if (holes == null || holes.isEmpty()) {
            return pairs;
        }
        // 按 name 拆分:A 行 / B 行分别放进 map,key 是数字部分
        java.util.Map<Integer, SampleHoleModel> aMap = new java.util.HashMap<>();
        java.util.Map<Integer, SampleHoleModel> bMap = new java.util.HashMap<>();
        for (SampleHoleModel hole : holes) {
            if (hole == null) continue;
            String name = hole.name.get();
            if (name == null || name.length() < 2) continue;
            char type = name.charAt(0);
            int num;
            try {
                num = Integer.parseInt(name.substring(1));
            } catch (NumberFormatException e) {
                continue;
            }
            if (type == 'A' || type == 'a') {
                aMap.put(num, hole);
            } else if (type == 'B' || type == 'b') {
                bMap.put(num, hole);
            }
        }
        // 倒序配对: A8+B8, A7+B7, ..., A1+B1
        for (int n = 8; n >= 1; n--) {
            SampleHoleModel aHole = aMap.get(n);
            SampleHoleModel bHole = bMap.get(n);
            pairs.add(new PairHoleModel(aHole, bHole));
        }
        return pairs;
    }
   private void refreshModel(){
       if(model!=null) {
           binding.executePendingBindings();
       }
        // 直接重新构建容器,确保最新数据显示
        initSampleHoleListCycleView();
   }
    Timer timer;
    TimerTask timerTask;
    private void initUIHandler() {
        handler = new UIHandler();
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                // 在这里执行你的后台任务
                try{
                    if(handler!=null) {
                        handler.sendEmptyMessage(ConstantData.MSG_UPDATE_UI);
                    }
                } catch (Exception e) {
                    Log.e("ArmTaskWorker", e.getMessage());
                }
                //  Log.i(TAG, "执行刷新");
            }
        };
        timer.schedule(timerTask, 1000L, 1000L);
    }
    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }
    private void removeUIHandler(){
        stopTimer();
        if (null != handler) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    UIHandler handler;
    class UIHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case ConstantData.MSG_UPDATE_UI:
                    // 定时刷新:只更新数据,不重建 Adapter/LayoutManager,避免闪烁
                    refreshModel();
                    break;
            }
        }
    }
}