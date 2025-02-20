/**
ø * Copyright (C) 2015-2016 Uber Technology Inc. (streaming-core@uber.com)
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

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.helix.Criteria;
import org.apache.helix.HelixAdmin;
import org.apache.helix.HelixManager;
import org.apache.helix.InstanceType;
import org.apache.helix.PropertyKey.Builder;
import org.apache.helix.model.IdealState;
import org.apache.helix.model.LiveInstance;
import org.apache.helix.model.Message;
import org.apache.helix.model.Message.MessageState;
import org.apache.helix.model.Message.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alipay.sofa.ark.spi.service.ArkInject;
import com.gm.mqtransfer.facade.config.ClusterConfig;
import com.gm.mqtransfer.facade.exception.BusinessException;
import com.gm.mqtransfer.facade.model.TaskShardingConfig;
import com.gm.mqtransfer.facade.model.TransferPartitionConfig;
import com.gm.mqtransfer.facade.model.TransferTask;
import com.gm.mqtransfer.facade.service.IApplicationStartedService;
import com.gm.mqtransfer.facade.service.plugin.PluginProviderManagerService;
import com.gm.mqtransfer.manager.config.TomcatConfig;
import com.gm.mqtransfer.provider.facade.util.NetUtils;

/**
 * Main logic for Helix Controller. Provided all necessary APIs for topics
 * management. Have two modes auto/custom: Auto mode is for helix taking care of
 * all the idealStates changes Custom mode is for creating a balanced
 * idealStates when necessary, like instances added/removed, new topic
 * added/expanded, old topic deleted
 */
@Component
public class HelixMirrorMakerManager implements IApplicationStartedService{

	private static final String HELIX_CLUSTER_NAME = "default";
	private static final String HELIX_CLUSTER_ROOT_NODE = "/mqtransfer_cluster";
	
	private static final Logger logger = LoggerFactory.getLogger(HelixMirrorMakerManager.class);
	private String instanceId;
	private volatile boolean hasStopInitHelixCluster = false;
	private Thread initHelixClusterThread;
	/** helix集群管理集合，key：集群名 */
	private HelixManager helixManager;
	/** helix集群自动均衡管理集合，key：集群名 */
	private HelixBalanceManager helixBalanceManager;
	
	@Autowired
	private TomcatConfig tomcatConfig;
//	@Autowired
	@ArkInject
	private PluginProviderManagerService pluginProviderManagerService;
	
	@Override
	public int order() {
		return 10;
	}
	public void start() {
		logger.info("Trying to start HelixMirrorMakerManager!");
		this.init();
	}

	public void stop() {
		logger.info("Trying to stop HelixMirrorMakerManager!");
		try {
			//停止初始化Helix集群
			hasStopInitHelixCluster = true;
			if (initHelixClusterThread != null) {
				initHelixClusterThread.interrupt();
			}
		} catch (Exception e) {}
		//停止集群均衡服务
		helixBalanceManager.stop();
		//关闭Helix集群连接
		helixManager.disconnect();
	}

	private String generateClusterName() {
		return HELIX_CLUSTER_NAME;
	}

	private String generalInstanceId() {
		//获取当前地址与端口
		String ip = NetUtils.getLocalHostIP();
		return String.format("C_%s@%s", ip, tomcatConfig.getPort());
	}
	private String[] parseInstanceId(String instanceId) {
		if (StringUtils.isBlank(instanceId)) {
			return null;
		}
		if (instanceId.startsWith("C_")) {
			instanceId = instanceId.substring(2);
		}
		return instanceId.split("[@_]");
	}
	
	private void init() {
		instanceId = this.generalInstanceId();//由于Http端口在start时才能确定，所以生成实例ID必须在start里操作
		initHelixClusterThread = new Thread() {
			private AtomicInteger loadTimes = new AtomicInteger(0);
			@Override
			public void run() {
				while(!hasStopInitHelixCluster) {
					logger.info("start async helix cluster...");
					try {
						HelixMirrorMakerManager.this.initHelixCluster();
						hasStopInitHelixCluster = true;
						loadTimes.set(0);
					} catch (Exception e) {
						logger.error("async helix cluster error", e);
						int times = loadTimes.incrementAndGet();
						if (times <= 0) {
							loadTimes.set(1);
							times = 1;
						}
						long sleepTime = 1000 * times;
						try {
							Thread.sleep(sleepTime);
						} catch (Exception ex) {}
					}
				}
				logger.info("stop async helix cluster.");
			}
		};
		//由于多个集群共享一个Controller服务，所以controller将监控所有集群，如果未初始化成功，则会一直循环进行初始化，直到成功
		initHelixClusterThread.start();
	}
	
