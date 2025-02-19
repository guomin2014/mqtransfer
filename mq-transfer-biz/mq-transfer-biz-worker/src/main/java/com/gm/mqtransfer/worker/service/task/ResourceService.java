package com.gm.mqtransfer.worker.service.task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.gm.mqtransfer.facade.exception.BusinessException;
import com.gm.mqtransfer.facade.model.TaskMessage;
import com.gm.mqtransfer.facade.model.TaskShardingConfig;
import com.gm.mqtransfer.facade.model.TransferCluster;
import com.gm.mqtransfer.facade.model.TransferConfig;
import com.gm.mqtransfer.facade.model.TransferPartitionConfig;
import com.gm.mqtransfer.facade.model.TransferTaskPartition;
import com.gm.mqtransfer.facade.service.IApplicationService;
import com.gm.mqtransfer.facade.service.plugin.PluginProviderManagerService;
import com.gm.mqtransfer.module.cluster.model.ClusterEntity;
import com.gm.mqtransfer.module.cluster.service.ClusterService;
import com.gm.mqtransfer.module.task.model.TaskEntity;
import com.gm.mqtransfer.module.task.model.TaskMigrationStatus;
import com.gm.mqtransfer.module.task.service.TaskService;
import com.gm.mqtransfer.provider.facade.model.TopicPartitionMapping;
import com.gm.mqtransfer.provider.facade.service.PluginProviderService;
import com.gm.mqtransfer.provider.facade.service.rebalance.AllocateQueueAveragelyByCircle;

/**
 * 集群模式，由远程统一指定任务
 * @author GM
 * @date 2024-05-13
 */
@Component
public class ResourceService implements IApplicationService {
	
	private final Logger logger = LoggerFactory.getLogger(ResourceService.class);
	
	@Autowired
	private PluginProviderManagerService pluginProviderManagerService;
	@Autowired
	private TaskShardingRunningService taskShardingService;
	@Autowired
	private TaskService taskService;
	@Autowired
	private ClusterService clusterService;
	
	private Map<String, TaskShardingConfig> waitAddMap = new ConcurrentHashMap<>();
	
	private Thread waitAddThread;
	
	private boolean waitAddThreadStop = false;
	
	public void addResource(TaskMessage message) {
		logger.info("add resource --> {}", message);
		String key = this.generationTaskKey(message);
		if (waitAddMap.containsKey(key)) {
			return;
		}
		TaskShardingConfig task = this.convertMessage(message);
		TaskMigrationStatus taskMigrationStatus = taskService.getPartitionMigrationStatus(message.getTaskCode(), message.getFromPartitionKey());
		//获取任务状态是否允许添加，不允许则等待
		if (taskMigrationStatus != null && taskMigrationStatus != TaskMigrationStatus.STOPED) {
			waitAddMap.put(key, task);
		} else {
			taskShardingService.addTask(task);
		}
	}
	public void deleteResource(TaskMessage message) {
		logger.info("delete resource --> {}", message);
		String key = this.generationTaskKey(message);
		TaskShardingConfig task = waitAddMap.remove(key);
		if (task == null) {
			task = this.convertMessage(message);
		}
		taskShardingService.deleteTask(task);
		taskService.tryCompletePartitionMigration(message.getTaskCode(), message.getFromPartitionKey());
	}
	@Override
	public void start() {
		logger.info("resource Service Starting...");
		waitAddThread = new Thread("worker-wait-add-thread") {
			@Override
			public void run() {
				while(!waitAddThreadStop) {
					for (String key : waitAddMap.keySet()) {
						if (waitAddThreadStop) {
							break;
						}
						TaskShardingConfig task = waitAddMap.get(key);
						if (task == null) {
							continue;
						}
						TaskMigrationStatus taskMigrationStatus = taskService.getPartitionMigrationStatus(task.getTaskCode(), task.getFromPartition().getPartitionKey());
						//获取任务状态是否允许添加，不允许则等待
						if (taskMigrationStatus != null && taskMigrationStatus == TaskMigrationStatus.STOPED) {
							task = waitAddMap.remove(key);
							if (task != null) {
								taskShardingService.addTask(task);
							}
						}
					}
					try {
						sleep(100);
					} catch (Exception e) {}
				}
			}
		};
		waitAddThread.start();
	}
	@Override
	public void stop() {
		logger.info("resource service stopping...");
		this.waitAddThreadStop = true;
		if (waitAddThread != null) {
			try {
				waitAddThread.interrupt();
			} catch (Exception e) {}
		}
		waitAddMap.clear();
	}
	
