package com.ciplogic.statemachine;

import com.ciplogic.statemachine.impl.XyzStateChangeEvent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

        stateMachine.changeState(XyzState.RUNNING);
        assertEquals(3, expected[0]);

        stateMachine.changeState(XyzState.STOPPED);
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

        stateMachine.changeState(XyzState.RUNNING);
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
        stateMachine.changeState(XyzState.RUNNING);

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
            stateMachine.changeState(XyzState.RUNNING);
            stateMachine.changeState(XyzState.DEFAULT);
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
        stateMachine.changeState(XyzState.RUNNING);
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
        stateMachine.changeState(XyzState.RUNNING, 3);
        assertEquals(4, expected[0]);
    }

    @Test
    public void changingTheStateInAnAfterListener() {
        XyzStateMachine stateMachine = new XyzStateMachine(XyzState.DEFAULT);
        final long[] expected = {0};

        stateMachine.afterEnter(XyzState.RUNNING, (ev) -> {
            stateMachine.changeState(XyzState.STOPPED);
        });

        stateMachine.afterEnter(XyzState.RUNNING, (ev) -> {
            expected[0] += 1;
        });

        assertEquals(XyzState.DEFAULT, stateMachine.getState());
        stateMachine.changeState(XyzState.RUNNING);
        assertEquals(XyzState.STOPPED, stateMachine.getState());
        assertEquals(1, expected[0]);
    }

    @Test(expected = com.ciplogic.statemachine.impl.XyzStateException.class)
    public void changingTheStateInAnBeforeListenerIsNotAllowed() {
        XyzStateMachine stateMachine = new XyzStateMachine(XyzState.DEFAULT);

        stateMachine.beforeEnter(XyzState.RUNNING, (ev) -> {
            stateMachine.changeState(XyzState.STOPPED);
        });

        assertEquals(XyzState.DEFAULT, stateMachine.getState());
        stateMachine.changeState(XyzState.RUNNING);
    }

    @Test
    public void testDataRouting() {
        XyzStateMachine stateMachine = new XyzStateMachine(XyzState.DEFAULT);

        StringBuilder data = new StringBuilder();

        stateMachine.<String>onData(XyzState.DEFAULT, (name) -> {
             data.append("DEFAULT:").append(name).append(",");
        });
        stateMachine.<String>onData(XyzState.RUNNING, (name) -> {
            data.append("RUNNING:").append(name).append(",");

            return XyzState.STOPPED;
        });

        stateMachine.sendData("default");
        stateMachine.sendData("default");
        stateMachine.changeState(XyzState.RUNNING);
        stateMachine.run();
        stateMachine.sendData("running");
        stateMachine.sendData("running");

        assertEquals(XyzState.STOPPED, stateMachine.getState());
        assertEquals("DEFAULT:default,DEFAULT:default,RUNNING:running,", data.toString());
    }

    @Test
    public void testInvalidTransitionShouldNotWork() {
        XyzStateMachine stateMachine = new XyzStateMachine(XyzState.STOPPED);
        XyzState newState = stateMachine.changeState(XyzState.RUNNING);

        assertEquals(XyzState.STOPPED, newState);
    }

    @Test
    public void testInitializationOnTransitions() {
        XyzStateMachine stateMachine = new XyzStateMachine(XyzState.DEFAULT);
        stateMachine.transition("run");

        assertEquals(XyzState.RUNNING, stateMachine.getState());
    }

    @Test
    public void testResendingData() {
        XyzStateMachine stateMachine = new XyzStateMachine();
        int[] totalSum = {0};

        stateMachine.<Integer>onData(XyzState.DEFAULT, (data) -> {
            stateMachine.sendData(XyzState.RUNNING, data + 2);
        });

        stateMachine.<Integer>onData(XyzState.RUNNING, (data) -> {
            stateMachine.sendData(XyzState.STOPPED, data + 3);
        });

        stateMachine.<Integer>onData(XyzState.STOPPED, (data) -> {
            totalSum[0] = data;
        });

        XyzState state = stateMachine.sendData(1);

        assertEquals(XyzState.STOPPED, state);
        assertEquals(6, totalSum[0]);
        assertEquals(XyzState.STOPPED, stateMachine.getState());
    }

    @Test
    public void testEnteringTheSameStateAgain() {
        XyzStateMachine stateMachine = new XyzStateMachine(XyzState.RUNNING);
        final boolean[] testFailed = {false};

        stateMachine.afterLeave(XyzState.RUNNING, () -> {
            testFailed[0] = true;
        });

        stateMachine.changeState(XyzState.RUNNING);
        assertFalse(testFailed[0]);
    }
}
