package com.gm.mqtransfer.worker.model;

import java.util.concurrent.atomic.AtomicInteger;

import com.gm.mqtransfer.facade.filter.ConvertChain;
import com.gm.mqtransfer.facade.filter.ConvertFactory;
import com.gm.mqtransfer.facade.filter.FilterChain;
import com.gm.mqtransfer.facade.filter.FilterFactory;
import com.gm.mqtransfer.facade.model.CustomMessage;
import com.gm.mqtransfer.facade.model.TaskShardingConfig;
import com.gm.mqtransfer.facade.model.TransferPartitionConfig;
import com.gm.mqtransfer.facade.model.TransferTaskPartition;
import com.gm.mqtransfer.provider.facade.model.ConsumerClientConfig;
import com.gm.mqtransfer.provider.facade.model.ConsumerPartition;
import com.gm.mqtransfer.provider.facade.model.ProducerPartition;
import com.gm.mqtransfer.provider.facade.service.consumer.ConsumerService;
import com.gm.mqtransfer.provider.facade.service.producer.ProducerService;
import com.gm.mqtransfer.worker.service.task.thread.ConsumerThread;
import com.gm.mqtransfer.worker.service.task.thread.ProducerThread;

public class TaskSharding {

	private TaskShardingConfig taskShardingConfig;
	private FilterChain filterChain;
	private ConvertChain convertChain;
	
	private ConsumerPartition consumerPartition;
	private ProducerPartition producerPartition;
	/** 消费客户端配置 */
	private ConsumerClientConfig consumerClientConfig;
	
	/** 源分区最小消费位置点 */
	private Long minOffset;
	/** 源分区最大消费位置点 */
	private Long maxOffset;
	/** 源分区开始消费位置点（如果为空：表示还未初始化位置点） */
	private Long fetchOffset;
	/** 源分区已提交消费位置点 */
	private Long commitOffset;
	/** 源分区最后消费时间 */
	private Long lastConsumerTime;
	/** 目标分区最后生产时间 */
	private Long lastProducerTime;
	/** 待提交位置点 */
	private volatile Long waitCommitOffset;
	/** 源分区最后提交消费位置点时间 */
	private Long commitOffsetLastTime;
	/** 最后一次提交失败的位置点 */
	private Long lastCommitOffsetForFail;
	/** 源最大延迟告警 */
	private Long lagAlarmMaxLag;
	/** 延迟告警间隔时间 */
	private Long lagAlarmIntervalSecond;
	/** 源分区最后告警时间 */
	private Long lagAlarmLastTime;
	
	/** 订阅状态 */
	private volatile boolean initSubscribe = false;
	/** 获取位置点状态 */
	private volatile boolean initFetchOffset = false;
	/** 生产订阅状态 */
	private volatile boolean initSubscribeForProducer = false;
	
	/** 源初始化失败次数 */
	private AtomicInteger initFailureCountForConsumer = new AtomicInteger(0);
	/** 目标初始化失败次数 */
	private AtomicInteger initFailureCountForProducer = new AtomicInteger(0);
	
	private ConsumerService consumer;
	private ProducerService<CustomMessage> producer;
	
	private ConsumerThread consumerThread;
	private ProducerThread producerThread;
	
	public TaskSharding(TaskShardingConfig taskShardingConfig) {
		this.taskShardingConfig = taskShardingConfig;
		if (taskShardingConfig.getConsumerConfig() != null) {
			this.filterChain = FilterFactory.createFilters(taskShardingConfig.getConsumerConfig().getFilterConfigs());
		}
		if (taskShardingConfig.getProducerConfig() != null) {
			this.convertChain = ConvertFactory.createConvert(taskShardingConfig.getProducerConfig().getConvertConfigs());
		}
		String expectStartOffset = null;
		Long expectStartOffsetValue = null;
		TransferPartitionConfig transferPartitionConfig = taskShardingConfig.getTransferPartitionConfig();
		if (transferPartitionConfig != null) {
			TransferTaskPartition taskPartition = transferPartitionConfig.getPartition();
			if (taskPartition != null) {
				expectStartOffset = taskPartition.getOffsetType();
				expectStartOffsetValue = taskPartition.getOffset();
			}
		}
		this.consumerPartition = new ConsumerPartition(taskShardingConfig.getTaskCode(), taskShardingConfig.getFromCluster().getCode(), taskShardingConfig.getFromTopic(), 
				taskShardingConfig.getFromPartition().getPartition(), taskShardingConfig.getFromPartition().getBrokerName(), 
				expectStartOffset, expectStartOffsetValue);
		this.producerPartition = new ProducerPartition(taskShardingConfig.getTaskCode(), taskShardingConfig.getToCluster().getCode(), taskShardingConfig.getToTopic(), 
				taskShardingConfig.getToPartition().getPartition(), taskShardingConfig.getToPartition().getBrokerName());
	}
	
