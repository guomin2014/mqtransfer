package com.gm.mqtransfer.worker.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongUnaryOperator;

import org.slf4j.Logger;

import com.gm.mqtransfer.facade.exception.BusinessException;
import com.gm.mqtransfer.facade.filter.ConvertChain;
import com.gm.mqtransfer.facade.filter.FilterChain;
import com.gm.mqtransfer.facade.handler.LoggerHandler;
import com.gm.mqtransfer.facade.model.CustomMessage;
import com.gm.mqtransfer.facade.model.ProducerConfig;
import com.gm.mqtransfer.facade.model.TaskPartitionStatInfo;
import com.gm.mqtransfer.facade.model.TaskShardingConfig;
import com.gm.mqtransfer.facade.service.alarm.AlarmService;
import com.gm.mqtransfer.module.task.service.TaskService;
import com.gm.mqtransfer.provider.facade.api.ResponseCode;
import com.gm.mqtransfer.provider.facade.api.producer.CheckMessageBatchRequest;
import com.gm.mqtransfer.provider.facade.api.producer.CheckMessageRequest;
import com.gm.mqtransfer.provider.facade.api.producer.CheckMessageResponse;
import com.gm.mqtransfer.provider.facade.api.producer.SendMessageAsyncRequest;
import com.gm.mqtransfer.provider.facade.api.producer.SendMessageAsyncResponse;
import com.gm.mqtransfer.provider.facade.api.producer.SendMessageResult;
import com.gm.mqtransfer.provider.facade.api.producer.SubscribeRequest;
import com.gm.mqtransfer.provider.facade.api.producer.SubscribeResponse;
import com.gm.mqtransfer.provider.facade.api.producer.UnsubscribeRequest;
import com.gm.mqtransfer.provider.facade.api.producer.UnsubscribeResponse;
import com.gm.mqtransfer.provider.facade.common.Constants;
import com.gm.mqtransfer.provider.facade.model.ClusterInfo;
import com.gm.mqtransfer.provider.facade.model.ProducerPartition;
import com.gm.mqtransfer.provider.facade.service.producer.ProducerService;
import com.gm.mqtransfer.provider.facade.service.producer.ResultMetadata;
import com.gm.mqtransfer.provider.facade.service.producer.SendCallback;
import com.gm.mqtransfer.provider.facade.service.producer.SendResult;
import com.gm.mqtransfer.provider.facade.util.StringUtils;
import com.gm.mqtransfer.worker.config.CommonConfiguration;
import com.gm.mqtransfer.worker.config.CommonProducerConfiguration;
import com.gm.mqtransfer.worker.config.ProducerConfiguration;
import com.gm.mqtransfer.worker.core.SpringContextWorkerUtils;
import com.gm.mqtransfer.worker.model.PollCacheRequest;
import com.gm.mqtransfer.worker.model.PollCacheResponse;
import com.gm.mqtransfer.worker.model.SendRequest;
import com.gm.mqtransfer.worker.model.TaskSharding;
import com.gm.mqtransfer.worker.service.cache.DataCacheService;
import com.gm.mqtransfer.worker.service.cache.StatService;

/**
 * 生产者处理类
 * @author GM
 * @date 2023-05-10 17:46:07
 *
 */
public class ProducerHandler extends LoggerHandler {
	
	private final TaskSharding task;
	private final ProducerService<CustomMessage> producer;
	/** 初始化失败次数 */
	private AtomicInteger initFailureCount = new AtomicInteger(0);
	/** 连续发送失败次数 */
	private AtomicInteger sendFailureCount = new AtomicInteger(0);
	private final int maxInitFailureCount = 10;
	private final int maxSendFailureCount = 10;
	private int producerVersion = 0;
	/** 生产消息条数计数器 */
	private AtomicLong producerMessageCountCounter = new AtomicLong(0);
	/** 生产消息大小计数器 */
	private AtomicLong producerMessageSizeCounter = new AtomicLong(0);
	/** 过滤消息条数计数器 */
	private AtomicLong filterMessageCountCounter = new AtomicLong(0);
	/** 过滤消息大小计数器 */
	private AtomicLong filterMessageSizeCounter = new AtomicLong(0);
	/** 消息上报最后发送时间 */
	private long lastSendMetricsTime = 0;
	/** 是否需要重新初始化任务 */
	private volatile boolean needReInitProducer = false;
	private volatile CountDownLatch sendingCountDown = null;
	/** 是否可执行 */
	private volatile boolean runnable = false;
	private volatile ReentrantLock sendLock = new ReentrantLock();
	private ProducerConfiguration producerConfiguration;
	private CommonConfiguration commonConfiguration;
	private AlarmService alarmService;
	private DataCacheService dataCacheService;
	private StatService statService;
	private TaskService taskService;
	
