package com.gm.mqtransfer.facade.filter;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import com.gm.mqtransfer.facade.model.TransferTaskConvert;

public class ConvertFactory {

	public static Convert createConvert(TransferTaskConvert convertConfig) {
		if (convertConfig == null || !convertConfig.isEnable()) {
			return null;
		}
		ConvertMode mode = ConvertMode.getByValue(convertConfig.getMode());
		if (mode == null) {
			throw new IllegalArgumentException("Unknow mode[" + convertConfig.getMode() + "]");
		}
		switch (mode) {
		case CUSTOM:
			String handler = convertConfig.getHandler();
			try {
				Class<?> clazz = Class.forName(handler);
				if (Convert.class.isAssignableFrom(clazz)) {
					Constructor<?> constructor = clazz.getConstructor(String.class);
					Convert convert = (Convert)constructor.newInstance(convertConfig.getRule());
					return convert;
				} else {
					throw new IllegalArgumentException("Parameter[" + handler + "] is not an instance of Convert");
				}
			} catch (Exception e) {
				throw new IllegalArgumentException("Class [" + handler + "] load error", e);
			}
		}
		return null;
	}
	
	public static ConvertChain createConvert(List<TransferTaskConvert> convertConfigs) {
		if (convertConfigs == null || convertConfigs.isEmpty()) {
			return null;
		}
		List<Convert> convertList = new ArrayList<>();
		for (TransferTaskConvert config : convertConfigs) {
			Convert convert = createConvert(config);
			if (convert != null) {
				convertList.add(convert);
			}
		}
		ConvertChain convertChain = new ConvertChain(convertList);
		return convertChain;
	}
}
