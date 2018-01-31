package com.massestech.core.base.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lyb on 2017/1/19.
 */
@ApiModel
public class PageModel<T> implements Serializable {

    @ApiModelProperty(value = "列表内容")
    private List<T> content = new ArrayList<>();

    @ApiModelProperty(value = "总条数")
    private long totalElements;

    @ApiModelProperty(value = "总页数")
    private long totalPages;

    @ApiModelProperty(value = "当前页")
    private long number;

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public long getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(long totalPages) {
        this.totalPages = totalPages;
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
    }
}
