package com.gm.mqtransfer.facade.common.util;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import com.gm.mqtransfer.facade.exception.BusinessException;
/**
  * 反射工具类.
  * 
  * 提供访问私有变量,获取泛型类型Class, 提取集合中元素的属性, 转换字符串到对象等Util函数.
  * 
  * 使用apache的BeanUtils有缺陷，详见：https://segmentfault.com/a/1190000019356477?utm_source=tag-newest
  * 
  * 1、性能缺陷
  * 2、其它缺陷
  * 在进行属性拷贝时，虽然 CommonsBeanUtils 默认不会给原始包装类赋默认值的，但是在使用低版本(1.8.0及以下)的时候，如果你的类有 Date 类型属性，而且来源对象中该属性值为 null 的话，就会发生异常：
  * org.apache.commons.beanutils.ConversionException: No value specified for 'Date'
  * 解决这个问题的办法是注册一个 DateConverter：
  * ConvertUtils.register(new DateConverter(null), java.util.Date.class);
  * 然而这个语句，会导致包装类型会被赋予原始类型的默认值，如 Integer 属性默认赋值为 0，尽管你的来源对象该字段的值为 null。
  * 在高版本(1.9.3)中，日期 null 值的问题和包装类赋默认值的问题都被修复了。
  * 这个在我们的包装类属性为 null 值时有特殊含义的场景，非常容易踩坑！例如搜索条件对象，一般 null 值表示该字段不做限制，而 0 表示该字段的值必须为0。
  * 
  */
public class ReflectionUtil {
	 
	     private static Logger logger = LoggerFactory.getLogger(ReflectionUtil.class);
	 
//	     static {
//	         DateLocaleConverter dc = new DateLocaleConverter();
//	         // dc.setPatterns(new String[] { "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss" });
//	         ConvertUtils.register(dc, Date.class);
//	     }
	 
	     /**
	      * 调用Getter方法.
	      */
	     public static Object invokeGetterMethod(Object target, String propertyName) {
	         String getterMethodName = "get" + StringUtils.capitalize(propertyName);
	         return invokeMethod(target, getterMethodName, new Class[] {},
	                 new Object[] {});
	     }
	 
	     /**
	      * 调用Setter方法.使用value的Class来查找Setter方法.
	      */
	     public static void invokeSetterMethod(Object target, String propertyName,
	             Object value) {
	         invokeSetterMethod(target, propertyName, value, null);
	     }
	 
	     /**
	      * 调用Setter方法.
	      * 
	      * @param propertyType
	      *            用于查找Setter方法,为空时使用value的Class替代.
	      */
	     public static void invokeSetterMethod(Object target, String propertyName,
	             Object value, Class<?> propertyType) {
	         Class<?> type = propertyType != null ? propertyType : value.getClass();
	         String setterMethodName = "set" + StringUtils.capitalize(propertyName);
	         invokeMethod(target, setterMethodName, new Class[] { type },
	                 new Object[] { value });
	     }
	 
	     /**
	      * 直接读取对象属性值, 无视private/protected修饰符, 不经过getter函数.
	      */
	     public static Object getFieldValue(final Object object,
	             final String fieldName) {
	         Field field = getDeclaredField(object, fieldName);
	 
	         if (field == null) {
	             throw new IllegalArgumentException("Could not find field ["
	                     + fieldName + "] on target [" + object + "]");
	         }
	 
	         makeAccessible(field);
	 
	         Object result = null;
	         try {
	             result = field.get(object);
	         } catch (IllegalAccessException e) {
	             logger.error("不可能抛出的异常{}" + e.getMessage());
	         }
	         return result;
	     }
	 
	     /**
	      * 直接设置对象属性值, 无视private/protected修饰符, 不经过setter函数.
	      */
	     public static void setFieldValue(final Object object,
	             final String fieldName, final Object value) {
	         Field field = getDeclaredField(object, fieldName);
	 
	         if (field == null) {
	             throw new IllegalArgumentException("Could not find field ["
	                     + fieldName + "] on target [" + object + "]");
	         }
	 
	         makeAccessible(field);
	 
	         try {
	             field.set(object, value);
	         } catch (IllegalAccessException e) {
	             logger.error("不可能抛出的异常:{}" + e.getMessage());
	         }
	     }
	 
	     /**
	      * 直接调用对象方法, 无视private/protected修饰符.
	      */
	     public static Object invokeMethod(final Object object,
	             final String methodName, final Class<?>[] parameterTypes,
	             final Object[] parameters) {
	         Method method = getDeclaredMethod(object, methodName, parameterTypes);
	         if (method == null) {
	             throw new IllegalArgumentException("Could not find method ["
	                     + methodName + "] parameterType " + parameterTypes
	                     + " on target [" + object + "]");
	         }
	 
	         method.setAccessible(true);
	 
	         try {
	             return method.invoke(object, parameters);
	         } catch (Exception e) {
	             throw convertReflectionExceptionToUnchecked(e);
	         }
	     }
	 
