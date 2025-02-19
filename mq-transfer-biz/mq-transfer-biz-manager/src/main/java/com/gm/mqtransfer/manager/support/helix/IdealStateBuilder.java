package com.gm.mqtransfer.manager.support.helix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

import org.apache.helix.model.IdealState;
import org.apache.helix.model.builder.CustomModeISBuilder;

import com.gm.mqtransfer.facade.model.TaskShardingConfig;
import com.gm.mqtransfer.facade.model.TransferPartitionConfig;
import com.gm.mqtransfer.provider.facade.model.TopicPartitionInfo;
import com.gm.mqtransfer.provider.facade.util.StringUtils;

public class IdealStateBuilder {
	
	public static final String CUSTOM_ASSIGN_INSTANCE_KEY = "CUSTOM_ASSIGN_INSTANCE";
	public static final String CUSTOM_PARTITION_WEIGHT_KEY = "CUSTOM_PARTITION_WEIGHT";

	/**
	 * 添加资源
	 * @param task
	 * @param oldIdealState
	 * @param instanceQueue
	 * @param enable
	 * @return
	 */
	public static IdealState buildCustomIdealStateForAdd(TaskShardingConfig task, IdealState oldIdealState, 
			PriorityQueue<InstanceResourceHolder> instanceQueue, Boolean enable) {
		List<HelixPartition> partitionList = buildPartitions(task);
		return buildCustomIdealStateFor(task, oldIdealState, partitionList, instanceQueue, enable);
	}
	/**
	 * 移除资源
	 * @param task
	 * @param oldIdealState
	 * @param instanceQueue
	 * @param enable
	 * @return
	 */
	public static IdealState buildCustomIdealStateForRemove(TaskShardingConfig task, IdealState oldIdealState, PriorityQueue<InstanceResourceHolder> instanceQueue) {
		if (oldIdealState == null) {
			return null;
		}
		TopicPartitionInfo fromPartition = task.getFromPartition();
		HelixPartition partition = new HelixPartition(task.getTaskCode(), fromPartition.getBrokerName(), fromPartition.getPartition());
		Map<String, Map<String, String>> mapFields = oldIdealState.getRecord().getMapFields();
		String partitionName = task.getFromPartition().getPartitionKey();
		mapFields.remove(partitionName);
//		if (mapFields.containsKey(partitionName)) {
//			Map<String, String> map = mapFields.get(partitionName);
//			for (String key : map.keySet()) {
//				map.put(key, OnlineOfflineStateModel.States.OFFLINE.name());
//			}
//			mapFields.put(partitionName, map);
//		}
		oldIdealState.getRecord().setMapFields(mapFields);
		for (InstanceResourceHolder holder : instanceQueue) {
			holder.removePartition(task.getTaskCode(), partition);
		}
		return oldIdealState;
	}
	
	public static IdealState expandCustomRebalanceModeIdealStateFor(TaskShardingConfig task, IdealState oldIdealState,
			PriorityQueue<InstanceResourceHolder> instanceQueue) {
		List<HelixPartition> partitionList = buildPartitions(task);
		return buildCustomIdealStateFor(task, oldIdealState, partitionList, instanceQueue, null);
	}
	/**
	 * 构建分区
	 * @param clusterType
	 * @param data
	 * @return
	 */
	private static List<HelixPartition> buildPartitions(TaskShardingConfig task) {
		List<HelixPartition> partitionList = new ArrayList<>();
		TopicPartitionInfo fromPartition = task.getFromPartition();
		partitionList.add(new HelixPartition(task.getTaskCode(), fromPartition.getBrokerName(), fromPartition.getPartition()));
		return partitionList;
	}
	
	private static IdealState buildCustomIdealStateFor(TaskShardingConfig task, IdealState oldIdealState, List<HelixPartition> partitionList, 
			PriorityQueue<InstanceResourceHolder> instanceQueue, Boolean enable) {
		String taskCode = task.getTaskCode();
		String resourceName = HelixUtils.generateResourceName(task);
		Integer weight = task.getTransferPartitionConfig() != null ? task.getTransferPartitionConfig().getPartitionWeight() : null;
		if (weight == null) {
			weight = 1;
		}
		String assignExecuteHost = null;//task.getAssignExecuteHost();
		Integer assignExecutePid = null;//task.getAssignExecutePid();
		String assignInstance = null;
		if (StringUtils.isNotBlank(assignExecuteHost)) {
			assignInstance = assignExecuteHost + "_" + (assignExecutePid != null ? assignExecutePid.intValue() : "*");
		}
		return buildCustomIdealState(resourceName, taskCode, oldIdealState, partitionList, weight, assignInstance, instanceQueue, enable);
	}
	
