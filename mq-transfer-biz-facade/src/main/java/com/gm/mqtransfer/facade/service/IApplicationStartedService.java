package com.gm.mqtransfer.facade.service;

/**
 * 应用级服务，在应用启动后、停止过程中调用
 * 应用已经完成启动完成，才调用该服务
 * 应用场景：
 * 1、应用任务，应用启动后定时或间隔执行的任务
 * 2、Socket服务端
 * @author	GM
 * @date	2023年9月15日
 */
public interface IApplicationStartedService extends IService {

    /** 启动服务 */
    void start();

    /** 停止服务 */
    void stop();
    
    /** 排序编号，启动时按从小到大顺序执行，停止时按从大到小顺序执行 */
    default int order() {return 100;};
}
