package com.ciplogic.statemachine.impl;

import com.ciplogic.statemachine.XyzState;

import java.util.List;
import java.util.function.Function;

public class XyzDataListenersSnapshot<T> {
    private final List<Function<T, XyzState>> dataListeners;

    public XyzDataListenersSnapshot(List<Function<T, XyzState>> dataListeners) {

        this.dataListeners = dataListeners;
    }

    public XyzState notifyData(T data) {
        XyzState result = null;

        for (int i = 0; i < dataListeners.size(); i++) {
            XyzState newResult = throwEventualErrors(data, dataListeners.get(i));

            if (newResult == null) {
                continue;
            }

            if (result == null) {
                result = newResult;
                continue;
            }

            if (result != newResult) {
                throw new XyzStateException(String.format(
                        "Data listeners return both: %s and %s for the state machine to transition.",
                        newResult,
                        result));
            }
        }

        return result;
    }

    private XyzState throwEventualErrors(T data, Function<T, XyzState> callback) {
        try {
            return callback.apply(data);
        } catch (Exception e) {
            if (e instanceof XyzStateException) {
                throw e;
            }

            throw new XyzStateException("Unable to process data.", e);
        }
    }
}
