package com.ciplogic.statemachine;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class XyzStateListenerRegistration<T> {
    private XyzStateListeners<T> xyzStateListeners;
    private final Map<XyzState, Set<Consumer<T>>> callbackCollection;
    private final Consumer<T> callback;

    public XyzStateListenerRegistration(XyzStateListeners<T> xyzStateListeners,
                                        Map<XyzState, Set<Consumer<T>>> callbackCollection,
                                        Consumer<T> callback) {
        this.xyzStateListeners = xyzStateListeners;
        this.callbackCollection = callbackCollection;
        this.callback = callback;
    }

    void detach() {
        xyzStateListeners.removeListener(callbackCollection, callback);
    }
}
