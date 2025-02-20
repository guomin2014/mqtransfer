package com.gm.mqtransfer.provider.rocketmq.v420.common;

public class Constants {
	
	/**
	 * message property: specific for RocketMQ, original RocketMQ message ID key
	 * name
	 */
	public static final String MSG_PROP_RMQ_OMID_KEY = "omid";
	
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
	      public static final String CONSUME_FROM_LAST_VAL = "CONSUME_FROM_LAST_OFFSET";
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
