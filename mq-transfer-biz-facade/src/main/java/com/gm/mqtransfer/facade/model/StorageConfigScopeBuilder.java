package com.gm.mqtransfer.facade.model;

import java.util.Arrays;

import com.gm.mqtransfer.facade.model.StorageConfigScope.ConfigScopeProperty;

public class StorageConfigScopeBuilder {
	private final ConfigScopeProperty _type;
	private String _clusterName;
	private String _resourceName;
	private String _partitionName;

	public StorageConfigScopeBuilder(ConfigScopeProperty type, String... keys) {
		int argNum = type.getZkPathArgNum();
		if (keys == null || (keys.length != argNum && keys.length != argNum - 1)) {
			throw new IllegalArgumentException("invalid keys. type: " + type + ", keys: " + Arrays.asList(keys));
		}
		_type = type;
		switch (type) {
		case CLUSTER:
			if (keys.length > 0) {
				_clusterName = keys[0];
			}
			break;
		case RESOURCE:
			if (keys.length > 0) {
				_resourceName = keys[0];
			}
			break;
		case PARTITION:
			if (keys.length > 0) {
				_resourceName = keys[0];
			}
			if (keys.length > 1) {
				_partitionName = keys[1];
			}
			break;
		case STAT:
			if (keys.length > 0) {
				_resourceName = keys[0];
			}
			if (keys.length > 1) {
				_partitionName = keys[1];
			}
			break;
		case LOCK:
			if (keys.length > 0) {
				_resourceName = keys[0];
			}
			break;
		case MIGRATION:
			if (keys.length > 0) {
				_resourceName = keys[0];
			}
			if (keys.length > 1) {
				_partitionName = keys[1];
			}
			break;
		default:
			break;
		}
	}

	public StorageConfigScopeBuilder(ConfigScopeProperty type) {
		_type = type;
	}

	public StorageConfigScopeBuilder forCluster(String clusterName) {
		_clusterName = clusterName;
		return this;
	}

	public StorageConfigScopeBuilder forResource(String resourceName) {
		_resourceName = resourceName;
		return this;
	}

	public StorageConfigScopeBuilder forPartition(String partitionName) {
		_partitionName = partitionName;
		return this;
	}

	public StorageConfigScope build() {
		StorageConfigScope scope = null;
		switch (_type) {
		case CLUSTER:
			if (_clusterName == null) {
				scope = new StorageConfigScope(_type, Arrays.asList());
			} else {
				scope = new StorageConfigScope(_type, Arrays.asList(_clusterName));
			}
			break;
		case RESOURCE:
			if (_resourceName == null) {
				scope = new StorageConfigScope(_type, Arrays.asList());
			} else {
				scope = new StorageConfigScope(_type, Arrays.asList(_resourceName));
			}
			break;
		case PARTITION:
			if (_resourceName == null) {
				scope = new StorageConfigScope(_type, Arrays.asList());
			} else if (_partitionName == null) {
				scope = new StorageConfigScope(_type, Arrays.asList(_resourceName));
			} else {
				scope = new StorageConfigScope(_type, Arrays.asList(_resourceName, _partitionName));
			}
			break;
		case STAT:
			if (_resourceName == null) {
				scope = new StorageConfigScope(_type, Arrays.asList());
			} else if (_partitionName == null) {
				scope = new StorageConfigScope(_type, Arrays.asList(_resourceName));
			} else {
				scope = new StorageConfigScope(_type, Arrays.asList(_resourceName, _partitionName));
			}
			break;
		case LOCK:
			if (_resourceName == null) {
				scope = new StorageConfigScope(_type, Arrays.asList());
			} else {
				scope = new StorageConfigScope(_type, Arrays.asList(_resourceName));
			}
			break;
		case MIGRATION:
			if (_resourceName == null) {
				scope = new StorageConfigScope(_type, Arrays.asList());
			} else if (_partitionName == null) {
				scope = new StorageConfigScope(_type, Arrays.asList(_resourceName));
			} else {
				scope = new StorageConfigScope(_type, Arrays.asList(_resourceName, _partitionName));
			}
			break;
		default:
			break;
		}
		return scope;
	}
}