	public ProducerHandler(TaskSharding task, CommonConfiguration commonConfiguration, Logger logger) {
		super(logger, task.toToString());
		this.task = task;
		this.producer = task.getProducer();
		this.producerVersion = producer.getCurrentEpoch();
		this.commonConfiguration = commonConfiguration;
		this.dataCacheService = SpringContextWorkerUtils.getBean(DataCacheService.class);
		this.statService = SpringContextWorkerUtils.getBean(StatService.class);
		this.alarmService = SpringContextWorkerUtils.getBean(AlarmService.class);
		this.taskService = SpringContextWorkerUtils.getBean(TaskService.class);
		this.producerConfiguration = SpringContextWorkerUtils.getBean(ProducerConfiguration.class);
		this.runnable = true;
	}
	
	public boolean isReady() {
		return this.runnable && this.producer.isReady();
	}
	
	public Long getMaxSuspendTime() {
		ProducerConfig producerConfig = this.task.getTaskShardingConfig().getProducerConfig();
		CommonProducerConfiguration consumerConfiguration = this.commonConfiguration.getProducer();
		Long maxSuspendTimeMs = producerConfig.getMaxSuspendTime();
		if (maxSuspendTimeMs == null || maxSuspendTimeMs.longValue() == 0) {
			maxSuspendTimeMs = consumerConfiguration.getMaxSuspendTimeForProducer();
		}
		return maxSuspendTimeMs;
	}
	public Long getSendTimeoutMs() {
		ProducerConfig producerConfig = this.task.getTaskShardingConfig().getProducerConfig();
		CommonProducerConfiguration consumerConfiguration = this.commonConfiguration.getProducer();
		Long maxSuspendTimeMs = producerConfig.getSendTimeoutMs();
		if (maxSuspendTimeMs == null || maxSuspendTimeMs.longValue() == 0) {
			maxSuspendTimeMs = consumerConfiguration.getSendMessageTimeMaxMsForProducer();
		}
		return maxSuspendTimeMs;
	}
	
	/**
	 * 创建一个Poll请求对象
	 * @return
	 */
	public PollCacheRequest generatePollRequest() {
		ClusterInfo cluster = this.producer.getClusterInfo();
		//根据批量发送大小计算允许最大行数
		int maxBatchRecords = cluster.getReceiveMaxRecords() != null ? cluster.getReceiveMaxRecords().intValue() : producerConfiguration.getMaxBatchRecordsForProducer();//获取当前生产者允许发送的最大行数
		long maxSingleSize = cluster.getSingleMessageMaxBytes() != null ? cluster.getSingleMessageMaxBytes().longValue() : producerConfiguration.getMaxSingleSizeForProducer();//每行最大数据量
		long maxBatchSize = cluster.getReceiveMaxBytes() != null ? cluster.getReceiveMaxBytes().longValue() : producerConfiguration.getMaxBatchSizeForProducer();
		int maxTTL = producerConfiguration.getMaxTTLForProducer();//消息最大转发次数
		PollCacheRequest request = new PollCacheRequest(task);
		request.setMaxSingleSize(maxSingleSize);
		request.setMaxBatchRecords(maxBatchRecords);
		request.setMaxBatchSize(maxBatchSize);
		request.setMaxTTL(maxTTL);
		return request;
	}
	
	private void checkReInitTask(String errMsg) {
		//持续发送失败到达N次，清空缓存，重新初始化生产端
		if (sendFailureCount.incrementAndGet() >= maxSendFailureCount) {
			sendFailureCount.set(0);
			printInfoLogForForce("it fails for {} consecutive times, it will be reinitialized", maxSendFailureCount);
			if (producerConfiguration.isSendMessageFailAlarmEnableForProducer()) {
				if (StringUtils.isBlank(errMsg)) {
					errMsg = "unknow exception";
				}
				if (errMsg.length() > 100) {
					errMsg = errMsg.substring(0, 100);
				}
				alarmService.alarm("producer more than " + maxSendFailureCount + " failed attempts, cluster info:" + this.producer.getClusterInfo() + ", error:" + errMsg);
			}
			this.needReInitProducer = true;
		}
	}
	/**
	 * 检查是否可以发送消息
	 * @param task
	 * @return
	 */
	public boolean canSendMessage() {
		if (!this.checkOrInitProducer()) {
			return false;
		}
		if (!this.checkOrInitTask()) {
			return false;
		}
		return true;
	}
	
