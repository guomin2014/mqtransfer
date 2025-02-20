package com.gm.mqtransfer.provider.kafka.v230.service.producer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gm.mqtransfer.provider.facade.api.ResponseBuilder;
import com.gm.mqtransfer.provider.facade.api.ResponseCode;
import com.gm.mqtransfer.provider.facade.api.SubscribeData;
import com.gm.mqtransfer.provider.facade.api.producer.CheckMessageBatchRequest;
import com.gm.mqtransfer.provider.facade.api.producer.CheckMessageRequest;
import com.gm.mqtransfer.provider.facade.api.producer.CheckMessageResponse;
import com.gm.mqtransfer.provider.facade.api.producer.SendMessageAsyncRequest;
import com.gm.mqtransfer.provider.facade.api.producer.SendMessageAsyncResponse;
import com.gm.mqtransfer.provider.facade.api.producer.SendMessageRequest;
import com.gm.mqtransfer.provider.facade.api.producer.SendMessageResponse;
import com.gm.mqtransfer.provider.facade.api.producer.SendMessageResult;
import com.gm.mqtransfer.provider.facade.model.ClusterInfo;
import com.gm.mqtransfer.provider.facade.model.KafkaClusterInfo;
import com.gm.mqtransfer.provider.facade.model.MQMessage;
import com.gm.mqtransfer.provider.facade.model.ProducerClientConfig;
import com.gm.mqtransfer.provider.facade.model.ProducerPartition;
import com.gm.mqtransfer.provider.facade.service.delay.DefaultDelayedOperation;
import com.gm.mqtransfer.provider.facade.service.delay.DefaultDelayedOperationCallback;
import com.gm.mqtransfer.provider.facade.service.delay.DelayedOperationPurgatory;
import com.gm.mqtransfer.provider.facade.service.producer.AbstractProducerService;
import com.gm.mqtransfer.provider.facade.service.producer.ResponseFuture;
import com.gm.mqtransfer.provider.facade.service.producer.ResultMetadata;
import com.gm.mqtransfer.provider.facade.service.producer.SendCallback;
import com.gm.mqtransfer.provider.facade.util.StringUtils;
import com.gm.mqtransfer.provider.kafka.v230.common.Constants;

public class Kafka230ProducerService<T extends MQMessage> extends AbstractProducerService<T> {

	private final KafkaClusterInfo clusterInfo;
	private final Properties producerProps;
	private KafkaProducer<String, byte[]> producer;
	private int currentEpoch = 0;
	private boolean isReady = false;
	private long maxRequestSize;
	private long maxBatchSize;
	/** 初始化生产者锁 */
	private final ReentrantLock initProducerLock = new ReentrantLock();
	
	private final Logger logger = LoggerFactory.getLogger(Kafka230ProducerService.class);
	
	private DelayedOperationPurgatory<DefaultDelayedOperation> delayedOperationPurgatory;
	
