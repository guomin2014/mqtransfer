package com.gm.mqtransfer.provider.facade.exception;

/**
 * 用于服务抛出的异常
 * @author gm
 *
 */
public class BusinessException extends RuntimeException {
	
	private static final long serialVersionUID = 6299492064783746524L;

	public BusinessException(String message){
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public BusinessException(Throwable e){
        super(e);
    }
    
}
