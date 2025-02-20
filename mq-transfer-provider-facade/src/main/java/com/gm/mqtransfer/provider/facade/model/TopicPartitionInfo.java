package com.gm.mqtransfer.provider.facade.model;

import java.util.Objects;

import com.gm.mqtransfer.provider.facade.util.PartitionUtils;

public class TopicPartitionInfo {

	private Integer partition;
	
	private String brokerName;
	
	private int hash;
	
	public TopicPartitionInfo(Integer partition) {
		this.partition = partition;
	}
	public TopicPartitionInfo(Integer partition, String brokerName) {
		this.partition = partition;
		this.brokerName = brokerName;
	}
	public Integer getPartition() {
		return partition;
	}
	public void setPartition(Integer partition) {
		this.partition = partition;
	}
	public String getBrokerName() {
		return brokerName;
	}
	public void setBrokerName(String brokerName) {
		this.brokerName = brokerName;
	}
	
	public String getPartitionKey() {
		return PartitionUtils.generatorPartitionKey(brokerName, partition);
	}
	
	@Override
	public int hashCode() {
		if (hash != 0)
            return hash;
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hashCode(this.getBrokerName());
        result = prime * result + Objects.hashCode(this.getPartition());
        this.hash = result;
        return result;
	}
	@Override
	public String toString() {
		return this.getPartitionKey();
	}
	
}
