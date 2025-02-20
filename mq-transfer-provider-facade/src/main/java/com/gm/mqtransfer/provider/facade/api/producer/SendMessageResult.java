package com.gm.mqtransfer.provider.facade.api.producer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.gm.mqtransfer.provider.facade.api.ResponseCode;
import com.gm.mqtransfer.provider.facade.model.MQMessage;
import com.gm.mqtransfer.provider.facade.service.producer.ResultMetadata;

public class SendMessageResult<T extends MQMessage> {

	/** 总记录数 */
	private AtomicInteger totalCount = new AtomicInteger(0);
	/** 发送成功记录数 */
	private AtomicInteger succCount = new AtomicInteger(0);
	/** 发送失败记录数 */
	private AtomicInteger errorCount = new AtomicInteger(0);
	/** 发送时间 */
	private long sendTime;
	/** 发送结果集合 */
	private Map<T, ResultMetadata> resultMap = new LinkedHashMap<>();
	/** 最后发送成功的结果 */
	private ResultMetadata lastSuccResult;
	
	public SendMessageResult(int totalCount, Long sendTime) {
		this.totalCount.set(totalCount);
		this.sendTime = sendTime;
	}
	
	public int getTotalCount() {
		return totalCount.get();
	}
	public int getSuccCount() {
		return succCount.get();
	}
	public int getErrorCount() {
		return errorCount.get();
	}
	public ResultMetadata getLastSuccResult() {
		return lastSuccResult;
	}
	public void setLastSuccResult(ResultMetadata lastSuccResult) {
		this.lastSuccResult = lastSuccResult;
	}

	public Map<T, ResultMetadata> getResultMap() {
		return resultMap;
	}
	public void setResultMap(Map<T, ResultMetadata> resultMap) {
		this.resultMap = resultMap;
	}

	public long getSendTime() {
		return sendTime;
	}

	public void setSendTime(long sendTime) {
		this.sendTime = sendTime;
	}
	
	public synchronized void addResult(SendMessageResult<T> result) {
		if (result == null) {
			return;
		}
		this.succCount.addAndGet(result.getSuccCount());
		this.errorCount.addAndGet(result.getErrorCount());
		if (result.getLastSuccResult() != null) {
			this.setLastSuccResult(result.getLastSuccResult());
		}
		this.resultMap.putAll(result.getResultMap());
	}
	public synchronized void addResult(T message, ResultMetadata result) {
		ResultMetadata oldResult = resultMap.putIfAbsent(message, result);
		if (oldResult == null) {//表示首次加入
			if (ResponseCode.Success.name().equalsIgnoreCase(result.getCode())) {
				this.lastSuccResult = result;
				this.succCount.incrementAndGet();
			} else {
				this.errorCount.incrementAndGet();
			}
		}
	}
	/**
	 * 判断消息是否已经存在响应
	 * @param message
	 * @return
	 */
	public boolean isExistsResponse(T message) {
		return this.resultMap.containsKey(message);
	}
	/**
	 * Are there any errors that cannot be ignored
	 * @return
	 */
	public boolean isExistsError() {
		return this.errorCount.get() > 0;
	}
	/**
	 * 发送是否完成
	 * @return
	 */
	public boolean isCompletion() {
		return this.getTotalCount() == resultMap.size();
	}

	@Override
	public String toString() {
		return "SendMessageResult [totalCount=" + totalCount + ", succCount=" + succCount + ", errorCount=" + errorCount
				+ ", sendTime=" + sendTime + ", resultMap=" + resultMap + "]";
	}

	
	
}
