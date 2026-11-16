package com.paul.pipeline.scheduling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.model.TrayDispatchSnapshot;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

/**
 * tray 编排层纯 JVM 单测：锁定 SchedulingEngine 的 6 个 tray 方法与原 checktray* 一致。
 * 重点：4 趟顺序与中止、返回值不对称（urgent false vs error 无参 true）、
 * 分配空闲设备、checktrayend 推进/整盘结束、错误路径写回顺序。
 */
public class SchedulingEngineTrayTest {

    private SchedulingPort port;
    private SchedulingEngine engine;

    @Before
    public void setUp() {
        port = Mockito.mock(SchedulingPort.class);
        engine = new SchedulingEngine(port);
        when(port.isDoorHold()).thenReturn(false);
        when(port.isWssStart()).thenReturn(true);
        when(port.isSelfCheck()).thenReturn(true);
        when(port.isArmCanuse()).thenReturn(true);
        when(port.closeRnaDoor(anyInt())).thenReturn(true);
    }

    private TrayDispatchSnapshot dispatch(int status, int targettype, int target) {
        TrayDispatchSnapshot d = new TrayDispatchSnapshot();
        d.dispatchIndex = 1;
        d.status = status;
        d.statusKnown = true;
        d.targettype = targettype;
        d.targettypeKnown = true;
        d.target = target;
        d.targetKnown = true;
        d.devicestatus = 0;
        d.devicestatusKnown = true;
        return d;
    }

    // ---------------- checkTrayUrgent ----------------

    @Test
    public void urgent_closeDoor1Fails_returnsFalse() {
        when(port.closeRnaDoor(1)).thenReturn(false);

        assertFalse(engine.checkTrayUrgent());
        verify(port, never()).closeRnaDoor(2);
        verify(port, never()).trayCount();
    }

    @Test
    public void urgent_closeDoor2Fails_returnsFalse() {
        when(port.closeRnaDoor(2)).thenReturn(false);

        assertFalse(engine.checkTrayUrgent());
        verify(port).closeRnaDoor(1);
    }

    @Test
    public void urgent_selfCheckFalse_returnsTrueNoWork() {
        when(port.isSelfCheck()).thenReturn(false);

        assertTrue(engine.checkTrayUrgent());
        verify(port, never()).trayCount();
    }

    @Test
    public void urgent_fourPassOrdering() {
        // 2 trays: A(index=10) urgent, B(index=20) normal. 全空闲不中止。
        when(port.trayCount()).thenReturn(2);
        when(port.trayIndexAt(0)).thenReturn(10);
        when(port.trayIndexAt(1)).thenReturn(20);
        when(port.trayUrgentAt(0)).thenReturn(true);
        when(port.trayUrgentAt(1)).thenReturn(false);
        // 让 checkTrayRnawork/Pcrwork 走到“状态非 RUN”最短路径：返回 true 无副作用
        when(port.trayDispatchSize(anyInt())).thenReturn(2);
        when(port.trayStatusIs(anyInt(), eq(ConstantData.str_RUN))).thenReturn(false);

        assertTrue(engine.checkTrayUrgent());

        // 断言 4 趟顺序：加急RNA(10) -> 加急PCR(10) -> 普通RNA(10,20) -> 普通PCR(10,20)
        // 用 trayStatusIs(idx, str_RUN) 的调用序列近似锁定（每趟每托盘各调一次）
        InOrder o = inOrder(port);
        o.verify(port).trayStatusIs(10, ConstantData.str_RUN); // 加急RNA A
        o.verify(port).trayStatusIs(10, ConstantData.str_RUN); // 加急PCR A
        o.verify(port).trayStatusIs(10, ConstantData.str_RUN); // 普通RNA A
        o.verify(port).trayStatusIs(20, ConstantData.str_RUN); // 普通RNA B
        o.verify(port).trayStatusIs(10, ConstantData.str_RUN); // 普通PCR A
        o.verify(port).trayStatusIs(20, ConstantData.str_RUN); // 普通PCR B
    }

