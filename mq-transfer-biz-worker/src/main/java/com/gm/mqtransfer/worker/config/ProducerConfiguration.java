package com.gm.mqtransfer.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "transfer.producer")
public class ProducerConfiguration {
	/** 生产初始化失败是否告警 */
	private boolean initFailAlarmEnableForProducer = true;
	/** 生产丢弃日志是否告警 */
	private boolean skipMessageAlarmEnableForProducer = false;
	/** 生产发送失败是否告警 */
	private boolean sendMessageFailAlarmEnableForProducer = true;
	/** 生产发送慢是否打印日志 */
	private boolean sendMessageSlowPrintEnableForProducer = true;
	/** 生产发送慢的最大时间 */
	private long sendMessageSlowMaxMsForProducer = 10000;
	/** 生产发送的最大时间，超时将重发 */
	private long sendMessageTimeMaxMsForProducer = 120000;
	/** 生产每隔5秒自动提交位置点 */
	private long autoCommitIntervalMsForProducer = 5000;
	/** 生产批量发送最大可发送记录数 */
	private int maxBatchRecordsForProducer = 1000;
	/** 生产批量发送最大可发送大小，单位：byte */
	private long maxBatchSizeForProducer = 8388608;//8M
	/** 生产批量发送单条最大可发送大小，单位：byte */
	private long maxSingleSizeForProducer = 8388608;//8M
	/** 生产消息最大暂停时间，单位：毫秒 */
	private long maxSuspendTimeForProducer = 2000;//2s
	/** 消息最大可转发次数 */
	private int maxTTLForProducer = 3;
	/** 生产调式 */
	private boolean debugForProducer = false;
	/** 生产上报间隔时间，单位：毫秒 */
	private long reportIntervalTimeForProducer = 2000;//2s
	
	public boolean isInitFailAlarmEnableForProducer() {
		return initFailAlarmEnableForProducer;
	}
	public void setInitFailAlarmEnableForProducer(boolean initFailAlarmEnableForProducer) {
		this.initFailAlarmEnableForProducer = initFailAlarmEnableForProducer;
	}
	public boolean isSkipMessageAlarmEnableForProducer() {
		return skipMessageAlarmEnableForProducer;
	}
	public void setSkipMessageAlarmEnableForProducer(boolean skipMessageAlarmEnableForProducer) {
		this.skipMessageAlarmEnableForProducer = skipMessageAlarmEnableForProducer;
	}
	public boolean isSendMessageFailAlarmEnableForProducer() {
		return sendMessageFailAlarmEnableForProducer;
	}
	public void setSendMessageFailAlarmEnableForProducer(boolean sendMessageFailAlarmEnableForProducer) {
		this.sendMessageFailAlarmEnableForProducer = sendMessageFailAlarmEnableForProducer;
	}
	public boolean isSendMessageSlowPrintEnableForProducer() {
		return sendMessageSlowPrintEnableForProducer;
	}
	public void setSendMessageSlowPrintEnableForProducer(boolean sendMessageSlowPrintEnableForProducer) {
		this.sendMessageSlowPrintEnableForProducer = sendMessageSlowPrintEnableForProducer;
	}
	public long getSendMessageSlowMaxMsForProducer() {
		return sendMessageSlowMaxMsForProducer;
	}
	public void setSendMessageSlowMaxMsForProducer(long sendMessageSlowMaxMsForProducer) {
		this.sendMessageSlowMaxMsForProducer = sendMessageSlowMaxMsForProducer;
	}
	public long getSendMessageTimeMaxMsForProducer() {
		return sendMessageTimeMaxMsForProducer;
	}
	public void setSendMessageTimeMaxMsForProducer(long sendMessageTimeMaxMsForProducer) {
		this.sendMessageTimeMaxMsForProducer = sendMessageTimeMaxMsForProducer;
	}
	public long getAutoCommitIntervalMsForProducer() {
		return autoCommitIntervalMsForProducer;
	}
	public void setAutoCommitIntervalMsForProducer(long autoCommitIntervalMsForProducer) {
		this.autoCommitIntervalMsForProducer = autoCommitIntervalMsForProducer;
	}
	public int getMaxBatchRecordsForProducer() {
		return maxBatchRecordsForProducer;
	}
	public void setMaxBatchRecordsForProducer(int maxBatchRecordsForProducer) {
		this.maxBatchRecordsForProducer = maxBatchRecordsForProducer;
	}
	public long getMaxBatchSizeForProducer() {
		return maxBatchSizeForProducer;
	}
	public void setMaxBatchSizeForProducer(long maxBatchSizeForProducer) {
		this.maxBatchSizeForProducer = maxBatchSizeForProducer;
	}
	public long getMaxSingleSizeForProducer() {
		return maxSingleSizeForProducer;
	}
	public void setMaxSingleSizeForProducer(long maxSingleSizeForProducer) {
		this.maxSingleSizeForProducer = maxSingleSizeForProducer;
	}
	public long getMaxSuspendTimeForProducer() {
		return maxSuspendTimeForProducer;
	}
	public void setMaxSuspendTimeForProducer(long maxSuspendTimeForProducer) {
		this.maxSuspendTimeForProducer = maxSuspendTimeForProducer;
	}
	public int getMaxTTLForProducer() {
		return maxTTLForProducer;
	}
	public void setMaxTTLForProducer(int maxTTLForProducer) {
		this.maxTTLForProducer = maxTTLForProducer;
	}
	public boolean isDebugForProducer() {
		return debugForProducer;
	}
	public void setDebugForProducer(boolean debugForProducer) {
		this.debugForProducer = debugForProducer;
	}
	public long getReportIntervalTimeForProducer() {
		return reportIntervalTimeForProducer;
	}
	public void setReportIntervalTimeForProducer(long reportIntervalTimeForProducer) {
		this.reportIntervalTimeForProducer = reportIntervalTimeForProducer;
	}
	
}
