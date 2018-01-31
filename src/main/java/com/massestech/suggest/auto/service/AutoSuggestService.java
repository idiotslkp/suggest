package com.massestech.suggest.auto.service;


import com.massestech.core.base.es.service.ESBaseService;
import com.massestech.core.base.model.PageModel;
import com.massestech.suggest.auto.controller.entity.PutSuggestEntity;
import com.massestech.suggest.auto.dal.model.AutoSuggest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AutoSuggestService extends ESBaseService<AutoSuggest> {

    /**
     * 自动补全功能(目前只支持前缀补全)
     * @param index 索引
     * @param queryString 自动补全前缀
     * @return 建议列表,只返回前10条
     */
    List<String> suggest(String index, String queryString);

    /**
     * 创建索引
     * @param index 索引
     */
    void createIndex(String index);

    /**
     * 判断索引是否存在
     * @param index 索引
     */
    boolean indexExists(String index);

    /**
     * 新增一个自动补全内容
     * @param entity 自动补全entity
     */
    void createSuggest(PutSuggestEntity entity);

    PageModel<AutoSuggest> search(Pageable pageable, String index);

    /**
     * 根据索引index以及id,删除自动补全
     */
    void delete(String index, String id);

    /**
     * 根据索引以及id获取单条详情
     */
    AutoSuggest getOne(String index, String id);

    /**
     * 修改数据
     * @param entity
     */
    void update(PutSuggestEntity entity);
}