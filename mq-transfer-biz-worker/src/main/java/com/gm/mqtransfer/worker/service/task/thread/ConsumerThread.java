package com.gm.mqtransfer.worker.service.task.thread;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

import com.gm.mqtransfer.worker.config.CommonConfiguration;
import com.gm.mqtransfer.worker.handler.ConsumerHandler;
import com.gm.mqtransfer.worker.model.PollRequest;
import com.gm.mqtransfer.worker.model.PollResponse;
import com.gm.mqtransfer.worker.model.PutCacheResponse;
import com.gm.mqtransfer.worker.model.TaskSharding;

public class ConsumerThread extends AbstractTaskThread<ConsumerHandler> {
	
	/** 连续未拉取到消息的次数 */
	private AtomicInteger pollEmptyMessageCount = new AtomicInteger(0);
	private final int maxPollEmptyMessageCount = 10;
	
	private final TaskSharding task;
	
	public ConsumerThread(TaskSharding task, CommonConfiguration commonConfiguration) {
		super(task.toFromString(), new ConsumerHandler(task, commonConfiguration, LoggerFactory.getLogger(ConsumerThread.class)), commonConfiguration.getLogger());
		this.task = task;
	}
	
	public TaskSharding getTask() {
		return task;
	}

	@Override
	public void runBody() {
		long suspendTime = this.handler.getMaxSuspendTime();
		try {
			//检查任务是否准备好（消费者正确初始化）
			if (!this.handler.canPollMessage()) {
				this.suspend(suspendTime);
				return;
			}
		} catch (Throwable e) {
			this.handler.printErrorLogForForce("consumer init failure", e);
			this.suspend(suspendTime);
			return;
		}
		try {
			//创建Poll对象
			PollRequest pollRequest = this.handler.generatePollRequest();
			//锁定占用内存
			if (!this.handler.tryLockMemory(pollRequest)) {
				suspend(suspendTime);
				return;
			}
			int pollCount = 0;
			long maxSingleSize = pollRequest.getMaxSingleSize();
			int maxBatchRecords = pollRequest.getMaxBatchRecords();
			long maxBatchSize = pollRequest.getMaxBatchSize();
			long maxBatchWaitMs = pollRequest.getMaxBatchWaitMs();
			int maxCacheRecords = pollRequest.getMaxCacheRecords();
			long maxFetchSize = pollRequest.getMaxFetchSize();
			long startOffset = pollRequest.getTask().getFetchOffset();
			long totalCacheMessageSize = 0;//真实缓存消息的总大小
			int totalCacheMessageCount = 0;//真实缓存消息的总记录数
			long pollStartTime = System.currentTimeMillis();
			try {
				this.handler.printInfoLog("will poll records from broker, maxRecords:{},singleSize:{},batchSize:{},waitMs:{},startOffset:{}", maxBatchRecords, maxSingleSize, maxBatchSize, maxBatchWaitMs, startOffset);
				while(true) {
					pollCount++;
					int cacheQueueCount = 0;
					PollResponse pollResponse = null;
					if (isStop()) {
						break;
					}
					try {
						String batchCode = pollStartTime + "-" + pollCount;
						//消费拉取数据
						pollResponse = this.handler.pollMessages(pollRequest, batchCode);
						//将消息放入缓存
						PutCacheResponse cacheResponse = this.handler.putMessageCache(pollResponse);
						totalCacheMessageSize += cacheResponse.getCacheMessageSize();
						totalCacheMessageCount += cacheResponse.getCacheMessageCount();
						cacheQueueCount = cacheResponse.getCacheQueueCount();
						//更新统计数据
						this.handler.refreshStat(pollResponse, cacheResponse);
					} catch (Exception e) {
						this.handler.printErrorLogForForce("consumer poll error", e);
						break;
					}
					//满足以下任一条件，都立即结束拉取消息：一条消息都未拉到、拉取的消息超过限定大小或条数、拉取消息时间超过最长等待时长、队列缓存数量超过限制
					if (pollResponse.getPollMessageCount() == 0
							|| totalCacheMessageSize >= maxFetchSize || totalCacheMessageCount >= maxBatchRecords 
							|| (System.currentTimeMillis() - pollStartTime) >= maxBatchWaitMs 
							|| cacheQueueCount >= maxCacheRecords) {
						break;
					}
				}
			} finally {
				//释放锁定内存
				this.handler.tryReleaseMemory(pollRequest, totalCacheMessageSize);
			}
			if (totalCacheMessageSize == 0) {
				if (pollEmptyMessageCount.incrementAndGet() >= maxPollEmptyMessageCount) {//连续10次未拉取到消息，消费暂停一段时间
					pollEmptyMessageCount.set(0);
					this.suspend(suspendTime);
				}
			} else {
				pollEmptyMessageCount.set(0);
			}
		} catch (Throwable e) {
			this.handler.printErrorLogForForce("consumer failure", e);
		} finally {
			//检查是否有需要提交的位置点
			this.handler.maybeCommitOffset();
			//检查消费延迟并告警
			this.handler.checkAlarmForLag();
			//上报统计数据
			this.handler.maybeReportMetrics(false);
		}
	}
	/**
	 * 是否可运行
	 * @return
	 */
	public boolean runable() {
		if (!super.runable()) {
			return false;
		}
		//判断是否有可用内存
		if (this.handler.isOutOfAvaiableMemory()) {
			return false;
		}
		return true;
	}
	
	public void stop() {
		super.stop();
		this.handler.doStopTask();
	}

}
