package com.gm.mqtransfer.worker.model;

public class ProducerStatInfo {
	/** 源分区 */
	private String sourcePartition;
	/** 目标分区 */
	private String targetPartition;
	/** 转发位置点 */
	private Long commitOffset;
	/** 转发Lag */
	private Long producerLag;
	/** 最后转发时间 */
	private Long lastProducerTime;
	/** 总过滤记录数 */
	private Long totalFilterCount;
	/** 总过滤数据大小，单位：byte */
	private Long totalFilterDataSize;
	/** 总转发记录数 */
	private Long totalProducerCount;
	/** 总转发数据大小，单位：byte */
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
	public Long getLastProducerTime() {
		return lastProducerTime;
	}
	public void setLastProducerTime(Long lastProducerTime) {
		this.lastProducerTime = lastProducerTime;
	}
	public Long getTotalFilterCount() {
		return totalFilterCount;
	}
	public void setTotalFilterCount(Long totalFilterCount) {
		this.totalFilterCount = totalFilterCount;
	}
	public Long getTotalFilterDataSize() {
		return totalFilterDataSize;
	}
	public void setTotalFilterDataSize(Long totalFilterDataSize) {
		this.totalFilterDataSize = totalFilterDataSize;
	}
	public Long getTotalProducerCount() {
		return totalProducerCount;
	}
	public void setTotalProducerCount(Long totalProducerCount) {
		this.totalProducerCount = totalProducerCount;
	}
	public Long getTotalProducerDataSize() {
		return totalProducerDataSize;
	}
	public void setTotalProducerDataSize(Long totalProducerDataSize) {
		this.totalProducerDataSize = totalProducerDataSize;
	}
	
}
