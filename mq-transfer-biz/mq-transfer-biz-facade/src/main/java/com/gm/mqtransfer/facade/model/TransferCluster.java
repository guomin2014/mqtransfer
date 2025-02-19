package com.gm.mqtransfer.facade.model;

import com.gm.mqtransfer.provider.facade.model.ClusterInfo;

public class TransferCluster extends ClusterInfo{

	/** 群集ZK地址 */
	private String zkUrl;
	/** 群集nameserver地址 */
	private String nameSvrAddr;
	/** 群集Broker列表 */
	private String brokerList;
	/** 状态，0：停用，1：启用 */
	private Integer status;
	
	public String getZkUrl() {
		return zkUrl;
	}

	public void setZkUrl(String zkUrl) {
		this.zkUrl = zkUrl;
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
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	@Override
	public String toString() {
		return "TransferCluster [code=" + this.getCode() + ", type=" + this.getType() + ", version=" + this.getVersion() + ", clientVersion="
				+ getClientVersion() + ", zkUrl=" + this.getZkUrl() + ", brokers=" + this.getBrokerList() + "]";
	}
	
}
