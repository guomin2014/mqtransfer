package com.gm.mqtransfer.provider.facade.common;

import com.gm.mqtransfer.provider.facade.util.NetUtils;

public class Constants {

	public static final String INSTANCE_IP = NetUtils.getInet4Address();
	public static final int INSTANCE_PID = NetUtils.getPid();
	// mark client id
	public static final String DEF_INSTANCE_ID_VAL = String.format("%s_%s", INSTANCE_IP, INSTANCE_PID);
	
	/**
     * Topic中每条消息来源，如 _REFERER: 物理集群id#topic名称 eg: mqtransfer_order#5|mqtransfer_order#3 表示消息经过的路径多个用'|'分隔
     */
    public static final String MSG_REFERER_HEADER = "_REFERER";

    public static final String MSG_REFERER_SEPARATOR = "#";

    public static final String MSG_REFERER_SOURCE_SEPARATOR = "\\|";

    public static final String MSG_REFERER_SOURCE_SEPARATOR_STR = "|";
}
