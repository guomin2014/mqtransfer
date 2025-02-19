package com.gm.mqtransfer.manager.support.helix;

/**
 * 资源迁移状态
 * @author gm
 *
 */
public class HelixResourceMoveStatus {
	/** 资源名 */
	private String resourceName;
	/** 分区 */
	private Integer partition;
	/** 状态，0：待迁移，1：源方已迁出，2：目标方已迁入 */
	private Integer status;
	/** 源实例名称 */
	private String originalInstanceName;
	/** 目标实例名称 */
	private String assignInstanceName;
	
	public HelixResourceMoveStatus(String resourceName, Integer partition, Integer status) {
		this.resourceName = resourceName;
		this.partition = partition;
		this.status = status;
	}
	
	public String getResourceName() {
		return resourceName;
	}
	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}
	public Integer getPartition() {
		return partition;
	}
	public void setPartition(Integer partition) {
		this.partition = partition;
	}
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	public String getOriginalInstanceName() {
		return originalInstanceName;
	}
	public void setOriginalInstanceName(String originalInstanceName) {
		this.originalInstanceName = originalInstanceName;
	}
	public String getAssignInstanceName() {
		return assignInstanceName;
	}
	public void setAssignInstanceName(String assignInstanceName) {
		this.assignInstanceName = assignInstanceName;
	}
}
