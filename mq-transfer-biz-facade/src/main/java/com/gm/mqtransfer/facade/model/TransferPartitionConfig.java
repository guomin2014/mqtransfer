package com.gm.mqtransfer.facade.model;

/**
 * 转发配置
 * @author GM
 * @date 2024-06-20
 *
 */
public class TransferPartitionConfig {

	/** 分区匹配策略，AVG_BY_CIRCLE:平均分配，CONFIG：配置，PARTITION_ROOM：分区匹配，HASH：hash值，RANDOM：随机 */
	private String partitionMatchStrategy;
	/** 分区匹配配图，如：1:1,2:2,3:3,4:4 */
	private String partitionMatchConfig;
	/** 分区权重 */
	private Integer partitionWeight;
	/** 缓存队列内存最大使用占比，值为1-100 */
	private Integer cacheQueueMemoryMaxRatio;
	/** 单分区任务最大可缓存记录数 */
	private int maxCacheRecords = 10000;
	/** 转发源分区信息 */
	private TransferTaskPartition partition;
	/** 指定转发实例 */
	private String assignInstance;
	
	/**
	 * 获取 分区匹配策略，AVG_BY_CIRCLE:平均分配，CONFIG：配置，PARTITION_ROOM：分区匹配，HASH：hash值，RANDOM：随机
	 * @return String
	 */
	public String getPartitionMatchStrategy(){
		return this.partitionMatchStrategy;
	}

	/**
	 * 设置 分区匹配策略，AVG_BY_CIRCLE:平均分配，CONFIG：配置，PARTITION_ROOM：分区匹配，HASH：hash值，RANDOM：随机
	 * @param partitionMatchStrategy
	 */
	public void setPartitionMatchStrategy(String partitionMatchStrategy){
		this.partitionMatchStrategy = partitionMatchStrategy;
	}

	/**
	 * 获取 分区匹配配图，如：1:1,2:2,3:3,4:4
	 * @return String
	 */
	public String getPartitionMatchConfig(){
		return this.partitionMatchConfig;
	}

	/**
	 * 设置 分区匹配配图，如：1:1,2:2,3:3,4:4
	 * @param partitionMatchConfig
	 */
	public void setPartitionMatchConfig(String partitionMatchConfig){
		this.partitionMatchConfig = partitionMatchConfig;
	}

	public Integer getCacheQueueMemoryMaxRatio() {
		return cacheQueueMemoryMaxRatio;
	}

	public void setCacheQueueMemoryMaxRatio(Integer cacheQueueMemoryMaxRatio) {
		this.cacheQueueMemoryMaxRatio = cacheQueueMemoryMaxRatio;
	}

	public int getMaxCacheRecords() {
		return maxCacheRecords;
	}

	public void setMaxCacheRecords(int maxCacheRecords) {
		this.maxCacheRecords = maxCacheRecords;
	}

	public TransferTaskPartition getPartition() {
		return partition;
	}

	public void setPartition(TransferTaskPartition partition) {
		this.partition = partition;
	}

	public Integer getPartitionWeight() {
		return partitionWeight;
	}

	public void setPartitionWeight(Integer partitionWeight) {
		this.partitionWeight = partitionWeight;
	}

	public String getAssignInstance() {
		return assignInstance;
	}

	public void setAssignInstance(String assignInstance) {
		this.assignInstance = assignInstance;
	}

}
