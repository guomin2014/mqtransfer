package com.gm.mqtransfer.manager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "transfer.cluster")
public class ClusterConfig {

	/** ZK地址 */
	private String zkUrl;
	/** 集群配置，格式:JSON对象 */
	private String clusterConfig;
	/** 自动均衡延迟执行时间，单位：秒 */
	private int autoRebalanceDelayInSeconds;
	/** 自动均衡是否启用 */
	private boolean autoRebalanceEnable;
	/** 自动均衡策略，AssignPriority：指定优先（先将指定的资源分配，然后在将余下的平均分配），UnassignPriority：未指定优先（先将未指定的平均分配，然后再将指定的分配） */
	private String autoRebalanceStrategy;
	
	public String getZkUrl() {
		return zkUrl;
	}
	public void setZkUrl(String zkUrl) {
		this.zkUrl = zkUrl;
	}
	public String getClusterConfig() {
		return clusterConfig;
	}
	public void setClusterConfig(String clusterConfig) {
		this.clusterConfig = clusterConfig;
	}
	public int getAutoRebalanceDelayInSeconds() {
		return autoRebalanceDelayInSeconds;
	}
	public void setAutoRebalanceDelayInSeconds(int autoRebalanceDelayInSeconds) {
		this.autoRebalanceDelayInSeconds = autoRebalanceDelayInSeconds;
	}
	public boolean isAutoRebalanceEnable() {
		return autoRebalanceEnable;
	}
	public void setAutoRebalanceEnable(boolean autoRebalanceEnable) {
		this.autoRebalanceEnable = autoRebalanceEnable;
	}
	public String getAutoRebalanceStrategy() {
		return autoRebalanceStrategy;
	}
	public void setAutoRebalanceStrategy(String autoRebalanceStrategy) {
		this.autoRebalanceStrategy = autoRebalanceStrategy;
	}
}
