package com.gm.mqtransfer.provider.rocketmq.v420.service.producer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.rocketmq.client.Validators;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageBatch;
import org.apache.rocketmq.common.message.MessageClientIDSetter;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageQueue;

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
import com.gm.mqtransfer.provider.facade.model.MQMessage;
import com.gm.mqtransfer.provider.facade.model.ProducerClientConfig;
import com.gm.mqtransfer.provider.facade.model.ProducerPartition;
import com.gm.mqtransfer.provider.facade.model.RocketMQClusterInfo;
import com.gm.mqtransfer.provider.facade.service.producer.AbstractProducerService;
import com.gm.mqtransfer.provider.facade.service.producer.ResponseFuture;
import com.gm.mqtransfer.provider.facade.service.producer.ResultMetadata;
import com.gm.mqtransfer.provider.facade.service.producer.SendCallback;
import com.gm.mqtransfer.provider.facade.util.StringUtils;
import com.gm.mqtransfer.provider.rocketmq.v420.common.Constants;
import com.gm.mqtransfer.provider.rocketmq.v420.common.RktMQMessage;

public class RocketMQ420ProducerService<T extends MQMessage> extends AbstractProducerService<T> {

	private final Properties producerProps;
	private final RocketMQClusterInfo clusterInfo;
	private int currentEpoch = 0;
	private boolean isReady = false;
	private long maxRequestSize;
	private long maxBatchSize;
	private DefaultMQProducer producer;
	/** 初始化生产者锁 */
	private final ReentrantLock initProducerLock = new ReentrantLock();
	
	public RocketMQ420ProducerService(ProducerClientConfig config) {
		super(config);
		this.producerProps = config.getProducerProps();
		ClusterInfo clusterInfo = config.getCluster();
		if (clusterInfo instanceof RocketMQClusterInfo) {
			this.clusterInfo = (RocketMQClusterInfo)clusterInfo;
		} else {
			this.clusterInfo = (RocketMQClusterInfo)clusterInfo.toPluginClusterInfo();
		}
		this.init();
	}
	
	private void init() {
		fillEmptyPropWithDefVal(producerProps, Constants.RocketConsumer.NAMESVR_ADDR_KEY, clusterInfo.getNameSvrAddr());
		fillEmptyPropWithDefVal(producerProps, Constants.RocketProducer.CLIENT_ID_NAME, generateInstanceName());
		fillEmptyPropWithDefVal(producerProps, Constants.RocketProducer.PRODUCER_GROUP_NAME, generateProducerGroup());
		fillEmptyPropWithDefVal(producerProps, Constants.RocketProducer.COMPRESS_MESSAGE_THRESHOLD_NAME, Constants.RocketProducer.DEF_COMPRESS_MESSAGE_THRESHOLD_VAL.toString());
		fillEmptyPropWithDefVal(producerProps, Constants.RocketProducer.TOPIC_QUEUE_NUMS_NAME, Constants.RocketProducer.DEF_TOPIC_QUEUE_NUMS_VAL + "");
		fillEmptyPropWithDefVal(producerProps, Constants.RocketProducer.RETRY_TIMES_SEND_FAILED_NAME, Constants.RocketProducer.DEF_RETRY_TIMES_SEND_FAILED_VAL + "");
		fillEmptyPropWithDefVal(producerProps, Constants.RocketProducer.RETRY_ANOTHER_BROKER_SEND_FAILED_NAME, Constants.RocketProducer.DEF_RETRY_ANOTHER_BROKER_SEND_FAILED_VAL + "");
		fillEmptyPropWithDefVal(producerProps, Constants.RocketProducer.MAX_MESSAGE_SIZE_NAME, Constants.RocketProducer.DEF_MAX_MESSAGE_SIZE_VAL + "");
		fillEmptyPropWithDefVal(producerProps, Constants.RocketProducer.POLL_NAME_SERVER_INTERVAL_NAME, Constants.RocketProducer.DEF_POLL_NAME_SERVER_INTERVAL_VAL + "");
		fillEmptyPropWithDefVal(producerProps, Constants.RocketProducer.HEARTBEAT_BROKER_INTERVAL_NAME, Constants.RocketProducer.DEF_HEARTBEAT_BROKER_INTERVAL_VAL + "");
		fillEmptyPropWithDefVal(producerProps, Constants.RocketProducer.SEND_MESSAGE_TIMEOUT_NAME, Constants.RocketProducer.DEF_SEND_MESSAGE_TIMEOUT_VAL + "");
		
		String nameServerAddr = producerProps.getProperty(Constants.RocketProducer.NAMESVR_ADDR_KEY);
		if (StringUtils.isBlank(nameServerAddr)) {
			throw new IllegalArgumentException("the value of param 'nameServerAddr' is null!");
		}
	}
	
