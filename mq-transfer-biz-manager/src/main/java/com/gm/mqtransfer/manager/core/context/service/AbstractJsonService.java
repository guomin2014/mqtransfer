package com.gm.mqtransfer.manager.core.context.service;

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
import com.gm.mqtransfer.manager.config.StoreConfig;
import com.gm.mqtransfer.provider.facade.util.StringUtils;

public class AbstractJsonService<T> {
	
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	private String storePath;
	
	private String storeFileName;
	
	private String storeFilePath;
	
	private Type entityType;
	@Autowired
	private StoreConfig storeConfig;
	
	public AbstractJsonService() {
		// 获取当前实例的泛型的真实类类型
        Type ptype = this.getClass().getGenericSuperclass();
        if (ptype instanceof ParameterizedType) {
            // 获取本类泛型TF的运行时实际类类型
            Type type = ((ParameterizedType) ptype).getActualTypeArguments()[0];// 0代表当前类的第一个泛型参数，即T的运行时类型
            this.entityType = type;
//            System.out.println("***********" + type.getTypeName());
            storeFileName = type.getTypeName() + ".json";
        }
	}
	
	public List<T> loadData() {
		storePath = storeConfig != null ? storeConfig.getPath() : "./";
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
