package com.gm.mqtransfer.manager.support.helix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.apache.helix.ClusterMessagingService;
import org.apache.helix.Criteria;
import org.apache.helix.HelixAdmin;
import org.apache.helix.HelixDataAccessor;
import org.apache.helix.HelixManager;
import org.apache.helix.InstanceType;
import org.apache.helix.PropertyKey;
import org.apache.helix.PropertyKey.Builder;
import org.apache.helix.messaging.AsyncCallback;
import org.apache.helix.model.HelixConfigScope;
import org.apache.helix.model.HelixConfigScope.ConfigScopeProperty;
import org.apache.helix.model.IdealState;
import org.apache.helix.model.LiveInstance;
import org.apache.helix.model.Message;
import org.apache.helix.model.Message.MessageState;
import org.apache.helix.model.Message.MessageType;
import org.apache.helix.model.builder.HelixConfigScopeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gm.mqtransfer.facade.common.TaskMessageType;
import com.gm.mqtransfer.facade.exception.BusinessException;
import com.gm.mqtransfer.manager.config.ClusterConfig;


public class HelixBalanceManager {

	private final Logger logger = LoggerFactory.getLogger(HelixBalanceManager.class);
	private static final String ENABLE = "enable";
	private static final String DISABLE = "disable";
	private static final String AUTO_BALANCING = "AutoBalancing";
	/** 分区未分配实例的名称 */
	private final String UNASSIGNED_INSTANCE_NAME = "-1";
	private final String EMPYT_INSTANCE_NAME = "";
	private ScheduledExecutorService delayedScheuler;
	private HelixManager helixManager;
	private ClusterConfig config;
	private String clusterName;
	private String instanceName;
	private boolean rebalanceRunning = false;
	/** 任务锁 */
	private final ReentrantLock rebalanceLock = new ReentrantLock();
	
	private volatile AtomicInteger rebalanceRunningCount = new AtomicInteger(0);
	
	/** 存活worker实例队列 */
	private PriorityQueue<InstanceResourceHolder> workerInstanceQueue;
	
	public HelixBalanceManager(ClusterConfig config, HelixManager helixManager) {
		this.config = config;
		this.helixManager = helixManager;
		this.clusterName = helixManager.getClusterName();
		this.instanceName = helixManager.getInstanceName();
		String rebalanceStrategyName = config.getAutoRebalanceStrategy();
		RebalanceStrategyType rebalanceStrategy = RebalanceStrategyType.getByName(rebalanceStrategyName);
		if (rebalanceStrategy == null) {
			rebalanceStrategy = RebalanceStrategyType.AssignPriority;
		}
		this.workerInstanceQueue = new PriorityQueue<>(1, InstanceResourceHolder.getComparator(rebalanceStrategy));
	}
	
	public void start() {
		logger.info("start cluster balance manager [{}] [{}]", clusterName, instanceName);
		delayedScheuler = Executors.newSingleThreadScheduledExecutor();
	}
	
	public void stop() {
		logger.info("stop cluster balance manager [{}] [{}]", clusterName, instanceName);
		delayedScheuler.shutdown();
	}
	
	public InstanceResourceHolder getMaxIdleInstance() {
		return this.workerInstanceQueue.peek();
	}
	
	public InstanceResourceHolder getInstanceHolderByName(String instanceName) {
		return null;
	}
	
	public void rebalance() {
		int delayedAutoReblanceTimeInSeconds = this.config.getAutoRebalanceDelayInSeconds();
		boolean autoRebalanceEnable = this.config.isAutoRebalanceEnable();
		if (!autoRebalanceEnable) {
			logger.info("[{}] [{}] automatic rebalancing is not enabled, skip it.", clusterName, instanceName);
			return;
		}
		if (rebalanceRunningCount.incrementAndGet() > 1) {
			logger.info("[{}] [{}] automatic rebalancing is exists, skip it.", clusterName, instanceName);
			rebalanceRunningCount.decrementAndGet();
			return;
		}
		logger.info("[{}] [{}] automatic rebalancing will start in {} seconds", clusterName, instanceName, delayedAutoReblanceTimeInSeconds);
		delayedScheuler.schedule(new Runnable() {
			@Override
			public void run() {
				try {
					rebalanceLock.lock();
					rebalanceCurrentCluster(); // 实例数变更时;
				} catch (Exception e) {
					logger.error("[" + clusterName + "] Got exception during rebalance the whole cluster! ", e);
				} finally {
					rebalanceRunningCount.decrementAndGet();
					rebalanceLock.unlock();
				}
			}
		}, delayedAutoReblanceTimeInSeconds, TimeUnit.SECONDS);
	}
	
	public void rebalance(List<LiveInstance> liveInstances) {
		this.updateCurrentServingInstance(liveInstances);
		this.rebalance();
	}
	
