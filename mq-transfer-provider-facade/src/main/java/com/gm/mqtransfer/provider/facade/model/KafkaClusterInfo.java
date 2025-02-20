package com.gm.mqtransfer.provider.facade.model;

/**
 * Kafka集群信息
 * @author GuoMin
 * @date 2023-05-11 19:55:04
 *
 */
public class KafkaClusterInfo extends ClusterInfo {
	/** 群集ZK地址 */
	private String zkUrl;
	/** 群集Broker列表 */
	private String brokerList;
	
	public KafkaClusterInfo() {
		
	}
	
	public KafkaClusterInfo(String code, String name, String type, String version, String zone, String zkUrl,
			String brokerList) {
		super(code, name, type, version, zone);
		this.zkUrl = zkUrl;
		this.brokerList = brokerList;
	}
	public String getZkUrl() {
		return zkUrl;
	}
	public void setZkUrl(String zkUrl) {
		this.zkUrl = zkUrl;
	}
	public String getBrokerList() {
		return brokerList;
	}
	public void setBrokerList(String brokerList) {
		this.brokerList = brokerList;
	}
	
	public String toSimpleString() {
		StringBuilder build = new StringBuilder();
		build.append("[");
		build.append("code:").append(this.getCode()).append(",");
		build.append("name:").append(this.getName()).append(",");
		build.append("zone:").append(this.getZone()).append(",");
		if (this.getZkUrl() != null) {
			build.append("zk:").append(this.getZkUrl()).append(",");
		}
		if (this.getBrokerList() != null) {
			build.append("brokers:").append(this.getBrokerList()).append(",");
		}
		build.append("]");
		return build.toString();
	}
}