	/**
	 * 检查生产者是否准备好
	 * @return
	 */
	public boolean checkOrInitProducer() {
		//检查生产者是否可用（初始化任务时，允许生产者创建失败（比如网络异常导致），故在发送消息过程中，检查是否可用）（包含是否创建连接，是否映射分区）
		if (!this.producer.isReady() || needReInitProducer) {
			//持续发送失败到达N次，重新初始化生产端
			printErrorLogForForce("Task for producer not initialized OR send message failure, it will be reinitialized");
			if (initFailureCount.incrementAndGet() >= maxInitFailureCount) {
				printErrorLogForForce("Task for producer initialized more than {} failed attempts, restart producer and task, cluster info:{}", maxInitFailureCount, this.producer.getClusterInfo());
				initFailureCount.set(0);
				if (producerConfiguration.isInitFailAlarmEnableForProducer()) {
					alarmService.alarm("Task for producer initialized more than " + maxInitFailureCount + " failed attempts, restart producer and task, cluster info:" + this.producer.getClusterInfo());
				}
				this.producer.restart(this.producerVersion + 1);
				this.producerVersion = this.producer.getCurrentEpoch();
			} else {
				this.producer.start();
			}
			task.setInitSubscribeForProducer(false);
			this.needReInitProducer = false;
			return false;
		}
		return true;
	}
	/**
	 * 检查任务是否准备好
	 * @param task				分区任务
	 * @param restartProducer	是否重启producer客户端
	 * @param callback			方法回调
	 * @return
	 */
	public boolean checkOrInitTask() {
		if (!this.producer.isReady()) {
			return false;
		}
		//检查生产者是否可用（初始化任务时，允许生产者创建失败（比如网络异常导致），故在发送消息过程中，检查是否可用）（包含是否创建连接，是否映射分区）
		if (!task.isInitSubscribeForProducer()) {
			printErrorLogForForce("Task for producer not initialized, Unable to send message");
			ProducerPartition partition = task.getProducerPartition();
			SubscribeResponse response = null;
			//持续发送失败到达N次，重新初始化生产端
			if (task.incrementAndGetInitFailureCountForProducer() >= maxInitFailureCount) {
				printErrorLogForForce("Task for producer initialized more than {} failed attempts, restart producer and task, task info:{}", maxInitFailureCount, partition.getToDesc());
				task.resetInitFailureCountForProducer();
				if (producerConfiguration.isInitFailAlarmEnableForProducer()) {
					alarmService.alarm("Task for producer initialized more than " + maxInitFailureCount + " failed attempts, restart producer and task, task info:" + partition.getToDesc());
				}
				this.needReInitProducer = false;
				return false;
//				response = this.producer.resubscribe(new SubscribeRequest(partition));
			} else {
				response = this.producer.subscribe(new SubscribeRequest(partition));
			}
			boolean subscribeResult = response != null && response.isSuccess();
			if (subscribeResult) {
				task.setInitSubscribeForProducer(true);//更新订阅状态
			}
			return subscribeResult;
		}
		return true;
	}
	public boolean hasWaitSendData()  {
		int size = dataCacheService.getQueueCount(task);
		return size > 0;
	}
	public void doStopTask() {
		this.runnable = false;
		boolean normalEnd = this.tryWaitCompeleteSendMessage(31, TimeUnit.SECONDS);
		if (!normalEnd) {
			this.printErrorLogForForce("Producer failed end after 60 seconds，{}", this.task.toString());
		}
		TaskShardingConfig taskConfig = task.getTaskShardingConfig();
		this.printInfoLogForForce("last commit offset,taskId[{}],topic[{}],partition[{}],succOffset[{}],failOffset[{}]", 
				taskConfig.getTaskCode(), taskConfig.getFromTopic(), taskConfig.getFromPartition(), task.getCommitOffset(), task.getLastCommitOffsetForFail());
		try {
			UnsubscribeResponse response = this.producer.unsubscribe(new UnsubscribeRequest(task.getProducerPartition()));
			if (!response.isSuccess()) {
				this.printErrorLogForForce("stop producer task error, code:{}, msg:{}", response.getCode(), response.getMsg());
			}
		} catch (Exception e) {
			this.printErrorLogForForce("stop producer task error", e);
		}
		this.maybeReportMetrics(true);
	}
	/**
	 * 从缓存中拉取消息
	 * @param task
	 * @param maxBatchRecords
	 * @param maxSingleSize
	 * @param maxBatchSize
	 * @param maxTTL
	 * @param callback
	 * @return
	 */
	public PollCacheResponse pollMessages(PollCacheRequest pollRequest) {
		TaskSharding task = pollRequest.getTask(); 
		int maxBatchRecords = pollRequest.getMaxBatchRecords();
		long maxSingleSize = pollRequest.getMaxSingleSize();
		long maxBatchSize = pollRequest.getMaxBatchSize();
		int maxTTL = pollRequest.getMaxTTL();
		printInfoLog("starting poll record from cache, maxRecords:{},singleSize:{},batchSize:{}", maxBatchRecords, maxSingleSize, maxBatchSize);
		PollCacheResponse response = new PollCacheResponse(task);
		//RocketMQ不管是单条发送还是批量发送，消息总大小有限制，故在些单条消息取出并进行转换，后校验大小，直到满足条件后进行批量提交
		try {
			FilterChain filterChain = task.getFilterChain();
			ConvertChain convertChain = task.getConvertChain();
			while(response.getMessages().size() < maxBatchRecords) {
				if (!this.runnable) {
					break;
				}
				CustomMessage message = dataCacheService.poll(task);
				if (message == null) {
					break;
				}
				long messageMemorySize = this.countMessageSize(message);
				response.setPollMessageMaxOffset(message.getOffset());//更新本次拉取消息最大位点
				response.incrementPollMessageTotalMemorySizeAndCount(messageMemorySize, 1);
				if (filterChain != null && filterChain.doFilter(message)) {//不满足过滤条件
					response.incrementFilterMessageTotalMemorySizeAndCount(messageMemorySize, 1);
					continue;
				}
				//判断是否是循环消息
				if (this.checkLoopMessage(message, maxTTL)) {
					if (producerConfiguration.isSkipMessageAlarmEnableForProducer()) {
						alarmService.alarm("Illegal message by loop check, skip, task info:" + task.toString() + ", messageId:" + message.getMessageId());
					}
					printWarnLogForForce("Illegal message by loop check, skip it-->messageId:{},header:{}", message.getMessageId(), message.getHeaders());
					response.incrementFilterMessageTotalMemorySizeAndCount(messageMemorySize, 1);
					continue;
				}
				//消息转换
				if (convertChain != null) {
					message = convertChain.doConvert(message);
				}
				//对单条消息进行校验
				CheckMessageResponse checkResponse = this.producer.checkMessage(new CheckMessageRequest<>(task.getProducerPartition(), message));
				if (!checkResponse.isSuccess()) {
					if (producerConfiguration.isSkipMessageAlarmEnableForProducer()) {
						//消息不符合规范，比如长度超限，将丢弃，并告警
						alarmService.alarm("message size exceeded, skip, task info:" + task.toString() + ", messageId:" + message.getMessageId());
					}
					printWarnLogForForce("message size exceeded({}), skip it-->messageId:{},header:{}", maxSingleSize, message.getMessageId(), message.getHeaders());
					response.incrementFilterMessageTotalMemorySizeAndCount(messageMemorySize, 1);
					continue;
				}
				response.putMessage(message);
				//对批量消息进行校验
				checkResponse = this.producer.checkMessage(new CheckMessageBatchRequest<CustomMessage>(task.getProducerPartition(), response.getMessages()));
				if (!checkResponse.isSuccess()) {//批量消息长度超限
					if (response.getMessages().size() > 1) {//允许存在这样的设置：单条最大4M，但批量提交最多2M，这样当单条等于4M时，最少可以发送一条，当单条小于4M时，多条记录共2M就发送
						//移除最后一条消息
						response.rollbackMessage(message, messageMemorySize);
						//将最后一条消息重新发放队列
						dataCacheService.putHead(task, message);
					}
					break;
				}
			}
		} catch (Exception e) {
			printErrorLogForForce("poll message failure", e);
		}
		return response;
	}
	/**
	 * 检查是否是循环转发消息
	 * @param message
	 * @return
	 */
	public boolean checkLoopMessage(CustomMessage message, int maxTTL) {
		try {
			String fromReferer = String.format("%s%s%s", task.getTaskShardingConfig().getFromTopic(), Constants.MSG_REFERER_SEPARATOR, task.getTaskShardingConfig().getFromCluster().getCode());
			String toReferer = String.format("%s%s%s", task.getTaskShardingConfig().getToTopic(), Constants.MSG_REFERER_SEPARATOR, task.getTaskShardingConfig().getToCluster().getCode());
			try {
				Map<String, String> headers = message.getHeaders();
				if (headers != null && headers.containsKey(Constants.MSG_REFERER_HEADER)) {
					String refererString = headers.get(Constants.MSG_REFERER_HEADER);
					if (StringUtils.isBlank(refererString)) {
						refererString = "";
					}
					String[] refererArr = refererString.split(Constants.MSG_REFERER_SOURCE_SEPARATOR);
					boolean retrySend = false;//是否是重发
					int ttl = 0;//转发次数
					//判断是否是循环转发
					for (String fromRefer : refererArr) {
						if (fromRefer.equals(toReferer)) {
							return true;
						}
						if (fromRefer.equals(fromReferer)) {
							retrySend = true;
						} else {
							ttl++;
						}
					}
					if (ttl >= maxTTL) {//判断是否超过转发次数
						return true;
					}
					if (!retrySend) {//排除发送失败重发消息的场景
						StringBuilder builder = new StringBuilder(refererString);
						message.realAddHeaders(Constants.MSG_REFERER_HEADER, builder.append(Constants.MSG_REFERER_SOURCE_SEPARATOR_STR).append(fromReferer).toString());
					}
				} else {
					message.realAddHeaders(Constants.MSG_REFERER_HEADER, fromReferer);
				}
			} catch (Exception e) {
				printErrorLogForForce(String.format("checkLoopMessage error-->messageId:%s, header:%s", message.getMessageId(), message.getHeaders()), e);
				message.realAddHeaders(Constants.MSG_REFERER_HEADER, fromReferer);
			}
		} catch (Throwable e) {}
		return false;
	}
	/**
	 * 更新过滤消息缓存统计
	 * @param task
	 * @param response
	 */
	public void refreshFilterStat(PollCacheResponse response) {
		if (task == null || response == null) {
			return;
		}
		String taskCode = task.getTaskShardingConfig().getTaskCode();
		try {
			int filterMessageCount = response.getFilterMessageCount();
			long filterMessageTotalMemorySize = response.getFilterMessageTotalMemorySize();
			//更新缓存统计
			if (filterMessageCount != 0) {
				//减少待发送队列数量
				int size = dataCacheService.getQueueCount(task);
				printInfoLog("decrement sent queue size {} for filter, current total wait send queue size {}", filterMessageCount, size);
				statService.incrTotalFilterMsgCountForTask(taskCode, filterMessageCount);
				this.filterMessageCountCounter.addAndGet(filterMessageCount);
			}
			if (filterMessageTotalMemorySize != 0) {//表示从缓存中拉取了消息，但由于某些原因被丢弃，但缓存统计应该进行处理
				long totalUsedMemory = statService.decrMemoryUsedForTask(taskCode, filterMessageTotalMemorySize);
				printInfoLog("free memory {} byte for filter, current total used memory {} byte", filterMessageTotalMemorySize, totalUsedMemory);
				this.filterMessageSizeCounter.addAndGet(filterMessageTotalMemorySize);
			}
		} catch (Exception e) {
			printErrorLog("refresh filter stat failure", e);
		}
	}
	public long countMessageSize(CustomMessage msg) {
		if (msg == null) {
			return 0;
		}
		return msg.getTotalUsedMemorySize();
	}
	
