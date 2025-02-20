package com.gm.mqtransfer.provider.rocketmq.v420.service.consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.consumer.PullStatus;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.heartbeat.SubscriptionData;

import com.gm.mqtransfer.provider.facade.api.CommitOffsetRequest;
import com.gm.mqtransfer.provider.facade.api.CommitOffsetResponse;
import com.gm.mqtransfer.provider.facade.api.FetchOffsetData;
import com.gm.mqtransfer.provider.facade.api.FetchOffsetRangeData;
import com.gm.mqtransfer.provider.facade.api.FetchOffsetRangeRequest;
import com.gm.mqtransfer.provider.facade.api.FetchOffsetRangeResponse;
import com.gm.mqtransfer.provider.facade.api.FetchOffsetRequest;
import com.gm.mqtransfer.provider.facade.api.FetchOffsetResponse;
import com.gm.mqtransfer.provider.facade.api.PollMessageData;
import com.gm.mqtransfer.provider.facade.api.PollMessageRequest;
import com.gm.mqtransfer.provider.facade.api.PollMessageResponse;
import com.gm.mqtransfer.provider.facade.api.ResponseBuilder;
import com.gm.mqtransfer.provider.facade.api.ResponseCode;
import com.gm.mqtransfer.provider.facade.api.SubscribeData;
import com.gm.mqtransfer.provider.facade.api.SubscribeRequest;
import com.gm.mqtransfer.provider.facade.api.SubscribeResponse;
import com.gm.mqtransfer.provider.facade.api.UnsubscribeRequest;
import com.gm.mqtransfer.provider.facade.api.UnsubscribeResponse;
import com.gm.mqtransfer.provider.facade.common.OffsetTypeEnum;
import com.gm.mqtransfer.provider.facade.exception.BusinessException;
import com.gm.mqtransfer.provider.facade.model.ClusterInfo;
import com.gm.mqtransfer.provider.facade.model.ConsumerClientConfig;
import com.gm.mqtransfer.provider.facade.model.ConsumerPartition;
import com.gm.mqtransfer.provider.facade.model.MQMessage;
import com.gm.mqtransfer.provider.facade.model.RocketMQClusterInfo;
import com.gm.mqtransfer.provider.facade.service.consumer.AbstractConsumerService;
import com.gm.mqtransfer.provider.facade.util.PartitionUtils;
import com.gm.mqtransfer.provider.facade.util.StringUtils;
import com.gm.mqtransfer.provider.rocketmq.v420.common.Constants;
import com.gm.mqtransfer.provider.rocketmq.v420.common.RktMQMessage;

public class RocketMQ420ConsumerService extends AbstractConsumerService {

	protected Properties consumerProps;
	private final RocketMQClusterInfo clusterInfo;
	private DefaultMQPullConsumer consumer;
	private Map<String, MessageQueue> taskMessageQueueMap = new ConcurrentHashMap<>();
	/** 初始化消费者锁 */
	protected final ReentrantLock initConsumerLock = new ReentrantLock();
	/** 位置点提交锁 */
	protected final ReentrantLock commitLock = new ReentrantLock();
	private int currentEpoch;
	private boolean isReady = false;
	
	public RocketMQ420ConsumerService(ConsumerClientConfig config) {
		super(config);
		this.consumerProps = config.getConsumerProps();
		ClusterInfo clusterInfo = config.getCluster();
		if (clusterInfo instanceof RocketMQClusterInfo) {
			this.clusterInfo = (RocketMQClusterInfo)clusterInfo;
		} else {
			this.clusterInfo = (RocketMQClusterInfo)clusterInfo.toPluginClusterInfo();
		}
		this.init();
	}
	private void init() {
		fillEmptyPropWithDefVal(consumerProps, Constants.RocketConsumer.NAMESVR_ADDR_KEY, clusterInfo.getNameSvrAddr());
		fillEmptyPropWithDefVal(consumerProps, Constants.RocketConsumer.CONSUME_GROUP_KEY, getConsumerGroup());
		fillEmptyPropWithDefVal(consumerProps, Constants.RocketConsumer.CONSUMER_INSTANCE_NAME_KEY, generateInstanceName());
		fillEmptyPropWithDefVal(consumerProps, Constants.RocketConsumer.PULL_TIMEOUT, Constants.RocketConsumer.DEF_PULL_TIMEOUT_MS_VAL + "");
		
		String srcNameSvr = consumerProps.getProperty(Constants.RocketConsumer.NAMESVR_ADDR_KEY);
		if (StringUtils.isBlank(srcNameSvr)) {
			throw new IllegalArgumentException("the value of param 'nameServerAddr' is null!");
		}
	}
	
