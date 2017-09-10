package com.ciplogic.statemachine.impl;

/**
 * Data event to bo processed by the state machine.
 * @param <T>
 */
public class XyzDataEvent<T> {
	private boolean consumed;
	private T data;

	public XyzDataEvent(T data) {
		this.data = data;
	}

	/**
	 * Consuming a data event makes it unavailable for the other listeners that were
	 * registered after this listener.
	 */
	public void consume() {
		this.consumed = true;
	}

	public boolean isConsumed() {
		return consumed;
	}

	public T getData() {
		return data;
	}
}