	public void updateCurrentServingInstance() {
		logger.info("[{}] Trying to update serving instance to cache", clusterName);
		this.workerInstanceQueue = this.getInstanceResourceQueue();
	}
	
	public void updateCurrentServingInstance(List<LiveInstance> liveInstances) {
		logger.info("[{}] Trying to update current server instance and resource queue!", clusterName);
		Set<String> liveInstanceNames = this.getLiveInstanceName(liveInstances);
		Set<String> existsInstanceName = new HashSet<>();
		Set<InstanceResourceHolder> noliveInstanceHolders = new HashSet<>();
		for (InstanceResourceHolder holder : workerInstanceQueue) {
			existsInstanceName.add(holder.getInstanceName());
			if (!liveInstanceNames.contains(holder.getInstanceName())) {
				noliveInstanceHolders.add(holder);
			}
		}
		//获取存活但未分配资源的实例
		for (LiveInstance liveInstance : liveInstances) {
			String liveInstanceName = liveInstance.getInstanceName();
			if (!existsInstanceName.contains(liveInstanceName)) {
				workerInstanceQueue.add(new InstanceResourceHolder(liveInstanceName));
			}
		}
		for (InstanceResourceHolder nolive : noliveInstanceHolders) {
			workerInstanceQueue.remove(nolive);
		}
	}
	
	/**
	 * 获取集群的实例列表
	 * @param helixManager
	 * @return
	 */
	private PriorityQueue<InstanceResourceHolder> getInstanceResourceQueue() {
		List<ResourceInfo> resourceList = HelixUtils.getAllResource(helixManager);
		//计算各资源的权重积分和
		Map<String, InstanceResourceHolder> instanceResourceMap = new HashMap<>();
		for (ResourceInfo resource : resourceList) {
			Map<HelixPartition, String> partitionInstanceMap = resource.getPartitionInstanceMap();
			for (Map.Entry<HelixPartition, String> entry : partitionInstanceMap.entrySet()) {
				HelixPartition partition = entry.getKey();
				String instance = entry.getValue();
				if (StringUtils.isBlank(instance)) {
					continue;
				}
				if (StringUtils.isNotBlank(instance)) {
					if (instanceResourceMap.containsKey(instance)) {
						instanceResourceMap.get(instance).addPartition(resource.getResourceName(), partition, resource.getWeight(), resource.getAssignInstance());
					} else {
						InstanceResourceHolder holder = new InstanceResourceHolder(instance);
						holder.addPartition(resource.getResourceName(), partition, resource.getWeight(), resource.getAssignInstance());
						instanceResourceMap.put(instance, holder);
					}
				}
			}
		}
		//获取存活但未分配资源的实例
		List<LiveInstance> liveInstances = this.getCurrentLiveInstances();
		for (LiveInstance liveInstance : liveInstances) {
			String liveInstanceName = liveInstance.getInstanceName();
			if (!instanceResourceMap.containsKey(liveInstanceName)) {
				instanceResourceMap.put(liveInstanceName, new InstanceResourceHolder(liveInstanceName));
			}
		}
		
		String rebalanceStrategyName = this.config.getAutoRebalanceStrategy();
		RebalanceStrategyType rebalanceStrategy = RebalanceStrategyType.getByName(rebalanceStrategyName);
		if (rebalanceStrategy == null) {
			rebalanceStrategy = RebalanceStrategyType.AssignPriority;
		}
		PriorityQueue<InstanceResourceHolder> servingInstance = new PriorityQueue<>(1, InstanceResourceHolder.getComparator(rebalanceStrategy));
		//添加已分配分区的实例
		servingInstance.addAll(instanceResourceMap.values());
		return servingInstance;
	}
	
	public List<LiveInstance> getCurrentLiveInstances() {
		if (helixManager == null) {
			throw new BusinessException("can not find cluster by tag:" + clusterName);
		}
		HelixDataAccessor helixDataAccessor = helixManager.getHelixDataAccessor();
		PropertyKey liveInstancePropertyKey = new Builder(clusterName).liveInstances();
		List<LiveInstance> liveInstances = helixDataAccessor.getChildValues(liveInstancePropertyKey, false);
		return liveInstances;
	}
	
	public boolean isAutoBalancingEnabled(String clusterName) {
		if (helixManager == null) {
			throw new BusinessException("can not find cluster by tag:" + clusterName);
		}
		HelixConfigScope scope = new HelixConfigScopeBuilder(ConfigScopeProperty.CLUSTER).forCluster(clusterName).build();
		HelixAdmin helixAdmin = helixManager.getClusterManagmentTool();
		try {
			Map<String, String> config = helixAdmin.getConfig(scope, Arrays.asList(AUTO_BALANCING));
			if (config.containsKey(AUTO_BALANCING) && config.get(AUTO_BALANCING).equals(DISABLE)) {
				return false;
			}
			return true;
		} finally {
			if (helixAdmin != null) {
				helixAdmin.close();
			}
		}
	}
	
