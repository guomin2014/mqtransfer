/**
* 文件：ClusterService.java
* 版本：1.0.0
* 日期：2024-05-28
* Copyright &reg; 
* All right reserved.
*/
package com.gm.mqtransfer.module.cluster.service;

import java.util.List;

import com.gm.mqtransfer.module.cluster.model.ClusterEntity;

/**
 * <p>Title: 集群管理</p>
 * <p>Description: 集群管理-Service接口  </p>
 * <p>Copyright: Copyright &reg;  </p>
 * <p>Company: </p>
 * @author 
 * @version 1.0.0
 */

public interface ClusterService {

	/**
	 * 查询所有可用集群
	 * @return
	 */
	List<ClusterEntity> findAllEnable();
	/**
	 * 查询所有集群（包含停用的集群）
	 * @return
	 */
	List<ClusterEntity> findAll();
	
	ClusterEntity get(String code);
	
	void save(ClusterEntity entity);

	void delete(String code);
}