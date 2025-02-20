package com.gm.mqtransfer.provider.kafka.v082.service.consumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

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
import com.gm.mqtransfer.provider.facade.model.KafkaClusterInfo;
import com.gm.mqtransfer.provider.facade.model.MQMessage;
import com.gm.mqtransfer.provider.facade.service.consumer.AbstractConsumerService;
import com.gm.mqtransfer.provider.facade.util.StringUtils;
import com.gm.mqtransfer.provider.kafka.v082.common.Constants;

import kafka.api.PartitionFetchInfo;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.api.TopicMetadataRequest;
import kafka.common.ErrorMapping;
import kafka.common.OffsetAndMetadata;
import kafka.common.OffsetMetadataAndError;
import kafka.common.TopicAndPartition;
import kafka.consumer.ConsumerConfig;
import kafka.javaapi.FetchRequest;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.OffsetCommitRequest;
import kafka.javaapi.OffsetCommitResponse;
import kafka.javaapi.OffsetFetchRequest;
import kafka.javaapi.OffsetFetchResponse;
import kafka.javaapi.OffsetRequest;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.message.MessageAndMetadata;
import kafka.message.MessageAndOffset;
import kafka.serializer.Decoder;
import kafka.serializer.DefaultDecoder;

public class Kafka082ConsumerService extends AbstractConsumerService {

	protected Properties consumerProps;
	protected ConsumerConfig config;
	protected Kafka08Cluster cluster;
	protected KafkaClusterInfo clusterInfo;
	private Decoder<byte[]> keyDecoder;
	private Decoder<byte[]>  valueDecoder;
	private Map<String, SimpleConsumer> taskConsumerMap = new ConcurrentHashMap<>();
	/** 初始化消费者锁 */
	protected final ReentrantLock initConsumerLock = new ReentrantLock();
	/** 位置点提交锁 */
	protected final ReentrantLock commitLock = new ReentrantLock();
	
	private int currentEpoch;
	private boolean isReady = false;
	
	public Kafka082ConsumerService(ConsumerClientConfig config) {
		super(config);
		this.consumerProps = config.getConsumerProps();
		ClusterInfo clusterInfo = config.getCluster();
		if (clusterInfo instanceof KafkaClusterInfo) {
			this.clusterInfo = (KafkaClusterInfo)clusterInfo;
		} else {
			this.clusterInfo = (KafkaClusterInfo)clusterInfo.toPluginClusterInfo();
		}
		this.init();
	}
	
