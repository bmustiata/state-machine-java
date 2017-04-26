package com.ciplogic.statemachine;

import com.ciplogic.statemachine.impl.XyzStateChangeEvent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XyzStateMachineTest {
    @Test
    public void testStateMachine() {
        XyzStateMachine stateMachine = new XyzStateMachine();

        final long[] expected = {0};

        stateMachine.beforeEnter(XyzState.RUNNING, (XyzStateChangeEvent ev) -> {
            assertEquals(0, expected[0]);
            expected[0] += 1;
        });

        stateMachine.afterEnter(XyzState.RUNNING, (XyzStateChangeEvent ev) -> {
            assertEquals(1, expected[0]);
            expected[0] += 2;
        });


        stateMachine.beforeLeave(XyzState.RUNNING, (XyzStateChangeEvent ev) -> {
            assertEquals(3, expected[0]);
            expected[0] += 3;
        });

        stateMachine.afterLeave(XyzState.RUNNING, (XyzStateChangeEvent ev) -> {
            assertEquals(6, expected[0]);
            expected[0] += 4;
        });

        stateMachine.transition(XyzState.RUNNING);
        assertEquals(3, expected[0]);

        stateMachine.transition(XyzState.STOPPED);
        assertEquals(10, expected[0]);
    }

    @Test
    public void failedListenerDoesntFailStateMachine() {
        XyzStateMachine stateMachine = new XyzStateMachine();

        final long[] expected = {0};

        stateMachine.beforeEnter(XyzState.RUNNING, (XyzStateChangeEvent ev) -> {
            assertEquals(0, expected[0]);
            expected[0] += 1;
        });

        stateMachine.afterEnter(XyzState.RUNNING, (XyzStateChangeEvent ev) -> {
            throw new IllegalArgumentException("test error");
        });

        stateMachine.transition(XyzState.RUNNING);
        assertEquals(1, expected[0]);
    }

    @Test
    public void cancellingEventsStopsTransitions() {
        XyzStateMachine stateMachine = new XyzStateMachine();
        final long[] expected = {0};

        stateMachine.beforeEnter(XyzState.RUNNING, (XyzStateChangeEvent ev) -> {
            assertEquals(0, expected[0]);
            expected[0] += 1;

            ev.cancel();
        });

        stateMachine.afterEnter(XyzState.RUNNING, (XyzStateChangeEvent ev) -> {
            assertTrue("Should not enter, since the event was cancelled", false);
        });

        assertEquals(XyzState.DEFAULT, stateMachine.getState());
        stateMachine.transition(XyzState.RUNNING);

        assertEquals(XyzState.DEFAULT, stateMachine.getState());
        assertEquals(1, expected[0]);
    }


    @Test
    public void performanceTest() {
        XyzStateMachine stateMachine = new XyzStateMachine();
        final long[] expected = {0};

        stateMachine.beforeEnter(XyzState.RUNNING, (XyzStateChangeEvent ev) -> {
            expected[0] += 1;
        });

        stateMachine.afterEnter(XyzState.RUNNING, (XyzStateChangeEvent ev) -> {
            expected[0] += 2;
        });


        stateMachine.beforeLeave(XyzState.RUNNING, (XyzStateChangeEvent ev) -> {
            expected[0] += 3;
        });

        stateMachine.afterLeave(XyzState.RUNNING, (XyzStateChangeEvent ev) -> {
            expected[0] += 4;
        });

        for (int i = 0; i < 10_000_000; i++) {
            stateMachine.transition(XyzState.RUNNING);
            stateMachine.transition(XyzState.DEFAULT);
        }

        assertEquals(100_000_000, expected[0]);
    }

    @Test
    public void initialStateTest() {
        XyzStateMachine stateMachine = new XyzStateMachine(XyzState.DEFAULT);
        final long[] expected = {0};

        stateMachine.beforeEnter(XyzState.DEFAULT, (ev) -> {
            expected[0] += 1;
        });

        stateMachine.beforeLeave(XyzState.DEFAULT, (ev) -> {
            expected[0] += 2;
        });

        assertEquals(0, expected[0]);
        stateMachine.transition(XyzState.RUNNING);
        assertEquals(3, expected[0]);
    }

    @Test
    public void dataGetsPassedIntoTheEvent() {
        XyzStateMachine stateMachine = new XyzStateMachine(XyzState.DEFAULT);
        final long[] expected = {0};

        stateMachine.beforeLeave(XyzState.DEFAULT, (ev) -> {
            expected[0] += 1 + (Integer) ev.getData();
        });

        assertEquals(0, expected[0]);
        stateMachine.transition(XyzState.RUNNING, 3);
        assertEquals(4, expected[0]);
    }

    @Test
    public void changingTheStateInAnAfterListener() {
        XyzStateMachine stateMachine = new XyzStateMachine(XyzState.DEFAULT);
        final long[] expected = {0};

        stateMachine.afterEnter(XyzState.RUNNING, (ev) -> {
            stateMachine.transition(XyzState.STOPPED);
        });

        stateMachine.afterEnter(XyzState.RUNNING, (ev) -> {
            expected[0] += 1;
        });

        assertEquals(XyzState.DEFAULT, stateMachine.getState());
        stateMachine.transition(XyzState.RUNNING);
        assertEquals(XyzState.STOPPED, stateMachine.getState());
        assertEquals(1, expected[0]);
    }

    @Test(expected = com.ciplogic.statemachine.impl.XyzStateException.class)
    public void changingTheStateInAnBeforeListenerIsNotAllowed() {
        XyzStateMachine stateMachine = new XyzStateMachine(XyzState.DEFAULT);

        stateMachine.beforeEnter(XyzState.RUNNING, (ev) -> {
            stateMachine.transition(XyzState.STOPPED);
        });

        assertEquals(XyzState.DEFAULT, stateMachine.getState());
        stateMachine.transition(XyzState.RUNNING);
    }
}
