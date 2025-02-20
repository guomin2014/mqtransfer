package com.gm.mqtransfer.provider.facade.service.producer;

/**
 * 生产消息发送结果
 * @author GM
 *
 */
public class SendResult {

	/** 本次需要重试消息占用内存大小 */
	private long retryMessageTotalMemorySize = 0;
	/** 本次需要重试消息条数 */
	private int retryMessageCount = 0;
	/** 本次发送失败忽略消息占用内存大小 */
	private long ignoreMessageTotalMemorySize = 0;
	/** 本次发送失败忽略消息条数 */
	private int ignoreMessageCount = 0;
	/** 本次发送消息占用内存大小 */
	private long sendMessageTotalMemorySize = 0;
	/** 本次发送消息条数 */
	private int sendMessageCount = 0;
	
	public void incrementIgnoreMessageTotalMemorySizeAndCount(long size, int count) {
		this.ignoreMessageTotalMemorySize += size;
		this.ignoreMessageCount += count;
	}
	public long getRetryMessageTotalMemorySize() {
		return retryMessageTotalMemorySize;
	}
	public void setRetryMessageTotalMemorySize(long retryMessageTotalMemorySize) {
		this.retryMessageTotalMemorySize = retryMessageTotalMemorySize;
	}
	public int getRetryMessageCount() {
		return retryMessageCount;
	}
	public void setRetryMessageCount(int retryMessageCount) {
		this.retryMessageCount = retryMessageCount;
	}
	public long getIgnoreMessageTotalMemorySize() {
		return ignoreMessageTotalMemorySize;
	}
	public void setIgnoreMessageTotalMemorySize(long ignoreMessageTotalMemorySize) {
		this.ignoreMessageTotalMemorySize = ignoreMessageTotalMemorySize;
	}
	public int getIgnoreMessageCount() {
		return ignoreMessageCount;
	}
	public void setIgnoreMessageCount(int ignoreMessageCount) {
		this.ignoreMessageCount = ignoreMessageCount;
	}
	public long getSendMessageTotalMemorySize() {
		return sendMessageTotalMemorySize;
	}
	public void setSendMessageTotalMemorySize(long sendMessageTotalMemorySize) {
		this.sendMessageTotalMemorySize = sendMessageTotalMemorySize;
	}
	public int getSendMessageCount() {
		return sendMessageCount;
	}
	public void setSendMessageCount(int sendMessageCount) {
		this.sendMessageCount = sendMessageCount;
	}
	@Override
	public String toString() {
		return "SendResult [retryMessageTotalMemorySize=" + retryMessageTotalMemorySize + ", retryMessageCount="
				+ retryMessageCount + ", ignoreMessageTotalMemorySize=" + ignoreMessageTotalMemorySize
				+ ", ignoreMessageCount=" + ignoreMessageCount + ", sendMessageTotalMemorySize="
				+ sendMessageTotalMemorySize + ", sendMessageCount=" + sendMessageCount + "]";
	}

}