	public void disableAutoBalancing() {
		if (helixManager == null) {
			throw new BusinessException("can not find cluster by tag:" + clusterName);
		}
		HelixConfigScope scope = new HelixConfigScopeBuilder(ConfigScopeProperty.CLUSTER).forCluster(clusterName).build();
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(AUTO_BALANCING, DISABLE);
		HelixAdmin helixAdmin = helixManager.getClusterManagmentTool();
		try {
			helixAdmin.setConfig(scope, properties);
		} finally {
			if (helixAdmin != null) {
				helixAdmin.close();
			}
		}
	}

	public void enableAutoBalancing() {
		if (helixManager == null) {
			throw new BusinessException("can not find cluster by tag:" + clusterName);
		}
		HelixConfigScope scope = new HelixConfigScopeBuilder(ConfigScopeProperty.CLUSTER).forCluster(clusterName).build();
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(AUTO_BALANCING, ENABLE);
		HelixAdmin helixAdmin = helixManager.getClusterManagmentTool();
		try {
			helixAdmin.setConfig(scope, properties);
		} finally {
			if (helixAdmin != null) {
				helixAdmin.close();
			}
		}
	}
	
	public boolean isRebalanceRunning() {
		return rebalanceRunning;
	}

	public boolean tryLockRefreshClusterResource() {
		rebalanceLock.lock();
		return true;
	}
	
	public void releaseRefreshClusterResource() {
		rebalanceLock.unlock();
	}

	private void rebalanceCurrentCluster() {
		logger.info("[{}] [{}] automatic rebalancing starts...", clusterName, instanceName);
		long startTime = System.currentTimeMillis();
		try {
			rebalanceRunning = true;
			boolean autoRebalanceEnable = this.config.isAutoRebalanceEnable();
			if (!autoRebalanceEnable) {
				logger.info("[{}] [{}] automatic rebalancing is not enabled, skip it.", clusterName, instanceName);
				return;
			}
			if (!helixManager.isLeader()) {
				logger.info("[{}] [{}] The current instance is not a leader, do nothing!", clusterName, instanceName);
				return;
			}
			if (!this.isAutoBalancingEnabled(clusterName)) {
				logger.info("[{}] [{}] The current instance is leader, but auto-balancing is disabled, do nothing!", clusterName, instanceName);
				return;
			}
			logger.info("[{}] Trying to fetch live instances from cluster!", clusterName);
			List<LiveInstance> liveInstances = this.getCurrentLiveInstances();
			if (liveInstances.isEmpty()) {
				logger.info("[{}] No live instances, do nothing!", clusterName);
				return;
			}
			logger.info("[{}] Trying to fetch resources from cluster!", clusterName);
			//获取集群内的所有资源
			List<ResourceInfo> resourceList = HelixUtils.getAllResource(helixManager);
			if (resourceList.isEmpty()) {
				logger.info("[{}] No resource got assigned yet, do nothing!", clusterName);
				return;
			}
			logger.info("[{}] Trying to reassign resources!", clusterName);
			//重新分配资源
			Set<InstanceResourceHolder> balanceAssignment = reassignResourceToInstance(liveInstances, resourceList);
			if (balanceAssignment == null || balanceAssignment.isEmpty()) {
				logger.info("[{}] No resource reassign, do nothing!", clusterName);
				return;
			}
			logger.info("[{}] Trying to fetch change instance from current assignment!", clusterName);
			Set<InstanceResourceHolder> changeInstance = getChangeInstance(resourceList, balanceAssignment);
			if (changeInstance == null || changeInstance.isEmpty()) {
				logger.info("[{}] No Live Instances got changed, do nothing!", clusterName);
				return;
			}
			logger.info("[{}] Trying to fetch IdealStates from current assignment!", clusterName);
			Map<String, IdealState> idealStatesFromAssignment = HelixUtils.getIdealStatesFromAssignment(changeInstance);
			logger.info("[{}] Trying to assign new IdealStates!", clusterName);
			assignIdealStates(idealStatesFromAssignment);
		} catch (Exception e) {
//			AlarmUtils.alarm("[" + clusterName + "] [" + instanceName + "] Got exception during rebalance the whole cluster! ");
			logger.error("[" + clusterName + "] [" + instanceName + "] Got exception during rebalance the whole cluster! ", e);
		} finally {
			logger.info("[{}] Trying to update current server instance and resource queue!", clusterName);
			this.updateCurrentServingInstance();
			logger.info("[{}] [{}] automatic rebalancing completed, time: {}", clusterName, instanceName, System.currentTimeMillis() - startTime);
			rebalanceRunning = false;
		}
	}
	
