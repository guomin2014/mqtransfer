package com.gm.mqtransfer.facade.model;

/**
 * 告警配置
 * @author GM
 * @date 2022-07-04
 */
public class AlarmConfig {
	/** 源消费延迟告警是否启用 */
	private Boolean lagAlarmEnable;
	/** 源最大延迟告警 */
	private Long lagAlarmMaxLag;
	/** 延迟告警间隔时间 */
	private Long lagAlarmIntervalSecond;
	
	public Boolean getLagAlarmEnable() {
		return lagAlarmEnable;
	}
	public void setLagAlarmEnable(Boolean lagAlarmEnable) {
		this.lagAlarmEnable = lagAlarmEnable;
	}
	public Long getLagAlarmMaxLag() {
		return lagAlarmMaxLag;
	}
	public void setLagAlarmMaxLag(Long lagAlarmMaxLag) {
		this.lagAlarmMaxLag = lagAlarmMaxLag;
	}
	public Long getLagAlarmIntervalSecond() {
		return lagAlarmIntervalSecond;
	}
	public void setLagAlarmIntervalSecond(Long lagAlarmIntervalSecond) {
		this.lagAlarmIntervalSecond = lagAlarmIntervalSecond;
	}
}
