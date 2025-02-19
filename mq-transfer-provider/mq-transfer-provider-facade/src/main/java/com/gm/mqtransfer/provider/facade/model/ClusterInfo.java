package com.gm.mqtransfer.provider.facade.model;

import java.util.ArrayList;
import java.util.List;

import com.gm.mqtransfer.provider.facade.common.MQTypeEnum;
import com.gm.mqtransfer.provider.facade.util.BeanUtils;

/**
 * 集群基础信息
 * @author GuoMin
 * @date 2023-05-12 09:13:27
 *
 */
public class ClusterInfo {
	/** 群集ID */
	private String code;
	/** 群集名称 */
	private String name;
	/** 群集类型 */
	private String type;
	/** 群集版本 */
	private String version;
	/** 群集单元 */
	private String zone;
	
	/** 拉取消息最大大小 */
	private Long fetchMaxBytes;
	/** 拉取消息最大行数 */
	private Integer fetchMaxRecords;
	/** 拉取消息最大等待时长，单位：毫秒 */
	private Long fetchMaxWaitMs;
	/** 接收消息最大大小 */
	private Long receiveMaxBytes;
	/** 接收消息最大行数 */
	private Integer receiveMaxRecords;
	/** 单条消息最大大小 */
	private Long singleMessageMaxBytes;
	
	/** 允许访问的客户端版本号，多个使用逗号分隔 */
	private String clientVersion;
	
	public ClusterInfo() {
		
	}
	
	public ClusterInfo(String code, String name, String type, String version, String zone) {
		this.code = code;
		this.name = name;
		this.type = type;
		this.version = version;
		this.zone = zone;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getZone() {
		return zone;
	}
	public void setZone(String zone) {
		this.zone = zone;
	}
	public Long getFetchMaxBytes() {
		return fetchMaxBytes;
	}
	public void setFetchMaxBytes(Long fetchMaxBytes) {
		this.fetchMaxBytes = fetchMaxBytes;
	}
	public Integer getFetchMaxRecords() {
		return fetchMaxRecords;
	}
	public void setFetchMaxRecords(Integer fetchMaxRecords) {
		this.fetchMaxRecords = fetchMaxRecords;
	}
	public Long getFetchMaxWaitMs() {
		return fetchMaxWaitMs;
	}
	public void setFetchMaxWaitMs(Long fetchMaxWaitMs) {
		this.fetchMaxWaitMs = fetchMaxWaitMs;
	}
	public Long getReceiveMaxBytes() {
		return receiveMaxBytes;
	}
	public void setReceiveMaxBytes(Long receiveMaxBytes) {
		this.receiveMaxBytes = receiveMaxBytes;
	}
	public Integer getReceiveMaxRecords() {
		return receiveMaxRecords;
	}
	public void setReceiveMaxRecords(Integer receiveMaxRecords) {
		this.receiveMaxRecords = receiveMaxRecords;
	}
	public Long getSingleMessageMaxBytes() {
		return singleMessageMaxBytes;
	}
	public void setSingleMessageMaxBytes(Long singleMessageMaxBytes) {
		this.singleMessageMaxBytes = singleMessageMaxBytes;
	}
	public String getClientVersion() {
		return clientVersion;
	}
	public void setClientVersion(String clientVersion) {
		this.clientVersion = clientVersion;
	}
	/**
	 * 获取允许访问的客户端列表
	 * @return
	 */
	public List<String> fetchClientKeyList() {
		List<String> list = new ArrayList<>();
		if (clientVersion != null && clientVersion.length() > 0) {
			for (String version : clientVersion.split("[,，]")) {
				list.add(this.getType() + "#" + version);
			}
		} else {
			list.add(this.getType() + "#" + this.getVersion());
		}
		return list;
	}
	
	/**
	 * 转换成plugin需要的集群配置信息
	 * @return
	 */
	public ClusterInfo toPluginClusterInfo() {
		MQTypeEnum mqType = MQTypeEnum.getByName(this.getType());
		ClusterInfo clusterInfo = null;
		switch (mqType) {
		case Kafka:
			clusterInfo = BeanUtils.copy(this, KafkaClusterInfo.class);
			break;
		case RocketMQ:
			clusterInfo = BeanUtils.copy(this, RocketMQClusterInfo.class);
			break;
		}
		return clusterInfo;
	}

	public String toSimpleString() {
		StringBuilder build = new StringBuilder();
		build.append("[");
		build.append("code:").append(this.getCode()).append(",");
		build.append("name:").append(this.getName()).append(",");
		build.append("zone:").append(this.getZone()).append(",");
		build.append("]");
		return build.toString();
	}
}
