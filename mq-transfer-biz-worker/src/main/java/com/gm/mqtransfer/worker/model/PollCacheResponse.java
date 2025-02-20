package com.gm.mqtransfer.worker.model;

import java.util.ArrayList;
import java.util.List;

import com.gm.mqtransfer.facade.model.CustomMessage;

/**
 * 从缓存拉取消息响应对象
 * @author GM
 * @date 2024-06-26
 *
 * @param <T>
 */
public class PollCacheResponse {
	/** 任务分区信息 */
	private TaskSharding task;
	/** 本次拉取消息占用内存大小 */
	private long pollMessageTotalMemorySize = 0;
	/** 本次拉取消息条数 */
	private int pollMessageCount = 0;
	/** 本次过滤消息占用内存大小 */
	private long filterMessageTotalMemorySize = 0;
	/** 本次过滤消息条数 */
	private int filterMessageCount = 0;
	/** 本次拉取消息的最大位置点 */
	private long pollMessageMaxOffset = -1;
	/** 待发送消息队列(原始消息) */
	private List<CustomMessage> messages = new ArrayList<>();
	/** 本次拉取消息的最大位置点临时值，用于存储更新前位点值 */
	private long pollMessageMaxOffsetTmp = 0;
	
	public PollCacheResponse(TaskSharding task) {
		this.task = task;
	}
	
	public boolean hasMessage() {
		return messages != null && !messages.isEmpty();
	}
	
	public void incrementPollMessageTotalMemorySizeAndCount(long size, int count) {
		this.pollMessageTotalMemorySize += size;
		this.pollMessageCount += count;
	}
	public void incrementFilterMessageTotalMemorySizeAndCount(long size, int count) {
		this.filterMessageTotalMemorySize += size;
		this.filterMessageCount += count;
	}
	/**
	 * 放入消息
	 * @param message
	 * @param convertMessage
	 */
	public void putMessage(CustomMessage message) {
		messages.add(message);
	}
	/**
	 * 回退指定消息
	 * @param message
	 * @param convertMessage
	 * @param messageMemorySize
	 */
	public void rollbackMessage(CustomMessage message, long messageMemorySize) {
		messages.remove(message);
		pollMessageCount--;
		pollMessageTotalMemorySize -= messageMemorySize;
		this.pollMessageMaxOffset = this.pollMessageMaxOffsetTmp;//将位点回退
	}

	public TaskSharding getTask() {
		return task;
	}

	public void setTask(TaskSharding task) {
		this.task = task;
	}

	public long getPollMessageTotalMemorySize() {
		return pollMessageTotalMemorySize;
	}

	public void setPollMessageTotalMemorySize(long pollMessageTotalMemorySize) {
		this.pollMessageTotalMemorySize = pollMessageTotalMemorySize;
	}

	public int getPollMessageCount() {
		return pollMessageCount;
	}

	public void setPollMessageCount(int pollMessageCount) {
		this.pollMessageCount = pollMessageCount;
	}

	public long getFilterMessageTotalMemorySize() {
		return filterMessageTotalMemorySize;
	}

	public void setFilterMessageTotalMemorySize(long filterMessageTotalMemorySize) {
		this.filterMessageTotalMemorySize = filterMessageTotalMemorySize;
	}

	public int getFilterMessageCount() {
		return filterMessageCount;
	}

	public void setFilterMessageCount(int filterMessageCount) {
		this.filterMessageCount = filterMessageCount;
	}

	public long getPollMessageMaxOffset() {
		return pollMessageMaxOffset;
	}

	public void setPollMessageMaxOffset(long pollMessageMaxOffset) {
		this.pollMessageMaxOffsetTmp = this.pollMessageMaxOffset;
		this.pollMessageMaxOffset = pollMessageMaxOffset;
	}

	public List<CustomMessage> getMessages() {
		return messages;
	}

	public void setMessages(List<CustomMessage> messages) {
		this.messages = messages;
	}

}
