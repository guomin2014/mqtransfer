package com.gm.mqtransfer.facade.model;

import com.gm.mqtransfer.provider.facade.model.TopicPartitionInfo;

public class TaskShardingConfig {
	/** 任务编码 */
	private String taskCode;
	/** 源集群 */
	private TransferCluster fromCluster;
	/** 源主题 */
	private String fromTopic;
	/** 源主题分区 */
	private TopicPartitionInfo fromPartition;
	/** 目标集群 */
	private TransferCluster toCluster;
	/** 目标主题 */
	private String toTopic;
	/** 目标分区 */
	private TopicPartitionInfo toPartition;
	/** 源消费配置 */
	private ConsumerConfig consumerConfig;
	/** 目标生产配置 */
	private ProducerConfig producerConfig;
	/** 转发配置 */
	private TransferPartitionConfig transferPartitionConfig;
	/** 告警配置 */
	private AlarmConfig alarmConfig;
	

	public String getTaskCode() {
		return taskCode;
	}

	public void setTaskCode(String taskCode) {
		this.taskCode = taskCode;
	}

	public TransferCluster getFromCluster() {
		return fromCluster;
	}

	public void setFromCluster(TransferCluster fromCluster) {
		this.fromCluster = fromCluster;
	}

	public String getFromTopic() {
		return fromTopic;
	}

	public void setFromTopic(String fromTopic) {
		this.fromTopic = fromTopic;
	}

	public TopicPartitionInfo getFromPartition() {
		return fromPartition;
	}

	public void setFromPartition(TopicPartitionInfo fromPartition) {
		this.fromPartition = fromPartition;
	}

	public TransferCluster getToCluster() {
		return toCluster;
	}

	public void setToCluster(TransferCluster toCluster) {
		this.toCluster = toCluster;
	}

	public String getToTopic() {
		return toTopic;
	}

	public void setToTopic(String toTopic) {
		this.toTopic = toTopic;
	}

	public TopicPartitionInfo getToPartition() {
		return toPartition;
	}

	public void setToPartition(TopicPartitionInfo toPartition) {
		this.toPartition = toPartition;
	}

	public ConsumerConfig getConsumerConfig() {
		return consumerConfig;
	}

	public void setConsumerConfig(ConsumerConfig consumerConfig) {
		this.consumerConfig = consumerConfig;
	}

	public ProducerConfig getProducerConfig() {
		return producerConfig;
	}

	public void setProducerConfig(ProducerConfig producerConfig) {
		this.producerConfig = producerConfig;
	}

	public TransferPartitionConfig getTransferPartitionConfig() {
		return transferPartitionConfig;
	}

	public void setTransferPartitionConfig(TransferPartitionConfig transferPartitionConfig) {
		this.transferPartitionConfig = transferPartitionConfig;
	}

	public AlarmConfig getAlarmConfig() {
		return alarmConfig;
	}

	public void setAlarmConfig(AlarmConfig alarmConfig) {
		this.alarmConfig = alarmConfig;
	}

	@Override
	public String toString() {
		return "TransferTaskSharding [taskCode=" + taskCode + ", fromCluster=" + fromCluster + ", fromTopic=" + fromTopic + ", fromPartition=" + fromPartition
				+ ", toCluster=" + toCluster + ", toTopic=" + toTopic + ", toPartition=" + toPartition + "]";
	}
	
	public String toFromString() {
		StringBuilder build = new StringBuilder();
		build.append("taskCode").append(":").append(this.getTaskCode()).append(",");
		build.append("fromCluster").append(":").append(this.getFromCluster()).append(",");
		build.append("fromTopic").append(":").append(this.getFromTopic());
		return build.toString();
	}
	
	public String toToString() {
		StringBuilder build = new StringBuilder();
		build.append("taskCode").append(":").append(this.getTaskCode()).append(",");
		build.append("toCluster").append(":").append(this.getToCluster()).append(",");
		build.append("toTopic").append(":").append(this.getToTopic()).append(",");
		return build.toString();
	}
}
