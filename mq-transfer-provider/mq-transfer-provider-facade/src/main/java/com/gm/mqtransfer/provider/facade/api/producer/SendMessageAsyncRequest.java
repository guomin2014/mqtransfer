package com.gm.mqtransfer.provider.facade.api.producer;

import java.util.List;

import com.gm.mqtransfer.provider.facade.api.Request;
import com.gm.mqtransfer.provider.facade.model.MQMessage;
import com.gm.mqtransfer.provider.facade.model.ProducerPartition;
import com.gm.mqtransfer.provider.facade.service.producer.SendCallback;

public class SendMessageAsyncRequest<T extends MQMessage> extends Request {

	private ProducerPartition partition;
	
	private List<T> messages;
	
	private SendCallback<T> callback;
	
	/** 发送消息超时时间，空：表示不超时 */
	private Long sendTimeoutMs;
	
	public SendMessageAsyncRequest(ProducerPartition partition, List<T> messages, Long sendTimeoutMs, SendCallback<T> callback) {
		this.partition = partition;
		this.messages = messages;
		this.sendTimeoutMs = sendTimeoutMs;
		this.callback = callback;
	}

	public ProducerPartition getPartition() {
		return partition;
	}

	public void setPartition(ProducerPartition partition) {
		this.partition = partition;
	}

	public List<T> getMessages() {
		return messages;
	}

	public void setMessages(List<T> messages) {
		this.messages = messages;
	}

	public SendCallback<T> getCallback() {
		return callback;
	}

	public void setCallback(SendCallback<T> callback) {
		this.callback = callback;
	}

	public Long getSendTimeoutMs() {
		return sendTimeoutMs;
	}

	public void setSendTimeoutMs(Long sendTimeoutMs) {
		this.sendTimeoutMs = sendTimeoutMs;
	}

}
