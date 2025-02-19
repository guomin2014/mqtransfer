package com.gm.mqtransfer.provider.facade.service;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractService {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public void fillEmptyPropWithDefVal(Properties props, String pname, String defVal) {
		if (!props.containsKey(pname)) {
			props.put(pname, defVal);
		}
	}
	public long getPropLongValueWithDefVal(Properties props, String pname, long defVal) {
		try {
			Object value = props.get(pname);
			if (value != null) {
				return Long.parseLong(value.toString());
			} else {
				return defVal;
			}
		} catch (Exception e) {
			return defVal;
		}
	}
	public int getPropIntValueWithDefVal(Properties props, String pname, int defVal) {
		try {
			Object value = props.get(pname);
			if (value != null) {
				return Integer.parseInt(value.toString());
			} else {
				return defVal;
			}
		} catch (Exception e) {
			return defVal;
		}
	}
	public String getPropStringValueWithDefVal(Properties props, String pname, String defVal) {
		try {
			if (!props.containsKey(pname)) {
				return defVal;
			}
			Object value = props.get(pname);
			if (value != null) {
				return value.toString();
			} else {
				return defVal;
			}
		} catch (Exception e) {
			return defVal;
		}
	}
	public boolean getPropBooleanValueWithDefVal(Properties props, String pname, boolean defVal) {
		try {
			Object value = props.get(pname);
			if (value != null) {
				return Boolean.parseBoolean(value.toString());
			} else {
				return defVal;
			}
		} catch (Exception e) {
			return defVal;
		}
	}
}
