package com.gm.mqtransfer.facade.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class DataUtil {
    /**
     * 数据转换
     * 
     * @param value
     * @return
     */
    public static double conver2Double(BigDecimal value) {
        if (value == null) {
            return 0;
        }
        return value.doubleValue();
    }

    public static double conver2Double(BigDecimal value, int scale) {
        if (value == null) {
            return 0;
        }
        return value.setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    public static double conver2Double(Double value) {
        if (value == null) {
            return 0;
        }
        return value.doubleValue();
    }

    public static int conver2Int(Integer value) {
        if (value == null) {
            return 0;
        }
        return value.intValue();
    }

    public static int conver2Int(Long value) {
        if (value == null) {
            return 0;
        }
        return value.intValue();
    }

    public static int conver2Int(BigDecimal value) {
        if (value == null) {
            return 0;
        }
        return value.intValue();
    }

    public static int conver2Int(Double value) {
        if (value == null) {
            return 0;
        }
        return value.intValue();
    }

    public static int conver2Int(Object value) {
        if (value == null || value.toString().trim().length() == 0) {
            return 0;
        }
        return Integer.parseInt(value.toString());
    }

    public static long conver2Long(Long value) {
        if (value == null) {
            return 0;
        }
        return value.longValue();
    }

    public static String conver2String(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    public static Integer converObj2Integer(Object value) {
        if (value == null || value.toString().trim().length() == 0) {
            return 0;
        }
        return new Integer(value.toString());
    }

    public static Integer converObj2IntegerWithNull(Object value) {
        if (value == null || value.toString().trim().length() == 0) {
            return null;
        }
        try {
            return new Integer(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static Long converObj2Long(Object value) {
        if (value == null || value.toString().trim().length() == 0) {
            return 0L;
        }
        return Long.parseLong(value.toString());
    }

    public static Long converObj2LongWithNull(Object value) {
        if (value == null || value.toString().trim().length() == 0) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static Float converObj2Float(Object value) {
        if (value == null || value.toString().trim().length() == 0) {
            return 0F;
        }
        return Float.parseFloat(value.toString());
    }

    public static Float converObj2FloatWithNull(Object value) {
        if (value == null || value.toString().trim().length() == 0) {
            return null;
        }
        try {
            return Float.parseFloat(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static Double converObj2Double(Object value) {
        if (value == null || value.toString().trim().length() == 0) {
            return 0.0;
        }
        return Double.parseDouble(value.toString());
    }

    public static Double converObj2DoubleWithNull(Object value) {
        if (value == null || value.toString().trim().length() == 0) {
            return null;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
    
    public static Boolean converObj2Boolean(Object value) {
        if (value == null || value.toString().trim().length() == 0) {
            return false;
        }
        return Boolean.parseBoolean(value.toString());
    }
    
    public static Boolean converObj2BooleanWithNull(Object value) {
        if (value == null || value.toString().trim().length() == 0) {
            return null;
        }
        try {
            return Boolean.parseBoolean(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static BigDecimal converObj2BigDecimal(Object value) {
        if (value == null || value.toString().trim().length() == 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.toString());
    }

    public static BigDecimal converObj2BigDecimalWithNull(Object value) {
        if (value == null || value.toString().trim().length() == 0) {
            return null;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static Number converObj2Number(Object value) {
        if (value == null || value.toString().trim().length() == 0) {
            return 0;
        }
        try {
            return NumberFormat.getNumberInstance().parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static Number converObj2NumberWithNull(Object value) {
        if (value == null || value.toString().trim().length() == 0) {
            return null;
        }
        try {
            return NumberFormat.getNumberInstance().parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static String converObj2String(Object value) {
        if (value == null || value.toString().trim().length() == 0) {
            return "";
        }
        return value.toString().trim();
    }

    public static String converObj2StringWithNull(Object value) {
        if (value == null || value.toString().trim().length() == 0) {
            return null;
        }
        return value.toString().trim();
    }
    /**
     * 转换成字符串，按指定位数四舍五入
     * @param value
     * @param scale
     * @return
     */
    public static String conver2String(BigDecimal value, int scale) {
        if (value == null) {
            return "0";
        }
        return value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }
    /**
     * 转换成字符串，a-b并将结果按指定位数四舍五入
     * @param a
     * @param b
     * @param scale
     * @return
     */
    public static String conver2StringForSubtract(BigDecimal a, BigDecimal b, int scale) {
        if (a == null) {
            a = BigDecimal.ZERO;
        }
        if (b == null) {
            b = BigDecimal.ZERO;
        }
        return a.setScale(scale, RoundingMode.HALF_UP).subtract(b.setScale(scale, RoundingMode.HALF_UP)).toPlainString();
    }
    /**
     * 转换成字符串，a/b并将结果按指定位数四舍五入
     * @param a
     * @param b
     * @param scale
     * @return
     */
    public static String conver2StringForDivide(BigDecimal a, BigDecimal b, int scale) {
        if (a == null) {
            a = BigDecimal.ZERO;
        }
        if (b == null) {
            b = BigDecimal.ONE;
        }
        return a.setScale(scale, RoundingMode.HALF_UP).divide(b.setScale(scale, RoundingMode.HALF_UP), scale, BigDecimal.ROUND_HALF_UP).toPlainString();
    }

    /**
     * 计算两数的商
     * 
     * @param a
     * @param b
     * @return
     */
    public static Double quotient(BigDecimal a, BigDecimal b) {
        return quotient(a, b, 3);
    }
    public static Double quotient(BigDecimal a, BigDecimal b, int scale) {
        if (a == null) {
            a = BigDecimal.ZERO;
        }
        if (b == null) {
            b = BigDecimal.ONE;
        }
        return a.divide(b, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    public static int converStr2Int(String value, int defaultValue) {
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static long converStr2Long(String value, long defaultValue) {
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static double converStr2Double(String value, double defaultValue) {
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T converObj(Object value, Class<T> clazz) {
        if (clazz == Integer.TYPE || clazz == Integer.class) {
            return (T) converObj2Integer(value);
        } else if (clazz == Long.TYPE || clazz == Long.class) {
            return (T) converObj2Long(value);
        } else if (clazz == Float.TYPE || clazz == Float.class) {
            return (T) converObj2Float(value);
        } else if (clazz == Double.TYPE || clazz == Double.class) {
            return (T) converObj2Double(value);
        } else if (clazz == Boolean.TYPE || clazz == Boolean.class) {
            return (T) converObj2Boolean(value);
        } else if (clazz == BigDecimal.class) {
            return (T) converObj2BigDecimal(value);
        } else if (clazz == String.class) {
            return (T) converObj2String(value);
        } else if (clazz == Number.class) {
            return (T) converObj2Number(value);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T converObjWithNull(Object value, Class<T> clazz) {
        try {
            if (clazz == Integer.TYPE || clazz == Integer.class) {
                return (T) converObj2IntegerWithNull(value);
            } else if (clazz == Long.TYPE || clazz == Long.class) {
                return (T) converObj2LongWithNull(value);
            } else if (clazz == Float.TYPE || clazz == Float.class) {
                return (T) converObj2FloatWithNull(value);
            } else if (clazz == Double.TYPE || clazz == Double.class) {
                return (T) converObj2DoubleWithNull(value);
            } else if (clazz == Boolean.TYPE || clazz == Boolean.class) {
                return (T) converObj2BooleanWithNull(value);
            } else if (clazz == BigDecimal.class) {
                return (T) converObj2BigDecimalWithNull(value);
            } else if (clazz == String.class) {
                return (T) converObj2StringWithNull(value);
            } else if (clazz == Number.class) {
                return (T) converObj2NumberWithNull(value);
            }
        } catch (Exception e) {

        }
        return null;
    }
    @SuppressWarnings("unchecked")
    public static <T> T converJsonObj(Object value, Class<T> clazz) {
    	if (value == null) {
    		return null;
    	}
    	if (clazz == Integer.TYPE || clazz == Integer.class) {
    		return (T) converObj2Integer(value);
    	} else if (clazz == Long.TYPE || clazz == Long.class) {
    		return (T) converObj2Long(value);
    	} else if (clazz == Float.TYPE || clazz == Float.class) {
    		return (T) converObj2Float(value);
    	} else if (clazz == Double.TYPE || clazz == Double.class) {
    		return (T) converObj2Double(value);
    	} else if (clazz == Boolean.TYPE || clazz == Boolean.class) {
    		return (T) converObj2Boolean(value);
    	} else if (clazz == BigDecimal.class) {
    		return (T) converObj2BigDecimal(value);
    	} else if (clazz == String.class) {
    		return (T) converObj2String(value);
    	} else if (clazz == Number.class) {
    		return (T) converObj2Number(value);
    	} else {
    		return JSON.parseObject(value.toString(), clazz);
    	}
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T converJsonObjWithNull(Object value, Class<T> clazz) {
    	if (value == null) {
    		return null;
    	}
    	if (clazz == Integer.TYPE || clazz == Integer.class) {
			return (T) converObj2IntegerWithNull(value);
		} else if (clazz == Long.TYPE || clazz == Long.class) {
			return (T) converObj2LongWithNull(value);
		} else if (clazz == Float.TYPE || clazz == Float.class) {
			return (T) converObj2FloatWithNull(value);
		} else if (clazz == Double.TYPE || clazz == Double.class) {
			return (T) converObj2DoubleWithNull(value);
		} else if (clazz == Boolean.TYPE || clazz == Boolean.class) {
			return (T) converObj2BooleanWithNull(value);
		} else if (clazz == BigDecimal.class) {
			return (T) converObj2BigDecimalWithNull(value);
		} else if (clazz == String.class) {
			return (T) converObj2StringWithNull(value);
		} else if (clazz == Number.class) {
			return (T) converObj2NumberWithNull(value);
		} else {
    		return JSON.parseObject(value.toString(), clazz);
    	}
    }
    
    public static byte[] converObj2JsonByte(Object value) {
    	if (value == null) {
    		return new byte[0];
    	}
    	Class<?> clazz = value.getClass();
    	if (clazz == Integer.TYPE || clazz == Integer.class 
    			|| clazz == Long.TYPE || clazz == Long.class
    			|| clazz == Float.TYPE || clazz == Float.class
    			|| clazz == Double.TYPE || clazz == Double.class
    			|| clazz == Boolean.TYPE || clazz == Boolean.class
    			|| clazz == BigDecimal.class
    			|| clazz == Number.class
    			|| clazz == String.class) {
			return value.toString().getBytes();
		} else {
    		return JSON.toJSONBytes(value);
    	}
    }

    /**
     * 将byte转换为一个长度为8的byte数组，数组每个值代表bit
     */
    public static byte[] byte2BitArray(byte b) {
        byte[] array = new byte[8];
        for (int i = 7; i >= 0; i--) {
            array[i] = (byte) (b & 1);
            b = (byte) (b >> 1);
        }
        return array;
    }

    /**
     * 把byte转为字符串的bit
     */
    public static String byte2Bit(byte b) {
        return "" + (byte) ((b >> 7) & 0x1) + (byte) ((b >> 6) & 0x1) + (byte) ((b >> 5) & 0x1) + (byte) ((b >> 4) & 0x1) + (byte) ((b >> 3) & 0x1) + (byte) ((b >> 2) & 0x1)
                + (byte) ((b >> 1) & 0x1) + (byte) ((b >> 0) & 0x1);
    }

    /**
     * 将列表转换成Map
     * 
     * @param list
     * @return key：list对象的ID，value：list对象的name
     */
    public static Map<String, String> converList2Map(List<?> list) {
        return converList2Map(list, false);
    }

    /**
     * 将列表转换成Map
     * 
     * @param list
     * @param sort
     *            是否排序(按添加顺序)
     * @return
     */
    public static Map<String, String> converList2Map(List<?> list, boolean sort) {
        return converList2Map(list, "name", sort);
    }

    public static Map<String, String> converList2Map(List<?> list, String labelFieldName) {
        return converList2Map(list, labelFieldName, false);
    }

    public static Map<String, String> converList2Map(List<?> list, String labelFieldName, boolean sort) {
        return converList2Map(list, labelFieldName, "id", sort);
    }

    public static Map<String, String> converList2Map(List<?> list, String labelFieldName, String valueFieldName) {
        return converList2Map(list, labelFieldName, valueFieldName, false);
    }

    public static Map<String, String> converList2Map(List<?> list, String labelFieldName, String valueFieldName, boolean sort) {
        if (StringUtils.isEmpty(labelFieldName)) {
            labelFieldName = "name";
        }
        if (StringUtils.isEmpty(valueFieldName)) {
            valueFieldName = "id";
        }
        Map<String, String> map = null;
        if (sort) {
            map = new LinkedHashMap<>();
        } else {
            map = new HashMap<>();
        }
        if (list != null) {
            try {
                for (Object obj : list) {
                    if (obj == null) {
                        continue;
                    }
                    Object id = ReflectionUtil.getFieldValue(obj, valueFieldName);
                    Object name = ReflectionUtil.getFieldValue(obj, labelFieldName);
                    if (id == null || name == null) {
                        continue;
                    }
                    map.put(id.toString(), name.toString());
                }
            } catch (Exception e) {
            }
        }
        return map;
    }

    public static JSONArray converList2JsonArray(List<?> list) {
        return converList2JsonArray(list, "name");
    }

    public static JSONArray converList2JsonArray(List<?> list, String labelFieldName) {
        if (StringUtils.isEmpty(labelFieldName)) {
            labelFieldName = "name";
        }
        String valueFieldName = "id";
        JSONArray ret = new JSONArray();
        if (list != null) {
            try {
                for (Object obj : list) {
                    if (obj == null) {
                        continue;
                    }
                    Object id = ReflectionUtil.getFieldValue(obj, valueFieldName);
                    Object name = ReflectionUtil.getFieldValue(obj, labelFieldName);
                    if (id == null || name == null) {
                        continue;
                    }
                    JSONObject json = new JSONObject();
                    json.put("value", id.toString());
                    json.put("label", name.toString());
                    ret.add(json);
                }
            } catch (Exception e) {
            }
        }
        return ret;
    }

    /**
     * 将数据转换成16进制（不带0x）
     * 
     * @param value
     * @return
     */
    public static String toHexString(Integer value) {
        if (value == null) {
            return null;
        }
        return Integer.toHexString(value).toUpperCase();
    }
}