	public long countMessageSize(List<CustomMessage> list) {
		if (list == null || list.isEmpty()) {
			return 0;
		}
		long total = 0;
		for (CustomMessage msg : list) {
			total += msg.getTotalUsedMemorySize();
		}
		return total;
	}
	/**
	 * 检查并提交位置点(从缓存中拉取了消息，但都被过滤了，需要将消费的位置点提交，避免任务转移后重复消费)
	 * @param task
	 * @param request
	 * @param callback
	 */
	public void checkAndCommitOffset(PollCacheResponse request) {
		if (task == null) {
			return;
		}
		// 从缓存中拉取了消息，但都被过滤了，需要将消费的位置点提交，避免任务转移后重复消费
		if (request != null && !request.hasMessage() && request.getPollMessageMaxOffset() >= 0) {
			commitOffset(request.getPollMessageMaxOffset() + 1);//提交的位置点为下一次要消费消息的开始点位置，所以+1
		}
	}
	/**
	 * 提交位置点
	 * @param lastSendMsg
	 */
	public void commitOffset(long offset) {
		if (task.getCommitOffset() != null && task.getCommitOffset().longValue() >= offset) {
			return;
		}
		task.setWaitCommitOffset(offset);
		TaskShardingConfig taskConfig = task.getTaskShardingConfig();
		printInfoLog("will commit offset,taskId[{}],topic[{}],partition[{}],oldOffset[{}],newOffset[{}]", 
				taskConfig.getTaskCode(), taskConfig.getFromTopic(), taskConfig.getFromPartition(), task.getCommitOffset(), offset);
//		ConsumerService consumerService = taskService.getTaskConsumerService(task);
//		//提交位置点，如果位置点一直提交失败，而任务转移或实例重启，将导致重复消费
//		CommitOffsetResponse response = consumerService.commitOffset(new CommitOffsetRequest(task.getConsumerPartition(), offset));
//		if (response.isSuccess()) {
//			printInfoLog("commit offset success,taskId[{}],topic[{}],partition[{}],oldOffset[{}],newOffset[{}]", 
//					task.getTaskCode(), task.getFromTopic(), task.getFromPartition(), task.getCommitOffset(), offset);
//			task.setCommitOffset(offset);
//			task.setCommitOffsetLastTime(System.currentTimeMillis());
//		} else {
//			printErrorLogForForce("commit offset failure,taskId[{}],topic[{}],partition[{}],oldOffset[{}],newOffset[{}]-->code:{},msg:{}", 
//					task.getTaskCode(), task.getFromTopic(), task.getFromPartition(), task.getCommitOffset(), offset, response.getCode(), response.getMsg());
//		}
	}
	