	private Set<InstanceResourceHolder> reassignResourceToInstance(List<LiveInstance> liveInstances, List<ResourceInfo> resourceList) {
		Set<String> liveInstanceNames = getLiveInstanceName(liveInstances);//当前存在的实例
		logger.info("[{}] Live Instances, {}", clusterName, liveInstanceNames);
		//获取已分配分区的实例
		Set<String> instanceNameToPartitions = new HashSet<>();
		int unassignedPartitionCount = 0;
		Map<String, Map<ResourceInfo, List<HelixPartition>>> instancePartitionMap = this.groupByInstance(resourceList);
		//打印群集中资源分配情况
		for (Map.Entry<String, Map<ResourceInfo, List<HelixPartition>>> entry : instancePartitionMap.entrySet()) {
			String instanceName = entry.getKey();
			Map<ResourceInfo, List<HelixPartition>> partitionMap = entry.getValue();
			if (StringUtils.isBlank(instanceName)) {
				unassignedPartitionCount = partitionMap.size();
				instanceName = "未分配";
			} else {
				instanceNameToPartitions.add(instanceName);
			}
			int totalResourceCount = partitionMap.size();
			int enableResourceCount = 0;
			int totalPartitionCount = 0;
			int enablePartitionCount = 0;
			int weight = 0;
			Set<String> resourceNames = new HashSet<>();
			for (Map.Entry<ResourceInfo, List<HelixPartition>> pentry : partitionMap.entrySet()) {
				ResourceInfo resource = pentry.getKey();
				List<HelixPartition> list = pentry.getValue();
				totalPartitionCount += list.size();
				if (resource.isEnabled()) {
					enableResourceCount++;
					enablePartitionCount += list.size();
					weight += resource.getTotalWeight();
				}
				resourceNames.add(resource.getResourceName());
			}
			logger.info("[{}] resource assigned, Instance[{}] ResourceNum[{}][enable:{}] PartitionNum[{}][enable:{}] Weight[{}]-->{}", 
					clusterName, instanceName, totalResourceCount, enableResourceCount, 
					totalPartitionCount, enablePartitionCount, weight, resourceNames);
		}
		Set<String> newInstances = getAddedInstanceSet(liveInstanceNames, instanceNameToPartitions);
		Set<String> removedInstances = getRemovedInstanceSet(liveInstanceNames, instanceNameToPartitions);
		if (!newInstances.isEmpty() || !removedInstances.isEmpty() || unassignedPartitionCount > 0) {
			StringBuilder build = new StringBuilder();
			if (!newInstances.isEmpty()) {
				build.append(" new instances").append(newInstances.toString());
			}
			if (!removedInstances.isEmpty()) {
				build.append(" removed instances").append(removedInstances.toString());
			}
			if (unassignedPartitionCount > 0) {
				build.append("number of unassigned partitions[").append(unassignedPartitionCount).append("]");
			}
			logger.info("[{}] Trying to reassign resource with {}", clusterName, build.toString());
		}
		//业务规则：将topic的所有分区任务都分配到同一个实例上
		//故：先将未分配的分区任务过滤出已经存在分配实例的，并分配，再将其它未分配的分区任务按均衡策略分配到实例上，最后根据实例上的分配情况进行最终均衡
		Map<String, InstanceResourceHolder> instanceResourceMap = new HashMap<>();
		for (ResourceInfo resource : resourceList) {
			Map<HelixPartition, String> partitionInstanceMap = resource.getPartitionInstanceMap();
			Map<String, Integer> instanceMap = new HashMap<>();
			Set<HelixPartition> offlinePartitions = new HashSet<>();
			//统计已分配实例的分区数，并过滤已分配但已下线的分区
			for (Map.Entry<HelixPartition, String> entry : partitionInstanceMap.entrySet()) {
				String instance = entry.getValue();
				if (StringUtils.isBlank(instance)) {//未分配实例
					continue;
				} else if (removedInstances.contains(instance)){//已下线的实例
					offlinePartitions.add(entry.getKey());
				} else {//已分配实例
					Integer count = instanceMap.get(instance);
					if (count == null) {
						count = 0;
					}
					instanceMap.put(instance, count + 1);
				}
			}
			if (offlinePartitions.size() > 0) {
				//将下线实例对应分配分区的实例信息清除
				for (HelixPartition p : offlinePartitions) {
					partitionInstanceMap.put(p, UNASSIGNED_INSTANCE_NAME);
				}
			}
			//如果存在资源的分区在被分配到多个实例上，则重新将所有分区都分配给当前已分配分区最多的实例
			String originalInstanceName = null;
			if (instanceMap.isEmpty()) {//分区都未分配或实例已下线，则先随机获取一个在线实例
				if (newInstances.size() > 0) {
					originalInstanceName = newInstances.iterator().next();
				} else {
					originalInstanceName = liveInstanceNames.iterator().next();
				}
			} else if (instanceMap.size() == 1) {
				originalInstanceName = instanceMap.keySet().iterator().next();
			} else {//获取分区数最多的实例
				int currCount = 0;
				for (Map.Entry<String, Integer> entry : instanceMap.entrySet()) {
					if (entry.getValue() > currCount) {
						originalInstanceName = entry.getKey();
						currCount = entry.getValue();
					}
				}
			}
			if (StringUtils.isBlank(originalInstanceName)) {
				if (newInstances.size() > 0) {
					originalInstanceName = newInstances.iterator().next();
				} else {
					originalInstanceName = liveInstanceNames.iterator().next();
				}
			}
			String assignInstance = resource.getAssignInstance();
			if (StringUtils.isNotBlank(assignInstance)) {//指定了执行主机
				originalInstanceName = null;//清除已分配实例
				for (String liveInstance : liveInstanceNames) {
//					if (HelixUtils.matchInstanceForWorker(liveInstance, assignInstance)) {
//						originalInstanceName = liveInstance;
//						break;
//					}
				}
			}
			if (StringUtils.isBlank(originalInstanceName)) {//未找到符合条件的执行主机
				//TODO 需要清除该资源的已分配情况
				continue;
			}
			if (instanceResourceMap.containsKey(originalInstanceName)) {
				instanceResourceMap.get(originalInstanceName).addResource(resource);
			} else {
				InstanceResourceHolder holder = new InstanceResourceHolder(originalInstanceName);
				holder.addResource(resource);
				instanceResourceMap.put(originalInstanceName, holder);
			}
		}
		String rebalanceStrategyName = this.config.getAutoRebalanceStrategy();
		RebalanceStrategyType rebalanceStrategy = RebalanceStrategyType.getByName(rebalanceStrategyName);
		if (rebalanceStrategy == null) {
			rebalanceStrategy = RebalanceStrategyType.AssignPriority;
		}
		TreeSet<InstanceResourceHolder> instanceResourceOrderedSet = new TreeSet<>(InstanceResourceHolder.getComparator(rebalanceStrategy));
		//添加已分配分区的实例
		instanceResourceOrderedSet.addAll(instanceResourceMap.values());
		//添加未分配分区的实例
		for (String instance : newInstances) {
			if (!instanceResourceMap.containsKey(instance)) {
				InstanceResourceHolder holder = new InstanceResourceHolder(instance);
				instanceResourceOrderedSet.add(holder);
			}
		}
		//做均衡
		Set<InstanceResourceHolder> balanceAssignedResource = balanceAssignment(instanceResourceOrderedSet, rebalanceStrategy);
		return balanceAssignedResource;
	}
	/**
	 * 获取资源存在变更的实例
	 * @param balanceAssignment
	 * @return
	 */
	private Set<InstanceResourceHolder> getChangeInstance(List<ResourceInfo> resourceList, Set<InstanceResourceHolder> balanceAssignment) {
		Set<InstanceResourceHolder> changeInstanceList = new HashSet<>();
		//获取原始分配情况
		Map<String, Map<ResourceInfo, List<HelixPartition>>> oldInstancePartitionMap = this.groupByInstance(resourceList);
		Map<String, Map<String, Set<HelixPartition>>> instanceMigrationResourceMap = new HashMap<>();
		//获取有变更的实例列表
		for (InstanceResourceHolder holder : balanceAssignment) {
			//打印重新分配的情况
			String instanceName = holder.getInstanceName();
			List<ResourceInfo> rlist = holder.getResourceList();
			int totalWeight = holder.getResourceWeight();
			int totalResourceCount = holder.getResourceNum();
			int enableResourceCount = 0;
			int totalPartitionCount = holder.getPartitionNum();
			int enablePartitionCount = 0;
			Set<String> resourceNames = new HashSet<>();
			if (rlist != null && rlist.size() > 0) {
				for (ResourceInfo resource : rlist) {
					resourceNames.add(resource.getResourceName());
					if (resource.isEnabled()) {
						enableResourceCount++;
						enablePartitionCount += resource.getPartitionInstanceMap().size();
					}
				}
			}
			logger.info("[{}] resource reassigned, Instance[{}] ResourceNum[{}][enableNum:{}] PartitionNum[{}][enableNum:{}] Weight[{}]-->{}", 
					clusterName, instanceName, totalResourceCount, enableResourceCount, 
					totalPartitionCount, enablePartitionCount, totalWeight, resourceNames);
			//实例原始分配资源
			Map<ResourceInfo, List<HelixPartition>> oldResourcePartitionMap = oldInstancePartitionMap.get(instanceName);
			if (oldResourcePartitionMap == null) {
				oldResourcePartitionMap = new HashMap<>();
			}
			//判断是否有变更的情况
			boolean hasChange = false;
			if (rlist != null && rlist.size() > 0) {
				for (ResourceInfo resource : rlist) {
					String resourceName = resource.getResourceName();
					boolean enable = resource.isEnabled();
					int weight = resource.getTotalWeight();
					Map<HelixPartition, String> partitionMap = resource.getPartitionInstanceMap();
					int partitionNum = partitionMap.size();
					oldResourcePartitionMap.remove(resource);//从历史分配记录中移除
					int newAssignCount = 0;
					int moveAssignCount = 0;
					Set<String> fromInstances = new HashSet<>();
					boolean hasChangePartition = false;
					for (Map.Entry<HelixPartition, String> entry : partitionMap.entrySet()) {
						HelixPartition partition = entry.getKey();
						String originalInstanceName = entry.getValue();
						if (StringUtils.isBlank(originalInstanceName)//未分配实例
								|| originalInstanceName.equals(UNASSIGNED_INSTANCE_NAME)) {//原实例已下线
							logger.debug("[{}] instance of resource[{}] partition[{}] allocation is offline, The migration identity will be cleared", clusterName, resourceName, partition.getPartition());
//							HelixManagerUtil.completePartitionMigration(helixManager, resourceName, partition.getPartition());
							hasChangePartition = true;
							newAssignCount++;
						} else if (!StringUtils.isBlank(originalInstanceName) && !originalInstanceName.equals(holder.getInstanceName())) {
							//需要迁移资源, 需要在集群上添加迁移标识，只有在原实例上完成下线，才能在新实例上添加
							logger.debug("[{}] resource[{}] partition[{}] allocation from {} to {}, The migration identity will be add", clusterName, 
									resourceName, partition.getPartition(), originalInstanceName, holder.getInstanceName());
//							HelixManagerUtil.waitPartitionMigration(helixManager, resourceName, partition.getPartition(), originalInstanceName, holder.getInstanceName());
							Map<String, Set<HelixPartition>> resourceMigrationMap = new HashMap<>();
							Map<String, Set<HelixPartition>> oldResourceMigrationMap = instanceMigrationResourceMap.putIfAbsent(originalInstanceName, resourceMigrationMap);
							if (oldResourceMigrationMap != null) {
								resourceMigrationMap = oldResourceMigrationMap;
							}
							Set<HelixPartition> partitions = new HashSet<>();
							Set<HelixPartition> oldPartitions = resourceMigrationMap.putIfAbsent(resourceName, partitions);
							if (oldPartitions != null) {
								partitions = oldPartitions;
							}
							partitions.add(partition);
							hasChangePartition = true;
							moveAssignCount++;
							fromInstances.add(originalInstanceName);
						} else if (!StringUtils.isBlank(originalInstanceName) && originalInstanceName.equals(holder.getInstanceName())) {
							//存在问题：如果实例重启，在集群还未监控到实例下线状态，执行该流程，导致任务不能分配，所以需要在实例分配未改变的情况下，也清除迁移标识
							logger.debug("[{}] instance of resource[{}] partition[{}] allocation is not change, However, in order to handle instance restart, the identity cannot be cleared, So the migration identity will be cleared", 
									clusterName, resourceName, partition.getPartition());
//							HelixManagerUtil.completePartitionMigration(helixManager, resourceName, partition.getPartition());
						}
					}
					if (hasChangePartition) {
						hasChange = true;
						StringBuilder build = new StringBuilder();
						if (newAssignCount > 0) {
							build.append(" from unassign PartitionNum: ").append(newAssignCount);
						}
						if (moveAssignCount > 0) {
							build.append(" from ").append(fromInstances).append(" PartitionNum: ").append(moveAssignCount);
						}
						logger.info("[{}] resource reassigned detail, Instance[{}] Add Resource[{}][{}] PartitionNum[{}] Weight[{}]-->{}", 
								clusterName, instanceName, resourceName, enable ? "enable" : "disable", partitionNum, weight, build.toString());
					}
				}
			}
			if (oldResourcePartitionMap.size() > 0) {//历史分配的资源，现在未分配，即删除
				hasChange = true;
				for (Map.Entry<ResourceInfo, List<HelixPartition>> entry : oldResourcePartitionMap.entrySet()) {
					ResourceInfo resource = entry.getKey();
					List<HelixPartition> partitionList = entry.getValue();
					String resourceName = resource.getResourceName();
					boolean enable = resource.isEnabled();
					int weight = resource.getWeight() == null ? 0 : resource.getWeight();
					int partitionNum = partitionList.size();
					logger.info("[{}] resource reassigned detail, Instance[{}] Remove Resource[{}][{}] PartitionNum[{}] Weight[{}]", 
							clusterName, instanceName, resourceName, enable ? "enable" : "disable", partitionNum, weight);
					for (HelixPartition partition : partitionList) {
						try {
//							HelixManagerUtil.completePartitionMigration(helixManager, resourceName, partition.getPartition());
						} catch (Exception e) {
							logger.error("[{}] resource reassigned detail, Instance[{}] Remove Resource[{}][{}] Partition[{}] failure-->{}", 
									clusterName, instanceName, resourceName, enable ? "enable" : "disable", partition.getPartition(), e.getMessage());
						}
					}
				}
			}
			//判断是否有变更
			if (hasChange) {
				changeInstanceList.add(holder);
			}
		}
		if (!instanceMigrationResourceMap.isEmpty()) {
			if (!this.notifyWorkerMigration(instanceMigrationResourceMap)) {
				throw new BusinessException("Failed to send task migration message to worker");
			}
		}
		return changeInstanceList;
	}
	
