package com.ciplogic.statemachine.impl;

import com.ciplogic.statemachine.XyzState;

/**
 * An event that is triggered when the state machine transitions
 * from one state to the other.
 */
public class XyzStateChangeEvent<T> {
    private final XyzState previousState;
    private final XyzState targetState;
    private final Object data;

    private boolean cancelled;

    public XyzStateChangeEvent(XyzState previousState, XyzState targetState, Object data) {
        this.previousState = previousState;
        this.targetState = targetState;
        this.data = data;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public XyzState getTargetState() {
        return targetState;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public XyzState getPreviousState() {
        return previousState;
    }

    /**
     * Associated changeState data.
     * @return
     */
    public <T> T getData() {
        return (T) data;
    }
}
