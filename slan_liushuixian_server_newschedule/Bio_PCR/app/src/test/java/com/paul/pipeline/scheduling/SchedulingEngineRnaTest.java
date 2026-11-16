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
import com.paul.pipeline.model.RnaSchedulingState;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * RNA 状态机纯 JVM 单测：mock SchedulingPort，用真 RnaSchedulingState + DeviceRuntimeState，
 * 锁定第 3 步抽出的 SchedulingEngine.stepRna 行为与原 checkrnawork 一致。
 */
public class SchedulingEngineRnaTest {

    private static final int RNA = 1;
    private static final int TRAY = 5;
    private static final int DISPATCH = 7;

    private SchedulingPort port;
    private SchedulingEngine engine;

    @Before
    public void setUp() {
        port = Mockito.mock(SchedulingPort.class);
        engine = new SchedulingEngine(port);
        // 默认全开的门控，个别用例再覆盖
        when(port.isDoorHold()).thenReturn(false);
        when(port.isWssStart()).thenReturn(true);
        when(port.isSelfCheck()).thenReturn(true);
        when(port.isArmCanuse()).thenReturn(true);
        when(port.currentDispatchIndex(TRAY)).thenReturn(DISPATCH);
    }

    private RnaSchedulingState state(ArmStep step) {
        RnaSchedulingState s = new RnaSchedulingState();
        s.index = RNA;
        s.currentTray = TRAY;
        s.statusFree = true;
        s.exist = false;
        s.armStep = step;
        s.runtime = new DeviceRuntimeState();
        return s;
    }

    // 1. GET 成功下发 -> 占用机械臂 return false
    @Test
    public void get_issuesCommand_returnsBusy() {
        when(port.createGetTray(TRAY)).thenReturn(true);
        RnaSchedulingState s = state(ArmStep.GET);

        boolean idle = engine.stepRna(s, TRAY);

        assertFalse(idle);
        verify(port).createGetTray(TRAY);
        assertTrue(s.runtime.isGettraystart());
        assertFalse(s.runtime.isGettrayfinish());
    }

    // 2. GET 命令下发失败 -> 仍空闲，不置标志
    @Test
    public void get_commandFails_staysIdle() {
        when(port.createGetTray(TRAY)).thenReturn(false);
        RnaSchedulingState s = state(ArmStep.GET);

        boolean idle = engine.stepRna(s, TRAY);

        assertTrue(idle);
        assertFalse(s.runtime.isGettraystart());
    }

    // 3. GET 被开门拦截 -> 不下发命令
    @Test
    public void get_blockedByDoorHold() {
        when(port.isDoorHold()).thenReturn(true);
        RnaSchedulingState s = state(ArmStep.GET);

        boolean idle = engine.stepRna(s, TRAY);

        assertTrue(idle);
        verify(port, never()).createGetTray(anyInt());
    }

    // 4. RUN -> 起模板
    @Test
    public void run_startsTemplate() {
        when(port.startRunTemplateRna(RNA, TRAY, DISPATCH)).thenReturn(true);
        RnaSchedulingState s = state(ArmStep.RUN);

        boolean idle = engine.stepRna(s, TRAY);

        assertTrue(idle); // RUN 成功不改变“空闲”返回值
        verify(port).startRunTemplateRna(RNA, TRAY, DISPATCH);
        assertTrue(s.runtime.isExcutestart());
        assertFalse(s.runtime.isExcutefinish());
    }

    // 5. MOVE 有空闲 PCR -> false + markFinish
    @Test
    public void move_withFreePcr_issuesMove() {
        when(port.findFreePcrIndex()).thenReturn(3);
        when(port.createMoveTray(RNA, 3, TRAY, DISPATCH)).thenReturn(true);
        RnaSchedulingState s = state(ArmStep.MOVE);

        boolean idle = engine.stepRna(s, TRAY);

        assertFalse(idle);
        verify(port).createMoveTray(RNA, 3, TRAY, DISPATCH);
        verify(port).markDispatchDeviceFinish(TRAY, DISPATCH);
        assertTrue(s.runtime.isMovetraystart());
        assertFalse(s.runtime.isMovetrayfinish());
    }

