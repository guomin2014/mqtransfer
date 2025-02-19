package com.gm.mqtransfer.provider.facade.api;

import java.util.List;

import com.gm.mqtransfer.provider.facade.model.MQMessage;

public class PollMessageData {

	/** 消息列表 */
	private List<MQMessage> messages;
	/** 消息的最后位置点 */
	private Long endOffset;
	/** 当前高水位 */
	private Long highWatermark;
	
	public List<MQMessage> getMessages() {
		return messages;
	}
	public void setMessages(List<MQMessage> messages) {
		this.messages = messages;
	}
	public Long getEndOffset() {
		return endOffset;
	}
	public void setEndOffset(Long endOffset) {
		this.endOffset = endOffset;
	}
	public Long getHighWatermark() {
		return highWatermark;
	}
	public void setHighWatermark(Long highWatermark) {
		this.highWatermark = highWatermark;
	}
	@Override
	public String toString() {
		return "PollMessageData [messages=" + messages + ", endOffset=" + endOffset + ", highWatermark=" + highWatermark
				+ "]";
	}
}
