package com.gm.mqtransfer.provider.facade.model;

import java.util.List;

public class TopicInfo {

	private String topic;
	
	private List<TopicPartitionInfo> partitions;
	
	public TopicInfo(String topic) {
		this.topic = topic;
	}
	public TopicInfo(String topic, List<TopicPartitionInfo> partitions) {
		this.partitions = partitions;
	}

	public String getTopic() {
		return topic;
	}
	public void setTopic(String topic) {
		this.topic = topic;
	}
	public List<TopicPartitionInfo> getPartitions() {
		return partitions;
	}
	public void setPartitions(List<TopicPartitionInfo> partitions) {
		this.partitions = partitions;
	}
	
}
