package com.gm.mqtransfer.provider.kafka.v230.common;

import java.util.HashSet;
import java.util.Set;

public class Constants {
	
	public static final String CHARSET_FORMAT = "UTF-8";
	public static final String AUTH_TIME_OUT_MS_NAME = "auth.timeout.ms";

	public static final String GRAY_SIGN = "gray";

	public static final String ONLINE = "online";
	public static final String ONLINE_TO_OFFLINE_SIGN = "online.to.offline";

	/**
	 * message property: unit/cell/traffic group code key name
	 */
	public static final String MSG_PROP_ZONE_KEY = "cell";
	/**
	 * message property: trace id key name
	 */
	public static final String MSG_PROP_TID_KEY = "tid";
	/**
	 * message property: vendor id key name
	 */
	public static final String MSG_PROP_VID_KEY = "vid";
	/**
	 * message property: specific for RocketMQ, original RocketMQ message ID key
	 * name
	 */
	public static final String MSG_PROP_RMQ_OMID_KEY = "omid";

	public static final String MSG_PROP_FROM_ZONE_KEY = "_ZONE";

	public static final String REFERER_IP_KEY = "_IP";
	
	/**
     * Topic中每条消息来源，如 _REFERER: 物理集群id#topic名称 eg: mqtransfer_order#5|mqtransfer_order#3 表示消息经过的路径多个用'|'分隔
     */
    public static final String MSG_REFERER_HEADER = "_REFERER";

    public static final String MSG_REFERER_SEPARATOR = "#";

    public static final String MSG_REFERER_SOURCE_SEPARATOR = "\\|";

    public static final String MSG_REFERER_SOURCE_SEPARATOR_STR = "|";

	public static final Set<String> BUILT_IN_MSG_HEADERS;

	static {
		BUILT_IN_MSG_HEADERS = new HashSet<>();
		BUILT_IN_MSG_HEADERS.add(MSG_PROP_VID_KEY);
		BUILT_IN_MSG_HEADERS.add(MSG_PROP_FROM_ZONE_KEY);
		BUILT_IN_MSG_HEADERS.add(REFERER_IP_KEY);

	}
	/**
	 * http header key name
	 */
	public static final String HEADER_ZONE_NAME = "zone";
	public static final String HEADER_GROUP_NAME = "group";

	public static final Integer DEF_RATE_LIMITER_FAST_FAIL_SECS_VAL = 3;
	
	public static class CustomConsumer {
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
		/** 共享模式 */
		public static final String SHARE_MODE_KEY = "share.mode";
		/** 启用共享的最大大小 */
		public static final String SHARE_MAX_BYTES_KEY = "share.max.bytes";
		/** 启用共享的最大记录 */
		public static final String SHARE_MAX_RECORDS_KEY = "share.max.records";
		/** 结束共享的最小Lag */
		public static final String SHARE_END_MIN_LAG_KEY = "share.end.min.lag";
		/** 结束共享的小于最小延迟的持续时长 */
		public static final String SHARE_END_INTERVAL_SECOND_KEY = "share.end.interval.second";
		/** 结束共享的小于最小延迟的持续次数 */
		public static final String SHARE_END_INTERVAL_TIMES_KEY = "share.end.interval.times";
	}
	public static class CustomProducer {
		/** 批量生产最大条数 */
		public static final String MAX_BATCH_RECORDS_KEY = "max.batch.records";
		/** 批量生产最大大小，单位：字节 */
		public static final String MAX_BATCH_SIZE_KEY = "max.batch.size";
		/** 批量生产最大等待时间 */
		public static final String MAX_BATCH_WAIT_MS_KEY = "max.batch.wait.ms";
		/** 单条生产最大大小，单位：字节 */
		public static final String MAX_SINGLE_SIZE_KEY = "max.single.size";
		
		/** 分区分配策略，默认为平均（AVG_BY_CIRCLE） */
		public static final String PARTITION_ALLOCATE_STRATEGY = "partition.allocate.strategy";
		/** 分区分配配置，Map映射 */
		public static final String PARTITION_ALLOCATE_CONFIG = "partition.allocate.config";
	}

  public abstract static class KafkaConsumer {
	  
	  /************* public parameters after kafka ********/
	  public static final String GROUP_ID_NAME = "group.id";
	  public static final String BOOTSTRAP_SERVERS_NAME = "bootstrap.servers";
	  
      /************* specific parameters after kafka 2.0 ********/
	  
	  public static final String ZOOKEEPER_CONNECT_NAME = "zookeeper.connect";
	  public static final String CONSUMER_ID_NAME = "consumer.id"; //default: null
	  
	  public static final String REBALANCE_BACKOFF_MS_NAME = "rebalance.backoff.ms";  // default: 2000
      public static final String DEF_REBALANCE_BACKOFF_MS_VAL = "3000";

      public static final String MAX_PARTITION_FETCH_BYTES_NAME = "max.partition.fetch.bytes";
      public static final String MAX_PARTITION_FETCH_BYTES_VAL = "8388608"; //8M

