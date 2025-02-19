package com.gm.mqtransfer.worker.service.task.thread;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.util.RamUsageEstimator;

import com.gm.mqtransfer.facade.handler.LoggerHandler;
import com.gm.mqtransfer.facade.model.CustomMessage;
import com.gm.mqtransfer.worker.config.TransferLogConfiguration;

public abstract class AbstractTaskThread<H extends LoggerHandler> implements Runnable {

	/** 任务信息描述 */
	protected String taskDesc = "";
	/** 任务运行状态 */
	protected AtomicBoolean runStatus = new AtomicBoolean(false);
	/** 任务是否停止 */
	protected volatile boolean stopStatus = false;
	/** 最后暂停时间 */
	private long lastSuspendTime = 0;
	
	protected H handler;
	
	protected TransferLogConfiguration logConfig;
	
	public AbstractTaskThread(String taskDesc, H handler, TransferLogConfiguration logConfig) {
		this.taskDesc = taskDesc;
		this.handler = handler;
		if (logConfig == null) {
			logConfig = new TransferLogConfiguration();
		}
		this.logConfig = logConfig;
	}
	
	@Override
	public void run() {
		//计算本次执行是否可以打印日志（规则：每10次或间隔1分钟打印一次）
		boolean limitLogPrintEnable = logConfig.getLimitLogPrintEnable();
		long limitLogPrintIntervalMs = logConfig.getLimitLogPrintIntervalMs();
		int limitLogPrintIntervalTimes = logConfig.getLimitLogPrintIntervalTimes();
		handler.countAndDeterminePrintLog(limitLogPrintEnable, limitLogPrintIntervalMs, limitLogPrintIntervalTimes);
		handler.printInfoLog("starting...");
		long startTime = System.currentTimeMillis();
		try {
			if (!this.isStop()) {
				runBody();
			} else {
				handler.printInfoLog("task disable");
			}
		} finally {
			//结束任务前变更任务执行状态
			releaseRunning();
			handler.printInfoLog("complete, time:{}", (System.currentTimeMillis() - startTime));
		}
	}
	/**
	 * 设置运行状态为：运行中（前提：未运行）
	 * @return
	 */
	public boolean lockRunning() {
		return runStatus.compareAndSet(false, true);
	}
	/**
	 * 设置运行状态为：结束运行
	 */
	public void releaseRunning() {
		runStatus.set(false);
	}
	/**
	 * 获取运行状态，true：正在运行
	 * @return
	 */
	public boolean isRunning() {
		return runStatus.get();
	}
	/**
	 * 任务是否停止
	 * @return
	 */
	public boolean isStop() {
		return this.stopStatus;
	}
	public String getTaskDesc() {
		return taskDesc;
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
	 * 停止
	 */
	public void stop() {
		this.stopStatus = true;
		this.handler.printInfoLogForForce("stoping task...");
	}
	/**
	 * 标识为停止
	 */
	public void stopMark() {
		this.stopStatus = true;
	}
	
	/**
	 * 是否可运行
	 * @return
	 */
	public boolean runable() {
		//判断任务是否停用
		if (isStop()) {
			return false;
		}
		//判断任务是否在运行
		if (isRunning()) {
			return false;
		}
		//判断暂时消费时间是否满足
		if (isSuspend()) {
			return false;
		}
		return true;
	}
	/**
	 * 暂停任务执行
	 * @param suspendTime	暂停时长
	 */
	public void suspend(long suspendTime) {
		this.lastSuspendTime = System.currentTimeMillis() + suspendTime;
	}
	/**
	 * 任务是否暂停
	 * @return
	 */
	public boolean isSuspend() {
		return System.currentTimeMillis() < this.lastSuspendTime;
	}
	
	public abstract void runBody();
	
}