	private void init() {
		this.keyDecoder = new DefaultDecoder(null);
	    this.valueDecoder = new DefaultDecoder(null);
		//根据0.8特性配置
	    fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.ZOOKEEPER_CONNECT_NAME,  clusterInfo.getZkUrl());
	    fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.BOOTSTRAP_SERVERS_NAME,  clusterInfo.getBrokerList());
	    fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.GROUP_ID_NAME,  this.getConsumerGroup());
	    fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.CONSUMER_ID_NAME,  generateInstanceName());
	    fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.AUTO_COMMIT_ENABLE_NAME,  Constants.KafkaConsumer.DEF_AUTO_COMMIT_ENABLE_VAL);
	    fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.MAX_POLL_RECORDS, Constants.KafkaConsumer.DEF_MAX_POLL_RECORDS_VAL);
		fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.NEW_KEY_DESERIALIZER_NAME, Constants.KafkaConsumer.DEF_KEY_NEW_DESERIALIZER_VAL);
	  	fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.NEW_VALUE_DESERIALIZER_NAME, Constants.KafkaConsumer.DEF_VALUE_NEW_DESERIALIZER_VAL);
	  	fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.FETCH_MESSAGE_MAX_BYTES_NAME, Constants.KafkaConsumer.DEF_FETCH_MESSAGE_MAX_BYTES_VAL);
	  	fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.REBALANCE_BACKOFF_MS_NAME, Constants.KafkaConsumer.DEF_REBALANCE_BACKOFF_MS_VAL);
	  	fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.NUM_CONSUMERS_NAME, Constants.KafkaConsumer.DEF_NUM_CONSUMERS_VAL);
	  	String bootStrapSevers = consumerProps.getProperty(Constants.KafkaConsumer.BOOTSTRAP_SERVERS_NAME);
		if (StringUtils.isEmpty(bootStrapSevers)) {
		  throw new IllegalArgumentException("the value of param '" + Constants.KafkaConsumer.BOOTSTRAP_SERVERS_NAME + "' is null!");
		}
		String zkUrl = consumerProps.getProperty(Constants.KafkaConsumer.ZOOKEEPER_CONNECT_NAME);
		if (StringUtils.isEmpty(zkUrl)) {
			throw new IllegalArgumentException("the value of param '" + Constants.KafkaConsumer.ZOOKEEPER_CONNECT_NAME + "' is null!");
		}
	  	this.config = new ConsumerConfig(consumerProps);
	}
	
	public void start() {
		logger.info("starting consumer --> {}", consumerProps);
		if (this.isReady) {
			return;
		}
		cluster = new Kafka08Cluster(config);
		this.isReady = true;
	}
	@Override
	public int restart(int epoch) {
		initConsumerLock.lock();
		try {
			if (epoch <= this.currentEpoch) {
				return -1;
			}
			logger.info("restarting consumer --> {}", consumerProps);
			this.stop();
			this.start();
			this.currentEpoch++;
			return this.currentEpoch;
		} finally {
			initConsumerLock.unlock();
		}
	}
	public void stop() {
		logger.info("stoping consumer --> {} ", consumerProps);
		this.isReady = false;
		for (Map.Entry<String, SimpleConsumer> entry : taskConsumerMap.entrySet()) {
			try {
				cluster.close(entry.getValue());
			} catch (Exception e) {}
		}
		taskConsumerMap.clear();
		cluster.close();
	}
	
	@Override
	public PollMessageResponse pollMessage(PollMessageRequest request) {
		FetchResponse rsp = null;
		ConsumerPartition task = request.getPartition();
		int maxNums = request.getMaxNums();
		long maxSize = request.getMaxSize() != null ? request.getMaxSize().longValue() : 200000;
        Long startOffset = task.getStartOffset();
        SimpleConsumer consumer = this.getConsumer(task);
        if (consumer == null) {
        	return ResponseBuilder.toBuild(PollMessageResponse.class, ResponseCode.ConsumerClientNotFound);
        }
        if (startOffset == null) {
        	return ResponseBuilder.toBuild(PollMessageResponse.class, ResponseCode.StartOffsetEmptyError);
		}
        try {
			Map<TopicAndPartition, PartitionFetchInfo> requestInfo = new HashMap<>();
			TopicAndPartition tp = new TopicAndPartition(task.getFromTopic(), task.getFromPartition());
			PartitionFetchInfo fetchInfo = new PartitionFetchInfo(startOffset, (int)maxSize);
			requestInfo.put(tp, fetchInfo);
			FetchRequest fetchRequest = new FetchRequest(kafka.api.TopicMetadataRequest.CurrentVersion(), 
					config.clientId(), 
					config.fetchWaitMaxMs()*10, 
					config.fetchMinBytes(),
					requestInfo);
			if (logger.isDebugEnabled()) {
				logger.debug("[{}] poll message-->offset[{}],maxPollRecords[{}],maxBytes[{}]", task.getFromDesc(), startOffset, maxNums, maxSize);
			}
			rsp = consumer.fetch(fetchRequest);
			PollMessageResponse errResponse = handleFetchErr(task, rsp);//处理错误的响应结果
			if (errResponse != null) {
				return errResponse;
			}
        } catch (Throwable e) {
        	logger.error(String.format("[%s] fetch batch message from kafka error:offset[%s]", task.getFromDesc(), startOffset), e);
        	return ResponseBuilder.toBuild(PollMessageResponse.class, ResponseCode.FetchMessageError.name(), e.getMessage());
        }
        Long endOffset = null;
        List<MQMessage> retList = new ArrayList<>();
        int totalSize = 0;
        Iterator<MessageAndOffset> sourceIterator = rsp.messageSet(task.getFromTopic(), task.getFromPartition()).iterator();
        while(sourceIterator.hasNext()) {
        	MessageAndOffset message = sourceIterator.next();
        	totalSize += message.message().size();
        	if (message.offset() >= startOffset) {
        		endOffset = message.offset();
        		try {
        			MQMessage cmsg = this.doConvert(task, message);//转换消息
	        		retList.add(cmsg);
        		} catch (Throwable e) {
        			logger.error("[" + task.getFromDesc() + "] kafka msg parse error", e);
        		}
        	}
        	if (totalSize >= maxSize) {//消息存在压缩的情况，在解压后，大小可能远超限定大小，所以判断消息大小超过限制大小后，就退出消息的获取，避免获取消息过程中造成内存占用超过限定
        		break;
        	}
        }
        PollMessageData data = new PollMessageData();
        data.setEndOffset(endOffset);
        data.setHighWatermark(rsp.highWatermark(task.getFromTopic(), task.getFromPartition()));
        data.setMessages(retList);
        return ResponseBuilder.toBuild(PollMessageResponse.class, data);
	}
	
	/**
	 * 将kafka native消息对象转换成自定义消息对象
	 * @param message
	 * @return
	 */
	public MQMessage doConvert(ConsumerPartition task, MessageAndOffset message) throws Exception {
		MessageAndMetadata<byte[], byte[]> mate = new MessageAndMetadata<>(task.getFromTopic(), task.getFromPartition(),
				message.message(), message.offset(), keyDecoder ,valueDecoder);
		String topic = mate.topic();
        long offset = mate.offset();
        int partition = mate.partition();
        String key = mate.key() == null ? null : new String(mate.key(), Constants.CHARSET_FORMAT);
        MQMessage msgExt = new MQMessage();
        msgExt.setOffset(offset);
        msgExt.setTopic(topic);
        msgExt.setPartition(partition + "");
        msgExt.setMessageKey(key);
        msgExt.setMessageBytes(mate.message());
        return msgExt;
	}
	
	@Override
	public SubscribeResponse subscribe(SubscribeRequest request) {
		ConsumerPartition task = request.getPartition();
		SimpleConsumer consumer = this.getConsumer(task);
        if (consumer == null) {
        	try {
    	        consumer = cluster.connectLeader(task.getFromTopic(), task.getFromPartition(), true);
    	        this.setConsumer(task, consumer);
            } catch (Exception e) {
            	logger.error("[" + task.getFromDesc() + "] create kafka consumer error --> " + consumerProps, e);
            	return ResponseBuilder.toBuild(SubscribeResponse.class, ResponseCode.CreateConsumerError.name(), e.getMessage());
            }
        }
        SubscribeData data = new SubscribeData();
        if (consumer != null) {
        	try {
	        	long startOffset = this.getOffset(consumer, task, Constants.KafkaConsumer.SMALL_AUTO_OFFSET_RESET_VAL);
	        	long endOffset = this.getOffset(consumer, task, Constants.KafkaConsumer.LARGEST_AUTO_OFFSET_RESET_VAL);
	        	long consumerOffset = this.getConsumerOffset(consumer, task, startOffset, endOffset);
	        	data.setStartOffset(startOffset);
	        	data.setEndOffset(endOffset);
	        	data.setConsumerOffset(consumerOffset);
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        }
		return ResponseBuilder.toBuild(SubscribeResponse.class, data);
	}

	@Override
	public SubscribeResponse resubscribe(SubscribeRequest request) {
		ConsumerPartition task = request.getPartition();
		try {
            this.unsubscribe(new UnsubscribeRequest(request.getPartition()));
	        return this.subscribe(request);
        } catch (Exception e) {
        	logger.error("[" + task.getFromDesc() + "] create kafka consumer error --> " + consumerProps, e);
        	return ResponseBuilder.toBuild(SubscribeResponse.class, ResponseCode.CreateConsumerError.name(), e.getMessage());
        }
	}

	@Override
	public UnsubscribeResponse unsubscribe(UnsubscribeRequest request) {
		ConsumerPartition task = request.getPartition();
		SimpleConsumer consumer = this.removeConsumer(task);
		if (consumer != null) {
			cluster.close(consumer);
		}
		return ResponseBuilder.toBuild(UnsubscribeResponse.class, new SubscribeData());
	}

	@Override
	public int getCurrentEpoch() {
		return this.currentEpoch;
	}

	@Override
	public boolean isReady() {
		return isReady;
	}
	
	@Override
	public FetchOffsetResponse fetchOffset(FetchOffsetRequest request) {
		ConsumerPartition task = request.getPartition();
		SimpleConsumer consumer = this.getConsumer(task);
        if (consumer == null) {
        	return ResponseBuilder.toBuild(FetchOffsetResponse.class, ResponseCode.ConsumerClientNotFound);
        }
        long offset = this.getConsumerOffset(consumer, task);
        FetchOffsetData data = new FetchOffsetData();
        data.setOffset(offset);
		return ResponseBuilder.toBuild(FetchOffsetResponse.class, data);
	}
	
	public FetchOffsetRangeResponse fetchOffsetRange(FetchOffsetRangeRequest request) {
		ConsumerPartition task = request.getPartition();
		SimpleConsumer consumer = this.getConsumer(task);
        if (consumer == null) {
        	return ResponseBuilder.toBuild(FetchOffsetRangeResponse.class, ResponseCode.ConsumerClientNotFound);
        }
        long startOffset = this.getOffset(consumer, task, Constants.KafkaConsumer.SMALL_AUTO_OFFSET_RESET_VAL);
    	long endOffset = this.getOffset(consumer, task, Constants.KafkaConsumer.LARGEST_AUTO_OFFSET_RESET_VAL);
        FetchOffsetRangeData data = new FetchOffsetRangeData();
        data.setStartOffset(startOffset);
    	data.setEndOffset(endOffset);
		return ResponseBuilder.toBuild(FetchOffsetRangeResponse.class, data);
	}

	/**
	 * 获取消费者组的位置点
	 * @param consumer
	 * @param topic
	 * @param partition
	 * @return
	 */
	public long getConsumerOffset(SimpleConsumer consumer, ConsumerPartition task, long partitionStartOffset, long partitionEndOffset) {
		String taskCode = task.getTaskCode(); 
		String topic = task.getFromTopic();
		Integer partition = task.getFromPartition(); 
		String expectStartOffset = task.getExpectStartOffset();
		Long expectStartOffsetValue = task.getExpectStartOffsetValue();
		TopicAndPartition tp = new TopicAndPartition(topic, partition);
//		//Version 0 of the request will fetch offsets from Zookeeper and version 1 version 1 and above will fetch offsets from Kafka.
		//新创建Topic且未生产任何消息时，调用该方法获取offset会出现UnknownTopicOrPartitionException的异常(zookeeper上无相关信息)
		List<TopicAndPartition> partitionList = new ArrayList<>();
		partitionList.add(tp);
		OffsetFetchRequest request = new OffsetFetchRequest(getConsumerGroup(), partitionList, TopicMetadataRequest.CurrentVersion(), 0, config.clientId());
		OffsetFetchResponse rsp = consumer.fetchOffsets(request);
		//未找到位置点信息，可能是由于消费者组未注册
		Map<TopicAndPartition, OffsetMetadataAndError> offsetMap = rsp.offsets();
		if (offsetMap == null || offsetMap.isEmpty()) {
			throw new BusinessException("fetch offset error，cannot found metadata");
		}
		OffsetMetadataAndError offsetMeta = offsetMap.get(tp);
		if (offsetMeta == null) {
			throw new BusinessException("fetch offset error，cannot found metadata of partiton");
		}
		if (offsetMeta.error() == ErrorMapping.NoError()) {
			return offsetMeta.offset();
		} else if (offsetMeta.error() == ErrorMapping.UnknownTopicOrPartitionCode()) {//可能是由于消费者组未注册
			OffsetTypeEnum offsetType = null;
			if (StringUtils.isNotBlank(expectStartOffset)) {
				offsetType = OffsetTypeEnum.getByName(expectStartOffset);
			}
			if (offsetType == null) {
				throw new BusinessException("Unsupported configuration[expectStartOffset=" + expectStartOffset + "]");
			}
			long consumerOffset = 0;
			switch (offsetType) {
			case FIRST_OFFSET:
				consumerOffset = partitionStartOffset;
				break;
			case LAST_OFFSET:
				consumerOffset = partitionEndOffset;
				break;
			case DESIGN_OFFSET://如果指定了消费位置，则从指定位置开始消费
				logger.info("[{}] will start consuming from the expect offset --> expectStartOffset[{}]", taskCode, expectStartOffsetValue);
				consumerOffset = expectStartOffsetValue.longValue();
				break;
			case DESIGN_TIMESTAMP:
				throw new BusinessException("fetch offset error:Unsupported start offset[" + expectStartOffset + "]");
			}
//			try {
//				//对于未获取到位置点的，强制提交一次位置点
//				this.commitOffset(consumer, task, consumerOffset);
//			} catch (Exception e) {
//				logger.error("[{}] offset[{}] commit offset failure,{}", task.getFromDesc(), retOffset, e.getMessage());
//			}
			return consumerOffset;
		} else {
			throw new BusinessException("fetch offset error:" + offsetMeta.error() + "-->" + ErrorMapping.exceptionFor(offsetMeta.error()));
		}
	}
	private long getConsumerOffset(SimpleConsumer consumer, ConsumerPartition task) {
		long startOffset = this.getOffset(consumer, task, Constants.KafkaConsumer.SMALL_AUTO_OFFSET_RESET_VAL);
    	long endOffset = this.getOffset(consumer, task, Constants.KafkaConsumer.LARGEST_AUTO_OFFSET_RESET_VAL);
    	long consumerOffset = this.getConsumerOffset(consumer, task, startOffset, endOffset);
    	return consumerOffset;
	}
	/**
	 * 获取分区的offset范围
	 * @param consumer
	 * @param topic
	 * @param partition
	 * @param offsetReset
	 * @return
	 */
	public long getOffset(SimpleConsumer consumer, ConsumerPartition task, String offsetReset) {
		TopicAndPartition tp = new TopicAndPartition(task.getFromTopic(), task.getFromPartition());
		//获取分区最后位置点作为开始消费的位置点
		Map<TopicAndPartition, PartitionOffsetRequestInfo> reqMap = new HashMap<>(1);
		//通过配置获取从那里开始消费
		if (StringUtils.isBlank(offsetReset)) {
			offsetReset = Constants.KafkaConsumer.DEF_AUTO_OFFSET_RESET_VAL;
		}
		//-1L表示：OffsetRequest.LatestTime,-2L表示：OffsetRequest.EarliestTime
		if (offsetReset.equalsIgnoreCase(Constants.KafkaConsumer.LARGEST_AUTO_OFFSET_RESET_VAL)) {
			reqMap.put(tp, new PartitionOffsetRequestInfo(kafka.api.OffsetRequest.LatestTime(), 1));
		} else if (offsetReset.equalsIgnoreCase(Constants.KafkaConsumer.SMALL_AUTO_OFFSET_RESET_VAL)) {
			reqMap.put(tp, new PartitionOffsetRequestInfo(kafka.api.OffsetRequest.EarliestTime(), 1));
		} else {
			throw new BusinessException("fetch offset error，Unrecognized parameter:" + Constants.KafkaConsumer.AUTO_OFFSET_RESET_NAME + "=" + offsetReset);
		}
		OffsetRequest req = new OffsetRequest(reqMap, TopicMetadataRequest.CurrentVersion(), "");
		OffsetResponse offsetRsp = consumer.getOffsetsBefore(req);
		if (!offsetRsp.hasError()) {
			long[] offsets = offsetRsp.offsets(tp.topic(), tp.partition());
			if (offsets != null && offsets.length > 0) {
				return offsets[0];
			} else {
				throw new BusinessException("fetch offset error, can not find offset");
			}
		} else {
			Throwable e = ErrorMapping.exceptionFor(offsetRsp.errorCode(tp.topic(),tp.partition()));
    		if (e != null) {
    			throw new BusinessException(e);
    		} else {
    			throw new BusinessException("unknow error");
    		}
		}
	}
	
	@Override
	public CommitOffsetResponse commitOffset(CommitOffsetRequest request) {
		ConsumerPartition task = request.getPartition();
		Long offset = request.getOffset();
		SimpleConsumer consumer = this.getConsumer(task);
		return this.commitOffset(consumer, task, offset);
	}
	
	private CommitOffsetResponse commitOffset(SimpleConsumer consumer, ConsumerPartition task, Long offset) {
		String topic = task.getFromTopic(); 
		Integer partition = task.getFromPartition();
		if (consumer == null) {
			logger.error("[{}] offset[{}] commit faliure, error:Cannot find the consumer", task.getFromDesc(), offset);
			return ResponseBuilder.toBuild(CommitOffsetResponse.class, ResponseCode.ConsumerClientNotFound);
        }
		try {
			Map<TopicAndPartition, OffsetAndMetadata> tpOffsetMap = new HashMap<>();
			TopicAndPartition tp = new TopicAndPartition(topic, partition);
			OffsetAndMetadata offsetData = new OffsetAndMetadata(offset, OffsetAndMetadata.NoMetadata(), -1L);
			tpOffsetMap.put(tp, offsetData);
			OffsetCommitRequest request = new OffsetCommitRequest(getConsumerGroup(), tpOffsetMap, 
					TopicMetadataRequest.CurrentVersion(),
					config.clientId(), TopicMetadataRequest.CurrentVersion());
			OffsetCommitResponse rsp = consumer.commitOffsets(request);
			if (rsp.hasError()) {
				int errorCode = rsp.errorCode(tp);
				logger.error("[{}] offset[{}] commit faliure, error:{}", task.getFromDesc(), offset, errorCode);
				return ResponseBuilder.toBuild(CommitOffsetResponse.class, ResponseCode.CommitOffsetError);
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("[{}] offset[{}] commit success", task.getFromDesc(), offset);
				}
				return ResponseBuilder.toBuild(CommitOffsetResponse.class, new Object());
			}
		} catch (Exception e) {
			logger.error("[{}] offset[{}] commit faliure, error:{}", task.getFromDesc(), offset, e.getMessage());
			return ResponseBuilder.toBuild(CommitOffsetResponse.class, ResponseCode.CommitOffsetError);
		}
	}
	public PollMessageResponse handleFetchErr(ConsumerPartition task, FetchResponse resp) throws Throwable {
        if (resp.hasError()) {
            short err = resp.errorCode(task.getFromTopic(), task.getFromPartition());
            logger.error("kafka.common.ErrorMapping code=" + err, ErrorMapping.exceptionFor(err));
            if (err == ErrorMapping.LeaderNotAvailableCode() ||
                    err == ErrorMapping.NotLeaderForPartitionCode()) {
//                String errInfo = String.format("[%s] Lost leader for topic, sleeping for %s ms", task.getFromDesc(), this.config.refreshLeaderBackoffMs());
//                logger.error(errInfo);
//                try {
//                    Thread.sleep(this.config.refreshLeaderBackoffMs());
//                } catch (InterruptedException e) {
//                	logger.error("error:",e);
//                }
//                this.resubscribe(task);//重新初始化
                return ResponseBuilder.toBuild(PollMessageResponse.class, ResponseCode.NotLeaderForPartitionError);
            } else if (err == ErrorMapping.OffsetOutOfRangeCode()) {//文件过期被清除等原因，造成从当前位置点不能消费
//            	this.doReInitOffsetFromPartition(task);
            	return ResponseBuilder.toBuild(PollMessageResponse.class, ResponseCode.OffsetOutOfRangeError);
            }
            return ResponseBuilder.toBuild(PollMessageResponse.class, ResponseCode.OtherError.name(), "code:" + err + ",msg:" + ErrorMapping.exceptionFor(err).getClass().getCanonicalName());
        }
        return null;
    }
	/**
	 * 重新初始化位置点
	 * @param task
	 */
	private boolean doReInitOffsetFromPartition(ConsumerPartition task) {
		SimpleConsumer consumer = this.getConsumer(task);
        if (consumer != null) {
        	try {
        		long oldOffset = task.getStartOffset();
        		long startOffset = getOffset(consumer, task, Constants.KafkaConsumer.SMALL_AUTO_OFFSET_RESET_VAL);
		        task.setStartOffset(startOffset);
		        if (task.getFirstOffset() == null) {//该初始方法可能是在消费了一些消息后才出现文件过期被清除，所以需要首次位点未设置的情况下才设置
		        	task.setFirstOffset(startOffset);
		        }
		        logger.info("[{}] consumer reinit offset successes --> oldOffset[{}],newOffset[{}]", task.getFromDesc(), oldOffset, startOffset);
    	        return true;
            } catch (Exception e) {
            	logger.error("[" + task.getFromDesc() + "] reinit kafka consumer offset error --> " + consumerProps, e);
            	return false;
            }
        } else {
        	return false;
        }
	}
	public SimpleConsumer getConsumer(ConsumerPartition task) {
        return this.getConsumer(task.getFromTopic(), task.getFromPartition());
	}
	private SimpleConsumer getConsumer(String topic, Integer partition) {
		String consumerKey = this.generateConsumerKey(topic, partition);
		return taskConsumerMap.get(consumerKey);
	}
	private SimpleConsumer setConsumer(ConsumerPartition task, SimpleConsumer consumer) {
		String consumerKey = this.generateConsumerKey(task.getFromTopic(), task.getFromPartition());
        return taskConsumerMap.put(consumerKey, consumer);
	}
	private SimpleConsumer removeConsumer(ConsumerPartition task) {
		String consumerKey = this.generateConsumerKey(task);
		return taskConsumerMap.remove(consumerKey);
	}
}
