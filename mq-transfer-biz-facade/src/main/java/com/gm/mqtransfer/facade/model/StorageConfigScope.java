package com.gm.mqtransfer.facade.model;

import java.util.List;
import java.util.Set;

import com.gm.mqtransfer.facade.common.util.StringTemplate;

public class StorageConfigScope {

	/**
	 * Defines the various scopes of configs, and how they are represented on Zookeeper
	 */
	public enum ConfigScopeProperty {
		CLUSTER(1), RESOURCE(1), PARTITION(2), STAT(2), LOCK(1), MIGRATION(2);

		final int _zkPathArgNum;

		private ConfigScopeProperty(int zkPathArgNum) {
			_zkPathArgNum = zkPathArgNum;
		}

		/**
		 * Get the number of template arguments required to generate a full path
		 * 
		 * @return number of template arguments in the path
		 */
		public int getZkPathArgNum() {
			return _zkPathArgNum;
		}
	}

	/**
	 * string templates to generate znode path
	 */
	private static final StringTemplate template = new StringTemplate();
	
	static {
		// get the znode
		template.addEntry(ConfigScopeProperty.CLUSTER, 0, "/mqtransfer/CONFIGS/CLUSTER");
		template.addEntry(ConfigScopeProperty.RESOURCE, 0, "/mqtransfer/CONFIGS/RESOURCE");
		template.addEntry(ConfigScopeProperty.PARTITION, 0, "/mqtransfer/CONFIGS/RESOURCE_PARTITION");
		template.addEntry(ConfigScopeProperty.STAT, 0, "/mqtransfer/STAT");
		template.addEntry(ConfigScopeProperty.LOCK, 0, "/mqtransfer/LOCK");
		template.addEntry(ConfigScopeProperty.MIGRATION, 0, "/mqtransfer/MIGRATION");

		// get children
		template.addEntry(ConfigScopeProperty.CLUSTER, 1, "/mqtransfer/CONFIGS/CLUSTER/{clusterName}");
		template.addEntry(ConfigScopeProperty.RESOURCE, 1, "/mqtransfer/CONFIGS/RESOURCE/{resourceName}");
		template.addEntry(ConfigScopeProperty.PARTITION, 1, "/mqtransfer/CONFIGS/RESOURCE_PARTITION/{resourceName}");
		template.addEntry(ConfigScopeProperty.PARTITION, 2, "/mqtransfer/CONFIGS/RESOURCE_PARTITION/{resourceName}/{partitionName}");
		template.addEntry(ConfigScopeProperty.STAT, 1, "/mqtransfer/STAT/{resourceName}");
		template.addEntry(ConfigScopeProperty.STAT, 2, "/mqtransfer/STAT/{resourceName}/{partitionName}");
		template.addEntry(ConfigScopeProperty.LOCK, 1, "/mqtransfer/LOCK/{resourceName}");
		template.addEntry(ConfigScopeProperty.MIGRATION, 1, "/mqtransfer/MIGRATION/{resourceName}");
		template.addEntry(ConfigScopeProperty.MIGRATION, 2, "/mqtransfer/MIGRATION/{resourceName}/{partitionName}");
	}
	final ConfigScopeProperty _type;
	final String _zkPath;

	/**
	 * Initialize with a type of scope and unique identifiers
	 * 
	 * @param type the scope
	 * @param zkPathKeys keys identifying a ZNode location
	 */
	public StorageConfigScope(ConfigScopeProperty type, List<String> zkPathKeys) {
		Set<Integer> numKeys = template.getEntryNumKeys(type);
		if (!numKeys.contains(zkPathKeys.size())) {
			throw new IllegalArgumentException(type + " requires " + numKeys 
					+ " arguments to get znode, but was: " + zkPathKeys);
		}
		_type = type;
		_zkPath = template.instantiate(type, zkPathKeys.toArray(new String[0]));
	}
	
	/**
	 * Get the path to the corresponding ZNode
	 * 
	 * @return a Zookeeper path
	 */
	public String getZkPath() {
		return _zkPath;
	}
}
