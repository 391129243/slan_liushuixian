package com.paul.pipeline.scheduling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paul.pipeline.model.ArmStep;
import com.paul.pipeline.model.DeviceRuntimeState;
import com.paul.pipeline.model.PcrSchedulingState;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * PCR 状态机纯 JVM 单测：锁定 SchedulingEngine.stepPcr 与原 checkpcrwork 一致。
 * 重点覆盖与 RNA 的差异：MOVE 找空闲 RNA（PCR→RNA）、外层托盘守卫仅在 currentTrayKnown 时判。
 */
public class SchedulingEnginePcrTest {

    private static final int PCR = 2;
    private static final int TRAY = 5;
    private static final int DISPATCH = 7;

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
        when(port.currentDispatchIndex(TRAY)).thenReturn(DISPATCH);
    }

    private PcrSchedulingState state(ArmStep step) {
        PcrSchedulingState s = new PcrSchedulingState();
        s.index = PCR;
        s.currentTrayKnown = true;
        s.currentTray = TRAY;
        s.statusFree = true;
        s.exist = false;
        s.armStep = step;
        s.runtime = new DeviceRuntimeState();
        return s;
    }

    // GET 成功 -> busy
    @Test
    public void get_issuesCommand_returnsBusy() {
        when(port.createGetTray(TRAY)).thenReturn(true);
        PcrSchedulingState s = state(ArmStep.GET);

        boolean idle = engine.stepPcr(s, TRAY);

        assertFalse(idle);
        verify(port).createGetTray(TRAY);
        assertTrue(s.runtime.isGettraystart());
        assertFalse(s.runtime.isGettrayfinish());
    }

    // GET 被开门拦截
    @Test
    public void get_blockedByDoorHold() {
        when(port.isDoorHold()).thenReturn(true);
        PcrSchedulingState s = state(ArmStep.GET);

        assertTrue(engine.stepPcr(s, TRAY));
        verify(port, never()).createGetTray(anyInt());
    }

    // RUN -> 起模板（走 PCR 端口）
    @Test
    public void run_startsPcrTemplate() {
        when(port.startRunTemplatePcr(PCR, TRAY, DISPATCH)).thenReturn(true);
        PcrSchedulingState s = state(ArmStep.RUN);

        assertTrue(engine.stepPcr(s, TRAY));
        verify(port).startRunTemplatePcr(PCR, TRAY, DISPATCH);
        assertTrue(s.runtime.isExcutestart());
        assertFalse(s.runtime.isExcutefinish());
    }

    // MOVE 有空闲 RNA -> false + markFinish（PCR→RNA 方向）
    @Test
    public void move_withFreeRna_issuesMove() {
        when(port.findFreeRnaIndex()).thenReturn(1);
        when(port.createMovePcr(PCR, 1, TRAY, DISPATCH)).thenReturn(true);
        PcrSchedulingState s = state(ArmStep.MOVE);

        boolean idle = engine.stepPcr(s, TRAY);

        assertFalse(idle);
        verify(port).createMovePcr(PCR, 1, TRAY, DISPATCH);
        verify(port).markDispatchDeviceFinish(TRAY, DISPATCH);
        assertTrue(s.runtime.isMovetraystart());
        // 确认没有误用 RNA 侧的找空闲 PCR / move
        verify(port, never()).findFreePcrIndex();
    }

    // MOVE 无空闲 RNA -> armStep=BACK 且 return true
    @Test
    public void move_noFreeRna_fallsBackToBack() {
        when(port.findFreeRnaIndex()).thenReturn(-1);
        PcrSchedulingState s = state(ArmStep.MOVE);

        boolean idle = engine.stepPcr(s, TRAY);

        assertTrue(idle);
        assertEquals(ArmStep.BACK, s.armStep);
        assertFalse(s.runtime.isBacktraystart());
        verify(port, never()).createMovePcr(anyInt(), anyInt(), anyInt(), anyInt());
    }

    // BACK -> false + markFinish
    @Test
    public void back_issuesCommand_returnsBusy() {
        when(port.createBackPcr(PCR, TRAY, DISPATCH)).thenReturn(true);
        PcrSchedulingState s = state(ArmStep.BACK);

        boolean idle = engine.stepPcr(s, TRAY);

        assertFalse(idle);
        verify(port).createBackPcr(PCR, TRAY, DISPATCH);
        verify(port).markDispatchDeviceFinish(TRAY, DISPATCH);
        assertTrue(s.runtime.isBacktraystart());
    }

    // BACK 只需 WSSStart（SelfCheck=false 仍下发）
    @Test
    public void back_onlyNeedsWssStart() {
        when(port.isSelfCheck()).thenReturn(false);
        when(port.createBackPcr(PCR, TRAY, DISPATCH)).thenReturn(true);
        PcrSchedulingState s = state(ArmStep.BACK);

        assertFalse(engine.stepPcr(s, TRAY));
        verify(port).createBackPcr(PCR, TRAY, DISPATCH);
    }

    // FREE + uiinit + !exist -> fireUiInitPcr
    @Test
    public void free_uiinitAndNotExist_firesUiInitPcr() {
        PcrSchedulingState s = state(ArmStep.FREE);
        s.runtime.setUiinit(true);

        assertTrue(engine.stepPcr(s, TRAY));
        verify(port).fireUiInitPcr(PCR);
        assertFalse(s.runtime.isUiinit());
    }

    // 差异点：currentTray 已知且不匹配 -> 早退 true，不下命令
    @Test
    public void trayKnownMismatch_returnsIdle() {
        PcrSchedulingState s = state(ArmStep.GET);
        s.currentTrayKnown = true;

        assertTrue(engine.stepPcr(s, 9));
        verify(port, never()).createGetTray(anyInt());
    }

    // 差异点：currentTray 未知（null）时，不因“不匹配”早退，仍进入 switch。
    // 用 FREE+uiinit 观察确实进入了 switch（RNA 无此语义）。
    @Test
    public void trayUnknown_doesNotEarlyReturnOnMismatch() {
        PcrSchedulingState s = state(ArmStep.FREE);
        s.currentTrayKnown = false;
        s.currentTray = 0;
        s.runtime.setUiinit(true);

        boolean idle = engine.stepPcr(s, 9); // tray 与 currentTray 不同，但 unknown 不早退

        assertTrue(idle);
        verify(port).fireUiInitPcr(PCR); // 证明进入了 switch 的 FREE 分支
    }

    // statusFree=false -> true 无命令
    @Test
    public void statusNotFree_returnsIdle() {
        PcrSchedulingState s = state(ArmStep.GET);
        s.statusFree = false;

        assertTrue(engine.stepPcr(s, TRAY));
        verify(port, never()).createGetTray(anyInt());
    }

    // null -> true
    @Test
    public void nullState_returnsIdle() {
        assertTrue(engine.stepPcr(null, TRAY));
    }
}
