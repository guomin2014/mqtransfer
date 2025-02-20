package com.gm.mqtransfer.provider.facade.api;

public class FetchOffsetData {

	/** 源分区开始消费位置点 */
	private Long offset;

	public Long getOffset() {
		return offset;
	}

	public void setOffset(Long offset) {
		this.offset = offset;
	}

}
