package com.paul.pipeline.scheduling;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.model.ArmItemSnapshot;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

/**
 * 机械臂命令回调引擎纯 JVM 单测：锁定 ArmCallbackEngine 的 6 个方法与原匿名 ArmFinishCallback 一致。
 * 重点承重项：
 * <ul>
 *   <li>Get.finish 按 armStep==GET 守卫路由 RNA/PCR；null 入参不动。</li>
 *   <li>Back/Move.finish 里 updateDispatchStatus(FINISH) 在 null 检查之前无条件执行。</li>
 *   <li>三个 fail 写回顺序：Get/Back 先 notifyArmError 再 updateDispatchStatus(ERROR)；
 *       Move 先 updateDispatchStatus(ERROR) 再 notifyArmError（对照测试锁定，禁止归一化）。</li>
 * </ul>
 */
public class ArmCallbackEngineTest {

    private ArmCallbackPort port;
    private ArmCallbackEngine engine;

    @Before
    public void setUp() {
        port = Mockito.mock(ArmCallbackPort.class);
        engine = new ArmCallbackEngine(port);
    }

    private ArmItemSnapshot snap(String type, int pos) {
        return new ArmItemSnapshot(type, pos);
    }

    // ================= Get =================

    @Test
    public void getFinish_rnaArmStepGet_setsFlagAndRunsTemplate() {
        when(port.deviceArmStepIsGet(ConstantData.DEVICE_RNA, 1)).thenReturn(true);
        engine.onGetFinish(snap(ConstantData.nameRNA, 1), snap(ConstantData.nameTray, 5));
        InOrder io = inOrder(port);
        io.verify(port).deviceArmStepIsGet(ConstantData.DEVICE_RNA, 1);
        io.verify(port).setDeviceGettrayfinish(ConstantData.DEVICE_RNA, 1);
        io.verify(port).createRunTemplate(ConstantData.DEVICE_RNA, 1);
    }

    @Test
    public void getFinish_pcrArmStepGet_setsFlagAndRunsTemplate() {
        when(port.deviceArmStepIsGet(ConstantData.DEVICE_PCR, 3)).thenReturn(true);
        engine.onGetFinish(snap(ConstantData.namePCR, 3), snap(ConstantData.nameTray, 2));
        verify(port).setDeviceGettrayfinish(ConstantData.DEVICE_PCR, 3);
        verify(port).createRunTemplate(ConstantData.DEVICE_PCR, 3);
    }

    @Test
    public void getFinish_armStepNotGet_doesNothing() {
        when(port.deviceArmStepIsGet(ConstantData.DEVICE_RNA, 1)).thenReturn(false);
        engine.onGetFinish(snap(ConstantData.nameRNA, 1), snap(ConstantData.nameTray, 5));
        verify(port, never()).setDeviceGettrayfinish(anyInt(), anyInt());
        verify(port, never()).createRunTemplate(anyInt(), anyInt());
    }

    @Test
    public void getFinish_nullAim_doesNothing() {
        engine.onGetFinish(null, snap(ConstantData.nameTray, 5));
        verify(port, never()).deviceArmStepIsGet(anyInt(), anyInt());
        verify(port, never()).setDeviceGettrayfinish(anyInt(), anyInt());
    }

    @Test
    public void getFail_sourceMatches_notifyThenError() {
        engine.onGetFail(snap(ConstantData.nameTray, 5), 5, 2, "boom");
        InOrder io = inOrder(port);
        io.verify(port).notifyArmError("boom");
        io.verify(port).updateDispatchStatus(5, 2, ConstantData.STATUS_ERROR);
    }

    @Test
    public void getFail_sourceMismatch_onlyNotify() {
        engine.onGetFail(snap(ConstantData.nameTray, 9), 5, 2, "boom");
        verify(port).notifyArmError("boom");
        verify(port, never()).updateDispatchStatus(anyInt(), anyInt(), anyInt());
    }

