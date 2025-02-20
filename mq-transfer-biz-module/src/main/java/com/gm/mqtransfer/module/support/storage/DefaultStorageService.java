package com.gm.mqtransfer.module.support.storage;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.gm.mqtransfer.facade.common.StorageType;
import com.gm.mqtransfer.facade.config.StorageConfig;
import com.gm.mqtransfer.facade.model.StorageConfigScope;
import com.gm.mqtransfer.facade.service.IApplicationService;
import com.gm.mqtransfer.module.support.storage.listener.StorageChildDataChangeListener;
import com.gm.mqtransfer.module.support.storage.zookeeper.ZookeeperStorageService;

@Component
public class DefaultStorageService implements StorageService, IApplicationService {

	private final Logger logger = LoggerFactory.getLogger(DefaultStorageService.class);
	
	private StorageConfig storageConfig = StorageConfig.getInstance();
	
	private StorageService storageService;
	
	@Override
	public void start() {
		logger.info("Trying to start storage service!");
		String type = storageConfig.getType();
		StorageType storageType = StorageType.getByName(type);
		if (storageType == null) {
			storageType = StorageType.ZOOKEEPER;
		}
		switch (storageType) {
		case FILE:
			//TODO
			break;
		case ZOOKEEPER:
			storageService = new ZookeeperStorageService(storageConfig);
			break;
		}
		if (storageService != null) {
			storageService.start();
		}
	}

	@Override
	public void stop() {
		logger.info("Trying to stop storage service!");
		if (storageService != null) {
			storageService.stop();
		}
	}
	
	private void checkService() {
		if (storageService == null) {
			throw new RuntimeException("Storage service not initialized");
		}
	}

	@Override
	public void addListener(StorageConfigScope scope, StorageChildDataChangeListener listener) {
		this.checkService();
		storageService.addListener(scope, listener);
	}

	@Override
	public <T> List<T> getChildrenList(StorageConfigScope scope, Class<T> clazz) {
		this.checkService();
		return storageService.getChildrenList(scope, clazz);
	}

	@Override
	public <T> T getData(StorageConfigScope scope, Class<T> clazz) {
		this.checkService();
		return storageService.getData(scope, clazz);
	}

	@Override
	public <T> void saveData(StorageConfigScope scope, T data) throws Exception {
		this.checkService();
		storageService.saveData(scope, data);
	}

	@Override
	public void removeData(StorageConfigScope scope) throws Exception {
		this.checkService();
		storageService.removeData(scope);
	}

	@Override
	public boolean tryLock(StorageConfigScope scope, Long waitTime, TimeUnit waitUnit) {
		this.checkService();
		return storageService.tryLock(scope, waitTime, waitUnit);
	}

	@Override
	public void releaseLock(StorageConfigScope scope) {
		this.checkService();
		storageService.releaseLock(scope);
	}

	@Override
	public boolean createEphemeralPath(String path, String data) {
		this.checkService();
		return storageService.createEphemeralPath(path, data);
	}

	@Override
	public boolean createPersistentPath(String path, boolean throwable) {
		this.checkService();
		return storageService.createPersistentPath(path, throwable);
	}

}
