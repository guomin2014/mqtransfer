package com.gm.mqtransfer.facade.handler;

public class DefaultMethodCallback implements MethodCallback {

	@Override
	public boolean isEndMethodCall() {
		return false;
	}

	@Override
	public boolean isPrintLogEnable() {
		return false;
	}
	@Override
	public Object methodBefore(String methodName, Object... args) {
		return null;
	}

	@Override
	public Object methodAfter(String methodName, Object... args) {
		return null;
	}

}