	public void handlerException(SendRequest request, Throwable e) {
		SendResult sendResultStat = new SendResult();
		PollCacheResponse cacheMessageBatch = request.getCacheMessageBatch();
		List<CustomMessage> messages = request.getMessages();
		printErrorLogForForce("producer send failure", e);
		if (!this.runnable) {
			return;
		}
		//发送失败，重新放入队列
		dataCacheService.putHead(task, messages);
		printInfoLogForForce("producer send failure, reput {} records into cache", messages.size());
		sendResultStat.setRetryMessageCount(cacheMessageBatch.getPollMessageCount() - cacheMessageBatch.getFilterMessageCount());
		sendResultStat.setRetryMessageTotalMemorySize(cacheMessageBatch.getPollMessageTotalMemorySize() - cacheMessageBatch.getFilterMessageTotalMemorySize());
		//持续发送失败到达N次，清空缓存，重新初始化生产端
		this.checkReInitTask(e.getMessage());
		//重置缓存统计
		this.refreshStat(request, sendResultStat);
		this.tryCompeleteSendMessage(request.getRequestTime());
	}
	
	public synchronized void handlerSendResult(SendRequest request, SendMessageResult<CustomMessage> sendResult) {
		if (sendResult == null) {
			printErrorLogForForce("producer send failure, can not found send result");
			return;
		}
		SendResult sendResultStat = new SendResult();
		try {
			PollCacheResponse cacheMessageBatch = request.getCacheMessageBatch();
			List<CustomMessage> messages = request.getMessages();
			if (sendResult.getSuccCount() > 0) {//存在发送成功的消息，将连续失败次数置为0
				sendFailureCount.set(0);
			}
			List<CustomMessage> retryList = new ArrayList<>();
			CustomMessage lastSendSuccessMessage = null;
			Map<CustomMessage, ResultMetadata> resultMap = sendResult.getResultMap();
			String lastErrorMsg = null;
			for (CustomMessage message : messages) {
				ResultMetadata meta = resultMap.get(message);
				if (meta == null || meta.getCode() == null) {//表示无响应
					retryList.add(message);
					continue;
				}
				ResponseCode code = ResponseCode.getByName(meta.getCode());
				if (code == ResponseCode.Success) {//发送成功
					lastSendSuccessMessage = message;
				} else if (this.sendMessageFailureIgnoreEnable(message, code)) {
					printWarnLogForForce("message send failure, skip it.-->[reason:{}]{}", meta.getMsg(), message.toString());
					sendResultStat.incrementIgnoreMessageTotalMemorySizeAndCount(message.getTotalUsedMemorySize(), 1);
					//无发送成功消息，或发送成功但位置点在当前消息位置点前面，将需要将位置点调整到当前忽略消息位置点，避免重复发送
					lastSendSuccessMessage = message;
				} else {//发送失败，重新放入队列
					retryList.add(message);
					lastErrorMsg = meta.getMsg();
				}
			}
			long diffTime = System.currentTimeMillis() - sendResult.getSendTime();
			boolean forcePrint = producerConfiguration.isSendMessageSlowPrintEnableForProducer() && diffTime >= producerConfiguration.getSendMessageSlowMaxMsForProducer();
			int noRspCount = sendResult.getTotalCount() - sendResult.getSuccCount() - sendResult.getErrorCount();
			printInfoLog(forcePrint, "producer sended {} records, success: {}, failure: {}, ignore: {}, noRsp: {}, time: {}", 
					sendResult.getTotalCount(), sendResult.getSuccCount(), sendResult.getErrorCount(), sendResultStat.getIgnoreMessageCount(), noRspCount, diffTime);
			if (lastSendSuccessMessage != null) {
				//提交消费端的位置点
				//获取最后一条消息的位置点，提交的位置点为下一次要消费消息的开始点位置，所以+1
				long offset = lastSendSuccessMessage.getOffset() + 1;
				if (sendResult.getErrorCount() == 0) {//不存在异常，则取拉取消息的最大位置点与发送消息的最大位点，取2个值的最大值提交（避免出现重复消费过滤了的消息）
					offset = Math.max(offset, cacheMessageBatch.getPollMessageMaxOffset());
				}
				this.commitOffset(offset);
			}
			//处理发送失败需要重发的消息
			if (retryList.size() > 0) {
				dataCacheService.putHead(task, retryList);
				printInfoLogForForce("reput {} records into cache", retryList.size());
				sendResultStat.setRetryMessageCount(retryList.size());
				sendResultStat.setRetryMessageTotalMemorySize(this.countMessageSize(retryList));
				retryList.clear();
				this.checkReInitTask(lastErrorMsg);
			}
			long sendMessageTotalMemorySize = cacheMessageBatch.getPollMessageTotalMemorySize() - cacheMessageBatch.getFilterMessageTotalMemorySize() 
					- sendResultStat.getRetryMessageTotalMemorySize() - sendResultStat.getIgnoreMessageTotalMemorySize();
			sendResultStat.setSendMessageCount(sendResult.getSuccCount());
			sendResultStat.setSendMessageTotalMemorySize(sendMessageTotalMemorySize);
		} finally {
			//重置缓存统计
			this.refreshStat(request, sendResultStat);
			this.tryCompeleteSendMessage(sendResult.getSendTime());
		}
	}
	/**
	 * 消息发送失败是否可忽略错误
	 * @param task
	 * @param message
	 * @param errCode
	 * @return
	 */
	private boolean sendMessageFailureIgnoreEnable(CustomMessage message, ResponseCode errCode) {
		if (errCode == null) {
			return false;
		}
		//获取忽略配置列表，判断是否存在
		return false;
	}
	
