package com.gm.mqtransfer.manager.support.helix;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gm.mqtransfer.facade.model.TransferCluster;
import com.gm.mqtransfer.facade.service.plugin.PluginProviderManagerService;

public class PartitionScaleDetector {

  	protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
  	protected MQType mqType;
	protected HelixMirrorMakerManager _helixMirrorMakerManager;
	protected Long clusterId;
	
	private TransferCluster cluster;
	private PluginProviderManagerService pluginProviderManagerService;
	
	public PartitionScaleDetector(HelixMirrorMakerManager helixMirrorMakerManager, TransferCluster cluster, PluginProviderManagerService pluginProviderManagerService) {
		this._helixMirrorMakerManager = helixMirrorMakerManager;
		this.cluster = cluster;
		this.pluginProviderManagerService = pluginProviderManagerService;
	}

	private final ScheduledExecutorService executor =
			Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r, "Partition-Scale-Detector-Thread");
				}
			});

	/**
	 * 如果某个Topic已经加入待同步列表（已加入helix）,此时其partition 或者 queue数增加了;
	 */
	public void start() {
		LOGGER.info("start partition scale detector...");
		executor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				try {
//					addPartitionMismatchedTopicsToHelix();
				} catch (Throwable e) {
					LOGGER.error("detect mismatched partition(queue) failed:", e);
				}


			}
		}, 60, 60, TimeUnit.SECONDS);
	}

	public void stop() {
		LOGGER.info("stop partition scale detector");
		executor.shutdown();
	}

//	protected void addPartitionMismatchedTopicsToHelix() {
//		Set<String> clusterNames = _helixMirrorMakerManager.getHelixClusterNames(this.clusterId);
//		if (clusterNames != null && clusterNames.size() > 0) {
//			for (String clusterName : clusterNames) {
//				Map<ForwardTaskEntity, T> topicMap = getPartitionMismatchedTopics(clusterName);
//				for (Map.Entry<ForwardTaskEntity, T> entry : topicMap.entrySet()) {
//					_helixMirrorMakerManager.expandTopicInMirrorMaker(clusterName, entry.getKey(), this.mqType, entry.getValue());
//				}
//			}
//		}
//	}
//	
//	private Map<ForwardTaskEntity, T> getPartitionMismatchedTopics(String clusterName) {
//		Map<ForwardTaskEntity, T> partitionsMismatchedTopics = new HashMap<>();
//		if (!_helixMirrorMakerManager.hasPartitionDetectorAuth(clusterName)) {//没有分区变更监控权限
//			return partitionsMismatchedTopics;
//		}
//		List<String> resourcesList = _helixMirrorMakerManager.getHelixResourceList(clusterName);
//		if (resourcesList != null && resourcesList.size() > 0) {
//			for (String resourceName : resourcesList) {
//				//判断资源是否启用
//				IdealState idealState = _helixMirrorMakerManager.getIdealStateForResourceName(clusterName, resourceName);
//				if (!idealState.isEnabled()) {
//					continue;
//				}
//				//获取资源关联的任务信息
//				TransferTask task = _helixMirrorMakerManager.getTaskByResourceName(resourceName);
//				if (task == null) {
//					LOGGER.error("Cannot find task matching resource[{}]", resourceName);
//					continue;
//				}
//				if (task.getFromClusterId() == null || task.getFromClusterId().longValue() != this.clusterId.longValue()) {
//					continue;
//				}
//				MQType type = MQType.getByValue(task.getFromClusterType());
//				if (type != this.mqType) {
//					continue;
//				}
//				String topicName = task.getFromTopic();
//				Set<String> assignPartitions = idealState.getPartitionSet();
//				// 获取queue数不匹配的Topic,直接从 broker 中找;
//				T topicInfo = observer.getTopicInfoFromBroker(topicName);
//				//扩容缩容或变更主机(rocket存在更换主机，但分区数不变的情况)
//				if (partitionChange(task, topicInfo, assignPartitions)) {
//					int partitionInBroker = getPartitionNum(topicInfo);
//					int numPartitionsInHelix = assignPartitions != null ? assignPartitions.size() : 0;
//					LOGGER.info("[{}]detect topic:{} queue change from {} to {}", type.name(), topicName, numPartitionsInHelix, partitionInBroker);
//					partitionsMismatchedTopics.put(task, topicInfo);
//				}
//			}
//		}
//		return partitionsMismatchedTopics;
//	}
//	/**
//	 * 通过topic信息获取分区数量
//	 * @param topicInfo
//	 * @return
//	 */
//	public abstract int getPartitionNum(T topicInfo);
//	/**
//	 * 判断分区是否变更
//	 * @param topicInfo			当前Broker上的分区信息
//	 * @param assignPartitions	集群已分配的分区信息
//	 * @return
//	 */
//	public abstract boolean partitionChange(TransferTask task, T topicInfo, Set<String> assignPartitions);

}
