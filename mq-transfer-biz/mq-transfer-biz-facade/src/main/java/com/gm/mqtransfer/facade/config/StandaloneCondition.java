package com.gm.mqtransfer.facade.config;

public class StandaloneCondition extends ModeCondition{

	/** 模式名 */
    public static String MODE_NAME = "standalone";
    
	@Override
	public String getClusterMode() {
		return MODE_NAME;
	}
}