    // 6. MOVE 无空闲 PCR -> armStep=BACK 且 return true（关键回退分支）
    @Test
    public void move_noFreePcr_fallsBackToBack() {
        when(port.findFreePcrIndex()).thenReturn(-1);
        RnaSchedulingState s = state(ArmStep.MOVE);

        boolean idle = engine.stepRna(s, TRAY);

        assertTrue(idle);
        assertEquals(ArmStep.BACK, s.armStep);
        assertFalse(s.runtime.isBacktraystart());
        assertFalse(s.runtime.isBacktrayfinish());
        verify(port, never()).createMoveTray(anyInt(), anyInt(), anyInt(), anyInt());
    }

    // 7. BACK -> false + markFinish
    @Test
    public void back_issuesCommand_returnsBusy() {
        when(port.createBackTray(RNA, TRAY, DISPATCH)).thenReturn(true);
        RnaSchedulingState s = state(ArmStep.BACK);

        boolean idle = engine.stepRna(s, TRAY);

        assertFalse(idle);
        verify(port).createBackTray(RNA, TRAY, DISPATCH);
        verify(port).markDispatchDeviceFinish(TRAY, DISPATCH);
        assertTrue(s.runtime.isBacktraystart());
    }

    // 8. BACK 只需 WSSStart：SelfCheck=false 仍下发（锁 line-525 不对称）
    @Test
    public void back_onlyNeedsWssStart_notSelfCheck() {
        when(port.isSelfCheck()).thenReturn(false);
        when(port.createBackTray(RNA, TRAY, DISPATCH)).thenReturn(true);
        RnaSchedulingState s = state(ArmStep.BACK);

        boolean idle = engine.stepRna(s, TRAY);

        assertFalse(idle);
        verify(port).createBackTray(RNA, TRAY, DISPATCH);
    }

    // 8b. 对照：RUN 在 SelfCheck=false 时不下发（证明不对称是刻意的）
    @Test
    public void run_requiresSelfCheck() {
        when(port.isSelfCheck()).thenReturn(false);
        RnaSchedulingState s = state(ArmStep.RUN);

        boolean idle = engine.stepRna(s, TRAY);

        assertTrue(idle);
        verify(port, never()).startRunTemplateRna(anyInt(), anyInt(), anyInt());
    }

    // 9. FREE + uiinit + !exist -> fireUiInit，清 uiinit
    @Test
    public void free_uiinitAndNotExist_firesUiInit() {
        RnaSchedulingState s = state(ArmStep.FREE);
        s.runtime.setUiinit(true);
        s.exist = false;

        boolean idle = engine.stepRna(s, TRAY);

        assertTrue(idle);
        verify(port).fireUiInit(RNA);
        assertFalse(s.runtime.isUiinit());
    }

    // 10. FREE + uiinit + exist -> 不 fireUiInit，uiinit 不变
    @Test
    public void free_uiinitButExists_doesNotFire() {
        RnaSchedulingState s = state(ArmStep.FREE);
        s.runtime.setUiinit(true);
        s.exist = true;

        boolean idle = engine.stepRna(s, TRAY);

        assertTrue(idle);
        verify(port, never()).fireUiInit(anyInt());
        assertTrue(s.runtime.isUiinit());
    }

    // 11. 托盘不匹配 -> true 且不下任何命令
    @Test
    public void trayMismatch_returnsIdleNoCommand() {
        RnaSchedulingState s = state(ArmStep.GET);

        boolean idle = engine.stepRna(s, 9); // 传入不同 tray

        assertTrue(idle);
        verify(port, never()).createGetTray(anyInt());
    }

    // 12. statusFree=false -> true 且不下任何命令
    @Test
    public void statusNotFree_returnsIdleNoCommand() {
        RnaSchedulingState s = state(ArmStep.GET);
        s.statusFree = false;

        boolean idle = engine.stepRna(s, TRAY);

        assertTrue(idle);
        verify(port, never()).createGetTray(anyInt());
    }

    // 13. null 快照 -> true
    @Test
    public void nullState_returnsIdle() {
        assertTrue(engine.stepRna(null, TRAY));
        verify(port, never()).createGetTray(anyInt());
    }
}