	public boolean isReady() {
		return this.helixManager != null && this.helixManager.isConnected();
	}

	/**
	 * 初始化Helix集群
	 * @return
	 */
	private boolean initHelixCluster() {
		//初始化Helix当前集群
		this.initHelixManager();
		return true;
	}
	/**
	 * 初始化集群管理对象
	 * @param clusterTag
	 * @return
	 */
	private synchronized HelixManager initHelixManager() {
		String clusterName = generateClusterName();
		if (this.helixManager != null && this.helixManager.isConnected()) {//集群已经初始化，直接返回
			return helixManager;
		}
		ClusterConfig config = ClusterConfig.getInstance();
		String zkUrl = config.getZkUrl();
		int slashIndex = zkUrl.indexOf("/");
		if (slashIndex < 0) {
			zkUrl += HELIX_CLUSTER_ROOT_NODE;
		}
		logger.info("Trying to init cluster manager[{}]", clusterName);
		String clusterConfig = config.getClusterConfig();
		//初始化Helix当前集群
		helixManager = HelixSetupUtils.setup(clusterName, zkUrl, instanceId, clusterConfig);//HelixManagerFactory.getZKHelixManager(clusterName, instanceId, InstanceType.CONTROLLER, zkUrl);
		try {
			helixManager.connect();
		} catch (Exception e) {
			throw new BusinessException(e);
		}
		//添加集群监控
		logger.info("Trying to start cluster balance manager[{}]", clusterName);
		this.helixBalanceManager = new HelixBalanceManager(config, helixManager);
		helixBalanceManager.start();
		logger.info("Trying to register LiveInstanceChangeListener[{}]", clusterName);
		CustomLiveInstanceChangeListener liveInstanceChangeListener = new CustomLiveInstanceChangeListener(helixBalanceManager);
		try {
			helixManager.addLiveInstanceChangeListener(liveInstanceChangeListener);
		} catch (Exception e) {
			logger.error("Failed to add LiveInstanceChangeListener[" + clusterName + "]", e);
		}
		//添加自定义消息处理器
		helixManager.getMessagingService().registerMessageHandlerFactory(MessageType.USER_DEFINE_MSG.name(), new CustomMessageHandlerFactory(this));
		return helixManager;
	}

