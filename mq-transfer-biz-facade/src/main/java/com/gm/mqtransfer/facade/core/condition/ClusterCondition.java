package com.gm.mqtransfer.facade.core.condition;

public class ClusterCondition extends ModeCondition{

	/** 模式名 */
    public static String MODE_NAME = "cluster";
    
	@Override
	public String getClusterMode() {
		return MODE_NAME;
	}

}