	@Override
	public void start() {
		try {
			logger.info("starting producer --> {}", producerProps);
			producer  = new DefaultMQProducer(getPropStringValueWithDefVal(producerProps, Constants.RocketProducer.PRODUCER_GROUP_NAME, generateProducerGroup()));
	        producer.setNamesrvAddr(producerProps.getProperty(Constants.RocketProducer.NAMESVR_ADDR_KEY));
	        // 消息 Body 超过多大开始压缩(Consumer 收到消息会自动解压缩),单位字节。默认: 1024 * 4
	        producer.setCompressMsgBodyOverHowmuch(getPropIntValueWithDefVal(producerProps, Constants.RocketProducer.COMPRESS_MESSAGE_THRESHOLD_NAME, Constants.RocketProducer.DEF_COMPRESS_MESSAGE_THRESHOLD_VAL));
	        // 在发送消息时,自动创建服务器不存在的 topic,默认创建的队列数。默认:4
	        producer.setDefaultTopicQueueNums(getPropIntValueWithDefVal(producerProps, Constants.RocketProducer.TOPIC_QUEUE_NUMS_NAME, Constants.RocketProducer.DEF_TOPIC_QUEUE_NUMS_VAL));
	        // 同步模式,发送失败重试次数。默认:2
	        producer.setRetryTimesWhenSendFailed(getPropIntValueWithDefVal(producerProps, Constants.RocketProducer.RETRY_TIMES_SEND_FAILED_NAME, Constants.RocketProducer.DEF_RETRY_TIMES_SEND_FAILED_VAL));
	        // 发送失败是否重试其他broker,默认:false
	        producer.setRetryAnotherBrokerWhenNotStoreOK(getPropBooleanValueWithDefVal(producerProps, Constants.RocketProducer.RETRY_ANOTHER_BROKER_SEND_FAILED_NAME, Constants.RocketProducer.DEF_RETRY_ANOTHER_BROKER_SEND_FAILED_VAL));
	        // 最大消息size, byte。默认:8M跟 broker保持一致;
	        producer.setMaxMessageSize(getPropIntValueWithDefVal(producerProps, Constants.RocketProducer.MAX_MESSAGE_SIZE_NAME, Constants.RocketProducer.DEF_MAX_MESSAGE_SIZE_VAL));
	        // name server 路由更新频率, ms。默认:30*1000
	        producer.setPollNameServerInterval(getPropIntValueWithDefVal(producerProps, Constants.RocketProducer.POLL_NAME_SERVER_INTERVAL_NAME, Constants.RocketProducer.DEF_POLL_NAME_SERVER_INTERVAL_VAL));
	        // 心跳上报频率, ms。默认:30 * 1000
	        producer.setHeartbeatBrokerInterval(getPropIntValueWithDefVal(producerProps, Constants.RocketProducer.HEARTBEAT_BROKER_INTERVAL_NAME, Constants.RocketProducer.DEF_HEARTBEAT_BROKER_INTERVAL_VAL));
	        //发送消息超时时间
	        producer.setSendMsgTimeout(getPropIntValueWithDefVal(producerProps, Constants.RocketProducer.SEND_MESSAGE_TIMEOUT_NAME, Constants.RocketProducer.DEF_SEND_MESSAGE_TIMEOUT_VAL));
	        producer.setInstanceName(getPropStringValueWithDefVal(producerProps, Constants.RocketProducer.CLIENT_ID_NAME, generateInstanceName()));
        	producer.start();
        	isReady = true;
        	this.maxRequestSize = producer.getMaxMessageSize();
        	this.maxBatchSize = producer.getMaxMessageSize();
        	logger.info("start producer successes-->" + producerProps + "-->" + producer.toString());
        } catch (Exception e) {
        	logger.error("start producer error-->" + producerProps, e);
        	isReady = false;
        }
	}