	private boolean notifyWorkerMigration(Map<String, Map<String, Set<HelixPartition>>> instanceMigrationResourceMap) {
		if (instanceMigrationResourceMap == null || instanceMigrationResourceMap.isEmpty()) {
			return true;
		}
		int messageCount = 0;
		for(Map.Entry<String, Map<String, Set<HelixPartition>>> instanceEntry : instanceMigrationResourceMap.entrySet()) {
			messageCount += instanceEntry.getValue().size();
		}
		// wait for 30 seconds
		int timeout = 10000;
		int retryCount = 2;
		final CountDownLatch waitComplete = new CountDownLatch(messageCount);
		ClusterMessagingService messageService = helixManager.getMessagingService();
		for(Map.Entry<String, Map<String, Set<HelixPartition>>> instanceEntry : instanceMigrationResourceMap.entrySet()) {
			String instanceName = instanceEntry.getKey();
			Map<String, Set<HelixPartition>> resourceMap = instanceEntry.getValue();
			// Set the Recipient criteria: all nodes that satisfy the criteria will receive the message
			Criteria recipientCriteria = new Criteria();
			recipientCriteria.setInstanceName(instanceName);
			recipientCriteria.setRecipientInstanceType(InstanceType.PARTICIPANT);
//			recipientCriteria.setResource("MyDB");
//			recipientCriteria.setPartition("");
			// Should be processed only by process(es) that are active at the time of sending the message
			// This means if the recipient is restarted after message is sent, it will not be processe.
			recipientCriteria.setSessionSpecific(true);
			for (Map.Entry<String, Set<HelixPartition>> entry : resourceMap.entrySet()) {
				String resourceName = entry.getKey();
				Set<HelixPartition> partitions = entry.getValue();
				Message message = new Message(MessageType.USER_DEFINE_MSG, UUID.randomUUID().toString());
				message.setMsgSubType(TaskMessageType.RESOURCE_MIGRATION.name());
				message.setMsgState(MessageState.NEW);
				message.setSrcSessionId(helixManager.getSessionId());
				message.setTgtSessionId("*");
				message.setResourceName(resourceName);
				for (HelixPartition partition : partitions) {
					message.addPartitionName(HelixUtils.convertPartition2Str(partition));
				}
//				message.setAttribute(Attributes.SRC_NAME, instanceName);
//				message.setAttribute(Attributes.TGT_NAME, instanceName);
				messageService.send(recipientCriteria, message, new AsyncCallback() {

					@Override
					public void onTimeOut() {
						logger.warn("Sending task migration message to worker timed out. --> worker:{},resource:{},partitions:{}", instanceName, resourceName, message.getPartitionNames());
						waitComplete.countDown();
					}

					@Override
					public void onReplyMessage(Message msg) {
						Map<String, String> resultMap = msg.getResultMap();
						logger.info("Successfully sent task migration message to worker. --> worker:{},resource:{},partitions:{},result:{}", instanceName, resourceName, message.getPartitionNames(), resultMap);
						waitComplete.countDown();
					}
					}, timeout, retryCount);
			}
		}
		try {
			waitComplete.await(timeout + 5000, TimeUnit.MILLISECONDS);
			return true;
		} catch (Exception e) {
			logger.warn("Sending task migration message to worker timed out,possibly due to some workers not responding.");
			return false;
		}
	}
	
