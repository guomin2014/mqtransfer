package com.gm.mqtransfer.module.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "transfer.module.storage")
public class StorageConfig {

	/** ZK地址 */
	private String zkUrl;
	/** session超时时间 */
	private Integer sessionTimeoutMs = 10000;
	/** 连接超时时间 */
	private Integer connectionTimeoutMs = 5000;
	/** 重试之间等待的初始时间 */
	private Integer maxSleepIntervalTimeMs = 1000;
	/** 最大重试次数 */
	private Integer maxRetries = 5;
	/** 每次重试的量大睡眠时间 */
	private Integer maxSleepMs = 5000;
	
	public String getZkUrl() {
		return zkUrl;
	}
	public void setZkUrl(String zkUrl) {
		this.zkUrl = zkUrl;
	}
	public Integer getSessionTimeoutMs() {
		return sessionTimeoutMs;
	}
	public void setSessionTimeoutMs(Integer sessionTimeoutMs) {
		this.sessionTimeoutMs = sessionTimeoutMs;
	}
	public Integer getConnectionTimeoutMs() {
		return connectionTimeoutMs;
	}
	public void setConnectionTimeoutMs(Integer connectionTimeoutMs) {
		this.connectionTimeoutMs = connectionTimeoutMs;
	}
	public Integer getMaxSleepIntervalTimeMs() {
		return maxSleepIntervalTimeMs;
	}
	public void setMaxSleepIntervalTimeMs(Integer maxSleepIntervalTimeMs) {
		this.maxSleepIntervalTimeMs = maxSleepIntervalTimeMs;
	}
	public Integer getMaxRetries() {
		return maxRetries;
	}
	public void setMaxRetries(Integer maxRetries) {
		this.maxRetries = maxRetries;
	}
	public Integer getMaxSleepMs() {
		return maxSleepMs;
	}
	public void setMaxSleepMs(Integer maxSleepMs) {
		this.maxSleepMs = maxSleepMs;
	}
}
