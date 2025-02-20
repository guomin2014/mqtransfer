package com.gm.mqtransfer.worker.service.cache;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gm.mqtransfer.facade.common.util.ByteUtil;
import com.gm.mqtransfer.facade.model.TaskCounter;
import com.gm.mqtransfer.facade.service.IApplicationService;
import com.gm.mqtransfer.provider.facade.exception.BusinessException;
import com.gm.mqtransfer.worker.config.CommonConfiguration;

/**
 * 统计服务
 * @author gm
 *
 */
@Component
public class StatService implements IApplicationService {
	private final Logger logger = LoggerFactory.getLogger(StatService.class);
	/** 所有任务总使用内存(由增减任务内存时连带进行操作) */
	private AtomicLong totalUsedMemory = new AtomicLong(0);
	/** 按任务分组统计使用内存 */
	private Map<String, AtomicLong> taskTotalUsedMemory = new ConcurrentHashMap<>();
	/** 任务可用内存占比 */
	private Map<String, Integer> taskMemoryRatio = new ConcurrentHashMap<>();
	/** 任务可用内存 */
	private Map<String, Long> taskTotalAvaiableMemory = new ConcurrentHashMap<>();
	/** 内存读锁 */
	private final ReentrantLock readLock = new ReentrantLock();
	/** 内存写锁 */
	private final ReentrantLock writeLock = new ReentrantLock();
	/** JVM可使用内存 */
	private long totalMemory = 0;
	/** 总的可使用内存 */
	private long totalAvaiableMemory = 0;
	/** 总的内存占比之和 */
	private AtomicInteger totalMemoryRatio = new AtomicInteger(0);
	
	/** 按任务分组统计已消费消息数量 */
	private Map<String, AtomicLong> taskTotalConsumerMsgCount = new ConcurrentHashMap<>();
	/** 按任务分组统计消费过滤消息数量 */
	private Map<String, AtomicLong> taskTotalFilterMsgCount = new ConcurrentHashMap<>();
	/** 按任务分组统计已生产消息数量 */
	private Map<String, AtomicLong> taskTotalProducerMsgCount = new ConcurrentHashMap<>();
	/** 按任务分组统计分片数量 */
	private Map<String, AtomicLong> taskTotalPartitionCount = new ConcurrentHashMap<>();
	/** 桶数量 */
	private final int maxBucketSize = 60;
//	/** 消费消息桶，每个桶统计1秒的数据 */
//	private AtomicLong[] consumerMsgCountBuckets = new AtomicLong[maxBucketSize];
//	/** 生产消息桶，每个桶统计1秒的数据 */
//	private AtomicLong[] producerMsgCountBuckets = new AtomicLong[maxBucketSize];
//	/** 消费消息桶，每个桶统计1秒的数据 */
//	private AtomicLong[] consumerMsgSizeBuckets = new AtomicLong[maxBucketSize];
//	/** 生产消息桶，每个桶统计1秒的数据 */
//	private AtomicLong[] producerMsgSizeBuckets = new AtomicLong[maxBucketSize];
	/** 全部任务的消费与生产统计信息 */
	private TaskCounter allTaskStat = new TaskCounter(maxBucketSize, 1, TimeUnit.SECONDS);
	
	/** 按任务分组统计最近1小时消费与生产信息 */
	private Map<String, TaskCounter> taskStatMap = new ConcurrentHashMap<>();
	
	private ScheduledExecutorService statScheuler;
	@Autowired
	private CommonConfiguration commonConfiguration;
	@Autowired
	private DataCacheService dataCacheService;

