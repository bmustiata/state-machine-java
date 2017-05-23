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
    private static Set<Integer> transitionSet = new HashSet<>();
    private static Map<Integer, Map<String, Integer>> linkMap = new HashMap<>();

    private final XyzState initialState;
    private volatile XyzState currentState;

    private volatile XyzStateChangeEvent currentChangeEvent;

    private XyzStateListeners<XyzStateChangeEvent> listeners = new XyzStateListeners<>();
    private XyzDataListeners dataListeners = new XyzDataListeners();

    //BEGIN_HANDLEBARS
    //{{#each properties}}
    //{{#if this.default}}
    //    private {{this.type}} {{@key}} = {{this.default}};
    //{{else}}
    //    private {{this}} {{@key}};
    //{{/if}}
    //{{/each}}
    //END_HANDLEBARS

    static {
        // BEGIN_TRANSITIONS: XyzStateMachine.registerTransition("TRANSITION_NAME", XyzState.FROM_STATE, XyzState.TO_STATE);
        XyzStateMachine.registerTransition("run", XyzState.DEFAULT, XyzState.RUNNING);
        XyzStateMachine.registerTransition(null, XyzState.DEFAULT, XyzState.STOPPED);
        XyzStateMachine.registerTransition(null, XyzState.RUNNING, XyzState.DEFAULT);
        XyzStateMachine.registerTransition(null, XyzState.RUNNING, XyzState.STOPPED);
        XyzStateMachine.registerTransition(null, XyzState.RUNNING, XyzState.RUNNING);
        // END_TRANSITIONS
    }

    public XyzStateMachine() {
        this(XyzState.values()[0]);
    }

    public XyzStateMachine(XyzState initialState) {
        if (initialState == null) {
            throw new IllegalArgumentException("Can not start state machine. Initial state is null.");
        }

        // initial state
        this.initialState = initialState;
    }

    // BEGIN_TRANSITION_SET: public XyzState TRANSITION_NAME() { return this.transition("TRANSITION_NAME"); }
    public XyzState run() { return this.transition("run"); }
    // END_TRANSITION_SET
    // BEGIN_TRANSITION_SET: public XyzState TRANSITION_NAME(Object data) { return this.transition("TRANSITION_NAME", data); }
    public XyzState run(Object data) { return this.transition("run", data); }
    // END_TRANSITION_SET

    private static void registerTransition(String connectionName, XyzState fromState, XyzState toState) {
        transitionSet.add(fromState.ordinal() << 14 | toState.ordinal());

        if (connectionName == null) {
            return;
        }

        linkMap.computeIfAbsent(fromState.ordinal(), x -> new HashMap<>())
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
        ensureStateMachineInitialized();

        return changeStateImpl(targetState, data);
    }

    private XyzState changeStateImpl(XyzState targetState, Object data) {
        if (targetState == null) {
            throw new NullPointerException("targetState is null. Can not changeState.");
        }

        // since the listeners are synchronized themselves, we can copy there before, so we don't do
        // that in the lock.
        XyzStateChangeEvent stateChangeEvent = new XyzStateChangeEvent(currentState, targetState, data);
        XyzStateListenersSnapshot<XyzStateChangeEvent> beforeListenersCopy = listeners.copyBefore(stateChangeEvent);
        XyzStateListenersSnapshot<XyzStateChangeEvent> afterListenersCopy = listeners.copyAfter(stateChangeEvent);

        // don't fire a new event, since this might change
        synchronized (this) {
            if (currentState == targetState) {
                return currentState;
            }

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

            currentChangeEvent = stateChangeEvent;
            stateChangeEvent = beforeListenersCopy.notifyTransition(stateChangeEvent);

            if (stateChangeEvent.isCancelled()) {
                return currentState; // state not changed.
            }

            this.currentState = targetState;
            this.currentChangeEvent = null;

            afterListenersCopy.notifyTransition(stateChangeEvent);

            return this.currentState;
        }
    }

    public XyzState getState() {
        ensureStateMachineInitialized();

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

    public XyzStateListenerRegistration<XyzStateChangeEvent> beforeEnter(XyzState state,
                                                                         Runnable callback) {
        return listeners.beforeEnter(state, (ev) -> callback.run());
    }

    public XyzStateListenerRegistration<XyzStateChangeEvent> afterEnter(XyzState state,
                                                                        Runnable callback) {
        return listeners.afterEnter(state, (ev) -> callback.run());
    }

    public XyzStateListenerRegistration<XyzStateChangeEvent> afterLeave(XyzState state,
                                                                        Runnable callback) {
        return listeners.afterLeave(state, (ev) -> callback.run());
    }

    public XyzStateListenerRegistration<XyzStateChangeEvent> beforeLeave(XyzState state,
                                                                         Runnable callback) {
        return listeners.beforeLeave(state, (ev) -> callback.run());
    }

    public <T> XyzDataListenerRegistration<T> onData(XyzState state, Consumer<T> callback) {
        return dataListeners.onData(state, callback);
    }

    public <T> XyzDataListenerRegistration<T> onData(XyzState state, Function<T, XyzState> callback) {
        return dataListeners.onData(state, callback);
    }

    /**
     * Forward the data for another state, ignoring the resulting state,
     * so we can just short circuit the execution with
     * `return stateMachine.forwardData(..)`
     * @param state
     * @param data
     * @param <T>
     * @return
     */
    public <T> XyzState forwardData(XyzState state, T data) {
        sendData(state, data);
        return null;
    }

    /**
     * Attempt at changing the state to the new state first, then pass
     * the data to the state machine.
     *
     * @param state
     * @param data
     * @param <T>
     * @return
     */
    public <T> XyzState sendData(XyzState state, T data) {
        ensureStateMachineInitialized();

        changeState(state);

        return sendData(data);
    }

    /**
     * Send some data into the state machine. This will be processed
     * by the current state.
     * @param data
     * @param <T>
     * @return
     */
    public <T> XyzState sendData(T data) {
        ensureStateMachineInitialized();

        XyzDataListenersSnapshot listeners = dataListeners.copy(currentState);
        XyzState newState = listeners.notifyData(data);

        if (newState != null) {
            return changeState(newState);
        }

        return currentState;
    }

    private void ensureStateMachineInitialized() {
        if (this.currentState == null) {
            changeStateImpl(this.initialState, null);
        }
    }

    public XyzState transition(String linkName) {
        return this.transition(linkName, null);
    }

    public XyzState transition(String linkName, Object data) {
        this.ensureStateMachineInitialized();

        Map<String, Integer> linkMap = this.linkMap.get(currentState.ordinal());

        if (linkMap == null) {
            return null;
        }

        Integer targetStateIndex = linkMap.get(linkName);

        if (targetStateIndex == null) {
            return null;
        }

        return changeState(XyzState.values()[targetStateIndex], data);
    }

    // BEGIN_HANDLEBARS
    //{{#each properties}}
    //{{#if this.default}}
    //    public void set{{capitalize @key}}({{this.type}} value) {
    //        this.{{@key}} = value;
    //    }
    //
    //    public {{this.type}} get{{capitalize @key}}() {
    //        return this.{{@key}};
    //    }
    //
    //{{else}}
    //    public void set{{capitalize @key}}({{this}} value) {
    //        this.{{@key}} = value;
    //    }
    //
    //    public {{this}} get{{capitalize @key}}() {
    //        return this.{{@key}};
    //    }
    //
    //{{/if}}
    //{{/each}}
    // END_HANDLEBARS

}