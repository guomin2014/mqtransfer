package com.gm.mqtransfer.facade.handler;

/**
 * 方法回调接口
 * @author GuoMin
 * @date 2023-05-10 18:54:23
 *
 */
public interface MethodCallback {

	/**
	 * 是否结束方法调用
	 * @return
	 */
	public boolean isEndMethodCall();
	/**
	 * 是否可以打印日志
	 * @return
	 */
	public boolean isPrintLogEnable();
	/**
	 * 方法前置调用
	 * @param args
	 * @return
	 */
	public Object methodBefore(String methodName, Object ... args);
	/**
	 * 方法后置调用
	 * @param args
	 * @return
	 */
	public Object methodAfter(String methodName, Object ... args);
}
