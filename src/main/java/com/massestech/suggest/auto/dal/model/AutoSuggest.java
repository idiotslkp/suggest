package com.massestech.suggest.auto.dal.model;

import com.massestech.core.base.es.dal.model.ESBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;


@Data
@ApiModel("自动匹配")
@Document(indexName = "auto",type = "autosuggest")
public class AutoSuggest extends ESBaseEntity {

    @ApiModelProperty(value = "查询内容")
    private String queryString;
    @ApiModelProperty(value = "存储内容")
    private String content;

}