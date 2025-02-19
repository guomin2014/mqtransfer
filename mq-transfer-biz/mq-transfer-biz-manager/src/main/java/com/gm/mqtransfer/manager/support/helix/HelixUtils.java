/**
 * Copyright (C) 2015-2016 Uber Technology Inc. (streaming-core@uber.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gm.mqtransfer.manager.support.helix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.helix.HelixAdmin;
import org.apache.helix.HelixDataAccessor;
import org.apache.helix.HelixManager;
import org.apache.helix.PropertyKey;
import org.apache.helix.PropertyPathBuilder;
import org.apache.helix.PropertyType;
import org.apache.helix.manager.zk.ZkBaseDataAccessor;
import org.apache.helix.model.IdealState;
import org.apache.helix.model.builder.CustomModeISBuilder;
import org.apache.helix.store.zk.ZkHelixPropertyStore;
import org.apache.helix.zookeeper.datamodel.ZNRecord;

import com.gm.mqtransfer.facade.common.util.DataUtil;
import com.gm.mqtransfer.facade.model.TaskShardingConfig;
import com.gm.mqtransfer.provider.facade.model.TopicPartitionInfo;
import com.gm.mqtransfer.provider.facade.util.PartitionUtils;
import com.google.common.collect.ImmutableList;


public class HelixUtils {
	
	public static String getAbsoluteZkPathForHelix(String zkBaseUrl) {
		zkBaseUrl = StringUtils.removeEnd(zkBaseUrl, "/");
		return zkBaseUrl;
	}

	public static ZkHelixPropertyStore<ZNRecord> getZkPropertyStore(HelixManager helixManager, String clusterName) {
		ZkBaseDataAccessor<ZNRecord> baseAccessor = (ZkBaseDataAccessor<ZNRecord>) helixManager.getHelixDataAccessor()
				.getBaseDataAccessor();
		String propertyStorePath = PropertyPathBuilder.getPath(PropertyType.PROPERTYSTORE, clusterName);

		ZkHelixPropertyStore<ZNRecord> propertyStore = new ZkHelixPropertyStore<ZNRecord>(baseAccessor,
				propertyStorePath, Arrays.asList(propertyStorePath));

		return propertyStore;
	}

	public static List<String> liveInstances(HelixManager helixManager) {
		HelixDataAccessor helixDataAccessor = helixManager.getHelixDataAccessor();
		PropertyKey liveInstancesKey = helixDataAccessor.keyBuilder().liveInstances();
		return ImmutableList.copyOf(helixDataAccessor.getChildNames(liveInstancesKey));
	}
	
	public static HelixPartition convertStr2Partition(String resourceName, String partition) {
		if (StringUtils.isBlank(partition)) {
			return null;
		}
		TopicPartitionInfo tp = PartitionUtils.parsePartitionKey(partition);
		if (tp == null) {
			return null;
		}
		HelixPartition p = new HelixPartition(resourceName, tp.getBrokerName(), tp.getPartition());
		return p;
	}
	
	public static String convertPartition2Str(HelixPartition partition) {
		if (partition == null) {
			return "";
		}
		return partition.generatorPartitionKey();
	}

	/**
	 * From IdealStates.
	 * 
	 * @param helixManager
	 * @return InstanceToNumTopicPartitionMap
	 */
	public static Map<String, Set<HelixPartition>> getInstanceToPartitionsMap(HelixManager helixManager) {
		Map<String, Set<HelixPartition>> instanceToNumTopicPartitionMap = new HashMap<>();
		HelixAdmin helixAdmin = helixManager.getClusterManagmentTool();
		String helixClusterName = helixManager.getClusterName();
		for (String resourceName : helixAdmin.getResourcesInCluster(helixClusterName)) {
			IdealState is = helixAdmin.getResourceIdealState(helixClusterName, resourceName);
			if (!is.isEnabled()) {//资源已停用
				continue;
			}
			for (String partition : is.getPartitionSet()) {
				HelixPartition p = convertStr2Partition(resourceName, partition);
				if (p == null) {
					continue;
				}
				for (String instance : is.getInstanceSet(partition)) {
					if (!instanceToNumTopicPartitionMap.containsKey(instance)) {
						instanceToNumTopicPartitionMap.put(instance, new HashSet<HelixPartition>());
					}
					instanceToNumTopicPartitionMap.get(instance).add(p);
				}
			}
		}
		return instanceToNumTopicPartitionMap;
	}
	
	public static List<ResourceInfo> getAllResource(HelixManager helixManager) {
		List<ResourceInfo> retList = new ArrayList<>();
		HelixAdmin helixAdmin = helixManager.getClusterManagmentTool();
		String helixClusterName = helixManager.getClusterName();
		for (String resourceName : helixAdmin.getResourcesInCluster(helixClusterName)) {
			IdealState is = helixAdmin.getResourceIdealState(helixClusterName, resourceName);
			Map<HelixPartition, String> partitons = new HashMap<>();
			for (String partition : is.getPartitionSet()) {
				HelixPartition p = convertStr2Partition(resourceName, partition);
				if (p == null) {
					continue;
				}
				Set<String> instances = is.getInstanceSet(partition);//目前使用的是onlineoffline模式，因此每个分区只有一个实例
				if (instances.isEmpty()) {
					partitons.put(p, "");//空串表示未分配实例
				} else {
					partitons.put(p, instances.iterator().next());
				}
			}
			boolean enabled = is.isEnabled();
			String assignInstance = is.getRecord().getSimpleField(IdealStateBuilder.CUSTOM_ASSIGN_INSTANCE_KEY);
			Integer weight = DataUtil.converObj2IntegerWithNull(is.getRecord().getSimpleField(IdealStateBuilder.CUSTOM_PARTITION_WEIGHT_KEY));
			if (weight == null) {
				weight = 1;
			}
			ResourceInfo resource = new ResourceInfo(resourceName, enabled, weight, partitons, assignInstance);
			retList.add(resource);
		}
		return retList;
	}

	public static Map<String, IdealState> getIdealStatesFromAssignment(Set<InstanceResourceHolder> newAssignment) {
		Map<String, CustomModeISBuilder> idealStatesBuilderMap = new HashMap<String, CustomModeISBuilder>();
		Map<String, ResourceInfo> resourceMap = new HashMap<>();
		for (InstanceResourceHolder instance : newAssignment) {
			List<ResourceInfo> resourceList = instance.getResourceList();
			if (resourceList != null && resourceList.size() > 0) {
				for (ResourceInfo resource : resourceList) {
					String resourceName = resource.getResourceName();
					resourceMap.put(resourceName, resource);
					Map<HelixPartition, String> partitionInstanceMap = resource.getPartitionInstanceMap();
					for (Map.Entry<HelixPartition, String> entry : partitionInstanceMap.entrySet()) {
						HelixPartition p = entry.getKey();
						String partition = convertPartition2Str(p);//JSON.toJSONString(p);
						if (!idealStatesBuilderMap.containsKey(resourceName)) {
							final CustomModeISBuilder customModeIdealStateBuilder = new CustomModeISBuilder(resourceName);
							customModeIdealStateBuilder.setStateModel(OnlineOfflineStateModel.STATE_MODEL_NAME).setNumReplica(1);
							idealStatesBuilderMap.put(resourceName, customModeIdealStateBuilder);
						}
						idealStatesBuilderMap.get(resourceName).assignInstanceAndState(partition, instance.getInstanceName(), OnlineOfflineStateModel.States.ONLINE.name());
					}
				}
			}
		}
		Map<String, IdealState> idealStatesMap = new HashMap<String, IdealState>();
		for (String resourceName : idealStatesBuilderMap.keySet()) {
			IdealState idealState = idealStatesBuilderMap.get(resourceName).build();
			ResourceInfo resource = resourceMap.get(resourceName);
			idealState.setMaxPartitionsPerInstance(idealState.getPartitionSet().size());
			idealState.setNumPartitions(idealState.getPartitionSet().size());
			String assignInstance = null;
			if (resource != null) {
				if (!resource.isEnabled()) {//资源被禁用
					idealState.enable(false);
				}
				assignInstance = resource.getAssignInstance();
			}
			idealState.getRecord().setSimpleField(IdealStateBuilder.CUSTOM_ASSIGN_INSTANCE_KEY, assignInstance);
			idealStatesMap.put(resourceName, idealState);
		}
		return idealStatesMap;
	}

	public static Set<HelixPartition> getUnassignedPartitions(HelixManager helixManager) {
		Set<HelixPartition> unassignedPartitions = new HashSet<>();
		HelixAdmin helixAdmin = helixManager.getClusterManagmentTool();
		String helixClusterName = helixManager.getClusterName();
		for (String resourceName : helixAdmin.getResourcesInCluster(helixClusterName)) {
			IdealState is = helixAdmin.getResourceIdealState(helixClusterName, resourceName);
			if (!is.isEnabled()) {//资源已停用
				continue;
			}
			Set<String> partitionSet = is.getPartitionSet();
			for (String partition : partitionSet) {
				if (is.getInstanceSet(partition).isEmpty()) {
					HelixPartition p = convertStr2Partition(resourceName, partition);
					if (p == null) {
						continue;
					}
					unassignedPartitions.add(p);
				}
			}
		}
		return unassignedPartitions;
	}
	
	public static String generateResourceName(TaskShardingConfig task) {
		return task.getTaskCode();// + "##" + task.getFromTopic();
	}
	public static String generateResourceName(HelixPartition partition) {
		return partition.getTaskCode();// + "##" + task.getFromTopic();
	}

	public static String generalInstanceIdForWorker(String host, Integer pid) {
		return host + "_" + (pid != null ? pid.intValue() : "*");
	}
	public static String[] parseInstanceIdForWorker(String instanceId) {
		if (StringUtils.isBlank(instanceId)) {
			return null;
		}
		return instanceId.split("[@_]");
	}
	public static boolean matchInstanceForWorker(String sourceInstance, String targetInstance) {
		if (StringUtils.isBlank(sourceInstance) && StringUtils.isBlank(targetInstance)) {
			return true;
		}
		if (StringUtils.isBlank(sourceInstance) || StringUtils.isBlank(targetInstance)) {
			return false;
		}
		String[] sourceNames = parseInstanceIdForWorker(sourceInstance);
		String[] targetNames = parseInstanceIdForWorker(targetInstance);
		if (sourceNames.length != targetNames.length || sourceNames.length != 2) {
			return false;
		}
		return sourceNames[0].equals(targetNames[0]) && (targetNames[1].equals("*") || targetNames[1].equals(sourceNames[1]));
	}
	
}
