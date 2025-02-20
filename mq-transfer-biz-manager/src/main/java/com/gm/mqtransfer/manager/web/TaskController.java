/**
* 文件：TaskController.java
* 版本：1.0.0
* 日期：2024-05-28
* Copyright &reg; 
* All right reserved.
*/
package com.gm.mqtransfer.manager.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.gm.mqtransfer.facade.common.TaskStatusEnum;
import com.gm.mqtransfer.facade.model.TaskStatInfo;
import com.gm.mqtransfer.manager.core.context.model.CommonResult;
import com.gm.mqtransfer.module.task.model.TaskEntity;
import com.gm.mqtransfer.module.task.service.TaskService;


/**
 * <p>Title: 任务管理</p>
 * <p>Description: 任务管理-Controller实现  </p>
 * <p>Copyright: Copyright &reg;  </p>
 * <p>Company: </p>
 * @author 
 * @version 1.0.0
 */
@RestController
@RequestMapping("task")
public class TaskController  {

	@Autowired
	private TaskService taskService;
	
	public TaskController() {
		
	}

	@PostMapping(value = "list")
	public CommonResult<List<TaskEntity>> list(@RequestBody(required=false) String data) {
		List<TaskEntity> list = taskService.findAll();
		CommonResult<List<TaskEntity>> result = new CommonResult<>(list);
		return result;
	}

	@PostMapping(value = "save")
	public CommonResult<TaskEntity> save(@RequestBody String data) {
		TaskEntity entity = JSON.parseObject(data, TaskEntity.class);
		taskService.save(entity);
		return new CommonResult<TaskEntity>(entity);
	}
	
	@PostMapping(value = "delete")
	public CommonResult<Void> delete(@RequestBody String data) {
		TaskEntity entity = JSON.parseObject(data, TaskEntity.class);
		taskService.delete(entity.getCode());
		return new CommonResult<>(null);
	}
	@PostMapping(value = "start")
	public CommonResult<Void> start(@RequestBody String data) {
		TaskEntity entity = JSON.parseObject(data, TaskEntity.class);
		taskService.updateTaskStatus(entity.getCode(), TaskStatusEnum.ENABLE.getValue());
		return new CommonResult<>(null);
	}
	@PostMapping(value = "stop")
	public CommonResult<Void> stop(@RequestBody String data) {
		TaskEntity entity = JSON.parseObject(data, TaskEntity.class);
		taskService.updateTaskStatus(entity.getCode(), TaskStatusEnum.DISABLE.getValue());
		return new CommonResult<>(null);
	}
	@PostMapping(value = "monitor")
	public CommonResult<TaskStatInfo> monitor(@RequestBody String data) {
		TaskEntity entity = JSON.parseObject(data, TaskEntity.class);
		TaskStatInfo statInfo = taskService.getStatInfo(entity.getCode());
		if (statInfo == null) {
			statInfo = new TaskStatInfo();
		}
		return new CommonResult<>(statInfo);
	}
}