	private void assignIdealStates(Map<String, IdealState> idealStatesFromAssignment) {
		HelixAdmin helixAdmin = helixManager.getClusterManagmentTool();
		String helixClusterName = helixManager.getClusterName();
		for (String resourceName : idealStatesFromAssignment.keySet()) {
			IdealState idealState = idealStatesFromAssignment.get(resourceName);
			helixAdmin.setResourceIdealState(helixClusterName, resourceName, idealState);
		}
	}

	private TreeSet<InstanceResourceHolder> balanceAssignment(TreeSet<InstanceResourceHolder> orderedSet, RebalanceStrategyType rebalanceStrategy) {
		if (orderedSet.size() <= 1) {
			return orderedSet;
		}
		//是否均衡，需要多方便考虑，目前使用的权重，所有数量上并不一定只相差1个
		//权重积分最高的实例与权重积分最低的实例之间的积分差值小于权重积分最高实例的资源中权重最小的分，表示均衡
		//按策略进行均衡，指定优先（先将指定的资源分配，然后在将余下的平均分配，所有实例在有指定资源的情况下也是尽量平均），未指定优先（先将未指定的平均分配，然后再将指定的分配）
		logger.info("[{}] Trying to rebalance cluster with strategy[{}]!", clusterName, rebalanceStrategy.name());
		InstanceResourceHolder lowestInstance = orderedSet.first();
		InstanceResourceHolder highestInstance = orderedSet.last();
		while(true) {
			int resourceNumForHighest = highestInstance.getResourceNum();
			int resourceNumForFreeForHighest = highestInstance.getResourceNumForFree();
			int minWeight = highestInstance.getResourceLowestWeight();
			int minFreeNum = rebalanceStrategy == RebalanceStrategyType.UnassignPriority ? 1 : 0;//未指定优先，则权重最高的实例最少保留一个自由资源
			if (resourceNumForHighest <= 1 || resourceNumForFreeForHighest <= minFreeNum || minWeight == 0) {//权重积分最高的实例只有一个资源或没有可自由分配资源，则取仅次于当前积分的实例
				highestInstance = orderedSet.lower(highestInstance);
				if (highestInstance == null || highestInstance == lowestInstance) {
					break;
				}
				continue;
			}
			int lowestTotalWeight = 0;
			int highestTotalWeight = 0;
			switch (rebalanceStrategy) {
			case AssignPriority:
				lowestTotalWeight = lowestInstance.getResourceWeight();
				highestTotalWeight = highestInstance.getResourceWeight();
				break;
			case UnassignPriority:
				lowestTotalWeight = lowestInstance.getResourceWeightForFree();
				highestTotalWeight = highestInstance.getResourceWeightForFree();
				break;
			default:
				break;
			}
			//最低的权重大于等于最高权重或最高权重移除一个资源后小于等于最低权重，都表示已经相对均衡
			if (lowestTotalWeight >= highestTotalWeight || highestTotalWeight - minWeight <= lowestTotalWeight) {
				break;
			}
			//先移除再添加，避免重复元素(注意该代码顺序不能随便移动)
			orderedSet.remove(lowestInstance);
			orderedSet.remove(highestInstance);
			//从权重最高实例中取一个权重最小的资源，添加到权重最低的实例中
			ResourceInfo resource = highestInstance.removeResourceForLowestWeight();
			lowestInstance.addResource(resource);
			//重新将实例加入队列（排序）
			orderedSet.add(lowestInstance);
			orderedSet.add(highestInstance);
//			System.out.println("from " + highestInstance.getInstanceName() + " to " + lowestInstance.getInstanceName() + "-->" + resource.getResourceName() + ":" + resource.getWeight() + ":" + resource.getPartitionInstanceMap().size());
			lowestInstance = orderedSet.first();
			highestInstance = orderedSet.last();
		}
		return orderedSet;
	}

