package com.gm.mqtransfer.worker.handler;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongUnaryOperator;

import org.apache.lucene.util.RamUsageEstimator;
import org.slf4j.Logger;

import com.gm.mqtransfer.facade.common.util.DateUtils;
import com.gm.mqtransfer.facade.handler.LoggerHandler;
import com.gm.mqtransfer.facade.model.AlarmConfig;
import com.gm.mqtransfer.facade.model.ConsumerConfig;
import com.gm.mqtransfer.facade.model.CustomMessage;
import com.gm.mqtransfer.facade.model.TaskPartitionStatInfo;
import com.gm.mqtransfer.facade.model.TaskShardingConfig;
import com.gm.mqtransfer.facade.model.TransferPartitionConfig;
import com.gm.mqtransfer.facade.service.alarm.AlarmService;
import com.gm.mqtransfer.module.task.service.TaskService;
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
import com.gm.mqtransfer.provider.facade.api.Response;
import com.gm.mqtransfer.provider.facade.api.ResponseCode;
import com.gm.mqtransfer.provider.facade.api.SubscribeData;
import com.gm.mqtransfer.provider.facade.api.SubscribeRequest;
import com.gm.mqtransfer.provider.facade.api.SubscribeResponse;
import com.gm.mqtransfer.provider.facade.api.UnsubscribeRequest;
import com.gm.mqtransfer.provider.facade.api.UnsubscribeResponse;
import com.gm.mqtransfer.provider.facade.exception.BusinessException;
import com.gm.mqtransfer.provider.facade.model.ClusterInfo;
import com.gm.mqtransfer.provider.facade.model.ConsumerPartition;
import com.gm.mqtransfer.provider.facade.model.MQMessage;
import com.gm.mqtransfer.provider.facade.service.consumer.ConsumerService;
import com.gm.mqtransfer.worker.config.CommonConfiguration;
import com.gm.mqtransfer.worker.config.CommonConsumerConfiguration;
import com.gm.mqtransfer.worker.core.SpringContextWorkerUtils;
import com.gm.mqtransfer.worker.model.CacheBatch;
import com.gm.mqtransfer.worker.model.PollRequest;
import com.gm.mqtransfer.worker.model.PollResponse;
import com.gm.mqtransfer.worker.model.PutCacheResponse;
import com.gm.mqtransfer.worker.model.TaskSharding;
import com.gm.mqtransfer.worker.service.cache.DataCacheService;
import com.gm.mqtransfer.worker.service.cache.StatService;

public class ConsumerHandler extends LoggerHandler{

	private final ConsumerService consumer;
	private int consumerVersion = 0;
	/** 初始化失败次数 */
	private AtomicInteger initFailureCount = new AtomicInteger(0);
	/** 连续消费失败次数 */
	private AtomicInteger pollFailureCount = new AtomicInteger(0);
	private final int maxInitFailureCount = 10;
	private final int maxPollFailureCount = 10;
	private final int maxLockFailureCount = 10;
	/** 消息条数计数器 */
	private AtomicLong messageCountCounter = new AtomicLong(0);
	/** 消息大小计数器 */
	private AtomicLong messageSizeCounter = new AtomicLong(0);
	/** 消息上报最后发送时间 */
	private long lastSendMetricsTime = 0;
	/** 是否需要重新初始化任务 */
	private volatile boolean needReInitConsumer = false;
	/** 是否可运行 */
	private volatile boolean runnable = false;
	private DataCacheService dataCacheService;
	private StatService statService;
	private TaskService taskService;
	private AlarmService alarmService;
	private CommonConfiguration commonConfiguration;
	
	private final TaskSharding task;
	
