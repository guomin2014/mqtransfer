package com.gm.mqtransfer.provider.facade.util;

public class StringUtils {

	/**
     * <p>Checks if a String is empty ("") or null.</p>
     *
     * <pre>
     * StringUtils.isEmpty(null)      = true
     * StringUtils.isEmpty("")        = true
     * StringUtils.isEmpty(" ")       = false
     * StringUtils.isEmpty("bob")     = false
     * StringUtils.isEmpty("  bob  ") = false
     * </pre>
     *
     * <p>NOTE: This method changed in Lang version 2.0.
     * It no longer trims the String.
     * That functionality is available in isBlank().</p>
     *
     * @param str  the String to check, may be null
     * @return <code>true</code> if the String is empty or null
     */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
    /**
     * <p>Checks if a String is not empty ("") and not null.</p>
     *
     * <pre>
     * StringUtils.isNotEmpty(null)      = false
     * StringUtils.isNotEmpty("")        = false
     * StringUtils.isNotEmpty(" ")       = true
     * StringUtils.isNotEmpty("bob")     = true
     * StringUtils.isNotEmpty("  bob  ") = true
     * </pre>
     *
     * @param str  the String to check, may be null
     * @return <code>true</code> if the String is not empty and not null
     */
    public static boolean isNotEmpty(String str) {
        return !StringUtils.isEmpty(str);
    }
    /**
     * <p>Checks if a String is whitespace, empty ("") or null.</p>
     *
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * </pre>
     *
     * @param str  the String to check, may be null
     * @return <code>true</code> if the String is null, empty or whitespace
     * @since 2.0
     */
    public static boolean isBlank(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if ((Character.isWhitespace(str.charAt(i)) == false)) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Checks if a String is not empty (""), not null and not whitespace only.</p>
     *
     * <pre>
     * StringUtils.isNotBlank(null)      = false
     * StringUtils.isNotBlank("")        = false
     * StringUtils.isNotBlank(" ")       = false
     * StringUtils.isNotBlank("bob")     = true
     * StringUtils.isNotBlank("  bob  ") = true
     * </pre>
     *
     * @param str  the String to check, may be null
     * @return <code>true</code> if the String is
     *  not empty and not null and not whitespace
     * @since 2.0
     */
    public static boolean isNotBlank(String str) {
        return !StringUtils.isBlank(str);
    }
    /**
     * <p>Removes control characters (char &lt;= 32) from both
     * ends of this String, handling <code>null</code> by returning
     * <code>null</code>.</p>
     *
     * <p>The String is trimmed using {@link String#trim()}.
     * Trim removes start and end characters &lt;= 32.
     * To strip whitespace use {@link #strip(String)}.</p>
     *
     * <p>To trim your choice of characters, use the
     * {@link #strip(String, String)} methods.</p>
     *
     * <pre>
     * StringUtils.trim(null)          = null
     * StringUtils.trim("")            = ""
     * StringUtils.trim("     ")       = ""
     * StringUtils.trim("abc")         = "abc"
     * StringUtils.trim("    abc    ") = "abc"
     * </pre>
     *
     * @param str  the String to be trimmed, may be null
     * @return the trimmed string, <code>null</code> if null String input
     */
    public static String trim(String str) {
        return str == null ? null : str.trim();
    }
    /**
     * Splice two paths into a complete path
     * <pre>
     * StringUtils.splitJointUrl("https://www.mqtransfer.com", null)				=	"https://www.mqtransfer.com/"
     * StringUtils.splitJointUrl(null, "/query/list")						=	"/query/list"
     * StringUtils.splitJointUrl("https://www.mqtransfer.com", "/query/list")	=	"https://www.mqtransfer.com/query/list"
     * StringUtils.splitJointUrl("https://www.mqtransfer.com", "query/list")		=	"https://www.mqtransfer.com/query/list"
     * StringUtils.splitJointUrl("https://www.dmmqtransferall.com", "https://www.mqtransfer.com/query/list")	=	"https://www.mqtransfer.com/query/list"
     * </pre>
     * @param domain
     * @param path
     * @return
     */
    public static String splitJointUrl(String domain, String path) {
    	domain = StringUtils.appendPathSeparator(domain);
    	path = StringUtils.removeFirstPathSeparator(path);
    	if (domain == null) {
    		domain = "";
    	}
    	if (path == null) {
    		path = "";
    	}
    	if (path.startsWith(domain)) {
    		return path;
    	} else {
    		return domain + path;
    	}
    }
    /**
     * Add separator after path
     * <pre>
     * StringUtils.appendPathSeparator("")				=	""
     * StringUtils.appendPathSeparator(null)			=	null
     * StringUtils.appendPathSeparator("/query/list")	=	"/query/list/"
     * StringUtils.appendPathSeparator("/query/list/")	=	"/query/list/"
     * </pre>
     * @param path
     * @return
     */
    public static String appendPathSeparator(String path) {
    	if (StringUtils.isBlank(path)) {
    		return path;
    	}
    	path = StringUtils.trim(path);
    	if (path.endsWith("/") || path.endsWith("\\")) {
    		return path;
    	} else {
    		return path + "/";
    	}
    }
    /**
     * Remove the last separator of the path
     * <pre>
     * StringUtils.removeLastPathSeparator("")				=	""
     * StringUtils.removeLastPathSeparator(null)			=	null
     * StringUtils.removeLastPathSeparator("/query/list")	=	"/query/list"
     * StringUtils.removeLastPathSeparator("/query/list/")	=	"/query/list"
     * </pre>
     * @param path
     * @return
     */
    public static String removeLastPathSeparator(String path) {
    	if (StringUtils.isBlank(path)) {
    		return path;
    	}
    	path = StringUtils.trim(path);
    	if (path.endsWith("/") || path.endsWith("\\")) {
    		return path.substring(0, path.length() - 1);
    	} else {
    		return path;
    	}
    }
    /**
     * Add separator before path
     * <pre>
     * StringUtils.prependPathSeparator("")				=	""
     * StringUtils.prependPathSeparator(null)			=	null
     * StringUtils.prependPathSeparator("query/list")	=	"/query/list"
     * StringUtils.prependPathSeparator("/query/list")	=	"/query/list"
     * </pre>
     * @param path
     * @return
     */
    public static String prependPathSeparator(String path) {
    	if (StringUtils.isBlank(path)) {
    		return path;
    	}
    	path = StringUtils.trim(path);
    	if (path.startsWith("/") || path.startsWith("\\")) {
    		return path;
    	} else {
    		return "/" + path;
    	}
    }
    /**
     * Remove the first separator of the path
     * <pre>
     * StringUtils.removeFirstPathSeparator("")				=	""
     * StringUtils.removeFirstPathSeparator(null)			=	null
     * StringUtils.removeFirstPathSeparator("query/list")	=	"query/list"
     * StringUtils.removeFirstPathSeparator("/query/list")	=	"query/list"
     * </pre>
     * @param path
     * @return
     */
    public static String removeFirstPathSeparator(String path) {
    	if (StringUtils.isBlank(path)) {
    		return path;
    	}
    	path = StringUtils.trim(path);
    	if (path.startsWith("/") || path.startsWith("\\")) {
    		return path.substring(1);
    	} else {
    		return path;
    	}
    }
    
	public static String firstToLowerCase(String str) {
		return Character.toLowerCase(str.charAt(0)) + str.substring(1, str.length());
	}

	public static String firstToUpperCase(String str) {
		return Character.toUpperCase(str.charAt(0)) + str.substring(1, str.length());
	}

	public static String replaceToJava(String name) {
		if (isEmpty(name))
			return name;
		if (name.indexOf("_") == -1) {
			return firstToUpperCase(name);
		}
		String[] ss = name.split("_");
		StringBuffer retValue = new StringBuffer();
		for (String s : ss) {
			retValue.append(firstToUpperCase(s));
		}
		return retValue.toString();
	}

	public static String replaceToJavaAttribute(String name) {
		String str = replaceToJava(name);
		return firstToLowerCase(str);
	}
}
