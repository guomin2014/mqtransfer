package com.gm.mqtransfer.provider.facade.common;

/**
 * 位置点类型
 * FIRST_OFFSET：开始位置,LAST_OFFSET：最新位置,DESIGN_TIMESTAMP：指定时间, DESIGN_OFFSET：指定位置
 * @author GM
 * @date 2024-06-17
 *
 */
public enum OffsetTypeEnum {
	FIRST_OFFSET("开始位置"),
	LAST_OFFSET("最新位置"),
	DESIGN_TIMESTAMP("指定时间"),
	DESIGN_OFFSET("指定位置"),
	;
	
	private String desc;
	
	public String getDesc() {
		return desc;
	}

	OffsetTypeEnum(String desc) {
		this.desc = desc;
	}
	
	public static OffsetTypeEnum getByName(String name) {
		if (name == null || name.trim().length() == 0) {
			return null;
		}
		for (OffsetTypeEnum type : values()) {
			if (type.name().equalsIgnoreCase(name)) {
				return type;
			}
		}
		return null;
	}
}
