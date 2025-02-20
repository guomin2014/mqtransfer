
package com.gm.mqtransfer.manager.support.helix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;

/**
 * InstanceTopicPartitionHolder is a wrapper for instance and the topicPartitionSet it's holding.
 */
public class InstanceResourceHolder {

  private final String _instanceName;
  /** 资源列表 */
  private final Map<String, ResourceInfo> resourceMap = new HashMap<>();
  /** 总权重 */
  private final AtomicInteger totalWeight = new AtomicInteger(0);
  /** 总分区数 */
  private final AtomicInteger totalPartition = new AtomicInteger(0);
  /** 自由资源的总权重（不包含指定资源外的权重） */
  private final AtomicInteger totalWeightForFree = new AtomicInteger(0);
  /** 自由资源的总分区数 */
  private final AtomicInteger totalPartitionForFree = new AtomicInteger(0);
  private final ReentrantLock lock = new ReentrantLock();

  public InstanceResourceHolder(String instance) {
    _instanceName = instance;
  }

  public String getInstanceName() {
    return _instanceName;
  }

  public List<ResourceInfo> getResourceList() {
    return ImmutableList.copyOf(resourceMap.values());
  }

  public int getResourceNum() {
    return resourceMap.size();
  }
  public int getPartitionNum() {
	  return totalPartition.get();
  }
  /**
   * 获取总权重积分
   * @return
   */
  public int getResourceWeight() {
	  return totalWeight.get();
  }
  public int getResourceNumForFree() {
	  int count = 0;
	  List<ResourceInfo> list = this.getResourceList();
	  for (ResourceInfo r : list) {
		  count += StringUtils.isBlank(r.getAssignInstance()) ? 1 : 0;
	  }
	  return count;
  }
  public int getPartitionNumForFree() {
	  return totalPartitionForFree.get();
  }
  /**
   * 获取总权重积分
   * @return
   */
  public int getResourceWeightForFree() {
	  return totalWeightForFree.get();
  }

  public void addResource(ResourceInfo resource) {
	  if (resource == null) {
		  return;
	  }
	  Map<HelixPartition, String> partitionInstanceMap = resource.getPartitionInstanceMap();
	  if (partitionInstanceMap == null) {
		  partitionInstanceMap = new HashMap<>();
		  resource.setPartitionInstanceMap(partitionInstanceMap);
	  }
	  int weight = resource.getTotalWeight();
	  if (!resource.isEnabled()) {
		  weight = 0;//不可用资源无权重
	  }
	  lock.lock();
	  try {
		  resourceMap.put(resource.getResourceName(), resource);
		  totalWeight.addAndGet(weight);
		  totalPartition.addAndGet(partitionInstanceMap.size());
		  if (StringUtils.isBlank(resource.getAssignInstance())) {//未指定主机的资源
			  totalWeightForFree.addAndGet(weight);
			  totalPartitionForFree.addAndGet(partitionInstanceMap.size());
		  }
	  } finally {
		  lock.unlock();
	  }
  }
  
  public void addPartition(String resourceName, HelixPartition partition, Integer weight, String assignInstance) {
	  lock.lock();
	  try {
		  if (weight == null) {
			  weight = 1;
		  }
		  ResourceInfo resource = resourceMap.get(resourceName);
		  if (resource == null) {
			  Map<HelixPartition, String> partitionInstanceMap = new HashMap<>();
			  partitionInstanceMap.put(partition, this._instanceName);
			  resource = new ResourceInfo(resourceName, true, weight, partitionInstanceMap, assignInstance);
			  resourceMap.put(resourceName, resource);
		  } else {
			  resource.setAssignInstance(assignInstance);
			  resource.addPartition(partition, this._instanceName, weight);
		  }
		  totalWeight.addAndGet(weight);
		  totalPartition.incrementAndGet();
		  if (StringUtils.isBlank(resource.getAssignInstance())) {//未指定主机的资源
			  totalWeightForFree.addAndGet(weight);
			  totalPartitionForFree.incrementAndGet();
		  }
	  } finally {
		  lock.unlock();
	  }
  }
  
  public void removePartition(String resourceName, HelixPartition partition) {
	  lock.lock();
	  try {
		  ResourceInfo resource = resourceMap.get(resourceName);
		  if (resource == null) {
			  return;
		  }
		  resource.removePartition(partition);
		  Integer weight = resource.getWeight();
		  if (weight != null) {
			  totalWeight.addAndGet(weight * -1);
		  }
		  totalPartition.decrementAndGet();
		  if (StringUtils.isBlank(resource.getAssignInstance())) {//未指定主机的资源
			  if (weight != null) {
				  totalWeightForFree.addAndGet(weight * -1);
			  }
			  totalPartitionForFree.decrementAndGet();
		  }
	  } finally {
		  lock.unlock();
	  }
  }
  
