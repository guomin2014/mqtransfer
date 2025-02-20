package com.gm.mqtransfer.provider.facade.model;

import java.util.Properties;

public class ProducerClientConfig {

	/** 是否是共享生产客户端 */
	private boolean clientShare;
	/** 生产集群信息 */
	private ClusterInfo cluster;
	/** 生产客户端实例序号 */
	private int instanceIndex;
	/** 生产客户端实例编号 */
	private String instanceCode;
	/** 生产客户端原生配置 */
	private Properties producerProps;
	
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
	public Properties getProducerProps() {
		return producerProps;
	}
	public void setProducerProps(Properties producerProps) {
		this.producerProps = producerProps;
	}
	
}
