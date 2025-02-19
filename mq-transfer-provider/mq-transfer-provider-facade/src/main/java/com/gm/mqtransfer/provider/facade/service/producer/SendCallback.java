package com.gm.mqtransfer.provider.facade.service.producer;

import com.gm.mqtransfer.provider.facade.api.producer.SendMessageResult;
import com.gm.mqtransfer.provider.facade.model.MQMessage;
import com.gm.mqtransfer.provider.facade.model.ProducerPartition;

public interface SendCallback<T extends MQMessage> {

	/**
	 * 发送完成回调
	 * @param sendResult
	 */
	public void onCompletion(ProducerPartition partition, SendMessageResult<T> sendResult);
	
}
