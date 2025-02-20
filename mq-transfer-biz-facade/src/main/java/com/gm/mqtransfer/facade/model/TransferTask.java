package com.gm.mqtransfer.facade.model;

import java.util.List;

import com.gm.mqtransfer.provider.facade.model.TopicPartitionInfo;
import com.gm.mqtransfer.provider.facade.model.TopicPartitionMapping;

public class TransferTask {
	private String code;
	private String fromCluster;
	private String fromTopic;
	private String fromConfig;
	private String toCluster;
	private String toTopic;
	private String toConfig;
	
	//*********非配置项
	private TransferCluster fromClusterInfo;
	private TransferCluster toClusterInfo;
	/** 源消费配置 */
	private ConsumerConfig consumerConfig;
	/** 目标生产配置 */
	private ProducerConfig producerConfig;
	/** 转发配置 */
	private TransferConfig transferConfig;
	/** 告警配置 */
	private AlarmConfig alarmConfig;
	/** 源分区 */
	List<TopicPartitionInfo> fromPartitionList;
	/** 目标分区 */
	List<TopicPartitionInfo> toPartitionList;
	/** 分区映射关系 */
	List<TopicPartitionMapping> partitionMappingList;
	
	/** 源分区最后告警时间 */
	private Long lagAlarmLastTime;
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getFromCluster() {
		return fromCluster;
	}
	public void setFromCluster(String fromCluster) {
		this.fromCluster = fromCluster;
	}
	public String getFromTopic() {
		return fromTopic;
	}
	public void setFromTopic(String fromTopic) {
		this.fromTopic = fromTopic;
	}
	public String getToCluster() {
		return toCluster;
	}
	public void setToCluster(String toCluster) {
		this.toCluster = toCluster;
	}
	public String getToTopic() {
		return toTopic;
	}
	public void setToTopic(String toTopic) {
		this.toTopic = toTopic;
	}
	public TransferCluster getFromClusterInfo() {
		return fromClusterInfo;
	}
	public void setFromClusterInfo(TransferCluster fromClusterInfo) {
		this.fromClusterInfo = fromClusterInfo;
	}
	public TransferCluster getToClusterInfo() {
		return toClusterInfo;
	}
	public void setToClusterInfo(TransferCluster toClusterInfo) {
		this.toClusterInfo = toClusterInfo;
	}
	public String getFromConfig() {
		return fromConfig;
	}
	public void setFromConfig(String fromConfig) {
		this.fromConfig = fromConfig;
	}
	public String getToConfig() {
		return toConfig;
	}
	public void setToConfig(String toConfig) {
		this.toConfig = toConfig;
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
	public TransferConfig getTransferConfig() {
		return transferConfig;
	}
	public void setTransferConfig(TransferConfig transferConfig) {
		this.transferConfig = transferConfig;
	}
	public AlarmConfig getAlarmConfig() {
		return alarmConfig;
	}
	public void setAlarmConfig(AlarmConfig alarmConfig) {
		this.alarmConfig = alarmConfig;
	}
	public Long getLagAlarmLastTime() {
		return lagAlarmLastTime;
	}
	public void setLagAlarmLastTime(Long lagAlarmLastTime) {
		this.lagAlarmLastTime = lagAlarmLastTime;
	}
	
	public List<TopicPartitionInfo> getFromPartitionList() {
		return fromPartitionList;
	}
	public void setFromPartitionList(List<TopicPartitionInfo> fromPartitionList) {
		this.fromPartitionList = fromPartitionList;
	}
	public List<TopicPartitionInfo> getToPartitionList() {
		return toPartitionList;
	}
	public void setToPartitionList(List<TopicPartitionInfo> toPartitionList) {
		this.toPartitionList = toPartitionList;
	}
	public List<TopicPartitionMapping> getPartitionMappingList() {
		return partitionMappingList;
	}
	public void setPartitionMappingList(List<TopicPartitionMapping> partitionMappingList) {
		this.partitionMappingList = partitionMappingList;
	}
	@Override
	public String toString() {
		return "TransferTask [code=" + code + ", fromCluster=" + fromCluster + ", fromTopic=" + fromTopic
				+ ", toCluster=" + toCluster + ", toTopic=" + toTopic + "]";
	}
	
	public String toFromString() {
		StringBuilder build = new StringBuilder();
		build.append("code").append(":").append(this.getCode()).append(",");
		build.append("fromCluster").append(":").append(this.getFromCluster()).append(",");
		build.append("fromTopic").append(":").append(this.getFromTopic());
		return build.toString();
	}
	
	public String toToString() {
		StringBuilder build = new StringBuilder();
		build.append("code").append(":").append(this.getCode()).append(",");
		build.append("toCluster").append(":").append(this.getToCluster()).append(",");
		build.append("toTopic").append(":").append(this.getToTopic()).append(",");
		return build.toString();
	}
	
}
