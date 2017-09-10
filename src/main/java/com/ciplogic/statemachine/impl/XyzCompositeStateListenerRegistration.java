package com.ciplogic.statemachine.impl;

import java.util.ArrayList;
import java.util.List;

public class XyzCompositeStateListenerRegistration<T> implements XyzStateListenerRegistration<T> {
	List<XyzStateListenerRegistration<T>> registrationList = new ArrayList<>();

	@Override
	public void detach() {
		registrationList.forEach(XyzStateListenerRegistration::detach);
	}

	void addListenerRegistration(XyzStateListenerRegistration<T> xyzDataListenerRegistration) {
		this.registrationList.add(xyzDataListenerRegistration);
	}
}
