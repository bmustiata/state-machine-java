package com.ciplogic.statemachine;


import com.ciplogic.statemachine.impl.XyzDataListenerRegistration;
import com.ciplogic.statemachine.impl.XyzDataListeners;
import com.ciplogic.statemachine.impl.XyzDataListenersSnapshot;
import com.ciplogic.statemachine.impl.XyzStateChangeEvent;
import com.ciplogic.statemachine.impl.XyzStateException;
import com.ciplogic.statemachine.impl.XyzStateListenerRegistration;
import com.ciplogic.statemachine.impl.XyzStateListeners;
import com.ciplogic.statemachine.impl.XyzStateListenersSnapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class XyzStateMachine {
    private Set<Integer> transitionSet = new HashSet<>();
    private Map<Integer, Map<String, Integer>> linkMap = new HashMap<>();

    private final XyzState initialState;
    private volatile XyzState currentState;

    private volatile XyzStateChangeEvent currentChangeEvent;

    private XyzStateListeners<XyzStateChangeEvent> listeners = new XyzStateListeners<>();
    private XyzDataListeners dataListeners = new XyzDataListeners();

    public XyzStateMachine() {
        this(XyzState.values()[0]);
    }

    public XyzStateMachine(XyzState initialState) {
        if (initialState == null) {
            throw new IllegalArgumentException("Can not start state machine. Initial state is null.");
        }

        // BEGIN_TRANSITIONS: this.registerTransition(TRANSITION_NAME`null`, XyzState.FROM_STATE, XyzState.TO_STATE);
        this.registerTransition("run", XyzState.DEFAULT, XyzState.RUNNING);
        this.registerTransition(null, XyzState.DEFAULT, XyzState.STOPPED);
        this.registerTransition(null, XyzState.RUNNING, XyzState.DEFAULT);
        this.registerTransition(null, XyzState.RUNNING, XyzState.STOPPED);
        // END_TRANSITIONS


        // initial state
        this.initialState = initialState;
    }

    private void registerTransition(String connectionName, XyzState fromState, XyzState toState) {
        this.transitionSet.add(fromState.ordinal() << 16 | toState.ordinal());

        if (connectionName == null) {
            return;
        }

        this.linkMap.computeIfAbsent(fromState.ordinal(), x -> new HashMap<>())
            .computeIfAbsent(connectionName, x -> toState.ordinal());
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
    public XyzState changeState(XyzState targetState) {
        return this.changeState(targetState, null);
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
    public XyzState changeState(XyzState targetState, Object data) {
        // the state machine was not initialized yet.
        ensureInitialized();

        return changeStateImpl(targetState, data);
    }

    private XyzState changeStateImpl(XyzState targetState, Object data) {
        if (targetState == null) {
            throw new NullPointerException("targetState is null. Can not changeState.");
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

            if (currentChangeEvent != null) {
                throw new XyzStateException(String.format(
                        "The XyzStateMachine is already in a changeState (%s -> %s). " +
                        "Transitioning the state machine (%s -> %s) in `before` events is not supported.",
                        currentChangeEvent.getPreviousState(),
                        currentChangeEvent.getTargetState(),
                        currentState,
                        targetState
                ));
            }

            XyzStateChangeEvent stateChangeEvent = new XyzStateChangeEvent(currentState, targetState, data);
            currentChangeEvent = stateChangeEvent;

            XyzStateListenersSnapshot<XyzStateChangeEvent> beforeListenersCopy = listeners.copyBefore(stateChangeEvent);

            stateChangeEvent = beforeListenersCopy.notifyTransition(stateChangeEvent);

            if (stateChangeEvent.isCancelled()) {
                return currentState; // state not changed.
            }

            this.currentState = targetState;

            this.currentChangeEvent = null;

            XyzStateListenersSnapshot<XyzStateChangeEvent> afterListenersCopy = listeners.copyAfter(stateChangeEvent);
            afterListenersCopy.notifyTransition(stateChangeEvent);

            return this.currentState;
        }
    }

    public XyzState getState() {
        ensureInitialized();

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

    public <T> XyzDataListenerRegistration<T> onData(XyzState state, Consumer<T> callback) {
        return dataListeners.onData(state, callback);
    }

    public <T> XyzDataListenerRegistration<T> onData(XyzState state, Function<T, XyzState> callback) {
        return dataListeners.onData(state, callback);
    }


    public <T> XyzState sendData(T data) {
        ensureInitialized();

        XyzDataListenersSnapshot listeners = dataListeners.copy(currentState);
        XyzState newState = listeners.notifyData(data);

        if (newState != null) {
            return changeState(newState);
        }

        return currentState;
    }

    private void ensureInitialized() {
        if (this.currentState == null) {
            changeStateImpl(this.initialState, null);
        }
    }

    public void transition(String linkName) {
        this.transition(linkName, null);
    }

    public void transition(String linkName, Object data) {
        Map<String, Integer> linkMap = this.linkMap.get(currentState.ordinal());

        if (linkMap == null) {
            return; // the state doesn't defines named transitions.
        }

        Integer targetStateIndex = linkMap.get(linkName);

        if (targetStateIndex == null) {
            return; // there is no link with that name.
        }

        changeState(XyzState.values()[targetStateIndex], data);
    }
}