      public static final String ALLOW_AUTO_CREATE_TOPICS_NAME = "allow.auto.create.topics";
      public static final String ALLOW_AUTO_CREATE_TOPICS_VAL = "false";

      public static final String ENABLE_AUTO_COMMIT_NAME = "enable.auto.commit";
      public static final String ENABLE_AUTO_COMMIT_VAL = "false";
      
      public static final String AUTO_OFFSET_RESET_CONFIG_NAME = "auto.offset.reset";
      public static final String DEF_AUTO_OFFSET_RESET_CONFIG_VAL = "latest";//earliest,latest
      public static final String LATEST_AUTO_OFFSET_RESET_VAL = "latest";
      public static final String EARLIEST_AUTO_OFFSET_RESET_VAL = "earliest";
      
      public static final String NUM_CONSUMERS_NAME = "num.consumers";
      public static final String DEF_NUM_CONSUMERS_VAL = "1";

      public static final String ISOLATION_LEVEL_NAME = "isolation.level";
      public static final String ISOLATION_LEVEL_VAL = "read_committed";

      public static final String CLIENT_RACK_NAME = "client.rack";
      
      public static final String MAX_POLL_RECORDS = "max.poll.records";
      public static final String DEF_MAX_POLL_RECORDS_VAL = "1000";
      
      public static final String FETCH_MAX_WAIT_MS_CONFIG = "fetch.max.wait.ms";
      public static final String DEF_FETCH_MAX_WAIT_MS_CONFIG_VAL = "500";

      public static final int AUTO_COMMIT_OFFSET_SIZE_NEW = 64;

      public static final String NEW_KEY_DESERIALIZER_NAME = "key.deserializer";
      public static final String DEF_KEY_NEW_DESERIALIZER_VAL = "org.apache.kafka.common.serialization.StringDeserializer";

      public static final String NEW_VALUE_DESERIALIZER_NAME = "value.deserializer";
      public static final String DEF_VALUE_NEW_DESERIALIZER_VAL = "org.apache.kafka.common.serialization.ByteArrayDeserializer";
      
  }
  
  public abstract static class KafkaProducer {
      public static final String BOOTSTRAP_SERVERS_NAME = "bootstrap.servers"; // broker list

      public static final String ACKS_NAME = "acks";  // default:1
      //public static final String DEF_ACKS_VAL = "-1";
      public static final String DEF_ACKS_VAL = "all";

      public static final String COMPRESSION_TYPE_NAME = "compression.type"; // none, gzip, snappy. default: none
      public static final String DEF_COMPRESSION_TYPE_VAL = "none";

      public static final String RETRIES_NAME = "retries"; // send fail retry, default: 0. none zero may cause message disorder and duplicate
      public static final String DEF_RETRIES_VAL = "3";

      public static final String CLINET_ID_NAME = "client.id"; // for metrics usage

      public static final String TIMEOUT_MS_NAME = "timeout.ms";  // leader server wait for follower sync. default: 30000
      public static final String DEF_TIMEOUT_MS_VAL = "10000";

      public static final String RECONNECT_BACKOFF_MS_NAME = "reconnect.backoff.ms"; // reconnect wait time. default: 10
      public static final String DEF_RECONNECT_BACKOFF_MS_VAL = "50";

      public static final String RETRY_BACKOFF_MS_NAME = "retry.backoff.ms"; // resend wait time. default: 100
      public static final String DEF_RETRY_BACKOFF_MS_VAL = "200";

      public static final String BATCH_SIZE_NAME = "batch.size";
      public static final int DEF_BATCH_SIZE_VAL = 2097152; // 2M，default is: 16384
      
      public static final String LINGER_MS_NAME = "linger.ms"; // default: 0
      public static final String DEF_LINGER_MS_VAL = "500";

      public static final String METADATA_MAX_AGE_MS = "metadata.max.age.ms";
      public static final String DEF_METADATA_MAX_AGE_MS_VAL = "60000";

      public static final String MAX_REQUEST_SIZE_NAME = "max.request.size";
      public static final int DEF_MAX_REQUEST_SIZE_VAL = 8388608; //8M;
      
      public static final String BUFFER_MEMORY_NAME = "buffer.memory";
      public static final int DEF_BUFFER_MEMORY_VAL = 33554432; //32M；

      //保持与kafka broker端${message.max.bytes}一致;
      //RecordTooLargeException[org.apache.kafka.common.errors.RecordTooLargeException:
      // The message is 1072970 bytes when serialized which is larger than the maximum request size
      // you have configured with the max.request.size configuration.],
      public static final String MAX_REQUEST_SIZE_VAL = "2097150";
      
      public static final String METADATA_FETCH_TIMEOUT_MS = "metadata.fetch.timeout.ms";
      public static final String DEF_METADATA_FETCH_TIMEOUT_MS_VAL = "2000";

      public static final String KEY_SERIALIZER_NAME = "key.serializer";
      public static final String DEF_KEY_SERIALIZER_VAL = "org.apache.kafka.common.serialization.StringSerializer";

      public static final String VALUE_SERIALIZER_NAME = "value.serializer";
      public static final String DEF_VALUE_SERIALIZER_VAL = "org.apache.kafka.common.serialization.ByteArraySerializer";
  }
  