	public synchronized void deleteResource(String resourceName) {
		String clusterName = generateClusterName();
		if (helixManager == null) {
			throw new BusinessException("can not find cluster" + clusterName);
		}
		helixManager.getClusterManagmentTool().dropResource(clusterName, resourceName);
	}
	/**
	 * 向集群添加资源
	 * @param clusterTag
	 * @param resourceName
	 */
	public synchronized void addResourceInMirrorMaker(TaskShardingConfig task, boolean notifyOther) {
		//更新集群资源
		if (helixManager.isLeader()) {
			logger.info("The current instance is a leader, and the resource information will be updated");
			//将资源添加到Helix集群
			this.addOrUpdateResourceIdealState(task, true);
		} else if (notifyOther) {
			logger.info("The current instance is not a leader, will notify leader controller");
			String clusterName = helixManager.getClusterName();//this.generateClusterName(clusterTag);
			String resourceName = task.getTaskCode();//HelixUtils.generateResourceName(task);
			//非Leader，则需要通知leader进行处理
			LiveInstance leader = helixManager.getHelixDataAccessor().getProperty(new Builder(clusterName).controllerLeader());
			if (leader != null) {
				try {
					Criteria cr = new Criteria();
					cr.setInstanceName("%");
					cr.setRecipientInstanceType(InstanceType.CONTROLLER);//CONTROLLER_PARTICIPANT
					cr.setSessionSpecific(true);
					Message msg = new Message(MessageType.USER_DEFINE_MSG, UUID.randomUUID().toString());
					msg.setMsgSubType("REQUEST_RESOURCE_ADD");
					msg.setMsgState(MessageState.NEW);
					msg.setSrcSessionId(helixManager.getSessionId());
					msg.setTgtSessionId("*");
					msg.setResourceName(resourceName);
					int result = helixManager.getMessagingService().send(cr, msg);
					logger.info("send message to leader controller, result:" + result);
				} catch (Exception e) {
					logger.error("send message to leader controller failure", e);
					throw new BusinessException("notify cluster leader failure");
				}
			} else {
				logger.error("cannot find cluster leader[{}]", clusterName);
				throw new BusinessException("cannot find cluster leader");
			}
		}
	}
	public synchronized String[] findLeaderByClusterTag() {
		logger.info("find leader ...");
		String clusterName = this.generateClusterName();
		//初始化集群
		HelixManager helixManager = this.initHelixManager();
		LiveInstance leader = helixManager.getHelixDataAccessor().getProperty(new Builder(clusterName).controllerLeader());
		if (leader != null) {
			String instanceName = leader.getInstanceName();
			String[] names = this.parseInstanceId(instanceName);
			return names;
		}
		return null;
	}
	public synchronized void rebalanceByClusterTag() {
		logger.info("rebalance cluster...");
		String clusterName = this.generateClusterName();
		if (helixManager == null) {
			throw new BusinessException("can not find cluster");
		}
		if (!helixManager.isLeader()) {
			throw new BusinessException("current instance is not leader");
		}
		//资源停用，但实例未变更，可能造成集群实例资源分布不均衡，故需要手动触发集群的均衡
		if (helixBalanceManager != null) {
			logger.info("Manually trigger cluster balancing [{}]", clusterName);
			helixBalanceManager.rebalance();
		} else {
			throw new BusinessException("can not find cluster balance service");
		}
	}
	public synchronized void disableResourceSharding(TaskShardingConfig task) {
		logger.info("disable resource...[{}]", task.toString());
		if (helixManager == null) {
			throw new BusinessException("can not find cluster manager tools");
		}
		if (!helixManager.isLeader()) {
			throw new BusinessException("current cluster manager instance is not leader");
		}
		String clusterName = helixManager.getClusterName();
		String resourceName = task.getTaskCode();
		HelixAdmin helixAdmin = helixManager.getClusterManagmentTool();
		boolean needBalance = false;
		try {
			if (helixBalanceManager != null) {
				helixBalanceManager.tryLockRefreshClusterResource();
			}
			IdealState oldIdealState = helixAdmin.getResourceIdealState(clusterName, resourceName);
			if (oldIdealState == null) {
				return;
			}
			HelixPartition partition = new HelixPartition(task.getTaskCode(), task.getFromPartition().getBrokerName(), task.getFromPartition().getPartition());
			Map<String, Map<String, String>> mapFields = oldIdealState.getRecord().getMapFields();
			String partitionName = HelixUtils.convertPartition2Str(partition);
			mapFields.remove(partitionName);
			oldIdealState.getRecord().setMapFields(mapFields);
			helixAdmin.setResourceIdealState(clusterName, resourceName, oldIdealState);
			if (mapFields.isEmpty()) {
				helixAdmin.enableResource(clusterName, resourceName, false);
				needBalance = true;
			}
		} finally {
			if (helixBalanceManager != null) {
				helixBalanceManager.releaseRefreshClusterResource();
			}
			if (helixAdmin != null) {
				helixAdmin.close();
			}
		}
		//资源停用，但实例未变更，可能造成集群实例资源分布不均衡，故需要手动触发集群的均衡
		if (helixBalanceManager != null && needBalance) {
			logger.info("Resource status change({}:{}), automatic cluster balancing will be triggered!", clusterName, resourceName);
			helixBalanceManager.rebalance();
		}
	}
	public synchronized void disableResource(TransferTask task) {
		logger.info("disable resource...[{}]", task.toString());
		if (helixManager == null) {
			throw new BusinessException("can not find cluster manager tools");
		}
		if (!helixManager.isLeader()) {
			throw new BusinessException("current cluster manager instance is not leader");
		}
		String clusterName = helixManager.getClusterName();
		String resourceName = task.getCode();
		HelixAdmin helixAdmin = helixManager.getClusterManagmentTool();
		try {
			if (helixBalanceManager != null) {
				helixBalanceManager.tryLockRefreshClusterResource();
			}
			IdealState oldIdealState = helixAdmin.getResourceIdealState(clusterName, resourceName);
			if (oldIdealState == null) {
				return;
			}
			helixAdmin.enableResource(clusterName, resourceName, false);
		} finally {
			if (helixBalanceManager != null) {
				helixBalanceManager.releaseRefreshClusterResource();
			}
			if (helixAdmin != null) {
				helixAdmin.close();
			}
		}
		//资源停用，但实例未变更，可能造成集群实例资源分布不均衡，故需要手动触发集群的均衡
		logger.info("Resource status change({}:{}), automatic cluster balancing will be triggered!", clusterName, resourceName);
		if (helixBalanceManager != null) {
			helixBalanceManager.rebalance();
		}
	}
	public synchronized void deleteResource(TransferTask task) {
		logger.info("delete resource...[{}]", task.toString());
		if (helixManager == null) {
			throw new BusinessException("can not find cluster manager tools");
		}
		if (!helixManager.isLeader()) {
			throw new BusinessException("current cluster manager instance is not leader");
		}
		String clusterName = helixManager.getClusterName();
		String resourceName = task.getCode();
		HelixAdmin helixAdmin = helixManager.getClusterManagmentTool();
		try {
			if (helixBalanceManager != null) {
				helixBalanceManager.tryLockRefreshClusterResource();
			}
			IdealState oldIdealState = helixAdmin.getResourceIdealState(clusterName, resourceName);
			if (oldIdealState == null) {
				return;
			}
			helixAdmin.dropResource(clusterName, resourceName);
		} finally {
			if (helixBalanceManager != null) {
				helixBalanceManager.releaseRefreshClusterResource();
			}
			if (helixAdmin != null) {
				helixAdmin.close();
			}
		}
		//资源停用，但实例未变更，可能造成集群实例资源分布不均衡，故需要手动触发集群的均衡
		logger.info("Resource status change({}:{}), automatic cluster balancing will be triggered!", clusterName, resourceName);
		if (helixBalanceManager != null) {
			helixBalanceManager.rebalance();
		}
	}

