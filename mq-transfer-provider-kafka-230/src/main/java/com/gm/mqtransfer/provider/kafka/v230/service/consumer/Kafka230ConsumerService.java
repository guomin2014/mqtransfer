package com.gm.mqtransfer.provider.kafka.v230.service.consumer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.utils.Utils;

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
import com.gm.mqtransfer.provider.facade.util.ReflectUtils;
import com.gm.mqtransfer.provider.facade.util.StringUtils;
import com.gm.mqtransfer.provider.kafka.v230.common.Constants;

public class Kafka230ConsumerService extends AbstractConsumerService {

	protected Properties consumerProps;
	protected KafkaClusterInfo clusterInfo;
	private Map<String, KafkaConsumer<String, byte[]>> taskConsumerMap = new ConcurrentHashMap<>();
	private Map<TopicPartition, OffsetAndMetadata> cacheOffsets = new ConcurrentHashMap<>();
	/** 拉取消息最大等待时间 */
	protected int pollMaxWait = 500;
	/** 缓存锁 */
	protected final ReentrantLock cacheLock = new ReentrantLock();
	/** 消费锁 */
	protected final ReentrantLock consumerLock = new ReentrantLock();
	/** 初始化消费者锁 */
	protected final ReentrantLock initConsumerLock = new ReentrantLock();
	
	private boolean isReady = false;
	private int currentEpoch;
	
