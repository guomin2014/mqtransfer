package com.gm.mqtransfer.manager.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.gm.mqtransfer.facade.common.TaskStatusEnum;
import com.gm.mqtransfer.facade.model.AlarmConfig;
import com.gm.mqtransfer.facade.model.ConsumerConfig;
import com.gm.mqtransfer.facade.model.ProducerConfig;
import com.gm.mqtransfer.facade.model.TaskShardingConfig;
import com.gm.mqtransfer.facade.model.TransferCluster;
import com.gm.mqtransfer.facade.model.TransferConfig;
import com.gm.mqtransfer.facade.model.TransferPartitionConfig;
import com.gm.mqtransfer.facade.model.TransferTask;
import com.gm.mqtransfer.facade.model.TransferTaskPartition;
import com.gm.mqtransfer.facade.service.IApplicationStartedService;
import com.gm.mqtransfer.facade.service.plugin.PluginProviderManagerService;
import com.gm.mqtransfer.manager.support.helix.HelixMirrorMakerManager;
import com.gm.mqtransfer.module.cluster.model.ClusterEntity;
import com.gm.mqtransfer.module.cluster.service.ClusterService;
import com.gm.mqtransfer.module.support.EventType;
import com.gm.mqtransfer.module.task.listener.TaskChangeEvent;
import com.gm.mqtransfer.module.task.listener.TaskChangeListener;
import com.gm.mqtransfer.module.task.model.TaskEntity;
import com.gm.mqtransfer.module.task.service.TaskService;
import com.gm.mqtransfer.provider.facade.exception.BusinessException;
import com.gm.mqtransfer.provider.facade.model.ClusterInfo;
import com.gm.mqtransfer.provider.facade.model.TopicInfo;
import com.gm.mqtransfer.provider.facade.model.TopicPartitionInfo;
import com.gm.mqtransfer.provider.facade.model.TopicPartitionMapping;
import com.gm.mqtransfer.provider.facade.service.PluginProviderService;
import com.gm.mqtransfer.provider.facade.service.admin.AdminService;
import com.gm.mqtransfer.provider.facade.service.rebalance.AllocateQueueAveragelyByCircle;
import com.gm.mqtransfer.provider.facade.service.rebalance.AllocateQueueUtil;
import com.gm.mqtransfer.provider.facade.util.StringUtils;

@Service
public class TaskObserverService implements TaskShardingChangeListener, IApplicationStartedService {

	private final Logger logger = LoggerFactory.getLogger(TaskObserverService.class);
	
	private ScheduledExecutorService executor;
	
	private ScheduledExecutorService taskShardingObserverExecutor;
	
	private Map<String, TransferTask> taskMonitorMap = new ConcurrentHashMap<>();
	
	private Map<String, AdminService> adminServiceMap = new ConcurrentHashMap<>();
	
	@Autowired
	private TaskService taskService;
	@Autowired
	private ClusterService clusterService;
	@Autowired
	private HelixMirrorMakerManager helixMirrorMakerManager;
	@Autowired
//	@ArkInject //使用容器的类，则用该注解
	private PluginProviderManagerService pluginProviderManagerService;
	
