package com.gm.mqtransfer.facade.common.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringTemplate {
	private static Logger LOG = LoggerFactory.getLogger(StringTemplate.class);

	Map<Enum, Map<Integer, String>> templateMap = new HashMap<Enum, Map<Integer, String>>();
	static Pattern pattern = Pattern.compile("(\\{.+?\\})");

	public void addEntry(Enum type, int numKeys, String template) {
		if (!templateMap.containsKey(type)) {
			templateMap.put(type, new HashMap<Integer, String>());
		}
		LOG.trace("Add template for type: " + type.name() + ", arguments: " + numKeys + ", template: " + template);
		templateMap.get(type).put(numKeys, template);
	}
	
	public boolean containsEntryTemplate(Enum type, int numKeys) {
		Map<Integer, String> map = templateMap.get(type);
		if (map == null) {
			return false;
		} else {
			return map.containsKey(numKeys);
		}
	}
	
	public Set<Integer> getEntryNumKeys(Enum type) {
		Map<Integer, String> map = templateMap.get(type);
		if (map == null) {
			return new HashSet<>();
		} else {
			return new HashSet<>(map.keySet());
		}
	}

	public String instantiate(Enum type, String... keys) {
		if (keys == null) {
			keys = new String[] {};
		}

		String template = null;
		if (templateMap.containsKey(type)) {
			template = templateMap.get(type).get(keys.length);
		}

		String result = null;

		if (template != null) {
			result = template;
			Matcher matcher = pattern.matcher(template);
			int count = 0;
			while (matcher.find()) {
				String var = matcher.group();
				result = result.replace(var, keys[count]);
				count++;
			}
		}

		if (result == null || result.indexOf('{') > -1 || result.indexOf('}') > -1) {
			String errMsg = "Unable to instantiate template: " + template + " using keys: " + Arrays.toString(keys);
			LOG.error(errMsg);
			throw new IllegalArgumentException(errMsg);
		}

		return result;
	}
}
