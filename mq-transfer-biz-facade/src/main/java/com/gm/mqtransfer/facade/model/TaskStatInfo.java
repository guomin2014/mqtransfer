package com.gm.mqtransfer.facade.model;

import java.util.List;

public class TaskStatInfo {

	/** 总可用内存 */
	private Long maxAvailableMemory;
	/** 总已用内存 */
	private Long totalUsedMemory;
	/** 总消费记录数 */
	private Long totalConsumerCount;
	/** 总缓存记录数 */
	private Long totalCacheQueueCount;
	/** 总过滤记录数 */
	private Long totalFilterCount;
	/** 总生产记录数 */
	private Long totalProducerCount;
	/** 总消费延迟数 */
	private Long totalConsumerLag;
	/** 总生产延迟数 */
	private Long totalProducerLag;
	/** 分区统计信息 */
	private List<TaskPartitionStatInfo> partitionStatList;
	
	public Long getMaxAvailableMemory() {
		return maxAvailableMemory;
	}
	public void setMaxAvailableMemory(Long maxAvailableMemory) {
		this.maxAvailableMemory = maxAvailableMemory;
	}
	public Long getTotalUsedMemory() {
		return totalUsedMemory;
	}
	public void setTotalUsedMemory(Long totalUsedMemory) {
		this.totalUsedMemory = totalUsedMemory;
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
	public Long getTotalCacheQueueCount() {
		return totalCacheQueueCount;
	}
	public void setTotalCacheQueueCount(Long totalCacheQueueCount) {
		this.totalCacheQueueCount = totalCacheQueueCount;
	}
	public Long getTotalConsumerLag() {
		return totalConsumerLag;
	}
	public void setTotalConsumerLag(Long totalConsumerLag) {
		this.totalConsumerLag = totalConsumerLag;
	}
	public Long getTotalProducerLag() {
		return totalProducerLag;
	}
	public void setTotalProducerLag(Long totalProducerLag) {
		this.totalProducerLag = totalProducerLag;
	}
	public List<TaskPartitionStatInfo> getPartitionStatList() {
		return partitionStatList;
	}
	public void setPartitionStatList(List<TaskPartitionStatInfo> partitionStatList) {
		this.partitionStatList = partitionStatList;
	}
}
