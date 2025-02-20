package com.gm.mqtransfer.provider.facade.model;

import java.util.Properties;

public class ConsumerClientConfig {

	/** 是否是共享消费客户端 */
	private boolean clientShare;
	/** 消费集群信息 */
	private ClusterInfo cluster;
	/** 消费客户端实例序号 */
	private int instanceIndex;
	/** 消费客户端实例编号 */
	private String instanceCode;
	/** 消费客户端原生配置 */
	private Properties consumerProps;
	/** 消费组 */
	private String consumerGroup;
	
	public boolean isClientShare() {
		return clientShare;
	}
	public void setClientShare(boolean clientShare) {
		this.clientShare = clientShare;
	}
	public ClusterInfo getCluster() {
		return cluster;
	}
	public void setCluster(ClusterInfo cluster) {
		this.cluster = cluster;
	}
	public int getInstanceIndex() {
		return instanceIndex;
	}
	public void setInstanceIndex(int instanceIndex) {
		this.instanceIndex = instanceIndex;
	}
	public String getInstanceCode() {
		return instanceCode;
	}
	public void setInstanceCode(String instanceCode) {
		this.instanceCode = instanceCode;
	}
	public Properties getConsumerProps() {
		return consumerProps;
	}
	public void setConsumerProps(Properties consumerProps) {
		this.consumerProps = consumerProps;
	}
	public String getConsumerGroup() {
		return consumerGroup;
	}
	public void setConsumerGroup(String consumerGroup) {
		this.consumerGroup = consumerGroup;
	}
	
}
