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

/**
 * Holds the references to all the listeners, in an easily
 * accessible data structure, and allows calling them
 * for updates.
 */
public class XyzStateListeners<T> {
    private Map<XyzState, Set<Consumer<T>>> beforeEnter = new HashMap<>();
    private Map<XyzState, Set<Consumer<T>>> afterEnter = new HashMap<>();

    private Map<XyzState, Set<Consumer<T>>> beforeLeave = new HashMap<>();
    private Map<XyzState, Set<Consumer<T>>> afterLeave = new HashMap<>();

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public XyzStateListenerRegistration<T> beforeEnter(XyzState state, Consumer<T> callback) {
        return addListener(state, callback, beforeEnter);
    }

    public XyzStateListenerRegistration<T> afterEnter(XyzState state, Consumer<T> callback) {
        return addListener(state, callback, afterEnter);
    }

    public XyzStateListenerRegistration<T> beforeLeave(XyzState state, Consumer<T> callback) {
        return addListener(state, callback, beforeLeave);
    }

    public XyzStateListenerRegistration<T> afterLeave(XyzState state, Consumer<T> callback) {
        return addListener(state, callback, afterLeave);
    }

    private XyzStateListenerRegistration<T> addListener(XyzState state, Consumer<T> callback, Map<XyzState, Set<Consumer<T>>> callbackCollection) {
        try {
            readWriteLock.writeLock().lock();

            Set<Consumer<T>> items = callbackCollection.get(state);

            if (items == null) {
                items = new LinkedHashSet<>();
                callbackCollection.put(state, items);
            }

            items.add(callback);

            return new XyzStateListenerRegistration<T>(this, callbackCollection, callback);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public void removeListener(Map<XyzState, Set<Consumer<T>>> callbackCollection, Consumer<T> callback) {
        try {
            readWriteLock.writeLock().lock();
            callbackCollection.remove(callback);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public XyzStateListenersSnapshot<T> copyBefore(XyzStateChangeEvent event) {
        return copyEventListeners(event, beforeLeave, beforeEnter);
    }

    public XyzStateListenersSnapshot<T> copyAfter(XyzStateChangeEvent event) {
        return copyEventListeners(event, afterLeave, afterEnter);
    }

    private XyzStateListenersSnapshot<T> copyEventListeners(XyzStateChangeEvent event, Map<XyzState, Set<Consumer<T>>> leaveListeners, Map<XyzState, Set<Consumer<T>>> enterListeners) {
        try {
            readWriteLock.readLock().lock();

            Set<Consumer<T>> afterLeaveCallbacks = leaveListeners.get(event.getPreviousState());
            Set<Consumer<T>> afterEnterCallbacks = enterListeners.get(event.getTargetState());

            return new XyzStateListenersSnapshot<T>(
                    afterLeaveCallbacks == null ? Collections.emptyList() : new ArrayList<>(afterLeaveCallbacks),
                    afterEnterCallbacks == null ? Collections.emptyList() : new ArrayList<>(afterEnterCallbacks)
            );
        } finally {
            readWriteLock.readLock().unlock();
        }
    }
}
