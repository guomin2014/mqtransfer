package com.gm.mqtransfer.facade.service;

/**
 * 应用级服务，在应用启动、停止过程中调用
 * 
 * 缺陷：类加载完成后就调用，会由于某些组件还未初始化而导致服务异常，
 * 比如Kafka的连接以及订阅初始化比较靠后，在服务启动过程中就调用操作kafka相关API，将导致失败
 * 比如开启Socket监听端口，可能端口都接收到连接请求了，但数据库连接还未初始化完成，导致请求处理失败
 * 比如定时任务，任务执行时，相关缓存还未初始化，导致处理失败
 * 
 * 应用场景：
 * 1、无依赖其它模块或框架的数据初始化等操作
 * @author	GM
 * @date	2023年9月15日
 */
public interface IApplicationService extends IService
{
    /**
     * 启动服务
     */
	void start();
	/**
	 * 停止服务
	 */
	void stop();
	
	default int order() {return 9999;};
}
