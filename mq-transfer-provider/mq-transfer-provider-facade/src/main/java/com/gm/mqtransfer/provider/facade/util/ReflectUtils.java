package com.gm.mqtransfer.provider.facade.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 反射工具类
 * @author GM
 * @date 2022-07-05
 */
public class ReflectUtils {

	/**
     * 根据属性名获取属性元素，包括各种安全范围和所有父类
     * 
     * @param object
     * @param fieldName
     * @return
     */
    public static Field getField(Object object, String fieldName) {
        Field field = null;
        Class<?> clazz = object.getClass();
        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;//只要找到，就返回，优先返回子类的属性
            } catch (Exception e) {
                // 这里甚么都不能抛出去。
                // 如果这里的异常打印或者往外抛，则就不会进入父类
            }
        }
        return field;
    }
    
    public static Object getFieldValue(Object object, String fieldName) {
    	Field field = getField(object, fieldName);
    	if (field == null) {
    		throw new RuntimeException("cannot find field[" + fieldName + "]");
    	}
    	field.setAccessible(true);
    	try {
    		return field.get(object);
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }
    
    public static void setFieldValue(Object object, String fieldName, Object fieldValue) {
    	Field field = getField(object, fieldName);
    	if (field == null) {
    		throw new RuntimeException("cannot find field[" + fieldName + "]");
    	}
    	field.setAccessible(true);
    	try {
    		field.set(object, fieldValue);
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }
    
    /**
     * 根据属性名获取属性元素，包括各种安全范围和所有父类
     * 
     * @param object
     * @param fieldName
     * @return
     */
    public static Method findMethod(Object object, String methodName, Class<?>... parameterTypes) {
    	Method method = null;
        Class<?> clazz = object.getClass();
        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
            	method = clazz.getDeclaredMethod(methodName, parameterTypes);
            	if (method != null) {
            		break;//只要找到，就返回，优先返回子类的属性
            	}
            } catch (Exception e) {
                // 这里甚么都不能抛出去。
                // 如果这里的异常打印或者往外抛，则就不会进入父类
            }
        }
        return method;
    }
    public static Object invokeMethod(Method method, Object target, Object... args) {
    	if (method == null) {
    		throw new RuntimeException("method is empty");
    	}
    	try {
    		method.setAccessible(true);
	    	return method.invoke(target, args);
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }
}
