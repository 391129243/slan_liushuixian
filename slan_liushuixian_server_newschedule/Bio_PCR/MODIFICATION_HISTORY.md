# 修改历史记录

## 2026-08-15 #2: PcrCurveFragment 曲线显示修复

### 问题描述
PCR 曲线界面默认没有选中颜色通道（FAM/HEX/ROX/CY5），导致即使有数据也不显示曲线。同时坐标轴范围设置不合理，Raw/AMP 曲线的 X 轴没有固定从 cycle 1 开始。

### 解决方案
1. 初始化时将所有颜色通道默认设置为选中状态
2. 修复 X 轴坐标范围：Raw/AMP 曲线固定从 cycle 1 开始，最大到 40（或数据实际最大值）

### 修改清单

#### 1. initChartViewListener() 方法 (line 965-972)
**位置**: `PcrCurveFragment.java` 颜色通道初始化
**原代码**:
```java
isA1Check = isA2Check = isA3Check = isA4Check = isA5Check = isA6Check = isA7Check = isA8Check = true;
isB1Check = isB2Check = isB3Check = isB4Check = isB5Check = isB6Check = isB7Check = isB8Check = true;
```

**修改后**:
```java
// 孔位通道默认全选
isA1Check = isA2Check = isA3Check = isA4Check = isA5Check = isA6Check = isA7Check = isA8Check = true;
isB1Check = isB2Check = isB3Check = isB4Check = isB5Check = isB6Check = isB7Check = isB8Check = true;

// 颜色通道默认全选
isFAMCheck = true;
isHEXCheck = true;
isROXCheck = true;
isCY5Check = true;
isAllChanelCheck = true;
```

---

#### 2. initChartViewListener() 方法末尾 (line 1061-1068)
**位置**: UI 状态初始化
**原代码**:
```java

  //  mAllCheckTv.setChecked();
    setAllholeChecked();
```

**修改后**:
```java

// 设置UI显示为全选状态
mAllCheckTv.setCheckState(R.drawable.item_chanel_check, isAllChanelCheck);
mFamChannelTv.setCheckState(R.drawable.item_chanel_check, isFAMCheck);
mHexChannelTv.setCheckState(R.drawable.item_chanel_check, isHEXCheck);
mRoxChannelTv.setCheckState(R.drawable.item_chanel_check, isROXCheck);
mCY5ChannelTv.setCheckState(R.drawable.item_chanel_check, isCY5Check);

setAllholeChecked();
```

---

#### 3. drawchart() 方法 X 轴范围修复 (line 1668-1686)
**位置**: `drawchart()` 方法中 X 轴范围设置逻辑
**说明**: 这部分代码已经在之前的版本中修复过，确保：
- Raw/AMP 曲线：X 轴最小值固定为 1（cycle 1），最大值为 max(40, 数据实际最大值)
- Melt/Peak 曲线：X 轴最小值为 min(50, 数据实际最小值)，最大值为 max(95, 数据实际最大值)

---

### 回退步骤

1. **移除颜色通道初始化** (line 968-972，共5行)
   - 删除从 `// 颜色通道默认全选` 到 `isAllChanelCheck = true;` 的5行

2. **移除 UI 状态设置** (line 1062-1067，共6行)
   - 删除从 `// 设置UI显示为全选状态` 到 `mCY5ChannelTv.setCheckState...` 的6行
   - 恢复注释掉的 `//  mAllCheckTv.setChecked();`

### 验证方法

#### 功能测试
1. 打开 PCR 曲线界面
2. 确认 FAM/HEX/ROX/CY5 四个颜色通道按钮显示为选中状态（蓝色勾选图标）
3. 确认 "All" 按钮也显示为选中状态
4. 切换到 Raw 或 AMP 曲线，确认 X 轴从 1 开始显示
5. 确认有数据时曲线正常显示

### 相关文件
- `app/src/main/java/com/paul/pipeline/fragment/PcrCurveFragment.java`

### 编译状态
✅ 编译通过 (2026-08-15)
```
BUILD SUCCESSFUL in 34s
```

---

## 2026-08-15 #1: APLManager PCR/RNA finish pending 重试机制 (已回退)

### 问题描述
PCR/RNA 实验结束后，finish 事件到达时 `status/linuxStatus` 仍为 `run`，导致 `checkpcrwork` / `checkrnawork` 的门控拦截 `WORK_BACK` 分支，机械臂还盘动作无法触发。

### 解决方案
在 finish 事件收到后登记 pending 记录，每轮 `checkArmTask`（5秒）检查设备状态是否变为 free，就绪后立即触发还盘逻辑。

**状态**: ❌ 已于 2026-08-15 完全回退

### 修改清单

#### 1. 新增字段 (line 116-120)

