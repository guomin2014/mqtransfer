package com.gm.mqtransfer.provider.facade.model;

public class TopicPartitionMapping {

	/** 源分区 */
	private TopicPartitionInfo fromPartition;
	/** 目标分区 */
	private TopicPartitionInfo toPartition;
	
	public TopicPartitionMapping() {}
	
	public TopicPartitionMapping(TopicPartitionInfo fromPartition, TopicPartitionInfo toPartition) {
		this.fromPartition = fromPartition;
		this.toPartition = toPartition;
	}
	
	public TopicPartitionInfo getFromPartition() {
		return fromPartition;
	}
	public void setFromPartition(TopicPartitionInfo fromPartition) {
		this.fromPartition = fromPartition;
	}
	public TopicPartitionInfo getToPartition() {
		return toPartition;
	}
	public void setToPartition(TopicPartitionInfo toPartition) {
		this.toPartition = toPartition;
	}

	@Override
	public String toString() {
		return fromPartition + "==>" + toPartition;
	}
	
}