    @Test
    public void urgent_abortInNormalRna_skipsNormalPcr() {
        // 单托盘，普通RNA 趟中止（delegateRna=false）
        when(port.trayCount()).thenReturn(1);
        when(port.trayIndexAt(0)).thenReturn(10);
        when(port.trayUrgentAt(0)).thenReturn(false); // 无加急，跳过前两趟
        when(port.trayDispatchSize(10)).thenReturn(2);
        when(port.trayStatusIs(10, ConstantData.str_RUN)).thenReturn(true);
        when(port.trayDispatchIndex(10)).thenReturn(1);
        when(port.currentTrayDispatch(10)).thenReturn(dispatch(ConstantData.STATUS_RUN, ConstantData.TYPE_RNA, 1));
        when(port.delegateRna(1, 10)).thenReturn(false); // 中止

        assertFalse(engine.checkTrayUrgent());
        verify(port, never()).delegatePcr(anyInt(), anyInt());
    }

    // ---------------- checkTrayRnawork / Pcrwork ----------------

    @Test
    public void rnawork_statusRun_delegatesTrue() {
        when(port.trayDispatchSize(10)).thenReturn(2);
        when(port.trayStatusIs(10, ConstantData.str_RUN)).thenReturn(true);
        when(port.trayDispatchIndex(10)).thenReturn(1);
        when(port.currentTrayDispatch(10)).thenReturn(dispatch(ConstantData.STATUS_RUN, ConstantData.TYPE_RNA, 3));
        when(port.delegateRna(3, 10)).thenReturn(true);

        assertTrue(engine.checkTrayRnawork(10));
        verify(port).delegateRna(3, 10);
        verify(port, never()).allocateFreeRnaToDispatch(anyInt(), anyInt());
    }

    @Test
    public void rnawork_statusRun_delegateFalse_returnsFalse() {
        when(port.trayDispatchSize(10)).thenReturn(2);
        when(port.trayStatusIs(10, ConstantData.str_RUN)).thenReturn(true);
        when(port.trayDispatchIndex(10)).thenReturn(1);
        when(port.currentTrayDispatch(10)).thenReturn(dispatch(ConstantData.STATUS_RUN, ConstantData.TYPE_RNA, 3));
        when(port.delegateRna(3, 10)).thenReturn(false);

        assertFalse(engine.checkTrayRnawork(10));
    }

    @Test
    public void rnawork_statusFree_allocatesFreeRna() {
        when(port.trayDispatchSize(10)).thenReturn(2);
        when(port.trayStatusIs(10, ConstantData.str_RUN)).thenReturn(true);
        when(port.trayDispatchIndex(10)).thenReturn(1);
        when(port.currentTrayDispatch(10)).thenReturn(dispatch(ConstantData.STATUS_FREE, ConstantData.DEVICE_RNA, 0));

        assertTrue(engine.checkTrayRnawork(10));
        verify(port).allocateFreeRnaToDispatch(10, 1);
    }

    @Test
    public void rnawork_statusFree_doorHold_noAllocate() {
        when(port.isDoorHold()).thenReturn(true);
        when(port.trayDispatchSize(10)).thenReturn(2);
        when(port.trayStatusIs(10, ConstantData.str_RUN)).thenReturn(true);
        when(port.trayDispatchIndex(10)).thenReturn(1);
        when(port.currentTrayDispatch(10)).thenReturn(dispatch(ConstantData.STATUS_FREE, ConstantData.DEVICE_RNA, 0));

        assertTrue(engine.checkTrayRnawork(10));
        verify(port, never()).allocateFreeRnaToDispatch(anyInt(), anyInt());
    }

    @Test
    public void rnawork_notRun_returnsTrueNoWork() {
        when(port.trayDispatchSize(10)).thenReturn(2);
        when(port.trayStatusIs(10, ConstantData.str_RUN)).thenReturn(false);

        assertTrue(engine.checkTrayRnawork(10));
        verify(port, never()).delegateRna(anyInt(), anyInt());
        verify(port, never()).allocateFreeRnaToDispatch(anyInt(), anyInt());
    }

    @Test
    public void rnawork_currentDispatchNull_returnsTrue() {
        when(port.trayDispatchSize(10)).thenReturn(2);
        when(port.trayStatusIs(10, ConstantData.str_RUN)).thenReturn(true);
        when(port.trayDispatchIndex(10)).thenReturn(1);
        when(port.currentTrayDispatch(10)).thenReturn(null);

        assertTrue(engine.checkTrayRnawork(10));
        verify(port, never()).delegateRna(anyInt(), anyInt());
    }