### 问题描述
PCR/RNA 实验结束后，finish 事件到达时 `status/linuxStatus` 仍为 `run`，导致 `checkpcrwork` / `checkrnawork` 的门控拦截 `WORK_BACK` 分支，机械臂还盘动作无法触发。

### 解决方案
在 finish 事件收到后登记 pending 记录，每轮 `checkArmTask`（5秒）检查设备状态是否变为 free，就绪后立即触发还盘逻辑。

### 修改清单

#### 1. 新增字段 (line 116-120)
**位置**: 类成员变量区
**原代码**:
```java
public boolean WSSStart = false;//启动是否成功
public boolean SelfCheck = false;//启动是否成功

public boolean startWSSserver() {
```

**修改后**:
```java
public boolean WSSStart = false;//启动是否成功
public boolean SelfCheck = false;//启动是否成功

// PCR/RNA finish 后等待 status/linuxStatus 变 free 的 pending 重试记录
// key = 设备 index，value = 剩余可重试轮次（每次 checkArmTask 消耗1次）
private final ConcurrentHashMap<Integer, Integer> pcrFinishPending = new ConcurrentHashMap<>();
private final ConcurrentHashMap<Integer, Integer> rnaFinishPending = new ConcurrentHashMap<>();
private static final int FINISH_PENDING_MAX_RETRY = 12; // 最多等 12 轮 × 5s = 60s

public boolean startWSSserver() {
```

---

#### 2. checkArmTask() 方法开头 (line 276-279)
**位置**: `checkArmTask()` 方法第一行
**原代码**:
```java
public void checkArmTask() {
    try {
        checkrnaRun();
        checkpcrRun();
```

**修改后**:
```java
public void checkArmTask() {
    try {
        checkPcrFinishPending();
        checkRnaFinishPending();
        checkrnaRun();
        checkpcrRun();
```

---

#### 3. 新增方法 checkPcrFinishPending() (line 314-353)
**位置**: `checkArmTask()` 方法之后
**新增完整方法**:
```java
/**
 * PCR finish 后记录 pending；等 status/linuxStatus 都变 free 后再触发 checkpcrwork。
 * 每次 checkArmTask 调用消耗一个重试轮次，最多等 FINISH_PENDING_MAX_RETRY 轮。
 */
private void checkPcrFinishPending() {
    if (pcrFinishPending.isEmpty()) return;

    Iterator<Map.Entry<Integer, Integer>> it = pcrFinishPending.entrySet().iterator();
    while (it.hasNext()) {
        Map.Entry<Integer, Integer> entry = it.next();
        int target = entry.getKey();
        int retry = entry.getValue();

        PipePCRModel m = PCRManager.getInstance().getPipePCRModel(target);
        if (m == null) {
            it.remove();
            continue;
        }

        String status = m.status.get();
        String linuxStatus = m.linuxStatus.get();
        boolean ready = status != null && status.equalsIgnoreCase(ConstantData.str_FREE)
                && linuxStatus != null && linuxStatus.equalsIgnoreCase(ConstantData.str_FREE);

        if (ready) {
            Log.w("ArmTaskWorker", "checkPcrFinishPending pcr:" + target
                    + " status ready, trigger checkpcrwork");
            checkpcrwork(target, m.currentTray.get());
            it.remove();
        } else {
            if (retry <= 0) {
                Log.e("ArmTaskWorker", "checkPcrFinishPending pcr:" + target
                        + " exceeded max retry, give up (status=" + status
                        + " linuxStatus=" + linuxStatus + ")");
                it.remove();
            } else {
                Log.i("ArmTaskWorker", "checkPcrFinishPending pcr:" + target
                        + " not ready yet, retry left=" + retry
                        + " (status=" + status + " linuxStatus=" + linuxStatus + ")");
                entry.setValue(retry - 1);
            }
        }
    }
}
```

---

#### 4. 新增方法 checkRnaFinishPending() (line 355-393)
**位置**: `checkPcrFinishPending()` 方法之后
**新增完整方法**:
```java
/**
 * RNA finish 后记录 pending；等 status/linuxStatus 都变 free 后再触发 checkrnawork。
 */
private void checkRnaFinishPending() {
    if (rnaFinishPending.isEmpty()) return;

    Iterator<Map.Entry<Integer, Integer>> it = rnaFinishPending.entrySet().iterator();
    while (it.hasNext()) {
        Map.Entry<Integer, Integer> entry = it.next();
        int target = entry.getKey();
        int retry = entry.getValue();

        PipeRNAModel m = RNAManager.getInstance().getPipeRNAModel(target);
        if (m == null) {
            it.remove();
            continue;
        }

        String status = m.status.get();
        String linuxStatus = m.linuxStatus.get();
        boolean ready = status != null && status.equalsIgnoreCase(ConstantData.str_FREE)
                && linuxStatus != null && linuxStatus.equalsIgnoreCase(ConstantData.str_FREE);

        if (ready) {
            Log.w("ArmTaskWorker", "checkRnaFinishPending rna:" + target
                    + " status ready, trigger checkrnawork");
            checkrnawork(target, m.currentTray.get());
            it.remove();
        } else {
            if (retry <= 0) {
                Log.e("ArmTaskWorker", "checkRnaFinishPending rna:" + target
                        + " exceeded max retry, give up (status=" + status
                        + " linuxStatus=" + linuxStatus + ")");
                it.remove();
            } else {
                Log.i("ArmTaskWorker", "checkRnaFinishPending rna:" + target
                        + " not ready yet, retry left=" + retry
                        + " (status=" + status + " linuxStatus=" + linuxStatus + ")");
                entry.setValue(retry - 1);
            }
        }
    }
}
```

