package com.gm.mqtransfer.manager.support.helix;

import java.io.Serializable;

import com.gm.mqtransfer.provider.facade.util.PartitionUtils;

public class HelixPartition implements Comparable<HelixPartition>, Serializable {
	
	private static final long serialVersionUID = -7492760555961473659L;
	
	/** 任务编号 */
	private String taskCode;
	/**
	 * kafka: 无 
	 * rocket:主机名 
	 */
	public String brokerName;
	/**
	 * kafka：partition
	 * rocket：queueId
	 */
	private Integer partition;
	
	public HelixPartition(String taskCode, Integer partition) {
		this.taskCode = taskCode;
		this.partition = partition;
	}
	public HelixPartition(String taskCode, String brokerName, Integer partition) {
		this.taskCode = taskCode;
		this.brokerName = brokerName;
		this.partition = partition;
	}
	
	public String getTaskCode() {
		return taskCode;
	}
	public void setTaskCode(String taskCode) {
		this.taskCode = taskCode;
	}
	public String getBrokerName() {
		return brokerName;
	}
	public void setBrokerName(String brokerName) {
		this.brokerName = brokerName;
	}
	public Integer getPartition() {
		return partition;
	}

	public void setPartition(Integer partition) {
		this.partition = partition;
	}
	public String generatorPartitionKey() {
		return PartitionUtils.generatorPartitionKey(this.brokerName, this.partition);
	}
	public String toString() {
		return String.format("{taskCode: %s, partition: %s}", this.taskCode, this.partition + (this.brokerName != null ? "#" + this.brokerName : ""));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof HelixPartition)) {
			return false;
		}
		HelixPartition other = (HelixPartition)obj;
		String rs = this.taskCode + "##" + this.brokerName + "##" + this.partition;
		String otherRS = other.taskCode + "##" + other.brokerName + "##"  + other.partition;
		return rs.equals(otherRS);
	}
	@Override
	public int hashCode() {
		String taskCode = this.taskCode;
		String brokerName = this.brokerName;
		final int prime = 31;
		int result = 1;
		result = prime * result + partition;
		result = prime * result + ((brokerName == null) ? 0 : brokerName.hashCode());
		result = prime * result + ((taskCode == null) ? 0 : taskCode.hashCode());
		return result;
	}

	@Override
	public int compareTo(HelixPartition other) {
		String rs = this.taskCode + "##" + this.brokerName + "##" + this.partition;
		String otherRS = other.taskCode + "##" + other.brokerName + "##"  + other.partition;
		int result = rs.compareTo(otherRS);
		if (result != 0) {
			return result;
		}
		return this.getPartition() - other.getPartition();
	}
}
