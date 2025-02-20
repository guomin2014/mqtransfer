package com.gm.mqtransfer.provider.facade.service.delay;

public interface DelayedOperationCallback {

	/** 
	 * 检查是否完成
	 * @return
	 */
	public boolean checkComplete();
	/**
	 * 发送完成回调
	 * @param sendResult
	 */
	public void onCompletion();
	
}