    @Test
    public void pcrwork_statusFree_allocatesFreePcr() {
        when(port.trayDispatchSize(10)).thenReturn(2);
        when(port.trayStatusIs(10, ConstantData.str_RUN)).thenReturn(true);
        when(port.trayDispatchIndex(10)).thenReturn(1);
        when(port.currentTrayDispatch(10)).thenReturn(dispatch(ConstantData.STATUS_FREE, ConstantData.DEVICE_PCR, 0));

        assertTrue(engine.checkTrayPcrwork(10));
        verify(port).allocateFreePcrToDispatch(10, 1);
    }

    @Test
    public void pcrwork_statusRun_delegateFalse_returnsFalse() {
        when(port.trayDispatchSize(10)).thenReturn(2);
        when(port.trayStatusIs(10, ConstantData.str_RUN)).thenReturn(true);
        when(port.trayDispatchIndex(10)).thenReturn(1);
        when(port.currentTrayDispatch(10)).thenReturn(dispatch(ConstantData.STATUS_RUN, ConstantData.TYPE_PCR, 4));
        when(port.delegatePcr(4, 10)).thenReturn(false);

        assertFalse(engine.checkTrayPcrwork(10));
    }

    // ---------------- checkTrayEnd ----------------

    @Test
    public void end_dispatchIndexBeyondSize_finishesTray() {
        when(port.trayCount()).thenReturn(1);
        when(port.trayExistsAt(0)).thenReturn(true);
        when(port.trayIndexAt(0)).thenReturn(10);
        when(port.trayDispatchSize(10)).thenReturn(2);
        when(port.trayHasStatus(10)).thenReturn(true);
        when(port.trayStatusIs(10, ConstantData.str_RUN)).thenReturn(true);
        when(port.trayDispatchIndex(10)).thenReturn(3); // di>size

        engine.checkTrayEnd();

        InOrder o = inOrder(port);
        o.verify(port).setTrayFlagFree(10);
        o.verify(port).setTrayStatusFinish(10);
        o.verify(port).createLedTask(10, 0);
        o.verify(port).saveTray(10);
    }

    @Test
    public void end_currentFinished_advances() {
        when(port.trayCount()).thenReturn(1);
        when(port.trayExistsAt(0)).thenReturn(true);
        when(port.trayIndexAt(0)).thenReturn(10);
        when(port.trayDispatchSize(10)).thenReturn(2);
        when(port.trayHasStatus(10)).thenReturn(true);
        when(port.trayStatusIs(10, ConstantData.str_RUN)).thenReturn(true);
        when(port.trayDispatchIndex(10)).thenReturn(1); // di+1<=size
        when(port.currentTrayDispatch(10)).thenReturn(dispatch(ConstantData.STATUS_FINISH, ConstantData.TYPE_RNA, 3));

        engine.checkTrayEnd();

        verify(port).advanceTrayDispatch(10, 2);
        verify(port, never()).setTrayStatusFinish(anyInt());
        verify(port, never()).createLedTask(anyInt(), anyInt());
    }

    @Test
    public void end_currentFinishedAtLast_finishesWholeTray() {
        when(port.trayCount()).thenReturn(1);
        when(port.trayExistsAt(0)).thenReturn(true);
        when(port.trayIndexAt(0)).thenReturn(10);
        when(port.trayDispatchSize(10)).thenReturn(2);
        when(port.trayHasStatus(10)).thenReturn(true);
        when(port.trayStatusIs(10, ConstantData.str_RUN)).thenReturn(true);
        when(port.trayDispatchIndex(10)).thenReturn(2); // di+1>size
        when(port.currentTrayDispatch(10)).thenReturn(dispatch(ConstantData.STATUS_FINISH, ConstantData.TYPE_RNA, 3));

        engine.checkTrayEnd();

        InOrder o = inOrder(port);
        o.verify(port).setTrayFlagFree(10);
        o.verify(port).setTrayStatusFinish(10);
        o.verify(port).createLedTask(10, 0);
        o.verify(port).saveTray(10);
        verify(port, never()).advanceTrayDispatch(anyInt(), anyInt());
    }