	public void refreshStat(PollCacheResponse request, SendResult sendResult) {
		if (task == null || request == null || sendResult == null) {
			return;
		}
		try {
			String taskCode = task.getTaskShardingConfig().getTaskCode();
//			SendRequest<CustomMessage> request = sendResult.getSendRequest();
			//由于过滤的消息已在拉取的时候进行了统计更新，故在此需要排序过滤相关的统计数据
			int decrCount = request.getPollMessageCount() - request.getFilterMessageCount() - sendResult.getRetryMessageCount();
			long decrSize = request.getPollMessageTotalMemorySize() - request.getFilterMessageTotalMemorySize() - sendResult.getRetryMessageTotalMemorySize();
			int sendMessageCount = sendResult.getSendMessageCount();
			int ignoreMessageCount = sendResult.getIgnoreMessageCount();
			//更新缓存统计
			if (decrCount > 0) {
				//减少待发送队列数量
				int size = dataCacheService.getQueueCount(task);
				this.producerMessageCountCounter.addAndGet(decrCount);
				printInfoLog("decrement sent queue size {}, current total wait send queue size {}", decrCount, size);
			}
			if (decrSize > 0) {
				long totalUsedMemory = statService.decrMemoryUsedForTask(taskCode, decrSize);
				this.producerMessageSizeCounter.addAndGet(decrSize);
				printInfoLog("free memory {} byte, current total used memory {} byte", decrSize, totalUsedMemory);
			}
			if (sendMessageCount > 0) {
				statService.incrTotalProducerMsgCountForTask(taskCode, sendMessageCount);
			}
			if (ignoreMessageCount > 0) {
				statService.incrTotalFilterMsgCountForTask(taskCode, ignoreMessageCount);
				this.filterMessageCountCounter.addAndGet(ignoreMessageCount);
				this.filterMessageSizeCounter.addAndGet(sendResult.getIgnoreMessageTotalMemorySize());
			}
		} catch (Exception e) {
			printErrorLog(this.isPrintLogEnable(), task.toToString(), "refresh stat failure", e);
		}
	}
	public void refreshStat(SendRequest request, SendResult sendResult) {
		if (task == null || request == null || sendResult == null) {
			return;
		}
		try {
			String taskCode = task.getTaskShardingConfig().getTaskCode();
			PollCacheResponse cacheMessageBatch = request.getCacheMessageBatch();
			int decrCount = cacheMessageBatch.getPollMessageCount() - sendResult.getRetryMessageCount();
			long decrSize = cacheMessageBatch.getPollMessageTotalMemorySize() - sendResult.getRetryMessageTotalMemorySize();
			int sendMessageCount = sendResult.getSendMessageCount();
			int ignoreMessageCount = cacheMessageBatch.getFilterMessageCount() + sendResult.getIgnoreMessageCount();//过滤的+发送失败忽略的
			//更新缓存统计
			if (decrCount > 0) {
				//减少待发送队列数量
				int size = dataCacheService.getQueueCount(task);
				this.producerMessageCountCounter.addAndGet(decrCount);
				printInfoLog("decrement sent queue size {}, current total wait send queue size {}", decrCount, size);
			}
			if (decrSize > 0) {
				long totalUsedMemory = statService.decrMemoryUsedForTask(taskCode, decrSize);
				this.producerMessageSizeCounter.addAndGet(decrSize);
				printInfoLog("free memory {} byte, current total used memory {} byte", decrSize, totalUsedMemory);
			}
			if (sendMessageCount > 0) {
				statService.incrTotalProducerMsgCountForTask(taskCode, sendMessageCount);
			}
			if (ignoreMessageCount > 0) {
				statService.incrTotalFilterMsgCountForTask(taskCode, ignoreMessageCount);
				this.filterMessageCountCounter.addAndGet(ignoreMessageCount);
				this.filterMessageSizeCounter.addAndGet(sendResult.getIgnoreMessageTotalMemorySize());
			}
		} catch (Exception e) {
			printErrorLog(this.isPrintLogEnable(), task.toToString(), "refresh stat failure", e);
		}
	}
	