  /**
   * 获取资源最小的权重分
   * @return
   */
  public int getResourceLowestWeight() {
	  lock.lock();
	  try {
		  List<ResourceInfo> list = this.getResourceList();
		  List<ResourceInfo> resourceList = new ArrayList<>();
		  for (ResourceInfo r : list) {
			  if (StringUtils.isBlank(r.getAssignInstance()) && r.isEnabled()) {//过滤掉指定主机并且可用的资源
				  resourceList.add(r);
			  }
		  }
		  if (resourceList.isEmpty()) {
			  return 0;
		  }
		  // 使用匿名比较器排序
	      Collections.sort(resourceList, new Comparator<ResourceInfo>() {
	          @Override
	          public int compare(ResourceInfo p1, ResourceInfo p2) {
	        	  if (p1 == null) {
	        		  return -1;
	        	  }
	        	  if (p2 == null) {
	        		  return 1;
	        	  }
	              return p1.getWeight().compareTo(p2.getWeight());
	          }
	      });
	      return resourceList.get(0).getWeight();
	  } finally {
		  lock.unlock();
	  }
  }

  /**
   * 将权重最低的资源移除
   * @return
   */
	public ResourceInfo removeResourceForLowestWeight() {
		lock.lock();
		try {
			List<ResourceInfo> list = this.getResourceList();
			List<ResourceInfo> freeList = new ArrayList<>();
			for (ResourceInfo r : list) {
				if (StringUtils.isBlank(r.getAssignInstance()) && r.isEnabled()) {// 过滤掉指定主机并且可用的资源
					freeList.add(r);
				}
			}
			if (freeList.isEmpty()) {
				return null;
			}
			if (freeList.size() >= 2) {
				Collections.sort(freeList, new Comparator<ResourceInfo>() {
					@Override
					public int compare(ResourceInfo p1, ResourceInfo p2) {
						if (p1 == null) {
							return -1;
						}
						if (p2 == null) {
							return 1;
						}
						if (!p1.isEnabled() || !p2.isEnabled()) {//不可用的资源都排在队列后面
							return 1;
						}
						return p1.getWeight().compareTo(p2.getWeight());
					}
				});
			}
			ResourceInfo retResource = null;
			for (ResourceInfo resource : freeList) {
				if (removeResource(resource)) {
					retResource = resource;
					break;
				}
			}
			return retResource;
		} finally {
			lock.unlock();
		}
	}
	
	public boolean removeResource(String resourceName) {
		lock.lock();
		try {
			ResourceInfo oldResource = resourceMap.remove(resourceName);
			if (oldResource != null) {
				Integer weight = oldResource.getTotalWeight();
				if (weight != null && oldResource.isEnabled()) {
					totalWeight.addAndGet(weight.intValue() * -1);
					if (StringUtils.isBlank(oldResource.getAssignInstance())) {// 未指定主机的资源
						totalWeightForFree.addAndGet(weight.intValue() * -1);
					}
				}
				if (oldResource.getPartitionInstanceMap() != null && oldResource.getPartitionInstanceMap().size() > 0) {
					totalPartition.addAndGet(oldResource.getPartitionInstanceMap().size() * -1);
					if (StringUtils.isBlank(oldResource.getAssignInstance())) {// 未指定主机的资源
						totalPartitionForFree.addAndGet(oldResource.getPartitionInstanceMap().size() * -1);
					}
				}
				return true;
			}
			return false;
		} finally {
			lock.unlock();
		}
	}
	private boolean removeResource(ResourceInfo resource) {
		return this.removeResource(resource.getResourceName());
	}

	public static Comparator<InstanceResourceHolder> getComparator(final RebalanceStrategyType rebalanceStrategy) {
		return new Comparator<InstanceResourceHolder>() {
			@Override
			public int compare(InstanceResourceHolder o1, InstanceResourceHolder o2) {
				if (o1.getInstanceName().equalsIgnoreCase(o2.getInstanceName())) {// 表示同一个实例
					return 0;
				}
				int size1 = rebalanceStrategy == RebalanceStrategyType.UnassignPriority ? o1.getResourceWeightForFree() : o1.getResourceWeight();
				int size2 = rebalanceStrategy == RebalanceStrategyType.UnassignPriority ? o2.getResourceWeightForFree() : o2.getResourceWeight();
				
				if (size1 != size2) {
					return size1 - size2;
				} else {
					int p1 = rebalanceStrategy == RebalanceStrategyType.UnassignPriority ? o1.getPartitionNumForFree() : o1.getPartitionNum();
					int p2 = rebalanceStrategy == RebalanceStrategyType.UnassignPriority ? o2.getPartitionNumForFree() : o2.getPartitionNum();
					if (p1 != p2) {
						return p1 - p2;
					} else {
						int r1 = rebalanceStrategy == RebalanceStrategyType.UnassignPriority ? o1.getResourceNumForFree() : o1.getResourceNum();
						int r2 = rebalanceStrategy == RebalanceStrategyType.UnassignPriority ? o2.getResourceNumForFree() : o2.getResourceNum();
						if (r1 != r2) {
							return r1 - r2;
						} else {
							return o1.getInstanceName().compareTo(o2.getInstanceName());
						}
					}
				}
			}
		};
	}

	@Override
	public int hashCode() {
		return _instanceName.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder build = new StringBuilder();
		build.append("instanceName").append(":").append(this.getInstanceName()).append(",");
		build.append("totalWeight").append(":").append(this.getResourceWeight()).append(",");
		build.append("totalResource").append(":").append(this.getResourceNum()).append(",");
		build.append("totalPartition").append(":").append(this.getPartitionNum());
		return build.toString();
	}

}
