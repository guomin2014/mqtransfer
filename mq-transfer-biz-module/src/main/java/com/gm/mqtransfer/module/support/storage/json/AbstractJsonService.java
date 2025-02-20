package com.gm.mqtransfer.module.support.storage.json;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.gm.mqtransfer.facade.common.util.FileUtil;
import com.gm.mqtransfer.facade.config.StorageConfig;
import com.gm.mqtransfer.module.support.storage.StorageService;
import com.gm.mqtransfer.provider.facade.util.StringUtils;

public abstract class AbstractJsonService<T> implements StorageService {
	
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	private String storePath;
	
	private String storeFileName;
	
	private String storeFilePath;
	
	private Type entityType;
	@Autowired
	private StorageConfig storeConfig;
	
	public AbstractJsonService() {
        Type ptype = this.getClass().getGenericSuperclass();
        if (ptype instanceof ParameterizedType) {
            Type type = ((ParameterizedType) ptype).getActualTypeArguments()[0];
            this.entityType = type;
            storeFileName = type.getTypeName() + ".json";
        }
	}
	
	public List<T> loadData() {
		storePath = storeConfig != null ? storeConfig.getFilePath() : "./";
		storeFilePath = StringUtils.appendPathSeparator(storePath) + storeFileName;
		logger.info("starting load data from json ==> {}", storeFilePath);
		try {
			FileUtil.mkdirs(storePath);
			File file = new File(storeFilePath);
			if (!file.exists()) {
				file.createNewFile();
			}
			String content = FileUtil.read(storeFilePath, "UTF-8");
			if (StringUtils.isNotBlank(content)) {
				content = content.replaceAll("\n", "").replaceAll("\r", "").replaceAll("/n", "").replaceAll("/r", "");
				if (content.endsWith(",")) {
					content = content.substring(0, content.length() - 1);
				}
				List<T> list = JSON.parseArray(content, (Class<T>)entityType);
				return list;
			} else {
				return new ArrayList<>();
			}
		} catch (Exception e) {
			logger.error("load data from json failure ==> " + storeFilePath, e);
			return new ArrayList<>();
		}
	}
	public void saveData(List<T> list) {
		try {
			FileUtil.write(storeFilePath, JSON.toJSONString(list, SerializerFeature.PrettyFormat, SerializerFeature.SortField, SerializerFeature.NotWriteDefaultValue, SerializerFeature.WriteNullStringAsEmpty), false, true, "UTF-8");
		} catch (Exception e) {
			logger.error("save data from json failure ==> " + storeFilePath, e);
		}
	}
}
