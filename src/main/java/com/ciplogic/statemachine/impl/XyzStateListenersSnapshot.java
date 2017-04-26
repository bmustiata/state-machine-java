package com.ciplogic.statemachine.impl;

import java.util.List;
import java.util.function.Consumer;

public class XyzStateListenersSnapshot<T> {
    private final List<Consumer<T>> beforeLeave;
    private final List<Consumer<T>> beforeEnter;

    public XyzStateListenersSnapshot(List<Consumer<T>> beforeLeave,
                                     List<Consumer<T>> beforeEnter) {

        this.beforeLeave = beforeLeave;
        this.beforeEnter = beforeEnter;
    }

    public T notifyTransition(T stateChangeEvent) {
        for (int i = 0; i < beforeLeave.size(); i++) {
            callCatchingErrors(stateChangeEvent, beforeLeave.get(i));
        }

        for (int i = 0; i < beforeEnter.size(); i++) {
            callCatchingErrors(stateChangeEvent, beforeEnter.get(i));
        }

        return stateChangeEvent;
    }

    private void callCatchingErrors(T stateChangeEvent, Consumer<T> callback) {
        try {
            callback.accept(stateChangeEvent);
        } catch (Exception e) {
            if (e instanceof XyzStateException) {
                throw e;
            }

            System.err.printf("%s - %s\n", e, e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