	@Override
	public int order() {
		return 1;
	}
	public void start() {
		logger.info("starting stat service");
		//配置的堆内存 * 可用百分比
		double memoryRatio = commonConfiguration.getMessageQueueMemoryRatio();
		this.totalMemory = Runtime.getRuntime().maxMemory();
		this.totalAvaiableMemory = (long)Math.floor(totalMemory * memoryRatio);
        logger.info("JVM_Max_Memory:{}，message_queue_max_memory:{}", (totalMemory / 1048576)+"MB", (totalAvaiableMemory/1048576)+"MB");
//		        for (int i = 0; i < maxBucketSize; i++) {
//		        	consumerMsgCountBuckets[i] = new AtomicLong(0);
//		        	producerMsgCountBuckets[i] = new AtomicLong(0);
//		        	consumerMsgSizeBuckets[i] = new AtomicLong(0);
//		        	producerMsgSizeBuckets[i] = new AtomicLong(0);
//		        }
        statScheuler = Executors.newSingleThreadScheduledExecutor();
		//秒级时间轮：每秒执行一次清空
		statScheuler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				long time = System.currentTimeMillis() / 1000;
				int index = (int)((time - maxBucketSize + 1) % maxBucketSize);
//				consumerMsgCountBuckets[index].set(0);
//				producerMsgCountBuckets[index].set(0);
//				consumerMsgSizeBuckets[index].set(0);
//				producerMsgSizeBuckets[index].set(0);
				allTaskStat.reset(index);
			}
		}, 0, 1000, TimeUnit.MILLISECONDS);
		//分钟级时间轮：每分钟执行一次清空
		statScheuler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				printStatInfo();//打印统计信息
				long time = System.currentTimeMillis() / 60000;
				int index = (int)((time - maxBucketSize + 1) % maxBucketSize);
				Iterator<String> it = taskStatMap.keySet().iterator();
				while(it.hasNext()) {
					String key = it.next();
					TaskCounter stat = taskStatMap.get(key);
					if (stat != null) {
						stat.reset(index);
					}
				}
			}
		}, 0, 1, TimeUnit.MINUTES);
	}
	public void stop() {
		logger.info("stoping stat service");
		this.printStatInfo();
		if (statScheuler != null) {
			statScheuler.shutdown();
		}
	}
	/**
	 * 重设消息缓存队列的内存占比
	 * @param messageQueueMemoryRatio
	 */
	public void resetMessageQueueMemoryRatio(double messageQueueMemoryRatio) {
		long totalMemory = Runtime.getRuntime().maxMemory();
		this.totalAvaiableMemory = (long)Math.floor(totalMemory * messageQueueMemoryRatio);
        logger.info("JVM_Max_Memory:{}，message_queue_max_memory:{}", (totalMemory / 1048576)+"MB", (totalAvaiableMemory/1048576)+"MB");
        this.reCountTotalAvaiableMemoryForTask();
	}
	/**
	 * 检查总使用的内存队列是否超限
	 * @return	true: 超限
	 */
	public boolean doCheckMemoryOverLimitForTotalUsed() {
		return totalUsedMemory.get() > totalAvaiableMemory;
	}
	/**
	 * 获取实例总可用内存
	 * @return
	 */
	public long getTotalMemory() {
		return this.totalMemory;
	}
	/**
	 * 获取总可用内存队列大小
	 * @return
	 */
	public long getTotalAvaiableMemory() {
		return this.totalAvaiableMemory;
	}
	/**
	 * 获取总使用内存大小
	 * @return
	 */
	public long getTotalUsedMemory() {
		return totalUsedMemory.get();
	}
	/**
	 * 获取所有任务使用的内存大小
	 * @return
	 */
	public Map<String, AtomicLong> getAllTaskUsedMemory() {
		return Collections.unmodifiableMap(this.taskTotalUsedMemory);
	}
	/**
	 * 获取所有任务可用的内存大小
	 * @return
	 */
	public Map<String, Long> getAllTaskAvaiableMemory() {
		return Collections.unmodifiableMap(this.taskTotalAvaiableMemory);
	}
	/**
	 * 获取当前任务的内存队列已使用内存
	 * @param taskCode
	 * @return
	 */
	public long getUsedMemoryForTask(String taskCode) {
		AtomicLong usedMemory = taskTotalUsedMemory.get(taskCode);
		return usedMemory == null ? 0 : usedMemory.get();
	}
	public long removeAndGetUsedMemoryForTask(String taskCode) {
		AtomicLong usedMemory = taskTotalUsedMemory.remove(taskCode);
		return usedMemory == null ? 0 : usedMemory.get();
	}
	/**
	 * 获取任务最大可使用内存
	 * @param taskCode
	 * @param maxMemoryRatio
	 * @return
	 */
	public long getMaxAvaiableMemoryForTask(String taskCode) {
		Long maxAvaiableMemory = taskTotalAvaiableMemory.get(taskCode);
		if (maxAvaiableMemory == null) {
			maxAvaiableMemory = this.totalAvaiableMemory;
		}
		return maxAvaiableMemory;
	}
	/**
	 * 计算任务的最大可用内存
	 * @param taskCode		资源名称
	 * @param maxMemoryRatio	内存占比，1-100，100：表示可全量占用内存
	 * @return	任务最大可用内存
	 */
	public Long addAndCountTotalAvaiableMemoryForTask(String taskCode, Integer maxMemoryRatio) {
		readLock.lock();
		try {
			if (!taskTotalUsedMemory.containsKey(taskCode)) {
				taskTotalUsedMemory.put(taskCode, new AtomicLong(0));
			}
			boolean needReCount = false;
			if (taskTotalAvaiableMemory.containsKey(taskCode)) {
				if (maxMemoryRatio == null || maxMemoryRatio.intValue() == 100) {//表示不限制大小
					taskTotalAvaiableMemory.put(taskCode, this.totalAvaiableMemory);
					Integer oldMemoryRatio = taskMemoryRatio.remove(taskCode);
					if (oldMemoryRatio != null) {//表示该任务以前存在内存限制，现取消，需要重新计算其它任务的内存限制
						needReCount = true;
					}
				} else {
					Integer oldMemoryRatio = taskMemoryRatio.get(taskCode);
					if (oldMemoryRatio == null || (oldMemoryRatio != null && oldMemoryRatio.intValue() != maxMemoryRatio.intValue())) {//占比变更，需要重新计算
						if (oldMemoryRatio == null) {
							taskMemoryRatio.put(taskCode, maxMemoryRatio);
							totalMemoryRatio.addAndGet(maxMemoryRatio);
						}
						needReCount = true;
					}
				}
			} else {
				if (maxMemoryRatio == null || maxMemoryRatio.intValue() == 100) {//表示不限制大小
					taskTotalAvaiableMemory.put(taskCode, this.totalAvaiableMemory);
				} else {
					taskMemoryRatio.put(taskCode, maxMemoryRatio);
					totalMemoryRatio.addAndGet(maxMemoryRatio);
					needReCount = true;
				}
			}
			if (needReCount) {
				reCountTotalAvaiableMemoryForTask();
			}
			return taskTotalAvaiableMemory.get(taskCode);
		} finally {
			readLock.unlock();
		}
	}
	/**
	 * 移除资源的内存分配并重新计算余下资源的内存分配
	 * @param taskCode
	 */
	public long removeAndCountTotalAvaiableMemoryForTask(String taskCode) {
		readLock.lock();
		try {
			Long totalAvaiableMemory = taskTotalAvaiableMemory.remove(taskCode);
			Integer oldMemoryRatio = taskMemoryRatio.remove(taskCode);
			if (oldMemoryRatio != null) {
				totalMemoryRatio.addAndGet(oldMemoryRatio * -1);
				reCountTotalAvaiableMemoryForTask();
			}
			return totalAvaiableMemory == null ? 0 : totalAvaiableMemory.longValue();
		} finally {
			readLock.unlock();
		}
	}
	/**
	 * 重新计算任务总的可用内存大小
	 */
	private void reCountTotalAvaiableMemoryForTask() {
		readLock.lock();
		try {
			int totalRatio = totalMemoryRatio.get();
			BigDecimal totalRatioDecimal = new BigDecimal(totalRatio);
			BigDecimal totalAvaiableMemoryDecimal = new BigDecimal(totalAvaiableMemory);
			for (Map.Entry<String, Integer> entry : taskMemoryRatio.entrySet()) {
				String rn = entry.getKey();
				Integer currRatio = entry.getValue();
				BigDecimal currRatioDecimal = new BigDecimal(currRatio);
				long avaiableMemory = currRatioDecimal.divide(totalRatioDecimal, 10, RoundingMode.FLOOR).multiply(totalAvaiableMemoryDecimal)
						.setScale(0, RoundingMode.FLOOR).longValue();
				taskTotalAvaiableMemory.put(rn, avaiableMemory);
			}
		} finally {
			readLock.unlock();
		}
	}
	/**
	 * 减少总占用内存大小
	 * @param dataSize
	 * @return
	 */
	public long decrTotalMemoryUsed(long dataSize) {
		dataSize = dataSize * -1;
		return totalUsedMemory.addAndGet(dataSize);
	}
	/**
	 * 增加任务占用的内存大小
	 * @param taskCode
	 * @param dataSize
	 * @return
	 */
	public long incrMemoryUsedForTask(String taskCode, long dataSize) {
		AtomicLong usedMemory = getOrInitValue(taskTotalUsedMemory, taskCode);
		writeLock.lock();
		try {
			long total = totalUsedMemory.addAndGet(dataSize);
			if (total > totalAvaiableMemory) {
				totalUsedMemory.addAndGet(dataSize * -1);
				throw new BusinessException("not enough avaiable memory for total");
			}
			long used = usedMemory.addAndGet(dataSize);
			long maxAvaiable = getMaxAvaiableMemoryForTask(taskCode);
			if (used > maxAvaiable) {
				totalUsedMemory.addAndGet(dataSize * -1);
				usedMemory.addAndGet(dataSize * -1);
				throw new BusinessException("not enough avaiable memory for task");
			}
			return used;
		} finally {
			writeLock.unlock();
		}
	}
	/**
	 * 减少任务占用的内存大小
	 * @param taskCode
	 * @param dataSize
	 * @return
	 */
	public long decrMemoryUsedForTask(String taskCode, long dataSize) {
		writeLock.lock();
		try {
			AtomicLong usedMemory = taskTotalUsedMemory.get(taskCode);
			if (usedMemory != null) {//只有当前资源存在的情况下，才操作资源占用内存与系统总内存
				dataSize = dataSize * -1;
				long currUsed = usedMemory.addAndGet(dataSize);
				totalUsedMemory.addAndGet(dataSize);
				return currUsed;
			}
		} finally {
			writeLock.unlock();
		}
		return 0;
	}
	
	private AtomicLong getOrInitValue(Map<String, AtomicLong> map, String key) {
		AtomicLong count = map.get(key);
		if (count == null) {
			writeLock.lock();
			try {
				count = map.get(key);
				if (count == null) {
					count = new AtomicLong(0);
					map.put(key, count);
				}
			} finally {
				writeLock.unlock();
			}
		}
		return count;
	}
	/**
	 * 增加消费消息数量
	 * @param taskCode
	 * @param consumerCount
	 * @param filterCount
	 * @return
	 */
	public long incrTotalConsumerMsgCountForTask(String taskCode, long consumerCount, long consumerSize) {
		AtomicLong totalConsumerCount = getOrInitValue(taskTotalConsumerMsgCount, taskCode);
		long currCount = 0;
		if (consumerCount > 0) {
			currCount = totalConsumerCount.addAndGet(consumerCount);
		} else {
			currCount = totalConsumerCount.get();
		}
		//统计消费速率
		this.countConsumerRate(taskCode, consumerCount, consumerSize);
		return currCount;
	}
	public long incrTotalFilterMsgCountForTask(String taskCode, long filterCount) {
		AtomicLong totalFilterCount = getOrInitValue(taskTotalFilterMsgCount, taskCode);
		return totalFilterCount.addAndGet(filterCount);
	}
	/**
	 * 增加生产消息数量
	 * @param taskCode
	 * @param producerCount
	 * @return
	 */
	public long incrTotalProducerMsgCountForTask(String taskCode, long producerCount) {
		AtomicLong totalProducerCount = getOrInitValue(taskTotalProducerMsgCount, taskCode);
		return totalProducerCount.addAndGet(producerCount);
	}
	
	public long getTotalConsumerMsgCountForTask(String taskCode) {
		AtomicLong totalConsumerCount = taskTotalConsumerMsgCount.get(taskCode);
		return totalConsumerCount != null ? totalConsumerCount.get() : 0;
	}
	public long getTotalFilterMsgCountForTask(String taskCode) {
		AtomicLong totalConsumerCount = taskTotalFilterMsgCount.get(taskCode);
		return totalConsumerCount != null ? totalConsumerCount.get() : 0;
	}
	public long getTotalProducerMsgCountForTask(String taskCode) {
		AtomicLong totalConsumerCount = taskTotalProducerMsgCount.get(taskCode);
		return totalConsumerCount != null ? totalConsumerCount.get() : 0;
	}
	
	public long removeAndGetConsumerMsgCount(String taskCode) {
		AtomicLong totalConsumerCount = taskTotalConsumerMsgCount.remove(taskCode);
		return totalConsumerCount != null ? totalConsumerCount.get() : 0;
	}
	public long removeAndGetFilterMsgCount(String taskCode) {
		AtomicLong totalFilterCount = taskTotalFilterMsgCount.remove(taskCode);
		return totalFilterCount != null ? totalFilterCount.get() : 0;
	}
	public long removeAndGetProducerMsgCount(String taskCode) {
		AtomicLong totalProducerCount = taskTotalProducerMsgCount.remove(taskCode);
		return totalProducerCount != null ? totalProducerCount.get() : 0;
	}
	
	public long incrTotalPartitionCountForTask(String taskCode) {
		AtomicLong totalPartitionCount = getOrInitValue(taskTotalPartitionCount, taskCode);
		return totalPartitionCount.incrementAndGet();
	}
	public long decrTotalPartitionCountForTask(String taskCode) {
		AtomicLong totalPartitionCount = getOrInitValue(taskTotalPartitionCount, taskCode);
		long result = totalPartitionCount.decrementAndGet();
		return result;
	}
	public long getTotalPartitionCountForTask(String taskCode) {
		AtomicLong totalPartitionCount = taskTotalPartitionCount.get(taskCode);
		return totalPartitionCount != null ? totalPartitionCount.get() : 0;
	}
	public long removeAndGetPartitionCount(String taskCode) {
		AtomicLong totalPartitionCount = taskTotalPartitionCount.remove(taskCode);
		return totalPartitionCount != null ? totalPartitionCount.get() : 0;
	}
	/**
	 * 统计消费速率
	 * @param totalCount
	 * @param totalSize
	 */
	public void countConsumerRate(String taskCode, long totalCount, long totalSize) {
		try {
//			long time = System.currentTimeMillis() / 1000;
//			int index = (int)(time % maxBucketSize);
//			if (totalCount != 0) {
//				consumerMsgCountBuckets[index].addAndGet(totalCount);
//			}
//			if (totalCount != 0) {
//				consumerMsgSizeBuckets[index].addAndGet(totalSize);
//			}
			allTaskStat.countConsumerRate(totalCount, totalSize);
		} catch (Exception e) {}
	}
	/**
	 * 统计生产速率
	 * @param totalCount
	 * @param totalSize
	 */
	public void countProducerRate(String taskCode, long totalCount, long totalSize) {
		try {
//			long time = System.currentTimeMillis() / 1000;
//			int index = (int)(time % maxBucketSize);
//			if (totalCount != 0) {
//				producerMsgCountBuckets[index].addAndGet(totalCount);
//			}
//			if (totalSize != 0) {
//				producerMsgSizeBuckets[index].addAndGet(totalSize);
//			}
			allTaskStat.countProducerRate(totalCount, totalSize);
		} catch (Exception e) {}
	}
	/**
	 * 获取消费速率，单位：条/秒
	 * @return
	 */
	public int getConsumerCountRate() {
//		long total = 0;
//		for (AtomicLong ato : consumerMsgCountBuckets) {
//			total += ato.get();
//		}
//		if (total == 0) {
//			return 0;
//		}
//		BigDecimal dec = new BigDecimal(total);
//		return dec.divide(new BigDecimal(consumerMsgCountBuckets.length), 0, RoundingMode.HALF_UP).intValue();
		return allTaskStat.getConsumerCountRate();
	}
	/**
	 * 获取消费速率，单位：byte/秒
	 * @return
	 */
	public long getConsumerSizeRate() {
//		long total = 0;
//		for (AtomicLong ato : consumerMsgSizeBuckets) {
//			total += ato.get();
//		}
//		if (total == 0) {
//			return 0;
//		}
//		BigDecimal dec = new BigDecimal(total);
//		return dec.divide(new BigDecimal(consumerMsgSizeBuckets.length), 0, RoundingMode.HALF_UP).longValue();
		return allTaskStat.getConsumerSizeRate();
	}
	/**
	 * 获取生产速率，单位：条/秒
	 * @return
	 */
	public int getProducerCountRate() {
//		long total = 0;
//		for (AtomicLong ato : producerMsgCountBuckets) {
//			total += ato.get();
//		}
//		if (total == 0) {
//			return 0;
//		}
//		BigDecimal dec = new BigDecimal(total);
//		return dec.divide(new BigDecimal(producerMsgCountBuckets.length), 0, RoundingMode.HALF_UP).intValue();
		return allTaskStat.getProducerCountRate();
	}
	/**
	 * 获取生产速率，单位：byte/秒
	 * @return
	 */
	public long getProducerSizeRate() {
//		long total = 0;
//		for (AtomicLong ato : producerMsgSizeBuckets) {
//			total += ato.get();
//		}
//		if (total == 0) {
//			return 0;
//		}
//		BigDecimal dec = new BigDecimal(total);
//		return dec.divide(new BigDecimal(producerMsgSizeBuckets.length), 0, RoundingMode.HALF_UP).longValue();
		return allTaskStat.getProducerSizeRate();
	}
	/**
	 * 获取任务的最近一小时的统计信息
	 * @param taskCode
	 * @return
	 */
	public TaskCounter getTaskStat(String taskCode) {
		return taskStatMap.get(taskCode);
	}
	
	private void printStatInfo() {
		long totalMemory = getTotalMemory();
		long totalAvaiableMemory = getTotalAvaiableMemory();
		long totalUsedMemory = getTotalUsedMemory();
		logger.info("******************JVMTotalMemory:{}, messageQueueMaxMemory:{}, messageQueueUsedMemory:{}",
				ByteUtil.formatMaxUnit(totalMemory), ByteUtil.formatMaxUnit(totalAvaiableMemory), ByteUtil.formatMaxUnit(totalUsedMemory));
		long totalResourcePartition = 0;
		Map<String, Long> taskTotalAvaiableMemory = getAllTaskAvaiableMemory();
		int totalTaskSize = taskTotalAvaiableMemory != null ? taskTotalAvaiableMemory.size() : 0;
		int consumerCountRate = getConsumerCountRate();
		long consumerSizeRate = this.getConsumerSizeRate();
		int producerCountRate = this.getProducerCountRate();
		long producerSizeRate = this.getProducerSizeRate();
		for (Map.Entry<String, Long> entry : taskTotalAvaiableMemory.entrySet()) {
			String task = entry.getKey();
			Long avaiableMemory = entry.getValue();
			long usedMemory = this.getUsedMemoryForTask(task);
			int queueSize = dataCacheService.countQueue(task);
			long totalConsumerMsgCount = this.getTotalConsumerMsgCountForTask(task);
			long totalFilterMsgCount = this.getTotalFilterMsgCountForTask(task);
			long totalProducerMsgCount = this.getTotalProducerMsgCountForTask(task);
			long partitionNum = this.getTotalPartitionCountForTask(task);
			totalResourcePartition += partitionNum;
			logger.info("******************task[{}], sharding[{}], avaiableMemory[{}], usedMemory[{}], queueSize[{}], consumerNum[{}], filterNum[{}], producerNum[{}],", 
					task,
					partitionNum,
					ByteUtil.formatMaxUnit(avaiableMemory),
					ByteUtil.formatMaxUnit(usedMemory), 
					queueSize,
					totalConsumerMsgCount,
					totalFilterMsgCount,
					totalProducerMsgCount);
		}
		logger.info("******************totalTask:{}, totalTaskPartition:{}", totalTaskSize, totalResourcePartition);
		logger.info("******************consumer rate, {} -- {}", consumerCountRate + " records/s", ByteUtil.formatMaxUnit(consumerSizeRate) + "/s");
		logger.info("******************producer rate, {} -- {}", producerCountRate + " records/s", ByteUtil.formatMaxUnit(producerSizeRate) + "/s");
//		logger.info("******************consumer pool,{}", consumerExcutorService.toString());
//		logger.info("******************producer pool,{}", producerExcutorService.toString());
	}
}