	/**
	 * 更新任务在集群中的资源信息
	 * @param helixAdmin
	 * @param task
	 * @return	分配的实例名称
	 */
	private String addOrUpdateResourceIdealState(TaskShardingConfig task, Boolean enable) {
		HelixAdmin helixAdmin = helixManager.getClusterManagmentTool();
		String clusterName = helixManager.getClusterName();
		//将资源添加到Helix集群
		String resourceName = HelixUtils.generateResourceName(task);
		//对于已经存在的资源，需要更新资源的内容（Task）
		//方案一：获取群集中已经存在的资源，发送资源变更通知（存在问题：如果此时进行了资源平衡且资源从目标主机上迁移，则导致变更不能用）
		//方案二：业务限制，即任务修改后，只有启停操作才能让修改生效（推荐）
		try {
			IdealState oldIdealState = helixAdmin.getResourceIdealState(clusterName, resourceName);
			InstanceResourceHolder idleHolder = helixBalanceManager != null ? helixBalanceManager.getMaxIdleInstance() : null;
			IdealState idealState = IdealStateBuilder.buildCustomIdealStateForAdd(task, oldIdealState, idleHolder);
			if (idealState != null) {
				if (oldIdealState != null) {
					//已经分配实例的资源分配信息保持不变（避免资源迁移发生）
					helixAdmin.setResourceIdealState(clusterName, resourceName, idealState);
					logger.info("update resource [{}] to helix cluster [{}] success", resourceName, clusterName);
				} else {
					helixAdmin.addResource(clusterName, resourceName, idealState);
					logger.info("add resource [{}] to helix cluster [{}] success", resourceName, clusterName);
				}
				HelixPartition partition = new HelixPartition(task.getTaskCode(), task.getFromPartition().getBrokerName(), task.getFromPartition().getPartition());
				String partitionName = HelixUtils.convertPartition2Str(partition);
				Set<String> instances = idealState.getInstanceSet(partitionName);
				String instanceName = null;
				if (instances.size() > 0) {
					instanceName = instances.iterator().next();
				}
				InstanceResourceHolder holder = helixBalanceManager != null ? helixBalanceManager.getInstanceHolderByName(instanceName) : null;
				if (holder != null) {
					TransferPartitionConfig transferPartitionConfig = task.getTransferPartitionConfig();
					holder.addPartition(resourceName, partition, transferPartitionConfig.getPartitionWeight(), transferPartitionConfig.getAssignInstance());
				}
				return instanceName;
			} else {
				logger.warn("add resource [{}] to helix cluster [{}] error, can not generation ideal state", resourceName, clusterName);
			}
		} catch (Exception e) {
			logger.error("add resource [{}] to helix cluster [{}] error, {}", resourceName, clusterName, e.getMessage());
			throw new BusinessException(e);
		}
		return null;
	}
	/**
	 * 获取资源当前状态
	 * @param resourceName
	 * @return	true:运行, false:禁止
	 */
	public boolean getResourceStatus(String resourceName) {
		HelixAdmin helixAdmin = helixManager.getClusterManagmentTool();
		String clusterName = helixManager.getClusterName();
		//将资源添加到Helix集群
		IdealState oldIdealState = helixAdmin.getResourceIdealState(clusterName, resourceName);
		return oldIdealState != null ? oldIdealState.isEnabled() : false;
	}

}