	public ConsumerHandler(TaskSharding task, CommonConfiguration commonConfiguration, Logger logger) {
		super(logger, task.toFromString());
		this.runnable = true;
		this.task = task;
		this.consumer = task.getConsumer();
		this.consumerVersion = consumer.getCurrentEpoch();
		this.commonConfiguration = commonConfiguration;
//		this.dataCacheService = SpringContextUtils.getBean("com.gm.mqtransfer.worker", DataCacheService.class);
		this.dataCacheService = SpringContextWorkerUtils.getBean(DataCacheService.class);
		this.statService = SpringContextWorkerUtils.getBean(StatService.class);
		this.alarmService = SpringContextWorkerUtils.getBean(AlarmService.class);
		this.taskService = SpringContextWorkerUtils.getBean(TaskService.class);
	}
	/**
	 * Get the maximum size of the line message
	 * cluster config > task config > common config
	 * @return
	 */
	public Long getMaxSingleSize() {
		ClusterInfo clusterInfo = this.consumer.getClusterInfo();
		ConsumerConfig consumerConfig = this.task.getTaskShardingConfig().getConsumerConfig();
		CommonConsumerConfiguration consumerConfiguration = this.commonConfiguration.getConsumer();
		Long maxSingleSize = clusterInfo != null && clusterInfo.getSingleMessageMaxBytes() != null ? clusterInfo.getSingleMessageMaxBytes() : consumerConfig.getMaxSingleSize();
		if (maxSingleSize == null || maxSingleSize.longValue() == 0) {
			maxSingleSize = consumerConfiguration.getMaxSingleSizeForConsumer();
		}
		return maxSingleSize;
	}
	public Integer getMaxBatchRecords() {
		ClusterInfo clusterInfo = this.consumer.getClusterInfo();
		ConsumerConfig consumerConfig = this.task.getTaskShardingConfig().getConsumerConfig();
		CommonConsumerConfiguration consumerConfiguration = this.commonConfiguration.getConsumer();
		//每批最大行数
		Integer maxBatchRecords = clusterInfo != null && clusterInfo.getFetchMaxRecords() != null ? clusterInfo.getFetchMaxRecords(): consumerConfig.getMaxBatchRecords();
		if (maxBatchRecords == null || maxBatchRecords.intValue() == 0) {
			maxBatchRecords = consumerConfiguration.getMaxBatchRecordsForConsumer();
		}
		return maxBatchRecords;
	}
	public Long getMaxBatchSize() {
		ClusterInfo clusterInfo = this.consumer.getClusterInfo();
		ConsumerConfig consumerConfig = this.task.getTaskShardingConfig().getConsumerConfig();
		CommonConsumerConfiguration consumerConfiguration = this.commonConfiguration.getConsumer();
		Long maxBatchSize = clusterInfo != null && clusterInfo.getFetchMaxBytes() != null ? clusterInfo.getFetchMaxBytes() : consumerConfig.getMaxBatchSize();
		if (maxBatchSize == null || maxBatchSize.longValue() == 0) {
			maxBatchSize = consumerConfiguration.getMaxBatchSizeForConsumer();
		}
		return maxBatchSize;
	}
	public Long getMaxBatchWaitMs() {
		ClusterInfo clusterInfo = this.consumer.getClusterInfo();
		ConsumerConfig consumerConfig = this.task.getTaskShardingConfig().getConsumerConfig();
		CommonConsumerConfiguration consumerConfiguration = this.commonConfiguration.getConsumer();
		Long maxBatchWaitMs = clusterInfo != null && clusterInfo.getFetchMaxWaitMs() != null ? clusterInfo.getFetchMaxWaitMs() : consumerConfig.getMaxBatchWaitMs();
		if (maxBatchWaitMs == null || maxBatchWaitMs.longValue() == 0) {
			maxBatchWaitMs = consumerConfiguration.getMaxBatchWaitMsForConsumer();
		}
		return maxBatchWaitMs;
	}
	public Long getMaxSuspendTime() {
		ConsumerConfig consumerConfig = this.task.getTaskShardingConfig().getConsumerConfig();
		CommonConsumerConfiguration consumerConfiguration = this.commonConfiguration.getConsumer();
		Long maxBatchWaitMs = consumerConfig.getMaxSuspendTime();
		if (maxBatchWaitMs == null || maxBatchWaitMs.longValue() == 0) {
			maxBatchWaitMs = consumerConfiguration.getMaxSuspendTimeForConsumer();
		}
		return maxBatchWaitMs;
	}
	public Integer getMaxCacheRecords() {
		TransferPartitionConfig transferConfig = this.task.getTaskShardingConfig().getTransferPartitionConfig();
		CommonConsumerConfiguration consumerConfiguration = this.commonConfiguration.getConsumer();
		Integer maxCacheRecords = transferConfig != null ? transferConfig.getMaxCacheRecords() : null;
		if (maxCacheRecords == null || maxCacheRecords.intValue() == 0) {
			maxCacheRecords = consumerConfiguration.getMaxCacheRecordsForConsumer();
		}
		return maxCacheRecords;
	}
	/**
	 * 停止任务
	 * @param task
	 */
	public void doStopTask() {
		this.runnable = false;
		if (task == null) {
			return;
		}
		TaskShardingConfig taskShardingConfig = task.getTaskShardingConfig();
		String taskCode = taskShardingConfig.getTaskCode();
		printInfoLogForForce("stoping task...");
		try {
			//停止consumer
			UnsubscribeResponse response = this.consumer.unsubscribe(new UnsubscribeRequest(task.getConsumerPartition()));
			if (!response.isSuccess()) {
				printErrorLogForForce("stop consumer error, code:{}, msg:{}", response.getCode(), response.getMsg());
			}
		} catch (Exception e) {
			printErrorLogForForce("stop consumer task error", e);
		}
		printInfoLogForForce("last fetch offset,taskId[{}],topic[{}],partition[{}],fetchOffset[{}],maxOffset[{}]", 
				taskShardingConfig.getTaskCode(), taskShardingConfig.getFromTopic(), taskShardingConfig.getFromPartition().getPartitionKey(), task.getFetchOffset(), task.getMaxOffset());
		//清除任务对应的缓存数据
		CacheBatch batch = dataCacheService.cleanQueue(task);
		printInfoLogForForce("clean queue size: {}", batch == null ? 0 : batch.getMessageCount());
		//更新内存统计
		if (batch != null && batch.getMessageSize() != 0) {
			long currSize = statService.decrMemoryUsedForTask(taskCode, batch.getMessageSize());
			printInfoLogForForce("clean memory, queue size: {}, used memory size: {}, current total memory size: {}", batch.getMessageCount(), batch.getMessageSize(), currSize);
		}
		//更新统计指标数据
		this.maybeReportMetrics(true);
	}
	/**
	 * 创建一个Poll请求对象
	 * @return
	 */
	public PollRequest generatePollRequest() {
		Long maxSingleSize = this.getMaxSingleSize();
		Integer maxBatchRecords = this.getMaxBatchRecords();
		Long maxBatchSize = this.getMaxBatchSize();
		Long maxBatchWaitMs = this.getMaxBatchWaitMs();
		CommonConsumerConfiguration consumerConfiguration = this.commonConfiguration.getConsumer();
		//消费消息批量最大分区数
		int maxBatchPartitionNums = consumerConfiguration.getMaxBatchPartitionsForConsumer();
		//最大缓存行数
		int maxCacheRecords = this.getMaxCacheRecords();
		PollRequest request = new PollRequest(task);
		request.setMaxSingleSize(maxSingleSize);
		request.setMaxBatchRecords(maxBatchRecords);
		request.setMaxBatchSize(maxBatchSize);
		request.setMaxBatchWaitMs(maxBatchWaitMs);
		request.setMaxBatchPartitionNums(maxBatchPartitionNums);
		request.setMaxCacheRecords(maxCacheRecords);
		return request;
	}
	/**
	 * 统计消息占用内存大小
	 * @param obj
	 * @return
	 */
	public long countMessageSizeFromRam(Object obj) {
		if (obj == null) {
			return 0;
		}
//		//计算指定对象及其引用树上的所有对象的综合大小，单位字节
//		long RamUsageEstimator.sizeOf(Object obj)
//		//计算指定对象本身在堆空间的大小，单位字节
//		long RamUsageEstimator.shallowSizeOf(Object obj)
//		//计算指定对象及其引用树上的所有对象的综合大小，返回可读的结果，如：2KB
//		String RamUsageEstimator.humanSizeOf(Object obj)
		try {
			if (obj instanceof Collection<?>) {//不统计当前集合的占用内存，只统计集合中对象占用
				Collection<?> col = (Collection<?>)obj;
				Iterator<?> it = col.iterator();
				long total = 0;
				while (it.hasNext()) {
					Object o = it.next();
					total += RamUsageEstimator.sizeOf(o);
				}
				return total;
			} else {
				return RamUsageEstimator.sizeOf(obj);
			}
		} catch (Throwable e) {
			//TODO 按属性值计算大小并求和
			return 0;
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
	 * 是否超过缓存限制
	 * @param task
	 * @return
	 */
	public boolean isOutOfCache() {
		String resourceName = task.getTaskShardingConfig().getTaskCode().toString();
		//最大可用内存
		long maxAvaiableMemory = statService.getMaxAvaiableMemoryForTask(resourceName);
		//检查当前任务内存是否超限
		long usedMemory = statService.getUsedMemoryForTask(resourceName);
		//每行最大数据量
		Long maxSingleSize = this.getMaxSingleSize();
		long avaiableMemory = maxAvaiableMemory - usedMemory;
		if (avaiableMemory <= maxSingleSize) {
			return true;
		}
		double memoryRatio = commonConfiguration.getMessageQueueMemoryRatioForSingle();
		int curQueueSize = dataCacheService.getQueueCount(task);
		if (maxAvaiableMemory * memoryRatio <= usedMemory && curQueueSize > 0) {//任务堆积超过指定阈值，暂停消费
			return true;
		}
		int maxCacheRecords = this.getMaxCacheRecords();//最大缓存行数
		if (curQueueSize >= maxCacheRecords) {
			return true;
		}
		return false;
	}
	
	/**
	 * 获取任务当前可用内存
	 * @return
	 */
	public long getAvaiableMemory(PollRequest pollRequest) {
		//检查总内存是否超限
		boolean overLimit = statService.doCheckMemoryOverLimitForTotalUsed();
		if (overLimit) {
			throw new BusinessException("suspend consumer, not enough avaiable memory for total.");
		}
		TaskSharding task = pollRequest.getTask();
		if (task == null) {//表示共享任务获取可用内存
			//最大可用内存
			long maxAvaiableMemory = statService.getTotalAvaiableMemory();
			return maxAvaiableMemory;
		}
		String resourceName = task.getTaskShardingConfig().getTaskCode().toString();
		//最大可用内存
		long maxAvaiableMemory = statService.getMaxAvaiableMemoryForTask(resourceName);
		//检查当前任务内存是否超限
		long usedMemory = statService.getUsedMemoryForTask(resourceName);
		//每行最大数据量
		long maxSingleSize = this.getMaxSingleSize();
		long avaiableMemory = maxAvaiableMemory - usedMemory;
		if (avaiableMemory <= maxSingleSize) {
			throw new BusinessException(String.format("suspend consumer, not enough avaiable memory for task. max:%s,used:%s", maxAvaiableMemory, usedMemory));
		}
		double memoryRatio = commonConfiguration.getMessageQueueMemoryRatioForSingle();
		int curQueueSize = dataCacheService.getQueueCount(task);
//		int partitionNum = this.controller.getDataCacheService().countTotalPartition(resourceName);
		if (maxAvaiableMemory * memoryRatio <= usedMemory && curQueueSize > 0) {//任务堆积超过指定阈值，暂停消费
			throw new BusinessException(String.format("suspend consumer, Memory used exceeds %s of available memory for task. max:%s,used:%s,queueSize:%s", 
					memoryRatio * 100 + "%", maxAvaiableMemory, usedMemory, curQueueSize));
		}
		int maxCacheRecords = this.getMaxCacheRecords();
		if (curQueueSize >= maxCacheRecords) {
			throw new BusinessException(String.format("suspend consumer, Cache record exceeds the specified size for task. max:%s,used:%s", maxCacheRecords, curQueueSize));
		}
		return avaiableMemory;
	}
	/**
	 * 是否超出可用内存
	 * @return
	 */
	public boolean isOutOfAvaiableMemory() {
		try {
			//创建Poll对象
			PollRequest pollRequest = this.generatePollRequest();
			this.getAvaiableMemory(pollRequest);
			return false;
		} catch (Exception e) {
			return true;
		}
	}
	/**
	 * 锁定内存
	 * @param maxLockMemory
	 * @return
	 */
	public boolean tryLockMemory(PollRequest pollRequest) {
		String resourceName = pollRequest.getResourceName();
		//检查并获取可用内存
		long avaiableMemory = 0;
		try {
			avaiableMemory = getAvaiableMemory(pollRequest);
		} catch (Exception e) {
			printErrorLog("Failed to get available memory for task," + e.getMessage());
			return false;
		}
		//计算本次拉取请求的最大行数与大小
		long maxBatchSize = pollRequest.getMaxBatchSize();
		long maxSingleSize = pollRequest.getMaxSingleSize();
		int maxFetchRecords = 0;
		long maxFetchSize = 0;
		if (maxBatchSize >= avaiableMemory) {//可用内存不足
			maxFetchSize = avaiableMemory;
			maxFetchRecords = (int)(avaiableMemory / maxSingleSize);
		} else {
			maxFetchSize = maxBatchSize;
			maxFetchRecords = (int)(maxBatchSize / maxSingleSize);
		}
		if (maxFetchRecords <= 0) {
			printInfoLogForForce("Suspend consumer, Too little memory locked [{}], At least [{}] for one message.", maxFetchSize, maxSingleSize);
			return false;
		}
		//锁定内存
		int lockCount = 0;
		while (true) {
			if (!this.runnable) {
				return false;
			}
			try {
				lockCount++;
				//按最大拉取消息内存进行锁定
				long totalUsedMemory = statService.incrMemoryUsedForTask(resourceName, maxFetchSize);
				pollRequest.setMaxFetchRecords(maxFetchRecords);
				pollRequest.setMaxFetchSize(maxFetchSize);
				printInfoLog("lock memory {} byte, current total used memory {} byte", maxFetchSize, totalUsedMemory);
				return true;
			} catch (Exception e) {
				if (lockCount >= maxLockFailureCount) {
					printInfoLogForForce("lock memory failure more than {} failed attempts, stoped consumer-->{}", maxLockFailureCount, e.getMessage());
					return false;
				} else {
					printErrorLog("lock memory failure, num: {}-->{}", lockCount, e.getMessage());
				}
			}
			try {
				Thread.sleep(1);
			} catch (Exception e) {}
		}
	}
	/**
	 * 释放内存
	 * @param pollRequest
	 * @param realUsedMemory
	 * @param callback
	 * @return
	 */
	public boolean tryReleaseMemory(PollRequest pollRequest, long realUsedMemory) {
		long diffValue = pollRequest.getMaxFetchSize() - realUsedMemory;
		if (diffValue == 0) {
			return true;
		}
		String resourceName = pollRequest.getResourceName();
		//实际未占用到锁定内存大小，需要减去(不能使用可用内存大小判断)
		long totalUsedMemory = statService.decrMemoryUsedForTask(resourceName, diffValue);
		printInfoLog("{} memory {} byte, current total used memory {} byte", diffValue > 0 ? "free" : "increment", diffValue, totalUsedMemory);
		return true;
	}
	/**
	 * 检查任务是否可以开始消费
	 * @param task
	 * @return
	 */
	public boolean canPollMessage() {
		if (!this.checkOrInitConsumer()) {//消费者未准备好
			return false;
		}
		if (!this.checkOrInitTask()) {//任务未准备好
			return false;
		}
		if (!task.isProducerReady()) {//生产未准备好的情况下，先暂停消费
			printInfoLog("Task for producer not initialized, Unable to poll message-->{}", task.toToString());
			return false;
		}
		return true;
	}
	
	/**
	 * 检查消费者是否准备好
	 * @return
	 */
	public boolean checkOrInitConsumer() {
		//检查消费者是否可用（初始化任务时，允许消费者创建失败（比如网络异常导致），故在消费消息过程中，检查是否可用）
		if (!this.consumer.isReady() || needReInitConsumer) {
			// 持续发送失败到达N次，重新初始化生产端
			printInfoLogForForce("Task for consumer need to initializ-->{}", this.consumer.getClusterInfo().toSimpleString());
			if (initFailureCount.incrementAndGet() >= maxInitFailureCount) {
				printErrorLogForForce(
						"Task for consumer initialized more than {} failed attempts, restart consumer and task, cluster info:{}",
						maxInitFailureCount, this.consumer.getClusterInfo());
				initFailureCount.set(0);
				CommonConsumerConfiguration consumerConfiguration = this.commonConfiguration.getConsumer();
				if (consumerConfiguration.isInitFailAlarmEnableForConsumer()) {
					alarmService.alarm("Task for consumer initialized more than " + maxInitFailureCount
							+ " failed attempts, restart consumer and task, cluster info:" + this.consumer.getClusterInfo());
				}
				this.consumerVersion = this.consumer.restart(this.consumerVersion + 1);// 重新启动消费者
			} else {
				this.consumer.start();
			}
			task.setInitSubscribe(false);//需要重新订阅
			this.needReInitConsumer = false;
			return false;
		}
		return true;
	}
	/**
	 * 检查任务是否准备好(订阅并获取消费位点)
	 * @return
	 */
	public boolean checkOrInitTask() {
		if (!this.consumer.isReady()) {
			return false;
		}
		//检查消费者是否可用（初始化任务时，允许消费者创建失败（比如网络异常导致），故在消费消息过程中，检查是否可用）
		if (!task.isInitSubscribe() || !task.isInitFetchOffset()) {
			ConsumerPartition partition = task.getConsumerPartition();
			//持续发送失败到达N次，重新初始化生产端
			printInfoLogForForce("Task for consumer not initialized, will to initializ");
			SubscribeResponse subscribeResponse = null;
			if (task.incrementAndGetInitFailureCountForConsumer() >= maxInitFailureCount) {
				printErrorLogForForce("Task for consumer initialized more than {} failed attempts, restart consumer and task, task info:{}", maxInitFailureCount, task.toFromString());
				task.resetInitFailureCountForConsumer();
				CommonConsumerConfiguration consumerConfiguration = this.commonConfiguration.getConsumer();
				if (consumerConfiguration.isInitFailAlarmEnableForConsumer()) {
					alarmService.alarm("Task for consumer initialized more than " + maxInitFailureCount + " failed attempts, restart consumer and task, task info:" + task.toFromString());
				}
				this.needReInitConsumer = true;
				return false;
			} else {
				if (!task.isInitSubscribe()) {
					subscribeResponse = this.consumer.subscribe(new SubscribeRequest(partition));
				}
			}
			if (!task.isInitSubscribe()) {
				if (subscribeResponse != null && subscribeResponse.isSuccess()) {
					task.setInitSubscribe(true);//更新订阅状态
				} else {
					return false;
				}
			}
			if (!task.isInitFetchOffset()) {
				if (subscribeResponse != null && subscribeResponse.getData() != null 
						&& subscribeResponse.getData().getStartOffset() != null 
						&& subscribeResponse.getData().getEndOffset() != null
						&& subscribeResponse.getData().getConsumerOffset() != null) {
					SubscribeData subscribeData = subscribeResponse.getData();
					task.setMinOffset(subscribeData.getStartOffset());
					task.setMaxOffset(subscribeData.getEndOffset());
					task.setFetchOffset(subscribeData.getConsumerOffset());//更新消费开始位置点
					task.setCommitOffset(subscribeData.getConsumerOffset());
					task.setInitFetchOffset(true);
				} else {
					FetchOffsetRangeResponse fetchOffsetRangeResponse = this.consumer.fetchOffsetRange(new FetchOffsetRangeRequest(partition));
					if (fetchOffsetRangeResponse.isSuccess()) {
						FetchOffsetRangeData offsetRangeData = fetchOffsetRangeResponse.getData();
						if (offsetRangeData != null) {
							task.setMinOffset(offsetRangeData.getStartOffset());
							task.setMaxOffset(offsetRangeData.getEndOffset());
						}
					}
					FetchOffsetResponse fetchOffsetResponse = this.consumer.fetchOffset(new FetchOffsetRequest(partition));
					if (fetchOffsetResponse.isSuccess()) {
						FetchOffsetData fetchOffsetData = fetchOffsetResponse.getData();
						if (fetchOffsetData != null && fetchOffsetData.getOffset() != null) {
							long commitOffset = fetchOffsetData.getOffset();
							printInfoLogForForce("Task for consumer init success, minOffset:{}, maxOffset:{}, startOffset:{}", task.getMinOffset(), task.getMaxOffset(), commitOffset);
							task.setFetchOffset(commitOffset);//更新消费开始位置点
							task.setCommitOffset(commitOffset);
							task.setInitFetchOffset(true);
						} else {
							return false;
						}
					} else {
						return false;
					}
				}
			}
			return true;
		}
		return true;
	}
	/**
	 * 拉取消息
	 * @param task
	 * @param maxNums
	 * @param maxSize
	 * @param callback
	 * @return
	 */
	public PollResponse pollMessages(PollRequest pollRequest, String batchCode) {
		TaskSharding task = pollRequest.getTask();
		ConsumerPartition partition = task.getConsumerPartition();
		//指定开始消费的位置点
		partition.setStartOffset(task.getFetchOffset());
		PollResponse response = new PollResponse(task, batchCode);
		try {
			int maxNums = pollRequest.getMaxFetchRecords();
			long maxSize = pollRequest.getMaxFetchSize();
			long pollBeforeTime = System.currentTimeMillis();
			PollMessageResponse pollMessageResponse = this.consumer.pollMessage(new PollMessageRequest(partition, maxNums, maxSize));
			if (!pollMessageResponse.isSuccess()) {
				//针对异常的处理，比如offset out of range等
				this.handlerError(pollMessageResponse);
//				throw new BusinessException(pollMessageResponse.getMsg());
			}
			PollMessageData messageData = pollMessageResponse.getData();
			if (messageData != null) {
				if (messageData.getHighWatermark() != null) {//update the high water mark
					task.setMaxOffset(messageData.getHighWatermark());
				}
				List<MQMessage> list = messageData.getMessages();
				if (list != null && !list.isEmpty()) {
					task.setLastConsumerTime(System.currentTimeMillis());//update the time for consume
//					FilterChain filterChain = task.getFilterChain();
					//过滤消息
					for (MQMessage msg : list) {
						task.setFetchOffset(msg.getOffset() + 1);//update the next consumer offset
						CustomMessage cmsg = this.doConvertMessage(msg);
//						boolean transferEnable = filterChain == null || (filterChain != null && filterChain.doFilter(task, cmsg));
						response.addMessage(cmsg, true);
					}
					list.clear();
				}
			}
			CommonConsumerConfiguration consumerConfiguration = this.commonConfiguration.getConsumer();
			long usedTime = System.currentTimeMillis() - pollBeforeTime;
			boolean forcePrint = consumerConfiguration.isPollMessageSlowPrintEnableForConsumer() 
					&& usedTime >= consumerConfiguration.getPollMessageSlowMaxMsForConsumer() && response.getPollMessageCount() > 0;
			printInfoLog(forcePrint, "[{}] consumer poll {} records(size:{} byte, startOffset:{}, endOffset:{}) from broker, time:{}", 
					batchCode, response.getPollMessageCount(), response.getPollMessageSize(), response.getPollMessageMinOffset(), response.getPollMessageMaxOffset(), usedTime);
			pollFailureCount.set(0);
		} catch (Exception e) {
			printErrorLogForForce("consumer poll error[offset:" + task.getFetchOffset() + "]", e);
			if (pollFailureCount.incrementAndGet() >= maxPollFailureCount) {//持续失败到达N次，清空缓存，重新初始化消费端
				needReInitConsumer = true;
			}
		}
		return response;
	}
	private CustomMessage doConvertMessage(MQMessage msg) {
		CustomMessage cmsg = new CustomMessage(msg);
		long messageSize = this.countMessageSizeFromRam(msg);//统计当前对象内存占用大小
		cmsg.setTotalUsedMemorySize(messageSize);
		return cmsg;
	}
	private void handlerError(Response<?> response) {
		if (response != null && !response.isSuccess()) {
			printErrorLogForForce("consumer poll error[offset:{}]-->{}:{}", task.getFetchOffset(), response.getCode(), response.getMsg());
			ResponseCode responseCode = ResponseCode.getByName(response.getCode());
			if (responseCode == null) {
				throw new BusinessException(response.getCode() + ":" + response.getMsg());
			}
			switch (responseCode) {
			case OffsetOutOfRangeError://位置点超出范围，原因1：数据过期清除，现象：当前获取的位置点小于分区的最小位置点；原因2：位置点被重置，现象：当前获取位置点大于分区的最大位置点
				//获取分区的最小与最大位点
				FetchOffsetRangeRequest request = new FetchOffsetRangeRequest(task.getConsumerPartition());
				FetchOffsetRangeResponse fetchOffsetRangeResponse = this.consumer.fetchOffsetRange(request);
				if (fetchOffsetRangeResponse.isSuccess()) {
					FetchOffsetRangeData offsetRange = fetchOffsetRangeResponse.getData();
					Long fetchOffset = task.getFetchOffset();
					if (fetchOffset.longValue() < offsetRange.getStartOffset().longValue()) {
						task.setFetchOffset(offsetRange.getStartOffset());
					} else if (fetchOffset.longValue() > offsetRange.getEndOffset().longValue()) {
						task.setFetchOffset(offsetRange.getEndOffset());
					}
					printWarnLogForForce("consumer reset offset --> {} to {}", fetchOffset, task.getFetchOffset());
				}
				break;
			default:
				throw new BusinessException(responseCode.name() + ":" + responseCode.getMsg());
			}
		}
	}
	/**
	 * 检查是否需要提交位置点
	 * @param task
	 */
	public void maybeCommitOffset() {
		Long waitCommitOffset = task.getWaitCommitOffset();
		Long commitOffset = task.getCommitOffset();
		if (waitCommitOffset != null) {
			try {
				if (commitOffset != null && commitOffset.longValue() >= waitCommitOffset.longValue()) {//已提交的位置点晚于待提交的位置点，丢弃
					return;
				}
				CommitOffsetResponse response = this.consumer.commitOffset(new CommitOffsetRequest(task.getConsumerPartition(), waitCommitOffset));
				if (response.isSuccess()) {
					task.setCommitOffset(waitCommitOffset);
					task.setCommitOffsetLastTime(System.currentTimeMillis());
					this.printDebugLog("commit offset success, oldOffset[{}],newOffset[{}]", commitOffset, waitCommitOffset);
				} else {
					task.setLastCommitOffsetForFail(waitCommitOffset);
					this.printWarnLogForForce("commit offset failure, newOffset[{}], code:{}, msg:{}", waitCommitOffset, response.getCode(), response.getMsg());
				}
			} catch (Exception e) {
				task.setLastCommitOffsetForFail(waitCommitOffset);
				this.printErrorLogForForce("commit offset failure, newOffset[{}], msg:{}", waitCommitOffset, e.getMessage());
			}
		}
	}
	
	public PutCacheResponse putMessageCache(PollResponse response) {
		PutCacheResponse cacheResponse = new PutCacheResponse();
		if (response == null) {
			return cacheResponse;
		}
		TaskSharding task = response.getTask();
		List<CustomMessage> messageList = response.getMessageList();
		String resourceName = task.getTaskShardingConfig().getTaskCode();
		String batchCode = response.getBatchCode();
		//将消费数据放入队列
		CacheBatch cacheBatch = dataCacheService.putAndGet(task, messageList);
		int cacheQueueCount = cacheBatch.getMessageCount();
		long cacheQueueSize = cacheBatch.getMessageSize();
		int cacheMessageCount = 0;
		long cacheMessageSize = 0;
		//可能场景：多个topic同时拉取，仅部份topic就已经超出限定大小，则这部份topic有消息，其它topic无消息但存在HW等信息
		if (messageList != null) {
			cacheMessageSize = this.countMessageSize(messageList);
			cacheMessageCount = messageList.size();
			messageList.clear();
		}
		cacheResponse.setCacheQueueCount(cacheQueueCount);
		cacheResponse.setCacheQueueSize(cacheQueueSize);
		cacheResponse.setCacheMessageCount(cacheMessageCount);
		cacheResponse.setCacheMessageSize(cacheMessageSize);
		long currUsedMemory = statService.getUsedMemoryForTask(resourceName);
		printInfoLog("[{}] put {} records ({} byte) to cache, current queue size {} ({} byte), task used memory {} byte", batchCode, cacheMessageCount, cacheMessageSize, cacheQueueCount, cacheQueueSize, currUsedMemory);
		return cacheResponse;
	}
	/**
	 * 更新统计数据
	 * @param task
	 * @param pollResponse
	 * @param putCacheResponse
	 */
	public void refreshStat(PollResponse pollResponse, PutCacheResponse putCacheResponse) {
		String resourceName = task.getTaskShardingConfig().getTaskCode();
		int totalCount = pollResponse.getPollMessageCount();
		long totalSize = pollResponse.getPollMessageSize();
		//内存统计
		statService.incrTotalConsumerMsgCountForTask(resourceName, totalCount, totalSize);
		//累计当前任务分区的消息消费条数与大小
		this.messageCountCounter.addAndGet(totalCount);
		this.messageSizeCounter.addAndGet(totalSize);
//		//持久化更新
//		this.handlerMetrics(pollResponse, putCacheResponse);
	}
	/**
	 * 上报统计指标数据
	 */
	public void maybeReportMetrics(boolean force) {
		long currTime = System.currentTimeMillis();
		CommonConsumerConfiguration consumerConfiguration = this.commonConfiguration.getConsumer();
		if (!force && (currTime - this.lastSendMetricsTime < consumerConfiguration.getReportIntervalTimeForConsumer())) {
			return;
		}
		Long consumerOffset = task.getFetchOffset();
		Long maxOffset = task.getMaxOffset();
		Long lag = null;
		if (consumerOffset != null && maxOffset != null) {
			lag = maxOffset.longValue() - consumerOffset.longValue();
		}
		long totalConsumerDataNum = this.messageCountCounter.getAndUpdate(new LongUnaryOperator() {
			@Override
			public long applyAsLong(long operand) {
				return 0;
			}});
		long totalConsumerDataSize = this.messageSizeCounter.getAndUpdate(new LongUnaryOperator() {
			@Override
			public long applyAsLong(long operand) {
				return 0;
			}});
		String taskCode = task.getTaskShardingConfig().getTaskCode();
		long cacheDataNum = dataCacheService.getQueueCount(task);
		long cacheDataSize = dataCacheService.countQueueSize(task);
		Long maxAvailableMemory = statService.getMaxAvaiableMemoryForTask(taskCode);
		Long totalUsedMemory = statService.getUsedMemoryForTask(taskCode);
		try {
			taskService.refreshMemoryStat(taskCode, maxAvailableMemory, totalUsedMemory);
			TaskPartitionStatInfo consumerStat = new TaskPartitionStatInfo();
			consumerStat.setSourcePartition(task.getTaskShardingConfig().getFromPartition().getPartitionKey());
			consumerStat.setTargetPartition(task.getTaskShardingConfig().getToPartition().getPartitionKey());
			consumerStat.setMinOffset(task.getMinOffset());
			consumerStat.setMaxOffset(maxOffset);
			consumerStat.setConsumerOffset(consumerOffset);
			consumerStat.setConsumerLag(lag);
			consumerStat.setLastConsumerTime(task.getLastConsumerTime());
			consumerStat.setCacheQueueSize(cacheDataNum);
			consumerStat.setCacheDataSize(cacheDataSize);
			consumerStat.setTotalConsumerCount(totalConsumerDataNum);
			consumerStat.setTotalConsumerDataSize(totalConsumerDataSize);
			taskService.refreshConsumerStat(taskCode, consumerStat);
			this.lastSendMetricsTime = currTime;
		} catch (Throwable ex) {
			printErrorLogForForce("upload consumer metrics failure", ex);
			messageCountCounter.addAndGet(totalConsumerDataNum);
			messageSizeCounter.addAndGet(totalConsumerDataSize);
		}
	}
	
	/**
	 * 检查是否延迟告警
	 * @param lag
	 */
	public void checkAlarmForLag() {
		Long startOffset = task.getFetchOffset();
		Long highWatermark = task.getMaxOffset();
		Long lag = null;
		if (startOffset != null && highWatermark != null) {
			lag = highWatermark.longValue() - startOffset.longValue();
		}
		AlarmConfig alarmConfig = task.getTaskShardingConfig().getAlarmConfig();
		CommonConsumerConfiguration consumerConfiguration = this.commonConfiguration.getConsumer();
		boolean alarmEnable = alarmConfig != null && alarmConfig.getLagAlarmEnable() != null ? alarmConfig.getLagAlarmEnable().booleanValue() : consumerConfiguration.isPollMessageLagAlarmEnableForConsumer();
		if (alarmEnable) {
			try {
				Long maxLag = alarmConfig != null && alarmConfig.getLagAlarmMaxLag() != null ? alarmConfig.getLagAlarmMaxLag() : consumerConfiguration.getLagAlarmMaxLagForConsumer();
				Long maxIntervalSecond = alarmConfig != null && alarmConfig.getLagAlarmIntervalSecond() != null ? alarmConfig.getLagAlarmIntervalSecond() : consumerConfiguration.getLagAlarmIntervalSecondForConsumer();
				Long lastAlarmTime = task.getLagAlarmLastTime();
				if (maxIntervalSecond == null || maxIntervalSecond.longValue() <= 0) {
					maxIntervalSecond = 3600L;
				}
				if (lastAlarmTime == null) {
					lastAlarmTime = 0L;
				}
				if (maxLag != null && maxLag.longValue() > 0 
						&& lag != null && lag.longValue() >= maxLag.longValue()
						&& System.currentTimeMillis() - lastAlarmTime.longValue() >= maxIntervalSecond * 1000) {//符合延迟告警条件
					Long pollLastTime = task.getLastConsumerTime();
					String pollLastTimeStr = DateUtils.convertTime2Str(pollLastTime != null ? pollLastTime : System.currentTimeMillis(), null);
					task.setLagAlarmLastTime(System.currentTimeMillis());
					String consumerGroup = task.getConsumerClientConfig().getConsumerGroup();
					printWarnLogForForce("consumer lag {} [group:{}], will alarm.", lag, consumerGroup);
					alarmService.alarm(String.format("消息延迟报警：%s[group:%s,lag:%s,lastConsumerTime:%s]", task.toFromDescString(), consumerGroup, lag, pollLastTimeStr));
				}
			} catch (Exception e) {
				printErrorLogForForce("check lag alarm failure", e);
			}
		}
	}
}