---

#### 5. checkPcrFinish() 方法 (line 1743-1747)
**位置**: `checkPcrFinish()` 方法中 WORK_BACK 分支
**原代码**:
```java
} else {
    model.backtraystart.set(false);
    model.backtrayfinish.set(false);
    model.armStep.set(ConstantData.WORK_BACK);
    Log.e("ArmTaskWorker", "finish PCR:" + target + " WORK_BACK ");
}
```

**修改后**:
```java
} else {
    model.backtraystart.set(false);
    model.backtrayfinish.set(false);
    model.armStep.set(ConstantData.WORK_BACK);
    Log.e("ArmTaskWorker", "finish PCR:" + target + " WORK_BACK ");
    // 登记 pending：等待 status/linuxStatus 变 free 后，由 checkPcrFinishPending 触发还盘
    pcrFinishPending.put(target, FINISH_PENDING_MAX_RETRY);
    Log.w("ArmTaskWorker", "checkPcrFinish pcr:" + target + " pending registered");
}
```

---

#### 6. checkRnaFinish() 方法 (line 1657-1661)
**位置**: `checkRnaFinish()` 方法中 WORK_BACK 分支
**原代码**:
```java
} else {
    model.backtraystart.set(false);
    model.backtrayfinish.set(false);
    model.armStep.set(ConstantData.WORK_BACK);
    Log.e("ArmTaskWorker", "rna :" + target + " WORK_BACK ");
}
```

**修改后**:
```java
} else {
    model.backtraystart.set(false);
    model.backtrayfinish.set(false);
    model.armStep.set(ConstantData.WORK_BACK);
    Log.e("ArmTaskWorker", "rna :" + target + " WORK_BACK ");
    // 登记 pending：等待 status/linuxStatus 变 free 后，由 checkRnaFinishPending 触发还盘
    rnaFinishPending.put(target, FINISH_PENDING_MAX_RETRY);
    Log.w("ArmTaskWorker", "checkRnaFinish rna:" + target + " pending registered");
}
```

---

### 回退步骤（严格按倒序执行）

1. **移除 checkRnaFinish() 中新增的两行** (line 1660-1661)
   - 删除 `rnaFinishPending.put(target, FINISH_PENDING_MAX_RETRY);`
   - 删除 `Log.w("ArmTaskWorker", "checkRnaFinish rna:" + target + " pending registered");`

2. **移除 checkPcrFinish() 中新增的两行** (line 1746-1747)
   - 删除 `pcrFinishPending.put(target, FINISH_PENDING_MAX_RETRY);`
   - 删除 `Log.w("ArmTaskWorker", "checkPcrFinish pcr:" + target + " pending registered");`

3. **删除 checkRnaFinishPending() 整个方法** (line 355-393，约39行)

4. **删除 checkPcrFinishPending() 整个方法** (line 314-353，约40行)

5. **移除 checkArmTask() 中的两行调用** (line 278-279)
   - 删除 `checkPcrFinishPending();`
   - 删除 `checkRnaFinishPending();`

6. **移除类成员变量** (line 116-120，共5行)
   - 删除从 `// PCR/RNA finish 后等待...` 到 `private static final int FINISH_PENDING_MAX_RETRY = 12;` 的所有行

### 验证方法

#### 功能测试
1. 运行 PCR 实验直到结束
2. 观察日志中是否出现 `pending registered`
3. 5秒后检查是否出现 `status ready, trigger checkpcrwork`
4. 确认机械臂执行还盘动作

#### 日志关键字
- `pending registered` - pending 已登记，等待状态变化
- `not ready yet, retry left=N` - 仍在等待，剩余 N 次重试
- `status ready, trigger check` - 状态就绪，触发还盘
- `exceeded max retry, give up` - 超时放弃（60秒内未就绪）

### 相关文件
- `app/src/main/java/com/paul/pipeline/manager/APLManager.java`

### 编译状态
✅ 编译通过 (2026-08-15)
```
BUILD SUCCESSFUL in 4s
```
