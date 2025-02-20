package com.gm.mqtransfer.facade.exception;

/**
 * 业务异常，会导致事务回滚
 * @author	GM
 * @date	2023年9月15日
 */
public class BusinessException extends RuntimeException {

	private static final long serialVersionUID = -1770965566712438104L;
	
	/** 错误代码 */
    private int code = -1;

    /** 错误信息 */
    private String message = "";

    public BusinessException() {

    }

    public BusinessException(String message) {
        this(-1, message);
    }

    public BusinessException(int code) {
        this.code = code;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(Throwable e) {
        super(e);
    }
    
    public BusinessException(ExceptionCodeEnum code) {
        this(code.getCode(), code.getDesc());
    }

    public BusinessException(String message, Throwable e) {
        super(message, e);
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