	@Override
	public void start() {
		logger.info("starting consumer --> {}", consumerProps);
		initConsumerLock.lock();
		try {
			initConsumer();
		} finally {
			initConsumerLock.unlock();
		}
	}

	@Override
	public void stop() {
		initConsumerLock.lock();
		try {
			this.isReady = false;
			taskMessageQueueMap.clear();
			if (consumer != null) {
				try {
					consumer.shutdown();
				} catch (Exception e) {}
				consumer = null;
			}
		} finally {
			initConsumerLock.unlock();
		}
	}

	@Override
	public int restart(int epoch) {
		initConsumerLock.lock();
		try {
			//多个线程共享客户端，所以只需要初始化一次
			if (epoch <= this.currentEpoch) {
				return -1;
			}
			logger.info("restarting consumer --> {}", consumerProps);
			this.currentEpoch = this.currentEpoch + 1;
			this.stop();
			initConsumer();
			return this.currentEpoch;
		} finally {
			initConsumerLock.unlock();
		}
	}
	
	private void initConsumer() {
		try {
			if (consumer != null) {
				return;
			}
			consumer = new DefaultMQPullConsumer(getPropStringValueWithDefVal(consumerProps, Constants.RocketConsumer.CONSUME_GROUP_KEY, getConsumerGroup()));
			//org.apache.rocketmq.client.exception.MQClientException:
			// Long polling mode, the consumer consumerTimeoutMillisWhenSuspend
			// must greater than brokerSuspendMaxTimeMillis
			//See http://rocketmq.apache.org/docs/faq/ for further details.
			//consumer.setConsumerTimeoutMillisWhenSuspend(10*1000)
			consumer.setInstanceName(getPropStringValueWithDefVal(consumerProps, Constants.RocketConsumer.CONSUMER_INSTANCE_NAME_KEY, generateInstanceName()));
			consumer.setNamesrvAddr(consumerProps.getProperty(Constants.RocketConsumer.NAMESVR_ADDR_KEY));
			consumer.setConsumerPullTimeoutMillis(getPropIntValueWithDefVal(consumerProps, Constants.RocketConsumer.PULL_TIMEOUT, Constants.RocketConsumer.DEF_PULL_TIMEOUT_MS_VAL));
			consumer.start();
			consumer.setOffsetStore(consumer.getDefaultMQPullConsumerImpl().getOffsetStore());
        	this.isReady = true;
        } catch (Exception e) {
        	logger.error("register rkt consumer error", e);
        	this.isReady = false;
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
	public SubscribeResponse subscribe(SubscribeRequest request) {
		ConsumerPartition task = request.getPartition();
        SubscribeData data = new SubscribeData();
        if (consumer == null) {
        	return ResponseBuilder.toBuild(SubscribeResponse.class, ResponseCode.ConsumerClientNotFound);
        }
        try {
    		long startOffset = this.getOffset(consumer, task, Constants.RocketConsumer.CONSUME_FROM_FIRST_VAL);
        	long endOffset = this.getOffset(consumer, task, Constants.RocketConsumer.CONSUME_FROM_LAST_VAL);
        	long consumerOffset = this.getConsumerOffset(consumer, task, startOffset, endOffset);
        	data.setStartOffset(startOffset);
        	data.setEndOffset(endOffset);
        	data.setConsumerOffset(consumerOffset);
    	} catch (Exception e) {
    		return ResponseBuilder.toBuild(SubscribeResponse.class, ResponseCode.FetchOffsetError);
    	}
		return ResponseBuilder.toBuild(SubscribeResponse.class, data);
	}
	@Override
	public SubscribeResponse resubscribe(SubscribeRequest request) {
		this.unsubscribe(new UnsubscribeRequest(request.getPartition()));
		return this.subscribe(request);
	}

	@Override
	public UnsubscribeResponse unsubscribe(UnsubscribeRequest request) {
		return ResponseBuilder.toBuild(UnsubscribeResponse.class, new SubscribeData());
	}
	/**
	 * 获取指定位置的offset
	 * @param consumer
	 * @param task
	 * @param offsetReset
	 * @return
	 * @throws MQClientException 
	 */
	private long getOffset(DefaultMQPullConsumer consumer, ConsumerPartition task, String offsetReset) throws MQClientException {
		MessageQueue messageQueue = new MessageQueue(task.getFromTopic(), task.getFromBrokerName(), task.getFromPartition());
		long startOffset;
		if (offsetReset.equalsIgnoreCase(Constants.RocketConsumer.CONSUME_FROM_LAST_VAL)) {
			startOffset = consumer.maxOffset(messageQueue);
		} else if (offsetReset.equalsIgnoreCase(Constants.RocketConsumer.CONSUME_FROM_FIRST_VAL)) {
			startOffset = consumer.minOffset(messageQueue);
		} else {
			throw new BusinessException("Unsupported configuration[" + Constants.RocketConsumer.CONSUME_FROM_WHERE_NAME + "=" + offsetReset + "]");
		}
		return startOffset;
	}
	private long getConsumerOffset(DefaultMQPullConsumer consumer, ConsumerPartition task) throws MQClientException {
		long startOffset = this.getOffset(consumer, task, Constants.RocketConsumer.CONSUME_FROM_FIRST_VAL);
    	long endOffset = this.getOffset(consumer, task, Constants.RocketConsumer.CONSUME_FROM_LAST_VAL);
    	long consumerOffset = this.getConsumerOffset(consumer, task, startOffset, endOffset);
    	return consumerOffset;
	}
	/**
	 * 获取消费的位置点
	 * @param consumer
	 * @param task
	 * @param partitionStartOffset
	 * @param partitionEndOffset
	 * @return
	 * @throws MQClientException
	 */
	private long getConsumerOffset(DefaultMQPullConsumer consumer, ConsumerPartition task, long partitionStartOffset, long partitionEndOffset) throws MQClientException {
		MessageQueue messageQueue = new MessageQueue(task.getFromTopic(), task.getFromBrokerName(), task.getFromPartition());
		long consumerOffset = consumer.fetchConsumeOffset(messageQueue, true);
		//startOffset==0有二种情况：第一种：新消费组首次注册消费，broker没有存储该队列的消息消费进度；第二种：消费组从头开始消费（比如重置位点或在注册消费组时，topic还没有消息）
		//由于业务需要指定位点进行消费，所以将等于0的情况放到指定位点流程处理
		//存在问题：如果消费组位点被重置到0，但指定位点不是0，则会从指定位点开始消费，会丢失从0到指定位点之间的消息，解决办法：重置位点到0，将指定位点也设置到0或者不设置
		if (consumerOffset <= 0) {
			//-1:表示并没有存储该队列的消息消费进度，如果指定了位点，就从指定位点开始消费，否则根据配置从分区的最小或最大位点开始消费；
			//0：表示并没有存储该队列的消息消费进度或当前位点为0，如果指定了位点，就从指定位点开始消费，否则就从0开始消费
			if (consumerOffset == -1 || consumerOffset == 0) {
				String expectStartOffset = task.getExpectStartOffset();
				Long expectStartOffsetValue = task.getExpectStartOffsetValue();
				if (StringUtils.isNotBlank(expectStartOffset)) {//从指定位置开始消费
					OffsetTypeEnum offsetType = OffsetTypeEnum.getByName(expectStartOffset);
					if (offsetType == null) {
						throw new BusinessException("Unsupported configuration[expectStartOffset=" + expectStartOffset + "]");
					}
					switch (offsetType) {
						case FIRST_OFFSET:
							consumerOffset = partitionStartOffset;
							break;
						case LAST_OFFSET:
							consumerOffset = partitionEndOffset;
							break;
						case DESIGN_OFFSET://如果指定了消费位置，则从指定位置开始消费
							logger.info("[{}] will start consuming from the expect offset --> expectStartOffset[{}]", task.getTaskCode(), expectStartOffsetValue);
							consumerOffset = expectStartOffsetValue.longValue();
							break;
						case DESIGN_TIMESTAMP:
							consumerOffset = consumer.searchOffset(messageQueue, expectStartOffsetValue.longValue());
							break;
					}
				} else if (consumerOffset == -1){//此处排除0的原因，是为了避免位置点重置到0不能生效的情况
					String fromWhere = getPropStringValueWithDefVal(consumerProps, Constants.RocketConsumer.CONSUME_FROM_WHERE_NAME, Constants.RocketConsumer.DEF_CONSUME_FROM_WHERE_VAL);
					if (StringUtils.isBlank(fromWhere)) {
						fromWhere = Constants.RocketConsumer.DEF_CONSUME_FROM_WHERE_VAL;
					}
					if (fromWhere.equalsIgnoreCase(Constants.RocketConsumer.DEF_CONSUME_FROM_WHERE_VAL)) {
						consumerOffset = partitionStartOffset;
					} else if (fromWhere.equalsIgnoreCase(Constants.RocketConsumer.CONSUME_FROM_FIRST_VAL)) {
						consumerOffset = partitionEndOffset;
					} else {
						throw new BusinessException("Unsupported configuration[" + Constants.RocketConsumer.CONSUME_FROM_WHERE_NAME + "=" + fromWhere + "]");
					}
				}
			}
		}
		return consumerOffset;
	}

	@Override
	public FetchOffsetResponse fetchOffset(FetchOffsetRequest request) {
		ConsumerPartition task = request.getPartition();
        if (consumer == null) {
        	return ResponseBuilder.toBuild(FetchOffsetResponse.class, ResponseCode.ConsumerClientNotFound);
        }
        try {
	        long offset = this.getConsumerOffset(consumer, task);
	        FetchOffsetData data = new FetchOffsetData();
	        data.setOffset(offset);
			return ResponseBuilder.toBuild(FetchOffsetResponse.class, data);
        } catch (Exception e) {
        	return ResponseBuilder.toBuild(FetchOffsetResponse.class, ResponseCode.FetchOffsetError);
        }
	}

	@Override
	public FetchOffsetRangeResponse fetchOffsetRange(FetchOffsetRangeRequest request) {
		ConsumerPartition task = request.getPartition();
        if (consumer == null) {
        	return ResponseBuilder.toBuild(FetchOffsetRangeResponse.class, ResponseCode.ConsumerClientNotFound);
        }
        try {
	        long startOffset = this.getOffset(consumer, task, Constants.RocketConsumer.CONSUME_FROM_FIRST_VAL);
	    	long endOffset = this.getOffset(consumer, task, Constants.RocketConsumer.CONSUME_FROM_LAST_VAL);
	        FetchOffsetRangeData data = new FetchOffsetRangeData();
	        data.setStartOffset(startOffset);
	    	data.setEndOffset(endOffset);
			return ResponseBuilder.toBuild(FetchOffsetRangeResponse.class, data);
        } catch (Exception e) {
        	return ResponseBuilder.toBuild(FetchOffsetRangeResponse.class, ResponseCode.FetchOffsetError);
        }
	}

	@Override
	public CommitOffsetResponse commitOffset(CommitOffsetRequest request) {
		ConsumerPartition task = request.getPartition();
		Long offset = request.getOffset();
		if (consumer == null) {
			logger.error("[{}] offset[{}] commit faliure, error:Cannot find the consumer", task.getFromDesc(), offset);
			return ResponseBuilder.toBuild(CommitOffsetResponse.class, ResponseCode.ConsumerClientNotFound);
        }
		try {
			MessageQueue messageQueue = new MessageQueue(task.getFromTopic(), task.getFromBrokerName(), task.getFromPartition());
			//保存至kc内存;
			consumer.updateConsumeOffset(messageQueue, offset);
			//持久化至broker;
			consumer.getOffsetStore().persist(messageQueue);
			if (logger.isDebugEnabled()) {
				logger.debug("taskId[{}],topic[{}],queueId[{}],offset[{}] commit success", task.getTaskCode(), task.getFromTopic(), task.getFromPartitionKey(), offset);
			}
			return ResponseBuilder.toBuild(CommitOffsetResponse.class, new Object());
		} catch (Exception e) {
			logger.info("taskId[{}],topic[{}],queueId[{}],offset[{}] commit faliure, error:{}", task.getTaskCode(), task.getFromTopic(), task.getFromPartitionKey(), offset, e.getMessage());
			return ResponseBuilder.toBuild(CommitOffsetResponse.class, ResponseCode.CommitOffsetError);
		}
	}

	@Override
	public PollMessageResponse pollMessage(PollMessageRequest request) {
		ConsumerPartition task = request.getPartition();
		int maxNums = request.getMaxNums();
		long maxSize = request.getMaxSize() != null ? request.getMaxSize().longValue() : 200000;
		MessageQueue messageQueue = new MessageQueue(task.getFromTopic(), task.getFromBrokerName(), task.getFromPartition());
		Long startOffset = task.getStartOffset();
		PullResult result = null;
		try{
			if (logger.isDebugEnabled()) {
				logger.debug("taskId[{}],topic[{}],partition[{}],offset[{}],maxPollRecords[{}],maxBytes[{}] poll message", task.getTaskCode(), messageQueue.getTopic(), messageQueue.getQueueId(), startOffset, maxNums, maxSize);
			}
			if (startOffset == null) {
				throw new BusinessException("startOffset is empty");
			}
        	result = consumer.pull(messageQueue, SubscriptionData.SUB_ALL, startOffset, maxNums);
        }catch (Throwable e) {
        	logger.error("fetch batch message from rocketmq error:", e);
        	return ResponseBuilder.toBuild(PollMessageResponse.class, ResponseCode.FetchMessageError);
        }
		if (result.getPullStatus() == PullStatus.NO_NEW_MSG 
				|| result.getPullStatus() == PullStatus.NO_MATCHED_MSG 
				|| result.getPullStatus() == PullStatus.OFFSET_ILLEGAL) {
			if (startOffset != result.getNextBeginOffset()) {
				logger.warn("taskId[{}],topic[{}],partition[{}],startOffset[{}],nextOffset[{}], The offset of the pull message failure[{}] will skip to nextOffset", 
						task.getTaskCode(), messageQueue.getTopic(), messageQueue.getQueueId(), startOffset, result.getNextBeginOffset(), result.getPullStatus());
			}
            return ResponseBuilder.toBuild(PollMessageResponse.class, ResponseCode.FetchMessageError);
        }
		List<MessageExt> messageList = result.getMsgFoundList();
		if (messageList == null || messageList.isEmpty()) {
			return ResponseBuilder.toBuild(PollMessageResponse.class, new PollMessageData());
		}
		Long endOffset = null;
		List<MQMessage> retList = new ArrayList<>();
		for (MessageExt message : messageList) {
			try {
				MQMessage cmsg = this.doConvert(message);//转换消息
        		retList.add(cmsg);
        		endOffset = message.getQueueOffset();
    		} catch (Exception e) {
    			logger.error("rocketmq msg parse error", e);
    		}
		}
		PollMessageData data = new PollMessageData();
        data.setEndOffset(endOffset);
        data.setHighWatermark(result.getMaxOffset());
        data.setMessages(retList);
        return ResponseBuilder.toBuild(PollMessageResponse.class, data);
	}
	
	private RktMQMessage doConvert(MessageExt message) throws Exception {
		String partitionKey = PartitionUtils.generatorPartitionKey(message.getBornHostNameString(), message.getQueueId());
		RktMQMessage msgExt = new RktMQMessage(message.getTopic(), partitionKey, message.getMsgId(), message.getKeys(), message.getBody());
        msgExt.setStoreId(message.getMsgId());
        msgExt.setOffset(message.getQueueOffset());
        msgExt.setTags(message.getTags());
        msgExt.setSentTimestamp(message.getBornTimestamp());
        msgExt.setReconsumeTimes(message.getReconsumeTimes());

        String omid = message.getProperty(Constants.MSG_PROP_RMQ_OMID_KEY);//转发保持消息ID不变
        if (omid != null && omid.trim().length() != 0) {
            msgExt.setMessageId(omid);
        }
        msgExt.setHeaders(message.getProperties());
        msgExt.setProps(message.getProperties());
        return msgExt;
	}

}
