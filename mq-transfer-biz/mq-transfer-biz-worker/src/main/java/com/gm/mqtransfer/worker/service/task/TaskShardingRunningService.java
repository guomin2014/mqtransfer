package com.gm.mqtransfer.worker.service.task;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.gm.mqtransfer.facade.model.ConsumerConfig;
import com.gm.mqtransfer.facade.model.CustomMessage;
import com.gm.mqtransfer.facade.model.ProducerConfig;
import com.gm.mqtransfer.facade.model.TaskShardingConfig;
import com.gm.mqtransfer.facade.model.TransferCluster;
import com.gm.mqtransfer.facade.model.TransferPartitionConfig;
import com.gm.mqtransfer.facade.service.IApplicationStartedService;
import com.gm.mqtransfer.facade.service.IService;
import com.gm.mqtransfer.facade.service.alarm.AlarmService;
import com.gm.mqtransfer.facade.service.plugin.PluginProviderManagerService;
import com.gm.mqtransfer.provider.facade.exception.BusinessException;
import com.gm.mqtransfer.provider.facade.model.ConsumerClientConfig;
import com.gm.mqtransfer.provider.facade.model.ProducerClientConfig;
import com.gm.mqtransfer.provider.facade.model.TopicPartitionInfo;
import com.gm.mqtransfer.provider.facade.service.PluginProviderService;
import com.gm.mqtransfer.provider.facade.service.consumer.ConsumerService;
import com.gm.mqtransfer.provider.facade.service.producer.ProducerService;
import com.gm.mqtransfer.provider.facade.util.PartitionUtils;
import com.gm.mqtransfer.provider.facade.util.StringUtils;
import com.gm.mqtransfer.worker.config.CommonConfiguration;
import com.gm.mqtransfer.worker.model.Task;
import com.gm.mqtransfer.worker.model.TaskSharding;
import com.gm.mqtransfer.worker.service.cache.StatService;
import com.gm.mqtransfer.worker.service.task.cluster.lock.ResourceLock;
import com.gm.mqtransfer.worker.service.task.thread.ConsumerThread;
import com.gm.mqtransfer.worker.service.task.thread.ProducerThread;

@Component
public class TaskShardingRunningService implements IService, IApplicationStartedService{
	
	private static final Logger logger = LoggerFactory.getLogger(TaskShardingRunningService.class);
	
	/** 当前服务停止状态 */
	private volatile AtomicBoolean isRunning = new AtomicBoolean(false);
	/** 消费任务分配是否停止 */
	private boolean stopConsumerTaskDispatcher = false;
	private final AtomicInteger stopTaskIdx = new AtomicInteger(0);
	/** 消费任务分配线程 */
	private Thread consumerTaskDispatcherThread;
	/** 消费者线程池 */
	private ThreadPoolExecutor consumerExcutorService;
	/** 生产任务分配是否停止 */
	private boolean stopProducerTaskDispatcher = false;
	/** 生产任务分配线程 */
	private Thread producerTaskDispatcherThread;
	/** 生产者线程池 */
	private ThreadPoolExecutor producerExcutorService;
	
	/** 分片任务集合，key:任务编号, value-key：分片任务唯一标识 */
	private Map<String, Task> taskRunningMap = new ConcurrentHashMap<>();
	
	private final ReentrantLock consumerLock = new ReentrantLock();
	private final ReentrantLock producerLock = new ReentrantLock();
	/** 资源锁 */
	private final ResourceLock resourceLock = new ResourceLock(10);
	
	@Autowired
	private CommonConfiguration commonConfiguration;
	@Autowired
	private StatService statService;
	@Autowired(required=false)
	private AlarmService alarmService;
	@Autowired(required=false)
	private PluginProviderManagerService pluginProviderManagerService;
	