	@Override
	public void stop() {
		isReady = false;
		if (producer != null) {
			try {
				producer.shutdown();
			} catch (Exception e) {}
		}
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
			this.start();
			this.currentEpoch++;
			return this.currentEpoch;
		} finally {
			initProducerLock.unlock();
		}
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
		T message = request.getMessage();
		if (message == null || message.getMessageBytes() == null) {
			return ResponseBuilder.toBuild(CheckMessageResponse.class, ResponseCode.MessageBodyEmptyError);
		}
		List<T> messages = new ArrayList<>();
		messages.add(message);
		long total = this.sizeOfMessages(messages);
		return total <= this.maxRequestSize ? 
				ResponseBuilder.toBuild(CheckMessageResponse.class, new SubscribeData()) : ResponseBuilder.toBuild(CheckMessageResponse.class, ResponseCode.MessageBodyOutOfRangeError);
	}

	@Override
	public CheckMessageResponse checkMessage(CheckMessageBatchRequest<T> request) {
		List<T> messages = request.getMessageList();
		if (messages == null || messages.isEmpty()) {
			return ResponseBuilder.toBuild(CheckMessageResponse.class, ResponseCode.MessageBodyEmptyError);
		}
		long total = this.sizeOfMessages(messages);
		return total <= this.maxBatchSize ? 
				ResponseBuilder.toBuild(CheckMessageResponse.class, new SubscribeData()) : ResponseBuilder.toBuild(CheckMessageResponse.class, ResponseCode.MessageBodyOutOfRangeError);
	}
	
	private long sizeOfMessages(List<T> messages) {
		if (messages.size() == 1) {
			T msg = messages.get(0);
			return msg == null || msg.getMessageBytes() == null ? 0 : msg.getMessageBytes().length;
		}
		// 消息体的长度 + 属性(propertiesLen, short类型)最大长度 + topic最大的长度
		// return msgs.stream().mapToInt(m -> m.getBody().length).sum() + 2^16 +
		// Validators.CHARACTER_MAX_LENGTH ;
		int messageInnerStructSize = 100 * 4; // 消息内部结构大小，参见：MessageDecoder#encodeMessage及CommitLog$MessageExtBatchEncoder#calMsgLength
		long totalLen = messages.stream().mapToLong(m -> m.getMessageBytes() != null ? m.getMessageBytes().length : 0).sum();
		return totalLen + ((2 ^ 16) + messageInnerStructSize) * messages.size();
	}
	public Message convertMessage(ProducerPartition partition, T message) {
		Message msg = new Message();
		msg.setBody(message.getMessageBytes());
		msg.setTopic(partition.getToTopic());
        if (message.getMessageKey() != null) {
        	msg.setKeys(message.getMessageKey());
        }
        if (message instanceof RktMQMessage) {
        	RktMQMessage rktMessage = (RktMQMessage)message;
        	if (rktMessage.getTags() != null) {
            	msg.setTags(rktMessage.getTags());
            }
        }
        if (message.getHeaders() != null && message.getHeaders().size() > 0) {
            for (String key: message.getHeaders().keySet()) {
                if (!MessageConst.STRING_HASH_SET.contains(key)) {
                	msg.putUserProperty(key, message.getHeaders().get(key));
                }
            }
        }
        setOriginMessageIdIfNeed(msg, message);
        setUniqKeyIfNeed(msg, message);
        return msg;
	}
	public List<Message> convertMessage(ProducerPartition partition, List<T> messages) {
		List<Message> retList = new ArrayList<>();
		for (T message : messages) {
			retList.add(this.convertMessage(partition, message));
		}
		return retList;
	}
	private void setOriginMessageIdIfNeed(Message tm, T message) {
        Map<String, String> headers = tm.getProperties();
        if ((headers != null && headers.containsKey(Constants.MSG_PROP_RMQ_OMID_KEY))) {
            return;
        } else {
            if (StringUtils.isNotBlank(message.getMessageId()))
                tm.putUserProperty(Constants.MSG_PROP_RMQ_OMID_KEY, message.getMessageId());
        }
    }
	/**
	 * 设置消息唯一key，保证转发消息的唯一key不会变
	 * @param tm
	 * @param message
	 * @return
	 */
	private Message setUniqKeyIfNeed(Message tm, T message) {
	    try {
	        Method method = tm.getClass().getDeclaredMethod("putProperty", String.class, String.class);
	        method.setAccessible(true);
	        method.invoke(tm, MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX, message.getMessageId());
	        return tm;
	    } catch (Exception e) {
	        logger.error("flush message set msgid has error.cause by:{}.", e.getMessage());
	    }
	    return null;
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
			sendTimeoutMs = 3000000L;
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
		return ResponseBuilder.toBuild(SendMessageResponse.class, result);
	}
	
	private MessageBatch batch(Collection<Message> msgs) throws MQClientException {
        MessageBatch msgBatch;
        try {
            msgBatch = MessageBatch.generateFromList(msgs);
            for (Message message : msgBatch) {
                Validators.checkMessage(message, producer);
                MessageClientIDSetter.setUniqID(message);
            }
            msgBatch.setBody(msgBatch.encode());
        } catch (Exception e) {
            throw new MQClientException("Failed to initiate the MessageBatch", e);
        }
        return msgBatch;
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
			sendTimeoutMs = 30000L;
		}
		MessageQueue mq = new MessageQueue(partition.getToTopic(), partition.getToBrokerName(), partition.getToPartition());
		ResponseFuture<T> responseFuture = new ResponseFuture<>(partition, messages, sendTimeoutMs, callback);
		try {
			List<Message> sendMessages = this.convertMessage(partition, messages);
			Message message = null;
			if (sendMessages.size() == 1) {
				message = sendMessages.get(0);//单条记录使用该方法可启用压缩
			} else {
				message = this.batch(sendMessages);
			}
			producer.send(message, mq, new org.apache.rocketmq.client.producer.SendCallback() {
				@Override
				public void onSuccess(SendResult sendResult) {
					long offset = sendResult.getQueueOffset();
					String msgId = sendResult.getMsgId();
					String offsetMsgId = sendResult.getOffsetMsgId();
					if (logger.isDebugEnabled()) {
						logger.debug("[{}] send message to rocketmq success-->count[{}],offset[{}],msgId[{}]", partition.getToDesc(), messages.size(), offset, msgId);
					}
//					String[] msgIds = org.apache.commons.lang3.StringUtils.split(msgId, ",");
					String[] offsetMsgIds = org.apache.commons.lang3.StringUtils.split(offsetMsgId, ",");
					for (int index = 0; index < messages.size(); index++) {
						T m = messages.get(index);
//						Long offset = Long.parseLong(msgIds[index]);
						String messageId = offsetMsgIds[index];
						responseFuture.putResponse(m, ResultMetadata.createSuccessResult(offset - messages.size() + index, messageId));
					}
					if (callback != null) {
						callback.onCompletion(partition, responseFuture.getResponse());
					}
				}
				@Override
				public void onException(Throwable e) {
					logger.error("[{}] async send message to rocketmq failure-->count[{}],message:{}", partition.getToDesc(), messages.size(), e.getMessage());
					//异步回调的异常，需要对异常结果进行处理(比如：批量发送，在broker会返回body长度超长，需要进行拆分重新发送)
					handlerException(partition, messages, responseFuture, e);
					if (callback != null) {
						callback.onCompletion(partition, responseFuture.getResponse());
					}
				}
			});
		} catch (Exception e) {
			
		}
		return ResponseBuilder.toBuild(SendMessageAsyncResponse.class, responseFuture.getResponse());
	}
	
	private void handlerException(ProducerPartition partition, List<T> messages, ResponseFuture<T> responseFuture, Throwable e) {
		String errMsg = e.getMessage();
		if (StringUtils.isBlank(errMsg)) {
			errMsg = "unknow exception";
		}
		if (errMsg.indexOf("PutMessages topic length too long") >= 0) {//topic超长，全部丢弃
			for (T message : messages) {
				responseFuture.putResponse(message, new ResultMetadata(ResponseCode.OtherError.name(), "topic length too long"));
			}
		} else if (errMsg.indexOf("the message body size over max value") >= 0 
				|| errMsg.indexOf("maybe msg body or properties length not matched") >= 0
				|| errMsg.indexOf("message size exceeded") >= 0
				) {//body超长，判断消息数量，拆分后重新发送
			for (T message : messages) {
				responseFuture.putResponse(message, new ResultMetadata(ResponseCode.MessageBodyOutOfRangeError.name(), ResponseCode.MessageBodyOutOfRangeError.getMsg()));
			}
		} else {//其它异常，需要重发
			for (T message : messages) {
				responseFuture.putResponse(message, new ResultMetadata(ResponseCode.OtherError.name(), errMsg));
			}
		}
	}

}
