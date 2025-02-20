package com.gm.mqtransfer.provider.facade.api;

public class SubscribeData {

	/** 源分区开始位置点 */
	private Long startOffset;
	/** 源分区结束位置点 */
	private Long endOffset;
	/** 消费位置点 */
	private Long consumerOffset;

	public Long getStartOffset() {
		return startOffset;
	}

	public void setStartOffset(Long startOffset) {
		this.startOffset = startOffset;
	}

	public Long getEndOffset() {
		return endOffset;
	}

	public void setEndOffset(Long endOffset) {
		this.endOffset = endOffset;
	}

	public Long getConsumerOffset() {
		return consumerOffset;
	}

	public void setConsumerOffset(Long consumerOffset) {
		this.consumerOffset = consumerOffset;
	}

	@Override
	public String toString() {
		return "SubscribeData [startOffset=" + startOffset + ", endOffset=" + endOffset + ", consumerOffset=" + consumerOffset + "]";
	}
	
}
