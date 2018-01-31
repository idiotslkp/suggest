package com.massestech.core.base.es.dal.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by lyb on 2016/10/8.
 */
@Data
public class ESBaseEntity implements Serializable{

    @ApiModelProperty(value = "id")
    protected String id;

    @ApiModelProperty(value = "创建时间")
    protected Date createdTime = new Date();

//    @ApiModelProperty(hidden = true)
//    @JsonIgnore
//    protected boolean deleted = false;

}
