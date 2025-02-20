package com.gm.mqtransfer.manager.support.helix;

import static org.apache.helix.manager.zk.ZKHelixAdmin.CONNECTION_TIMEOUT;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.helix.HelixAdmin;
import org.apache.helix.HelixManager;
import org.apache.helix.controller.HelixControllerMain;
import org.apache.helix.manager.zk.ZKHelixAdmin;
import org.apache.helix.manager.zk.ZKHelixManager;
import org.apache.helix.messaging.handling.HelixTaskExecutor;
import org.apache.helix.messaging.handling.TaskExecutor;
import org.apache.helix.model.HelixConfigScope;
import org.apache.helix.model.HelixConfigScope.ConfigScopeProperty;
import org.apache.helix.model.Message.MessageType;
import org.apache.helix.model.builder.HelixConfigScopeBuilder;
import org.apache.helix.zookeeper.datamodel.serializer.ZNRecordSerializer;
import org.apache.helix.zookeeper.impl.client.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * HelixSetupUtils handles how to create or get a helixCluster in controller.
 */
public class HelixSetupUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(HelixSetupUtils.class);

  private static final String HELIX_ROOT_NODE = "/mqtransfer_cluster";

	public static synchronized HelixManager setup(String helixClusterName, String zkPath, String controllerInstanceId,
			String clusterConfig) {
		// 向zk注册获取实例ID
		try {
			createHelixClusterIfNeeded(helixClusterName, zkPath, clusterConfig);
		} catch (final Exception e) {
			LOGGER.error("Caught exception", e);
			return null;
		}

		try {
			return startHelixControllerInStandadloneMode(helixClusterName, zkPath, controllerInstanceId);
		} catch (final Exception e) {
			LOGGER.error("Caught exception", e);
			return null;
		}
	}

	private static ZkClient createZkClient(String zkPath) {
		int timeOutInSec = Integer.parseInt(System.getProperty(CONNECTION_TIMEOUT, "30"));
		ZkClient _zkClient = new ZkClient(zkPath, timeOutInSec * 1000, timeOutInSec * 1000);
		_zkClient.setZkSerializer(new ZNRecordSerializer());
		_zkClient.waitUntilConnected(timeOutInSec, TimeUnit.SECONDS);
		return _zkClient;
	}

	private static void createHelixRootNodeIfNeeded(String zkPath) {
		int slashIndex = zkPath.indexOf("/");
		if (slashIndex < 0) {
//			throw new IllegalArgumentException("param 'controller.zk.str' should start with '" + HELIX_ROOT_NODE + "'");
			return;
		}
		String prefix = zkPath.substring(slashIndex);
//		if (!prefix.equalsIgnoreCase(HELIX_ROOT_NODE)) {
//			throw new IllegalArgumentException("param 'controller.zk.str' should start with '" + HELIX_ROOT_NODE + "'");
//		}
		ZkClient zkClient = createZkClient(zkPath.substring(0, slashIndex));
		if (!zkClient.exists(prefix)) {
			zkClient.createPersistent(prefix);
		}
		zkClient.close();
	}

	public static void createHelixClusterIfNeeded(String helixClusterName, String zkPath, String clusterConfig) {
		createHelixRootNodeIfNeeded(zkPath);
		final HelixAdmin admin = new ZKHelixAdmin.Builder().setZkAddress(zkPath).build();
		if (admin.getClusters().contains(helixClusterName)) {
			LOGGER.info("cluster[{}] already exist, skip it", helixClusterName);
			return;
		}

		LOGGER.info("Creating a new cluster, as the helix cluster : " + helixClusterName);
		admin.addCluster(helixClusterName, false);

		LOGGER.info("Enable mirror maker machines auto join.");
		final HelixConfigScope scope = new HelixConfigScopeBuilder(ConfigScopeProperty.CLUSTER)
				.forCluster(helixClusterName).build();

		final Map<String, String> props = new HashMap<String, String>();
		props.put(ZKHelixManager.ALLOW_PARTICIPANT_AUTO_JOIN, String.valueOf(true));
		props.put(MessageType.STATE_TRANSITION + "." + HelixTaskExecutor.MAX_THREADS,
				String.valueOf(TaskExecutor.DEFAULT_PARALLEL_TASKS));
		if (StringUtils.isNotBlank(clusterConfig)) {
			try {
				JSONObject configJson = JSON.parseObject(clusterConfig);
				if (configJson != null) {
					for (Map.Entry<String, Object> entry : configJson.entrySet()) {
						String key = entry.getKey();
						Object value = entry.getValue();
						if (StringUtils.isNotBlank(key)) {
							props.put(key, value == null ? "" : value.toString());
						}
					}
				}
			} catch (Exception e) {
				LOGGER.info("add custom cluster config failure-->" + clusterConfig, e);
			}
		}
		admin.setConfig(scope, props);

		LOGGER.info("Adding state model definition named : OnlineOffline generated using : "
				+ OnlineOfflineStateModel.class.toString());

		// add state model definition
		admin.addStateModelDef(helixClusterName, OnlineOfflineStateModel.STATE_MODEL_NAME,
				OnlineOfflineStateModel.build());
		LOGGER.info("New Cluster setup completed...");
		admin.close();
	}

	private static HelixManager startHelixControllerInStandadloneMode(String helixClusterName, String zkUrl,
			String controllerInstanceId) {
		LOGGER.info("Starting Helix Standalone Controller ... ");
		return HelixControllerMain.startHelixController(zkUrl, helixClusterName, controllerInstanceId,
				HelixControllerMain.STANDALONE);
	}
}