    @Test
    public void end_runDeviceBackFinished_writebackAndAdvance() {
        when(port.trayCount()).thenReturn(1);
        when(port.trayExistsAt(0)).thenReturn(true);
        when(port.trayIndexAt(0)).thenReturn(10);
        when(port.trayDispatchSize(10)).thenReturn(2);
        when(port.trayHasStatus(10)).thenReturn(true);
        when(port.trayStatusIs(10, ConstantData.str_RUN)).thenReturn(true);
        when(port.trayDispatchIndex(10)).thenReturn(1); // advance 分支
        TrayDispatchSnapshot d = dispatch(ConstantData.STATUS_RUN, ConstantData.TYPE_RNA, 3);
        when(port.currentTrayDispatch(10)).thenReturn(d);
        when(port.deviceBackFinishedInBack(ConstantData.DEVICE_RNA, 3)).thenReturn(true);

        engine.checkTrayEnd();

        InOrder o = inOrder(port);
        o.verify(port).markDeviceScheduleDone(ConstantData.DEVICE_RNA, 3);
        o.verify(port).setDispatchStatusFinish(10, d.dispatchIndex);
        o.verify(port).advanceTrayDispatch(10, 2);
        o.verify(port).saveTray(10);
        verify(port, never()).createLedTask(anyInt(), anyInt()); // advance 分支不发 LED
        verify(port, never()).setTrayStatusFinish(anyInt());
    }

    @Test
    public void end_runDeviceNotFinished_noWriteback() {
        when(port.trayCount()).thenReturn(1);
        when(port.trayExistsAt(0)).thenReturn(true);
        when(port.trayIndexAt(0)).thenReturn(10);
        when(port.trayDispatchSize(10)).thenReturn(2);
        when(port.trayHasStatus(10)).thenReturn(true);
        when(port.trayStatusIs(10, ConstantData.str_RUN)).thenReturn(true);
        when(port.trayDispatchIndex(10)).thenReturn(1);
        when(port.currentTrayDispatch(10)).thenReturn(dispatch(ConstantData.STATUS_RUN, ConstantData.TYPE_RNA, 3));
        when(port.deviceBackFinishedInBack(ConstantData.DEVICE_RNA, 3)).thenReturn(false);

        engine.checkTrayEnd();

        verify(port, never()).markDeviceScheduleDone(anyInt(), anyInt());
        verify(port, never()).advanceTrayDispatch(anyInt(), anyInt());
        verify(port, never()).saveTray(anyInt());
    }

    @Test
    public void end_trayNotExist_skipped() {
        when(port.trayCount()).thenReturn(1);
        when(port.trayExistsAt(0)).thenReturn(false);

        engine.checkTrayEnd();

        verify(port, never()).trayDispatchSize(anyInt());
    }

    // ---------------- checkTrayError(int) ----------------

    @Test
    public void error_rnaDelegateTrue_writebackOrderFlagBeforeDevice() {
        int t = 10;
        when(port.trayDispatchSize(t)).thenReturn(2);
        when(port.trayHasErrorDispatchIndex(t)).thenReturn(true);
        when(port.trayErrorDispatchIndex(t)).thenReturn(1);
        when(port.trayHasStatus(t)).thenReturn(true);
        when(port.trayStatusIs(t, ConstantData.str_ERROR)).thenReturn(true);
        when(port.findErrorDispatch(t)).thenReturn(dispatch(ConstantData.STATUS_ERROR, ConstantData.TYPE_RNA, 3));
        when(port.delegateRna(3, t)).thenReturn(true);
        when(port.deviceBackFinishedInBack(ConstantData.DEVICE_RNA, 3)).thenReturn(true);
        when(port.trayExistFlag(t)).thenReturn(true);

        assertTrue(engine.checkTrayError(t));

        InOrder o = inOrder(port);
        o.verify(port).delegateRna(3, t);
        o.verify(port).setTrayFlagFree(t);                          // flag 先
        o.verify(port).markDeviceScheduleDone(ConstantData.DEVICE_RNA, 3); // 设备写回后
    }

