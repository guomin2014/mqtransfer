package com.gm.mqtransfer.module.support.storage.zookeeper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gm.mqtransfer.facade.common.util.DataUtil;
import com.gm.mqtransfer.facade.exception.BusinessException;
import com.gm.mqtransfer.facade.model.StorageConfigScope;
import com.gm.mqtransfer.facade.model.StorageConfigScope.ConfigScopeProperty;
import com.gm.mqtransfer.facade.model.StorageConfigScopeBuilder;
import com.gm.mqtransfer.facade.service.IApplicationService;
import com.gm.mqtransfer.module.config.StorageConfig;
import com.gm.mqtransfer.module.support.storage.StorageService;
import com.gm.mqtransfer.module.support.storage.listener.StorageChangeEvent;
import com.gm.mqtransfer.module.support.storage.listener.StorageChildDataChangeListener;

@Component
public class ZookeeperStorageService implements StorageService, IApplicationService {

	private final Logger logger = LoggerFactory.getLogger(ZookeeperStorageService.class);

	@Autowired
	private StorageConfig clusterConfig;
	
	private CuratorFramework client;
	
	private volatile boolean hasStopInitClient = false;
	private Thread initClientThread;
	
	private Map<String, InterProcessMutex> lockMap = new ConcurrentHashMap<>();
	
	private Map<String, Set<StorageChildDataChangeListener>> childListenerMap = new ConcurrentHashMap<>();
	private Map<String, PathChildrenCache> childWatcherMap = new ConcurrentHashMap<>();
	
	private final long LOCK_MAX_WAIT_TIME = 5000;
	
	private final TimeUnit LOCK_MAX_WAIT_UNIT = TimeUnit.MILLISECONDS;
	
	private ExecutorService watcherExecutorService;
	
	public void start() {
		logger.info("Trying to start storage service!");
		watcherExecutorService = new ThreadPoolExecutor(0, 10, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		initClientThread = new Thread() {
			private AtomicInteger loadTimes = new AtomicInteger(0);
			@Override
			public void run() {
				while(!hasStopInitClient) {
					logger.info("start async init storage client...");
					try {
						ZookeeperStorageService.this.init();
						hasStopInitClient = true;
						loadTimes.set(0);
						logger.info("Compelet init storage client!");
					} catch (Exception e) {
						logger.error("async init storage client error", e);
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
				logger.info("stop async init storage client.");
			}
			
		};
		initClientThread.start();
	}
	public void stop() {
		logger.info("Trying to stop storage service!");
		try {
			//停止初始化client
			hasStopInitClient = true;
			if (initClientThread != null) {
				initClientThread.interrupt();
			}
		} catch (Exception e) {}
		if (client != null) {
			client.close();
		}
	}
	
	public boolean isReady() {
		return this.client != null && this.client.getState() == CuratorFrameworkState.STARTED;
	}
	
	private void init() {
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(clusterConfig.getMaxSleepIntervalTimeMs(), clusterConfig.getMaxRetries(), clusterConfig.getMaxSleepMs());
		client = CuratorFrameworkFactory.builder()
				.connectString(clusterConfig.getZkUrl())
				.sessionTimeoutMs(clusterConfig.getSessionTimeoutMs())
				.connectionTimeoutMs(clusterConfig.getConnectionTimeoutMs())
				.retryPolicy(retryPolicy)
				.build();
		client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
            @Override
            public void stateChanged(CuratorFramework client, ConnectionState newState) {
                if (newState == ConnectionState.CONNECTED) {
                	logger.info("storage client create connection success.-->zk:{}", clusterConfig.getZkUrl());
                    // 连接成功建立时的逻辑
                	initNodes();
                } else if (newState == ConnectionState.LOST) {
                    // 连接丢失时的逻辑
                } else if (newState == ConnectionState.RECONNECTED) {
                    // 重新连接成功时的逻辑
                }
            }
        });
		client.start();
	}
	
	private void initNodes() {
		//初始化配置节点
		this.initNode(new StorageConfigScopeBuilder(ConfigScopeProperty.CLUSTER).build());
		this.initNode(new StorageConfigScopeBuilder(ConfigScopeProperty.RESOURCE).build());
		this.initNode(new StorageConfigScopeBuilder(ConfigScopeProperty.PARTITION).build());
		this.initNode(new StorageConfigScopeBuilder(ConfigScopeProperty.STAT).build());
		this.initNode(new StorageConfigScopeBuilder(ConfigScopeProperty.LOCK).build());
		this.initNode(new StorageConfigScopeBuilder(ConfigScopeProperty.MIGRATION).build());
		
		this.initNodeChildWatcher(new StorageConfigScopeBuilder(ConfigScopeProperty.RESOURCE).build());
		
	}
	
	private void checkReady() {
		if (!this.isReady()) {
			throw new BusinessException("The storage service is not ready yet");
		}
	}
	
	public boolean createPersistentPath(String path, boolean throwable) {
		if (!this.isReady()) {
			if (throwable) {
				throw new BusinessException("Storage client is not ready");
			} else {
				logger.warn("Failed to create persistent node, Storage client is not ready --> {}", path);
				return false;
			}
		}
		try {
			Stat stat = client.checkExists().forPath(path);
			if (stat == null) {
				String result = client.create().creatingParentContainersIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
				logger.info("Success to create persistent node, node:{}-->result:{}", path, result);
			}
			return true;
		} catch (Exception e) {
			if (throwable) {
				throw new BusinessException("Failed to create persistent node", e);
			} else {
				logger.warn("Failed to create persistent node, {} --> {}", e.getMessage(), path);
				return false;
			}
		}
	}
	
	public boolean createEphemeralPath(String path, String data) {
		if (!this.isReady()) {
			throw new BusinessException("Storage client is not ready");
		}
		try {
			String result = client.create().creatingParentContainersIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path, StringUtils.isNotBlank(data) ? data.getBytes() : null);
			return true;
		} catch (Exception e) {
			throw new BusinessException("Failed to create ephemeral node", e);
		}
	}
	
