package com.gm.mqtransfer.provider.facade.api.producer;

import com.gm.mqtransfer.provider.facade.api.Request;
import com.gm.mqtransfer.provider.facade.model.MQMessage;
import com.gm.mqtransfer.provider.facade.model.ProducerPartition;

public class CheckMessageRequest<T extends MQMessage> extends Request {

	private ProducerPartition partition;
	
	private T message;
	
	public CheckMessageRequest(ProducerPartition partition, T message) {
		this.partition = partition;
		this.message = message;
	}

	public ProducerPartition getPartition() {
		return partition;
	}

	public void setPartition(ProducerPartition partition) {
		this.partition = partition;
	}

	public T getMessage() {
		return message;
	}

	public void setMessage(T message) {
		this.message = message;
	}
	
}
