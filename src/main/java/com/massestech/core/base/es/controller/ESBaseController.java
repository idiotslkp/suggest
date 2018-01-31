package com.massestech.core.base.es.controller;

import com.massestech.core.base.SimpleController;
import com.massestech.core.base.es.service.ESBaseService;
import com.massestech.core.base.model.ResponseModel;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

public class ESBaseController<T> extends SimpleController {

    @Autowired
    private ESBaseService<T> baseService;

    @PostMapping
    @ApiOperation(value = "创建")
    @ApiImplicitParams({
    })
    public ResponseEntity<ResponseModel<T>> create(@RequestBody T entity) {
        return success(baseService.create(entity));
    }

    @PutMapping
    @ApiOperation(value = "修改")
    @ApiImplicitParams({
    })
    public ResponseEntity<ResponseModel<T>> update(@RequestBody T entity) {
        baseService.update(entity);
        return success();
    }

    @GetMapping(value="/{id}")
    @ApiOperation(value = "详情")
    public ResponseEntity<ResponseModel<T>> info(@PathVariable String id) {
        T t = baseService.getOne(id);
        return success(t);
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除")
    @ApiImplicitParams({
    })
    public ResponseEntity<ResponseModel> delete(@PathVariable String id) {
        baseService.delete(id);
        return success();
    }

}