	/**
	 * 上报统计指标数据
	 */
	public void maybeReportMetrics(boolean force) {
		long currTime = System.currentTimeMillis();
		if (!force && (currTime - this.lastSendMetricsTime < producerConfiguration.getReportIntervalTimeForProducer())) {
			return;
		}
		Long producerOffset = task.getWaitCommitOffset() != null ? task.getWaitCommitOffset() : task.getCommitOffset();
		Long maxOffset = task.getMaxOffset();
		Long lag = null;
		if (producerOffset != null && maxOffset != null) {
			lag = maxOffset.longValue() - producerOffset.longValue();
		}
		long totalFilterDataNum = this.filterMessageCountCounter.getAndUpdate(new LongUnaryOperator() {
			@Override
			public long applyAsLong(long operand) {
				return 0;
			}});
		long totalFilterDataSize = this.filterMessageSizeCounter.getAndUpdate(new LongUnaryOperator() {
			@Override
			public long applyAsLong(long operand) {
				return 0;
			}});
		long totalProducerDataNum = this.producerMessageCountCounter.getAndUpdate(new LongUnaryOperator() {
			@Override
			public long applyAsLong(long operand) {
				return 0;
			}});
		long totalProducerDataSize = this.producerMessageSizeCounter.getAndUpdate(new LongUnaryOperator() {
			@Override
			public long applyAsLong(long operand) {
				return 0;
			}});
		String taskCode = task.getTaskShardingConfig().getTaskCode();
		try {
			TaskPartitionStatInfo producerStat = new TaskPartitionStatInfo();
			producerStat.setSourcePartition(task.getTaskShardingConfig().getFromPartition().getPartitionKey());
			producerStat.setTargetPartition(task.getTaskShardingConfig().getToPartition().getPartitionKey());
			producerStat.setCommitOffset(producerOffset);
			producerStat.setProducerLag(lag);
			producerStat.setLastProducerTime(task.getLastProducerTime());
			producerStat.setTotalFilterCount(totalFilterDataNum);
			producerStat.setTotalFilterDataSize(totalFilterDataSize);
			producerStat.setTotalProducerCount(totalProducerDataNum);
			producerStat.setTotalProducerDataSize(totalProducerDataSize);
			taskService.refreshProducerStat(taskCode, producerStat);
			this.lastSendMetricsTime = currTime;
		} catch (Throwable ex) {
			printErrorLogForForce("upload producer metrics failure", ex);
			filterMessageCountCounter.addAndGet(totalFilterDataNum);
			filterMessageSizeCounter.addAndGet(totalFilterDataSize);
			producerMessageCountCounter.addAndGet(totalProducerDataNum);
			producerMessageSizeCounter.addAndGet(totalProducerDataSize);
		}
	}
	
