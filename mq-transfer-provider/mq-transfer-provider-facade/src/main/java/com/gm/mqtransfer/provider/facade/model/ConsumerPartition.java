package com.gm.mqtransfer.provider.facade.model;

import java.util.Objects;

import com.gm.mqtransfer.provider.facade.util.PartitionUtils;

public class ConsumerPartition {
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
	/** 源分区期望开始消费位置点（自定义开始位置点，当分组的消费位点不存在的时候，该字段值有效）,FIRST_OFFSET：开始位置,LAST_OFFSET：最新位置,DESIGN_TIMESTAMP：指定时间, DESIGN_OFFSET：指定位置 */
	private String expectStartOffset;
	/** 源分区期望开始消费位置点的值 */
	private Long expectStartOffsetValue;
	
	/** 源分区开始消费位置点（如果为空：表示还未初始化位置点） */
	private Long startOffset;
	/** 源分区最后消费时间 */
	private Long startOffsetLastTime;
	/** 源分区最大消费位置点 */
	private Long highWatermark;
	/** 源分区首次消费位置点 */
	private Long firstOffset;
	
	private int hash = 0;
	
	public ConsumerPartition(String taskCode, String fromClusterCode, String fromTopic, Integer fromPartition, String fromBrokerName, String expectStartOffset, Long expectStartOffsetValue) {
		this.taskCode = taskCode;
		this.fromClusterCode = fromClusterCode;
		this.fromTopic = fromTopic;
		this.fromPartition = fromPartition;
		this.fromBrokerName = fromBrokerName;
		this.expectStartOffset = expectStartOffset;
		this.expectStartOffsetValue = expectStartOffsetValue;
		this.fromDesc = "code:" + taskCode + ",topic:" + fromTopic + ",partition:" + getFromPartitionKey();
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
	public Long getStartOffset() {
		return startOffset;
	}
	public void setStartOffset(Long startOffset) {
		this.startOffset = startOffset;
	}
	public Long getStartOffsetLastTime() {
		return startOffsetLastTime;
	}
	public void setStartOffsetLastTime(Long startOffsetLastTime) {
		this.startOffsetLastTime = startOffsetLastTime;
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
	public Long getHighWatermark() {
		return highWatermark;
	}
	public void setHighWatermark(Long highWatermark) {
		this.highWatermark = highWatermark;
	}
	public Long getFirstOffset() {
		return firstOffset;
	}
	public void setFirstOffset(Long firstOffset) {
		this.firstOffset = firstOffset;
	}
	public boolean isInitStartOffset() {
		return this.startOffset != null;
	}
	public String getFromTopicPartitionKey() {
		return PartitionUtils.generatorTopicPartitionKey(this.getFromTopic(), this.getFromBrokerName(), this.getFromPartition());
	}
	public String getFromPartitionKey() {
		return PartitionUtils.generatorPartitionKey(this.getFromBrokerName(), this.getFromPartition());
	}
	public String toString() {
		StringBuilder build = new StringBuilder();
		//fromTopic:{},fromPartition:{}:{},toTopic:{},toPartition:{}:{}
		build.append("taskCode").append(":").append(this.getTaskCode()).append(",");
		build.append("fromTopic").append(":").append(this.getFromTopic()).append(",");
		build.append("fromPartition").append(":").append(PartitionUtils.generatorPartitionKey(this.getFromBrokerName(), this.getFromPartition())).append(",");
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
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
            return true;
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass())
            return false;
		ConsumerPartition target = (ConsumerPartition)obj;
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
