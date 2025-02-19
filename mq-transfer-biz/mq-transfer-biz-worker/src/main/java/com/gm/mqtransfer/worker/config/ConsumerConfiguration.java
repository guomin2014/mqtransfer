package com.gm.mqtransfer.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "transfer.consumer")
public class ConsumerConfiguration {

	/** 消费初始化失败是否告警 */
	private boolean initFailAlarmEnableForConsumer = true;
	/** 消费丢弃日志是否告警 */
	private boolean skipMessageAlarmEnableForConsumer = false;
	/** 消费拉取失败是否告警 */
	private boolean pollMessageFailAlarmEnableForConsumer = true;
	/** 消费拉取慢是否告警 */
	private boolean pollMessageLagAlarmEnableForConsumer = true;
	/** 消费拉取慢是否打印日志 */
	private boolean pollMessageSlowPrintEnableForConsumer = true;
	/** 消费拉取慢的最大时间 */
	private long pollMessageSlowMaxMsForConsumer = 2000;
	/** 消费消息批量最大可拉取记录数 */
	private int maxBatchRecordsForConsumer = 1000;
	/** 消费消息批量最大可拉取大小 */
	private long maxBatchSizeForConsumer = 25165824;//24M;
	/** 消费消息单条最大大小，单位：byte */
	private long maxSingleSizeForConsumer = 8388608;//8M
	/** 消费消息最大等待时间，单位：毫秒 */
	private long maxBatchWaitMsForConsumer = 2000;//2s
	/** 单分区任务最大可缓存记录数 */
	private int maxCacheRecordsForConsumer = 10000;
	/** 消费消息最大暂停时间，单位：毫秒 */
	private long maxSuspendTimeForConsumer = 2000;//2s
	/** 消费告警的最大延迟 */
	private long lagAlarmMaxLagForConsumer = 20000;
	/** 消费告警的最大延迟持续时长 */
	private long lagAlarmIntervalSecondForConsumer = 3600;
	/** 消费消息最大批量分区数 */
	private int maxBatchPartitionsForConsumer = 30;
	/** 消费消息最大等待刷新meta时间，单位：毫秒 */
	private int maxWaiteRefreshMetadataMs = 500;
	/** 结束共享的最小延迟 */
	private int shareEndMinLagForConsumer = 1000;
	/** 结束共享的小于最小延迟的持续时长，单位：秒 */
	private int shareEndIntervalSecondForConsumer = 600;
	/** 结束共享的小于最小延迟的持续次数 */
	private int shareEndIntervalTimesForConsumer = 30;
	/** 消费调式 */
	private boolean debugForConsumer = false;
	/** 消费上报间隔时间，单位：毫秒 */
	private long reportIntervalTimeForConsumer = 2000;//2s
	