    @Test
    public void error_existFalse_noWriteback() {
        int t = 10;
        when(port.trayDispatchSize(t)).thenReturn(2);
        when(port.trayHasErrorDispatchIndex(t)).thenReturn(true);
        when(port.trayErrorDispatchIndex(t)).thenReturn(1);
        when(port.trayHasStatus(t)).thenReturn(true);
        when(port.trayStatusIs(t, ConstantData.str_ERROR)).thenReturn(true);
        when(port.findErrorDispatch(t)).thenReturn(dispatch(ConstantData.STATUS_ERROR, ConstantData.TYPE_RNA, 3));
        when(port.delegateRna(3, t)).thenReturn(true);
        when(port.deviceBackFinishedInBack(ConstantData.DEVICE_RNA, 3)).thenReturn(true);
        when(port.trayExistFlag(t)).thenReturn(false);

        assertTrue(engine.checkTrayError(t));
        verify(port, never()).setTrayFlagFree(anyInt());
        verify(port, never()).markDeviceScheduleDone(anyInt(), anyInt());
    }

    @Test
    public void error_delegateFalse_returnsFalse() {
        int t = 10;
        when(port.trayDispatchSize(t)).thenReturn(2);
        when(port.trayHasErrorDispatchIndex(t)).thenReturn(true);
        when(port.trayErrorDispatchIndex(t)).thenReturn(1);
        when(port.trayHasStatus(t)).thenReturn(true);
        when(port.trayStatusIs(t, ConstantData.str_ERROR)).thenReturn(true);
        when(port.findErrorDispatch(t)).thenReturn(dispatch(ConstantData.STATUS_ERROR, ConstantData.TYPE_RNA, 3));
        when(port.delegateRna(3, t)).thenReturn(false);
        when(port.deviceBackFinishedInBack(ConstantData.DEVICE_RNA, 3)).thenReturn(false);

        assertFalse(engine.checkTrayError(t));
    }

    @Test
    public void error_noErrorDispatch_returnsTrue() {
        int t = 10;
        when(port.trayDispatchSize(t)).thenReturn(2);
        when(port.trayHasErrorDispatchIndex(t)).thenReturn(true);
        when(port.trayErrorDispatchIndex(t)).thenReturn(1);
        when(port.trayHasStatus(t)).thenReturn(true);
        when(port.trayStatusIs(t, ConstantData.str_ERROR)).thenReturn(true);
        when(port.findErrorDispatch(t)).thenReturn(null);

        assertTrue(engine.checkTrayError(t));
        verify(port, never()).delegateRna(anyInt(), anyInt());
    }

    // ---------------- checkTrayError() 无参：不对称 ----------------

    @Test
    public void errorNoArg_innerAbort_returnsTrue() {
        // 关键不对称：内层 checkTrayError(idx) 返回 false 时，无参版返回 TRUE
        when(port.trayCount()).thenReturn(1);
        when(port.trayIndexKnownAt(0)).thenReturn(true);
        when(port.trayIndexAt(0)).thenReturn(10);
        when(port.trayStatusIs(10, ConstantData.str_ERROR)).thenReturn(true);
        // 令内层中止：
        when(port.trayDispatchSize(10)).thenReturn(2);
        when(port.trayHasErrorDispatchIndex(10)).thenReturn(true);
        when(port.trayErrorDispatchIndex(10)).thenReturn(1);
        when(port.trayHasStatus(10)).thenReturn(true);
        when(port.findErrorDispatch(10)).thenReturn(dispatch(ConstantData.STATUS_ERROR, ConstantData.TYPE_RNA, 3));
        when(port.delegateRna(3, 10)).thenReturn(false);

        assertTrue(engine.checkTrayError()); // <-- TRUE，与 urgent 的 false 对照
    }

    @Test
    public void errorNoArg_allClean_returnsFalse() {
        when(port.trayCount()).thenReturn(1);
        when(port.trayIndexKnownAt(0)).thenReturn(true);
        when(port.trayIndexAt(0)).thenReturn(10);
        when(port.trayStatusIs(10, ConstantData.str_ERROR)).thenReturn(false); // 非 error 托盘

        assertFalse(engine.checkTrayError());
    }

    @Test
    public void errorNoArg_exception_returnsFalse() {
        when(port.trayCount()).thenThrow(new RuntimeException("boom"));

        assertFalse(engine.checkTrayError());
    }
}
