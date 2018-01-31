package com.massestech.core.base;

/**
 * Created by lyb on 2016/10/8.
 */
public class BaseException extends RuntimeException{

    private int code = -1;

    public BaseException() {}

    public BaseException(String message){
        super(message);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
