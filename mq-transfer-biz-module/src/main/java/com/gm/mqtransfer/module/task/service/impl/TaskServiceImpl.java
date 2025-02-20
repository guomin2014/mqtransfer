/**
* 文件：TaskServiceImpl.java
* 版本：1.0.0
* 日期：2024-05-28
* Copyright &reg; 
* All right reserved.
*/
package com.gm.mqtransfer.module.task.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gm.mqtransfer.facade.common.TaskStatusEnum;
import com.gm.mqtransfer.facade.common.util.DataUtil;
import com.gm.mqtransfer.facade.exception.BusinessException;
import com.gm.mqtransfer.facade.model.StorageConfigScope;
import com.gm.mqtransfer.facade.model.StorageConfigScope.ConfigScopeProperty;
import com.gm.mqtransfer.facade.model.StorageConfigScopeBuilder;
import com.gm.mqtransfer.facade.model.TaskPartitionStatInfo;
import com.gm.mqtransfer.facade.model.TaskStatInfo;
import com.gm.mqtransfer.module.support.EventType;
import com.gm.mqtransfer.module.support.storage.StorageService;
import com.gm.mqtransfer.module.support.storage.listener.StorageChangeEvent;
import com.gm.mqtransfer.module.support.storage.listener.StorageChildDataChangeListener;
import com.gm.mqtransfer.module.task.listener.TaskChangeEvent;
import com.gm.mqtransfer.module.task.listener.TaskChangeListener;
import com.gm.mqtransfer.module.task.model.TaskEntity;
import com.gm.mqtransfer.module.task.model.TaskMigrationStatus;
import com.gm.mqtransfer.module.task.service.TaskService;
import com.gm.mqtransfer.provider.facade.model.TopicPartitionInfo;
import com.gm.mqtransfer.provider.facade.model.TopicPartitionMapping;
import com.gm.mqtransfer.provider.facade.util.PartitionUtils;


/**
 * <p>Title: 任务管理</p>
 * <p>Description: 任务管理-ServiceImpl实现  </p>
 * <p>Copyright: Copyright &reg;  </p>
 * <p>Company: </p>
 * @author 
 * @version 1.0.0
 */
@Service("taskService")
public class TaskServiceImpl implements TaskService {

	@Autowired
	private StorageService storageService;
	
	@Override
	public void addListener(TaskChangeListener listener) {
		storageService.addListener(new StorageConfigScopeBuilder(ConfigScopeProperty.RESOURCE).build(), new StorageChildDataChangeListener() {
			@Override
			public void onChange(StorageChangeEvent event) {
				TaskChangeEvent changeEvent = null;
				StorageChangeEvent.Type type = event.getType();
				String data = event.getData();
				TaskEntity entity = JSON.parseObject(data, TaskEntity.class);
				switch (type) {
				case CHILD_ADDED:
					changeEvent = new TaskChangeEvent(EventType.ADDED, entity);
					break;
				case CHILD_UPDATED:
					changeEvent = new TaskChangeEvent(EventType.UPDATED, entity);
					break;
				case CHILD_REMOVED:
					changeEvent = new TaskChangeEvent(EventType.REMOVED, entity);
					break;
				}
				listener.onChange(changeEvent);
			}});
	}
	
	@Override
	public List<TaskEntity> findAllEnable() {
		return this.findAll();
	}

	@Override
	public List<TaskEntity> findAll() {
		return storageService.getChildrenList(new StorageConfigScopeBuilder(ConfigScopeProperty.RESOURCE).build(), TaskEntity.class);
	}

	@Override
	public TaskEntity get(String code) {
		return storageService.getData(new StorageConfigScopeBuilder(ConfigScopeProperty.RESOURCE).forResource(code).build(), TaskEntity.class);
	}

	@Override
	public void save(TaskEntity entity) {
		try {
			TaskEntity oldTask = this.get(entity.getCode());
			if (oldTask != null) {
				entity.setStatus(oldTask.getStatus());//不更新状态字段
			} else {
				entity.setStatus(TaskStatusEnum.DISABLE.getValue());
			}
			storageService.saveData(new StorageConfigScopeBuilder(ConfigScopeProperty.RESOURCE).forResource(entity.getCode()).build(), entity);
		} catch (Exception e) {
			throw new BusinessException(e);
		}
	}

	@Override
	public void delete(String code) {
		try {
			storageService.removeData(new StorageConfigScopeBuilder(ConfigScopeProperty.RESOURCE).forResource(code).build());
		} catch (Exception e) {
			throw new BusinessException(e);
		}
	}

