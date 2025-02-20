package com.gm.mqtransfer.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "transfer.common")
public class CommonConfiguration {

	/** 总消息队列内存占比 */
	private double messageQueueMemoryRatio = 0.6;
	/** 单任务消息队列内存占比，超过该比例，则暂停消费 */
	private double messageQueueMemoryRatioForSingle = 0.6;
	/** 消费者线程池核心线程系数 */
	private int messageConsumerThreadPoolRatio = 0;
	/** 生产者线程池核心线程系数 */
	private int messageProducerThreadPoolRatio = 0;
	/** 消费者配置 */
	private CommonConsumerConfiguration consumer = new CommonConsumerConfiguration();
	/** 生产者配置 */
	private CommonProducerConfiguration producer = new CommonProducerConfiguration();
	/** 转发日志配置 */
	private TransferLogConfiguration logger = new TransferLogConfiguration();
	
	public double getMessageQueueMemoryRatio() {
		return messageQueueMemoryRatio;
	}
	public void setMessageQueueMemoryRatio(double messageQueueMemoryRatio) {
		this.messageQueueMemoryRatio = messageQueueMemoryRatio;
	}
	public double getMessageQueueMemoryRatioForSingle() {
		return messageQueueMemoryRatioForSingle;
	}
	public void setMessageQueueMemoryRatioForSingle(double messageQueueMemoryRatioForSingle) {
		this.messageQueueMemoryRatioForSingle = messageQueueMemoryRatioForSingle;
	}
	public int getMessageConsumerThreadPoolRatio() {
		return messageConsumerThreadPoolRatio;
	}
	public void setMessageConsumerThreadPoolRatio(int messageConsumerThreadPoolRatio) {
		this.messageConsumerThreadPoolRatio = messageConsumerThreadPoolRatio;
	}
	public int getMessageProducerThreadPoolRatio() {
		return messageProducerThreadPoolRatio;
	}
	public void setMessageProducerThreadPoolRatio(int messageProducerThreadPoolRatio) {
		this.messageProducerThreadPoolRatio = messageProducerThreadPoolRatio;
	}
	public CommonConsumerConfiguration getConsumer() {
		return consumer;
	}
	public void setConsumer(CommonConsumerConfiguration consumer) {
		this.consumer = consumer;
	}
	public CommonProducerConfiguration getProducer() {
		return producer;
	}
	public void setProducer(CommonProducerConfiguration producer) {
		this.producer = producer;
	}
	public TransferLogConfiguration getLogger() {
		return logger;
	}
	public void setLogger(TransferLogConfiguration logger) {
		this.logger = logger;
	}
	
}
