package com.gm.mqtransfer.facade.exception;

/**
 * 异常码的枚举
 * 1开头：请求参数异常，2：权限异常，3：状态异常，4：客户端异常，5：服务端异常
 * @author	GM
 * @date	2023年9月18日
 */
public enum ExceptionCodeEnum {
	/** 请求参数为空 */
	PARAM_IS_EMPTY(101, "Parameter is empty"),
	/** 请求参数无效 */
	PARAM_IS_INVALID(102, "Invalid parameter"),
	
    /** 无效的用户信息 */
    AUTH_USER_INVALID(201, "Invalid user info"),
    /** 无效的签名 */
    AUTH_SIGNATURE_INVALID(202, "Incorrect signature"),
    /** 无效的Token */
    AUTH_TOKEN_INVALID(203, "Access token invalid"),
    /**token过期 */
    AUTH_TOKEN_EXPIRED(204, "Access token expired"),
    /** 您没有执行该操作的权限（数据权限，比如编辑记录） */
    AUTH_OPERATION_INVALID(205, "Invalid operation"),
    
    /** 请求超时 */
    STATUS_REQUEST_TIMEOUT(301, "Request timeout"),
    /** 请求超限 */
    STATUS_REQUEST_OVER_LIMITED(302, "Request over limited"),
    /** 未知的方法 */
    STATUS_REQUEST_METHOD_UNSUPPORTED(303, "Unsupported method"),
    /** 数据已经存在 */
    STATUS_REQUEST_DATA_EXISTS(304, "Data already exists"),
    /** 数据不存在 */
    STATUS_REQUEST_DATA_NOT_EXISTS(305, "Data does not exist"),
    /** 用户已被停用 */
    STATUS_USER_DEACTIVATED(306, "User has been deactivated"),
    
    /** 错误的请求 */
    CLIENT_REQUEST_BAD(400, "bad request"),
    /** 请求未通过身份验证 */
    CLIENT_REQUEST_UNAUTHORIZED(401, "Unauthorized"),
    /** 请求拒绝，没有权限（资源权限，比如下载文件，访问静态资源） */
    CLIENT_REQUEST_FORBIDDEN(403, "Forbidden"),
    /** 请求地址不存在 */
    CLIENT_REQUEST_URI_NOT_EXISTS(404, "The requested address not found"),
    /** 请求方法不允许 */
    CLIENT_REQUEST_METHOD_NOT_ALLOWED(405, "The requested method not allowed"),
    /** 请求的响应内容无法解析 */
    CLIENT_REQUEST_METHOD_NOT_ACCEPTABLE(406, "Not Acceptable"),
    /** 请求内容类型不支持 */
    CLIENT_REQUEST_MEDIA_TYPE_UNSUPPORTED(415, "Unsupported media type"),
    
    /** 系统异常 */
    SYSTEM_ERROR(500, "System error"),
    /** 系统繁忙 */
    SYSTEM_BUSY(501, "System busy"),
    /** 网关错误 */
    SYSTEM_BAD_GATEWAY(502, "Bad Gateway"),
    /** 服务不可用 */
    SYSTEM_SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    /** 网关超时 */
    SYSTEM_GATEWAY_TIMEOUT(504, "Gateway Timeout"),
    ;


    private int code;
    private String desc;
    

    ExceptionCodeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
        
    }
    
    public int getCode() {
        return this.code;
    }

    public String getDesc() {
        return desc;
    }

    public static ExceptionCodeEnum getByValue(int code) {
        for (ExceptionCodeEnum custBalanceAlert : ExceptionCodeEnum.values()) {
            if (custBalanceAlert.getCode() == code) {
                return custBalanceAlert;
            }
        }
        return null;
    }
    
}
