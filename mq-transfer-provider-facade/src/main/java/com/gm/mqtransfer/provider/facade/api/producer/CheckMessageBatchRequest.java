package com.gm.mqtransfer.provider.facade.api.producer;

import java.util.List;

import com.gm.mqtransfer.provider.facade.api.Request;
import com.gm.mqtransfer.provider.facade.model.MQMessage;
import com.gm.mqtransfer.provider.facade.model.ProducerPartition;

public class CheckMessageBatchRequest<T extends MQMessage> extends Request {

	private ProducerPartition partition;
	
	private List<T> messageList;
	
	public CheckMessageBatchRequest(ProducerPartition partition, List<T> messageList) {
		this.partition = partition;
		this.messageList = messageList;
	}

	public ProducerPartition getPartition() {
		return partition;
	}

	public void setPartition(ProducerPartition partition) {
		this.partition = partition;
	}

	public List<T> getMessageList() {
		return messageList;
	}

	public void setMessageList(List<T> messageList) {
		this.messageList = messageList;
	}

}
