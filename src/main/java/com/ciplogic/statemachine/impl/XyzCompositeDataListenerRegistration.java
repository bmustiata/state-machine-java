package com.ciplogic.statemachine.impl;

import java.util.ArrayList;
import java.util.List;

public class XyzCompositeDataListenerRegistration<T> implements XyzDataListenerRegistration<T> {
	List<XyzDataListenerRegistration<T>> registrationList = new ArrayList<>();

	@Override
	public void detach() {
		registrationList.forEach(XyzDataListenerRegistration::detach);
	}

	void addListenerRegistration(XyzDataListenerRegistration<T> xyzDataListenerRegistration) {
		this.registrationList.add(xyzDataListenerRegistration);
	}
}
