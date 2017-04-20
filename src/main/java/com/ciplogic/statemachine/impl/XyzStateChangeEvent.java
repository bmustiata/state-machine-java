package com.ciplogic.statemachine.impl;

import com.ciplogic.statemachine.XyzState;

/**
 * An event that is triggered when the state machine transitions
 * from one state to the other.
 */
public class XyzStateChangeEvent {
    private final XyzState previousState;
    private final XyzState targetState;
    private boolean cancelled;

    public XyzStateChangeEvent(XyzState previousState, XyzState targetState) {
        this.previousState = previousState;
        this.targetState = targetState;
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
}
