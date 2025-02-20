package com.gm.mqtransfer.facade.config;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;

import com.gm.mqtransfer.facade.common.util.FileUtil;

public abstract class AbstractConfig {

	private String configPrefix;
	private String configFilePath;
	
	public AbstractConfig(String configFilePath, String configPrefix) {
		this.configFilePath = configFilePath;
		this.configPrefix = configPrefix;
	}
	
	public void loadContent() {
        try {
			InputStream is = FileUtil.getReadInputStream(configFilePath);
			Properties prop = new Properties();
			prop.load(is);
			Field[] fields = this.getClass().getDeclaredFields();
			for (Field field : fields) {
				String key = configPrefix + field.getName();
				if (prop.containsKey(key)) {
					String value = prop.getProperty(key);
					field.setAccessible(true);
					Class<?> type = field.getType();
					if (type == String.class) {
						field.set(this, value);
					} else if (type == Integer.TYPE || type == int.class) {
						field.setInt(this, Integer.parseInt(value));
					} else if (type == Long.TYPE || type == long.class) {
						field.setLong(this, Long.parseLong(value));
					} else if (type == Double.TYPE || type == double.class) {
						field.setDouble(this, Double.parseDouble(value));
					} else if (type == Float.TYPE || type == float.class) {
						field.setFloat(this, Float.parseFloat(value));
					} else if (type == Boolean.TYPE || type == boolean.class) {
						field.setBoolean(this, Boolean.parseBoolean(value));
					}
				}
			}
        } catch (Exception e) {
            throw new RuntimeException("load file failure: " + configFilePath, e);
        }
	}
}
