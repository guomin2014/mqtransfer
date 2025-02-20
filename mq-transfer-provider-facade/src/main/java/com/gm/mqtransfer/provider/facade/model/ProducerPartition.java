package com.gm.mqtransfer.provider.facade.model;

import com.gm.mqtransfer.provider.facade.util.PartitionUtils;

public class ProducerPartition {

	/** 任务编号 */
	private String taskCode;
	/** 目标集群编号 */
	private String toClusterCode;
	/** 目标主题 */
	private String toTopic;
	/** 目标分区 */
	private Integer toPartition;
	/** 目标分区所属brokerName */
	private String toBrokerName;
	/** 目标分区描述（任务ID+topic+partition） */
	private String toDesc;
	
	public ProducerPartition(String taskCode, String toClusterCode, String toTopic, Integer toPartition,
			String toBrokerName) {
		this.taskCode = taskCode;
		this.toClusterCode = toClusterCode;
		this.toTopic = toTopic;
		this.toPartition = toPartition;
		this.toBrokerName = toBrokerName;
		this.toDesc = "code:" + taskCode + ",topic:" + toTopic + ",partition:" + getToPartitionKey();
	}
	public String getTaskCode() {
		return taskCode;
	}
	public void setTaskCode(String taskCode) {
		this.taskCode = taskCode;
	}
	public String getToClusterCode() {
		return toClusterCode;
	}
	public void setToClusterCode(String toClusterCode) {
		this.toClusterCode = toClusterCode;
	}
	public String getToTopic() {
		return toTopic;
	}
	public void setToTopic(String toTopic) {
		this.toTopic = toTopic;
	}
	public Integer getToPartition() {
		return toPartition;
	}
	public void setToPartition(Integer toPartition) {
		this.toPartition = toPartition;
	}
	public String getToBrokerName() {
		return toBrokerName;
	}
	public void setToBrokerName(String toBrokerName) {
		this.toBrokerName = toBrokerName;
	}
	public String getToDesc() {
		return toDesc;
	}
	public void setToDesc(String toDesc) {
		this.toDesc = toDesc;
	}
	
	public String getToPartitionKey() {
		return PartitionUtils.generatorPartitionKey(this.getToBrokerName(), this.getToPartition());
	}
}
