package com.gm.mqtransfer.provider.facade.api.producer;

import java.util.List;

import com.gm.mqtransfer.provider.facade.api.Request;
import com.gm.mqtransfer.provider.facade.model.MQMessage;
import com.gm.mqtransfer.provider.facade.model.ProducerPartition;

public class SendMessageRequest<T extends MQMessage> extends Request {

	private ProducerPartition partition;
	
	private List<T> messageList;
	
	/** 发送消息超时时间，未设置：表示不超时 */
	private Long sendTimeoutMs;
	
	public SendMessageRequest(ProducerPartition partition, List<T> messageList) {
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

	public Long getSendTimeoutMs() {
		return sendTimeoutMs;
	}

	public void setSendTimeoutMs(Long sendTimeoutMs) {
		this.sendTimeoutMs = sendTimeoutMs;
	}

}
