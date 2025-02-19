package com.gm.mqtransfer.facade.model;

public class TransferTaskConfig {
	/** 源消费配置 */
	private ConsumerConfig consumerConfig;
	/** 目标生产配置 */
	private ProducerConfig producerConfig;
	/** 转发配置 */
	private TransferConfig transferConfig;
	/** 告警配置 */
	private AlarmConfig alarmConfig;
	
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
}
