package com.ciplogic.statemachine;

import java.util.List;
import java.util.function.Consumer;

public class XyzStateListenersSnapshot<T> {
    private final List<Consumer<T>> beforeLeave;
    private final List<Consumer<T>> beforeEnter;
    private final List<Consumer<T>> afterLeave;
    private final List<Consumer<T>> afterEnter;

    public XyzStateListenersSnapshot(List<Consumer<T>> beforeLeave,
                                     List<Consumer<T>> beforeEnter,
                                     List<Consumer<T>> afterLeave,
                                     List<Consumer<T>> afterEnter) {

        this.beforeLeave = beforeLeave;
        this.beforeEnter = beforeEnter;
        this.afterLeave = afterLeave;
        this.afterEnter = afterEnter;
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

    public void notifyTransitionDone(T stateChangeEvent) {
        for (int i = 0; i < afterLeave.size(); i++) {
            callCatchingErrors(stateChangeEvent, afterLeave.get(i));
        }

        for (int i = 0; i < afterEnter.size(); i++) {
            callCatchingErrors(stateChangeEvent, afterEnter.get(i));
        }
    }

    private void callCatchingErrors(T stateChangeEvent, Consumer<T> callback) {
        try {
            callback.accept(stateChangeEvent);
        } catch (Exception e) {
            System.err.printf("%s - %s\n", e, e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
