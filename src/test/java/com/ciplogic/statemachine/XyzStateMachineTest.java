package com.ciplogic.statemachine;

import com.ciplogic.statemachine.impl.XyzDataEvent;
import com.ciplogic.statemachine.impl.XyzStateChangeEvent;
import org.junit.Test;
import org.omg.CORBA.INITIALIZE;

import javax.print.attribute.standard.RequestingUserName;
import java.awt.*;

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

        stateMachine.<String>onData(XyzState.DEFAULT, (dataEvent) -> {
             data.append("DEFAULT:").append(dataEvent.getData()).append(",");
        });
        stateMachine.<String>onData(XyzState.RUNNING, (dataEvent) -> {
            data.append("RUNNING:").append(dataEvent.getData()).append(",");

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

        stateMachine.<Integer>onData(XyzState.DEFAULT, (dataEvent) -> {
            stateMachine.sendData(XyzState.RUNNING, dataEvent.getData() + 2);
        });

        stateMachine.<Integer>onData(XyzState.RUNNING, (dataEvent) -> {
            stateMachine.sendData(XyzState.STOPPED, dataEvent.getData() + 3);
        });

        stateMachine.<Integer>onData(XyzState.STOPPED, (dataEvent) -> {
            totalSum[0] = dataEvent.getData();
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

    @Test
    public void testListeningForAllTransitions() {
        final int[] currentCount = { 0 };

        XyzStateMachine stateMachine = new XyzStateMachine();
        stateMachine.afterEnter(null, () -> currentCount[0]++);

        stateMachine.transition("run");
        assertEquals(2, currentCount[0]);
        stateMachine.changeState(XyzState.STOPPED);
        assertEquals(3, currentCount[0]);
    }

    @Test
    public void testConsumingDataDoesNotCallTheNextListener() {
        XyzStateMachine stateMachine = new XyzStateMachine();
        final int[] currentCount = { 10 };

        stateMachine.onData(XyzState.RUNNING, (XyzDataEvent<Integer> ev) -> { currentCount[0] += ev.getData(); ev.consume(); });
        stateMachine.onData(null, (XyzDataEvent<Integer> ev) -> { currentCount[0] -= 1; });

        stateMachine.sendData(1); // this should substract 1 since running is not matching, to consume the event.
        assertEquals(9, currentCount[0]);
        stateMachine.changeState(XyzState.RUNNING);

        stateMachine.sendData(2); // this should add 2
        assertEquals(11, currentCount[0]);
        stateMachine.changeState(XyzState.STOPPED);

        stateMachine.sendData(1); // this should substract 1 since running is not matching, to consume the event.
        assertEquals(10, currentCount[0]);
    }

    @Test
    public void testTypedDataGetsRoutedCorrectly() {
        XyzStateMachine stateMachine = new XyzStateMachine(XyzState.RUNNING);
        final int[] currentCount = { 0 };

        stateMachine.onData(XyzState.RUNNING, Integer.class, (XyzDataEvent<Integer> ev) -> {
            currentCount[0] += ev.getData();
        });
        stateMachine.onData(XyzState.RUNNING, (XyzDataEvent<Number> ev) -> {
            currentCount[0] += ev.getData().intValue();
        });

        stateMachine.sendData(1L); // should be added by the Number listener only

        assertEquals(1, currentCount[0]);

        stateMachine.sendData(10);   // should be added by both listeners

        assertEquals(21, currentCount[0]);
    }
}
