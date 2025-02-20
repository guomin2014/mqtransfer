package com.gm.mqtransfer.facade.handler;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gm.mqtransfer.provider.facade.util.StringUtils;

public class LoggerHandler {
	
	protected final Logger logger;
	/** 日志前缀 */
	private String logPrefix;
	/** 最后一次打印日志时间 */
	private long lastPrintTime = 0;
	/** 任务执行次数 */
	private AtomicInteger runCount = new AtomicInteger(0);
	/** 是否可以打印日志 */
	private ThreadLocal<Boolean> printEnableLocal = new ThreadLocal<>();
	
	public LoggerHandler(Logger logger, String logPrefix) {
		if (logger == null) {
			logger = LoggerFactory.getLogger(this.getClass());
		}
		this.logger = logger;
		this.logPrefix = StringUtils.isNotBlank(logPrefix) ? "[" + logPrefix + "]" : "";
	}
	public boolean isPrintLogEnable() {
		Boolean enable =  printEnableLocal.get();
		return enable != null ? enable.booleanValue() : false;
	}
	public void setPrintLogEnable(boolean printLogEnable) {
		printEnableLocal.set(printLogEnable);
	}
	/**
	 * 检查打印日志计数
	 */
	public void countAndDeterminePrintLog(boolean limitLogPrintEnable, long limitLogPrintIntervalMs, int limitLogPrintIntervalTimes) {
		try {
			//计算本次执行是否可以打印日志（规则：每10次或间隔1分钟打印一次）
			long startTime = System.currentTimeMillis();
			int count = runCount.getAndIncrement();
			if (count == Integer.MAX_VALUE) {
				runCount.set(0);
			}
			long diffTime = startTime - lastPrintTime;
			if (!limitLogPrintEnable || (limitLogPrintEnable && (diffTime >= limitLogPrintIntervalMs || count % limitLogPrintIntervalTimes == 0))) {
				printEnableLocal.set(true);
				this.lastPrintTime = startTime;
			} else {
				printEnableLocal.set(false);
			}
		} catch (Throwable e) {
			printEnableLocal.set(false);
		}
	}
	
	private boolean isPrintEnable() {
		Boolean print = printEnableLocal.get();
		return print != null && print.booleanValue();
	}

	public void printInfoLogForForce(String format, Object... arguments) {
//		StackTraceElement[] eles = Thread.currentThread().getStackTrace();
		if (logger.isInfoEnabled()) {
			logger.info(this.logPrefix + format, arguments);
		}
	}
	public void printErrorLogForForce(String format, Object... arguments) {
		if (logger.isErrorEnabled()) {
			logger.error(this.logPrefix + format, arguments);
		}
	}
	public void printErrorLogForForce(String message, Throwable e) {
		if (logger.isErrorEnabled()) {
			logger.error(this.logPrefix + message, e);
		}
	}
	public void printWarnLogForForce(String format, Object... arguments) {
		if (logger.isWarnEnabled()) {
			logger.warn(this.logPrefix + format, arguments);
		}
	}
	public void printDebugLogForForce(String format, Object... arguments) {
		if (logger.isDebugEnabled()) {
			logger.debug(this.logPrefix + format, arguments);
		}
	}
	public void printInfoLog(String format, Object... arguments) {
		this.printInfoLog(false, format, arguments);
	}
	public void printInfoLog(boolean force, String format, Object... arguments) {
		if (isPrintEnable() || force) {
			this.printInfoLogForForce(format, arguments);
		}
	}
	public void printErrorLog(String format, Object... arguments) {
		this.printErrorLog(false, format, arguments);
	}
	public void printErrorLog(boolean force, String format, Object... arguments) {
		if (isPrintEnable() || force) {
			this.printErrorLogForForce(format, arguments);
		}
	}
	public void printDebugLog(String format, Object... arguments) {
		if (isPrintEnable()) {
			this.printDebugLogForForce(format, arguments);
		}
	}
}
