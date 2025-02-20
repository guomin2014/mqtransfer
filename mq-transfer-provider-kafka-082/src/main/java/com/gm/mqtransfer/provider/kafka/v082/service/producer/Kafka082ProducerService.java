package com.gm.mqtransfer.provider.kafka.v082.service.producer;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.RecordTooLargeException;

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
import com.gm.mqtransfer.provider.kafka.v082.common.Constants;
import com.gm.mqtransfer.provider.kafka.v082.service.consumer.MessageId;

public class Kafka082ProducerService<T extends MQMessage> extends AbstractProducerService<T> {

	private DelayedOperationPurgatory<DefaultDelayedOperation> delayedOperationPurgatory;
	private final KafkaClusterInfo clusterInfo;
	private final Properties producerProps;
	private KafkaProducer<String, byte[]> producer;
	private int currentEpoch = 0;
	private boolean isReady = false;
	private long maxRequestSize;
	private long maxBatchSize;
	/** 初始化生产者锁 */
	private final ReentrantLock initProducerLock = new ReentrantLock();
	
	public Kafka082ProducerService(ProducerClientConfig config) {
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
		delayedOperationPurgatory = new DelayedOperationPurgatory<>("Kafka082-Producer-" + config.getInstanceIndex());
	}
	
	private void init() {
		//根据0.8特性配置
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.BOOTSTRAP_SERVERS_NAME, clusterInfo.getBrokerList());
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.ACKS_NAME, Constants.KafkaProducer.DEF_ACKS_VAL);
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.COMPRESSION_TYPE_NAME, Constants.KafkaProducer.DEF_COMPRESSION_TYPE_VAL);
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.RETRIES_NAME, Constants.KafkaProducer.DEF_RETRIES_VAL);
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.CLINET_ID_NAME, generateInstanceName());
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.TIMEOUT_MS_NAME, Constants.KafkaProducer.DEF_TIMEOUT_MS_VAL);
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.RECONNECT_BACKOFF_MS_NAME, Constants.KafkaProducer.DEF_RECONNECT_BACKOFF_MS_VAL);
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.RETRY_BACKOFF_MS_NAME, Constants.KafkaProducer.DEF_RETRY_BACKOFF_MS_VAL);
		//这个设置是为发送设置一定是延迟来收集更多的消息，默认大小是0ms（就是有消息就立即发送）
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.LINGER_MS_NAME, Constants.KafkaProducer.DEF_LINGER_MS_VAL);
		//设置批量提交的数据大小，默认是16k,当积压的消息达到这个值的时候就会统一发送（发往同一分区的消息）
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.BATCH_SIZE_NAME, Constants.KafkaProducer.DEF_BATCH_SIZE_VAL + "");
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.MAX_REQUEST_SIZE_NAME, Constants.KafkaProducer.DEF_MAX_REQUEST_SIZE_VAL + "");
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.BUFFER_MEMORY_NAME, Constants.KafkaProducer.DEF_BUFFER_MEMORY_VAL + "");
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.METADATA_FETCH_TIMEOUT_MS, Constants.KafkaProducer.DEF_METADATA_FETCH_TIMEOUT_MS_VAL);
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.KEY_SERIALIZER_NAME, Constants.KafkaProducer.DEF_KEY_SERIALIZER_VAL);
		fillEmptyPropWithDefVal(producerProps, Constants.KafkaProducer.VALUE_SERIALIZER_NAME, Constants.KafkaProducer.DEF_VALUE_SERIALIZER_VAL);
		
		try {
			Object obj = producerProps.get(Constants.KafkaProducer.MAX_REQUEST_SIZE_NAME);
			if (obj != null) {
				this.maxRequestSize = Long.parseLong(obj.toString());
			}
		} catch (Exception e) {}
		try {
			Object batchSizeObj = producerProps.get(Constants.KafkaProducer.BATCH_SIZE_NAME);
			if (batchSizeObj != null) {
				this.maxBatchSize = Long.parseLong(batchSizeObj.toString());
			}
		} catch (Exception e) {}
	}
	
	@Override
	public void start() {
		logger.info("starting producer config --> {}", producerProps);
		producer = new KafkaProducer<>(producerProps);
		this.isReady = true;
	}

	@Override
	public void stop() {
		if (producer != null) {
			this.isReady = false;
			try {
				//低版本的kafka客户端在关闭时不能设置超时时间，某些情况将导致线程阻塞，一直卡到此处
				producer.close();
			} catch (Exception e) {
				logger.error("close producer failure", e);
			}
		}
	}

	@Override
	public int restart(int epoch) {
		initProducerLock.lock();
		try {
			//多个线程共享客户端，所以只需要初始化一次
			if (epoch <= this.currentEpoch) {
				return 0;
			}
			logger.info("restarting producer config --> {}", producerProps);
			this.stop();
			producer = new KafkaProducer<>(producerProps);
			this.isReady = true;
		} finally {
			initProducerLock.unlock();
		}
		return 1;
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
			try {
				ProducerRecord<String, byte[]> record = this.doConvert(partition, message);
				if (record == null) {
					lastErrorMsg = ResponseCode.MessageConvertError.getMsg();
					responseFuture.putResponse(message, new ResultMetadata(ResponseCode.MessageConvertError.name(), lastErrorMsg));
					continue;
				}
				if (!responseFuture.isExistsError()) {//没有错误
					try {
						//消息发送，API异常，通过callback回调，其它异常，直接抛出
						producer.send(record, new Callback() {
							public void onCompletion(RecordMetadata metadata, Exception exception) {
								if (exception == null) {
									responseFuture.putResponse(message, ResultMetadata.createSuccessResult(metadata.offset(), null));
								} else if (exception instanceof RecordTooLargeException) {//造成该异常的消息可丢弃，直接发送下一条消息
									logger.error("[{}] async send message to kafka v08 failure-->key[{}],reason:{}", partition.getToDesc(), record.key(), exception.getMessage());
									responseFuture.putResponse(message, new ResultMetadata(ResponseCode.MessageBodyOutOfRangeError.name(), exception.getMessage()));
								} else {//出现其它异常，都不能在继续发送消息，直接结束，避免当前消息发送失败而后继消息发送成功造成消息乱序
									logger.error("[{}] async send message to kafka v08 failure-->key[{}],reason:{}", partition.getToDesc(), record.key(), exception.getMessage());
									responseFuture.putResponse(message, new ResultMetadata(ResponseCode.OtherError.name(), exception.getMessage()));
								}
								delayedOperationPurgatory.checkAndComplete(partition);
							}
						});
					} catch (Throwable e) {
						lastErrorMsg = e.getMessage();
						if (StringUtils.isBlank(lastErrorMsg)) {
							lastErrorMsg = e.getClass().getSimpleName();
						}
						logger.error("[{}] async send message to kafka v08 failure-->key[{}],message:{}", partition.getToDesc(), record.topic(), record.key(), lastErrorMsg);
						responseFuture.putResponse(message, new ResultMetadata(ResponseCode.OtherError.name(), lastErrorMsg));
					}
				} else {//存在错误，不在继续发送（对于sdk-client错误处理，并未真实发送）
					responseFuture.putResponse(message, new ResultMetadata(ResponseCode.OtherError.name(), lastErrorMsg != null ? lastErrorMsg : "Previous record send error"));
				}
			} finally {
				delayedOperationPurgatory.checkAndComplete(partition);
			}
		}
		DefaultDelayedOperation delayedProducer = new DefaultDelayedOperation(sendTimeoutMs, new DefaultDelayedOperationCallback<T>(responseFuture));
		Queue<Object> watchKeys = new LinkedList<>();
		watchKeys.add(partition);
		delayedOperationPurgatory.tryCompleteElseWatch(delayedProducer, watchKeys);
		return ResponseBuilder.toBuild(SendMessageAsyncResponse.class, responseFuture.getResponse());
	}
	
	public ProducerRecord<String, byte[]> doConvert(ProducerPartition task, T message) {
		String topic = task.getToTopic();
        Integer toPartition = task.getToPartition();
        if (toPartition == null) {
//        	throw new BusinessException("can not find producer partition");
        	return null;
        }
        //String msgId = tm.getMessageId();
        MessageId mid = MessageId.create()
                .realMessageId(message.getMessageId())
                .props(message.getHeaders())
                .build();
        //根据读取消息的分区，查找发送消息的分区
        return new org.apache.kafka.clients.producer.ProducerRecord<>(topic, toPartition, mid.getPackMessageId(), message.getMessageBytes());
	}
	
}
