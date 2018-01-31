package com.massestech.suggest.auto.service;

import com.massestech.core.base.es.service.ESBaseServiceImpl;
import com.massestech.core.base.model.PageModel;
import com.massestech.suggest.auto.controller.entity.PutSuggestEntity;
import com.massestech.suggest.auto.dal.model.AutoSuggest;
import com.massestech.suggest.auto.dal.repository.AutoSuggestRepository;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.SuggestionBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.DefaultResultMapper;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ResultsMapper;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AutoSuggestServiceImpl extends ESBaseServiceImpl<AutoSuggest> implements AutoSuggestService {

    @Autowired
    private AutoSuggestRepository autoSuggestRepository;
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    /** 索引type */
    private static final String type = "autosuggest";
    /** 自动补全字段名称 */
    private static final String fieldName = "queryString.suggest";

    @Override
    public List<String> suggest(String index, String queryString) {
        String suggestionName = index + "_" + type;
        // 设置查询的字段名称,以及查询的内容,另外completionSuggestionBuilder可以有多个.
        SuggestBuilder suggestBuilder = new SuggestBuilder();
        SuggestionBuilder completionSuggestionBuilder =
                SuggestBuilders.completionSuggestion(fieldName) // 字段名称
                .prefix(queryString)
                .size(10);
        suggestBuilder.addSuggestion(suggestionName, completionSuggestionBuilder); // 给建议器设置一个名字.
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.suggest(suggestBuilder);
        searchSourceBuilder.fetchSource("content", null);
//        log.info(searchSourceBuilder.toString());
        // 索引查询
        SearchResponse searchResponse = elasticsearchTemplate.suggest(suggestBuilder, index);
        Suggest suggest = searchResponse.getSuggest();
        // 根据建议器的名称获取对应的建议结果
        CompletionSuggestion completionSuggestion = suggest.getSuggestion(suggestionName);
        List<String> suggestList = new LinkedList<>();

        List<CompletionSuggestion.Entry.Option> options = completionSuggestion.getOptions();

        // 自动补全查询不到数据,那么就切换到phrase模式进行匹配,如果再找不到并且性能允许的情况下,切换到term模式.
        if (completionSuggestion.getEntries().size() == 0) {
            // 发现phrase以及term对于中文的支持不是很好,后续看看如何解决该问题.
        } else {
            for (CompletionSuggestion.Entry.Option option : options) {
                Map<String, Object> sourceAsMap = option.getHit().getSourceAsMap();
//                String suggestText = option.getText().string();
                String suggestText = (String) sourceAsMap.get("content");
                suggestList.add(suggestText);
            }
        }
        return suggestList;
    }

    @Override
    public void createIndex(String index) {
        elasticsearchTemplate.createIndex(index, indexMapping);
        elasticsearchTemplate.putMapping(index, type, suggestMapping);
    }

    @Override
    public boolean indexExists(String index) {
        return elasticsearchTemplate.indexExists(index);
    }

    @Override
    public void createSuggest(PutSuggestEntity entity) {
        // 将属性复制到autoSuggest之中.
        AutoSuggest autoSuggest = new AutoSuggest();
        BeanUtils.copyProperties(entity, autoSuggest);
        // 新增内容,手动设置索引以及类型.参考save的操作操作重写一遍.
        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setObject(autoSuggest);
        indexQuery.setIndexName(entity.getIndex());
        indexQuery.setType(type);
        elasticsearchTemplate.index(indexQuery);
        elasticsearchTemplate.refresh(entity.getIndex());
    }

    @Override
    public PageModel<AutoSuggest> search(Pageable pageable, String index) {
        MatchAllQueryBuilder queryBuilder = QueryBuilders.matchAllQuery();
        // 创建搜索 DSL 查询
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withPageable(pageable)
                .withIndices(index)
                .withTypes(type)
                .withQuery(queryBuilder).build();
        Page<AutoSuggest> searchPageResults = autoSuggestRepository.search(searchQuery);
        PageModel pageModel = new PageModel();
        pageModel.setContent(searchPageResults.getContent());
        pageModel.setTotalElements(searchPageResults.getTotalElements());
        return pageModel;
    }

    @Override
    public void delete(String index, String id) {
        elasticsearchTemplate.delete(index, type, id);
        elasticsearchTemplate.refresh(index);
    }

    @Override
    public AutoSuggest getOne(String index, String id) {
        Client client = elasticsearchTemplate.getClient();
        // 转换器
        MappingElasticsearchConverter elasticsearchConverter = new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext());
        // 结果映射器
        ResultsMapper mapper = new DefaultResultMapper(elasticsearchConverter.getMappingContext());
        GetResponse response = client.prepareGet(index, type, id).execute().actionGet();
        AutoSuggest entity = mapper.mapResult(response, AutoSuggest.class);
        return entity;
    }

    @Override
    public void update(PutSuggestEntity entity) {
        this.createSuggest(entity);
    }

    /** 索引setting */
    private static final String indexMapping = "{\n" +
            "  \"index\": {\n" +
            "    \"analysis\": {\n" +
            "      \"analyzer\": {\n" +
            "        \"pinyin_analyzer\": {\n" +
            "          \"tokenizer\": \"my_pinyin_tokenizer\"\n" +
            "        },\n" +
            "        \"ik_pinyin_analyzer\": {\n" +
            "          \"type\": \"custom\",\n" +
            "          \"tokenizer\": \"ik_max_word\",\n" +
            "          \"filter\": [\n" +
            "            \"my_pinyin\",\n" +
            "            \"word_delimiter\"\n" +
            "          ]\n" +
            "        }\n" +
            "      },\n" +
            "      \"tokenizer\": {\n" +
            "        \"my_pinyin_tokenizer\": {\n" +
            "          \"type\": \"pinyin\",\n" +
            "          \"keep_full_pinyin\": true,\n" +
            "          \"keep_original\": true,\n" +
            "          \"limit_first_letter_length\": 100,\n" +
            "          \"lowercase\": true,\n" +
            "          \"remove_duplicated_term\": true\n" +
            "        }\n" +
            "      },\n" +
            "      \"filter\": {\n" +
            "        \"my_pinyin\": {\n" +
            "          \"type\": \"pinyin\",\n" +
            "          \"keep_separate_first_letter\": false,\n" +
            "          \"keep_full_pinyin\": true,\n" +
            "          \"keep_original\": true,\n" +
            "          \"limit_first_letter_length\": 16,\n" +
            "          \"lowercase\": true,\n" +
            "          \"remove_duplicated_term\": true\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    /** 映射 */
    private static final String suggestMapping = "{\n" +
            "  \"autosuggest\": {\n" +
            "    \"properties\": {\n" +
            "      \"queryString\": {\n" +
            "        \"type\": \"text\",\n" +
            "        \"analyzer\": \"ik_max_word\",\n" +
            "        \"fields\": {\n" +
            "          \"suggest\": {\n" +
            "            \"type\": \"completion\",\n" +
            "            \"analyzer\": \"ik_pinyin_analyzer\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      \"content\": {\n" +
            "        \"type\": \"text\",\n" +
            "        \"fields\": {\n" +
            "          \"keyword\": {\n" +
            "            \"type\": \"keyword\",\n" +
            "            \"ignore_above\": 256\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
}