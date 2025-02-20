package com.gm.mqtransfer.module.support.storage;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.gm.mqtransfer.facade.model.StorageConfigScope;
import com.gm.mqtransfer.module.support.storage.listener.StorageChildDataChangeListener;

public interface StorageService {

	void start();
	
	void stop();
	
	void addListener(StorageConfigScope scope, StorageChildDataChangeListener listener);
	/**
	 * 获取子节点列表
	 * @param scope
	 * @param clazz
	 * @return
	 */
	<T> List<T> getChildrenList(StorageConfigScope scope, Class<T> clazz);
	/**
	 * 获取节点数据
	 * @param scope
	 * @param clazz
	 * @return
	 */
	<T> T getData(StorageConfigScope scope, Class<T> clazz);
	/**
	 * 保存节点数据
	 * @param scope
	 * @param data
	 */
	<T> void saveData(StorageConfigScope scope, T data) throws Exception;
	/**
	 * 删除节点
	 * @param scope
	 * @throws Exception
	 */
	void removeData(StorageConfigScope scope) throws Exception;
	
	boolean tryLock(StorageConfigScope scope, Long waitTime, TimeUnit waitUnit);
	
	void releaseLock(StorageConfigScope scope);
	/**
	 * 创建一个临时节点
	 * @param path
	 * @param data
	 * @return
	 */
	boolean createEphemeralPath(String path, String data);
	/**
	 * 创建一个永久节点
	 * @param path
	 * @param throwable
	 * @return
	 */
	boolean createPersistentPath(String path, boolean throwable);
}