	@Override
	public void start() {
		logger.info("starting monitor for task");
		//分片添加变更监控
		executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "Task-Observer-Thread");
			}
		});
		
		taskService.addListener(new TaskChangeListener() {
			@Override
			public void onChange(TaskChangeEvent event) {
				logger.info(event.toString());
				EventType type = event.getType();
				TaskEntity task = event.getData();
				TaskStatusEnum taskStatus = TaskStatusEnum.getByValue(task.getStatus());
				switch (type) {
				case ADDED:
					if (taskStatus == TaskStatusEnum.ENABLE) {
						addTask(task);
					}
					break;
				case UPDATED:
					if (taskStatus == TaskStatusEnum.ENABLE) {
						addTask(task);
					} else if (taskStatus == TaskStatusEnum.DISABLE) {
						removeTask(task);
					}
					break;
				case REMOVED:
					removeTask(task);
					break;
				}
			}
		});
		
		taskShardingObserverExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "Task-Sharding-Observer-Thread");
			}
		});
		taskShardingObserverExecutor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				try {
					monitorTopic();
				} catch (Throwable e) {
					logger.error("detect mismatched partition(queue) failed:", e);
				}
			}
		}, 3, 60, TimeUnit.SECONDS);
	}

	@Override
	public void stop() {
		logger.info("stoping monitor for task");
		if (executor != null) {
			executor.shutdown();
		}
		if (taskShardingObserverExecutor != null) {
			taskShardingObserverExecutor.shutdown();
		}
	}
	
	public void addTask(TaskEntity entity) {
		this.handlerTask(entity);
	}
	
	public void removeTask(TaskEntity entity) {
		//停止任务分区变更监控
		TransferTask task = this.taskMonitorMap.remove(entity.getCode());
		if (task != null) {
			//停止分区任务
			List<TopicPartitionMapping> allocateList = task.getPartitionMappingList();
			if (allocateList != null) {
				TransferConfig transferConfig = task.getTransferConfig();
				if (transferConfig == null) {
					transferConfig = new TransferConfig();
					transferConfig.setPartitionMatchStrategy(AllocateQueueAveragelyByCircle.name);
				}
				Map<String, TransferTaskPartition> transferPartitionConfigMap = new HashMap<>();
				if (transferConfig != null && StringUtils.isNotBlank(transferConfig.getFromPartitions())) {
					List<TransferTaskPartition> taskPartitionList = JSON.parseArray(transferConfig.getFromPartitions(), TransferTaskPartition.class);
					for (TransferTaskPartition transferTaskPartition : taskPartitionList) {
						transferPartitionConfigMap.put(transferTaskPartition.getPartitionKey(), transferTaskPartition);
					}
				}
				for (TopicPartitionMapping allocatePartition : allocateList) {
					TopicPartitionInfo fromPartition = allocatePartition.getFromPartition();
					TopicPartitionInfo toPartition = allocatePartition.getToPartition();
					TransferTaskPartition transferTaskPartition = transferPartitionConfigMap.get(fromPartition.getPartitionKey());
					TaskShardingConfig taskSharding = new TaskShardingConfig();
					taskSharding.setTaskCode(task.getCode());
					taskSharding.setFromCluster(task.getFromClusterInfo());
					taskSharding.setFromTopic(task.getFromTopic());
					taskSharding.setFromPartition(fromPartition);
					taskSharding.setToCluster(task.getToClusterInfo());
					taskSharding.setToTopic(task.getToTopic());
					taskSharding.setToPartition(toPartition);
					taskSharding.setConsumerConfig(task.getConsumerConfig());
					taskSharding.setProducerConfig(task.getProducerConfig());
					TransferPartitionConfig transferPartitionConfig = new TransferPartitionConfig();
					transferPartitionConfig.setCacheQueueMemoryMaxRatio(transferConfig.getCacheQueueMemoryMaxRatio());
					transferPartitionConfig.setMaxCacheRecords(transferConfig.getMaxCacheRecords());
					transferPartitionConfig.setPartitionMatchStrategy(transferConfig.getPartitionMatchStrategy());
					transferPartitionConfig.setPartitionMatchConfig(transferConfig.getPartitionMatchConfig());
					transferPartitionConfig.setPartition(transferTaskPartition);
					taskSharding.setTransferPartitionConfig(transferPartitionConfig);
					taskSharding.setAlarmConfig(task.getAlarmConfig());
					//停止分区任务
					this.deleteTaskSharding(taskSharding);
				}
			}
		}
	}
	
 	private TransferCluster convertClusterEntity(ClusterEntity entity) {
 		if (entity == null) {
 			return null;
 		}
 		TransferCluster cluster = new TransferCluster();
		cluster.setCode(entity.getCode());
		cluster.setName(entity.getName());
		cluster.setType(entity.getType());
		cluster.setVersion(entity.getVersion());
		cluster.setNameSvrAddr(entity.getNameSvrAddr());
		cluster.setZkUrl(entity.getZkUrl());
		cluster.setBrokerList(entity.getBrokerList());
		cluster.setClientVersion(entity.getClientVersion());
		return cluster;
 	}
 	
	private void handlerTask(TaskEntity entity) {
		if (this.taskMonitorMap.containsKey(entity.getCode())) {
			return;
		}
		TransferTask task = new TransferTask();
		task.setCode(entity.getCode());
		TransferCluster fromCluster = this.convertClusterEntity(clusterService.get(entity.getFromClusterCode()));
		if (fromCluster == null) {
			logger.warn("Source cluster information for task not found, skip it. --> {}", task.toString());
			return;
		}
		TransferCluster toCluster = this.convertClusterEntity(clusterService.get(entity.getToClusterCode()));
		if (toCluster == null) {
			logger.warn("Target cluster information for task not found, skip it. --> {}", task.toString());
			return;
		}
		task.setFromCluster(fromCluster.getCode());
		task.setFromClusterInfo(fromCluster);
		task.setFromTopic(entity.getFromTopic());
		task.setToCluster(toCluster.getCode());
		task.setToClusterInfo(toCluster);
		task.setToTopic(entity.getToTopic());
		ConsumerConfig consumerConfig = entity.getFromConfig();
		ProducerConfig producerConfig = entity.getToConfig();
		TransferConfig transferConfig = entity.getTransferConfig();
		if (transferConfig == null) {
			transferConfig = new TransferConfig();
			transferConfig.setPartitionMatchStrategy(AllocateQueueAveragelyByCircle.name);
		}
		Map<String, TransferTaskPartition> transferPartitionConfigMap = new HashMap<>();
		if (transferConfig != null && StringUtils.isNotBlank(transferConfig.getFromPartitions())) {
			List<TransferTaskPartition> taskPartitionList = JSON.parseArray(transferConfig.getFromPartitions(), TransferTaskPartition.class);
			for (TransferTaskPartition transferTaskPartition : taskPartitionList) {
				transferPartitionConfigMap.put(transferTaskPartition.getPartitionKey(), transferTaskPartition);
			}
		}
		AlarmConfig alarmConfig = JSON.parseObject(entity.getAlarmConfig(), AlarmConfig.class);
		task.setConsumerConfig(consumerConfig);
		task.setProducerConfig(producerConfig);
		task.setTransferConfig(transferConfig);
		task.setAlarmConfig(alarmConfig);
		// 判断任务是否分配
		List<TopicPartitionMapping> allocateList = taskService.getPartitionMapping(entity.getCode());//获取分区分配信息
		if (allocateList == null || allocateList.isEmpty()) {
			//获取任务的分区信息
			List<TopicPartitionInfo> fromPartitionList = null;
			List<TopicPartitionInfo> toPartitionList = null;
			try {
				fromPartitionList = this.fetchTopicPartitons(fromCluster, task.getFromTopic());
			} catch (Exception e) {
				logger.warn("Failed to obtain source partition information[{}], skip it. --> {}", e.getMessage(), task.toString());
				return;
			}
			try {
				toPartitionList = this.fetchTopicPartitons(toCluster, task.getToTopic());
			} catch (Exception e) {
				logger.warn("Failed to obtain target partition information[{}], skip it. --> {}", e.getMessage(), task.toString());
				return;
			}
			//根据策略进行分区匹配
			try {
				allocateList = this.allocatePartition(transferConfig.getPartitionMatchStrategy(), transferConfig.getPartitionMatchConfig(), fromPartitionList, toPartitionList);
			} catch (Exception e) {
				logger.warn("Failed to allocate partition information[{}], skip it. --> {}", e.getMessage(), task.toString());
				return;
			}
			task.setFromPartitionList(fromPartitionList);
			task.setToPartitionList(toPartitionList);
			task.setPartitionMappingList(allocateList);
			if (allocateList != null) {
				//更新分区分配信息
				taskService.updatePartitionMapping(entity, allocateList);
				logger.info("update task patition mapping-->taskCode:{},mapping:{}", entity.getCode(), allocateList);
			}
		} else {
			List<TopicPartitionInfo> fromPartitionList = new ArrayList<>();
			List<TopicPartitionInfo> toPartitionList = new ArrayList<>();
			for (TopicPartitionMapping mapping : allocateList) {
				if (mapping.getFromPartition() != null) {
					fromPartitionList.add(mapping.getFromPartition());
				}
				if (mapping.getToPartition() != null) {
					toPartitionList.add(mapping.getToPartition());
				}
			}
			task.setFromPartitionList(fromPartitionList);
			task.setToPartitionList(toPartitionList);
			task.setPartitionMappingList(allocateList);
		}
		//添加到资源任务中
		for (TopicPartitionMapping allocatePartition : allocateList) {
			TopicPartitionInfo fromPartition = allocatePartition.getFromPartition();
			TopicPartitionInfo toPartition = allocatePartition.getToPartition();
			TransferTaskPartition transferTaskPartition = transferPartitionConfigMap.get(fromPartition.getPartitionKey());
			TaskShardingConfig taskSharding = new TaskShardingConfig();
			taskSharding.setTaskCode(task.getCode());
			taskSharding.setFromCluster(fromCluster);
			taskSharding.setFromTopic(task.getFromTopic());
			taskSharding.setFromPartition(fromPartition);
			taskSharding.setToCluster(toCluster);
			taskSharding.setToTopic(task.getToTopic());
			taskSharding.setToPartition(toPartition);
			taskSharding.setConsumerConfig(consumerConfig);
			taskSharding.setProducerConfig(producerConfig);
			TransferPartitionConfig transferPartitionConfig = new TransferPartitionConfig();
			transferPartitionConfig.setCacheQueueMemoryMaxRatio(transferConfig.getCacheQueueMemoryMaxRatio());
			transferPartitionConfig.setMaxCacheRecords(transferConfig.getMaxCacheRecords());
			transferPartitionConfig.setPartitionMatchStrategy(transferConfig.getPartitionMatchStrategy());
			transferPartitionConfig.setPartitionMatchConfig(transferConfig.getPartitionMatchConfig());
			transferPartitionConfig.setPartition(transferTaskPartition);
			taskSharding.setTransferPartitionConfig(transferPartitionConfig);
			taskSharding.setAlarmConfig(alarmConfig);
			//添加到分片任务中
			this.addTaskSharding(taskSharding);
		}
		//将任务添加到分区变更监控
		this.taskMonitorMap.put(task.getCode(), task);
		logger.info("Add to partition change monitoring-->taskCode:{}", task.getCode());
	}
	
	private void addTaskSharding(TaskShardingConfig taskSharding) {
		helixMirrorMakerManager.addResourceInMirrorMaker(taskSharding, false);
	}
	
	private void deleteTaskSharding(TaskShardingConfig taskSharding) {
		helixMirrorMakerManager.disableResourceSharding(taskSharding);
	}
	
	private void monitorTopic() {
		if (!taskMonitorMap.isEmpty()) {
			//只监控了源topic的分区变更，后期考虑是否对目标topic进行监控
			for (TransferTask task : taskMonitorMap.values()) {
				List<TopicPartitionInfo> fromPartitionList = null;
				try {
					fromPartitionList = this.fetchTopicPartitons(task.getFromClusterInfo(), task.getFromTopic());
				} catch (Exception e) {
					logger.warn("Failed to obtain source partition information[{}], skip it. --> {}", e.getMessage(), task.toString());
					continue;
				}
				if (partitionChange(task.getFromPartitionList(), fromPartitionList)) {//分区有变更，需要更新任务
					this.onPartitionChange(task, task.getFromPartitionList(), fromPartitionList);
				}
			}
		}
	}
	/**
	 * 获取topic的分区信息
	 * @param cluster
	 * @param topic
	 * @return
	 */
	public List<TopicPartitionInfo> fetchTopicPartitons(ClusterInfo cluster, String topic) {
		if (cluster == null || StringUtils.isBlank(cluster.getCode()) || StringUtils.isBlank(cluster.getType())) {
			throw new BusinessException("Incomplete cluster information");
		}
		AdminService adminService = null;
		if (adminServiceMap.containsKey(cluster.getCode())) {
			adminService = adminServiceMap.get(cluster.getCode());
		} else {
			PluginProviderService pluginProviderService = pluginProviderManagerService.getOnePluginProviderService(cluster);
			if (pluginProviderService == null) {
				throw new BusinessException("No matching abilities found");
			}
			adminService = pluginProviderService.getServiceFactory().createAdminService(cluster.toPluginClusterInfo());
			if (adminService != null) {
				adminServiceMap.put(cluster.getCode(), adminService);
			}
		}
		if (adminService == null) {
			throw new BusinessException("No matching admin abilities found");
		}
		TopicInfo topicInfo = adminService.getTopicInfo(topic);
		List<TopicPartitionInfo> partitionList = topicInfo.getPartitions();
		if (partitionList == null || partitionList.isEmpty()) {
			throw new BusinessException("No matching partition found");
		}
		return partitionList;
	}
	public List<TopicPartitionMapping> allocatePartition(String partitionMatchStrategy, String partitionMatchConfig, List<TopicPartitionInfo> fromPartitionList, List<TopicPartitionInfo> toPartitionList) {
        return AllocateQueueUtil.allocatePartition(partitionMatchStrategy, JSON.parseObject(partitionMatchConfig, Properties.class), fromPartitionList, toPartitionList);
	}
	/**
	 * 判断分区信息是否存在变更
	 * @param sourcePartitionList
	 * @param targetPartitionList
	 * @return
	 */
	private boolean partitionChange(List<TopicPartitionInfo> sourcePartitionList, List<TopicPartitionInfo> targetPartitionList) {
		if ((sourcePartitionList == null || sourcePartitionList.isEmpty()) && (targetPartitionList == null || targetPartitionList.isEmpty())) {
			return false;
		}
		if ((sourcePartitionList == null || sourcePartitionList.isEmpty()) || (targetPartitionList == null || targetPartitionList.isEmpty())) {
			return true;
		}
		if (sourcePartitionList.size() != targetPartitionList.size()) {
			return true;
		}
		Map<String, TopicPartitionInfo> sourceMap = sourcePartitionList.stream().collect(Collectors.toMap(TopicPartitionInfo::getPartitionKey, tp -> tp, (k1,k2)->k1));
		Map<String, TopicPartitionInfo> targetMap = targetPartitionList.stream().collect(Collectors.toMap(TopicPartitionInfo::getPartitionKey, tp -> tp, (k1,k2)->k1));
		for (String sourceKey : sourceMap.keySet()) {
			if (!targetMap.containsKey(sourceKey)) {
				return true;
			}
		}
		for (String targetKey : targetMap.keySet()) {
			if (!sourceMap.containsKey(targetKey)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onPartitionChange(TransferTask task, List<TopicPartitionInfo> oldPartitionList, List<TopicPartitionInfo> newPartitionList) {
		// TODO Auto-generated method stub
		
	}
}