	private Set<String> getLiveInstanceName(List<LiveInstance> liveInstances) {
		Set<String> liveInstanceNames = new HashSet<String>();
		for (LiveInstance liveInstance : liveInstances) {
			liveInstanceNames.add(liveInstance.getInstanceName());
		}
		return liveInstanceNames;
	}

	private Set<String> getAddedInstanceSet(Set<String> liveInstances, Set<String> currentInstances) {

		Set<String> addedInstances = new HashSet<String>();
		addedInstances.addAll(liveInstances);
		addedInstances.removeAll(currentInstances);
		return addedInstances;
	}

	private Set<String> getRemovedInstanceSet(Set<String> liveInstances, Set<String> currentInstances) {
		Set<String> removedInstances = new HashSet<String>();
		removedInstances.addAll(currentInstances);
		removedInstances.removeAll(liveInstances);
		return removedInstances;
	}
	/**
	 * 将资源按分配的实例分组
	 * @param resourceList
	 * @return
	 */
	private Map<String, Map<ResourceInfo, List<HelixPartition>>> groupByInstance(List<ResourceInfo> resourceList) {
		Map<String, Map<ResourceInfo, List<HelixPartition>>> instancePartitionMap = new HashMap<>();
		if (resourceList != null && !resourceList.isEmpty()) {
			for (ResourceInfo resource : resourceList) {
				Map<HelixPartition, String> partitionInstanceMap = resource.getPartitionInstanceMap();
				for (Map.Entry<HelixPartition, String> entry : partitionInstanceMap.entrySet()) {
					HelixPartition p = entry.getKey();
					String instance = entry.getValue();
					if (StringUtils.isBlank(instance)) {//表示未分配实例
						instance = EMPYT_INSTANCE_NAME;
					}
					Map<ResourceInfo, List<HelixPartition>> partitionMap = instancePartitionMap.get(instance);
					if (partitionMap == null) {
						partitionMap = new HashMap<>();
						instancePartitionMap.put(instance, partitionMap);
					}
					if (partitionMap.containsKey(resource)) {
						partitionMap.get(resource).add(p);
					} else {
						List<HelixPartition> values = new ArrayList<>();
						values.add(p);
						partitionMap.put(resource, values);
					}
				}
			}
		}
		return instancePartitionMap;
	}
	
}
