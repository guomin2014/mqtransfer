package com.gm.mqtransfer.facade.common.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

public final class EntityUtil {

	private static Logger logger = LoggerFactory.getLogger(EntityUtil.class);
	/** 需要过滤的属性名称 */
	private static final Set<String> filterProperties = new HashSet<>();
	
	static {
	    filterProperties.add("class");
	    filterProperties.add("orderCols");
	    filterProperties.add("notSelectCols");
	    filterProperties.add("symbolCols");
	    filterProperties.add("vague");
	}
	
    public static final Map entityToMap(Object entity, Map map) {
        if (entity == null) {
            return map;
        }
        try {
            Class<?> cls = entity.getClass();
            BeanInfo beanInfo = Introspector.getBeanInfo(cls);
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor descriptor : propertyDescriptors) {
                String propertyName = descriptor.getName();
                if (filterProperties.contains(propertyName)) {
                    continue;
                }
                Method readMethod = descriptor.getReadMethod();
                Object result = readMethod.invoke(entity, new Object[0]);
                if (result != null) {
                    if (result instanceof Iterable) {
                        result = doRecursionConvertIterable((Iterable)result);
                    } else if (result instanceof Map) {
                        result = doRecursionConvertMap((Map)result);
                    } else if (result.getClass().isArray()) {
                        result = doRecursionConvertArray(result);
                    }
                    map.put(propertyName, result);
                }
            }
        } catch (IntrospectionException e) {
        	logger.warn("将对象转换成Map异常-->" + entity + "-->" + e.getMessage());
        } catch (IllegalAccessException e) {
        	logger.warn("将对象转换成Map异常-->" + entity + "-->" + e.getMessage());
        } catch (InvocationTargetException e) {
        	logger.warn("将对象转换成Map异常-->" + entity + "-->" + e.getMessage());
        } catch (Exception e) {
            logger.warn("将对象转换成Map异常-->" + entity + "-->" + e.getMessage());
        }
        return map;
    }
    
    public static final Map<String, String> entityToMapWithString(Object entity) {
    	Map<String, String> map = new HashMap<>();
    	if (entity == null) {
            return map;
        }
        try {
            Class<?> cls = entity.getClass();
            BeanInfo beanInfo = Introspector.getBeanInfo(cls);
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor descriptor : propertyDescriptors) {
                String propertyName = descriptor.getName();
                if (filterProperties.contains(propertyName)) {
                    continue;
                }
                Method readMethod = descriptor.getReadMethod();
                Object result = readMethod.invoke(entity, new Object[0]);
                if (result != null) {
                    if (result instanceof Iterable) {
                        result = JSON.toJSONString(doRecursionConvertIterable((Iterable)result));
                    } else if (result instanceof Map) {
                        result = JSON.toJSONString(doRecursionConvertMap((Map)result));
                    } else if (result.getClass().isArray()) {
                        result = JSON.toJSONString(doRecursionConvertArray(result));
                    } else if (!(result instanceof Integer || result instanceof Long || result instanceof Double || result instanceof Float || result instanceof String)) {
                        result = JSON.toJSONString(result);
                    }
                    map.put(propertyName, result.toString());
                }
            }
        } catch (IntrospectionException e) {
        	logger.warn("将对象转换成Map异常-->" + entity + "-->" + e.getMessage());
        } catch (IllegalAccessException e) {
        	logger.warn("将对象转换成Map异常-->" + entity + "-->" + e.getMessage());
        } catch (InvocationTargetException e) {
        	logger.warn("将对象转换成Map异常-->" + entity + "-->" + e.getMessage());
        } catch (Exception e) {
            logger.warn("将对象转换成Map异常-->" + entity + "-->" + e.getMessage());
        }
        return map;
    }

    public static final <T> T mapToEntity(Map map, T entity) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(entity.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (int i = 0; i < propertyDescriptors.length; i++) {
                PropertyDescriptor descriptor = propertyDescriptors[i];
                String propertyName = descriptor.getName();
                if (map.containsKey(propertyName) && descriptor.getWriteMethod() != null) {
                    Object value = map.get(propertyName);
                    Object[] args = new Object[1];
                    args[0] = value;
                    descriptor.getWriteMethod().invoke(entity, args);
                }
            }
        } catch (IntrospectionException e) {
        	logger.warn("将Map转换成对象异常-->" + entity + "-->" + e.getMessage());
        } catch (IllegalArgumentException e) {
        	logger.warn("将Map转换成对象异常-->" + entity + "-->" + e.getMessage());
        } catch (IllegalAccessException e) {
        	logger.warn("将Map转换成对象异常-->" + entity + "-->" + e.getMessage());
        } catch (InvocationTargetException e) {
        	logger.warn("将Map转换成对象异常-->" + entity + "-->" + e.getMessage());
        }
        return entity;
    }
    /**
     * 获取实体类名（包含所有父级）
     * @param obj
     * @return
     */
    private static Set<String> getEntityAllClassName(Object obj) {
        Set<String> set = new HashSet<>();
        if (obj != null) {
            Class<?> cls = obj.getClass();
            set.add(cls.getSimpleName());
            Class<?> superCls = null;
            while ((superCls = cls.getSuperclass()) != null) {
                set.add(superCls.getSimpleName());
                cls = superCls;
            }
        }
        return set;
    }
    /**
     * 检查对象是否可以转换成Map（只将继承框架BaseEntity的类转换成Map）
     * @param obj
     * @return
     */
    private static final boolean enableToMap(Object obj) {
        if (obj == null) {
            return false;
        }
        Set<String> classNames = getEntityAllClassName(obj);
        return classNames.contains("BaseEntity");//是框架的基类，则递归转换
    }
    /**
     * 递归转换可叠代对象
     * @param it
     * @return
     */
    private static Object doRecursionConvertIterable(Iterable it) {
        if (it == null) {
            return null;
        }
        Iterator iterator = it.iterator();
        boolean recursion = false;
        while (iterator.hasNext()) {
            Object first = iterator.next();
            if (first != null) {
                recursion = enableToMap(first);
                break;
            }
        }
        if (recursion) {
            List list = new ArrayList();
            for (Object obj : it) {
                if (obj == null) {
                    continue;
                }
                Map childMap = new HashMap();
                childMap = entityToMap(obj, childMap);
                list.add(childMap);
            }
            return list;
        }
        return it;
    }
    /**
     * 递归转换Map对象
     * @param map
     * @return
     */
    private static Object doRecursionConvertMap(Map<Object, Object> map) {
        if (map == null || map.isEmpty()) {
            return map;
        }
        if (map.size() > 0) {
            boolean recursion = false;
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                Object value = entry.getValue();
                if (value != null) {
                    recursion = enableToMap(value);
                    break;
                }
            }
            if (recursion) {
                Map<Object, Object> resultMap = new HashMap<>();
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    Map childMap = new HashMap();
                    childMap = entityToMap(value, childMap);
                    resultMap.put(key, childMap);
                }
                return resultMap;
            }
        }
        return map;
    }
    /**
     * 递归转换Array对象
     * @param map
     * @return
     */
    private static Object doRecursionConvertArray(Object result) {
        if (result == null) {
            return result;
        }
        int len = Array.getLength(result);
        if (len > 0) {
            boolean recursion = false;
            for (int i = 0; i < len; i++) {
                Object obj = Array.get(result, i);
                if (obj != null) {
                    recursion = enableToMap(obj);
                    break;
                }
            }
            if (recursion) {
                List resultList = new ArrayList();
                for (int i = 0; i < len; i++) {
                    Object obj = Array.get(result, i);
                    if (obj == null) {
                        continue;
                    }
                    Map childMap = new HashMap();
                    childMap = entityToMap(obj, childMap);
                    resultList.add(childMap);
                }
                return resultList;
            }
        }
        return result;
    }
    /**
     * 获取实体属性列表(包含父类的属性)
     * @param entity
     * @return
     */
    public static List<String> entityPropertiesToList(Object entity) {
    	if (entity == null) {
            return new ArrayList<>();
        }
    	Set<String> props = new TreeSet<>();
        try {
            Class<?> cls = entity instanceof Class ? (Class<?>) entity : entity.getClass();
            while(cls != null && !cls.getSimpleName().equalsIgnoreCase("Object")) {
	            BeanInfo beanInfo = Introspector.getBeanInfo(cls);
	            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
	            for (PropertyDescriptor descriptor : propertyDescriptors) {
	                String propertyName = descriptor.getName();
	                if (filterProperties.contains(propertyName)) {
	                    continue;
	                }
	                if (props.contains(propertyName)) {
	                	continue;
	                }
	                props.add(propertyName);
	            }
	            cls = cls.getSuperclass();
            }
        } catch (Exception e) {
            logger.warn("获取对象属性异常-->" + entity + "-->" + e.getMessage());
        }
        return new ArrayList<>(props);
    }
}
