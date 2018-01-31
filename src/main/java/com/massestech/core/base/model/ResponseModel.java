package com.massestech.core.base.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 */
@ApiModel
public class ResponseModel<T>  implements Serializable {

    @ApiModelProperty(value = "返回码")
    private int code;

    @ApiModelProperty(value = "说明")
    private String msg;

    @ApiModelProperty(value = "返回数据")
    private T data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
