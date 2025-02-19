package com.gm.mqtransfer.provider.facade.service.delay;

import com.gm.mqtransfer.provider.facade.model.MQMessage;
import com.gm.mqtransfer.provider.facade.service.producer.ResponseFuture;

public class DefaultDelayedOperationCallback<T extends MQMessage> implements DelayedOperationCallback {

	private final ResponseFuture<T> responseFuture;
	
	public DefaultDelayedOperationCallback(ResponseFuture<T> responseFuture) {
		this.responseFuture = responseFuture;
	}
	
	@Override
	public boolean checkComplete() {
		return responseFuture.isCompletion();
	}

	@Override
	public void onCompletion() {
		responseFuture.onCompletion();
	}
	
}