	private static IdealState buildCustomIdealState(String resourceName, String taskCode, IdealState oldIdealState, List<HelixPartition> partitionList, Integer weightPrePartition,
			String assignInstance, PriorityQueue<InstanceResourceHolder> instanceQueue, Boolean enable) {
		int partitionNum = partitionList != null ? partitionList.size() : 0;
		IdealState idealState = null;
		if (oldIdealState == null) {
			CustomModeISBuilder customModeIdealStateBuilder = new CustomModeISBuilder(resourceName);
			customModeIdealStateBuilder.setStateModel(OnlineOfflineStateModel.STATE_MODEL_NAME).setNumPartitions(partitionNum)
					.setNumReplica(1).setMaxPartitionsPerNode(partitionNum);
			idealState = customModeIdealStateBuilder.build();
		} else {
			idealState = oldIdealState;
		}
		//分区缩容
		InstanceResourceHolder liveInstance = null;
		//获取已经分配的分区集合
		Map<String, Map<String, String>> oldPartitionMap = new HashMap<>();
		if (oldIdealState != null) {
			Set<String> instanceNames = new HashSet<>();
			Set<String> oldPartitions = oldIdealState.getPartitionSet();
			for (String partition : oldPartitions) {
				Map<String, String> instanceStateMap = oldIdealState.getInstanceStateMap(partition);
				oldPartitionMap.put(partition, instanceStateMap);
				instanceNames.addAll(instanceStateMap.keySet());
			}
			//查找已分配分区的实例，将所有未分配的分区都分配到该实例上
			//已经分配实例的资源分配信息保持不变（避免资源迁移发生）
			for (InstanceResourceHolder holder : instanceQueue) {
				if (instanceNames.contains(holder.getInstanceName())) {
					liveInstance = holder;
					break;
				}
			}
			if (liveInstance != null) {//先从优先级队列中移除，后面在加入（为了排序）
				instanceQueue.remove(liveInstance);
			}
		}
		if (StringUtils.isNotBlank(assignInstance)) {//指定了执行实例
			//判断当前分配的实例与指定实例是否一致
			if (liveInstance != null) {//分配了实例
				String oldInstanceName = liveInstance.getInstanceName();
				boolean match = HelixUtils.matchInstanceForWorker(oldInstanceName, assignInstance);
				if (!match) {//需要从原分配中移除，并添加到新实例中
					liveInstance.removeResource(resourceName);
					instanceQueue.add(liveInstance);//重新加入队列
					liveInstance = null;
					//查找新的匹配实例
					for (InstanceResourceHolder holder : instanceQueue) {
						if (HelixUtils.matchInstanceForWorker(holder.getInstanceName(), assignInstance)) {
							liveInstance = holder;
							instanceQueue.remove(liveInstance);
							break;
						}
					}
				}
			} else {//未分配实例，按指定实例获取
				for (InstanceResourceHolder holder : instanceQueue) {
					if (HelixUtils.matchInstanceForWorker(holder.getInstanceName(), assignInstance)) {
						liveInstance = holder;
						instanceQueue.remove(liveInstance);
						break;
					}
				}
			}
		} else {//未指定实例
			if (liveInstance == null) {//也不存在已分配实例，则直接从队列中获取一个权重最小的实例
				liveInstance = instanceQueue.poll();
			}
		}
		if (partitionNum > 0) {
			Map<HelixPartition, String> partitionInstanceMap = new HashMap<>();
			for (HelixPartition partition : partitionList) {
				//partitionName在IdealState中作为Map的key进行存储，具有唯一性，对于任务资源中非固定部份则不能放入该字段中，否则在字段值修改以后造成partitionName不一致，导致识别成不同的任务资源
				// partitionName = ${queueId}@${brokerName}  
				String partitionName = HelixUtils.convertPartition2Str(partition);//JSON.toJSONString(partition);
				Map<String, String> partitionToInstanceStateMapping = idealState.getRecord().getMapField(partitionName);
				if (partitionToInstanceStateMapping == null) {
					partitionToInstanceStateMapping = new TreeMap<String, String>();
					idealState.getRecord().getMapFields().put(partitionName, partitionToInstanceStateMapping);
				}
				if (liveInstance != null) {
					partitionToInstanceStateMapping.put(liveInstance.getInstanceName(), OnlineOfflineStateModel.States.ONLINE.name());
					partitionInstanceMap.put(partition, liveInstance.getInstanceName());
				}
			}
			if (liveInstance != null) {
				ResourceInfo resource = new ResourceInfo(resourceName, enable, weightPrePartition, partitionInstanceMap);
				liveInstance.addResource(resource);
			}
		}
		if (liveInstance != null) {
			instanceQueue.add(liveInstance);//重新加入队列并排序
		}
		idealState.getRecord().setSimpleField(CUSTOM_ASSIGN_INSTANCE_KEY, assignInstance);//设置指定执行实例
		idealState.getRecord().setSimpleField(CUSTOM_PARTITION_WEIGHT_KEY, weightPrePartition != null ? weightPrePartition.toString() : "1");//设置分区权重
		if (enable != null) {//指定了启用状态，则使用指定，否则沿用历史状态
			idealState.enable(enable);
		} else {
			if (oldIdealState != null && !oldIdealState.isEnabled()) {
				idealState.enable(false);
			}
		}
		return idealState;
	}
	
