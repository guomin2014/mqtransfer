package com.gm.mqtransfer.provider.facade.api;

import com.gm.mqtransfer.provider.facade.model.ConsumerPartition;

public class CommitOffsetRequest extends Request {

	private ConsumerPartition partition;
	
	private Long offset;
	
	public CommitOffsetRequest(ConsumerPartition partition, Long offset) {
		this.partition = partition;
		this.offset = offset;
	}

	public ConsumerPartition getPartition() {
		return partition;
	}

	public void setPartition(ConsumerPartition partition) {
		this.partition = partition;
	}

	public Long getOffset() {
		return offset;
	}

	public void setOffset(Long offset) {
		this.offset = offset;
	}
	
	
}
