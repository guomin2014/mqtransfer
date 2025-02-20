package com.gm.mqtransfer.facade.common.util;

import com.gm.mqtransfer.provider.facade.exception.BusinessException;

public class ByteUtil {

	/**
	 * 将byte大小转换成最大单位的数据
	 * @param value
	 * @return
	 */
	public static String formatMaxUnit(long value) {
		if (value >= 1073741824) {
			return String.format("%.1fGB", value / (double)1073741824.0);
		} else if (value >= 1048576) {
			return String.format("%.1fMB", value / (double)1048576.0);
		} else if (value >= 1024) {
			return String.format("%sKB", value / 1024);
		} else {
			return value + "Byte";
		}
	}
	/**
	 * 将带单位的值转换成byte
	 * @param value
	 * @return
	 */
	public static long convert2Byte(String value) {
		try {
			value = value.trim().toUpperCase();
			if (value.endsWith("K")) {
				return Long.parseLong(value.substring(0, value.length() - 1)) * 1024;
			} else if (value.endsWith("KB")) {
				return Long.parseLong(value.substring(0, value.length() - 2)) * 1024;
			} else if (value.endsWith("M")) {
				return Long.parseLong(value.substring(0, value.length() - 1)) * 1024 * 1024;
			} else if (value.endsWith("MB")) {
				return Long.parseLong(value.substring(0, value.length() - 2)) * 1024 * 1024;
			} else if (value.endsWith("G")) {
				return Long.parseLong(value.substring(0, value.length() - 1)) * 1024 * 1024 * 1024;
			} else if (value.endsWith("GB")) {
				return Long.parseLong(value.substring(0, value.length() - 2)) * 1024 * 1024 * 1024;
			} else if (value.endsWith("BYTE")) {
				return Long.parseLong(value.substring(0, value.length() - 4));
			} else {
				return Long.parseLong(value);
			}
		} catch (Exception e) {
			throw new BusinessException("unknow value[" + value + "]");
		}
	}
}
