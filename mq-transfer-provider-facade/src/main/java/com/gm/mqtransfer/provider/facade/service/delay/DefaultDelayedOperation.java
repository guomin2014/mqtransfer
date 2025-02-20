package com.gm.mqtransfer.provider.facade.service.delay;

import com.gm.mqtransfer.provider.facade.service.delay.DelayedOperation;
import com.gm.mqtransfer.provider.facade.service.delay.DelayedOperationCallback;

public class DefaultDelayedOperation extends DelayedOperation{

	private DelayedOperationCallback callback;
	
	public DefaultDelayedOperation(Long delayMs, DelayedOperationCallback callback) {
		super(delayMs, null);
		this.callback = callback;
	}
	/**
	 * 过期处理（该方法晚于onComplete调用）
	 */
	@Override
	public void onExpiration() {
//		logger.info("onExpiration------>" + result);
//		result.updateNoRspRecordToFail("send time out", false);
	}

	@Override
	public void onComplete() {
//		logger.info("onComplete------>" + result);
		if (this.callback != null) {
			this.callback.onCompletion();
		}
	}

	@Override
	public boolean tryComplete() {
//		logger.info("tryComplete------>" + result);
		if (callback != null && callback.checkComplete()) {
			return super.forceComplete();
		} else {
			return false;
		}
	}

}
