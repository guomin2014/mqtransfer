package com.gm.mqtransfer.facade.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 任务统计信息
 * 使用时间轮的方式记录每个刻度的统计信息
 * @author GuoMin
 * @date 2023-05-18 17:10:26
 *
 */
public class TaskCounter {
	
	/** 桶数量 */
	private int maxBucketSize = 0;
	/** 消费消息桶，每个桶统计1秒的数据 */
	private AtomicLong[] consumerMsgCountBuckets;
	/** 生产消息桶，每个桶统计1秒的数据 */
	private AtomicLong[] producerMsgCountBuckets;
	/** 消费消息桶，每个桶统计1秒的数据 */
	private AtomicLong[] consumerMsgSizeBuckets;
	/** 生产消息桶，每个桶统计1秒的数据 */
	private AtomicLong[] producerMsgSizeBuckets;
	/** 当前桶位置 */
	private volatile int currIndex = 0;
	
	/**
	 * 
	 * @param maxBucketSize	最大桶数量
	 * @param clockCycle	每个桶占用时长(时钟周期)
	 * @param clockCycleUnit每个桶占用时长单位
	 */
	public TaskCounter(int maxBucketSize, int clockCycle, TimeUnit clockCycleUnit) {
		if (maxBucketSize <= 0) {
			throw new RuntimeException("maxBucketSize cannot be less than zero");
		}
		if (clockCycle <= 0) {
			throw new RuntimeException("clockCycle cannot be less than zero");
		}
		this.maxBucketSize = maxBucketSize;
		consumerMsgCountBuckets = new AtomicLong[maxBucketSize];
		producerMsgCountBuckets = new AtomicLong[maxBucketSize];
		consumerMsgSizeBuckets = new AtomicLong[maxBucketSize];
		producerMsgSizeBuckets = new AtomicLong[maxBucketSize];
		for (int i = 0; i < this.maxBucketSize; i++) {
        	consumerMsgCountBuckets[i] = new AtomicLong(0);
        	producerMsgCountBuckets[i] = new AtomicLong(0);
        	consumerMsgSizeBuckets[i] = new AtomicLong(0);
        	producerMsgSizeBuckets[i] = new AtomicLong(0);
        }
		this.currIndex = 0;
	}
	private int getIndex() {
		return this.currIndex;
	}
	/**
	 * 重置指定桶的值
	 * @param index
	 */
	public void reset(int index) {
		if (index < this.maxBucketSize) {
			consumerMsgCountBuckets[index].set(0);
			producerMsgCountBuckets[index].set(0);
			consumerMsgSizeBuckets[index].set(0);
			producerMsgSizeBuckets[index].set(0);
			this.currIndex = index;
		}
	}
	
	/**
	 * 统计消费速率
	 * @param totalCount
	 * @param totalSize
	 */
	public void countConsumerRate(long totalCount, long totalSize) {
		try {
			int index = this.getIndex();
			if (totalCount != 0) {
				consumerMsgCountBuckets[index].addAndGet(totalCount);
			}
			if (totalCount != 0) {
				consumerMsgSizeBuckets[index].addAndGet(totalSize);
			}
		} catch (Exception e) {}
	}
	/**
	 * 统计生产速率
	 * @param totalCount
	 * @param totalSize
	 */
	public void countProducerRate(long totalCount, long totalSize) {
		try {
			int index = this.getIndex();
			if (totalCount != 0) {
				producerMsgCountBuckets[index].addAndGet(totalCount);
			}
			if (totalSize != 0) {
				producerMsgSizeBuckets[index].addAndGet(totalSize);
			}
		} catch (Exception e) {}
	}
	/**
	 * 获取消费速率，单位：条/秒
	 * @return
	 */
	public int getConsumerCountRate() {
		long total = 0;
		for (AtomicLong ato : consumerMsgCountBuckets) {
			total += ato.get();
		}
		if (total == 0) {
			return 0;
		}
		BigDecimal dec = new BigDecimal(total);
		return dec.divide(new BigDecimal(consumerMsgCountBuckets.length), 0, RoundingMode.HALF_UP).intValue();
	}
	/**
	 * 获取消费速率，单位：byte/秒
	 * @return
	 */
	public long getConsumerSizeRate() {
		long total = 0;
		for (AtomicLong ato : consumerMsgSizeBuckets) {
			total += ato.get();
		}
		if (total == 0) {
			return 0;
		}
		BigDecimal dec = new BigDecimal(total);
		return dec.divide(new BigDecimal(consumerMsgSizeBuckets.length), 0, RoundingMode.HALF_UP).longValue();
	}
	/**
	 * 获取生产速率，单位：条/秒
	 * @return
	 */
	public int getProducerCountRate() {
		long total = 0;
		for (AtomicLong ato : producerMsgCountBuckets) {
			total += ato.get();
		}
		if (total == 0) {
			return 0;
		}
		BigDecimal dec = new BigDecimal(total);
		return dec.divide(new BigDecimal(producerMsgCountBuckets.length), 0, RoundingMode.HALF_UP).intValue();
	}
	/**
	 * 获取生产速率，单位：byte/秒
	 * @return
	 */
	public long getProducerSizeRate() {
		long total = 0;
		for (AtomicLong ato : producerMsgSizeBuckets) {
			total += ato.get();
		}
		if (total == 0) {
			return 0;
		}
		BigDecimal dec = new BigDecimal(total);
		return dec.divide(new BigDecimal(producerMsgSizeBuckets.length), 0, RoundingMode.HALF_UP).longValue();
	}
	/**
	 * 获取消费数量
	 * @return
	 */
	public long getConsumerCount() {
		long total = 0;
		for (AtomicLong ato : consumerMsgCountBuckets) {
			total += ato.get();
		}
		if (total == 0) {
			return 0;
		}
		return total;
	}
	/**
	 * 获取消费大小，单位：byte
	 * @return
	 */
	public long getConsumerSize() {
		long total = 0;
		for (AtomicLong ato : consumerMsgSizeBuckets) {
			total += ato.get();
		}
		if (total == 0) {
			return 0;
		}
		return total;
	}
}
