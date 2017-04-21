package com.ciplogic.statemachine.impl;

import com.ciplogic.statemachine.XyzState;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class XyzStateListenerRegistration<T> {
    private XyzStateListeners<T> stateListeners;
    private final Map<XyzState, Set<Consumer<T>>> callbackCollection;
    private final Consumer<T> callback;

    public XyzStateListenerRegistration(XyzStateListeners<T> stateListeners,
                                        Map<XyzState, Set<Consumer<T>>> callbackCollection,
                                        Consumer<T> callback) {
        this.stateListeners = stateListeners;
        this.callbackCollection = callbackCollection;
        this.callback = callback;
    }

    void detach() {
        stateListeners.removeListener(callbackCollection, callback);
    }
}