	@Override
	public int order() {
		return 1;
	}
	public void init() {
		int consumerPoolRatio = commonConfiguration.getMessageConsumerThreadPoolRatio();
		int producerPoolRatio = commonConfiguration.getMessageProducerThreadPoolRatio();
		int consumerMaxPoolSize = 16 + Runtime.getRuntime().availableProcessors() * consumerPoolRatio;
		int producerMaxPoolSize = 16 + Runtime.getRuntime().availableProcessors() * producerPoolRatio;
		long keepAliveTime = 60;
		logger.info("starting init consumer pool, ratio[{}], corePollSize[{}]", consumerPoolRatio, consumerMaxPoolSize);
		//初始化消费线程池
		consumerExcutorService = new ThreadPoolExecutor(consumerMaxPoolSize, consumerMaxPoolSize,
				keepAliveTime, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new ThreadFactory() {
					private AtomicInteger idx = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "worker-consumer-" + idx.incrementAndGet());
                        return t;
                    }
        });
		consumerTaskDispatcherThread = new Thread(createConsumerDispatcherTask(), "worker-consumer-task-Dispatcher");
		logger.info("starting init producer pool, ratio[{}], corePollSize[{}]", producerPoolRatio, producerMaxPoolSize);
		//初始化生产者线程池
		producerExcutorService = new ThreadPoolExecutor(producerMaxPoolSize, producerMaxPoolSize,
				keepAliveTime, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new ThreadFactory() {
					private AtomicInteger idx = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "worker-producer-" + idx.incrementAndGet());
                        return t;
                    }
        });
		producerTaskDispatcherThread = new Thread(createProducerDispatcherTask(), "worker-producer-task-Dispatcher");
	}
	/**
	 * 重新初始化消费者线程池
	 * @param consumerPoolRatio
	 */
	public void reInitConsumerExcutorService(int consumerPoolRatio) {
		int consumerMaxPoolSize = 16 + Runtime.getRuntime().availableProcessors() * consumerPoolRatio;
		logger.info("starting reinit consumer pool, ratio[{}], corePollSize[{}]", consumerPoolRatio, consumerMaxPoolSize);
		consumerExcutorService.setCorePoolSize(consumerMaxPoolSize);
		consumerExcutorService.setMaximumPoolSize(consumerMaxPoolSize);
	}
	/**
	 * 重新初始化生产者线程池
	 * @param producerPoolRatio
	 */
	public void reInitProducerExcutorService(int producerPoolRatio) {
		int producerMaxPoolSize = 16 + Runtime.getRuntime().availableProcessors() * producerPoolRatio;
		logger.info("starting reinit producer pool, ratio[{}], corePollSize[{}]", producerPoolRatio, producerMaxPoolSize);
		producerExcutorService.setCorePoolSize(producerMaxPoolSize);
		producerExcutorService.setMaximumPoolSize(producerMaxPoolSize);
	}
	public String generateConsumerGroup(TaskShardingConfig task) {
		//自定义消费者组格式：consumer_group_任务ID
		return "group_mqtransfer_" + task.getTaskCode();
	}
	/**
	 * 初始化消费者
	 */
	public void initConsumer(Task task, TaskSharding taskSharding) {
		//判断是否启用共享消费者模式：一个任务下的多个分区共同使用一个消费者(减少消费者客户端本身的消费线程)
		consumerLock.lock();
		try {
			TaskShardingConfig taskConfig = taskSharding.getTaskShardingConfig();
			ConsumerConfig consumerConfig = taskConfig.getConsumerConfig();
			boolean clientShare = consumerConfig != null ? consumerConfig.isClientShare() : false;
			ConsumerService consumerService = null;
			if (clientShare) {
				if (task.getConsumer() != null) {
					consumerService = task.getConsumer();
				}
			}
			if (consumerService == null) {
				//初始化消费者
				Properties consumerProps = consumerConfig != null && StringUtils.isNotBlank(consumerConfig.getClientConfig()) ? JSON.parseObject(consumerConfig.getClientConfig(), Properties.class) : new Properties();
				int instanceIndex = 1;
				PluginProviderService pluginProviderService = pluginProviderManagerService.getOnePluginProviderService(taskConfig.getFromCluster());
				if (pluginProviderService == null) {
					throw new BusinessException("No matching abilities found for the source cluster");
				}
				ConsumerClientConfig config = new ConsumerClientConfig();
				config.setClientShare(clientShare);
				config.setCluster(taskConfig.getFromCluster().toPluginClusterInfo());
				config.setInstanceIndex(instanceIndex);
				config.setInstanceCode(taskConfig.getTaskCode());
				config.setConsumerProps(consumerProps);
				config.setConsumerGroup(this.generateConsumerGroup(taskConfig));
				consumerService = pluginProviderService.getServiceFactory().createConsumerService(config);
				if (clientShare) {
					task.setConsumer(consumerService);
				}
				try {
					//启动消费者
					consumerService.start();
				} catch (Exception e) {
					//允许启动失败，后续由任务线程进行重试
					logger.error("start consumer failure-->" + taskConfig.toFromString(), e);
				}
			}
			taskSharding.setConsumer(consumerService);
			ConsumerThread ct = new ConsumerThread(taskSharding, commonConfiguration);
			taskSharding.setConsumerThread(ct);
		} finally {
			consumerLock.unlock();
		}
	}
	/**
	 * 初始化生产者
	 */
	public void initProducer(Task task, TaskSharding taskSharding) {
		//判断是否启用共享生产者模式：一个任务下的多个分区共同使用一个生产者(减少生产者客户端本身的生产线程)
		producerLock.lock();
		try {
			TaskShardingConfig taskConfig = taskSharding.getTaskShardingConfig();
			ProducerConfig producerConfig = taskConfig.getProducerConfig();
			boolean clientShare = producerConfig != null ? producerConfig.isClientShare() : false;
			ProducerService<CustomMessage> producer = null;
			if (clientShare) {
				if (task.getProducer() != null) {
					producer = task.getProducer();
				}
			}
			if (producer == null) {
				//初始化生产者
				PluginProviderService pluginProviderService = pluginProviderManagerService.getOnePluginProviderService(taskConfig.getToCluster());
				if (pluginProviderService == null) {
					throw new BusinessException("No matching abilities found for the target cluster");
				}
				Properties producerProps = producerConfig != null && StringUtils.isNotBlank(producerConfig.getClientConfig()) ? JSON.parseObject(producerConfig.getClientConfig(), Properties.class) : new Properties();
				int instanceIndex = 1;
				ProducerClientConfig config = new ProducerClientConfig();
				config.setClientShare(clientShare);
				config.setCluster(taskConfig.getToCluster().toPluginClusterInfo());
				config.setInstanceIndex(instanceIndex);
				config.setInstanceCode(taskConfig.getTaskCode());
				config.setProducerProps(producerProps);
				producer = pluginProviderService.getServiceFactory().createProducerService(config);
				if (clientShare) {
					task.setProducer(producer);
				}
				try {
					//启动生产者
					producer.start();
				} catch (Exception e) {
					//允许启动失败，后续由任务线程进行重试
					logger.error("start producer failure-->" + taskConfig.toToString(), e);
				}
			}
			taskSharding.setProducer(producer);
			ProducerThread pt = new ProducerThread(taskSharding, commonConfiguration);
			taskSharding.setProducerThread(pt);
		} finally {
			producerLock.unlock();
		}
	}
	
	public void start() {
		logger.info("starting task service...");
		this.init();
		//启动消费分发任务
		consumerTaskDispatcherThread.start();
		//启动生产分发任务
		producerTaskDispatcherThread.start();
		
		isRunning.set(true);
	}
	
	public void stop() {
		logger.info("stoping task service...");
		isRunning.set(false);
		//在生产者消息发送成功后，才会将对应消费者的位置点提交，故需要先将生产者停止后，才能停止消费者
		try {
			stopProducerTaskDispatcher = true;
			if (producerTaskDispatcherThread != null) {
				producerTaskDispatcherThread.interrupt();
			}
		} catch (Throwable e) {}
		try {
			stopConsumerTaskDispatcher = true;
			if (consumerTaskDispatcherThread != null) {
				consumerTaskDispatcherThread.interrupt();
			}
		} catch (Throwable e) {}
		try {
			this.stopAllTask();
		} catch (Throwable e) {}
		try {
			if (producerExcutorService != null) {
				producerExcutorService.shutdown();
			}
		} catch (Throwable e) {}
		try {
			if (consumerExcutorService != null) {
				consumerExcutorService.shutdown();
			}
		} catch (Throwable e) {}
		try {
			int checkTimes = 0;
			while (!producerExcutorService.isTerminated() && checkTimes < 200) {
				sleepUninterruptible(100);
				checkTimes++;
			}
		} catch (Throwable e) {}
		try {
			int checkTimes = 0;
	        while (!consumerExcutorService.isTerminated() && checkTimes < 200) {
	            sleepUninterruptible(100);
	            checkTimes++;
	        }
		} catch (Throwable e) {}
	}
	protected void sleepUninterruptible(long timeMs) {
        try {
            Thread.sleep(timeMs);
        } catch (InterruptedException e) {
            logger.warn("sleep Interrupted!");
        }
    }
	private Runnable createConsumerDispatcherTask() {
		Runnable task = new Runnable() {
			@Override
			public void run() {
				logger.info("starting dispatcher consumer task...current pool:{}", consumerExcutorService.toString());
				while(!stopConsumerTaskDispatcher) {
					try {
						//从队列中获取一个任务，放入消费者线程池，不使用poll的原因是当停止或删除任务时，可能造成找不到匹配的任务的情况
						if (taskRunningMap.isEmpty()) {
							sleepUninterruptible(1000);
							continue;
						}
						//检查总可用内存是否超限
						boolean overLimit = statService.doCheckMemoryOverLimitForTotalUsed();
						if (overLimit) {
							logger.warn("total available memory queue exceeded, limit:{}", statService.getTotalAvaiableMemory());
							sleepUninterruptible(2000);
							continue;
						}
						Set<ConsumerThread> cts = new HashSet<>();//用于打乱顺序，避免同一任务的所有分区同时执行，阻塞其它任务执行
						for (Map.Entry<String, Task> entry : taskRunningMap.entrySet()) {
							Map<String, TaskSharding> ctMap = entry.getValue().getTaskShardingMap();
							for (Map.Entry<String, TaskSharding> ee : ctMap.entrySet()) {
								cts.add(ee.getValue().getConsumerThread());
							}
						}
						int runCount = 0;
						for (ConsumerThread ct : cts) {
							if (stopConsumerTaskDispatcher) {//快速停止
								break;
							}
							if (ct.runable()) {
								//在任务删除后，可能造成获取到已分配的任务，故添加运行状态标识来区分任务是否分配
								boolean result = ct.lockRunning();
								if (result) {
									consumerExcutorService.submit(ct);
									runCount++;
								}
							}
						}
						if (stopConsumerTaskDispatcher) {
							break;
						}
						sleepUninterruptible(runCount == 0 ? 100 : 1);
					} catch (Exception e) {
						logger.error("dispatcher consumer task error", e);
						if (stopConsumerTaskDispatcher) {
							break;
						}
						sleepUninterruptible(1000);
					}
				}
				logger.info("dispatcher consumer task is stopped!");
			}
		};
		return task;
	}
	private Runnable createProducerDispatcherTask() {
		Runnable task = new Runnable() {
			@Override
			public void run() {
				logger.info("starting dispatcher producer task...current pool:{}", producerExcutorService.toString());
				while(!stopProducerTaskDispatcher) {
					try {
						//从队列中获取一个任务，放入消费者线程池，不使用poll的原因是当停止或删除任务时，可能造成找不到匹配的任务的情况
						if (taskRunningMap.isEmpty()) {
							sleepUninterruptible(1000);
							continue;
						}
						Set<ProducerThread> cts = new HashSet<>();//用于打乱顺序，避免同一任务的所有分区同时执行，阻塞其它任务执行
						for (Map.Entry<String, Task> entry : taskRunningMap.entrySet()) {
							Map<String, TaskSharding> ctMap = entry.getValue().getTaskShardingMap();
							for (Map.Entry<String, TaskSharding> ee : ctMap.entrySet()) {
								cts.add(ee.getValue().getProducerThread());
							}
						}
						int runCount = 0;
						for (ProducerThread ct : cts) {
							if (stopProducerTaskDispatcher) {//快速停止
								break;
							}
							if (ct.runable()) {
								//在任务删除后，可能造成获取到已分配的任务，故添加运行状态标识来区分任务是否分配
								boolean result = ct.lockRunning();
								if (result) {
									producerExcutorService.submit(ct);
									runCount++;
								}
							}
						}
						if (stopProducerTaskDispatcher) {
							break;
						}
						sleepUninterruptible(runCount == 0 ? 100 : 1);
					} catch (Exception e) {
						logger.error("dispatcher producer task error", e);
						if (stopProducerTaskDispatcher) {
							break;
						}
						sleepUninterruptible(1000);
					}
				}
				logger.info("dispatcher producer task is stopped!");
			}
		};
		return task;
	}
	/**
	 * 获取锁
	 * @param resourceName	资源名
	 * @param partition		资源下的分区，仅当当前worker停止时，该字段才为空，故在worker停止时，资源锁最高优先级
	 * @return
	 */
	public long tryLock(String resourceName, String partitionKey) {
        if (StringUtils.isBlank(partitionKey)) {//当前请求需要获取资源锁
        	try {
        		return resourceLock.tryLockTable(30, TimeUnit.SECONDS);
        	} catch (Exception e) {
        		return 0;
        	}
        } else {//当前请求需要获取分区锁
        	String resourcePartitionKey = PartitionUtils.generatorResourcePartitionKey(resourceName, partitionKey);
        	try {
        		return resourceLock.tryLockRow(resourcePartitionKey, 30, TimeUnit.SECONDS);
        	} catch (Exception e) {
        		return 0;
        	}
        }
	}
	public void unlock(String resourceName, String partitionKey, long stamp) {
		if (stamp <= 0) {
			return;
		}
		if (StringUtils.isBlank(partitionKey)) {//当前请求也需要获取资源锁
			resourceLock.unLockTable(stamp);
		} else {//当前请求需要获取分区锁
			String resourcePartitionKey = PartitionUtils.generatorResourcePartitionKey(resourceName, partitionKey);
			resourceLock.unlockRow(resourcePartitionKey, stamp);
		}
	}
	/**
	 * 添加任务
	 * @param resource
	 */
	public void addTask(TaskShardingConfig taskConfig) {
		String taskCode = taskConfig.getTaskCode();
		TransferCluster fromCluster = taskConfig.getFromCluster();
		TransferCluster toCluster = taskConfig.getToCluster();
		String fromClusterName = fromCluster.getName();
		String toClusterName = toCluster.getName();
		TopicPartitionInfo fromPartition = taskConfig.getFromPartition();
		TopicPartitionInfo toPartition = taskConfig.getToPartition();
		String fromPartitionKey = fromPartition.getPartitionKey();
		if (toPartition == null) {//源分区未匹配到接收分区
			logger.warn("Unable to find matching target partition, skip it -->taskCode:" + taskCode + "," + fromClusterName + "(" + taskConfig.getFromTopic() + "),partition:" + fromPartitionKey);
			return;
		}
		String toPartitionKey = toPartition.getPartitionKey();
		logger.info("add task({}) --> {}@{}({}) To {}@{}({})", taskCode, fromClusterName, taskConfig.getFromTopic(), fromPartitionKey, toClusterName, taskConfig.getToTopic(), toPartitionKey);
		if (!isRunning.get()) {
			logger.warn("add task failure, service is stoping, skip it --> [{}]{}@{}({}) To {}@{}({})", taskCode, fromClusterName, taskConfig.getFromTopic(), fromPartitionKey, toClusterName, taskConfig.getToTopic(), toPartitionKey);
			return;
		}
		long stamp = this.tryLock(taskCode, fromPartitionKey);
		if (stamp <= 0) {
			logger.warn("add task failure, count found lock, skip it --> [{}]{}@{}({}) To {}@{}({})", taskCode, fromClusterName, taskConfig.getFromTopic(), fromPartitionKey, toClusterName, taskConfig.getToTopic(), toPartitionKey);
			//告警
			alarmService.alarm(String.format("新增任务失败：[%s]%s@%s(%s) To %s@%s(%s)", taskCode, fromClusterName, taskConfig.getFromTopic(), fromPartitionKey, toClusterName, taskConfig.getToTopic(), toPartitionKey));
			return;
		}
		try {
			//判断任务是否已经存在
			Task task = new Task();
			Task oldTask = taskRunningMap.putIfAbsent(taskCode, task);
			if (oldTask != null) {
				task = oldTask;
			}
			if (task.existsTaskSharding(fromPartitionKey)) {
				logger.warn("add task failure, task already exists, skip it-->id:" + taskCode + "," + fromClusterName + "(" + taskConfig.getFromTopic() + "),partition:" + fromPartitionKey);
				return;
			}
			//初始化配置信息
			TransferPartitionConfig transferConfig = taskConfig.getTransferPartitionConfig();
			TaskSharding taskSharding = new TaskSharding(taskConfig);
			//计算最大可用内存(资源名=任务ID)
			Integer maxMemoryRatio = transferConfig != null ? transferConfig.getCacheQueueMemoryMaxRatio() : null;//最大内存占比,1-100
			this.statService.addAndCountTotalAvaiableMemoryForTask(taskCode, maxMemoryRatio);
			this.statService.incrTotalPartitionCountForTask(taskCode);
			this.initConsumer(task, taskSharding);
			this.initProducer(task, taskSharding);
			task.addTaskSharding(fromPartitionKey, taskSharding);
		} finally {
			this.unlock(taskCode, fromPartitionKey, stamp);
		}
	}
	
	/**
	 * 删除指定的任务分片任务
	 * @param resourceName
	 * @param partition
	 */
	public void deleteTask(TaskShardingConfig taskConfig) {
		if (taskConfig == null) {
			return;
		}
		String taskCode = taskConfig.getTaskCode();
		TopicPartitionInfo fromPartition = taskConfig.getFromPartition();
		String fromPartitionKey = fromPartition.getPartitionKey();
		logger.info("remove task({})-->partition:{}", taskCode, fromPartitionKey);
		if (!isRunning.get() && taskCode != null) {
			logger.warn("remove task failure, service is stoping, skip it-->taskCode:{},partition:{}", taskCode, fromPartitionKey);
			return;
		}
		long stamp = this.tryLock(taskCode, fromPartitionKey);
		if (stamp <= 0) {
			logger.warn("remove task failure, count found lock, skip it --> taskCode:{},partition:{}", taskCode, fromPartitionKey);
			//告警
			alarmService.alarm(String.format("删除任务失败：taskCode:%s,partition:%s", taskCode, fromPartitionKey));
			return;
		}
		boolean existsOtherSharding = true;
		try {
			Task task = this.taskRunningMap.get(taskCode);
			if (task == null) {
				logger.warn("remove task failure, task is not exists, skip it-->taskCode:{},partition:{}", taskCode, fromPartitionKey);
				return;
			}
			TaskSharding taskShardingRunningInfo = task.removeTaskSharding(fromPartitionKey);
			if (taskShardingRunningInfo == null) {
				logger.warn("remove task failure, task is not exists, skip it-->taskCode:{},partition:{}", taskCode, fromPartitionKey);
				return;
			}
			existsOtherSharding = task.existsTaskSharding();
			if (!existsOtherSharding) {
				taskRunningMap.remove(taskCode);
			}
			ThreadPoolExecutor stopExcutorService = null;
			try {
				//没有其它分区任务，或者未开启共享客户端，则需要关闭客户端
				boolean closeProducerServer = !existsOtherSharding && taskShardingRunningInfo.getProducer().isShare();
				boolean closeConsumerServer = !existsOtherSharding && taskShardingRunningInfo.getConsumer().isShare();
				taskShardingRunningInfo.getConsumerThread().stopMark();//将线程标识为停止
				taskShardingRunningInfo.getProducerThread().stopMark();
				try {
					int maxPoolSize = 2;
					stopExcutorService = new ThreadPoolExecutor(maxPoolSize, maxPoolSize,
							10, TimeUnit.SECONDS,
			                new LinkedBlockingQueue<Runnable>(),
			                new ThreadFactory() {
			                    @Override
			                    public Thread newThread(Runnable r) {
			                        Thread t = new Thread(r, "worker-task-stop-" + stopTaskIdx.incrementAndGet());
			                        return t;
			                    }
			        });
					boolean normalEnd = true;
					//停止生产任务
					CountDownLatch countDown = new CountDownLatch(2);
					//使用线程池停止所有分区的任务(部份版本存在停止消费时卡死的情况，比如：kafka0.8.2，固使用异步方式)
					stopExcutorService.submit(new Runnable() {
						@Override
						public void run() {
							try {
								taskShardingRunningInfo.getProducerThread().stop();//停止任务线程
								if (closeProducerServer) {
									taskShardingRunningInfo.getProducer().stop();
								}
							} finally {
								countDown.countDown();
							}
						}
					});
					//使用线程池停止所有分区的任务
					stopExcutorService.submit(new Runnable() {
						@Override
						public void run() {
							try {
								taskShardingRunningInfo.getConsumerThread().stop();//停止任务线程
								if (closeConsumerServer) {
									taskShardingRunningInfo.getConsumer().stop();
								}
							} finally {
								countDown.countDown();
							}
						}
					});
					try {
						normalEnd = countDown.await(61, TimeUnit.SECONDS);
					} catch (Exception e) {
						normalEnd = false;
					}
					if (!normalEnd) {
						logger.error("task failed to stop normally after 61 seconds");
					}
				} catch(Exception e) {
					logger.error("stop failure-->taskId:" + taskCode, e);
				}
				this.statService.decrTotalPartitionCountForTask(taskCode);
			} finally {
				if (stopExcutorService != null) {
					try {
						stopExcutorService.shutdownNow();
					} catch (Exception e) {}
				}
			}
		} finally {
			this.unlock(taskCode, fromPartitionKey, stamp);
		}
		if (!existsOtherSharding) {
			//需要对资源进行锁
			stamp = this.tryLock(taskCode, null);
			if (stamp > 0) {
				try {
					//清除相关资源
					long producerMsgCount = this.statService.removeAndGetProducerMsgCount(taskCode);
					//清除任务的可用内存分配
					long totalAvaiableMemory = this.statService.removeAndCountTotalAvaiableMemoryForTask(taskCode);
					long consumerMsgCount = this.statService.removeAndGetConsumerMsgCount(taskCode);
					long filterMsgCount = this.statService.removeAndGetFilterMsgCount(taskCode);
					long remainMemory = this.statService.removeAndGetUsedMemoryForTask(taskCode);
					this.statService.removeAndGetPartitionCount(taskCode);
					logger.info("clear stat result, taskId: {}, totalAvaiableMemory: {}byte, totalUsedMemory: {}byte, consumerMsgCount: {}, filterMsgCount: {}, producerMsgCount: {}", 
						taskCode, totalAvaiableMemory, remainMemory, consumerMsgCount, filterMsgCount, producerMsgCount);
					if (remainMemory != 0) {//生产还未结束或异常结束，导致未正确更新占用内存
						long totalUsedMemory = this.statService.decrTotalMemoryUsed(remainMemory);
						logger.error("task memory stat error, need to decr total memory for current taskId: {}, remainMemoryForTask: {}byte, totalUsedMemory: {}byte", taskCode, remainMemory, totalUsedMemory);
					}
//				//将统计信息存储到helix集群
//				try {
//					ResourceStatInfo statInfo = new ResourceStatInfo(consumerMsgCount, filterMsgCount, producerMsgCount);
//					HelixManagerUtil.saveResourceInfo(this.controller.getHelixManager(), taskCode, JSONObject.toJSONString(statInfo));
//				} catch (Exception e) {
//					logger.error(String.format("save task memory stat error, taskId: %s, consumerMsgCount: %s, filterMsgCount: %s, producerMsgCount: %s", 
//							taskCode, consumerMsgCount, filterMsgCount, producerMsgCount), e);
//				}
				} finally {
					this.unlock(taskCode, null, stamp);
				}
			}
		}
	}
	private void stopAllTask() {
		logger.info("stoping consumer and producer task...");
		for (String taskCode : taskRunningMap.keySet()) {
			Task taskRunning = taskRunningMap.get(taskCode);
			for (Map.Entry<String, TaskSharding> entry : taskRunning.getTaskShardingMap().entrySet()) {
				this.deleteTask(entry.getValue().getTaskShardingConfig());
			}
		}
	}
	
	public Task getTask(String taskCode) {
		return taskRunningMap.get(taskCode);
	}
	
}
