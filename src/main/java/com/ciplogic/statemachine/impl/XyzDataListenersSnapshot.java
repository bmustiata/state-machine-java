package com.ciplogic.statemachine.impl;

import com.ciplogic.statemachine.XyzState;

import java.util.List;
import java.util.function.Function;

public class XyzDataListenersSnapshot<T> {
    private final List<Function<XyzDataEvent<T>, XyzState>> dataListeners;

    public XyzDataListenersSnapshot(List<Function<XyzDataEvent<T>, XyzState>> dataListeners) {

        this.dataListeners = dataListeners;
    }

    public XyzState notifyData(T data) {
        XyzState result = null;

        XyzDataEvent<T> dataEvent = new XyzDataEvent<>(data);

        for (int i = 0; i < dataListeners.size(); i++) {
            if (dataEvent.isConsumed()) {
                break;
            }

            XyzState newResult = throwEventualErrors(dataEvent, dataListeners.get(i));

            if (newResult == null) {
                continue;
            }

            if (result == null) {
                result = newResult;
                continue;
            }

            if (result != newResult) {
                throw new XyzStateException(String.format(
                        "Data listeners return both: %s and %s for the state machine to change state.",
                        newResult,
                        result));
            }
        }

        return result;
    }

    private XyzState throwEventualErrors(XyzDataEvent<T> data, Function<XyzDataEvent<T>, XyzState> callback) {
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