	@Override
	public void updateTaskStatus(String code, Integer status) {
		//更新任务状态
		TaskEntity oldTask = this.get(code);
		if (oldTask == null) {
			throw new BusinessException("No record found");
		}
		oldTask.setStatus(status);
		try {
			storageService.saveData(new StorageConfigScopeBuilder(ConfigScopeProperty.RESOURCE).forResource(code).build(), oldTask);
		} catch (Exception e) {
			throw new BusinessException(e);
		}
	}

	@Override
	public TaskStatInfo getStatInfo(String code) {
		TaskStatInfo retStat = new TaskStatInfo();
		try {
			StorageConfigScope statScope = new StorageConfigScopeBuilder(ConfigScopeProperty.STAT)
					.forResource(code)
					.build();
			try {
				TaskStatInfo stat = storageService.getData(statScope, TaskStatInfo.class);
				if (stat != null) {
					BeanUtils.copyProperties(stat, retStat);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			List<TaskPartitionStatInfo> partitionStatList = storageService.getChildrenList(statScope, TaskPartitionStatInfo.class);
			long totalConsumerCount = 0L;
			long totalFilterCount = 0L;
			long totalProducerCount = 0L;
			long totalCacheQueueCount = 0L;
			long totalConsumerLag = 0L;
			long totalProducerLag = 0L;
			for (TaskPartitionStatInfo partitionStat : partitionStatList) {
				totalConsumerCount += partitionStat.getTotalConsumerCount() != null ? partitionStat.getTotalConsumerCount() : 0;
				totalFilterCount += partitionStat.getTotalFilterCount() != null ? partitionStat.getTotalFilterCount() : 0;
				totalProducerCount += partitionStat.getTotalProducerCount() != null ? partitionStat.getTotalProducerCount() : 0;
				totalCacheQueueCount += partitionStat.getCacheQueueSize() != null ? partitionStat.getCacheQueueSize() : 0;
				totalConsumerLag += partitionStat.getConsumerLag() != null ? partitionStat.getConsumerLag() : 0;
				totalProducerLag += partitionStat.getProducerLag() != null ? partitionStat.getProducerLag() : 0;
			}
			retStat.setTotalConsumerCount(totalConsumerCount);
			retStat.setTotalConsumerLag(totalConsumerLag);
			retStat.setTotalFilterCount(totalFilterCount);
			retStat.setTotalCacheQueueCount(totalCacheQueueCount);
			retStat.setTotalProducerCount(totalProducerCount);
			retStat.setTotalProducerLag(totalProducerLag);
			retStat.setPartitionStatList(partitionStatList);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retStat;
	}

	@Override
	public void updatePartitionMapping(TaskEntity task, List<TopicPartitionMapping> allocateList) {
		for (TopicPartitionMapping mapping : allocateList) {
			TopicPartitionInfo fromPartition = mapping.getFromPartition();
			TopicPartitionInfo toPartition = mapping.getToPartition();
			StorageConfigScope partitionScope = new StorageConfigScopeBuilder(ConfigScopeProperty.PARTITION)
					.forResource(task.getCode())
					.forPartition(fromPartition.getPartitionKey())
					.build();
			try {
				storageService.saveData(partitionScope, toPartition.getPartitionKey());
			} catch (Exception e) {
				e.printStackTrace();
			}
			String partitionKey = fromPartition.getPartitionKey() + PartitionUtils.KEY_OUTER_SPLIT + toPartition.getPartitionKey();
			StorageConfigScope statScope = new StorageConfigScopeBuilder(ConfigScopeProperty.STAT)
					.forResource(task.getCode())
					.forPartition(partitionKey)
					.build();
			JSONObject data = new JSONObject();
			data.put("sourcePartition", fromPartition.getPartitionKey());
			data.put("targetPartition", toPartition.getPartitionKey());
			try {
				storageService.saveData(statScope, data);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public List<TopicPartitionMapping> getPartitionMapping(String code) {
		List<TopicPartitionMapping> list = new ArrayList<>();
		StorageConfigScope partitionScope = new StorageConfigScopeBuilder(ConfigScopeProperty.PARTITION)
				.forResource(code)
				.build();
		try {
			List<String> resourceList = storageService.getChildrenList(partitionScope, String.class);
			for (String resourceName : resourceList) {
				try {
					StorageConfigScope partitionMappingScope = new StorageConfigScopeBuilder(ConfigScopeProperty.PARTITION)
							.forResource(code)
							.forPartition(resourceName)
							.build();
					String value = storageService.getData(partitionMappingScope, String.class);
					TopicPartitionMapping mapping = new TopicPartitionMapping();
					mapping.setFromPartition(PartitionUtils.parsePartitionKey(resourceName));
					mapping.setToPartition(PartitionUtils.parsePartitionKey(value));
					list.add(mapping);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	@Override
	public TopicPartitionMapping getPartitionMapping(String code, String fromPartitionKey) {
		StorageConfigScope partitionScope = new StorageConfigScopeBuilder(ConfigScopeProperty.PARTITION)
				.forResource(code)
				.forPartition(fromPartitionKey)
				.build();
		String value = storageService.getData(partitionScope, String.class);
		TopicPartitionMapping mapping = new TopicPartitionMapping();
		mapping.setFromPartition(PartitionUtils.parsePartitionKey(fromPartitionKey));
		mapping.setToPartition(PartitionUtils.parsePartitionKey(value));
		return mapping;
	}

	@Override
	public void refreshMemoryStat(String code, Long maxAvailableMemory, Long totalUsedMemory) {
		StorageConfigScope lockScope = new StorageConfigScopeBuilder(ConfigScopeProperty.LOCK).forResource(code).build();
		if (storageService.tryLock(lockScope, 5000L, TimeUnit.MILLISECONDS)) {
			try {
				StorageConfigScope statScope = new StorageConfigScopeBuilder(ConfigScopeProperty.STAT).forResource(code).build();
				TaskStatInfo oldData = null;
				try {
					oldData = storageService.getData(statScope, TaskStatInfo.class);
				} catch (Exception e) {
					
				}
				if (oldData == null) {
					oldData = new TaskStatInfo();
				}
				oldData.setMaxAvailableMemory(maxAvailableMemory);
				oldData.setTotalUsedMemory(totalUsedMemory);
				storageService.saveData(statScope, oldData);
			} catch (Exception e) {
				throw new BusinessException("Update task occupying memory data failed", e);
			} finally {
				storageService.releaseLock(lockScope);
			}
		} else {
			throw new BusinessException("资源节点已经被其它任务占用");
		}
		
	}

	@Override
	public void refreshConsumerStat(String code, TaskPartitionStatInfo data) {
		String partitionName = data.getSourcePartition() + PartitionUtils.KEY_OUTER_SPLIT + data.getTargetPartition();
		String lockResourceName = code + PartitionUtils.KEY_OUTER_SPLIT + partitionName;
		StorageConfigScope lockScope = new StorageConfigScopeBuilder(ConfigScopeProperty.LOCK).forResource(lockResourceName).build();
		if (storageService.tryLock(lockScope, 5000L, TimeUnit.MILLISECONDS)) {
			try {
				StorageConfigScope statScope = new StorageConfigScopeBuilder(ConfigScopeProperty.STAT).forResource(code).forPartition(partitionName).build();
				TaskPartitionStatInfo oldData = null;
				try {
					oldData = storageService.getData(statScope, TaskPartitionStatInfo.class);
				} catch (Exception e) {
					
				}
				if (oldData == null) {
					oldData = new TaskPartitionStatInfo();
				}
				if (StringUtils.isBlank(oldData.getSourcePartition())) {
					oldData.setSourcePartition(data.getSourcePartition());
				}
				if (StringUtils.isBlank(oldData.getTargetPartition())) {
					oldData.setTargetPartition(data.getTargetPartition());
				}
				if (data.getMinOffset() != null) {
					oldData.setMinOffset(data.getMinOffset());
					if (oldData.getInitMinOffset() == null) {
						oldData.setInitMinOffset(data.getMinOffset());
					}
				}
				oldData.setMaxOffset(data.getMaxOffset());
				oldData.setCacheQueueSize(data.getCacheQueueSize());
				oldData.setCacheDataSize(data.getCacheDataSize());
				oldData.setConsumerOffset(data.getConsumerOffset());
				oldData.setConsumerLag(data.getConsumerLag());
				oldData.setTotalConsumerCount(DataUtil.conver2Long(oldData.getTotalConsumerCount()) + DataUtil.conver2Long(data.getTotalConsumerCount()));
				oldData.setTotalConsumerDataSize(DataUtil.conver2Long(oldData.getTotalConsumerDataSize()) + DataUtil.conver2Long(data.getTotalConsumerDataSize()));
				oldData.setLastConsumerTime(data.getLastConsumerTime());
				storageService.saveData(statScope, oldData);
			} catch (Exception e) {
				throw new BusinessException("Update task consumer data failed", e);
			} finally {
				storageService.releaseLock(lockScope);
			}
		} else {
			throw new BusinessException("资源节点已经被其它任务占用");
		}
	}

	@Override
	public void refreshProducerStat(String code, TaskPartitionStatInfo data) {
		String partitionName = data.getSourcePartition() + PartitionUtils.KEY_OUTER_SPLIT + data.getTargetPartition();
		String lockResourceName = code + PartitionUtils.KEY_OUTER_SPLIT + partitionName;
		StorageConfigScope lockScope = new StorageConfigScopeBuilder(ConfigScopeProperty.LOCK).forResource(lockResourceName).build();
		if (storageService.tryLock(lockScope, 5000L, TimeUnit.MILLISECONDS)) {
			try {
				StorageConfigScope statScope = new StorageConfigScopeBuilder(ConfigScopeProperty.STAT).forResource(code).forPartition(partitionName).build();
				TaskPartitionStatInfo oldData = null;
				try {
					oldData = storageService.getData(statScope, TaskPartitionStatInfo.class);
				} catch (Exception e) {
					
				}
				if (oldData == null) {
					oldData = new TaskPartitionStatInfo();
				}
				if (StringUtils.isBlank(oldData.getSourcePartition())) {
					oldData.setSourcePartition(data.getSourcePartition());
				}
				if (StringUtils.isBlank(oldData.getTargetPartition())) {
					oldData.setTargetPartition(data.getTargetPartition());
				}
				if (data.getCommitOffset() != null) {
					oldData.setCommitOffset(data.getCommitOffset());
				}
				oldData.setProducerLag(data.getProducerLag());
				oldData.setTotalFilterCount(DataUtil.conver2Long(oldData.getTotalFilterCount()) + DataUtil.conver2Long(data.getTotalFilterCount()));
				oldData.setTotalFilterDataSize(DataUtil.conver2Long(oldData.getTotalFilterDataSize()) + DataUtil.conver2Long(data.getTotalFilterDataSize()));
				oldData.setTotalProducerCount(DataUtil.conver2Long(oldData.getTotalProducerCount()) + DataUtil.conver2Long(data.getTotalProducerCount()));
				oldData.setTotalProducerDataSize(DataUtil.conver2Long(oldData.getTotalProducerDataSize()) + DataUtil.conver2Long(data.getTotalProducerDataSize()));
				oldData.setLastProducerTime(data.getLastProducerTime());
				storageService.saveData(statScope, oldData);
			} catch (Exception e) {
				throw new BusinessException("Update task consumer data failed", e);
			} finally {
				storageService.releaseLock(lockScope);
			}
		} else {
			throw new BusinessException("资源节点已经被其它任务占用");
		}
	}

	@Override
	public void watchPartitionMigration(String code, String partitionKey, String originalInstanceName, String newlInstanceName) {
		// TODO Auto-generated method stub
		
	}
	
	public boolean markPartitionMigration(String code, String partitionKey) {
		StorageConfigScope lockScope = new StorageConfigScopeBuilder(ConfigScopeProperty.MIGRATION).forResource(code).forPartition(partitionKey).build();
		return storageService.createEphemeralPath(lockScope.getZkPath(), TaskMigrationStatus.WAIT.name());
	}

	@Override
	public void tryCompletePartitionMigration(String code, String partitionKey) {
		this.updatePartitionMigration(code, partitionKey, TaskMigrationStatus.COMPLETED);
	}

	@Override
	public boolean updatePartitionMigration(String code, String partitionKey, TaskMigrationStatus status) {
		StorageConfigScope lockScope = new StorageConfigScopeBuilder(ConfigScopeProperty.MIGRATION).forResource(code).forPartition(partitionKey).build();
		try {
			if (status == TaskMigrationStatus.COMPLETED) {
				storageService.removeData(lockScope);
			} else {
				storageService.saveData(lockScope, status.name());
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public TaskMigrationStatus getPartitionMigrationStatus(String code, String partitionKey) {
		StorageConfigScope lockScope = new StorageConfigScopeBuilder(ConfigScopeProperty.MIGRATION).forResource(code).forPartition(partitionKey).build();
		String data = storageService.getData(lockScope, String.class);
		if (StringUtils.isBlank(data)) {
			return null;
		}
		return TaskMigrationStatus.getByName(data);
	}

}