	public TaskShardingConfig getTaskShardingConfig() {
		return taskShardingConfig;
	}
	public void setTaskShardingConfig(TaskShardingConfig taskShardingConfig) {
		this.taskShardingConfig = taskShardingConfig;
	}
	public FilterChain getFilterChain() {
		return filterChain;
	}
	public void setFilterChain(FilterChain filterChain) {
		this.filterChain = filterChain;
	}
	public ConvertChain getConvertChain() {
		return convertChain;
	}
	public void setConvertChain(ConvertChain convertChain) {
		this.convertChain = convertChain;
	}
	
	public ConsumerPartition getConsumerPartition() {
		return consumerPartition;
	}
	public void setConsumerPartition(ConsumerPartition consumerPartition) {
		this.consumerPartition = consumerPartition;
	}
	public ProducerPartition getProducerPartition() {
		return producerPartition;
	}
	public void setProducerPartition(ProducerPartition producerPartition) {
		this.producerPartition = producerPartition;
	}
	public Long getFetchOffset() {
		return fetchOffset;
	}
	public void setFetchOffset(Long fetchOffset) {
		this.fetchOffset = fetchOffset;
	}
	public Long getMaxOffset() {
		return maxOffset;
	}
	public void setMaxOffset(Long maxOffset) {
		this.maxOffset = maxOffset;
	}
	public Long getMinOffset() {
		return minOffset;
	}
	public void setMinOffset(Long minOffset) {
		this.minOffset = minOffset;
	}
	public Long getCommitOffset() {
		return commitOffset;
	}
	public void setCommitOffset(Long commitOffset) {
		this.commitOffset = commitOffset;
	}
	public Long getLastConsumerTime() {
		return lastConsumerTime;
	}
	public void setLastConsumerTime(Long lastConsumerTime) {
		this.lastConsumerTime = lastConsumerTime;
	}
	public boolean isInitSubscribe() {
		return initSubscribe;
	}
	public void setInitSubscribe(boolean initSubscribe) {
		this.initSubscribe = initSubscribe;
	}
	public boolean isInitFetchOffset() {
		return initFetchOffset;
	}
	public void setInitFetchOffset(boolean initFetchOffset) {
		this.initFetchOffset = initFetchOffset;
	}
	public boolean isInitSubscribeForProducer() {
		return initSubscribeForProducer;
	}
	public void setInitSubscribeForProducer(boolean initSubscribeForProducer) {
		this.initSubscribeForProducer = initSubscribeForProducer;
	}
	public Long getLastProducerTime() {
		return lastProducerTime;
	}
	public void setLastProducerTime(Long lastProducerTime) {
		this.lastProducerTime = lastProducerTime;
	}
	public Long getWaitCommitOffset() {
		return waitCommitOffset;
	}
	public void setWaitCommitOffset(Long waitCommitOffset) {
		this.waitCommitOffset = waitCommitOffset;
	}
	public Long getCommitOffsetLastTime() {
		return commitOffsetLastTime;
	}
	public void setCommitOffsetLastTime(Long commitOffsetLastTime) {
		this.commitOffsetLastTime = commitOffsetLastTime;
	}
	public Long getLastCommitOffsetForFail() {
		return lastCommitOffsetForFail;
	}
	public void setLastCommitOffsetForFail(Long lastCommitOffsetForFail) {
		this.lastCommitOffsetForFail = lastCommitOffsetForFail;
	}
	public Long getLagAlarmMaxLag() {
		return lagAlarmMaxLag;
	}
	public void setLagAlarmMaxLag(Long lagAlarmMaxLag) {
		this.lagAlarmMaxLag = lagAlarmMaxLag;
	}
	public Long getLagAlarmIntervalSecond() {
		return lagAlarmIntervalSecond;
	}
	public void setLagAlarmIntervalSecond(Long lagAlarmIntervalSecond) {
		this.lagAlarmIntervalSecond = lagAlarmIntervalSecond;
	}
	public Long getLagAlarmLastTime() {
		return lagAlarmLastTime;
	}
	public void setLagAlarmLastTime(Long lagAlarmLastTime) {
		this.lagAlarmLastTime = lagAlarmLastTime;
	}
	public ConsumerClientConfig getConsumerClientConfig() {
		return consumerClientConfig;
	}
	public void setConsumerClientConfig(ConsumerClientConfig consumerClientConfig) {
		this.consumerClientConfig = consumerClientConfig;
	}
	public ConsumerService getConsumer() {
		return consumer;
	}
	public void setConsumer(ConsumerService consumer) {
		this.consumer = consumer;
	}
	public ProducerService<CustomMessage> getProducer() {
		return producer;
	}
	public void setProducer(ProducerService<CustomMessage> producer) {
		this.producer = producer;
	}
	public ConsumerThread getConsumerThread() {
		return consumerThread;
	}
	public void setConsumerThread(ConsumerThread consumerThread) {
		this.consumerThread = consumerThread;
	}
	public ProducerThread getProducerThread() {
		return producerThread;
	}
	public void setProducerThread(ProducerThread producerThread) {
		this.producerThread = producerThread;
	}