	     /**
	      * 循环向上转型, 获取对象的DeclaredField.
	      * 
	      * 如向上转型到Object仍无法找到, 返回null.
	      */
	     public static Field getDeclaredField(final Object object, final String fieldName) {
	         if(object == null || fieldName == null || fieldName.trim().length() == 0)
	         {
	        	 return null;
	         }
	         for (Class<?> superClass = object.getClass(); superClass != Object.class; superClass = superClass
	                 .getSuperclass()) {
	             try {
	                 return superClass.getDeclaredField(fieldName);
	             } catch (NoSuchFieldException e) {// NOSONAR
	                  // Field不在当前类定义,继续向上转型
	            	 logger.debug("获取字段异常-->" + superClass + "-->" + e.getMessage());
	             }
	         }
	         return null;
	     }
	 
	     /**
	      * 强行设置Field可访问.
	      */
	     protected static void makeAccessible(final Field field) {
	         if (!Modifier.isPublic(field.getModifiers())
	                 || !Modifier.isPublic(field.getDeclaringClass().getModifiers())) {
	             field.setAccessible(true);
	         }
	     }
	 
	     /**
	      * 循环向上转型, 获取对象的DeclaredMethod.
	      * 
	      * 如向上转型到Object仍无法找到, 返回null.
	      */
	     public static Method getDeclaredMethod(Object object, String methodName, Class<?>[] parameterTypes) {
	    	 if(object == null)
	    	 {
	    		 return null;
	    	 }
	         for (Class<?> superClass = object.getClass(); superClass != Object.class; superClass = superClass
	                 .getSuperclass()) {
	             try {
	                 return superClass.getDeclaredMethod(methodName, parameterTypes);
	             } catch (NoSuchMethodException e) {// NOSONAR
	                 // Method不在当前类定义,继续向上转型
	            	 logger.debug("获取方法异常-->" + superClass + "-->" + e.getMessage());
	             }
	         }
	         return null;
	     }
	 
	     /**
	      * 通过反射, 获得Class定义中声明的父类的泛型参数的类型. 如无法找到, 返回Object.class. eg. public UserDao
	      * extends HibernateDao<User>
	      * 
	      * @param clazz
	      *            The class to introspect
	      * @return the first generic declaration, or Object.class if cannot be
	      *         determined
	      */
	     @SuppressWarnings("unchecked")
	     public static <T> Class<T> getSuperClassGenricType(final Class<?> clazz) {
	         return getSuperClassGenricType(clazz, 0);
	     }
	 
	     /**
	      * 通过反射, 获得定义Class时声明的父类的泛型参数的类型. 如无法找到, 返回Object.class.
	      * 
	      * 如public UserDao extends HibernateDao<User,Long>
	      * 
	      * @param clazz
	      *            clazz The class to introspect
	      * @param index
	      *            the Index of the generic ddeclaration,start from 0.
	      * @return the index generic declaration, or Object.class if cannot be
	      *         determined
	      */
	     @SuppressWarnings("unchecked")
	     public static Class getSuperClassGenricType(final Class<?> clazz,
	             final int index) {
	         Type genType = clazz.getGenericSuperclass();
	 
	         if (!(genType instanceof ParameterizedType)) {
	             logger.warn(clazz.getSimpleName()
	                     + "'s superclass not ParameterizedType");
	             return Object.class;
	         }
	 
	         Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
	 
	         if (index >= params.length || index < 0) {
	             logger.warn("Index: " + index + ", Size of "
	                     + clazz.getSimpleName() + "'s Parameterized Type: "
	                     + params.length);
	             return Object.class;
	         }
	         if (!(params[index] instanceof Class)) {
	             logger.warn(clazz.getSimpleName()
	                     + " not set the actual class on superclass generic parameter");
	             return Object.class;
	         }
	 
	         return (Class) params[index];
	     }
	 
	 
	     /**
	      * 将反射时的checked exception转换为unchecked exception.
	      */
	     public static RuntimeException convertReflectionExceptionToUnchecked(
	             Exception e) {
	         return convertReflectionExceptionToUnchecked(null, e);
	     }
	 
	     public static RuntimeException convertReflectionExceptionToUnchecked(
	             String desc, Exception e) {
	         desc = (desc == null) ? "Unexpected Checked Exception." : desc;
	         if (e instanceof IllegalAccessException
	                 || e instanceof IllegalArgumentException
	                 || e instanceof NoSuchMethodException) {
	             return new IllegalArgumentException(desc, e);
	         } else if (e instanceof BusinessException) {
	        	 return (RuntimeException)e;
	         } else if (e.getCause() instanceof BusinessException) {
	        	 return (RuntimeException)e.getCause();
	         } else if (e instanceof InvocationTargetException) {
	             return new RuntimeException(desc,
	                     ((InvocationTargetException) e).getTargetException());
	         } else if (e instanceof RuntimeException) {
	             return (RuntimeException) e;
	         }
	         return new RuntimeException(desc, e);
	     }
	 
