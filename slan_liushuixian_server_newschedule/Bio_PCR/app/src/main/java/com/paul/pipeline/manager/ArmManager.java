package com.paul.pipeline.manager;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.gidi.bio_pcr.R;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.fragment.model.PipeTrayModel;
import com.paul.pipeline.listener.ArmResetCallback;
import com.paul.pipeline.message.ArmworkMessage;
import com.paul.pipeline.message.ResultMessage;
import com.paul.pipeline.model.SampleHoleModel;
import com.paul.pipeline.utils.MmkvUtils;
import com.paul.pipeline.listener.ArmFinishCallback;
import com.paul.pipeline.fragment.model.PipeArmModel;
import com.winters.boweilibrary.autopipeline.model.item.WSSConnectionItem;
import com.winters.boweilibrary.autopipeline.worker.RobotWorkManager;
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_Robot;
import com.winters.boweilibrary.autopipeline.model.item.ItemProperty;
import com.winters.boweilibrary.autopipeline.model.item.QRCodeItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class ArmManager {
    private static final String TAG = "ArmManager";
    private static ArmManager mInstance;
    public static ArmManager getInstance(){
        if (null == mInstance) {
            synchronized (ArmManager.class){
                if (null == mInstance){
                    mInstance = new ArmManager();
                }
            }
        }
        return  mInstance;
    }
    public ArmManager(){

    }

    public List<ItemProperty> itemPropertyList = new ArrayList<>();//机械臂对象
    //    public List<QRCodeItem> qRCodeItems = new ArrayList<>();
    public ItemProperty itemProperty_source;//源位置
    public ItemProperty itemProperty_target;//目标位置

    public void init(Context context){
        itemPropertyList = SerialPortManager_Robot.getInstance().getItemPropertyList();
        initArmModel(context);
        RobotWorkManager.getInstance().setArmHoldingListener(new RobotWorkManager.ArmHoldingListener() {
            @Override
            public void onArmHoldingChanged(int kind, String type, int position, String reason) {
                switch (kind) {
                    case RobotWorkManager.HOLD_ITEM:
                        updateHolding(type, position, reason);
                        break;
                    case RobotWorkManager.HOLD_EMPTY:
                        updateHolding(HOLDING_EMPTY, 0, reason);
                        break;
                    case RobotWorkManager.HOLD_KEEP:
                        // 放失败手上仍有东西：原来是确定的物品就保持，否则仍为未知
                        if (getHoldingType() == null || HOLDING_EMPTY.equals(getHoldingType())) {
                            updateHolding(HOLDING_UNKNOWN, 0, reason);
                        } else {
                            Log.e(TAG, "holding 保持 " + getHoldingType() + getHoldingPosition() + " : " + reason);
                        }
                        break;
                    default:
                        updateHolding(HOLDING_UNKNOWN, 0, reason);
                        break;
                }
            }
        });
        RobotWorkManager.getInstance().setWorkQueueListener(new RobotWorkManager.WorkQueueListener() {
            @Override
            public void onAllTasksCompleted(String text, long chainTag) {
                updateFinish(1,true);
                ChainContext ctx = takeChainContext(chainTag);
                if (ctx == null) {
                    // 不是经 ArmManager 启动的链（如测试界面）：不调用任何回调，避免误调别人的回调
                    Log.w("ArmTaskWorker", "onAllTasksCompleted 无回调上下文 tag:" + chainTag + " " + text);
                    return;
                }
                if (ctx.reset) {
                    if (ctx.resetCallback != null) {
                        ctx.resetCallback.finish(true, ctx.name);
                    }
                } else {
                    Log.e("ArmTaskWorker", "arm1 callback :true " + ctx.name);
                    if (ctx.finishCallback != null) {
                        ctx.finishCallback.finish(1, ctx.target, ctx.source);
                    }
                    setFree();
                }
            }

            @Override
            public void onTaskFailed(String text, long chainTag, String failedStep) {
                ChainContext ctx = takeChainContext(chainTag);
                if (ctx == null) {
                    Log.w("ArmTaskWorker", "onTaskFailed 无回调上下文 tag:" + chainTag + " " + text);
                    return;
                }
                if (ctx.reset) {
                    if (ctx.resetCallback != null) {
                        ctx.resetCallback.finish(false, ctx.name);
                    }
                } else {
                    Log.e("ArmTaskWorker", "arm1 callback :false " + ctx.name);
                    if (ctx.resumable && failedStep != null) {
                        // 记住失败的那条链和失败的那一步，"异常继续"从这里恢复并沿用它的回调。
                        // 单动作（放盖/放盘/开关门）不可继续，不会覆盖这里
                        ctx.failedStep = failedStep;
                        lastFailedResumable = ctx;
                        Log.e(TAG, "记录可继续的失败步骤: " + failedStep);
                    }
                    if (ctx.finishCallback != null) {
                        // 文字沿用原来的模板名（异常弹窗显示用），该模板名已按链固定，不会被其他链改写
                        ctx.finishCallback.fail(1, ctx.target, ctx.source, text != null ? text : ctx.name);
                    }
                }
            }

            @Override
            public void onQrCodeRevert(List<QRCodeItem> qRCodeItems, int panNum) {
                if(qRCodeItems!=null&&qRCodeItems.size()!=0) {
                    checkHoles(panNum,qRCodeItems);
                }
            }
        });
    }

    public void clearData(Context context){
        if(armlist!=null) {
            synchronized(armlist) {
                for (int i = 0; i < armlist.size(); ++i) {
                    PipeArmModel model = armlist.get(i);
                    if (model != null) {
                        Integer index = model.index.get();
                        if (index != null) {
                            model.initModel(index, context.getString(R.string.pipe_main_arm_name));
                        }
                    }
//                armlist.set(i, newModel(context, index));
                }
            }
        }
    }
    public void initArmModel(Context context){
        ArrayList<PipeArmModel> list = new ArrayList<>();
        list.add(initPipeArmModel(context,1));
        setArmlist(list);
    }

    private PipeArmModel initPipeArmModel(Context context, int index){
        PipeArmModel model = MmkvUtils.getObject(ConstantData.nameARM+index,PipeArmModel.class);
        if(model==null) {
            model = newModel(context, index);
        } else {
            // 实时连接/状态不持久化恢复：重启后一律重置为未连接+异常，
            // 等设备真实上报后再刷新，避免自检页面显示过期的“已连接/正常/完成”绿色
            model.connect.set(false);
            model.status.set(ConstantData.str_ERROR);
            model.exist.set(false);
        }
        model.finish.set(false);
        return model;
    }

    public void saveModel(int index){
        if(armlist!=null) {
            synchronized(armlist) {
                for (int i = 0; i < armlist.size(); ++i) {
                    PipeArmModel model = armlist.get(i);
                    if (model != null) {
                        Integer modelIndex = model.index.get();
                        if (modelIndex != null && index == modelIndex) {
                            MmkvUtils.saveObject(ConstantData.nameARM + index, model);
                        }
                    }
                }
            }
        }
    }
    private PipeArmModel newModel(Context context, int index){
        PipeArmModel model = new PipeArmModel(index,context.getString(R.string.pipe_main_arm_name));
        model.finish.set(true);
        return model;
    }
    public void refreshConnectinfo(){
        synchronized(armlist) {
            for (int i = 0; i < armlist.size(); i++) {
                PipeArmModel model = armlist.get(i);
                if (model != null) {
                    Integer modelIndex = model.index.get();
                    if (modelIndex != null && modelIndex == 1) {
                        refreshModel(model);
                    }
                }
            }
        }
    }

    public PipeArmModel refreshModel(PipeArmModel model){
        SerialPortManager_Robot.RobotStatus status = SerialPortManager_Robot.getInstance().getRobotStatus();
        if(status!=null) {
//            Log.e("ArmTaskWorker", "机械臂 connect:"+status.isROBOTconnect());
//            Log.e("ArmTaskWorker", "机械臂 status:"+status.getStatus());
            model.exist.set(status.getSenseByKey("ARM"));
            model.status.set(status.getStatus());
            model.connect.set(status.isROBOTconnect());
            model.degree.set("" + status.getDegree());
            model.no.set(status.getNO());
            model.motorID.set(status.getMotorID());
            model.taskRunning.set(status.isROBOTTaskRunning() == true ? "运行中" : "空闲");
            model.alarm.set(status.getAlarm());
            if(itemProperty_source!=null){
                model.source.set(itemProperty_source.getType()+itemProperty_source.getPosition());
            }else {
                model.source.set("无");
            }
            if(itemProperty_target!=null){
                model.target.set(itemProperty_target.getType()+itemProperty_target.getPosition());
            }else {
                model.target.set("无");
            }
        }else{
            model.exist.set(false);
            model.connect.set(false);
            model.source.set("");
            model.target.set("");
        }
        return model;
    }
    private final List<PipeArmModel> armlist = Collections.synchronizedList(new ArrayList<>());
    public ArrayList<PipeArmModel> getArmlist() {
        synchronized(armlist) {
            return new ArrayList<>(armlist);
        }
    }

    public void setArmlist(ArrayList<PipeArmModel> list) {
        synchronized(armlist) {
            this.armlist.clear();
            if(list != null) {
                this.armlist.addAll(list);
            }
        }
    }

    public PipeArmModel getArmModel(int index){
        synchronized(armlist) {
            for (int i = 0; i < armlist.size(); i++) {
                PipeArmModel model = armlist.get(i);
                if (model != null) {
                    Integer modelIndex = model.index.get();
                    if (modelIndex != null && modelIndex == index) {
                        return model;
                    }
                }
            }
        }
        return null;
    }

    public void updateArmworkMessage(int index, ArmworkMessage value){
        synchronized(armlist) {
            for (int i = 0; i < armlist.size(); i++) {
                PipeArmModel model = armlist.get(i);
                if (model != null) {
                    Integer modelIndex = model.index.get();
                    if (modelIndex != null && modelIndex == index) {
                        model.currentArmworkMessage.set(value);
                        saveModel(1);
                        break;
                    }
                }
            }
        }
    }
    public void updateAction(int index,String value){
        synchronized(armlist) {
            for (int i = 0; i < armlist.size(); i++) {
                PipeArmModel model = armlist.get(i);
                if (model != null) {
                    Integer modelIndex = model.index.get();
                    if (modelIndex != null && modelIndex == index) {
                        model.action.set(value);
                        trackPcrCoverSource(model, value);
                        break;
                    }
                }
            }
        }
    }

    /**
     * 根据机械臂动作串追踪遮光盖来源PCR工位，用于一键还盘时按来源归还。
     * 动作串形如 "put_pcr_cover_to_temp:PAN3:PCR4" / "get_pcr_cover_from_pcr:PCR4:PAN3"，
     * 其中大写的 PCR&lt;n&gt; 即遮光盖的归属工位(小写的 pcr_cover 前缀不会匹配)。
     * 抓盖到暂存位(put_pcr_cover_to_temp)时记录来源；盖子放回PCR(put_pcr_cover_to_pcr)时清空。
     */
    private void trackPcrCoverSource(PipeArmModel model, String action){
        if(model == null || action == null){
            return;
        }
        // 仅记录来源；清空由 clearPcrCoverSource() 在"放盖成功"后调用，
        // 避免 put 刚入队/运行(非成功)时就清空，导致失败重试时丢失来源
        if(action.contains("put_pcr_cover_to_temp") || action.contains("get_pcr_cover_from_pcr")){
            int position = parsePcrCoverPosition(action);
            if(position >= 1 && position <= 6){
                Integer old = model.pcrCoverSource.get();
                if(old == null || old != position){
                    model.pcrCoverSource.set(position);
                    saveModel(1);
                    Log.e(TAG, "trackPcrCoverSource 记录遮光盖来源 PCR" + position + " : " + action);
                }
            }
        }
    }

    /** 遮光盖成功放回PCR工位后清空来源记录，避免下次误用过期来源。 */
    public void clearPcrCoverSource(){
        PipeArmModel model = getArmModel(1);
        if(model != null && model.pcrCoverSource.get() != null){
            model.pcrCoverSource.set(null);
            saveModel(1);
            Log.e(TAG, "clearPcrCoverSource 清空遮光盖来源");
        }
    }

    /** 从动作串里解析大写 PCR&lt;n&gt; 工位号，解析失败返回0。 */
    private int parsePcrCoverPosition(String action){
        try {
            java.util.regex.Matcher matcher =
                    java.util.regex.Pattern.compile("PCR(\\d+)").matcher(action);
            if(matcher.find()){
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            Log.e(TAG, "parsePcrCoverPosition : " + e.getMessage());
        }
        return 0;
    }

    /** 获取当前记录的遮光盖来源PCR工位(1-6)，无记录返回0。 */
    public int getPcrCoverSource(){
        PipeArmModel model = getArmModel(1);
        if(model != null && model.pcrCoverSource.get() != null){
            return model.pcrCoverSource.get();
        }
        return 0;
    }

    // ==== 机械臂手上物品 ====
    public static final String HOLDING_EMPTY = "EMPTY";
    public static final String HOLDING_UNKNOWN = "UNKNOWN";

    /** 更新手上物品记录并持久化（主线程调用） */
    public void updateHolding(String type, int position, String reason){
        PipeArmModel model = getArmModel(1);
        if(model == null){
            return;
        }
        model.holdingType.set(type);
        model.holdingPosition.set(position);
        saveModel(1);
        Log.e(TAG, "holding → " + type + (position > 0 ? String.valueOf(position) : "") + " : " + reason);
    }

    /** 手上物品类型：EMPTY / UNKNOWN / PAN / RNA / PCR / PCR_COVER；没有记录（老数据）返回 UNKNOWN */
    public String getHoldingType(){
        PipeArmModel model = getArmModel(1);
        if(model == null || model.holdingType.get() == null){
            return HOLDING_UNKNOWN;
        }
        return model.holdingType.get();
    }

    public int getHoldingPosition(){
        PipeArmModel model = getArmModel(1);
        if(model == null || model.holdingPosition.get() == null){
            return 0;
        }
        return model.holdingPosition.get();
    }

    /** 手上是遮光盖（PCR_COVER） */
    public boolean isHoldingCover(){
        return "PCR_COVER".equals(getHoldingType());
    }

    /** 手上是托盘（从料盘/核提/PCR 取出的盘） */
    public boolean isHoldingTray(){
        String t = getHoldingType();
        return "PAN".equals(t) || "RNA".equals(t) || "PCR".equals(t);
    }
    public void updateFinish(int index,boolean value){
        synchronized(armlist) {
            for (int i = 0; i < armlist.size(); i++) {
                PipeArmModel model = armlist.get(i);
                if (model != null) {
                    Integer modelIndex = model.index.get();
                    if (modelIndex != null && modelIndex == index) {
                        model.finish.set(value);
                        break;
                    }
                }
            }
        }
    }
    public void updateTray(int index,int tray){
        synchronized(armlist) {
            for (int i = 0; i < armlist.size(); i++) {
                PipeArmModel model = armlist.get(i);
                if (model != null) {
                    Integer modelIndex = model.index.get();
                    if (modelIndex != null && modelIndex == index) {
                        model.tray.set(tray);
                        break;
                    }
                }
            }
        }
    }
    public void updateArmwork(int index,int value){
        synchronized(armlist) {
            for (int i = 0; i < armlist.size(); i++) {
                PipeArmModel model = armlist.get(i);
                if (model != null) {
                    Integer modelIndex = model.index.get();
                    if (modelIndex != null && modelIndex == index) {
                        model.armwork.set(value);
                        break;
                    }
                }
            }
        }
    }
    public void setSource(int type,int index){
        switch(type){
            case ConstantData.DEVICE_TRAY:
                itemProperty_source = getItemPropertyTray(index);
                saveSource();
                break;
            case ConstantData.DEVICE_RNA:
                itemProperty_source = getItemPropertyRNA(index);
                saveSource();
                break;
            case ConstantData.DEVICE_PCR:
                itemProperty_source = getItemPropertyPCR(index);
                saveSource();
                break;
            default:
                itemProperty_source = null;
                saveSource();
                break;
        }
    }
    public void setTarget(int type,int index){
        switch(type){
            case ConstantData.DEVICE_TRAY:
                itemProperty_target = getItemPropertyTray(index);
                saveTarget();
                break;
            case ConstantData.DEVICE_RNA:
                itemProperty_target = getItemPropertyRNA(index);
                saveTarget();
                break;
            case ConstantData.DEVICE_PCR:
                itemProperty_target = getItemPropertyPCR(index);
                saveTarget();
                break;
            default:
                itemProperty_target = null;
                saveTarget();
                break;
        }
    }
    private void saveSource(){
        PipeArmModel model = getArmModel(1);
        if(model!=null) {
            model.source.set(itemProperty_source != null ? itemProperty_source.getType() + itemProperty_source.getPosition() : "");
            if(itemProperty_source != null) {
                model.sourcePosition.set( itemProperty_source.getPosition());
            }
        }
    }
    private void saveTarget(){
        PipeArmModel model = getArmModel(1);
        if(model!=null) {
            model.target.set(itemProperty_target != null ? itemProperty_target.getType() + itemProperty_target.getPosition() : "");
            if(itemProperty_target != null) {
                model.targetPosition.set( itemProperty_target.getPosition());
            }
        }
    }

    public ItemProperty getItemPropertyTray(int index){
        itemPropertyList = SerialPortManager_Robot.getInstance().getItemPropertyList();
        if(itemPropertyList!=null) {
            for (ItemProperty item : itemPropertyList) {
                if (item.getType().equalsIgnoreCase(ConstantData.nameTray)) {
                    if (item.getPosition() == index) {
                        return item;
                    }
                }
            }
        }
        return null;
    }

    public ItemProperty getItemPropertyRNA(int index){
        itemPropertyList = SerialPortManager_Robot.getInstance().getItemPropertyList();
        if(itemPropertyList!=null) {
            for (ItemProperty item : itemPropertyList) {
                if (item.getType().equalsIgnoreCase(ConstantData.nameRNA)) {
                    if (item.getPosition() == index) {
                        return item;
                    }
                }
            }
        }
        return null;
    }
    public ItemProperty getItemPropertyPCR(int index){
        itemPropertyList = SerialPortManager_Robot.getInstance().getItemPropertyList();
        if(itemPropertyList!=null) {
            for (ItemProperty item : itemPropertyList) {
                if (item.getType().equalsIgnoreCase(ConstantData.namePCR)) {
                    if (item.getPosition() == index) {
                        return item;
                    }
                }
            }
        }
        return null;
    }
    public ItemProperty getItemPropertyPCRCover(int index){
        itemPropertyList = SerialPortManager_Robot.getInstance().getItemPropertyList();
        if(itemPropertyList!=null) {
            for (ItemProperty item : itemPropertyList) {
                if (item.getType().equalsIgnoreCase(ConstantData.namePCRCover)) {
                    if (item.getPosition() == index) {
                        return item;
                    }
                }
            }
        }
        return null;
    }

    public void setFree(){
        itemProperty_source = null;
        itemProperty_target = null;
    }

    /** 已不用于任务链回调（回调绑定在 ChainContext 上），仅保留兼容 */
    ArmFinishCallback armFinishCallback;

    public void setArmFinishCallback(ArmFinishCallback armFinishCallback) {
        this.armFinishCallback = armFinishCallback;
    }
    /** 已不用于复位链回调（回调绑定在 ChainContext 上），仅 config() 仍设置，保留兼容 */
    ArmResetCallback armResetCallback;

    public void setArmResetCallback(ArmResetCallback armResetCallback) {
        this.armResetCallback = armResetCallback;
    }

    // ==== 回调与任务链绑定 ====
    /** 一条任务链启动时的回调和源/目标，链结束时用它自己的，不受之后其他调用覆盖 */
    private static class ChainContext {
        final String name;
        final ArmFinishCallback finishCallback;
        final ArmResetCallback resetCallback;
        final ItemProperty source;
        final ItemProperty target;
        final boolean reset;
        /** 失败后可"异常继续"（取放托盘的整条链） */
        final boolean resumable;
        /** 失败的那一步（如 put_pcr_pan_to_pan:PCR6:PAN2），链失败时填入 */
        String failedStep;

        ChainContext(String name, ArmFinishCallback finishCallback, ArmResetCallback resetCallback,
                     ItemProperty source, ItemProperty target, boolean reset, boolean resumable) {
            this.name = name;
            this.finishCallback = finishCallback;
            this.resetCallback = resetCallback;
            this.source = source;
            this.target = target;
            this.reset = reset;
            this.resumable = resumable;
        }
    }

    private long chainSeq = 0;
    /** tag -> 上下文；被复位取消的链不会回调，保留少量后自动淘汰 */
    private final java.util.LinkedHashMap<Long, ChainContext> chainContexts =
            new java.util.LinkedHashMap<Long, ChainContext>() {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<Long, ChainContext> eldest) {
                    return size() > 16;
                }
            };
    /** 最近一次失败、可继续的链 */
    private ChainContext lastFailedResumable;

    /** 在调用 RobotWorkManager start/resume 之前调用，返回本条链的 tag */
    private long beginChain(ChainContext ctx){
        long tag = ++chainSeq;
        synchronized (chainContexts) {
            chainContexts.put(tag, ctx);
        }
        RobotWorkManager.getInstance().setPendingChainTag(tag);
        return tag;
    }

    /** start/resume 返回后调用；没启动成功则丢弃上下文 */
    private void endChainStart(long tag, boolean started){
        RobotWorkManager.getInstance().clearPendingChainTag();
        if(!started){
            synchronized (chainContexts) {
                chainContexts.remove(tag);
            }
        }
    }

    private ChainContext takeChainContext(long tag){
        if(tag <= 0){
            return null;
        }
        synchronized (chainContexts) {
            return chainContexts.remove(tag);
        }
    }

    /**
     * "继续"前检查：要恢复的那一步执行前手上应有/应空，与 ARM 传感器对不上则拒绝。
     * 防止复位后人工取走或放上东西，再继续时机械臂空跑或带着东西去抓取。
     * @param step 失败步骤的动作名（如 put_pcr_pan_to_pan）
     * @return null 表示可以继续，否则为拒绝原因
     */
    private String checkResumeHandState(String step){
        if(step == null){
            return null;
        }
        Boolean expectHolding;
        if(step.startsWith("get_")){
            expectHolding = false;
        }else if(step.startsWith("put_") || step.startsWith("scan_qrcode")){
            expectHolding = true;
        }else{
            // open_rna_pan / close_rna_pan：拿着盘还是空手取决于链方向，不检查
            return null;
        }

        SerialPortManager_Robot.RobotStatus status = SerialPortManager_Robot.getInstance().getRobotStatus();
        if(status == null){
            return "读取不到机械臂传感器，无法确认手上状态，请人工处理";
        }
        Boolean arm = status.getSenseByKey("ARM");
        boolean holding = arm != null && arm;
        if(expectHolding && !holding){
            return "机械臂状态已变化：要继续的步骤（" + step + "）需要手上有东西，但传感器显示手上为空，请人工处理";
        }
        if(!expectHolding && holding){
            return "机械臂状态已变化：要继续的步骤（" + step + "）需要手上为空，但传感器显示手上有东西，请人工处理";
        }

        // 手上物品记录明确时，再核对类型（盖子/托盘）；记录为未知时只看传感器
        if(expectHolding){
            String type = getHoldingType();
            boolean known = !HOLDING_UNKNOWN.equals(type) && !HOLDING_EMPTY.equals(type);
            if(known){
                boolean needCover = step.contains("pcr_cover");
                if(needCover && !isHoldingCover()){
                    return "机械臂状态已变化：要继续的步骤（" + step + "）需要手上是盖子，但记录为 " + type + getHoldingPosition() + "，请人工处理";
                }
                if(!needCover && !isHoldingTray()){
                    return "机械臂状态已变化：要继续的步骤（" + step + "）需要手上是托盘，但记录为 " + type + getHoldingPosition() + "，请人工处理";
                }
            }
        }
        return null;
    }

    /** 清除可继续的失败记录（机械臂已开始别的任务，旧的失败步骤不再代表当前状态） */
    public void clearResumableFailure(String reason){
        if(lastFailedResumable != null){
            Log.e(TAG, "清除可继续的失败步骤 " + lastFailedResumable.failedStep + " : " + reason);
            lastFailedResumable = null;
        }
    }

    /** 单动作（放盘/放盖/开关核提门）：回调绑定到本条链 */
    private boolean startSingleChain(String action, ItemProperty source, ItemProperty target, ArmFinishCallback finishCallback){
        long tag = beginChain(new ChainContext(action, finishCallback, null, source, target, false, false));
        boolean started = false;
        try {
            started = RobotWorkManager.getInstance().startSingleAcion(source, target, action);
        } finally {
            endChainStart(tag, started);
        }
        return started;
    }

    public String startGet(int type,int index) {
//        if(armCanuse()) {
            setSource(type,index);
            if (itemProperty_source == null) {
                return "请选择源位置或者目标位置";
            } else {
                try {
                    updateArmwork(1,ConstantData.ARM_GET);
                    saveModel(1);
                    // 手动取不经过 RobotWorker 传感器校验，手上物品记为未知
                    updateHolding(HOLDING_UNKNOWN, 0, "手动取 " + itemProperty_source.getType() + itemProperty_source.getPosition());
                    SerialPortManager_Robot.getInstance().robotGet(itemProperty_source);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("ArmTaskWorker", "startGet : " + e.getMessage());
                    return "startGet : " + e.getMessage();
                }
            }
            return null;
//        }else{
//            return "机械臂忙！";
//        }
    }

    public String startPut(int type,int index) {
            setTarget(type, index);
            if (itemProperty_target == null) {
                return "请选择源位置或者目标位置";
            } else {
                try {
                    updateArmwork(1,ConstantData.ARM_PUT);
                    saveModel(1);
                    // 手动放不经过 RobotWorker 传感器校验，手上物品记为未知
                    updateHolding(HOLDING_UNKNOWN, 0, "手动放 " + itemProperty_target.getType() + itemProperty_target.getPosition());
                    SerialPortManager_Robot.getInstance().robotPut(itemProperty_target);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("ArmTaskWorker", "stop : " + e.getMessage());
                    return "stop : " + e.getMessage();
                }
            }
            return null;
    }
    public void stop() {
        try {
            updateArmwork(1,ConstantData.ARM_STOP);
            saveModel(1);
            SerialPortManager_Robot.getInstance().robotStop();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ArmTaskWorker", "stop : " + e.getMessage());
        }
    }

    public String lastAction;
    public String lastDetail;
    ///异常继续
    public String errorContinue(){

        String busy = RobotWorkManager.getInstance().getRobotBusyReason();
        if(busy != null){
            Log.e(TAG, "errorContinue 拒绝：" + busy);
            return "机械臂运行中（" + busy + "），请等待当前动作结束后再继续";
        }

        // 要恢复的步骤来自失败的那条链（不读全局 lastAction：它会被其他动作覆盖且从不清除）
        final ChainContext prev = lastFailedResumable;
        if(prev == null || prev.failedStep == null){
            return "没有可继续的失败动作（已继续过，或机械臂已执行其他任务/一键还盘）";
        }
        final String resumeStep = prev.failedStep;
        String[] actions = resumeStep.split(":");
        if(actions.length < 3){
            return "动作数据错误:" + resumeStep;
        }

        String handError = checkResumeHandState(actions[0]);
        if(handError != null){
            Log.e(TAG, "errorContinue 拒绝：" + handError + " step:" + resumeStep);
            return handError;
        }

        if(itemPropertyList==null){
            return "设备数据为空";
        }
        ItemProperty itemProperty_temp_source = null;

        ItemProperty itemProperty_temp_target = null;

        for(ItemProperty itemProperty : itemPropertyList){
            if((itemProperty.getType() + itemProperty.getPosition()).equals(actions[1])){
                itemProperty_temp_source = itemProperty;
            }
        }

        for(ItemProperty itemProperty : itemPropertyList){
            if((itemProperty.getType() + itemProperty.getPosition()).equals(actions[2])){
                itemProperty_temp_target = itemProperty;
            }
        }

        Log.i(TAG, "resumeRobotWork : actions[1] " + actions[1] );
        Log.i(TAG, "resumeRobotWork : actions[2] " + actions[2] );

        if(itemProperty_temp_source == null || itemProperty_temp_target == null){
            return "resumeRobotWork : error";
        }

        // 盘号取自要恢复的那条动作，不能用全局 itemProperty_source/target（可能已被其他任务改写）
        int panNum = 0;
        if(itemProperty_temp_source.getType().equals("PAN")){
            panNum = itemProperty_temp_source.getPosition();
        }else if(itemProperty_temp_target.getType().equals("PAN")){
            panNum = itemProperty_temp_target.getPosition();
        }
        ArrayList<SampleHoleModel> samples = TrayManager.getInstance().getSampleList(panNum);
        List<QRCodeItem> qRCodeItems = getQRCodeItems(samples);
        boolean result = false;
        try {
            Boolean prevFinish = getArmModel(1) != null ? getArmModel(1).finish.get() : null;
            updateFinish(1,false);
            updateArmwork(1,ConstantData.ARM_CONTINUE);
            saveModel(1);
            // 沿用失败那条链的回调（调度器的），不用全局最后设置的回调
            long tag = beginChain(new ChainContext("continue:" + resumeStep, prev.finishCallback, null,
                    itemProperty_temp_source, itemProperty_temp_target, false, true));
            try {
                result = RobotWorkManager.getInstance().resumeRobotWork(itemProperty_temp_source, itemProperty_temp_target, qRCodeItems, panNum, resumeStep);
            } finally {
                endChainStart(tag, result);
            }
            if (result) {
                // 防止重复继续；继续这条链如果又失败，会重新记录新的失败步骤
                clearResumableFailure("继续已下发");
            }
            if (!result) {
                updateFinish(1, prevFinish != null && prevFinish);
                Log.i(TAG, "resumeRobotWork : error");
                return "resumeRobotWork : error";
            } else {
                // 回调已绑定到本条链（ChainContext）；全局 source/target 仅保持与界面显示一致
                itemProperty_source = itemProperty_temp_source;
                itemProperty_target = itemProperty_temp_target;
                ArmworkMessage msg = getArmModel(1).currentArmworkMessage.get();
                if (msg != null) {
                    //再试一次
                    TrayManager.getInstance().updateDsiapatchStatus(panNum, msg.getDispatchindex(), ConstantData.STATUS_RUN);
                }

                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ArmTaskWorker", "robotReset : " + e.getMessage());
            return "Exception:"+e.getMessage();
        }
    }
    /**
     * 舱门是否处于打开状态。
     * 传感器 "DOOR" 为 true 表示门已到位（关闭），false 表示门已打开。
     * 读不到状态时按“未打开”处理，避免影响正常流程。
     */
    public boolean isDoorOpen() {
        try {
            SerialPortManager_Robot.RobotStatus status = SerialPortManager_Robot.getInstance().getRobotStatus();
            if (status == null) {
                return false;
            }
            Boolean closed = status.getSenseByKey("DOOR");
            return closed != null && !closed;
        } catch (Exception e) {
            Log.e(TAG, "isDoorOpen: " + e.getMessage());
            return false;
        }
    }

    //异常复位
    // 机械臂复位是安全动作（归位到原点，不取放盘），一键复位需要可用，不做关门判断
    public boolean robotReset(ArmResetCallback resetCallback) {
        boolean result = false;
        try {
            updateArmwork(1,ConstantData.ARM_RESET);
            // 复位完成前机械臂不可用；完成后由 onAllTasksCompleted 置 finish=true（失败则保持占用，需再次复位）
            updateFinish(1,false);
            saveModel(1);
//            result = SerialPortManager_Robot.getInstance().robotReset();
            long tag = beginChain(new ChainContext("reset_robot", null, resetCallback, null, null, true, false));
            try {
                result = RobotWorkManager.getInstance().startSingleReset();
            } finally {
                endChainStart(tag, result);
            }
            if (result) {
                Log.i("ArmTaskWorker", "robotReset : " + result);
            } else {
                Log.e("ArmTaskWorker", "robotReset : " + result);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ArmTaskWorker", "robotReset : " + e.getMessage());
            return false;
        }
    }
    //复位弃用
//    public boolean reset() {
//        boolean result = SerialPortManager_Robot.getInstance().robotReset();
//        return result;
//    }
    private List<QRCodeItem> getQRCodeItems(ArrayList<SampleHoleModel> samples){
        List<QRCodeItem> qrlist = new ArrayList<>();
        if(samples!=null) {
            for (int i = 0; i < samples.size(); i++) {
                SampleHoleModel model = samples.get(i);
                QRCodeItem item = new QRCodeItem();
                item.setIndex(model.index.get());
                item.setQrcode(model.scaninfo.get());
                qrlist.add(item);
            }
        }
        return qrlist;
    }

    private ArrayList<SampleHoleModel> checkHoles(int tray,List<QRCodeItem> qrlist){
        boolean flag = false;
        PipeArmModel armmodel = getArmModel(1);
        if(armmodel!=null){
            flag = armmodel.autosamplecode.get();
        }
        ArrayList<SampleHoleModel> holelist = TrayManager.getInstance().getSampleList(tray);
        if(flag) {
            if(qrlist!=null) {
                for (int i = 0; i < qrlist.size(); i++) {
                    QRCodeItem item = qrlist.get(i);
                    String code = item.getQrcode();
                    int itemindex = item.getIndex();
                    if (code != null && !code.isBlank() && !code.isEmpty()) {
                        for (int j = 0; j < holelist.size(); j++) {
                            SampleHoleModel model = holelist.get(j);
                            if (itemindex == model.index.get()) {
                                if (!code.equalsIgnoreCase(model.scaninfo.get())) {
                                    model.scaninfo.set(code);
                                }
                            }
                        }
                    }
                }
            }
        }
        return holelist;
    }

    public String startArmRobot(int source,int sourcetype,int target,int targettype,ArmFinishCallback finishCallback,int tray) {
        if (isDoorOpen()) {
            Log.e("ArmTaskWorker", "startArmRobot: 舱门已打开，禁止机械臂动作");
            return "舱门已打开，请先关门后再操作机械臂！";
        }
        if(armCanuse()) {
            setSource (sourcetype,source);
            setTarget(targettype,target);
            if(itemProperty_source == null ){
                return "请选择源位置或者目标位置";
            }
            if(itemProperty_target == null){
                return "请选择源位置或者目标位置";
            }
            if(itemProperty_source.getType().equals("PAN")&&itemProperty_target.getType().equals("PAN")){
                return "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合";
            }
            if(itemProperty_source.getType().equals("PCR")&&itemProperty_target.getType().equals("PCR")){
                return "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合";
            }
            if(itemProperty_source.getType().equals("RNA")&&itemProperty_target.getType().equals("RNA")){
                return "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合";
            }
//            if(itemProperty_source.getType().equals("RNA")&&itemProperty_target.getType().equals("PCR")){
//                return "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合";
//            }
//            if(itemProperty_source.getType().equals("PCR")&&itemProperty_target.getType().equals("RNA")){
//                return "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合";
//            }
            try {
                int panNum = 0;
                if(itemProperty_source.getType().equals("PAN")){
                    panNum = itemProperty_source.getPosition();
                }else if(itemProperty_target.getType().equals("PAN")){
                    panNum = itemProperty_target.getPosition();
                }else{
                    panNum = tray;
                }
                updateTray(1,tray);//记住盘来源
                ArrayList<SampleHoleModel> samples = TrayManager.getInstance().getSampleList(panNum);
                List<QRCodeItem> qRCodeItems = getQRCodeItems(samples);
                updateFinish(1,false);
                updateArmwork(1,ConstantData.ARM_START);
                saveModel(1);
                ItemProperty chainSource = itemProperty_source;
                ItemProperty chainTarget = itemProperty_target;
                long tag = beginChain(new ChainContext("startArmRobot:" + chainSource.getType() + chainSource.getPosition()
                        + ":" + chainTarget.getType() + chainTarget.getPosition(),
                        finishCallback, null, chainSource, chainTarget, false, true));
                boolean result = false;
                try {
                    result = RobotWorkManager.getInstance().startRobotWork(chainSource, chainTarget, qRCodeItems,panNum);
                } finally {
                    endChainStart(tag, result);
                }
                if (!result) {
                    updateFinish(1,true);
                    Log.e("ArmTaskWorker", "startRobotWork : error");
                    return "startRobotWork : error";
                } else {
                    clearResumableFailure("新的取放托盘任务已启动 " + chainSource.getType() + chainSource.getPosition()
                            + ":" + chainTarget.getType() + chainTarget.getPosition());
                    Log.i("ArmTaskWorker", "startRobotWork : succeed");
                    return null;
                }
            }catch (Exception e){
                e.printStackTrace();
                Log.e("ArmTaskWorker", "startRobotWork : " + e.getMessage());
                return "startRobotWork : exception";
            }
        }else{
            return "机械臂忙！";
        }
    }

    public boolean putTray(int index,ArmFinishCallback finishCallback){
        if (isDoorOpen()) {
            Log.e("ArmTaskWorker", "putTray: 舱门已打开，禁止机械臂动作");
            return false;
        }
        try{
            itemProperty_source = null;
            itemProperty_target = getItemPropertyTray(index);
            if(itemProperty_target==null){
                Log.e(TAG, "putTray 找不到目标 tray"+index);
                return false;
            }
            updateFinish(1, false);
            updateArmwork(1,ConstantData.ARM_PUTTRAY);
            saveModel(1);
            boolean result = startSingleChain("put_to_pan", itemProperty_source, itemProperty_target, finishCallback);
            if(!result){
                updateFinish(1,true);
                Log.e(TAG, "startSingleAcion put_to_pan : error");
                // Toast.makeText(mContext, "startSingleAcion : error",Toast.LENGTH_LONG).show();
            }
            return result;
        }catch (Exception e){
            e.printStackTrace();
            Log.e("ArmTaskWorker", "putTray : " + e.getMessage());
            return false;
        }
    }

    public boolean putPcrCover(int index,ArmFinishCallback finishCallback){
        if (isDoorOpen()) {
            Log.e("ArmTaskWorker", "putPcrCover: 舱门已打开，禁止机械臂动作");
            return false;
        }
        try{
            itemProperty_source = null;
            itemProperty_target = getItemPropertyPCRCover(index);
            if(itemProperty_target == null){
                Log.e(TAG, "putPcrCover 找不到目标 pcr"+index);
                return false;
            }
            updateFinish(1, false);
            updateArmwork(1,ConstantData.ARM_PCRCOVER);
            saveModel(1);
            boolean result = startSingleChain("put_pcr_cover_to_pcr", itemProperty_source, itemProperty_target, finishCallback);
            Log.e(TAG, "startSingleAcion put_pcr_cover_to_pcr : target "+itemProperty_target.getType()+itemProperty_target.getPosition());
            if(!result){
                updateFinish(1,true);
                Log.e(TAG, "startSingleAcion put_pcr_cover_to_pcr : error");
                //Toast.makeText(mContext, "startSingleAcion  put_pcr_cover_to_pcr : error",Toast.LENGTH_LONG).show();
            }
            return result;
        }catch (Exception e){
            e.printStackTrace();
            Log.e("ArmTaskWorker", "putPcrCover : " + e.getMessage());
            return false;
        }
    }

    public boolean putTempPcrCover(int index,ArmFinishCallback finishCallback){
        if (isDoorOpen()) {
            Log.e("ArmTaskWorker", "putTempPcrCover: 舱门已打开，禁止机械臂动作");
            return false;
        }
        try {
            itemProperty_source = getItemPropertyPCRCover(0);
            itemProperty_target = getItemPropertyPCRCover(index);
            if (itemProperty_target == null) {
                Log.e(TAG, "putTempPcrCover 找不到目标 pcr" + index);
                return false;
            }
            updateFinish(1, false);
            updateArmwork(1,ConstantData.ARM_TEMPPCRCOVER);
            saveModel(1);
            boolean result = startSingleChain("put_pcr_cover_from_temp_to_pcr", itemProperty_source, itemProperty_target, finishCallback);
            Log.e(TAG, "startSingleAcion put_pcr_cover_from_temp_to_pcr : target " + itemProperty_target.getType() + itemProperty_target.getPosition());
            if (!result) {
                updateFinish(1, true);
                Log.e(TAG, "startSingleAcion put_pcr_cover_from_temp_to_pcr : error");
                //Toast.makeText(mContext, "startSingleAcion  put_pcr_cover_to_pcr : error",Toast.LENGTH_LONG).show();
            }
            return result;
        }catch (Exception e){
            e.printStackTrace();
            Log.e("ArmTaskWorker", "putTempPcrCover : " + e.getMessage());
            return false;
        }
    }

    public boolean closeRNADoor(int index,ArmFinishCallback finishCallback){
        itemProperty_source = null;
        itemProperty_target = getItemPropertyRNA(index);
        if(itemProperty_target==null){
            Log.e(TAG, "closeRNADoor 找不到目标 rna"+index);
            return false;
        }
        updateArmwork(1,ConstantData.ARM_CLOSERNA);
        saveModel(1);
        boolean result = startSingleChain("close_rna_pan", itemProperty_source, itemProperty_target, finishCallback);
        if(!result){
            Log.e(TAG, "startSingleAcion close_rna_pan: error");
            // Toast.makeText(mContext, "startSingleAcion : error",Toast.LENGTH_LONG).show();
        }
        return result;
    }

    public boolean openRNADoor(int index,ArmFinishCallback finishCallback){
        itemProperty_source = null;
        itemProperty_target = getItemPropertyRNA(index);
        if(itemProperty_target==null){
            Log.e(TAG, "openRNADoor 找不到目标 rna"+index);
            return false;
        }
        updateArmwork(1,ConstantData.ARM_OPENRNA);
        saveModel(1);
        boolean result = startSingleChain("open_rna_pan", itemProperty_source, itemProperty_target, finishCallback);
        if(!result){
            Log.e(TAG, "startSingleAcion open_rna_pan : error");
            // Toast.makeText(mContext, "startSingleAcion : error",Toast.LENGTH_LONG).show();
        }
        return result;
    }

    //复位专用
    public boolean config(ArmResetCallback resetCallback) {
        boolean result = false;
        try {
            updateArmwork(1,ConstantData.ARM_RESET);
            saveModel(1);
            setArmResetCallback(resetCallback);
            result = SerialPortManager_Robot.getInstance().robotArmConfig();
            if (result) {
                updateFinish(1, true);
                Log.i("ArmTaskWorker", "robotArmConfig : " + result);
            } else {
                Log.e("ArmTaskWorker", "robotArmConfig : " + result);
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.e("ArmTaskWorker", "config : " + e.getMessage());
            return false;
        }
        return result;
    }
    public boolean clearFinish(){
        updateFinish(1, true);
        Log.e("ArmTaskWorker", "clearFinish :机械臂清除占用 ");
        return true;
    }
    public boolean armFinishCMD(){
        boolean result = false;
        try{
            result = SerialPortManager_Robot.getInstance().finishCMD();
            if(result){
                setFree();
            }
            Log.i("ArmTaskWorker", "arm1 finishCMD:"+result);
        }catch (Exception e){
            e.printStackTrace();
            Log.e("ArmTaskWorker", "armFinishCMD : " + e.getMessage());
            return false;
        }
        return result;
    }
    public boolean armCanuse(){
        //TODO fordebugtest
//        if (true) {
//            return true;
//        }
        int index = 1;
        boolean result = true;
        PipeArmModel m = getArmModel(index);
        if(m==null){
            Log.i("ArmTaskWorker", "arm1 : null");
            return false;
        }
        if(!m.connect.get()){
            Log.i("ArmTaskWorker", "arm1 : disconnect");
            return false;
        }
        if(m.status.get()==null){
            Log.i("ArmTaskWorker","arm1 :status null");
            return false;
        }
        if (!m.status.get().equalsIgnoreCase(ConstantData.str_FREE)) {
            Log.i("ArmTaskWorker", "arm1 : not free is " + m.status.get());
            return false;
        }
        if(m.finish.get()==null || !m.finish.get()){
            Log.i("ArmTaskWorker", "arm1 : not finish");
            return false;
        }
        //TODO fordebugtest
//        if (false) {
        if(m.exist.get()==null || m.exist.get()){
            Log.i("ArmTaskWorker", "arm1 : exist");
            return false;
        }
        String busy = RobotWorkManager.getInstance().getRobotBusyReason();
        if(busy != null){
            Log.i("ArmTaskWorker", "arm1 : busy " + busy);
            return false;
        }
        return result;
    }
    public boolean armSelfcheckCanuse(){
        //TODO fordebugtest
//        if (true) {
//            return true;
//        }
        int index = 1;
        boolean result = true;
        PipeArmModel m = getArmModel(index);
        if(m==null){
            Log.i("ArmTaskWorker", "arm1 : null");
            return false;
        }
        if(!m.connect.get()){
            Log.i("ArmTaskWorker", "arm1 : disconnect");
            return false;
        }
        if(m.status.get()==null){
            Log.i("ArmTaskWorker","arm1 :status null");
            return false;
        }
        if (!m.status.get().equalsIgnoreCase(ConstantData.str_FREE)) {
            Log.i("ArmTaskWorker", "arm1 : not free is " + m.status.get());
            return false;
        }
        if(m.finish.get()==null || !m.finish.get()){
            Log.i("ArmTaskWorker", "arm1 : not finish");
            return false;
        }
        String busy = RobotWorkManager.getInstance().getRobotBusyReason();
        if(busy != null){
            Log.i("ArmTaskWorker", "arm1 : busy " + busy);
            return false;
        }
        return result;
    }
    /**
     *
     * @param tray 盘位
     * @param temperatrue 0表示关闭
     * @return
     */
    public boolean setCooler(int tray,int temperatrue) {
        try {
            boolean result = SerialPortManager_Robot.getInstance().setCooler(tray, temperatrue);
            return result;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }

    /**
     *
     * @param tray
     * @param on_off
     * @return
     */
    public boolean setLed(int tray,int on_off) {
        try {
            boolean result = SerialPortManager_Robot.getInstance().setLed(tray, on_off);
            return result;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }


//    /**
//     * 指令正在执行
//     * @param robotManagerMessage
//     */
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEventMainThread(RobotManagerMessage robotManagerMessage) {
//
//        if (robotManagerMessage != null) {
//            //  String text = GsonUtil.toJson(robotManagerMessage, false);
//            //  callback(robotManagerMessage.isResult());
//        }
//    }
//    /**
//     * 指令正在执行
//     * @param robotRunningMessage
//     */
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEventMainThread(RNARunningMessage robotRunningMessage) {
//        Log.i("ROBOT", "ROBOT指令运行1" + robotRunningMessage.getID() + "getStatus" + robotRunningMessage.getStatus());
//        Log.i("ROBOT", "ROBOT指令运行2" + robotRunningMessage.getID() + "getNO" + robotRunningMessage.getNO());
//        Log.i("ROBOT", "ROBOT指令运行3" + robotRunningMessage.getID() + "getDegree" + robotRunningMessage.getDegree());
//
//        if(robotRunningMessage != null )
//        {
//            String text = GsonUtil.toJson(robotRunningMessage,false);
////            RobotRunningMessageText.append(text + "\n");
////            RobotRunningMessageText.scrollTo(0,0);
//        }
//    }

//    /*
//     * EventBus接收订阅的消息
//     * @param
//     */
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEvent(MessageEvent messageEvent) {
//        switch (messageEvent.getMsgType()) {
//            case MsgType.MSG_ROBOT_CONNECT_STATE:
//                Log.e("MSG_ROBOT_CONNECT_STATE", "is_ROBOT_connect" + SerialPortManager_Robot.getInstance().isIs_ROBOT_connect());
//                if (SerialPortManager_Robot.getInstance().isIs_ROBOT_connect()) {
////                    robot_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
////                    robot_alarm_msg_tv.setText("");
//                    arm1Connected = true;
//                } else {
////                    robot_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
////                    robot_alarm_msg_tv.setText("E3-机械臂断连");
//                    arm1Connected = false;
//                }
//                break;
//        }
//    }
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEventMainThread(QRCodeMessageEvent qRCodeMessageEvent) {
//
//        String text = qRCodeMessageEvent.getQRCodeMessage();
//
//        qRCodeItems.clear();
//
//        String regex = "#P#|#D#"; // 按照 "#P#" 和 "#D#" 分割
//
//        // 使用正则表达式分割字符串
//        String[] parts = text.split(regex);
//
//        // 输出分割结果
//        int i = 1;
//        for (String part : parts) {
//            if (!part.isEmpty()) { // 过滤空字符串
//                // (438,140)0100614141999996901234567890
//                // 使用正则表达式提取括号内的数字和后续的字符串
//                String regex1 = "\\((\\d+),(\\d+)\\)(\\d+)";
//                Pattern pattern = Pattern.compile(regex1);
//                Matcher matcher = pattern.matcher(part);
//
//                if (matcher.find()) {
//                    String x = matcher.group(1); // 提取第一个数字 438
//                    String y = matcher.group(2); // 提取第二个数字 140
//                    String qrcode = matcher.group(3); // 提取后续的字符串 0100614141999996901234567890
//
//                    QRCodeItem qRCodeItem = new QRCodeItem();
//
//                    qRCodeItem.setIndex(i++);
//                    qRCodeItem.setX_coordinate(NumberUtils.StringtoInt(x));
//                    qRCodeItem.setY_coordinate(NumberUtils.StringtoInt(y));
//                    qRCodeItem.setQrcode(qrcode);
//                    qRCodeItems.add(qRCodeItem);
//
//                    Log.i(TAG,"Part 1: " + x);
//                    Log.i(TAG,"Part 2: " + y);
//                    Log.i(TAG,"Part 3: " + qrcode);
//                }
//            }
//        }
//
////        qRCodeAdapter.notifyDataSetChanged();
//
//
//    }
}


