package com.ciplogic.statemachine;


import com.ciplogic.statemachine.impl.XyzStateChangeEvent;
import com.ciplogic.statemachine.impl.XyzStateListenerRegistration;
import com.ciplogic.statemachine.impl.XyzStateListeners;
import com.ciplogic.statemachine.impl.XyzStateListenersSnapshot;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class XyzStateMachine {
    private Set<Integer> transitionSet = new HashSet<>();

    private final XyzState initialState;
    private volatile XyzState currentState;

    private XyzStateListeners<XyzStateChangeEvent> listeners = new XyzStateListeners<>();

    public XyzStateMachine() {
        this(XyzState.values()[0]);
    }

    public XyzStateMachine(XyzState initialState) {
        if (initialState == null) {
            throw new IllegalArgumentException("Can not start state machine. Initial state is null.");
        }

        // BEGIN_TRANSITIONS: this.transitionSet.add(XyzState.FROM_STATE.ordinal() << 14 | XyzState.TO_STATE.ordinal());
        this.transitionSet.add(XyzState.DEFAULT.ordinal() << 14 | XyzState.RUNNING.ordinal());
        this.transitionSet.add(XyzState.RUNNING.ordinal() << 14 | XyzState.DEFAULT.ordinal());
        this.transitionSet.add(XyzState.RUNNING.ordinal() << 14 | XyzState.STOPPED.ordinal());
        // END_TRANSITIONS

        // initial transition
        this.initialState = initialState;
    }

    /**
     * Transition with no data.
     *
     * Attempts to transition the state machine into the new state.
     * In case the state cannot be changed, the old state will be returned.
     *
     * A transition must exist from the current state to the target state. If
     * no such transition exists, the current state will be returned.
     *
     * @param targetState The desired state we want the state machine to transition.
     * @return The current state where the machine was transitioned,
     * or the old value, in case the state machine could not be transitioned.

     * @return
     */
    public XyzState transition(XyzState targetState) {
        return this.transition(targetState, null);
    }


    /**
     * Attempts to transition the state machine into the new state.
     * In case the state cannot be changed, the old state will be returned.
     *
     * A transition must exist from the current state to the target state. If
     * no such transition exists, the current state will be returned.
     *
     * @param targetState The desired state we want the state machine to transition.
     * @param data The data to pass into the change state event.
     * @return The current state where the machine was transitioned,
     * or the old value, in case the state machine could not be transitioned.
     */
    public XyzState transition(XyzState targetState, Object data) {
        // the state machine was not initialized yet.
        if (currentState == null && targetState != this.initialState) {
            transition(this.initialState, data);
        }

        if (targetState == null) {
            throw new NullPointerException("targetState is null. Can not transition.");
        }

        // don't fire a new event, since this might change
        synchronized (this) {
            if (currentState != null && // if the currentState == null, we're initializing
                !transitionSet.contains(currentState.ordinal() << 14 | targetState.ordinal())) {
                System.err.println(String.format(
                        "No transition exists between %s -> %s.",
                        currentState.name(),
                        targetState.name()
                ));

                return currentState;
            }

            XyzStateChangeEvent stateChangeEvent = new XyzStateChangeEvent(currentState, targetState, data);

            XyzStateListenersSnapshot<XyzStateChangeEvent> listenersCopy = listeners.copy(stateChangeEvent);

            stateChangeEvent = listenersCopy.notifyTransition(stateChangeEvent);

            if (stateChangeEvent.isCancelled()) {
                return currentState; // state not changed.
            }

            this.currentState = targetState;

            listenersCopy.notifyTransitionDone(stateChangeEvent);

            return this.currentState;
        }
    }

    public XyzState getState() {
        if (this.currentState == null) {
            transition(this.initialState);
        }

        return this.currentState;
    }

    public XyzStateListenerRegistration<XyzStateChangeEvent> beforeEnter(XyzState state,
                                                                         Consumer<XyzStateChangeEvent> callback) {
        return listeners.beforeEnter(state, callback);
    }

    public XyzStateListenerRegistration<XyzStateChangeEvent> afterEnter(XyzState state,
                                                                        Consumer<XyzStateChangeEvent> callback) {
        return listeners.afterEnter(state, callback);
    }

    public XyzStateListenerRegistration<XyzStateChangeEvent> afterLeave(XyzState state,
                                                                        Consumer<XyzStateChangeEvent> callback) {
        return listeners.afterLeave(state, callback);
    }

    public XyzStateListenerRegistration<XyzStateChangeEvent> beforeLeave(XyzState state,
                                                                         Consumer<XyzStateChangeEvent> callback) {
        return listeners.beforeLeave(state, callback);
    }
}