	public Kafka230ProducerService(ProducerClientConfig config) {
		super(config);
		this.producerProps = config.getProducerProps();
		ClusterInfo clusterInfo = config.getCluster();
		if (clusterInfo instanceof KafkaClusterInfo) {
			this.clusterInfo = (KafkaClusterInfo)clusterInfo;
		} else {
			this.clusterInfo = (KafkaClusterInfo)clusterInfo.toPluginClusterInfo();
		}
		this.init();
		//添加一个超时执行线程任务(时间轮)，对生产进行超时处理
		delayedOperationPurgatory = new DelayedOperationPurgatory<>("Kafka230-Producer-" + config.getInstanceIndex());
	}
	private void init() {
		//根据2.3特性配置
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.BOOTSTRAP_SERVERS_NAME, clusterInfo.getBrokerList());
	  	fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.ACKS_NAME, Constants.KafkaProducer.DEF_ACKS_VAL);
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.COMPRESSION_TYPE_NAME, Constants.KafkaProducer.DEF_COMPRESSION_TYPE_VAL);
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.RETRIES_NAME, Constants.KafkaProducer.DEF_RETRIES_VAL);
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.CLINET_ID_NAME, this.generateInstanceName());
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.TIMEOUT_MS_NAME, Constants.KafkaProducer.DEF_TIMEOUT_MS_VAL);
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.RECONNECT_BACKOFF_MS_NAME, Constants.KafkaProducer.DEF_RECONNECT_BACKOFF_MS_VAL);
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.RETRY_BACKOFF_MS_NAME, Constants.KafkaProducer.DEF_RETRY_BACKOFF_MS_VAL);
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.BATCH_SIZE_NAME, Constants.KafkaProducer.DEF_BATCH_SIZE_VAL + "");
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.MAX_REQUEST_SIZE_NAME, Constants.KafkaProducer.DEF_MAX_REQUEST_SIZE_VAL + "");
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.LINGER_MS_NAME, Constants.KafkaProducer.DEF_LINGER_MS_VAL); // 500ms;
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.BUFFER_MEMORY_NAME, Constants.KafkaProducer.DEF_BUFFER_MEMORY_VAL + "");
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.METADATA_MAX_AGE_MS, Constants.KafkaProducer.DEF_METADATA_MAX_AGE_MS_VAL);
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.KEY_SERIALIZER_NAME, Constants.KafkaProducer.DEF_KEY_SERIALIZER_VAL);
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.VALUE_SERIALIZER_NAME, Constants.KafkaProducer.DEF_VALUE_SERIALIZER_VAL);
		
		//优先加载序列化类，避免底层使用线程的ClassLoader加载找不到类的情况
	  	if (producerProps.containsKey(Constants.KafkaProducer.KEY_SERIALIZER_NAME)) {
	  		Object keySerializer = producerProps.get(Constants.KafkaProducer.KEY_SERIALIZER_NAME);
	  		if (keySerializer instanceof String) {
	  			try {
	  				keySerializer = Class.forName(keySerializer.toString(), true, Utils.getKafkaClassLoader());
	  				producerProps.put(Constants.KafkaProducer.KEY_SERIALIZER_NAME, keySerializer);
	  			} catch (ClassNotFoundException e) {
	  	            throw new ConfigException(Constants.KafkaProducer.KEY_SERIALIZER_NAME, keySerializer, "Class " + keySerializer + " could not be found.");
	  	        }
	  		}
	  	} else {
	  		producerProps.put(Constants.KafkaProducer.KEY_SERIALIZER_NAME, StringDeserializer.class);
	  	}
	  	if (producerProps.containsKey(Constants.KafkaProducer.VALUE_SERIALIZER_NAME)) {
	  		Object valueSerializer = producerProps.get(Constants.KafkaProducer.VALUE_SERIALIZER_NAME);
	  		if (valueSerializer instanceof String) {
	  			try {
	  				valueSerializer = Class.forName(valueSerializer.toString(), true, Utils.getKafkaClassLoader());
	  				producerProps.put(Constants.KafkaProducer.VALUE_SERIALIZER_NAME, valueSerializer);
	  			} catch (ClassNotFoundException e) {
	  				throw new ConfigException(Constants.KafkaProducer.VALUE_SERIALIZER_NAME, valueSerializer, "Class " + valueSerializer + " could not be found.");
	  			}
	  		}
	  	} else {
	  		producerProps.put(Constants.KafkaProducer.VALUE_SERIALIZER_NAME, ByteArraySerializer.class);
	  	}
	  	
		String bootStrapSevers = producerProps.getProperty(Constants.KafkaProducer.BOOTSTRAP_SERVERS_NAME);
		if (StringUtils.isEmpty(bootStrapSevers)) {
			throw new IllegalArgumentException("the value of param '" + Constants.KafkaProducer.BOOTSTRAP_SERVERS_NAME + "' is null!");
		}
		this.maxRequestSize = this.getPropLongValueWithDefVal(producerProps, Constants.KafkaProducer.MAX_REQUEST_SIZE_NAME, Long.MAX_VALUE);
		this.maxBatchSize = this.getPropLongValueWithDefVal(producerProps, Constants.KafkaProducer.BATCH_SIZE_NAME, Long.MAX_VALUE);
	}
	@Override
	public void start() {
		logger.info("starting producer --> {}", producerProps);
		producer = new KafkaProducer<>(producerProps);
		this.isReady = true;
	}
	@Override
	public int restart(int epoch) {
		initProducerLock.lock();
		try {
			//多个线程共享客户端，所以只需要初始化一次
			if (epoch <= this.currentEpoch) {
				return -1;
			}
			logger.info("restarting producer --> {}", producerProps);
			this.stop();
			producer = new KafkaProducer<>(producerProps);
			this.currentEpoch++;
			return this.currentEpoch;
		} finally {
			initProducerLock.unlock();
		}
	}

	@Override
	public void stop() {
		initProducerLock.lock();
		try {
			logger.info("stopping producer --> {}", producerProps);
			if (producer != null) {
				try {
					producer.close(Duration.ofSeconds(30));
				} catch (Exception e) {
					logger.error("close producer error --> " + producerProps, e);
				}
			}
			this.isReady = false;
		} finally {
			initProducerLock.unlock();
		}
	}

	public ProducerRecord<String, byte[]> doConvert(ProducerPartition task, MQMessage message) {
		String topic = task.getToTopic();
        Integer toPartition = task.getToPartition();
        if (toPartition == null) {
        	return null;
        }
        //根据读取消息的分区，查找发送消息的分区
        List<Header> headers = new ArrayList<>();
        try {
	        Map<String, String> headerMap = message.getHeaders();
	        if (headerMap != null && headerMap.size() > 0) {
	            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
	            	headers.add(new RecordHeader(entry.getKey(), entry.getValue().getBytes(Constants.CHARSET_FORMAT)));
	            }
	        }
        } catch (Exception e) {}
        return new ProducerRecord<String, byte[]>(topic, toPartition, message.getMessageKey(), message.getMessageBytes(), headers);
	}

	@Override
	public int getCurrentEpoch() {
		return this.currentEpoch;
	}

	@Override
	public boolean isReady() {
		return this.isReady;
	}

	@Override
	public CheckMessageResponse checkMessage(CheckMessageRequest<T> request) {
		MQMessage message = request.getMessage();
		if (message == null || message.getMessageBytes() == null) {
			return ResponseBuilder.toBuild(CheckMessageResponse.class, ResponseCode.MessageBodyEmptyError);
		}
		return message.getMessageBytes().length <= this.maxRequestSize ? 
				ResponseBuilder.toBuild(CheckMessageResponse.class, new SubscribeData()) : ResponseBuilder.toBuild(CheckMessageResponse.class, ResponseCode.MessageBodyOutOfRangeError);
	}

	@Override
	public CheckMessageResponse checkMessage(CheckMessageBatchRequest<T> request) {
		List<T> messages = request.getMessageList();
		if (messages == null || messages.isEmpty()) {
			return ResponseBuilder.toBuild(CheckMessageResponse.class, ResponseCode.MessageBodyEmptyError);
		}
		long total = messages.stream().mapToLong(m -> m.getMessageBytes() != null ? m.getMessageBytes().length : 0).sum();
		return total <= this.maxBatchSize ? 
				ResponseBuilder.toBuild(CheckMessageResponse.class, new SubscribeData()) : ResponseBuilder.toBuild(CheckMessageResponse.class, ResponseCode.MessageBodyOutOfRangeError);
	}
	@Override
	public SendMessageResponse<T> sendMessage(SendMessageRequest<T> request) {
		ProducerPartition partition = request.getPartition();
		List<T> messages = request.getMessageList();
		Long sendTimeoutMs = request.getSendTimeoutMs();
		if (partition == null) {
			return ResponseBuilder.toBuild(SendMessageResponse.class, ResponseCode.PartitionEmptyError);
		}
		if (messages == null || messages.isEmpty()) {
			return ResponseBuilder.toBuild(SendMessageResponse.class, ResponseCode.MessageBodyEmptyError);
		}
		if (sendTimeoutMs == null || sendTimeoutMs.longValue() <= 0) {
			sendTimeoutMs = 3000L;
		}
		ResponseFuture<T> responseFuture = new ResponseFuture<>(partition, messages, sendTimeoutMs, null);
		SendCallback<T> sendCallback = new SendCallback<T>() {
			@Override
			public void onCompletion(ProducerPartition task, SendMessageResult<T> sendResult) {
				responseFuture.putResponse(sendResult);
			}
		};
		SendMessageAsyncResponse<T> asyncResponse = this.sendMessage(new SendMessageAsyncRequest<T>(partition, messages, sendTimeoutMs, sendCallback));
		try {
			responseFuture.waitResponse();
		} catch (InterruptedException e) {
			responseFuture.putResponse(asyncResponse.getData());//已发送的部份
		}
		responseFuture.onCompletion();
		SendMessageResult<T> result = responseFuture.getResponse();
		logger.info("[{}] send message to kafka success-->offset[{}],success[{}],failure:[{}]", 
				partition.getToDesc(), result.getLastSuccResult() != null ? result.getLastSuccResult().getOffset() : "", 
				result.getSuccCount(), result.getErrorCount());
		return ResponseBuilder.toBuild(SendMessageResponse.class, result);
	}
	@Override
	public SendMessageAsyncResponse<T> sendMessage(SendMessageAsyncRequest<T> request) {
		ProducerPartition partition = request.getPartition();
		SendCallback<T> callback = request.getCallback();
		List<T> messages = request.getMessages();
		Long sendTimeoutMs = request.getSendTimeoutMs();
		if (partition == null) {
			return ResponseBuilder.toBuild(SendMessageAsyncResponse.class, ResponseCode.PartitionEmptyError);
		}
		if (messages == null || messages.isEmpty()) {
			return ResponseBuilder.toBuild(SendMessageAsyncResponse.class, ResponseCode.MessageBodyEmptyError);
		}
		if (sendTimeoutMs == null || sendTimeoutMs.longValue() <= 0) {
			sendTimeoutMs = 3000L;
		}
		ResponseFuture<T> responseFuture = new ResponseFuture<>(partition, messages, sendTimeoutMs, callback);
		String lastErrorMsg = null;
		for (T message : messages) {
			ProducerRecord<String, byte[]> record = this.doConvert(partition, message);
			if (record == null) {
				lastErrorMsg = ResponseCode.MessageConvertError.getMsg();
				responseFuture.putResponse(message, new ResultMetadata(ResponseCode.MessageConvertError.name(), lastErrorMsg));
				delayedOperationPurgatory.checkAndComplete(partition);
				continue;
			}
			if (!responseFuture.isExistsError()) {//没有错误
				try {
					//消息发送，API异常，通过callback回调，其它异常，直接抛出
					producer.send(record, new Callback() {
						public void onCompletion(RecordMetadata metadata, Exception exception) {
							if (exception == null) {
//								sendResult.addSuccRecord(message, metadata.offset());
								responseFuture.putResponse(message, ResultMetadata.createSuccessResult(metadata.offset(), null));
							} else if (exception instanceof RecordTooLargeException) {//造成该异常的消息可丢弃，直接发送下一条消息
								logger.error("[{}] async send message to kafka v23 failure-->key[{}],reason:{}", partition.getToDesc(), record.key(), exception.getMessage());
								responseFuture.putResponse(message, new ResultMetadata(ResponseCode.MessageBodyOutOfRangeError.name(), exception.getMessage()));
							} else {//出现其它异常，都不能在继续发送消息，直接结束，避免当前消息发送失败而后继消息发送成功造成消息乱序
								logger.error("[{}] async send message to kafka v23 failure-->key[{}],reason:{}", partition.getToDesc(), record.key(), exception.getMessage());
								responseFuture.putResponse(message, new ResultMetadata(ResponseCode.OtherError.name(), exception.getMessage()));
							}
							delayedOperationPurgatory.checkAndComplete(partition);
						}
					});
				} catch (Throwable e) {
					logger.error("[{}] async send message to kafka v23 failure-->key[{}],message:{}", partition.getToDesc(), record.topic(), record.key(), e.getMessage());
					responseFuture.putResponse(message, new ResultMetadata(ResponseCode.OtherError.name(), e.getMessage()));
					delayedOperationPurgatory.checkAndComplete(partition);
					lastErrorMsg = e.getMessage();
				}
			} else {//存在错误，不在继续发送（对于sdk-client错误处理，并未真实发送）
				responseFuture.putResponse(message, new ResultMetadata(ResponseCode.OtherError.name(), lastErrorMsg != null ? lastErrorMsg : "Previous record send error"));
				delayedOperationPurgatory.checkAndComplete(partition);
			}
		}
		DefaultDelayedOperation delayedProducer = new DefaultDelayedOperation(sendTimeoutMs, new DefaultDelayedOperationCallback<T>(responseFuture));
		Queue<Object> watchKeys = new LinkedList<>();
		watchKeys.add(partition);
		delayedOperationPurgatory.tryCompleteElseWatch(delayedProducer, watchKeys);
		return ResponseBuilder.toBuild(SendMessageAsyncResponse.class, responseFuture.getResponse());
	}
	
}