  public static abstract class RocketProducer {
	  
	  public static final String NAMESVR_ADDR_KEY = "namesvr.addr";

      public static final String PRODUCER_GROUP_NAME = "producerGroup";
      public static final String DEF_PRODUCER_GROUP_VAL = "mqtransfer_default_producer_group";

      public static final String CLIENT_ID_NAME = "clientId";

      public static final String COMPRESS_MESSAGE_THRESHOLD_NAME = "compressMsgBodyOverHowmuch";
      public static final Integer DEF_COMPRESS_MESSAGE_THRESHOLD_VAL = 1024;  //1k

      public static final String TOPIC_QUEUE_NUMS_NAME = "defaultTopicQueueNums";
      public static final int DEF_TOPIC_QUEUE_NUMS_VAL = 2;

      public static final String SEND_MESSAGE_TIMEOUT_NAME = "sendMsgTimeout";
      public static final int DEF_SEND_MESSAGE_TIMEOUT_VAL = 30000;

      public static final String RETRY_TIMES_SEND_FAILED_NAME = "retryTimesWhenSendFailed";
      public static final int DEF_RETRY_TIMES_SEND_FAILED_VAL = 2;

      public static final String RETRY_ANOTHER_BROKER_SEND_FAILED_NAME = "retryAnotherBrokerWhenNotStoreOK";
      public static final boolean DEF_RETRY_ANOTHER_BROKER_SEND_FAILED_VAL = false;

      public static final String MAX_MESSAGE_SIZE_NAME = "maxMessageSize";
      public static final int DEF_MAX_MESSAGE_SIZE_VAL = 8388608;

      public static final String POLL_NAME_SERVER_INTERVAL_NAME = "pollNameServerInterval";
      public static final int DEF_POLL_NAME_SERVER_INTERVAL_VAL = 30 * 1000;

      public static final String HEARTBEAT_BROKER_INTERVAL_NAME = "heartbeatBrokerInterval";
      public static final int DEF_HEARTBEAT_BROKER_INTERVAL_VAL = 30 * 1000;
  }

  public static abstract class RocketConsumer {
	  
	  public static final String CONSUME_GROUP_KEY = "consume.group";

	  public static final String PRODUCE_GROUP_KEY = "produce.group";

	  public static final String CONSUMER_INSTANCE_NAME_KEY = "consumer.instance.name";

	  public static final String PRODUCER_INSTANCE_NAME_KEY = "producer.instance.name";

	  public static final String NAMESVR_ADDR_KEY = "namesvr.addr";

	  public static final String MAX_MESSAGE_SIZE_KEY = "max.message.size";

	  public static final String PULL_TIMEOUT = "pull.timeout.ms";
	  public static final int DEF_PULL_TIMEOUT_MS_VAL = 60000;
	  
      public static final String MAX_RECONSUME_TIMES_NAME = "maxReconsumeTimes";
      public static final Integer DEF_MAX_RECONSUME_TIMES_VAL = 5;

      public static final String CONSUME_FROM_WHERE_NAME = "consumeFromWhere";
      public static final String DEF_CONSUME_FROM_WHERE_VAL = "CONSUME_FROM_LAST_OFFSET";
      public static final String CONSUME_FROM_FIRST_VAL = "CONSUME_FROM_FIRST_OFFSET";
      public static final String CONSUME_FROM_TIMESTAMP_VAL = "CONSUME_FROM_TIMESTAMP";

      public static final String CONSUME_TIMESTAMP_NAME = "consumeTimestamp";

      public static final String CONSUME_THREAD_MIN_NAME = "consumeThreadMin";
      public static final String CONSUME_THREAD_MAX_NAME = "consumeThreadMax";

      public static final String CONSUME_CONCURRENTLY_MAX_SPAN_NAME = "consumeConcurrentlyMaxSpan";
      public static final String PULL_THRESHOLD_FOR_QUEUE_NAME = "pullThresholdForQueue";
      public static final String PULL_INTERVAL_NAME = "pullInterval";
      public static final String CONSUME_MESSAGE_BATCH_MAX_SIZE_NAME = "consumeMessageBatchMaxSize";
      public static final String PULL_BATCH_SIZE_NAME = "pullBatchSize";


      public static final String SUSPEND_CURRENT_QUEUE_TIMEMILLS_NAME = "suspendCurrentQueueTimeMillis";
      public static final String CONSUME_TIMEOUT_NAME = "consumeTimeout";

      public static final String POLL_NAME_SERVER_INTERVAL_NAME = "pollNameServerInterval";
      public static final String HEARTBEAT_BROKER_INTERVAL_NAME = "heartbeatBrokerInterval";
      public static final String PERSIST_OFFSET_INTERVAL_NAME = "persistConsumerOffsetInterval";

      // ** consumer related **
      public static final String MESSAGE_MODEL_NAME = "messageModel";
      public static final String MESSAGE_MODEL_CLUSTERING_NAME = "CLUSTERING";
      public static final String MESSAGE_MODEL_BROADCASTING_NAME = "BROADCASTING";
  }
}
