package com.gm.mqtransfer.provider.facade.model;

public class RocketMQClusterInfo extends ClusterInfo {

	/** 群集nameserver地址 */
	private String nameSvrAddr;
	/** 群集Broker列表 */
	private String brokerList;
	
	public RocketMQClusterInfo() {
		
	}
	
	public RocketMQClusterInfo(String code, String name, String type, String version, String zone, String nameSvrAddr,
			String brokerList) {
		super(code, name, type, version, zone);
		this.nameSvrAddr = nameSvrAddr;
		this.brokerList = brokerList;
	}
	public String getNameSvrAddr() {
		return nameSvrAddr;
	}
	public void setNameSvrAddr(String nameSvrAddr) {
		this.nameSvrAddr = nameSvrAddr;
	}
	public String getBrokerList() {
		return brokerList;
	}
	public void setBrokerList(String brokerList) {
		this.brokerList = brokerList;
	}

}
