package com.gm.mqtransfer.worker.model;

import java.util.ArrayList;
import java.util.List;

import com.gm.mqtransfer.facade.model.CustomMessage;
import com.gm.mqtransfer.facade.model.TaskPartition;

/**
 * 放入缓存请求对象
 * @author GuoMin
 * @date 2023-05-12 15:32:30
 *
 */
public class PutCacheRequest {
	/** 分区任务 */
	private TaskPartition task;
	/** 拉取批次 */
	private String batchCode;
	/** 本次拉取消息占用内存大小 */
	private long pollMessageTotalMemorySize = 0;
	/** 本次拉取消息条数 */
	private int pollMessageCount = 0;
	/** 本次过滤消息占用内存大小 */
	private long filterMessageTotalMemorySize = 0;
	/** 本次过滤消息条数 */
	private int filterMessageCount = 0;
	/** 本次拉取消息的最大位置点 */
	private Long pollMessageMinOffset;
	/** 本次拉取消息的最大位置点 */
	private Long pollMessageMaxOffset;
	/** 待发送消息队列(原始消息) */
	private List<CustomMessage> messageList = new ArrayList<>();
	
	public PutCacheRequest(TaskPartition task, String batchCode) {
		this.task = task;
		this.batchCode = batchCode;
	}
	public TaskPartition getTask() {
		return task;
	}
	public void setTask(TaskPartition task) {
		this.task = task;
	}
	public long getPollMessageTotalMemorySize() {
		return pollMessageTotalMemorySize;
	}
	public String getBatchCode() {
		return batchCode;
	}
	public void setBatchCode(String batchCode) {
		this.batchCode = batchCode;
	}
	public Long getPollMessageMinOffset() {
		return pollMessageMinOffset;
	}
	public void setPollMessageMinOffset(Long pollMessageMinOffset) {
		this.pollMessageMinOffset = pollMessageMinOffset;
	}
	public Long getPollMessageMaxOffset() {
		return pollMessageMaxOffset;
	}
	public void setPollMessageMaxOffset(Long pollMessageMaxOffset) {
		this.pollMessageMaxOffset = pollMessageMaxOffset;
	}
	public List<CustomMessage> getMessageList() {
		return messageList;
	}
	public void setMessageList(List<CustomMessage> messageList) {
		this.messageList = messageList;
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
	public void incrementPollMessageTotalMemorySizeAndCount(long size, int count) {
		this.pollMessageTotalMemorySize += size;
		this.pollMessageCount += count;
	}
	public void incrementFilterMessageTotalMemorySizeAndCount(long size, int count) {
		this.filterMessageTotalMemorySize += size;
		this.filterMessageCount += count;
	}
	/**
	 * 添加消息
	 * @param message
	 * @param transferEnable	可转发
	 */
	public void addMessage(CustomMessage message, boolean transferEnable) {
		if (this.pollMessageMinOffset == null) {
			this.pollMessageMinOffset = message.getOffset();
		}
		this.pollMessageMaxOffset = message.getOffset();
		this.incrementPollMessageTotalMemorySizeAndCount(message.getTotalUsedMemorySize(), 1);
		if (transferEnable) {
			this.messageList.add(message);
		} else {
			this.incrementFilterMessageTotalMemorySizeAndCount(message.getTotalUsedMemorySize(), 1);
		}
	}
}
