package com.gm.mqtransfer.facade.config;

//@Configuration
//@ConfigurationProperties(prefix = "transfer.module.storage")
public class StorageConfig extends AbstractConfig {

	/** 存储类型，file、zookeeper */
	private String type;
	/** 数据文件存储ZK地址 */
	private String zkUrl;
	/** 数据文件存储路径 */
	private String filePath;
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
	
	private static StorageConfig instance;
	
	private StorageConfig () {
		super("mqtransfer.properties", "transfer.data.storage.");
		this.loadContent();
	}
	
	public static StorageConfig getInstance() {
		if (instance == null) {
			synchronized (StorageConfig.class) {
				if (instance == null) {
					instance = new StorageConfig();
				}
			}
		}
		return instance;
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getZkUrl() {
		return zkUrl;
	}
	public void setZkUrl(String zkUrl) {
		this.zkUrl = zkUrl;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
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

	@Override
	public String toString() {
		return "StorageConfig [type=" + type + ", zkUrl=" + zkUrl + ", filePath=" + filePath + ", sessionTimeoutMs="
				+ sessionTimeoutMs + ", connectionTimeoutMs=" + connectionTimeoutMs + ", maxSleepIntervalTimeMs="
				+ maxSleepIntervalTimeMs + ", maxRetries=" + maxRetries + ", maxSleepMs=" + maxSleepMs + "]";
	}
	
}
