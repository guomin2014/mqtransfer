package com.gm.mqtransfer.provider.facade.api.producer;

import com.gm.mqtransfer.provider.facade.api.Request;
import com.gm.mqtransfer.provider.facade.model.ProducerPartition;

public class SubscribeRequest extends Request {

	private ProducerPartition partition;
	
	public SubscribeRequest(ProducerPartition partition) {
		this.partition = partition;
	}

	public ProducerPartition getPartition() {
		return partition;
	}

	public void setPartition(ProducerPartition partition) {
		this.partition = partition;
	}
	
}
