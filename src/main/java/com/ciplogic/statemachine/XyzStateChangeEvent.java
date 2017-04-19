package com.ciplogic.statemachine;

public class XyzStateChangeEvent {
    private final XyzState previousState;
    private final XyzState newState;
    private boolean cancelled;

    public XyzStateChangeEvent(XyzState previousState, XyzState newState) {
        this.previousState = previousState;
        this.newState = newState;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public XyzState getNewState() {
        return newState;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public XyzState getPreviousState() {
        return previousState;
    }
}
