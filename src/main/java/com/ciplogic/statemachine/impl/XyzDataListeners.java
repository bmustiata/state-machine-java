package com.ciplogic.statemachine.impl;

import com.ciplogic.statemachine.XyzState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Data listeners are listeners invoked when data is being sent into
 * the state machine.
 */
public class XyzDataListeners<T> {
    private Map<XyzState, Set<Function<XyzDataEvent<T>, XyzState>>> dataListeners = new HashMap<>();

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public XyzDataListenerRegistration<T> onData(XyzState state, Consumer<XyzDataEvent<T>> callback) {
        return onData(state, (e) -> {
            callback.accept(e);

            return null;
        });
    }

    public XyzDataListenerRegistration<T> onData(XyzState state, Function<XyzDataEvent<T>, XyzState> callback) {
        return addListener(state, callback, dataListeners);
    }

    private XyzDataListenerRegistration<T> addListener(XyzState state,
                                                       Function<XyzDataEvent<T>, XyzState> callback,
                                                       Map<XyzState, Set<Function<XyzDataEvent<T>, XyzState>>> callbackCollection) {
        try {
            readWriteLock.writeLock().lock();

            if (state != null) {
                return registerDataListener(state, callback, callbackCollection);
            }

            XyzCompositeDataListenerRegistration<T> result = new XyzCompositeDataListenerRegistration<>();

            for (XyzState state1 : XyzState.values()) {
                result.addListenerRegistration(registerDataListener(state1, callback, callbackCollection));
            }

            return result;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private XyzDataListenerRegistration<T> registerDataListener(XyzState state, Function<XyzDataEvent<T>, XyzState> callback, Map<XyzState, Set<Function<XyzDataEvent<T>, XyzState>>> callbackCollection) {
        Set<Function<XyzDataEvent<T>, XyzState>> items = callbackCollection.get(state);

        if (items == null) {
			items = new LinkedHashSet<>();
			callbackCollection.put(state, items);
		}

        items.add(callback);

        return new XyzDefaultDataListenerRegistration<T>(this, callbackCollection, callback);
    }

    public void removeListener(Map<XyzState, Set<Function<XyzDataEvent<T>, XyzState>>> callbackCollection,
                               Function<XyzDataEvent<T>, XyzState> callback) {
        try {
            readWriteLock.writeLock().lock();
            callbackCollection.remove(callback);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public XyzDataListenersSnapshot<T> copy(XyzState state) {
        return copyEventListeners(state, dataListeners);
    }

    private XyzDataListenersSnapshot<T> copyEventListeners(XyzState event,
                                                           Map<XyzState, Set<Function<XyzDataEvent<T>, XyzState>>> dataListeners) {
        try {
            readWriteLock.readLock().lock();

            Set<Function<XyzDataEvent<T>, XyzState>> dataListenerCallbacks = dataListeners.get(event);

            return new XyzDataListenersSnapshot<T>(
                    dataListenerCallbacks == null ? Collections.emptyList() : new ArrayList<>(dataListenerCallbacks)
            );
        } finally {
            readWriteLock.readLock().unlock();
        }
    }
}
