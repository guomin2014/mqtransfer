package com.gm.mqtransfer.facade.common;

/**
 * 存储类型
 * @author GM
 *
 */
public enum StorageType {
	FILE,ZOOKEEPER;
	
	public static StorageType getByName(String name) {
		if (name == null) {
			return null;
		}
		for (StorageType ele : values()) {
			if (ele.name().equalsIgnoreCase(name)) {
				return ele;
			}
		}
		return null;
	}
}
