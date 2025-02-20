package com.gm.mqtransfer.provider.facade.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BeanUtils {

	/**
	 * 拷贝数据到新对象（单个）
	 *
	 * @param source 源实例对象
	 * @return 拷贝后的新实例对象
	 */
	public static <T> T copy(T source) {
		if (Objects.isNull(source)) {
			return null;
		}
		Class<?> c = source.getClass();
		List<Field> fields = getFields(c);
		return newInstance(source, c, fields);
	}

	/**
	 * 拷贝数据到新对象（批量）
	 *
	 * @param sourceList 源实例对象集合
	 * @return 拷贝后的新实例对象集合
	 */
	public static <T> List<T> copyList(List<T> sourceList) {
		if (Objects.isNull(sourceList) || sourceList.isEmpty()) {
			return Collections.emptyList();
		}
		Class<?> c = getClass(sourceList);
		if (Objects.isNull(c)) {
			return Collections.emptyList();
		}
		List<Field> fields = getFields(c);
		List<T> ts = new ArrayList<>();
		for (T t : sourceList) {
			T s = newInstance(t, c, fields);
			if (Objects.nonNull(s)) {
				ts.add(s);
			}
		}
		return ts;
	}

	/**
	 * 单个深度拷贝
	 *
	 * @param source 源实例化对象
	 * @param target 目标对象类（如：User.class）
	 * @return 目标实例化对象
	 */
	public static <T> T copy(Object source, Class<T> target) {
		if (Objects.isNull(source) || Objects.isNull(target)) {
			return null;
		}
		List<Field> sourceFields = getFields(source.getClass());
		List<Field> targetFields = getFields(target);
		T t = null;
		try {
			t = newInstance(source, target, sourceFields, targetFields);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return t;
	}

	/**
	 * 批量深度拷贝（如果原集合中有null,则自动忽略）
	 *
	 * @param sourceList 源实例化对象集合
	 * @param target 目标对象类（如：User.class）
	 * @return 目标实例化对象集合
	 */
	public static <T, K> List<K> copyList(List<T> sourceList, Class<K> target) {
		if (Objects.isNull(sourceList) || sourceList.isEmpty() || Objects.isNull(target)) {
			return Collections.emptyList();
		}
		Class<?> c = getClass(sourceList);
		if (Objects.isNull(c)) {
			return Collections.emptyList();
		}
		List<Field> sourceFields = getFields(c);
		List<Field> targetFields = getFields(target);
		List<K> ks = new ArrayList<>();
		for (T t : sourceList) {
			if (Objects.nonNull(t)) {
				try {
					K k = newInstance(t, target, sourceFields, targetFields);
					ks.add(k);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return ks;
	}

	/**
	 * 获取List集合中的类名
	 *
	 * @param list 对象集合
	 * @return 类名
	 */
	private static <T> Class<?> getClass(List<T> list) {
		for (T t : list) {
			if (Objects.nonNull(t)) {
				return t.getClass();
			}
		}
		return null;
	}

	/**
	 * 实例化同源对象
	 *
	 * @param source 源对象
	 * @param c 源对象类名
	 * @param fields 源对象属性集合
	 * @return 同源新对象
	 */
	@SuppressWarnings("unchecked")
	private static <T> T newInstance(T source, Class<?> sourceClass, List<Field> fields) {
		T t = null;
		try {
			t = (T)sourceClass.newInstance();
			for (Field field : fields) {
				field.setAccessible(true);
				field.set(t, field.get(source));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return t;
	}

	/**
	 * 目标实例化对象
	 *
	 * @param source 原对实例化象
	 * @param target 目标对象类
	 * @param sourceFields 源对象字段集合
	 * @param targetFields 目标对象属性字段集合
	 * @return 目标实例化对象
	 */
	private static <T> T newInstance(Object source, Class<T> target, List<Field> sourceFields, List<Field> targetFields)
			throws Exception {
		T t = target.newInstance();
		if (targetFields.isEmpty()) {
			return t;
		}
		for (Field field : sourceFields) {
			field.setAccessible(true);
			Object o = field.get(source);
			Field sameField = getSameField(field, targetFields);
			if (Objects.nonNull(sameField)) {
				sameField.setAccessible(true);
				sameField.set(t, o);
			}
		}
		return t;
	}

	/**
	 * 获取目标对象中同源对象属性相同的属性（字段名称，字段类型一致则判定为相同）
	 *
	 * @param field 源对象属性
	 * @param fields 目标对象属性集合
	 * @return 目标对象相同的属性
	 */
	private static Field getSameField(Field field, List<Field> fields) {
		String name = field.getName();
		String type = field.getType().getName();
		for (Field f : fields) {
			if (name.equals(f.getName()) && type.equals(f.getType().getName())) {
				return f;
			}
		}
		return null;
	}

	/**
	 * 获取一个类中的所有属性（包括父类属性）
	 *
	 * @param c 类名
	 * @return List<Field>
	 */
	private static List<Field> getFields(Class<?> c) {
		List<Field> fieldList = new ArrayList<>();
		Field[] fields = c.getDeclaredFields();
		if (fields.length > 0) {
			fieldList.addAll(Arrays.asList(fields));
		}
		return getSuperClassFields(c, fieldList);
	}

	/**
	 * 递归获取父类属性
	 *
	 * @param o 类名
	 * @param allFields 外层定义的所有属性集合
	 * @return 父类所有属性
	 */
	private static List<Field> getSuperClassFields(Class<?> o, List<Field> allFields) {
		Class<?> superclass = o.getSuperclass();
		if (Objects.isNull(superclass) || Object.class.getName().equals(superclass.getName())) {
			return allFields;
		}
		Field[] fields = superclass.getDeclaredFields();
		if (fields.length == 0) {
			return allFields;
		}
		allFields.addAll(Arrays.asList(fields));
		return getSuperClassFields(superclass, allFields);
	}

}