	public static IdealState buildCustomIdealStateForAdd(TaskShardingConfig task, IdealState oldIdealState, InstanceResourceHolder instanceHolder) {
		int partitionNum = 1;
		String resourceName = HelixUtils.generateResourceName(task);
		String oldAssignInstanceName = null;
		IdealState idealState = null;
		if (oldIdealState == null) {
			CustomModeISBuilder customModeIdealStateBuilder = new CustomModeISBuilder(resourceName);
			customModeIdealStateBuilder.setStateModel(OnlineOfflineStateModel.STATE_MODEL_NAME).setNumPartitions(partitionNum)
					.setNumReplica(1).setMaxPartitionsPerNode(partitionNum);
			idealState = customModeIdealStateBuilder.build();
			TransferPartitionConfig transferPartitionConfig = task.getTransferPartitionConfig();
			Integer weightPrePartition = null;
			String assignInstance = null;
			if (transferPartitionConfig != null) {
				assignInstance = transferPartitionConfig.getAssignInstance();
				weightPrePartition = transferPartitionConfig.getPartitionWeight();
			}
			idealState.getRecord().setSimpleField(CUSTOM_ASSIGN_INSTANCE_KEY, assignInstance);//设置指定执行实例
			idealState.getRecord().setSimpleField(CUSTOM_PARTITION_WEIGHT_KEY, weightPrePartition != null ? weightPrePartition.toString() : "1");//设置分区权重
		} else {
			idealState = oldIdealState;
			idealState.setNumPartitions(oldIdealState.getNumPartitions() + 1);
			Set<String> instanceNames = new HashSet<>();
			Set<String> oldPartitions = oldIdealState.getPartitionSet();
			for (String partition : oldPartitions) {
				Map<String, String> instanceStateMap = oldIdealState.getInstanceStateMap(partition);
				instanceNames.addAll(instanceStateMap.keySet());
			}
			if (!instanceNames.isEmpty()) {
				oldAssignInstanceName = instanceNames.iterator().next();
			}
		}
		String assignInstanceName = null;
		if (StringUtils.isNotBlank(oldAssignInstanceName)) {
			assignInstanceName = oldAssignInstanceName;
		} else if (instanceHolder != null) {
			assignInstanceName = instanceHolder.getInstanceName();
		}
		HelixPartition partition = new HelixPartition(task.getTaskCode(), task.getFromPartition().getBrokerName(), task.getFromPartition().getPartition());
		String partitionName = HelixUtils.convertPartition2Str(partition);
		Map<String, String> partitionToInstanceStateMapping = new TreeMap<String, String>();
		if (StringUtils.isNotBlank(assignInstanceName)) {
			partitionToInstanceStateMapping.put(assignInstanceName, OnlineOfflineStateModel.States.ONLINE.name());
		}
		idealState.getRecord().getMapFields().put(partitionName, partitionToInstanceStateMapping);
		idealState.enable(true);
		return idealState;
	}
}