	/**
	 * 消费端是否准备好
	 * @return
	 */
	public boolean isConsumerReady() {
		return this.initSubscribe && this.initFetchOffset;
	}
	/**
	 * 生产端是否准备好
	 * @return
	 */
	public boolean isProducerReady() {
		return this.initSubscribeForProducer;
	}
	public int incrementAndGetInitFailureCountForConsumer() {
		return this.initFailureCountForConsumer.incrementAndGet();
	}
	public void resetInitFailureCountForConsumer() {
		this.initFailureCountForConsumer.set(0);
	}
	public int incrementAndGetInitFailureCountForProducer() {
		return this.initFailureCountForProducer.incrementAndGet();
	}
	public void resetInitFailureCountForProducer() {
		this.initFailureCountForProducer.set(0);
	}
	public String toString() {
		StringBuilder build = new StringBuilder();
		build.append("taskCode").append(":").append(this.taskShardingConfig.getTaskCode()).append(",");
		build.append("fromTopic").append(":").append(this.taskShardingConfig.getFromTopic()).append(",");
		build.append("fromPartition").append(":").append(this.taskShardingConfig.getFromPartition().getPartitionKey()).append(",");
		build.append("toTopic").append(":").append(this.taskShardingConfig.getToTopic()).append(",");
		build.append("toPartition").append(":").append(this.taskShardingConfig.getToPartition().getPartitionKey());
		return build.toString();
	}
	public String toFromString() {
		StringBuilder build = new StringBuilder();
		build.append("taskCode").append(":").append(this.taskShardingConfig.getTaskCode()).append(",");
		build.append("fromTopic").append(":").append(this.taskShardingConfig.getFromTopic()).append(",");
		build.append("fromPartition").append(":").append(this.taskShardingConfig.getFromPartition().getPartitionKey());
		return build.toString();
	}
	public String toFromDescString() {
		StringBuilder build = new StringBuilder();
		build.append("taskCode").append(":").append(this.taskShardingConfig.getTaskCode()).append(",");
		build.append("cluster").append(":").append(this.taskShardingConfig.getFromCluster().getCode()).append(",");
		build.append("topic").append(":").append(this.taskShardingConfig.getFromTopic()).append(",");
		build.append("partition").append(":").append(this.taskShardingConfig.getFromPartition().getPartitionKey());
		return build.toString();
	}
	public String toToString() {
		StringBuilder build = new StringBuilder();
		build.append("taskCode").append(":").append(this.taskShardingConfig.getTaskCode()).append(",");
		build.append("toTopic").append(":").append(this.taskShardingConfig.getToTopic()).append(",");
		build.append("toPartition").append(":").append(this.taskShardingConfig.getToPartition().getPartitionKey());
		return build.toString();
	}
	public String toToDescString() {
		StringBuilder build = new StringBuilder();
		build.append("taskCode").append(":").append(this.taskShardingConfig.getTaskCode()).append(",");
		build.append("cluster").append(":").append(this.taskShardingConfig.getToCluster().getCode()).append(",");
		build.append("topic").append(":").append(this.taskShardingConfig.getToTopic()).append(",");
		build.append("partition").append(":").append(this.taskShardingConfig.getToPartition().getPartitionKey());
		return build.toString();
	}
}
