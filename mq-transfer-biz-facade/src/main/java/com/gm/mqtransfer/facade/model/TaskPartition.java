package com.gm.mqtransfer.facade.model;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import com.gm.mqtransfer.facade.filter.ConvertChain;
import com.gm.mqtransfer.facade.filter.FilterChain;
import com.gm.mqtransfer.provider.facade.model.ConsumerPartition;
import com.gm.mqtransfer.provider.facade.model.ProducerPartition;
import com.gm.mqtransfer.provider.facade.util.PartitionUtils;

public class TaskPartition {
	/** 任务编号 */
	private String taskCode;
	/** 源集群编号 */
	private String fromClusterCode;
	/** 源主题 */
	private String fromTopic;
	/** 源分区 */
	private Integer fromPartition;
	/** 源分区所属brokerName */
	private String fromBrokerName;
	/** 源分区描述（任务ID+topic+partition） */
	private String fromDesc;
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
	/** 位置点提交模式，空：表示同步，async：异步，sync：同步 */
	private String commitOffsetMode;
	
	/** 源分区消费组 */
	private String consumerGroup;
	/** 源分区的排序编号 */
	private Integer fromPartitionIndex;
	/** 源分区开始消费位置点（如果为空：表示还未初始化位置点） */
	private Long fetchOffset;
	/** 源分区最后消费时间 */
	private Long lastConsumeTime;
	/** 源分区期望开始消费位置点（自定义开始位置点，当分组的消费位点不存在的时候，该字段值有效）,FIRST_OFFSET：开始位置,LAST_OFFSET：最新位置,DESIGN_TIMESTAMP：指定时间, DESIGN_OFFSET：指定位置 */
	private String expectStartOffset;
	/** 源分区期望开始消费位置点的值 */
	private Long expectStartOffsetValue;
	/** 源分区最小消费位置点 */
	private Long minOffset;
	/** 源分区最大消费位置点 */
	private Long highWatermark;
	/** 源分区已提交消费位置点 */
	private Long commitOffset;
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
	/** 源初始化失败次数 */
	private AtomicInteger initFailureCountForConsumer = new AtomicInteger(0);
	/** 目标初始化失败次数 */
	private AtomicInteger initFailureCountForProducer = new AtomicInteger(0);
	
	/** 待提交位置点 */
	private volatile Long waitCommitOffset;
	/** 目标分区最后生产时间 */
	private Long lastProducerTime;
	/** 最大缓存记录数 */
	private Long maxCacheRecords;
	
	private int hash = 0;
	
	private FilterChain filterChain;
	private ConvertChain convertChain;
	/** 订阅状态 */
	private volatile boolean initSubscribe = false;
	/** 获取位置点状态 */
	private volatile boolean initFetchOffset = false;
	/** 生产订阅状态 */
	private volatile boolean initSubscribeForProducer = false;
	
	private ConsumerPartition consumerPartition;
	private ProducerPartition producerPartition;
	
	private ConsumerConfig consumerConfig;
	private ProducerConfig producerConfig;
	private TransferPartitionConfig transferConfig;
	/** 告警配置 */
	private AlarmConfig alarmConfig;
	
	public TaskPartition(String taskCode, String fromClusterCode, String fromTopic, Integer fromPartition, String fromBrokerName, 
			String expectStartOffset, Long expectStartOffsetValue,
			String toClusterCode, String toTopic, Integer toPartition, String toBrokerName) {
		this.taskCode = taskCode;
		this.fromClusterCode = fromClusterCode;
		this.fromTopic = fromTopic;
		this.fromPartition = fromPartition;
		this.fromBrokerName = fromBrokerName;
		this.expectStartOffset = expectStartOffset;
		this.expectStartOffsetValue = expectStartOffsetValue;
		this.toClusterCode = toClusterCode;
		this.toTopic = toTopic;
		this.toPartition = toPartition;
		this.toBrokerName = toBrokerName;
		this.fromDesc = "code:" + taskCode + ",topic:" + fromTopic + ",partition:" + getFromPartitionKey();
		this.toDesc = "code:" + taskCode + ",topic:" + toTopic + ",partition:" + getFromPartitionKey();
		this.consumerPartition = new ConsumerPartition(this.taskCode, this.fromClusterCode, this.fromTopic, this.fromPartition, this.fromBrokerName, this.expectStartOffset, this.expectStartOffsetValue);
		this.producerPartition = new ProducerPartition(this.taskCode, this.toClusterCode, this.toTopic, this.toPartition, this.toBrokerName);
	}
	
