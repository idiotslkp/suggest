package com.massestech.suggest.auto.controller;

import com.massestech.core.base.SimpleController;
import com.massestech.core.base.es.controller.ESBaseController;
import com.massestech.core.base.model.PageModel;
import com.massestech.core.base.model.ResponseModel;
import com.massestech.suggest.auto.controller.entity.PutSuggestEntity;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.massestech.suggest.auto.dal.model.AutoSuggest;
import com.massestech.suggest.auto.service.AutoSuggestService;

import java.util.List;

@Api(tags = "自动补全API")
@RestController
@RequestMapping(value = "/spjc/autosuggest")
public class AutoSuggestController extends SimpleController {
	
	@Autowired
	private AutoSuggestService autoSuggestService;

	@PostMapping("/suggest/{index}/{queryString}")
	@ApiOperation(value = "自动补全")
	public ResponseEntity<ResponseModel<List<String>>> suggest(@PathVariable("index") String index, @PathVariable("queryString") String queryString) {
		List<String> suggestList = autoSuggestService.suggest(index, queryString);
		return success(suggestList);
	}

	@GetMapping("/createIndex/{index}")
	@ApiOperation(value = "创建索引")
	public ResponseEntity createIndex(@PathVariable String index) {
		autoSuggestService.createIndex(index);
		return success();
	}

	@GetMapping("/indexExists/{index}")
	@ApiOperation(value = "判断索引是否存在")
	public ResponseEntity indexExists(@PathVariable String index) {
		boolean indexExists = autoSuggestService.indexExists(index);
		return success(indexExists);
	}

	@PostMapping("/createSuggest")
	@ApiOperation(value = "新增自动补全数据")
	@ApiImplicitParams({
	})
	public ResponseEntity createSuggest(@RequestBody PutSuggestEntity entity) {
		autoSuggestService.createSuggest(entity);
		return success();
	}

	@PostMapping("/search")
	@ApiOperation(value = "根据索引查询该索引对应的自动补全列表")
	public ResponseEntity<ResponseModel<PageModel<AutoSuggest>>> search(@RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
																	 @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
																		@RequestParam("index") String index) {
		PageModel<AutoSuggest> pageModel = autoSuggestService.search(pageable(pageNumber, pageSize), index);
		return success(pageModel);
	}

	@PutMapping
	@ApiOperation(value = "修改自动补全数据")
	@ApiImplicitParams({
	})
	public ResponseEntity<ResponseModel<AutoSuggest>> update(@RequestBody PutSuggestEntity entity) {
		autoSuggestService.update(entity);
		return success();
	}

	@GetMapping(value="/{index}/{id}")
	@ApiOperation(value = "详情")
	public ResponseEntity<ResponseModel<AutoSuggest>> info(@PathVariable String index, @PathVariable String id) {
		AutoSuggest t = autoSuggestService.getOne(index, id);
		return success(t);
	}

	@DeleteMapping("/{index}/{id}")
	@ApiOperation(value = "删除")
	@ApiImplicitParams({
	})
	public ResponseEntity<ResponseModel> delete(@PathVariable String index, @PathVariable String id) {
		autoSuggestService.delete(index, id);
		return success();
	}

}