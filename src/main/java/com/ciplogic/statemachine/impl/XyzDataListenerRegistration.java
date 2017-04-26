package com.ciplogic.statemachine.impl;

import com.ciplogic.statemachine.XyzState;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class XyzDataListenerRegistration<T> {
    private XyzDataListeners<T> stateListeners;
    private final Map<XyzState, Set<Function<T, XyzState>>> callbackCollection;
    private final Function<T, XyzState> callback;

    public XyzDataListenerRegistration(XyzDataListeners<T> stateListeners,
                                       Map<XyzState, Set<Function<T, XyzState>>> callbackCollection,
                                       Function<T, XyzState> callback) {
        this.stateListeners = stateListeners;
        this.callbackCollection = callbackCollection;
        this.callback = callback;
    }

    public void detach() {
        stateListeners.removeListener(callbackCollection, callback);
    }
}
