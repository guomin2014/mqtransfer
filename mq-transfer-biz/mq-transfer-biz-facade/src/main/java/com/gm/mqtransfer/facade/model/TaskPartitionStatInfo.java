package com.gm.mqtransfer.facade.model;

public class TaskPartitionStatInfo {
	/** 源分区 */
	private String sourcePartition;
	/** 目标分区 */
	private String targetPartition;
	/** 初始化最小位置点 */
	private Long initMinOffset;
	/** 最小位置点 */
	private Long minOffset;
	/** 最大位置点 */
	private Long maxOffset;
	/** 消费位置点 */
	private Long consumerOffset;
	/** 消费Lag */
	private Long consumerLag;
	/** 提交位置点 */
	private Long commitOffset;
	/** 生产Lag */
	private Long producerLag;
	/** 最后消费时间 */
	private Long lastConsumerTime;
	/** 最后生产时间 */
	private Long lastProducerTime;
	/** 缓存队列数 */
	private Long cacheQueueSize;
	/** 缓存数据大小 */
	private Long cacheDataSize;
	/** 总消费记录数 */
	private Long totalConsumerCount;
	/** 总消费数据大小，单位：byte */
	private Long totalConsumerDataSize;
	/** 总过滤记录数 */
	private Long totalFilterCount;
	/** 总过滤数据大小，单位：byte */
	private Long totalFilterDataSize;
	/** 总生产记录数 */
	private Long totalProducerCount;
	/** 总生产数据大小，单位：byte */
	private Long totalProducerDataSize;
	
	public String getSourcePartition() {
		return sourcePartition;
	}
	public void setSourcePartition(String sourcePartition) {
		this.sourcePartition = sourcePartition;
	}
	public String getTargetPartition() {
		return targetPartition;
	}
	public void setTargetPartition(String targetPartition) {
		this.targetPartition = targetPartition;
	}
	public Long getInitMinOffset() {
		return initMinOffset;
	}
	public void setInitMinOffset(Long initMinOffset) {
		this.initMinOffset = initMinOffset;
	}
	public Long getMinOffset() {
		return minOffset;
	}
	public void setMinOffset(Long minOffset) {
		this.minOffset = minOffset;
	}
	public Long getMaxOffset() {
		return maxOffset;
	}
	public void setMaxOffset(Long maxOffset) {
		this.maxOffset = maxOffset;
	}
	public Long getConsumerOffset() {
		return consumerOffset;
	}
	public void setConsumerOffset(Long consumerOffset) {
		this.consumerOffset = consumerOffset;
	}
	public Long getConsumerLag() {
		return consumerLag;
	}
	public void setConsumerLag(Long consumerLag) {
		this.consumerLag = consumerLag;
	}
	public Long getCommitOffset() {
		return commitOffset;
	}
	public void setCommitOffset(Long commitOffset) {
		this.commitOffset = commitOffset;
	}
	public Long getProducerLag() {
		return producerLag;
	}
	public void setProducerLag(Long producerLag) {
		this.producerLag = producerLag;
	}
	public Long getLastConsumerTime() {
		return lastConsumerTime;
	}
	public void setLastConsumerTime(Long lastConsumerTime) {
		this.lastConsumerTime = lastConsumerTime;
	}
	public Long getLastProducerTime() {
		return lastProducerTime;
	}
	public void setLastProducerTime(Long lastProducerTime) {
		this.lastProducerTime = lastProducerTime;
	}
	public Long getCacheQueueSize() {
		return cacheQueueSize;
	}
	public void setCacheQueueSize(Long cacheQueueSize) {
		this.cacheQueueSize = cacheQueueSize;
	}
	public Long getTotalConsumerCount() {
		return totalConsumerCount;
	}
	public void setTotalConsumerCount(Long totalConsumerCount) {
		this.totalConsumerCount = totalConsumerCount;
	}
	public Long getTotalFilterCount() {
		return totalFilterCount;
	}
	public void setTotalFilterCount(Long totalFilterCount) {
		this.totalFilterCount = totalFilterCount;
	}
	public Long getTotalProducerCount() {
		return totalProducerCount;
	}
	public void setTotalProducerCount(Long totalProducerCount) {
		this.totalProducerCount = totalProducerCount;
	}
	public Long getCacheDataSize() {
		return cacheDataSize;
	}
	public void setCacheDataSize(Long cacheDataSize) {
		this.cacheDataSize = cacheDataSize;
	}
	public Long getTotalConsumerDataSize() {
		return totalConsumerDataSize;
	}
	public void setTotalConsumerDataSize(Long totalConsumerDataSize) {
		this.totalConsumerDataSize = totalConsumerDataSize;
	}
	public Long getTotalFilterDataSize() {
		return totalFilterDataSize;
	}
	public void setTotalFilterDataSize(Long totalFilterDataSize) {
		this.totalFilterDataSize = totalFilterDataSize;
	}
	public Long getTotalProducerDataSize() {
		return totalProducerDataSize;
	}
	public void setTotalProducerDataSize(Long totalProducerDataSize) {
		this.totalProducerDataSize = totalProducerDataSize;
	}
	
}