    @Test
    public void getFail_nullSource_onlyNotify() {
        engine.onGetFail(null, 5, 2, "boom");
        verify(port).notifyArmError("boom");
        verify(port, never()).updateDispatchStatus(anyInt(), anyInt(), anyInt());
    }

    // ================= Back =================

    @Test
    public void backFinish_unconditionalFinishThenRecycleRna() {
        engine.onBackFinish(snap(ConstantData.nameTray, 4), snap(ConstantData.nameRNA, 1), 4, 2);
        InOrder io = inOrder(port);
        io.verify(port).updateDispatchStatus(4, 2, ConstantData.STATUS_FINISH);
        io.verify(port).finishBacktray(ConstantData.DEVICE_RNA, 1, 4);
    }

    @Test
    public void backFinish_pcrSource_recyclePcr() {
        engine.onBackFinish(snap(ConstantData.nameTray, 4), snap(ConstantData.namePCR, 3), 4, 2);
        verify(port).updateDispatchStatus(4, 2, ConstantData.STATUS_FINISH);
        verify(port).finishBacktray(ConstantData.DEVICE_PCR, 3, 4);
    }

    @Test
    public void backFinish_nullSource_stillWritesFinish() {
        engine.onBackFinish(null, null, 4, 2);
        verify(port).updateDispatchStatus(4, 2, ConstantData.STATUS_FINISH);
        verify(port, never()).finishBacktray(anyInt(), anyInt(), anyInt());
    }

    @Test
    public void backFail_targetMatches_notifyThenError() {
        engine.onBackFail(snap(ConstantData.nameTray, 4), 4, 2, "back-fail");
        InOrder io = inOrder(port);
        io.verify(port).notifyArmError("back-fail");
        io.verify(port).updateDispatchStatus(4, 2, ConstantData.STATUS_ERROR);
    }

    @Test
    public void backFail_targetMismatch_onlyNotify() {
        engine.onBackFail(snap(ConstantData.nameTray, 7), 4, 2, "back-fail");
        verify(port).notifyArmError("back-fail");
        verify(port, never()).updateDispatchStatus(anyInt(), anyInt(), anyInt());
    }

    // ================= Move =================

    @Test
    public void moveFinish_rnaSourceToPcr_fullChain() {
        engine.onMoveFinish(snap(ConstantData.namePCR, 3), snap(ConstantData.nameRNA, 1), 6, 2);
        InOrder io = inOrder(port);
        io.verify(port).updateDispatchStatus(6, 2, ConstantData.STATUS_FINISH);
        io.verify(port).finishMoveouttray(ConstantData.DEVICE_RNA, 1, 3);
        io.verify(port).updateDispatchNext(6, 2, ConstantData.DEVICE_PCR, 3);
        io.verify(port).createRunTemplate(ConstantData.DEVICE_PCR, 3);
    }

    @Test
    public void moveFinish_pcrSourceToRna_fullChain() {
        engine.onMoveFinish(snap(ConstantData.nameRNA, 1), snap(ConstantData.namePCR, 3), 6, 2);
        verify(port).finishMoveouttray(ConstantData.DEVICE_PCR, 3, 1);
        verify(port).updateDispatchNext(6, 2, ConstantData.DEVICE_RNA, 1);
        verify(port).createRunTemplate(ConstantData.DEVICE_RNA, 1);
    }

    @Test
    public void moveFinish_nullSource_stillWritesFinish() {
        engine.onMoveFinish(null, null, 6, 2);
        verify(port).updateDispatchStatus(6, 2, ConstantData.STATUS_FINISH);
        verify(port, never()).finishMoveouttray(anyInt(), anyInt(), anyInt());
        verify(port, never()).updateDispatchNext(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    public void moveFail_errorBeforeNotify_oppositeOrder() {
        engine.onMoveFail(6, 2, "move-fail");
        InOrder io = inOrder(port);
        // 与 Get/Back 相反：先写 ERROR 再 notify
        io.verify(port).updateDispatchStatus(6, 2, ConstantData.STATUS_ERROR);
        io.verify(port).notifyArmError("move-fail");
    }
}
