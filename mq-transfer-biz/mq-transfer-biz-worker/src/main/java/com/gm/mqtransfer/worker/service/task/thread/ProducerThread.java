package com.gm.mqtransfer.worker.service.task.thread;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

import com.gm.mqtransfer.facade.model.CustomMessage;
import com.gm.mqtransfer.provider.facade.api.producer.SendMessageResult;
import com.gm.mqtransfer.provider.facade.model.ProducerPartition;
import com.gm.mqtransfer.provider.facade.service.producer.SendCallback;
import com.gm.mqtransfer.provider.facade.service.producer.SendResult;
import com.gm.mqtransfer.worker.config.CommonConfiguration;
import com.gm.mqtransfer.worker.handler.ProducerHandler;
import com.gm.mqtransfer.worker.model.PollCacheRequest;
import com.gm.mqtransfer.worker.model.PollCacheResponse;
import com.gm.mqtransfer.worker.model.SendRequest;
import com.gm.mqtransfer.worker.model.TaskSharding;

public class ProducerThread extends AbstractTaskThread<ProducerHandler> {

	/** 连续未拉取到消息的次数 */
	private AtomicInteger pollEmptyMessageCount = new AtomicInteger(0);
	private final int maxPollEmptyMessageCount = 10;
	private final TaskSharding task;
	
	public ProducerThread(TaskSharding task, CommonConfiguration commonConfiguration) {
		super(task.toToString(), new ProducerHandler(task, commonConfiguration, LoggerFactory.getLogger(ProducerThread.class)), commonConfiguration.getLogger());
		this.task = task;
	}
	
	@Override
	public void runBody() {
		long suspendTime = this.handler.getMaxSuspendTime();
		try {
			//检查生产者是否准备好
			if (!handler.canSendMessage()) {
				this.suspend(suspendTime);
				return;
			}
		} catch (Throwable e) {
			this.handler.printErrorLogForForce("producer init failure", e);
			this.suspend(suspendTime);
			return;
		}
		try {
			PollCacheRequest pollRequest = handler.generatePollRequest();
			if (this.isStop()) {
				return;
			}
			//从缓存中获取待发送数据
			PollCacheResponse pollResponse = handler.pollMessages(pollRequest);
			if (!pollResponse.hasMessage()) {
				//更新过滤消息占用的缓存(存在消息完全过滤一条都不转发的情况，故优先将过滤消息占用资源更新)
				handler.refreshFilterStat(pollResponse);
				//检查并提交位置点(从缓存中拉取了消息，但都被过滤了，需要将消费的位置点提交，避免任务转移后重复消费)
				handler.checkAndCommitOffset(pollResponse);
				//连续10次未拉取到消息，生产暂停一段时间
				if (pollEmptyMessageCount.incrementAndGet() >= maxPollEmptyMessageCount) {
					pollEmptyMessageCount.set(0);
					suspend(suspendTime);
				}
				return;
			} else {
				pollEmptyMessageCount.set(0);
			}
			if (isStop()) {//任务结束
				//消息未到异步发送流程，可能由于超长等原因从缓存队列中取出，但不会发送，需要将取出部份的缓存更新
				handler.refreshStat(pollResponse, new SendResult());
				return;
			}
			SendRequest sendRequest = new SendRequest(pollResponse, this.handler.getSendTimeoutMs());
			try {
				this.handler.printInfoLog("producer will send {} records ({} byte)", pollResponse.getMessages().size(), 
						pollResponse.getPollMessageTotalMemorySize() - pollResponse.getFilterMessageTotalMemorySize());
				//发送消息
				handler.sendMessage(sendRequest, new SendCallback<CustomMessage>() {
					@Override
					public void onCompletion(ProducerPartition partition, SendMessageResult<CustomMessage> sendResult) {
						try {
							handler.handlerSendResult(sendRequest, sendResult);
						} catch (Exception ex) {
							handler.printErrorLogForForce("Handler failure for send result", ex);
						}
					}});
			} catch (Throwable e) {
				try {
					handler.handlerException(sendRequest, e);
				} catch (Exception ex) {
					handler.printErrorLogForForce("Handler failure for send result", ex);
				}
			}
		} catch (Throwable e) {
			this.handler.alarm("producer send failure, task info:" + task.toToString() + ", error:" + e.getMessage());
			this.handler.printErrorLogForForce("producer failure", e);
		} finally {
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
		//判断是否正在发送消息
		if (this.handler.isSendingMessage()) {
			return false;
		}
		//判断任务相关状态是否可用，如果不可用，需要执行线程进行初始化
		if (!this.handler.isReady()) {
			return true;
		}
		//判断是否有待发送数据
		if (this.handler.hasWaitSendData()) {
			return true;
		}
		return true;
	}
	
	@Override
	public void stop() {
		super.stop();
		this.handler.doStopTask();
	}

}