	     public static final <T> T getNewInstance(Class<T> cls) {
	         try {
	             return cls.newInstance();
	         } catch (InstantiationException e) {
	        	 logger.debug("获取对象实例异常-->" + cls + "-->" + e.getMessage());
	         } catch (IllegalAccessException e) {
	        	 logger.debug("获取对象实例异常-->" + cls + "-->" + e.getMessage());
	         }
	         return null;
	     }
	     
	     public static final <T> T getNewInstance(Class<T> cls, Class<?>[] parameterTypes, Object[] parameterValues) {
	         try {
	             Constructor<T> cons = cls.getConstructor(parameterTypes);
	             if (cons != null) {
	                 return cons.newInstance(parameterValues);
	             }
             } catch (Exception e) {
                 logger.debug("获取对象实例异常-->" + cls + "-->" + e.getMessage());
            }
             return null;
	     }
	 
	     /**
	      * 两者属性名一致时，拷贝source里的属性到dest里
	      * 
	      * @return void
	      * @throws IllegalAccessException
	      * @throws InvocationTargetException
	      */
	     public static void copyPorperties(Object dest, Object source)
	             throws IllegalAccessException, InvocationTargetException {
	         //remove 详见类上描述
//	         Class<? extends Object> srcCla = source.getClass();
//	         Field[] fsF = srcCla.getDeclaredFields();
//	 
//	         for (Field s : fsF) {
//	             String name = s.getName();
//	             try {
//	                 Object srcObj = invokeGetterMethod(source, name);
//	                 BeanUtils.setProperty(dest, name, srcObj);
//	             } catch (Exception e) {
//	            	 logger.debug("设置对象属性值异常-->" + dest +"-->" + name + "-->" + e.getMessage());
//	             }
//	         }
	         // BeanUtils.copyProperties(dest, orig);
	         
	         BeanUtils.copyProperties(source, dest);
	     }
	     /**
	      * 拷贝对象属性，Null值不拷贝
	      * @param dest
	      * @param source
	      * @throws IllegalAccessException
	      * @throws InvocationTargetException
	      */
	     public static void copyPorpertiesFilterNullValue(Object dest, Object source)
                 throws IllegalAccessException, InvocationTargetException {
	         Set<String> ignoreProperties = new HashSet<>();
	         // 获取f对象对应类中的所有属性域  
             Field[] fields = dest.getClass().getDeclaredFields();  
             for (Field field : fields) {  
                 String varName = field.getName();  
                 try {  
                     // 获取原来的访问控制权限  
                     boolean accessFlag = field.isAccessible();  
                     // 修改访问控制权限  
                     field.setAccessible(true);  
                     // 获取在对象f中属性fields[i]对应的对象中的变量  
                     Object o = field.get(dest);
                     if (o == null) {
                         ignoreProperties.add(varName);
                     }
                     // 恢复访问控制权限  
                     field.setAccessible(accessFlag);  
                 } catch (Exception ex) {  
                 } 
             }
             BeanUtils.copyProperties(dest, source);
	     }
	     /**
	      * 通过反射 将一个bean对象转换成map
	      * @param obj
	      * @return
	      */
	     public static Map<String, Object> getValueMap(Object obj) {  
	         
	         Map<String, Object> map = new HashMap<String, Object>();  
	         // System.out.println(obj.getClass());  
	         // 获取f对象对应类中的所有属性域  
	         Field[] fields = obj.getClass().getDeclaredFields();  
	         for (int i = 0, len = fields.length; i < len; i++) {  
	             String varName = fields[i].getName();  
	             try {  
	                 // 获取原来的访问控制权限  
	                 boolean accessFlag = fields[i].isAccessible();  
	                 // 修改访问控制权限  
	                 fields[i].setAccessible(true);  
	                 // 获取在对象f中属性fields[i]对应的对象中的变量  
	                 Object o = fields[i].get(obj);
	                 if (o != null)  
	                     map.put(varName, o);  
	                 // System.out.println("传入的对象中包含一个如下的变量：" + varName + " = " + o);  
	                 // 恢复访问控制权限  
	                 fields[i].setAccessible(accessFlag);  
	             } catch (IllegalArgumentException ex) {  
	            	 logger.debug("获取对象属性值异常-->" + obj +"-->" + varName + "-->" + ex.getMessage());
	             } catch (IllegalAccessException ex) {  
	            	 logger.debug("获取对象属性值异常-->" + obj +"-->" + varName + "-->" + ex.getMessage());
	             }  
	         }  
	         return map;  
	   
	     }
}