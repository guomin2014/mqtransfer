package com.gm.mqtransfer.manager.support.helix;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceInfo {
	/** 资源名 */
	private String resourceName;
	/** 资源是否可用 */
	private boolean enabled;
	/** 资源分区权重 */
	private Integer weight;
	/** 资源分区的集合，key：分区，value：分配的实例 */
	private Map<HelixPartition, String> partitionInstanceMap;
	/** 指定执行主机 */
	private String assignInstance;
	
	public ResourceInfo(String resourceName, Integer weight, Map<HelixPartition, String> partitionInstanceMap) {
		this(resourceName, true, weight, partitionInstanceMap);
	}
	public ResourceInfo(String resourceName, boolean enabled, Integer weight, Map<HelixPartition, String> partitionInstanceMap) {
		this(resourceName, true, weight, partitionInstanceMap, null);
	}
	public ResourceInfo(String resourceName, boolean enabled, Integer weight, Map<HelixPartition, String> partitionInstanceMap, String assignInstance) {
		this.resourceName = resourceName;
		this.enabled = enabled;
		this.weight = weight;
		this.partitionInstanceMap = partitionInstanceMap == null ? new ConcurrentHashMap<HelixPartition, String>() : partitionInstanceMap;
		this.assignInstance = assignInstance;
	}
	
	public String getResourceName() {
		return resourceName;
	}
	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}
	public Integer getWeight() {
		return weight;
	}
	public void setWeight(Integer weight) {
		this.weight = weight;
	}
	public int getTotalWeight() {
		int weight = this.weight == null ? 1 : this.weight;
		int size = partitionInstanceMap == null ? 0 : partitionInstanceMap.size();
		return weight * size;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public Map<HelixPartition, String> getPartitionInstanceMap() {
		return partitionInstanceMap;
	}
	public void setPartitionInstanceMap(Map<HelixPartition, String> partitionInstanceMap) {
		this.partitionInstanceMap = partitionInstanceMap;
	}
	
	public void addPartition(HelixPartition partition, String instanceName, Integer weight) {
		this.partitionInstanceMap.put(partition, instanceName);
		if (weight != null) {
			this.weight = this.weight == null ? 0 : this.weight + weight;
		}
	}
	public void removePartition(HelixPartition partition) {
		this.partitionInstanceMap.remove(partition);
	}
	public String getAssignInstance() {
		return assignInstance;
	}
	public void setAssignInstance(String assignInstance) {
		this.assignInstance = assignInstance;
	}
	@Override
	public String toString() {
		return "ResourceInfo [resourceName=" + resourceName + ", enabled=" + enabled + ", weight=" + weight
				+ ", assignInstance=" + assignInstance + "]";
	}
	
}
