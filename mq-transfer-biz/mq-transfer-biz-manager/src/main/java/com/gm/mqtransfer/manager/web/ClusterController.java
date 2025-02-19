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
import com.gm.mqtransfer.manager.core.context.model.CommonResult;
import com.gm.mqtransfer.module.cluster.model.ClusterEntity;
import com.gm.mqtransfer.module.cluster.service.ClusterService;


/**
 * <p>Title: 任务管理</p>
 * <p>Description: 任务管理-Controller实现  </p>
 * <p>Copyright: Copyright &reg;  </p>
 * <p>Company: </p>
 * @author 
 * @version 1.0.0
 */
@RestController
@RequestMapping("cluster")
public class ClusterController  {
	
	@Autowired
	private ClusterService clusterService;

	public ClusterController() {
		
	}
	
	@PostMapping(value = "list")
	public CommonResult<List<ClusterEntity>> list(@RequestBody(required=false) String data) {
		List<ClusterEntity> list = clusterService.findAll();
		CommonResult<List<ClusterEntity>> result = new CommonResult<>(list);
		return result;
	}

	@PostMapping(value = "save")
	public CommonResult<ClusterEntity> save(@RequestBody String data) {
		System.out.println(data);
		ClusterEntity entity = JSON.parseObject(data, ClusterEntity.class);
		clusterService.save(entity);
		return new CommonResult<>(entity);
	}
	@PostMapping(value = "delete")
	public CommonResult<Void> delete(@RequestBody String data) {
		System.out.println(data);
		ClusterEntity entity = JSON.parseObject(data, ClusterEntity.class);
		clusterService.delete(entity.getCode());
		return new CommonResult<>(null);
	}
	
}