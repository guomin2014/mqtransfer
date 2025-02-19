package com.gm.mqtransfer.worker.service.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.helix.HelixManager;
import org.apache.helix.HelixManagerFactory;
import org.apache.helix.InstanceType;
import org.apache.helix.LiveInstanceInfoProvider;
import org.apache.helix.manager.zk.HelixManagerStateListener;
import org.apache.helix.participant.StateMachineEngine;
import org.apache.helix.participant.statemachine.StateModel;
import org.apache.helix.participant.statemachine.StateModelFactory;
import org.apache.helix.zookeeper.datamodel.ZNRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.gm.mqtransfer.facade.service.IApplicationStartedService;
import com.gm.mqtransfer.facade.service.plugin.PluginProviderManagerService;
import com.gm.mqtransfer.provider.facade.common.Constants;
import com.gm.mqtransfer.provider.facade.model.ServiceDesc;
import com.gm.mqtransfer.worker.config.ClusterConfig;
import com.gm.mqtransfer.worker.service.task.ResourceService;
import com.gm.mqtransfer.worker.service.task.cluster.helix.CustomMessageHandlerFactory;
import com.gm.mqtransfer.worker.service.task.cluster.helix.HelixWorkerOnlineOfflineStateModelFactory;

@Component
public class RegistryService implements IApplicationStartedService{

	private final Logger logger = LoggerFactory.getLogger(RegistryService.class);
	
	@Autowired
	private ClusterConfig clusterConfig;
	@Autowired
	private ResourceService resourceService;
	@Autowired
	private HelixManagerStateListener helixManagerStateListener;
	@Autowired
	private CustomMessageHandlerFactory customMessageHandlerFactory;
	@Autowired
	private PluginProviderManagerService pluginProviderManagerService;
	
	private static final String HELIX_CLUSTER_NAME = "default";
	private static final String HELIX_CLUSTER_ROOT_NODE = "/mqtransfer_cluster";
	
	private HelixManager helixManager;
	private ScheduledExecutorService startHelixExecutor;
	private ScheduledFuture<?> startHelixFuture;
	
	@Override
	public int order() {
		return Integer.MAX_VALUE;
	}

	public void start() {
		try {
			logger.info("starting connect helix cluster...");
			String helixZkUrl = clusterConfig.getZkUrl();
			String instanceId = "W_" + Constants.DEF_INSTANCE_ID_VAL;
			int slashIndex = helixZkUrl.indexOf("/");
			if (slashIndex < 0) {
				helixZkUrl += HELIX_CLUSTER_ROOT_NODE;
			}
			helixManager = HelixManagerFactory.getZKHelixManager(HELIX_CLUSTER_NAME, instanceId, InstanceType.PARTICIPANT, helixZkUrl, helixManagerStateListener);
			helixManager.setLiveInstanceInfoProvider(new LiveInstanceInfoProvider() {
				@Override
				public ZNRecord getAdditionalLiveInstanceInfo() {
					//Registration ability
					ZNRecord record = new ZNRecord(instanceId);
					Collection<ServiceDesc> serviceList = pluginProviderManagerService.getAllProviderService();
					List<String> abilityList = new ArrayList<>();
					for (ServiceDesc desc : serviceList) {
						abilityList.add(JSON.toJSONString(desc));
					}
					record.getListFields().put("ABILITY_LIST", abilityList);
					return record;
				}});
			StateMachineEngine stateMachineEngine = helixManager.getStateMachineEngine();
			// register the MirrorMaker worker
			StateModelFactory<StateModel> stateModelFactory = new HelixWorkerOnlineOfflineStateModelFactory(instanceId, this.resourceService);
			stateMachineEngine.registerStateModelFactory("OnlineOffline", stateModelFactory);
			//add message handler
			helixManager.getMessagingService().registerMessageHandlerFactory(customMessageHandlerFactory.getMessageTypes(), customMessageHandlerFactory);
			helixManager.connect();
		} catch (Exception e) {
			logger.error("connect helix cluster error:", e);
			if (helixManager != null && startHelixExecutor == null) {
				startHelixExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
		            @Override
		            public Thread newThread(Runnable r) {
		                return new Thread(r, "worker-registry-Thread");
		            }
		        });
				if (startHelixFuture != null) {
					startHelixFuture.cancel(true);
		    	}
				startHelixFuture = startHelixExecutor.scheduleAtFixedRate(new Runnable() {
		            @Override
		            public void run() {
		                try {
		                	if (helixManager != null) {
		                		helixManager.connect();
		                		startHelixFuture.cancel(true);
		                	}
		                } catch (Throwable t) {
		                    logger.error("connect helix cluster error", t);
		                }
		            }
		        }, 0, 60, TimeUnit.SECONDS);
		    	logger.info("Start helix connect, interval time {}s", 60);
			}
		}
	}
	
	public void stop() {
		logger.info("stopping disconnect helix cluster...");
		try {
			if (startHelixFuture != null) {
				startHelixFuture.cancel(true);
	    	}
		} catch (Exception e) {}
		try {
			if (helixManager != null) {
				helixManager.disconnect();
			}
		} catch (Exception e) {
			logger.error("disconnect helix cluster error:", e);
		}
	}
}
