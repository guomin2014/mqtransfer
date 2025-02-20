package com.gm.mqtransfer.worker.model;

public class ConsumerStatInfo {
	/** 源分区 */
	private String sourcePartition;
	/** 目标分区 */
	private String targetPartition;
	/** 最小位置点 */
	private Long minOffset;
	/** 最大位置点 */
	private Long maxOffset;
	/** 消费位置点 */
	private Long consumerOffset;
	/** 消费Lag */
	private Long consumerLag;
	/** 最后消费时间 */
	private Long lastConsumerTime;
	/** 缓存队列数 */
	private Long cacheQueueSize;
	/** 缓存数据大小 */
	private Long cacheDataSize;
	/** 总消费记录数 */
	private Long totalConsumerCount;
	/** 总消费数据大小，单位：byte */
	private Long totalConsumerDataSize;
	
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
	public Long getLastConsumerTime() {
		return lastConsumerTime;
	}
	public void setLastConsumerTime(Long lastConsumerTime) {
		this.lastConsumerTime = lastConsumerTime;
	}
	public Long getCacheQueueSize() {
		return cacheQueueSize;
	}
	public void setCacheQueueSize(Long cacheQueueSize) {
		this.cacheQueueSize = cacheQueueSize;
	}
	public Long getCacheDataSize() {
		return cacheDataSize;
	}
	public void setCacheDataSize(Long cacheDataSize) {
		this.cacheDataSize = cacheDataSize;
	}
	public Long getTotalConsumerCount() {
		return totalConsumerCount;
	}
	public void setTotalConsumerCount(Long totalConsumerCount) {
		this.totalConsumerCount = totalConsumerCount;
	}
	public Long getTotalConsumerDataSize() {
		return totalConsumerDataSize;
	}
	public void setTotalConsumerDataSize(Long totalConsumerDataSize) {
		this.totalConsumerDataSize = totalConsumerDataSize;
	}
}