	public Kafka230ConsumerService(ConsumerClientConfig config) {
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
		//根据2.3特性配置		
		// force config
	    fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.ENABLE_AUTO_COMMIT_NAME,  Constants.KafkaConsumer.ENABLE_AUTO_COMMIT_VAL);
	    fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.ALLOW_AUTO_CREATE_TOPICS_NAME,  Constants.KafkaConsumer.ALLOW_AUTO_CREATE_TOPICS_VAL);
	    fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.ZOOKEEPER_CONNECT_NAME,  clusterInfo.getZkUrl());
	    fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.BOOTSTRAP_SERVERS_NAME,  clusterInfo.getBrokerList());
	    fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.GROUP_ID_NAME,  this.getConsumerGroup());
	    fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.CONSUMER_ID_NAME,  generateInstanceName());
	    fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.MAX_POLL_RECORDS, Constants.KafkaConsumer.DEF_MAX_POLL_RECORDS_VAL);
	    fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.FETCH_MAX_WAIT_MS_CONFIG, Constants.KafkaConsumer.DEF_FETCH_MAX_WAIT_MS_CONFIG_VAL);
	  	fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.MAX_PARTITION_FETCH_BYTES_NAME, Constants.KafkaConsumer.MAX_PARTITION_FETCH_BYTES_VAL);
	  	fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.REBALANCE_BACKOFF_MS_NAME, Constants.KafkaConsumer.DEF_REBALANCE_BACKOFF_MS_VAL);
	  	fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.ISOLATION_LEVEL_NAME, Constants.KafkaConsumer.ISOLATION_LEVEL_VAL);
	  	fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.NUM_CONSUMERS_NAME, Constants.KafkaConsumer.DEF_NUM_CONSUMERS_VAL);
	  	fillEmptyPropWithDefVal(consumerProps, Constants.KafkaConsumer.AUTO_OFFSET_RESET_CONFIG_NAME, Constants.KafkaConsumer.DEF_AUTO_OFFSET_RESET_CONFIG_VAL);
	  	//优先加载序列化类，避免底层使用线程的ClassLoader加载找不到类的情况
		if (StringUtils.isEmpty(consumerProps.getProperty(Constants.KafkaConsumer.BOOTSTRAP_SERVERS_NAME))) {
			throw new IllegalArgumentException("the value of param 'bootstrap.servers' is null!");
		}
		if (StringUtils.isEmpty(consumerProps.getProperty(Constants.KafkaConsumer.ZOOKEEPER_CONNECT_NAME))) {
			throw new IllegalArgumentException("the value of param 'zkUrl' is null!");
		}
	  	if (consumerProps.containsKey(Constants.KafkaConsumer.NEW_KEY_DESERIALIZER_NAME)) {
	  		Object keyDeserializer = consumerProps.get(Constants.KafkaConsumer.NEW_KEY_DESERIALIZER_NAME);
	  		if (keyDeserializer instanceof String) {
	  			try {
		  			keyDeserializer = Class.forName(keyDeserializer.toString(), true, Utils.getKafkaClassLoader());
		  			consumerProps.put(Constants.KafkaConsumer.NEW_KEY_DESERIALIZER_NAME, keyDeserializer);
	  			} catch (ClassNotFoundException e) {
	  	            throw new ConfigException(Constants.KafkaConsumer.NEW_KEY_DESERIALIZER_NAME, keyDeserializer, "Class " + keyDeserializer + " could not be found.");
	  	        }
	  		}
	  	} else {
	  		consumerProps.put(Constants.KafkaConsumer.NEW_KEY_DESERIALIZER_NAME, StringDeserializer.class);
	  	}
	  	if (consumerProps.containsKey(Constants.KafkaConsumer.NEW_VALUE_DESERIALIZER_NAME)) {
	  		Object valueDeserializer = consumerProps.get(Constants.KafkaConsumer.NEW_VALUE_DESERIALIZER_NAME);
	  		if (valueDeserializer instanceof String) {
	  			try {
	  				valueDeserializer = Class.forName(valueDeserializer.toString(), true, Utils.getKafkaClassLoader());
	  				consumerProps.put(Constants.KafkaConsumer.NEW_VALUE_DESERIALIZER_NAME, valueDeserializer);
	  			} catch (ClassNotFoundException e) {
	  				throw new ConfigException(Constants.KafkaConsumer.NEW_VALUE_DESERIALIZER_NAME, valueDeserializer, "Class " + valueDeserializer + " could not be found.");
	  			}
	  		}
	  	} else {
	  		consumerProps.put(Constants.KafkaConsumer.NEW_VALUE_DESERIALIZER_NAME, ByteArrayDeserializer.class);
	  	}
	  	
	  	Object maxWaitObj = consumerProps.getOrDefault(Constants.KafkaConsumer.FETCH_MAX_WAIT_MS_CONFIG, Constants.KafkaConsumer.DEF_FETCH_MAX_WAIT_MS_CONFIG_VAL);
		try {
			this.pollMaxWait = Integer.parseInt(maxWaitObj.toString());
		} catch (Exception e) {
			logger.error("The value configuration of the key[{}] is incorrect, the default value will be used[{}]", 
					Constants.KafkaConsumer.FETCH_MAX_WAIT_MS_CONFIG, this.pollMaxWait);
		}
	}
	
	/**
	 * 初始化消费端的请求参数
	 * @param consumer
	 */
	public void resetRequestParams(KafkaConsumer<String, byte[]> consumer, int maxNums, long maxSize) {
		try {
			//如何限制poll大小，通过反射获取fetcher属性，并设置值
			Object fetcher = ReflectUtils.getFieldValue(consumer, "fetcher");
			if (fetcher != null) {
				ReflectUtils.setFieldValue(fetcher, "maxPollRecords", maxNums);
				ReflectUtils.setFieldValue(fetcher, "maxBytes", maxSize);
			}
		} catch (Exception e) {}
	}

	public KafkaConsumer<String, byte[]> getConsumer(ConsumerPartition task) {
        return this.getConsumer(task.getFromTopic(), task.getFromPartition());
	}
	private KafkaConsumer<String, byte[]> getConsumer(String topic, Integer partition) {
		String consumerKey = this.generateConsumerKey(topic, partition);
		return taskConsumerMap.get(consumerKey);
	}
	private KafkaConsumer<String, byte[]> setConsumer(ConsumerPartition task, KafkaConsumer<String, byte[]> consumer) {
		String consumerKey = this.generateConsumerKey(task.getFromTopic(), task.getFromPartition());
        return taskConsumerMap.put(consumerKey, consumer);
	}
	private KafkaConsumer<String, byte[]> removeConsumer(ConsumerPartition task) {
		String consumerKey = this.generateConsumerKey(task);
		return taskConsumerMap.remove(consumerKey);
	}
	
	public void start() {
		logger.info("starting consumer --> {}", consumerProps);
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
		logger.info("stoping consumer --> {}", consumerProps);
		this.isReady = false;
		for (Map.Entry<String, KafkaConsumer<String, byte[]>> entry : taskConsumerMap.entrySet()) {
			try {
				entry.getValue().close();
			} catch (Exception e) {}
		}
		taskConsumerMap.clear();
	}
	
	@Override
	public PollMessageResponse pollMessage(PollMessageRequest request) {
		ConsumerPartition task = request.getPartition();
		int maxNums = request.getMaxNums();
		long maxSize = request.getMaxSize();
		KafkaConsumer<String, byte[]> consumer = getConsumer(task);
		if (consumer == null) {
			return ResponseBuilder.toBuild(PollMessageResponse.class, ResponseCode.ConsumerClientNotFound);
        }
		TopicPartition tp = new TopicPartition(task.getFromTopic(), task.getFromPartition());
//		consumer.seek(tp, task.getStartOffset());
		Long startOffset = task.getStartOffset();
		if (startOffset == null) {
//			throw new BusinessException("startOffset is empty");
			return ResponseBuilder.toBuild(PollMessageResponse.class, ResponseCode.StartOffsetEmptyError);
		}
		logger.debug("taskCode[{}],topic[{}],partition[{}],offset[{}],maxPollRecords[{}],maxBytes[{}] poll message", task.getTaskCode(), tp.topic(), tp.partition(), startOffset, maxNums, maxSize);
		ConsumerRecords<String, byte[]> records = null;
		List<ConsumerRecord<String, byte[]>> recordList = null;
		this.resetRequestParams(consumer, maxNums, maxSize);
		//消费者由消费线程池处理，提交位置点由生产线程池触发，导致在同一个consumer里，出现多个线程执行操作，最终出现提示错误:KafkaConsumer is not safe for multi-threaded access
		consumerLock.lock();
		Long highWatermarkLong = null;
		try {
			records = consumer.poll(Duration.ofMillis(pollMaxWait));
			recordList = records.records(tp);
			if ((recordList != null && recordList.size() > 0) || task.getHighWatermark() == null) {//存在记录，则获取当前最大offset
				boolean fetchSuccess = false;
				//方式二：通过反射获取缓存的HighWatermark
				try {
					//通过反射获取fetcher属性
					Object fetcher = ReflectUtils.getFieldValue(consumer, "fetcher");
					if (fetcher != null) {
						Object subscriptions = ReflectUtils.getFieldValue(fetcher, "subscriptions");
						if (subscriptions != null) {
							java.lang.reflect.Method method = ReflectUtils.findMethod(subscriptions, "assignedStateOrNull", TopicPartition.class);
							Object topicPartitionState = ReflectUtils.invokeMethod(method, subscriptions, tp);
							if (topicPartitionState != null) {
								Object highWatermark = ReflectUtils.getFieldValue(topicPartitionState, "highWatermark");
								if (highWatermark != null) {
									highWatermarkLong = Long.parseLong(highWatermark.toString());
									fetchSuccess = true;
								}
							}
						}
					}
				} catch (Exception e) {}
				if (!fetchSuccess) {//反射未获取到值，通过broker获取
					try {
						//方式一：通过请求broker获取HighWatermark
						List<TopicPartition> tpList = new ArrayList<>();
						tpList.add(tp);
						Map<TopicPartition, Long> tpMap = consumer.endOffsets(tpList, Duration.ofMillis(pollMaxWait));
						if (tpMap != null) {
							Long maxOffset = tpMap.get(tp);
							if (maxOffset != null) {
								highWatermarkLong = maxOffset;
							}
						}
					} catch (Exception e) {}
				}
			}
		} finally {
			consumerLock.unlock();
		}
		List<MQMessage> retList = new ArrayList<>();
		Long endOffset = null;
		if (recordList != null) {
			for (ConsumerRecord<String, byte[]> record : recordList) {
				endOffset = record.offset();
				try {
					MQMessage cmsg = this.doConvert(task, record);//转换消息
	        		retList.add(cmsg);
	    		} catch (Exception e) {
	    			logger.error("kafka msg parse error", e);
	    		}
			}
		}
		PollMessageData data = new PollMessageData();
        data.setEndOffset(endOffset);
        data.setHighWatermark(highWatermarkLong);
        data.setMessages(retList);
        return ResponseBuilder.toBuild(PollMessageResponse.class, data);
	}
	
	/**
	 * 将kafka native消息对象转换成自定义消息对象
	 * @param message
	 * @return
	 */
	public MQMessage doConvert(ConsumerPartition task, ConsumerRecord<String, byte[]> record) throws Exception {
		 String topic = record.topic();
        long offset = record.offset();
        int partition = record.partition();
        String key = record.key();
        MQMessage msgExt = new MQMessage();
        msgExt.setMessageKey(key);
        Map<String, String> headers = new HashMap<>();
        if (record.headers() != null) {
            for (Iterator<Header> ite = record.headers().iterator(); ite.hasNext(); ) {
                Header header = ite.next();
                headers.put(header.key(), new String(header.value(), Constants.CHARSET_FORMAT));
            }
        }
        msgExt.setHeaders(headers);
        msgExt.setOffset(offset);
        msgExt.setTopic(topic);
        msgExt.setPartition(partition + "");
        msgExt.setMessageBytes(record.value());
        return msgExt;
	}
	
	public long getConsumerOffset(KafkaConsumer<String, byte[]> consumer, ConsumerPartition task) {
		long startOffset = this.getOffset(consumer, task, Constants.KafkaConsumer.EARLIEST_AUTO_OFFSET_RESET_VAL);
    	long endOffset = this.getOffset(consumer, task, Constants.KafkaConsumer.LATEST_AUTO_OFFSET_RESET_VAL);
    	long consumerOffset = this.getConsumerOffset(consumer, task, startOffset, endOffset);
    	return consumerOffset;
	}

	/**
	 * 获取消费者组的位置点
	 * @param consumer
	 * @param topic
	 * @param partition
	 * @return
	 */
	public long getConsumerOffset(KafkaConsumer<String, byte[]> consumer, ConsumerPartition task, long partitionStartOffset, long partitionEndOffset) {
		String taskCode = task.getTaskCode();
		String topic = task.getFromTopic();
		Integer partition = task.getFromPartition();
		String expectStartOffset = task.getExpectStartOffset();
		Long expectStartOffsetValue = task.getExpectStartOffsetValue();
		TopicPartition tp = new TopicPartition(topic, partition);
		OffsetAndMetadata om = consumer.committed(tp);
		if (om != null) {
			return om.offset();
		} else {
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
//				OffsetAndMetadata offsets = new OffsetAndMetadata(consumerOffset);
//				//对于未获取到位置点的，强制提交一次位置点
//				this.commitOffset(consumer, tp, offsets, taskCode, false, true);
//			} catch (Exception e) {
//				logger.error("taskId[{}],topic[{}],partition[{}],offset[{}] commit offset failure,{}", taskCode, topic, partition, consumerOffset, e.getMessage());
//			}
			return consumerOffset;
		}
	}
	/**
	 * 获取分区的offset范围
	 * @param consumer
	 * @param topic
	 * @param partition
	 * @param offsetReset
	 * @return
	 */
	public long getOffset(KafkaConsumer<String, byte[]> consumer, ConsumerPartition task, String offsetReset) {
		TopicPartition tp = new TopicPartition(task.getFromTopic(), task.getFromPartition());
		List<TopicPartition> partitions = new ArrayList<>();
		partitions.add(tp);
		//通过配置获取从那里开始消费
		if (StringUtils.isBlank(offsetReset)) {
			offsetReset = Constants.KafkaConsumer.DEF_AUTO_OFFSET_RESET_CONFIG_VAL;
		}
		Long retOffset = null;
		if (offsetReset.equalsIgnoreCase(Constants.KafkaConsumer.LATEST_AUTO_OFFSET_RESET_VAL)) {
			Map<TopicPartition, Long> map = consumer.endOffsets(partitions);
			if (map != null && map.containsKey(tp)) {
				retOffset = map.get(tp);
			}
		} else if (offsetReset.equalsIgnoreCase(Constants.KafkaConsumer.EARLIEST_AUTO_OFFSET_RESET_VAL)) {
			Map<TopicPartition, Long> map = consumer.beginningOffsets(partitions);
			if (map != null && map.containsKey(tp)) {
				retOffset = map.get(tp);
			}
		} else {
			throw new BusinessException("fetch offset error，Unrecognized parameter:" + Constants.KafkaConsumer.AUTO_OFFSET_RESET_CONFIG_NAME + "=" + offsetReset);
		}
		if (retOffset == null) {
			throw new BusinessException("can not found offset");
		}
		return retOffset;
	}
	
	/**
	 * 提交缓存的位置点
	 * @param consumer
	 * @param task
	 * @return
	 */
	public boolean commitCacheOffset(KafkaConsumer<String, byte[]> consumer, ConsumerPartition task, boolean failReQueue, boolean printLog) {
		TopicPartition tp = new TopicPartition(task.getFromTopic(), task.getFromPartition());
		cacheLock.lock();
		OffsetAndMetadata offsets = null;
		try {
			//拉取之前，先提交位置点
			offsets = cacheOffsets.remove(tp);
		} finally {
			cacheLock.unlock();
		}
		if (offsets == null) {
			return false;
		}
		return this.commitOffset(consumer, tp, offsets, task.getTaskCode(), failReQueue, printLog);
	}
	
	public boolean commitOffset(KafkaConsumer<String, byte[]> consumer, TopicPartition tp, OffsetAndMetadata offsets, String taskId, boolean failReQueue, boolean printLog) {
		if (offsets != null) {
			Map<TopicPartition, OffsetAndMetadata> offsetsMap = new HashMap<>();
			offsetsMap.put(tp, offsets);
			cacheOffsets.remove(tp);//将缓存的位点移除
			try {
				//消费者由消费线程池处理，提交位置点由生产线程池触发，导致在同一个consumer里，出现多个线程执行操作，最终出现提示错误:KafkaConsumer is not safe for multi-threaded access
				consumerLock.lock();
				try {
					consumer.commitSync(offsetsMap);
				} finally {
					consumerLock.unlock();
				}
				if (printLog) {
					logger.info("taskId[{}],topic[{}],partition[{}],offset[{}] commit success", taskId, tp.topic(), tp.partition(), offsets.offset());
				}
				return true;
			} catch (Exception e) {
				logger.error("taskId[{}],topic[{}],partition[{}],offset[{}] commit faliure, error:{}", taskId, tp.topic(), tp.partition(), offsets.offset(), e.getMessage());
				if (failReQueue) {
					cacheLock.lock();
					try {
						//重新放入队列前，先判断队列中是否存在新的位置点，如果不存在，则直接放入
						if (!cacheOffsets.containsKey(tp)) {
							logger.info("taskId[{}],topic[{}],partition[{}],offset[{}] put cache success", taskId, tp.topic(), tp.partition(), offsets.offset());
							cacheOffsets.put(tp, offsets);
							return true;
						} else {
							OffsetAndMetadata oldOffset = cacheOffsets.get(tp);
							if (oldOffset.offset() < offsets.offset()) {//如果已经存在未提交的位置点且小于当前位置点，直接覆盖
								logger.info("taskId[{}],topic[{}],partition[{}],offset[{}] put cache success", taskId, tp.topic(), tp.partition(), offsets.offset());
								cacheOffsets.put(tp, offsets);
								return true;
							}
						}
					} finally {
						cacheLock.unlock();
					}
				}
			}
		}
		return false;
	}
	
	@Override
	public CommitOffsetResponse commitOffset(CommitOffsetRequest request) {
		ConsumerPartition task = request.getPartition();
		Long offset = request.getOffset();
		TopicPartition tp = new TopicPartition(task.getFromTopic(), task.getFromPartition());
		OffsetAndMetadata offsets = new OffsetAndMetadata(offset);
		KafkaConsumer<String, byte[]> consumer = getConsumer(task);
		this.commitOffset(consumer, tp, offsets, task.getTaskCode(), true, false);
		return ResponseBuilder.toBuild(CommitOffsetResponse.class, new Object());
	}

	@Override
	public SubscribeResponse subscribe(SubscribeRequest request) {
		ConsumerPartition task = request.getPartition();
		try {
			TopicPartition tp = new TopicPartition(task.getFromTopic(), task.getFromPartition());
			KafkaConsumer<String, byte[]> consumer = getConsumer(task);
			if (consumer != null) {
				if (!consumer.assignment().contains(tp)) {
					try {
						consumer.assign(Arrays.asList(tp));//指定分区消费
					} catch (Exception e) {
						logger.error("kafka2.3 consumer assign partition error --> " + consumerProps + "-->" + task.getFromTopic() + "[" + task.getFromPartition() + "]", e);
						return ResponseBuilder.toBuild(SubscribeResponse.class, ResponseCode.AssignPartitionError);
					}
				}
			} else {
				try {
					consumer = new KafkaConsumer<>(consumerProps);
					consumer.assign(Arrays.asList(tp));//指定分区消费
					this.setConsumer(task, consumer);
				} catch (Exception e) {
					logger.error("create kafka2.3 consumer error --> " + consumerProps + "-->" + task.getFromTopic() + "[" + task.getFromPartition() + "]", e);
					return ResponseBuilder.toBuild(SubscribeResponse.class, ResponseCode.CreateConsumerError);
				}
			}
			SubscribeData data = new SubscribeData();
			long startOffset = this.getOffset(consumer, task, Constants.KafkaConsumer.EARLIEST_AUTO_OFFSET_RESET_VAL);
	        long endOffset = this.getOffset(consumer, task, Constants.KafkaConsumer.LATEST_AUTO_OFFSET_RESET_VAL);
	        data.setStartOffset(startOffset);
        	data.setEndOffset(endOffset);
	        logger.info("taskCode[{}],topic[{}],partition[{}],offset[{}] start consumer", task.getTaskCode(), task.getFromTopic(), task.getFromPartition(), startOffset);
			return ResponseBuilder.toBuild(SubscribeResponse.class, data);
		} catch (Exception e) {
			logger.error("create kafka2.3 consumer error --> " + consumerProps + "-->" + task.getFromTopic() + "[" + task.getFromPartition() + "]", e);
			return ResponseBuilder.toBuild(SubscribeResponse.class, ResponseCode.CreateConsumerError);
		}
	}

	@Override
	public SubscribeResponse resubscribe(SubscribeRequest request) {
		this.unsubscribe(new UnsubscribeRequest(request.getPartition()));
		return this.subscribe(request);
	}

	@Override
	public UnsubscribeResponse unsubscribe(UnsubscribeRequest request) {
		ConsumerPartition task = request.getPartition();
		KafkaConsumer<String, byte[]> consumer = removeConsumer(task);
		if (consumer != null) {
			consumerLock.lock();
			try {
				commitCacheOffset(consumer, task, false, true);
				consumer.close();
			} finally {
				consumerLock.unlock();
			}
		}
		return ResponseBuilder.toBuild(UnsubscribeResponse.class, new SubscribeData());
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
	public FetchOffsetResponse fetchOffset(FetchOffsetRequest request) {
		ConsumerPartition task = request.getPartition();
		KafkaConsumer<String, byte[]> consumer = this.getConsumer(task);
        if (consumer == null) {
        	return ResponseBuilder.toBuild(FetchOffsetResponse.class, ResponseCode.ConsumerClientNotFound);
        }
        long offset = this.getConsumerOffset(consumer, task);
        FetchOffsetData data = new FetchOffsetData();
        data.setOffset(offset);
		return ResponseBuilder.toBuild(FetchOffsetResponse.class, data);
	}

	@Override
	public FetchOffsetRangeResponse fetchOffsetRange(FetchOffsetRangeRequest request) {
		ConsumerPartition task = request.getPartition();
		KafkaConsumer<String, byte[]> consumer = this.getConsumer(task);
        if (consumer == null) {
        	return ResponseBuilder.toBuild(FetchOffsetRangeResponse.class, ResponseCode.ConsumerClientNotFound);
        }
        long startOffset = this.getOffset(consumer, task, Constants.KafkaConsumer.EARLIEST_AUTO_OFFSET_RESET_VAL);
    	long endOffset = this.getOffset(consumer, task, Constants.KafkaConsumer.LATEST_AUTO_OFFSET_RESET_VAL);
        FetchOffsetRangeData data = new FetchOffsetRangeData();
        data.setStartOffset(startOffset);
    	data.setEndOffset(endOffset);
		return ResponseBuilder.toBuild(FetchOffsetRangeResponse.class, data);
	}
}
