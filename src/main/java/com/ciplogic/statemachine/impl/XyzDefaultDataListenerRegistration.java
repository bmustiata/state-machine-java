package com.ciplogic.statemachine.impl;

import com.ciplogic.statemachine.XyzState;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class XyzDefaultDataListenerRegistration<T> implements XyzDataListenerRegistration<T> {
    private XyzDataListeners<T> stateListeners;
    private final Map<XyzState, Set<Function<XyzDataEvent<T>, XyzState>>> callbackCollection;
    private final Function<XyzDataEvent<T>, XyzState> callback;

    public XyzDefaultDataListenerRegistration(XyzDataListeners<T> stateListeners,
                                              Map<XyzState, Set<Function<XyzDataEvent<T>, XyzState>>> callbackCollection,
                                              Function<XyzDataEvent<T>, XyzState> callback) {
        this.stateListeners = stateListeners;
        this.callbackCollection = callbackCollection;
        this.callback = callback;
    }

    @Override
    public void detach() {
        stateListeners.removeListener(callbackCollection, callback);
    }
}
