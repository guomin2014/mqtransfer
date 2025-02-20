package com.gm.mqtransfer.manager.support.helix;

public enum RebalanceStrategyType {

	/** 指定优先（先将指定的资源分配，然后在将余下的平均分配） */
	AssignPriority,
	/** 未指定优先（先将未指定的平均分配，然后再将指定的分配） */
	UnassignPriority;
	
	public static RebalanceStrategyType getByName(String name) {
		if (name == null) {
			return null;
		}
		for (RebalanceStrategyType type : values()) {
			if (type.name().equalsIgnoreCase(name)) {
				return type;
			}
		}
		return null;
	}
}
