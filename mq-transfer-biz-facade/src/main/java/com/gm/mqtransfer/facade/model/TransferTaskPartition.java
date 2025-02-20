package com.gm.mqtransfer.facade.model;

import com.gm.mqtransfer.provider.facade.util.PartitionUtils;

public class TransferTaskPartition {
	/** 分区 */
	private int partition;
	/** 分区所属brokerName */
	private String brokerName;
	/** 分区开始消费位置点类型（自定义开始位置点，当分组的消费位点不存在的时候，该字段值有效）,FIRST_OFFSET：开始位置,LAST_OFFSET：最新位置,DESIGN_TIMESTAMP：指定时间, DESIGN_OFFSET：指定位置 */
	private String offsetType;
	/** 分区开始消费位置点的值，可能是位置点或时间截 */
	private Long offset;
	
	public int getPartition() {
		return partition;
	}
	public void setPartition(int partition) {
		this.partition = partition;
	}
	public String getBrokerName() {
		return brokerName;
	}
	public void setBrokerName(String brokerName) {
		this.brokerName = brokerName;
	}
	
	public String getOffsetType() {
		return offsetType;
	}
	public void setOffsetType(String offsetType) {
		this.offsetType = offsetType;
	}
	public Long getOffset() {
		return offset;
	}
	public void setOffset(Long offset) {
		this.offset = offset;
	}
	
	public String getPartitionKey() {
		return PartitionUtils.generatorPartitionKey(brokerName, partition);
	}
}
