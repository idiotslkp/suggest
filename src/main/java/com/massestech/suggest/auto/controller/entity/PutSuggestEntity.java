package com.massestech.suggest.auto.controller.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("新增索引内容model")
public class PutSuggestEntity {
    @ApiModelProperty(value = "id")
    private String id;
    @ApiModelProperty(value = "对应索引")
    private String index;
    @ApiModelProperty(value = "查询内容")
    private String queryString;
    @ApiModelProperty(value = "存储内容")
    private String content;
}
