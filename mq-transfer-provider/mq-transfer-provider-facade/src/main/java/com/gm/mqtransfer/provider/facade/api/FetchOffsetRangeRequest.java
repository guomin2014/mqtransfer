package com.gm.mqtransfer.provider.facade.api;

import com.gm.mqtransfer.provider.facade.model.ConsumerPartition;

public class FetchOffsetRangeRequest extends Request {

	private ConsumerPartition partition;
	
	public FetchOffsetRangeRequest(ConsumerPartition partition) {
		this.partition = partition;
	}

	public ConsumerPartition getPartition() {
		return partition;
	}

	public void setPartition(ConsumerPartition partition) {
		this.partition = partition;
	}
	
}
