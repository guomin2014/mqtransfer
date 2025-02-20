package com.gm.mqtransfer.module.cluster.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gm.mqtransfer.facade.exception.BusinessException;
import com.gm.mqtransfer.facade.model.StorageConfigScope.ConfigScopeProperty;
import com.gm.mqtransfer.facade.model.StorageConfigScopeBuilder;
import com.gm.mqtransfer.module.cluster.model.ClusterEntity;
import com.gm.mqtransfer.module.cluster.service.ClusterService;
import com.gm.mqtransfer.module.support.storage.StorageService;

@Service("clusterService")
public class ClusterServiceImpl implements ClusterService{

	@Autowired
	private StorageService storageService;
	
	@Override
	public List<ClusterEntity> findAllEnable() {
		return this.findAll();
	}

	@Override
	public List<ClusterEntity> findAll() {
		return storageService.getChildrenList(new StorageConfigScopeBuilder(ConfigScopeProperty.CLUSTER).build(), ClusterEntity.class);
	}

	@Override
	public ClusterEntity get(String code) {
		return storageService.getData(new StorageConfigScopeBuilder(ConfigScopeProperty.CLUSTER).forCluster(code).build(), ClusterEntity.class);
	}

	@Override
	public void save(ClusterEntity entity) {
		try {
			storageService.saveData(new StorageConfigScopeBuilder(ConfigScopeProperty.CLUSTER).forCluster(entity.getCode()).build(), entity);
		} catch (Exception e) {
			throw new BusinessException(e);
		}
	}

	@Override
	public void delete(String code) {
		try {
			storageService.removeData(new StorageConfigScopeBuilder(ConfigScopeProperty.CLUSTER).forCluster(code).build());
		} catch (Exception e) {
			throw new BusinessException(e);
		}
	}
	
}