	public String getTaskCode() {
		return taskCode;
	}
	public void setTaskCode(String taskCode) {
		this.taskCode = taskCode;
	}
	public String getFromClusterCode() {
		return fromClusterCode;
	}
	public void setFromClusterCode(String fromClusterCode) {
		this.fromClusterCode = fromClusterCode;
	}
	public String getFromTopic() {
		return fromTopic;
	}
	public void setFromTopic(String fromTopic) {
		this.fromTopic = fromTopic;
	}
	public Integer getFromPartition() {
		return fromPartition;
	}
	public void setFromPartition(Integer fromPartition) {
		this.fromPartition = fromPartition;
	}
	public String getFromBrokerName() {
		return fromBrokerName;
	}
	public void setFromBrokerName(String fromBrokerName) {
		this.fromBrokerName = fromBrokerName;
	}
	public String getFromDesc() {
		return fromDesc;
	}
	public void setFromDesc(String fromDesc) {
		this.fromDesc = fromDesc;
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
	public String getCommitOffsetMode() {
		return commitOffsetMode;
	}
	public void setCommitOffsetMode(String commitOffsetMode) {
		this.commitOffsetMode = commitOffsetMode;
	}
	public String getConsumerGroup() {
		return consumerGroup;
	}
	public void setConsumerGroup(String consumerGroup) {
		this.consumerGroup = consumerGroup;
	}
	public Integer getFromPartitionIndex() {
		return fromPartitionIndex;
	}
	public void setFromPartitionIndex(Integer fromPartitionIndex) {
		this.fromPartitionIndex = fromPartitionIndex;
	}
	public Long getFetchOffset() {
		return fetchOffset;
	}
	public void setFetchOffset(Long fetchOffset) {
		this.fetchOffset = fetchOffset;
	}
	public Long getLastConsumeTime() {
		return lastConsumeTime;
	}
	public void setLastConsumeTime(Long lastConsumeTime) {
		this.lastConsumeTime = lastConsumeTime;
	}
	public String getExpectStartOffset() {
		return expectStartOffset;
	}
	public void setExpectStartOffset(String expectStartOffset) {
		this.expectStartOffset = expectStartOffset;
	}
	public Long getExpectStartOffsetValue() {
		return expectStartOffsetValue;
	}
	public void setExpectStartOffsetValue(Long expectStartOffsetValue) {
		this.expectStartOffsetValue = expectStartOffsetValue;
	}
	public Long getMinOffset() {
		return minOffset;
	}
	public void setMinOffset(Long minOffset) {
		this.minOffset = minOffset;
	}
	public Long getHighWatermark() {
		return highWatermark;
	}
	public void setHighWatermark(Long highWatermark) {
		this.highWatermark = highWatermark;
	}
	public Long getCommitOffset() {
		return commitOffset;
	}
	public void setCommitOffset(Long commitOffset) {
		this.commitOffset = commitOffset;
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
	public AtomicInteger getInitFailureCountForConsumer() {
		return initFailureCountForConsumer;
	}
	public void setInitFailureCountForConsumer(AtomicInteger initFailureCountForConsumer) {
		this.initFailureCountForConsumer = initFailureCountForConsumer;
	}
	public AtomicInteger getInitFailureCountForProducer() {
		return initFailureCountForProducer;
	}
	public void setInitFailureCountForProducer(AtomicInteger initFailureCountForProducer) {
		this.initFailureCountForProducer = initFailureCountForProducer;
	}
	public Long getMaxCacheRecords() {
		return maxCacheRecords;
	}
	public void setMaxCacheRecords(Long maxCacheRecords) {
		this.maxCacheRecords = maxCacheRecords;
	}
	
	public Long getLastProducerTime() {
		return lastProducerTime;
	}

	public void setLastProducerTime(Long lastProducerTime) {
		this.lastProducerTime = lastProducerTime;
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

	public Long getWaitCommitOffset() {
		return waitCommitOffset;
	}

	public void setWaitCommitOffset(Long waitCommitOffset) {
		this.waitCommitOffset = waitCommitOffset;
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
		return this.consumerPartition;
	}
	public ProducerPartition getProducerPartition() {
		return this.producerPartition;
	}
	
	public boolean isInitSubscribeForProducer() {
		return initSubscribeForProducer;
	}

	public void setInitSubscribeForProducer(boolean initSubscribeForProducer) {
		this.initSubscribeForProducer = initSubscribeForProducer;
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

	public TransferPartitionConfig getTransferConfig() {
		return transferConfig;
	}

	public void setTransferConfig(TransferPartitionConfig transferConfig) {
		this.transferConfig = transferConfig;
	}

	public AlarmConfig getAlarmConfig() {
		return alarmConfig;
	}

	public void setAlarmConfig(AlarmConfig alarmConfig) {
		this.alarmConfig = alarmConfig;
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

	public String getFromTopicPartitionKey() {
		return PartitionUtils.generatorTopicPartitionKey(this.getFromTopic(), this.getFromBrokerName(), this.getFromPartition());
	}
	public String getFromPartitionKey() {
		return PartitionUtils.generatorPartitionKey(this.getFromBrokerName(), this.getFromPartition());
	}
	public String getToResourcePartitionKey() {
		return PartitionUtils.generatorResourcePartitionKey(this.getTaskCode(), this.getToBrokerName(), this.getToPartition());
	}
	public String getToTopicPartitionKey() {
		return PartitionUtils.generatorTopicPartitionKey(this.getToTopic(), this.getToBrokerName(), this.getToPartition());
	}
	public String getToPartitionKey() {
		return PartitionUtils.generatorPartitionKey(this.getToBrokerName(), this.getToPartition());
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
		//fromTopic:{},fromPartition:{}:{},toTopic:{},toPartition:{}:{}
		build.append("taskCode").append(":").append(this.getTaskCode()).append(",");
		build.append("fromTopic").append(":").append(this.getFromTopic()).append(",");
		build.append("fromPartition").append(":").append(PartitionUtils.generatorPartitionKey(this.getFromBrokerName(), this.getFromPartition())).append(",");
		build.append("toTopic").append(":").append(this.getToTopic()).append(",");
		build.append("toPartition").append(":").append(PartitionUtils.generatorPartitionKey(this.getToBrokerName(), this.getToPartition()));
		return build.toString();
	}
	public String toFromString() {
		StringBuilder build = new StringBuilder();
		build.append("taskCode").append(":").append(this.getTaskCode()).append(",");
		build.append("fromTopic").append(":").append(this.getFromTopic()).append(",");
		build.append("fromPartition").append(":").append(PartitionUtils.generatorPartitionKey(this.getFromBrokerName(), this.getFromPartition()));
		return build.toString();
	}
	public String toFromDescString() {
		StringBuilder build = new StringBuilder();
		build.append("taskCode").append(":").append(this.getTaskCode()).append(",");
		build.append("cluster").append(":").append(this.getFromClusterCode()).append(",");
		build.append("topic").append(":").append(this.getFromTopic()).append(",");
		build.append("partition").append(":").append(PartitionUtils.generatorPartitionKey(this.getFromBrokerName(), this.getFromPartition()));
		return build.toString();
	}
	public String toToString() {
		StringBuilder build = new StringBuilder();
		build.append("taskCode").append(":").append(this.getTaskCode()).append(",");
		build.append("toTopic").append(":").append(this.getToTopic()).append(",");
		build.append("toPartition").append(":").append(PartitionUtils.generatorPartitionKey(this.getToBrokerName(), this.getToPartition()));
		return build.toString();
	}
	public String toToDescString() {
		StringBuilder build = new StringBuilder();
		build.append("taskCode").append(":").append(this.getTaskCode()).append(",");
		build.append("cluster").append(":").append(this.getToClusterCode()).append(",");
		build.append("topic").append(":").append(this.getToTopic()).append(",");
		build.append("partition").append(":").append(PartitionUtils.generatorPartitionKey(this.getToBrokerName(), this.getToPartition()));
		return build.toString();
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
            return true;
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass())
            return false;
		TaskPartition target = (TaskPartition)obj;
		String targetKey = target.getTaskCode() + "#" + target.getFromPartitionKey();
		String sourceKey = this.getTaskCode() + "#" + this.getFromPartitionKey();
		return sourceKey.equals(targetKey);
	}
	@Override
	public int hashCode() {
		if (hash != 0)
            return hash;
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hashCode(this.getTaskCode());
        result = prime * result + Objects.hashCode(this.getFromPartitionKey());
        result = prime * result + Objects.hashCode(this.getFromTopic());
        this.hash = result;
        return result;
	}
	
}
