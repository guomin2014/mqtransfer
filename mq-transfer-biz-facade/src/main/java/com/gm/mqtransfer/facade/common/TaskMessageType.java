package com.gm.mqtransfer.facade.common;

/**
 * 自定义消息类型
 * @author GM
 * @date 2022-10-12
 */
public enum TaskMessageType {
	/** 资源迁移 */
	RESOURCE_MIGRATION,
	/** 资源状态 */
	REQUEST_RESOURCE_STATUS,
	/** 所有资源状态 */
	REQUEST_ALL_RESOURCE_STATUS,
	;
	
	TaskMessageType() {
	}

}
