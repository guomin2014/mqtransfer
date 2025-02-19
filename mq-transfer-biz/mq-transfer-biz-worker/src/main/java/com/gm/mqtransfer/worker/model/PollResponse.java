package com.gm.mqtransfer.worker.model;

import java.util.ArrayList;
import java.util.List;

import com.gm.mqtransfer.facade.model.CustomMessage;

/**
 * 消费拉取消息响应
 * @author GuoMin
 * @date 2023-05-12 15:32:30
 *
 */
public class PollResponse {
	/** 分区任务 */
	private TaskSharding task;
	/** 拉取批次 */
	private String batchCode;
	/** 本次拉取消息占用内存大小 */
	private long pollMessageSize = 0;
	/** 本次拉取消息条数 */
	private int pollMessageCount = 0;
	/** 本次拉取消息的最大位置点 */
	private Long pollMessageMinOffset;
	/** 本次拉取消息的最大位置点 */
	private Long pollMessageMaxOffset;
	/** 待发送消息队列(原始消息) */
	private List<CustomMessage> messageList = new ArrayList<>();
	
	public PollResponse(TaskSharding task, String batchCode) {
		this.task = task;
		this.batchCode = batchCode;
	}
	public TaskSharding getTask() {
		return task;
	}
	public void setTask(TaskSharding task) {
		this.task = task;
	}
	public long getPollMessageSize() {
		return pollMessageSize;
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
	public void setPollMessageSize(long pollMessageSize) {
		this.pollMessageSize = pollMessageSize;
	}
	public int getPollMessageCount() {
		return pollMessageCount;
	}
	public void setPollMessageCount(int pollMessageCount) {
		this.pollMessageCount = pollMessageCount;
	}
	public void incrementPollMessageSizeAndCount(long size, int count) {
		this.pollMessageSize += size;
		this.pollMessageCount += count;
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
		this.incrementPollMessageSizeAndCount(message.getTotalUsedMemorySize(), 1);
		if (transferEnable) {
			this.messageList.add(message);
		}
	}
}
