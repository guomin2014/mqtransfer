package com.gm.mqtransfer.provider.facade.api;

import com.gm.mqtransfer.provider.facade.model.ConsumerPartition;

public class PollMessageRequest extends Request {

	/** Topic and partition */
	private ConsumerPartition partition;
	/** The maximum number of each pull message */
	private Integer maxNums;
	/** The maximum size of each pull message, in bytes */
	private Long maxSize;
	
	public PollMessageRequest(ConsumerPartition partition, Integer maxNums, Long maxSize) {
		this.partition = partition;
		this.maxNums = maxNums;
		this.maxSize = maxSize;
	}

	public ConsumerPartition getPartition() {
		return partition;
	}

	public void setPartition(ConsumerPartition partition) {
		this.partition = partition;
	}

	public Integer getMaxNums() {
		return maxNums;
	}

	public void setMaxNums(Integer maxNums) {
		this.maxNums = maxNums;
	}

	public Long getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(Long maxSize) {
		this.maxSize = maxSize;
	}

	
}
