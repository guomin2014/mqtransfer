package com.gm.mqtransfer.provider.facade.api;

public class FetchOffsetRangeData {

	/** 源分区开始位置点 */
	private Long startOffset;
	/** 源分区结束位置点 */
	private Long endOffset;

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

	@Override
	public String toString() {
		return "FetchOffsetRangeData [startOffset=" + startOffset + ", endOffset=" + endOffset + "]";
	}
	
}
