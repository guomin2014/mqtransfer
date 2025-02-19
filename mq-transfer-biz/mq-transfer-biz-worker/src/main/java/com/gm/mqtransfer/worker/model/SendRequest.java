package com.gm.mqtransfer.worker.model;

import java.util.ArrayList;
import java.util.List;

import com.gm.mqtransfer.facade.model.CustomMessage;

/**
 * 生产消息发送请求
 * @author GuoMin
 * @date 2023-05-10 15:07:11
 *
 */
public class SendRequest {
	/** 任务分区信息 */
	private TaskSharding task;
	/** 缓存消息批次 */
	private PollCacheResponse cacheMessageBatch;
	/** 待发送消息队列(原始消息) */
	private List<CustomMessage> messages = new ArrayList<>();
	/** 发送超时时间，单位：毫秒 */
	private long maxSendTimeoutMs;
	/** 请求时间 */
	private long requestTime = System.currentTimeMillis();
	
	public SendRequest(PollCacheResponse cacheMessageBatch, long maxSendTimeoutMs) {
		this.cacheMessageBatch = cacheMessageBatch;
		this.task = cacheMessageBatch.getTask();
		this.messages = cacheMessageBatch.getMessages();
		this.maxSendTimeoutMs = maxSendTimeoutMs;
	}
	
	public boolean hasMessage() {
		return messages != null && !messages.isEmpty();
	}
	
	/**
	 * 放入消息
	 * @param message
	 * @param convertMessage
	 */
	public void putMessage(CustomMessage message) {
		messages.add(message);
	}

	public TaskSharding getTask() {
		return task;
	}

	public void setTask(TaskSharding task) {
		this.task = task;
	}

	public List<CustomMessage> getMessages() {
		return messages;
	}

	public void setMessages(List<CustomMessage> messages) {
		this.messages = messages;
	}

	public long getMaxSendTimeoutMs() {
		return maxSendTimeoutMs;
	}

	public void setMaxSendTimeoutMs(long maxSendTimeoutMs) {
		this.maxSendTimeoutMs = maxSendTimeoutMs;
	}

	public PollCacheResponse getCacheMessageBatch() {
		return cacheMessageBatch;
	}

	public void setCacheMessageBatch(PollCacheResponse cacheMessageBatch) {
		this.cacheMessageBatch = cacheMessageBatch;
	}

	public long getRequestTime() {
		return requestTime;
	}

	public void setRequestTime(long requestTime) {
		this.requestTime = requestTime;
	}

}
