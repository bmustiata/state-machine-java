package com.ciplogic.statemachine;

/**
 * Defines a transition.
 */
public class XyzTransition {
    private XyzState startState;
    private XyzState endState;

    public XyzTransition(XyzState startState, XyzState endState) {
        this.startState = startState;
        this.endState = endState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XyzTransition that = (XyzTransition) o;

        if (startState != that.startState) return false;
        return endState == that.endState;

    }

    @Override
    public int hashCode() {
        int result = startState != null ? startState.hashCode() : 0;
        result = 31 * result + (endState != null ? endState.hashCode() : 0);
        return result;
    }
}
