package com.gm.mqtransfer.facade.common;

public class TaskConfigConstants {
	/** 批量消费最大条数 */
	public static final String MAX_BATCH_RECORDS_KEY = "max.batch.records";
	/** 批量消费最大大小，单位：字节 */
	public static final String MAX_BATCH_SIZE_KEY = "max.batch.size";
	/** 批量消费最大等待时间 */
	public static final String MAX_BATCH_WAIT_MS_KEY = "max.batch.wait.ms";
	/** 单条消息最大大小，单位：字节 */
	public static final String MAX_SINGLE_SIZE_KEY = "max.single.size";
	/** 单分区最大缓存条数 */
	public static final String MAX_CACHE_RECORDS_KEY = "max.cache.records";
	/** 单分区最大缓存大小占比 */
	public static final String MAX_CACHE_RATIO_KEY = "max.cache.ratio";
	/** 期望消费的分区队列 */
	public static final String EXPECT_QUEUE_IDS_KEY = "expect.queue.ids";
	/** 期望消费的分区的开始位置点 */
	public static final String EXPECT_QUEUE_START_OFFSET_KEY = "expect.queue.start.offset";
	/** 延迟告警阈值 */
	public static final String LAG_ALARM_MAX_LAG_KEY = "lag.alarm.max.lag";
	/** 延迟告警阈值 */
	public static final String LAG_ALARM_INTERVAL_SECOND_KEY = "lag.alarm.interval.second";
	/** 位置点提交模式 */
	public static final String COMMIT_OFFSET_MODE_KEY = "commit.offset.mode";
	
	public static final String GROUP_ID_NAME = "group.id";
}