	private TaskShardingConfig convertMessage(TaskMessage message) {
		//去zk相应节点获取任务信息（resourceName等于任务编号）
		TaskEntity taskEntity = taskService.get(message.getTaskCode());
		if (taskEntity == null) {
			throw new BusinessException("No matching abilities found for the task config");
		}
		if (StringUtils.isBlank(taskEntity.getFromClusterCode())) {
			throw new BusinessException("fromClusterCode is Empty for the task config");
		}
		ClusterEntity fromClusterEntity = clusterService.get(taskEntity.getFromClusterCode());
		if (fromClusterEntity == null) {
			throw new BusinessException("No matching abilities found for the from cluster config");
		}
		TransferCluster fromCluster = new TransferCluster();
		BeanUtils.copyProperties(fromClusterEntity, fromCluster);
		if (StringUtils.isBlank(taskEntity.getToClusterCode())) {
			throw new BusinessException("toClusterCode is Empty for the task config");
		}
		ClusterEntity toClusterEntity = clusterService.get(taskEntity.getToClusterCode());
		if (toClusterEntity == null) {
			throw new BusinessException("No matching abilities found for the to cluster config");
		}
		TransferCluster toCluster = new TransferCluster();
		BeanUtils.copyProperties(toClusterEntity, toCluster);
		
		TopicPartitionMapping partitionMapping = taskService.getPartitionMapping(message.getTaskCode(), message.getFromPartitionKey());
		if (partitionMapping == null) {
			throw new BusinessException("Task partition not assigned transfering");
		}
		
		//Ability to find matches
		PluginProviderService fromPluginProviderService = pluginProviderManagerService.getOnePluginProviderService(fromCluster);
		if (fromPluginProviderService == null) {
			throw new BusinessException("No matching abilities found for the source cluster");
		}
		PluginProviderService toPluginProviderService = pluginProviderManagerService.getOnePluginProviderService(toCluster);
		if (toPluginProviderService == null) {
			throw new BusinessException("No matching abilities found for the target cluster");
		}
		TransferConfig transferConfig = taskEntity.getTransferConfig();
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
		TransferTaskPartition transferTaskPartition = transferPartitionConfigMap.get(message.getFromPartitionKey());
		TransferPartitionConfig transferPartitionConfig = new TransferPartitionConfig();
		transferPartitionConfig.setCacheQueueMemoryMaxRatio(transferConfig.getCacheQueueMemoryMaxRatio());
		transferPartitionConfig.setMaxCacheRecords(transferConfig.getMaxCacheRecords());
		transferPartitionConfig.setPartitionMatchStrategy(transferConfig.getPartitionMatchStrategy());
		transferPartitionConfig.setPartitionMatchConfig(transferConfig.getPartitionMatchConfig());
		transferPartitionConfig.setPartition(transferTaskPartition);
		
		TaskShardingConfig task = new TaskShardingConfig();
		task.setTaskCode(taskEntity.getCode());
		task.setFromCluster(fromCluster);
		task.setFromTopic(taskEntity.getFromTopic());
		task.setFromPartition(partitionMapping.getFromPartition());
		task.setToCluster(toCluster);
		task.setToTopic(taskEntity.getToTopic());
		task.setToPartition(partitionMapping.getToPartition());
		task.setConsumerConfig(taskEntity.getFromConfig());
		task.setProducerConfig(taskEntity.getToConfig());
		task.setTransferPartitionConfig(transferPartitionConfig);
		return task;
	}
	
	private String generationTaskKey(TaskMessage message) {
		return message.getTaskCode() + "#" + message.getFromPartitionKey();
	}
}