	/**
	 * 发送消息
	 * @param task
	 * @param sendRequest
	 * @param sendCallback
	 * @param callback
	 */
	public void sendMessage(SendRequest sendRequest, SendCallback<CustomMessage> sendCallback) {
		if (this.trySendMessage()) {
			SendMessageAsyncResponse<CustomMessage> response = this.producer.sendMessage(
					new SendMessageAsyncRequest<CustomMessage>(sendRequest.getTask().getProducerPartition(), sendRequest.getMessages(), sendRequest.getMaxSendTimeoutMs(), sendCallback));
			if (!response.isSuccess()) {
				throw new BusinessException("send message failure," + response.getMsg());
			} else {
				sendRequest.getTask().setLastProducerTime(System.currentTimeMillis());
			}
		}
	}
	/**
	 * 是否正在发送消息
	 * @return
	 */
	public boolean isSendingMessage() {
		sendLock.lock();
		try {
			return sendingCountDown != null && sendingCountDown.getCount() > 0;
		} finally {
			sendLock.unlock();
		}
	}
	
	private boolean trySendMessage() {
		sendLock.lock();
		try {
			if (isSendingMessage()) {
				throw new BusinessException("Send message failure, A message is being sent.");
			}
			sendingCountDown = new CountDownLatch(1);
		} finally {
			sendLock.unlock();
		}
		return true;
	}
	private void tryCompeleteSendMessage(Long sendTime) {
		sendLock.lock();
		try {
			long usageTime = sendTime != null ? System.currentTimeMillis() - sendTime.longValue() : 0;
			if (sendingCountDown != null) {
				sendingCountDown.countDown();
				sendingCountDown = null;
				this.printInfoLog("Complete message sending, Release thread lock, time:" + usageTime);
			} else {
				this.printInfoLog("Complete message sending, but count not found thread lock, time:" + usageTime);
			}
		} finally {
			sendLock.unlock();
		}
	}
	
	private boolean tryWaitCompeleteSendMessage(long timeout, TimeUnit unit) {
		boolean normalEnd = true;
		sendLock.lock();
		try {
			if (sendingCountDown != null) {
				try {
					//如果线程正在进行数据发送或已发送，则等待提交位置点后才结束线程
					normalEnd = sendingCountDown.await(timeout, unit);
				} catch (Exception e) {
					normalEnd = false;
				}
				this.sendingCountDown = null;
			}
		} finally {
			sendLock.unlock();
		}
		return normalEnd;
	}
	
	public void alarm(String msg) {
		alarmService.alarm(msg);
	}
}
