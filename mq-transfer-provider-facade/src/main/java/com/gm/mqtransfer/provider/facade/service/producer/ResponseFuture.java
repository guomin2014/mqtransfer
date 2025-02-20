package com.gm.mqtransfer.provider.facade.service.producer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.gm.mqtransfer.provider.facade.api.ResponseCode;
import com.gm.mqtransfer.provider.facade.api.producer.SendMessageResult;
import com.gm.mqtransfer.provider.facade.model.MQMessage;
import com.gm.mqtransfer.provider.facade.model.ProducerPartition;

public class ResponseFuture<T extends MQMessage> {
	/** 消息数量 */
	private final ProducerPartition partition;
	private final List<T> messages;
	/** 超时时间 */
	private final long timeoutMillis;
	private final SendCallback<T> sendCallback;
	
	private final long beginTimestamp = System.currentTimeMillis();
	private final CountDownLatch countDownLatch;
	private volatile SendMessageResult<T> response;
	
	public ResponseFuture(ProducerPartition partition, List<T> messages, long timeoutMillis, SendCallback<T> sendCallback) {
		this.partition = partition;
		this.messages = messages;
		this.timeoutMillis = timeoutMillis;
		this.sendCallback = sendCallback;
		this.response = new SendMessageResult<>(messages.size(), beginTimestamp);
		this.countDownLatch = new CountDownLatch(messages.size());
	}
	
	public boolean isTimeout() {
		long diff = System.currentTimeMillis() - this.beginTimestamp;
		return diff > this.timeoutMillis;
	}
	
	public void putResponse(T message, ResultMetadata result) {
		this.response.addResult(message, result);
		this.countDownLatch.countDown();
	}
	
	public void putResponse(SendMessageResult<T> results) {
		if (results != null) {
			Map<T, ResultMetadata> resultMap = results.getResultMap();
			if (resultMap != null) {
				for (Map.Entry<T, ResultMetadata> entry : resultMap.entrySet()) {
					this.putResponse(entry.getKey(), entry.getValue());
				}
			}
		}
	}

	public SendMessageResult<T> getResponse() {
		return response;
	}
	
	public SendMessageResult<T> waitResponse() throws InterruptedException {
		this.countDownLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
		return response;
	}
	
	public boolean isExistsError() {
		return this.response.isExistsError();
	}
	
	public boolean isCompletion() {
		return this.response.isCompletion() || this.isTimeout();
	}
	
	public void onCompletion() {
		//可能是超时完成，所以将无响应的都变成超时
		if (messages != null) {
			for (T message : messages) {
				if (!response.isExistsResponse(message)) {
					this.putResponse(message, new ResultMetadata(ResponseCode.SendMessageTimeoutError.name(), ResponseCode.SendMessageTimeoutError.getMsg()));
				}
			}
		}
		if (sendCallback != null) {
			sendCallback.onCompletion(partition, response);
		}
	}

}