	public boolean isInitFailAlarmEnableForConsumer() {
		return initFailAlarmEnableForConsumer;
	}
	public void setInitFailAlarmEnableForConsumer(boolean initFailAlarmEnableForConsumer) {
		this.initFailAlarmEnableForConsumer = initFailAlarmEnableForConsumer;
	}
	public boolean isSkipMessageAlarmEnableForConsumer() {
		return skipMessageAlarmEnableForConsumer;
	}
	public void setSkipMessageAlarmEnableForConsumer(boolean skipMessageAlarmEnableForConsumer) {
		this.skipMessageAlarmEnableForConsumer = skipMessageAlarmEnableForConsumer;
	}
	public boolean isPollMessageFailAlarmEnableForConsumer() {
		return pollMessageFailAlarmEnableForConsumer;
	}
	public void setPollMessageFailAlarmEnableForConsumer(boolean pollMessageFailAlarmEnableForConsumer) {
		this.pollMessageFailAlarmEnableForConsumer = pollMessageFailAlarmEnableForConsumer;
	}
	public boolean isPollMessageLagAlarmEnableForConsumer() {
		return pollMessageLagAlarmEnableForConsumer;
	}
	public void setPollMessageLagAlarmEnableForConsumer(boolean pollMessageLagAlarmEnableForConsumer) {
		this.pollMessageLagAlarmEnableForConsumer = pollMessageLagAlarmEnableForConsumer;
	}
	public boolean isPollMessageSlowPrintEnableForConsumer() {
		return pollMessageSlowPrintEnableForConsumer;
	}
	public void setPollMessageSlowPrintEnableForConsumer(boolean pollMessageSlowPrintEnableForConsumer) {
		this.pollMessageSlowPrintEnableForConsumer = pollMessageSlowPrintEnableForConsumer;
	}
	public long getPollMessageSlowMaxMsForConsumer() {
		return pollMessageSlowMaxMsForConsumer;
	}
	public void setPollMessageSlowMaxMsForConsumer(long pollMessageSlowMaxMsForConsumer) {
		this.pollMessageSlowMaxMsForConsumer = pollMessageSlowMaxMsForConsumer;
	}
	public int getMaxBatchRecordsForConsumer() {
		return maxBatchRecordsForConsumer;
	}
	public void setMaxBatchRecordsForConsumer(int maxBatchRecordsForConsumer) {
		this.maxBatchRecordsForConsumer = maxBatchRecordsForConsumer;
	}
	public long getMaxBatchSizeForConsumer() {
		return maxBatchSizeForConsumer;
	}
	public void setMaxBatchSizeForConsumer(long maxBatchSizeForConsumer) {
		this.maxBatchSizeForConsumer = maxBatchSizeForConsumer;
	}
	public long getMaxSingleSizeForConsumer() {
		return maxSingleSizeForConsumer;
	}
	public void setMaxSingleSizeForConsumer(long maxSingleSizeForConsumer) {
		this.maxSingleSizeForConsumer = maxSingleSizeForConsumer;
	}
	public long getMaxBatchWaitMsForConsumer() {
		return maxBatchWaitMsForConsumer;
	}
	public void setMaxBatchWaitMsForConsumer(long maxBatchWaitMsForConsumer) {
		this.maxBatchWaitMsForConsumer = maxBatchWaitMsForConsumer;
	}
	public int getMaxCacheRecordsForConsumer() {
		return maxCacheRecordsForConsumer;
	}
	public void setMaxCacheRecordsForConsumer(int maxCacheRecordsForConsumer) {
		this.maxCacheRecordsForConsumer = maxCacheRecordsForConsumer;
	}
	public long getMaxSuspendTimeForConsumer() {
		return maxSuspendTimeForConsumer;
	}
	public void setMaxSuspendTimeForConsumer(long maxSuspendTimeForConsumer) {
		this.maxSuspendTimeForConsumer = maxSuspendTimeForConsumer;
	}
	public long getLagAlarmMaxLagForConsumer() {
		return lagAlarmMaxLagForConsumer;
	}
	public void setLagAlarmMaxLagForConsumer(long lagAlarmMaxLagForConsumer) {
		this.lagAlarmMaxLagForConsumer = lagAlarmMaxLagForConsumer;
	}
	public long getLagAlarmIntervalSecondForConsumer() {
		return lagAlarmIntervalSecondForConsumer;
	}
	public void setLagAlarmIntervalSecondForConsumer(long lagAlarmIntervalSecondForConsumer) {
		this.lagAlarmIntervalSecondForConsumer = lagAlarmIntervalSecondForConsumer;
	}
	public int getMaxBatchPartitionsForConsumer() {
		return maxBatchPartitionsForConsumer;
	}
	public void setMaxBatchPartitionsForConsumer(int maxBatchPartitionsForConsumer) {
		this.maxBatchPartitionsForConsumer = maxBatchPartitionsForConsumer;
	}
	public int getShareEndMinLagForConsumer() {
		return shareEndMinLagForConsumer;
	}
	public void setShareEndMinLagForConsumer(int shareEndMinLagForConsumer) {
		this.shareEndMinLagForConsumer = shareEndMinLagForConsumer;
	}
	public int getShareEndIntervalSecondForConsumer() {
		return shareEndIntervalSecondForConsumer;
	}
	public void setShareEndIntervalSecondForConsumer(int shareEndIntervalSecondForConsumer) {
		this.shareEndIntervalSecondForConsumer = shareEndIntervalSecondForConsumer;
	}
	public int getShareEndIntervalTimesForConsumer() {
		return shareEndIntervalTimesForConsumer;
	}
	public void setShareEndIntervalTimesForConsumer(int shareEndIntervalTimesForConsumer) {
		this.shareEndIntervalTimesForConsumer = shareEndIntervalTimesForConsumer;
	}
	public boolean isDebugForConsumer() {
		return debugForConsumer;
	}
	public void setDebugForConsumer(boolean debugForConsumer) {
		this.debugForConsumer = debugForConsumer;
	}
	public int getMaxWaiteRefreshMetadataMs() {
		return maxWaiteRefreshMetadataMs;
	}
	public void setMaxWaiteRefreshMetadataMs(int maxWaiteRefreshMetadataMs) {
		this.maxWaiteRefreshMetadataMs = maxWaiteRefreshMetadataMs;
	}
	public long getReportIntervalTimeForConsumer() {
		return reportIntervalTimeForConsumer;
	}
	public void setReportIntervalTimeForConsumer(long reportIntervalTimeForConsumer) {
		this.reportIntervalTimeForConsumer = reportIntervalTimeForConsumer;
	}
	
}
