package com.ciplogic.statemachine;


import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class XyzStateMachine {
    private Set<Integer> transitionSet = new HashSet<>();

    private volatile XyzState currentState;
    private XyzStateListeners<XyzStateChangeEvent> listeners = new XyzStateListeners<>();

    public XyzStateMachine() {
        this(XyzState.values()[0]);
    }

    public XyzStateMachine(XyzState initialState) {
        this.currentState = initialState;

        // Transitions are serialized here.
        this.transitionSet.add(XyzState.DEFAULT.ordinal() << 14 | XyzState.RUNNING.ordinal());
        this.transitionSet.add(XyzState.RUNNING.ordinal() << 14 | XyzState.DEFAULT.ordinal());
        this.transitionSet.add(XyzState.RUNNING.ordinal() << 14 | XyzState.STOPPED.ordinal());
    }

    public void transition(XyzState targetState) {
        if (targetState == null) {
            throw new NullPointerException("targetState is null. Can not transition.");
        }

        // don't fire a new event, since this might change
        synchronized (this) {
            if (!transitionSet.contains(currentState.ordinal() << 14 | targetState.ordinal())) {
                throw new IllegalArgumentException(String.format(
                        "No transition exists between %s -> %s.",
                        currentState.name(),
                        targetState.name()
                ));
            }

            XyzStateChangeEvent stateChangeEvent = new XyzStateChangeEvent(currentState, targetState);

            XyzStateListenersSnapshot<XyzStateChangeEvent> listenersCopy = listeners.copy(stateChangeEvent);

            stateChangeEvent = listenersCopy.notifyTransition(stateChangeEvent);

            if (stateChangeEvent.isCancelled()) {
                return;
            }

            this.currentState = targetState;

            listenersCopy.notifyTransitionDone(stateChangeEvent);
        }
    }

    public XyzState getState() {
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