	private boolean initNode(StorageConfigScope scope) {
		return this.createPersistentPath(scope.getZkPath(), false);
	}
	
	private void initNodeChildWatcher(StorageConfigScope scope) {
		PathChildrenCache pathCache = childWatcherMap.get(scope.getZkPath());
		if (pathCache != null) {
			return;
		}
		pathCache = new PathChildrenCache(client, scope.getZkPath(), true);
		pathCache.getListenable().addListener(new PathChildrenCacheListener() {
			@Override
			public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
				Set<StorageChildDataChangeListener> listeners = childListenerMap.get(scope.getZkPath());
				StorageChangeEvent storageEvent = convertPathChildrenCacheEvent(event);
				if (storageEvent != null && listeners != null) {
					for (StorageChildDataChangeListener listener : listeners) {
						listener.onChange(storageEvent);
					}
				}
			}
		}, watcherExecutorService);
		childWatcherMap.put(scope.getZkPath(), pathCache);
		try {
			pathCache.start();
		} catch (Exception e) {}
	}
	
	private StorageChangeEvent convertPathChildrenCacheEvent(PathChildrenCacheEvent event) {
		StorageChangeEvent retEvent = null;
		PathChildrenCacheEvent.Type type = event.getType();
		String value = null;
		if (event.getData() != null && event.getData().getData() != null) {
			value = new String(event.getData().getData());
		}
		switch (type) {
		case CHILD_ADDED:
			retEvent = new StorageChangeEvent(StorageChangeEvent.Type.CHILD_ADDED, value);
			break;
		case CHILD_UPDATED:
			retEvent = new StorageChangeEvent(StorageChangeEvent.Type.CHILD_UPDATED, value);
			break;
		case CHILD_REMOVED:
			retEvent = new StorageChangeEvent(StorageChangeEvent.Type.CHILD_REMOVED, value);
			break;
		default:
			break;
		}
		return retEvent;
	}
	
	private InterProcessMutex createLock(StorageConfigScope scope) {
		String path = scope.getZkPath();
		if (lockMap.containsKey(path)) {
			return lockMap.get(path);
		} else {
			InterProcessMutex newLock = new InterProcessMutex(client, path);
			InterProcessMutex oldLock = lockMap.putIfAbsent(path, newLock);
			if (oldLock != null) {
				return oldLock;
			}
			return newLock;
		}
	}
	
	private void saveData(String path, byte[] bytes, boolean retryWhenNoNode) throws Exception{
		this.checkReady();
		try {
			client.setData().forPath(path, bytes);
		} catch (org.apache.zookeeper.KeeperException.NoNodeException ex) {
			if (retryWhenNoNode) {
				client.create().creatingParentContainersIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path, bytes);
			} else {
				throw ex;
			}
		}
	}
	
	public <T> void saveData(StorageConfigScope scope, T data) throws Exception {
		this.saveData(scope.getZkPath(), DataUtil.converObj2JsonByte(data), true);
	}
	
	public <T> T getData(StorageConfigScope scope, Class<T> clazz) {
		this.checkReady();
		try {
			byte[] bytes = client.getData().forPath(scope.getZkPath());
			String value = new String(bytes);
			T v = DataUtil.converJsonObjWithNull(value, clazz);
			return v;
		} catch (Exception e) {
//			e.printStackTrace();
		}
		return null;
	}
	
	public <T> List<T> getChildrenList(StorageConfigScope scope, Class<T> clazz) {
		this.checkReady();
		List<T> list = new ArrayList<>();
		try {
			String clusterPath = scope.getZkPath();
			List<String> clusterResourceList = client.getChildren().forPath(clusterPath);
			for (String resourceName : clusterResourceList) {
				String resourcePath = clusterPath + "/" + resourceName;
				try {
					byte[] bytes = client.getData().forPath(resourcePath);
					String value = new String(bytes);
					T v = DataUtil.converJsonObjWithNull(value, clazz);
					if (v != null) {
						list.add(v);
					}
				} catch (Exception e) {
//					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}
	
	public void removeData(StorageConfigScope scope) throws Exception{
		this.checkReady();
		client.delete().forPath(scope.getZkPath());
	}
	
	public boolean tryLock(StorageConfigScope scope, Long waitTime, TimeUnit waitUnit) {
		InterProcessMutex lock = this.createLock(scope);
		try {
			return lock.acquire(waitTime == null ? LOCK_MAX_WAIT_TIME : waitTime, waitTime == null ? LOCK_MAX_WAIT_UNIT : waitUnit);
		} catch (Exception e) {
			return false;
		}
	}
	public void releaseLock(StorageConfigScope scope) {
		InterProcessMutex lock = this.createLock(scope);
		try {
			lock.release();
		} catch (Exception e) {}
	}
	
	public void addListener(StorageConfigScope scope, StorageChildDataChangeListener listener) {
		Set<StorageChildDataChangeListener> newSet = new HashSet<>();
		Set<StorageChildDataChangeListener> oldSet = this.childListenerMap.putIfAbsent(scope.getZkPath(), newSet);
		if (oldSet == null) {
			oldSet = newSet;
		}
		oldSet.add(listener);
	}
	
}
