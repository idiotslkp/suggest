/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder.Item;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.facet.FacetRequest;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.util.CloseableIterator;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.elasticsearch.client.Requests.indicesExistsRequest;
import static org.elasticsearch.client.Requests.refreshRequest;
import static org.elasticsearch.index.VersionType.EXTERNAL;
import static org.springframework.data.elasticsearch.core.MappingBuilder.buildMapping;
import static org.springframework.util.CollectionUtils.isEmpty;

//import org.elasticsearch.action.count.CountRequestBuilder;
//import org.elasticsearch.action.suggest.SuggestRequestBuilder;
//import org.elasticsearch.action.suggest.SuggestResponse;
//import static org.elasticsearch.action.search.SearchType.SCAN;
//import static org.elasticsearch.cluster.metadata.AliasAction.Type.*;
//import static org.elasticsearch.cluster.metadata.AliasAction.Type.ADD;

/**
 * ElasticsearchTemplate
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Kevin Leturc
 * @author Mason Chan
 * @author Young Gu
 * @author Oliver Gierke
 * @author Mark Janssen
 */

public class ElasticsearchTemplate implements ElasticsearchOperations, ApplicationContextAware {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchTemplate.class);
	private Client client;
	private ElasticsearchConverter elasticsearchConverter;
	private ResultsMapper resultsMapper;
	private String searchTimeout;

	public ElasticsearchTemplate(Client client) {
		this(client, new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()));
	}

	public ElasticsearchTemplate(Client client, EntityMapper entityMapper) {
		this(client, new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()), entityMapper);
	}

	public ElasticsearchTemplate(Client client, ElasticsearchConverter elasticsearchConverter, EntityMapper entityMapper) {
		this(client, elasticsearchConverter, new DefaultResultMapper(elasticsearchConverter.getMappingContext(), entityMapper));
	}

	public ElasticsearchTemplate(Client client, ResultsMapper resultsMapper) {
		this(client, new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()), resultsMapper);
	}

	public ElasticsearchTemplate(Client client, ElasticsearchConverter elasticsearchConverter) {
		this(client, elasticsearchConverter, new DefaultResultMapper(elasticsearchConverter.getMappingContext()));
	}

	public ElasticsearchTemplate(Client client, ElasticsearchConverter elasticsearchConverter, ResultsMapper resultsMapper) {

		Assert.notNull(client, "Client must not be null!");
		Assert.notNull(elasticsearchConverter, "ElasticsearchConverter must not be null!");
		Assert.notNull(resultsMapper, "ResultsMapper must not be null!");

		this.client = client;
		this.elasticsearchConverter = elasticsearchConverter;
		this.resultsMapper = resultsMapper;
	}

	@Override
	public Client getClient() {
		return client;
	}

	public void setSearchTimeout(String searchTimeout) {
		this.searchTimeout = searchTimeout;
	}

	@Override
	public <T> boolean createIndex(Class<T> clazz) {
		return createIndexIfNotCreated(clazz);
	}

	@Override
	public boolean createIndex(String indexName) {
		Assert.notNull(indexName, "No index defined for Query");
		return client.admin().indices()
				.create(Requests.createIndexRequest(indexName))
				.actionGet().isAcknowledged();
	}

	@Override
	public <T> boolean putMapping(Class<T> clazz) {
		if (clazz.isAnnotationPresent(Mapping.class)) {
			String mappingPath = clazz.getAnnotation(Mapping.class).mappingPath();
			if (isNotBlank(mappingPath)) {
				String mappings = readFileFromClasspath(mappingPath);
				if (isNotBlank(mappings)) {
					return putMapping(clazz, mappings);
				}
			} else {
				logger.info("mappingPath in @Mapping has to be defined. Building mappings using @Field");
			}
		}
		ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
		XContentBuilder xContentBuilder = null;
		try {
			xContentBuilder = buildMapping(clazz, persistentEntity.getIndexType(), persistentEntity
					.getIdProperty().getFieldName(), persistentEntity.getParentType());
		} catch (Exception e) {
			throw new ElasticsearchException("Failed to build mapping for " + clazz.getSimpleName(), e);
		}
		return putMapping(clazz, xContentBuilder);
	}

	@Override
	public <T> boolean putMapping(Class<T> clazz, Object mapping) {
		return putMapping(getPersistentEntityFor(clazz).getIndexName(), getPersistentEntityFor(clazz).getIndexType(), mapping);
	}

	@Override
	public boolean putMapping(String indexName, String type, Object mapping) {
		Assert.notNull(indexName, "No index defined for putMapping()");
		Assert.notNull(type, "No type defined for putMapping()");
		PutMappingRequestBuilder requestBuilder = client.admin().indices()
				.preparePutMapping(indexName).setType(type);
		if (mapping instanceof String) {
			// 这里我们使用json
//			requestBuilder.setSource(String.valueOf(mapping));
			requestBuilder.setSource(String.valueOf(mapping), XContentType.JSON);
		} else if (mapping instanceof Map) {
			requestBuilder.setSource((Map) mapping);
		} else if (mapping instanceof XContentBuilder) {
			requestBuilder.setSource((XContentBuilder) mapping);
		}
		return requestBuilder.execute().actionGet().isAcknowledged();
	}

	@Override
	public Map getMapping(String indexName, String type) {
		Assert.notNull(indexName, "No index defined for putMapping()");
		Assert.notNull(type, "No type defined for putMapping()");
		Map mappings = null;
		try {
			mappings = client.admin().indices().getMappings(new GetMappingsRequest().indices(indexName).types(type))
					.actionGet().getMappings().get(indexName).get(type).getSourceAsMap();
		} catch (Exception e) {
			throw new ElasticsearchException("Error while getting mapping for indexName : " + indexName + " type : " + type + " " + e.getMessage());
		}
		return mappings;
	}

	@Override
	public <T> Map getMapping(Class<T> clazz) {
		return getMapping(getPersistentEntityFor(clazz).getIndexName(), getPersistentEntityFor(clazz).getIndexType());
	}

	@Override
	public ElasticsearchConverter getElasticsearchConverter() {
		return elasticsearchConverter;
	}

	@Override
	public <T> T queryForObject(GetQuery query, Class<T> clazz) {
		return queryForObject(query, clazz, resultsMapper);
	}

	@Override
	public <T> T queryForObject(GetQuery query, Class<T> clazz, GetResultMapper mapper) {
		ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
		GetResponse response = client
				.prepareGet(persistentEntity.getIndexName(), persistentEntity.getIndexType(), query.getId()).execute()
				.actionGet();

		T entity = mapper.mapResult(response, clazz);
		return entity;
	}

	@Override
	public <T> T queryForObject(CriteriaQuery query, Class<T> clazz) {
		Page<T> page = queryForPage(query, clazz);
		Assert.isTrue(page.getTotalElements() < 2, "Expected 1 but found " + page.getTotalElements() + " results");
		return page.getTotalElements() > 0 ? page.getContent().get(0) : null;
	}

	@Override
	public <T> T queryForObject(StringQuery query, Class<T> clazz) {
		Page<T> page = queryForPage(query, clazz);
		Assert.isTrue(page.getTotalElements() < 2, "Expected 1 but found " + page.getTotalElements() + " results");
		return page.getTotalElements() > 0 ? page.getContent().get(0) : null;
	}

	@Override
	public <T> AggregatedPage<T> queryForPage(SearchQuery query, Class<T> clazz) {
		return queryForPage(query, clazz, resultsMapper);
	}

	@Override
	public <T> AggregatedPage<T> queryForPage(SearchQuery query, Class<T> clazz, SearchResultMapper mapper) {
		SearchResponse response = doSearch(prepareSearch(query, clazz), query);
		return mapper.mapResults(response, clazz, query.getPageable());
	}

	@Override
	public <T> T query(SearchQuery query, ResultsExtractor<T> resultsExtractor) {
		SearchResponse response = doSearch(prepareSearch(query), query);
		return resultsExtractor.extract(response);
	}

	@Override
	public <T> List<T> queryForList(CriteriaQuery query, Class<T> clazz) {
		return queryForPage(query, clazz).getContent();
	}

	@Override
	public <T> List<T> queryForList(StringQuery query, Class<T> clazz) {
		return queryForPage(query, clazz).getContent();
	}

	@Override
	public <T> List<T> queryForList(SearchQuery query, Class<T> clazz) {
		return queryForPage(query, clazz).getContent();
	}

	@Override
	public <T> List<String> queryForIds(SearchQuery query) {
		SearchRequestBuilder request = prepareSearch(query).setQuery(query.getQuery()).storedFields();
		if (query.getFilter() != null) {
			request.setPostFilter(query.getFilter());
		}
		SearchResponse response = getSearchResponse(request.execute());
		return extractIds(response);
	}

	@Override
	public <T> Page<T> queryForPage(CriteriaQuery criteriaQuery, Class<T> clazz) {
		QueryBuilder elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(criteriaQuery.getCriteria());
		QueryBuilder elasticsearchFilter = new CriteriaFilterProcessor().createFilterFromCriteria(criteriaQuery.getCriteria());
		SearchRequestBuilder searchRequestBuilder = prepareSearch(criteriaQuery, clazz);

		if (elasticsearchQuery != null) {
			searchRequestBuilder.setQuery(elasticsearchQuery);
		} else {
			searchRequestBuilder.setQuery(QueryBuilders.matchAllQuery());
		}

		if (criteriaQuery.getMinScore() > 0) {
			searchRequestBuilder.setMinScore(criteriaQuery.getMinScore());
		}

		if (elasticsearchFilter != null)
			searchRequestBuilder.setPostFilter(elasticsearchFilter);
		if (logger.isDebugEnabled()) {
			logger.debug("doSearch query:\n" + searchRequestBuilder.toString());
		}

		SearchResponse response = getSearchResponse(searchRequestBuilder
				.execute());
		return resultsMapper.mapResults(response, clazz, criteriaQuery.getPageable());
	}

	@Override
	public <T> Page<T> queryForPage(StringQuery query, Class<T> clazz) {
		return queryForPage(query, clazz, resultsMapper);
	}

	@Override
	public <T> Page<T> queryForPage(StringQuery query, Class<T> clazz, SearchResultMapper mapper) {
		SearchResponse response = getSearchResponse(prepareSearch(query, clazz).setQuery(QueryBuilders.queryStringQuery(query.getSource())).execute());
//		SearchResponse response = getSearchResponse(prepareSearch(query, clazz).setQuery(query.getSource()).execute());
		return mapper.mapResults(response, clazz, query.getPageable());
	}

	@Override
	public <T> CloseableIterator<T> stream(CriteriaQuery query, Class<T> clazz) {
		final long scrollTimeInMillis = TimeValue.timeValueMinutes(1).millis();
		final String initScrollId = scan(query, scrollTimeInMillis, false, clazz);
		return doStream(initScrollId, scrollTimeInMillis, clazz, resultsMapper);
	}

	@Override
	public <T> CloseableIterator<T> stream(SearchQuery query, Class<T> clazz) {
		return stream(query, clazz, resultsMapper);
	}

	@Override
	public <T> CloseableIterator<T> stream(SearchQuery query, final Class<T> clazz, final SearchResultMapper mapper) {
		final long scrollTimeInMillis = TimeValue.timeValueMinutes(1).millis();
		final String initScrollId = scan(query, scrollTimeInMillis, false, clazz);
		return doStream(initScrollId, scrollTimeInMillis, clazz, mapper);
	}

	private <T> CloseableIterator<T> doStream(final String initScrollId, final long scrollTimeInMillis, final Class<T> clazz, final SearchResultMapper mapper) {
		return new CloseableIterator<T>() {

			/** As we couldn't retrieve single result with scroll, store current hits. */
			private volatile Iterator<T> currentHits;

			/** The scroll id. */
			private volatile String scrollId = initScrollId;

			/** If stream is finished (ie: cluster returns no results. */
			private volatile boolean finished;

			@Override
			public void close() {
				try {
					// Clear scroll on cluster only in case of error (cause elasticsearch auto clear scroll when it's done)
					if (!finished && scrollId != null && currentHits != null && currentHits.hasNext()) {
						client.prepareClearScroll().addScrollId(scrollId).execute().actionGet();
					}
				} finally {
					currentHits = null;
					scrollId = null;
				}
			}

			@Override
			public boolean hasNext() {
				// Test if stream is finished
				if (finished) {
					return false;
				}
				// Test if it remains hits
				if (currentHits == null || !currentHits.hasNext()) {
					// Do a new request
					SearchResponse response = getSearchResponse(client.prepareSearchScroll(scrollId)
							.setScroll(TimeValue.timeValueMillis(scrollTimeInMillis)).execute());
					// Save hits and scroll id
					currentHits = mapper.mapResults(response, clazz, null).iterator();
					finished = !currentHits.hasNext();
					scrollId = response.getScrollId();
				}
				return currentHits.hasNext();
			}

			@Override
			public T next() {
				if (hasNext()) {
					return currentHits.next();
				}
				throw new NoSuchElementException();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("remove");
			}
		};
	}

	@Override
	public <T> long count(CriteriaQuery criteriaQuery, Class<T> clazz) {
		QueryBuilder elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(criteriaQuery.getCriteria());
		QueryBuilder elasticsearchFilter = new CriteriaFilterProcessor().createFilterFromCriteria(criteriaQuery.getCriteria());

		if (elasticsearchFilter == null) {
			return doCount(prepareCount(criteriaQuery, clazz), elasticsearchQuery);
		} else {
			// filter could not be set into CountRequestBuilder, convert request into search request
			return doCount(prepareSearch(criteriaQuery, clazz), elasticsearchQuery, elasticsearchFilter);
		}
	}

	@Override
	public <T> long count(SearchQuery searchQuery, Class<T> clazz) {
		QueryBuilder elasticsearchQuery = searchQuery.getQuery();
		QueryBuilder elasticsearchFilter = searchQuery.getFilter();

		if (elasticsearchFilter == null) {
			return doCount(prepareCount(searchQuery, clazz), elasticsearchQuery);
		} else {
			// filter could not be set into CountRequestBuilder, convert request into search request
			return doCount(prepareSearch(searchQuery, clazz), elasticsearchQuery, elasticsearchFilter);
		}
	}

	@Override
	public <T> long count(CriteriaQuery query) {
		return count(query, null);
	}

	@Override
	public <T> long count(SearchQuery query) {
		return count(query, null);
	}

	private long doCount(SearchRequestBuilder countRequestBuilder, QueryBuilder elasticsearchQuery) {
		if (elasticsearchQuery != null) {
			countRequestBuilder.setQuery(elasticsearchQuery);
		}
		
		return countRequestBuilder.execute().actionGet().getHits().getTotalHits();
//		return countRequestBuilder.execute().actionGet().getCount();
	}

	private long doCount(SearchRequestBuilder searchRequestBuilder, QueryBuilder elasticsearchQuery, QueryBuilder elasticsearchFilter) {
		if (elasticsearchQuery != null) {
			searchRequestBuilder.setQuery(elasticsearchQuery);
		} else {
			searchRequestBuilder.setQuery(QueryBuilders.matchAllQuery());
		}
		if (elasticsearchFilter != null) {
			searchRequestBuilder.setPostFilter(elasticsearchFilter);
		}
//		searchRequestBuilder.setSearchType(SearchType.COUNT);
		searchRequestBuilder.setSearchType(SearchType.QUERY_THEN_FETCH);
		return searchRequestBuilder.execute().actionGet().getHits().getTotalHits();
	}

	private <T> SearchRequestBuilder prepareCount(Query query, Class<T> clazz) {
		String indexName[] = !isEmpty(query.getIndices()) ? query.getIndices().toArray(new String[query.getIndices().size()]) : retrieveIndexNameFromPersistentEntity(clazz);
		String types[] = !isEmpty(query.getTypes()) ? query.getTypes().toArray(new String[query.getTypes().size()]) : retrieveTypeFromPersistentEntity(clazz);

		Assert.notNull(indexName, "No index defined for Query");

//		CountRequestBuilder countRequestBuilder = client.prepareCount(indexName);
		SearchRequestBuilder countRequestBuilder = client.prepareSearch(indexName);
//		SearchRequestBuilder

		if (types != null) {
			countRequestBuilder.setTypes(types);
		}
		return countRequestBuilder;
	}

	@Override
	public <T> LinkedList<T> multiGet(SearchQuery searchQuery, Class<T> clazz) {
		return resultsMapper.mapResults(getMultiResponse(searchQuery, clazz), clazz);
	}

	private <T> MultiGetResponse getMultiResponse(Query searchQuery, Class<T> clazz) {

		String indexName = !isEmpty(searchQuery.getIndices()) ? searchQuery.getIndices().get(0) : getPersistentEntityFor(clazz).getIndexName();
		String type = !isEmpty(searchQuery.getTypes()) ? searchQuery.getTypes().get(0) : getPersistentEntityFor(clazz).getIndexType();

		Assert.notNull(indexName, "No index defined for Query");
		Assert.notNull(type, "No type define for Query");
		Assert.notEmpty(searchQuery.getIds(), "No Id define for Query");

		MultiGetRequestBuilder builder = client.prepareMultiGet();

		for (String id : searchQuery.getIds()) {

			MultiGetRequest.Item item = new MultiGetRequest.Item(indexName, type, id);

			if (searchQuery.getRoute() != null) {
				item = item.routing(searchQuery.getRoute());
			}

			if (searchQuery.getFields() != null && !searchQuery.getFields().isEmpty()) {
				item = item.storedFields(toArray(searchQuery.getFields()));
			}
			builder.add(item);
		}
		return builder.execute().actionGet();
	}

	@Override
	public <T> LinkedList<T> multiGet(SearchQuery searchQuery, Class<T> clazz, MultiGetResultMapper getResultMapper) {
		return getResultMapper.mapResults(getMultiResponse(searchQuery, clazz), clazz);
	}

	@Override
	public String index(IndexQuery query) {
		String documentId = prepareIndex(query).execute().actionGet().getId();
		// We should call this because we are not going through a mapper.
		if (query.getObject() != null) {
			setPersistentEntityId(query.getObject(), documentId);
		}
		return documentId;
	}

	@Override
	public UpdateResponse update(UpdateQuery query) {
		return this.prepareUpdate(query).execute().actionGet();
	}

	private UpdateRequestBuilder prepareUpdate(UpdateQuery query) {
		String indexName = isNotBlank(query.getIndexName()) ? query.getIndexName() : getPersistentEntityFor(query.getClazz()).getIndexName();
		String type = isNotBlank(query.getType()) ? query.getType() : getPersistentEntityFor(query.getClazz()).getIndexType();
		Assert.notNull(indexName, "No index defined for Query");
		Assert.notNull(type, "No type define for Query");
		Assert.notNull(query.getId(), "No Id define for Query");
		Assert.notNull(query.getUpdateRequest(), "No IndexRequest define for Query");
		UpdateRequestBuilder updateRequestBuilder = client.prepareUpdate(indexName, type, query.getId());
		updateRequestBuilder.setRouting(query.getUpdateRequest().routing());

		if (query.getUpdateRequest().script() == null) {
			// doc
			if (query.DoUpsert()) {
				updateRequestBuilder.setDocAsUpsert(true)
						.setDoc(query.getUpdateRequest().doc());
			} else {
				updateRequestBuilder.setDoc(query.getUpdateRequest().doc());
			}
		} else {
			// or script
			updateRequestBuilder.setScript(query.getUpdateRequest().script());
		}

		return updateRequestBuilder;
	}

	@Override
	public void bulkIndex(List<IndexQuery> queries) {
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		for (IndexQuery query : queries) {
			bulkRequest.add(prepareIndex(query));
		}
		BulkResponse bulkResponse = bulkRequest.execute().actionGet();
		if (bulkResponse.hasFailures()) {
			Map<String, String> failedDocuments = new HashMap<String, String>();
			for (BulkItemResponse item : bulkResponse.getItems()) {
				if (item.isFailed())
					failedDocuments.put(item.getId(), item.getFailureMessage());
			}
			throw new ElasticsearchException(
					"Bulk indexing has failures. Use ElasticsearchException.getFailedDocuments() for detailed messages ["
							+ failedDocuments + "]", failedDocuments
			);
		}
	}

	@Override
	public void bulkUpdate(List<UpdateQuery> queries) {
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		for (UpdateQuery query : queries) {
			bulkRequest.add(prepareUpdate(query));
		}
		BulkResponse bulkResponse = bulkRequest.execute().actionGet();
		if (bulkResponse.hasFailures()) {
			Map<String, String> failedDocuments = new HashMap<String, String>();
			for (BulkItemResponse item : bulkResponse.getItems()) {
				if (item.isFailed())
					failedDocuments.put(item.getId(), item.getFailureMessage());
			}
			throw new ElasticsearchException(
					"Bulk indexing has failures. Use ElasticsearchException.getFailedDocuments() for detailed messages ["
							+ failedDocuments + "]", failedDocuments
			);
		}
	}

	@Override
	public <T> boolean indexExists(Class<T> clazz) {
		return indexExists(getPersistentEntityFor(clazz).getIndexName());
	}

	@Override
	public boolean indexExists(String indexName) {
		return client.admin().indices().exists(indicesExistsRequest(indexName)).actionGet().isExists();
	}

	@Override
	public boolean typeExists(String index, String type) {
		return client.admin().cluster().prepareState().execute().actionGet()
				.getState().metaData().index(index).getMappings().containsKey(type);
	}

	@Override
	public <T> boolean deleteIndex(Class<T> clazz) {
		return deleteIndex(getPersistentEntityFor(clazz).getIndexName());
	}

	@Override
	public boolean deleteIndex(String indexName) {
		Assert.notNull(indexName, "No index defined for delete operation");
		if (indexExists(indexName)) {
			return client.admin().indices().delete(new DeleteIndexRequest(indexName)).actionGet().isAcknowledged();
		}
		return false;
	}

	@Override
	public String delete(String indexName, String type, String id) {
		return client.prepareDelete(indexName, type, id).execute().actionGet().getId();
	}

	@Override
	public <T> String delete(Class<T> clazz, String id) {
		ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(clazz);
		return delete(persistentEntity.getIndexName(), persistentEntity.getIndexType(), id);
	}

	@Override
	public <T> void delete(DeleteQuery deleteQuery, Class<T> clazz) {

		String indexName = isNotBlank(deleteQuery.getIndex()) ? deleteQuery.getIndex() : getPersistentEntityFor(clazz).getIndexName();
		String typeName = isNotBlank(deleteQuery.getType()) ? deleteQuery.getType() : getPersistentEntityFor(clazz).getIndexType();
		Integer pageSize = deleteQuery.getPageSize() != null ? deleteQuery.getPageSize() : 1000;
		Long scrollTimeInMillis = deleteQuery.getScrollTimeInMillis() != null ? deleteQuery.getScrollTimeInMillis() : 10000l;

		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(deleteQuery.getQuery())
				.withIndices(indexName)
				.withTypes(typeName)
				.withPageable(new PageRequest(0, pageSize))
				.build();

		String scrollId = scan(searchQuery, scrollTimeInMillis, true);

		BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
		List<String> ids = new ArrayList<String>();
		boolean hasRecords = true;
		while (hasRecords) {
			Page<String> page = scroll(scrollId, scrollTimeInMillis, new SearchResultMapper() {
				@Override
				public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
					List<String> result = new ArrayList<String>();
					for (SearchHit searchHit : response.getHits()) {
						String id = searchHit.getId();
						result.add(id);
					}
					if (result.size() > 0) {
						return new AggregatedPageImpl<T>((List<T>) result);
					}
					return null;
				}
			});
			if (page != null && page.getContent().size() > 0) {
				ids.addAll(page.getContent());
			} else {
				hasRecords = false;
			}
		}

		for(String id : ids) {
			bulkRequestBuilder.add(client.prepareDelete(indexName, typeName, id));
		}

		if(bulkRequestBuilder.numberOfActions() > 0) {
			bulkRequestBuilder.execute().actionGet();
		}

		clearScroll(scrollId);
	}

	@Override
	public void delete(DeleteQuery deleteQuery) {
		Assert.notNull(deleteQuery.getIndex(), "No index defined for Query");
		Assert.notNull(deleteQuery.getType(), "No type define for Query");
		delete(deleteQuery, null);
	}

	@Override
	public <T> void delete(CriteriaQuery criteriaQuery, Class<T> clazz) {
		QueryBuilder elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(criteriaQuery.getCriteria());
		Assert.notNull(elasticsearchQuery, "Query can not be null.");
		DeleteQuery deleteQuery = new DeleteQuery();
		deleteQuery.setQuery(elasticsearchQuery);
		delete(deleteQuery, clazz);
	}

	@Override
	public String scan(CriteriaQuery criteriaQuery, long scrollTimeInMillis, boolean noFields) {
		return doScan(prepareScan(criteriaQuery, scrollTimeInMillis, noFields), criteriaQuery);
	}

	@Override
	public <T> String scan(CriteriaQuery criteriaQuery, long scrollTimeInMillis, boolean noFields, Class<T> clazz) {
		return doScan(prepareScan(criteriaQuery, scrollTimeInMillis, noFields, clazz), criteriaQuery);
	}

	@Override
	public String scan(SearchQuery searchQuery, long scrollTimeInMillis, boolean noFields) {
		return doScan(prepareScan(searchQuery, scrollTimeInMillis, noFields), searchQuery);
	}

	@Override
	public <T> String scan(SearchQuery searchQuery, long scrollTimeInMillis, boolean noFields, Class<T> clazz) {
		return doScan(prepareScan(searchQuery, scrollTimeInMillis, noFields, clazz), searchQuery);
	}

	private <T> SearchRequestBuilder prepareScan(Query query, long scrollTimeInMillis, boolean noFields, Class<T> clazz) {
		setPersistentEntityIndexAndType(query, clazz);
		return prepareScan(query, scrollTimeInMillis, noFields);
	}

	private SearchRequestBuilder prepareScan(Query query, long scrollTimeInMillis, boolean noFields) {
//		SearchRequestBuilder requestBuilder = client.prepareSearch(toArray(query.getIndices())).setSearchType(SCAN)
		SearchRequestBuilder requestBuilder = client.prepareSearch(toArray(query.getIndices())).setSearchType(SearchType.DEFAULT)
				.setTypes(toArray(query.getTypes()))
				.setScroll(TimeValue.timeValueMillis(scrollTimeInMillis)).setFrom(0)
				.setSize(query.getPageable().getPageSize());

		if (!isEmpty(query.getFields())) {
			requestBuilder.storedFields(toArray(query.getFields()));
		}

		if (noFields) {
			requestBuilder.storedFields();
		}
		return requestBuilder;
	}

	private String doScan(SearchRequestBuilder requestBuilder, CriteriaQuery criteriaQuery) {
		Assert.notNull(criteriaQuery.getIndices(), "No index defined for Query");
		Assert.notNull(criteriaQuery.getTypes(), "No type define for Query");
		Assert.notNull(criteriaQuery.getPageable(), "Query.pageable is required for scan & scroll");

		QueryBuilder elasticsearchQuery = new CriteriaQueryProcessor().createQueryFromCriteria(criteriaQuery.getCriteria());
		QueryBuilder elasticsearchFilter = new CriteriaFilterProcessor().createFilterFromCriteria(criteriaQuery.getCriteria());

		if (elasticsearchQuery != null) {
			requestBuilder.setQuery(elasticsearchQuery);
		} else {
			requestBuilder.setQuery(QueryBuilders.matchAllQuery());
		}

		if (elasticsearchFilter != null) {
			requestBuilder.setPostFilter(elasticsearchFilter);
		}

		return getSearchResponse(requestBuilder.execute()).getScrollId();
	}

	private String doScan(SearchRequestBuilder requestBuilder, SearchQuery searchQuery) {
		Assert.notNull(searchQuery.getIndices(), "No index defined for Query");
		Assert.notNull(searchQuery.getTypes(), "No type define for Query");
		Assert.notNull(searchQuery.getPageable(), "Query.pageable is required for scan & scroll");

		if (searchQuery.getFilter() != null) {
			requestBuilder.setPostFilter(searchQuery.getFilter());
		}

		return getSearchResponse(requestBuilder.setQuery(searchQuery.getQuery()).execute()).getScrollId();
	}

	@Override
	public <T> Page<T> scroll(String scrollId, long scrollTimeInMillis, Class<T> clazz) {
		SearchResponse response = getSearchResponse(client.prepareSearchScroll(scrollId)
				.setScroll(TimeValue.timeValueMillis(scrollTimeInMillis)).execute());
		return resultsMapper.mapResults(response, clazz, null);
	}

	@Override
	public <T> Page<T> scroll(String scrollId, long scrollTimeInMillis, SearchResultMapper mapper) {
		SearchResponse response = getSearchResponse(client.prepareSearchScroll(scrollId)
				.setScroll(TimeValue.timeValueMillis(scrollTimeInMillis)).execute());
		return mapper.mapResults(response, null, null);
	}

	@Override
	public void clearScroll(String scrollId) {
		client.prepareClearScroll().addScrollId(scrollId).execute().actionGet();
	}

	@Override
	public <T> Page<T> moreLikeThis(MoreLikeThisQuery query, Class<T> clazz) {

		ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(clazz);
		String indexName = isNotBlank(query.getIndexName()) ? query.getIndexName() : persistentEntity.getIndexName();
		String type = isNotBlank(query.getType()) ? query.getType() : persistentEntity.getIndexType();

		Assert.notNull(indexName, "No 'indexName' defined for MoreLikeThisQuery");
		Assert.notNull(type, "No 'type' defined for MoreLikeThisQuery");
		Assert.notNull(query.getId(), "No document id defined for MoreLikeThisQuery");
		
		Item item = new Item(indexName, type, query.getId());
		Item[] items = new Item[1];
		items[0] = item;
		// 这里有bug......这个构造方法目前不能用,会报错,因为likeTexts不能为空,暂时用不到.后续出现问题再考虑怎么修改
		MoreLikeThisQueryBuilder moreLikeThisQueryBuilder = new MoreLikeThisQueryBuilder(null, items);
		
		
		
//		MoreLikeThisQueryBuilder moreLikeThisQueryBuilder = MoreLikeThisQueryBuilder.moreLikeThisQuery()
//				.addLikeItem(new MoreLikeThisQueryBuilder.Item(indexName, type, query.getId()));

		if (query.getMinTermFreq() != null) {
			moreLikeThisQueryBuilder.minTermFreq(query.getMinTermFreq());
		}
		if (query.getMaxQueryTerms() != null) {
			moreLikeThisQueryBuilder.maxQueryTerms(query.getMaxQueryTerms());
		}
		if (!isEmpty(query.getStopWords())) {
			moreLikeThisQueryBuilder.stopWords(toArray(query.getStopWords()));
		}
		if (query.getMinDocFreq() != null) {
			moreLikeThisQueryBuilder.minDocFreq(query.getMinDocFreq());
		}
		if (query.getMaxDocFreq() != null) {
			moreLikeThisQueryBuilder.maxDocFreq(query.getMaxDocFreq());
		}
		if (query.getMinWordLen() != null) {
			moreLikeThisQueryBuilder.minWordLength(query.getMinWordLen());
		}
		if (query.getMaxWordLen() != null) {
			moreLikeThisQueryBuilder.maxWordLength(query.getMaxWordLen());
		}
		if (query.getBoostTerms() != null) {
			moreLikeThisQueryBuilder.boostTerms(query.getBoostTerms());
		}

		return queryForPage(new NativeSearchQueryBuilder().withQuery(moreLikeThisQueryBuilder).build(), clazz);
	}

	private SearchResponse doSearch(SearchRequestBuilder searchRequest, SearchQuery searchQuery) {
		if (searchQuery.getFilter() != null) {
			searchRequest.setPostFilter(searchQuery.getFilter());
		}

		if (!isEmpty(searchQuery.getElasticsearchSorts())) {
			for (SortBuilder sort : searchQuery.getElasticsearchSorts()) {
				searchRequest.addSort(sort);
			}
		}

		if (!searchQuery.getScriptFields().isEmpty()) {
//			searchRequest.addField("_source");

			searchRequest.storedFields("_source");
			for (ScriptField scriptedField : searchQuery.getScriptFields()) {
				searchRequest.addScriptField(scriptedField.fieldName(), scriptedField.script());
			}
		}

		// 高亮的这块先注释掉......
//		if (searchQuery.getHighlightFields() != null) {
//			for (HighlightBuilder.Field highlightField : searchQuery.getHighlightFields()) {
//				searchRequest.addHighlightedField(highlightField);
//			}
//		}

		if (!isEmpty(searchQuery.getIndicesBoost())) {
			for (IndexBoost indexBoost : searchQuery.getIndicesBoost()) {
				searchRequest.addIndexBoost(indexBoost.getIndexName(), indexBoost.getBoost());
			}
		}

		if (!isEmpty(searchQuery.getAggregations())) {
			for (AbstractAggregationBuilder aggregationBuilder : searchQuery.getAggregations()) {
				searchRequest.addAggregation(aggregationBuilder);
			}
		}

		if (!isEmpty(searchQuery.getFacets())) {
			for (FacetRequest aggregatedFacet : searchQuery.getFacets()) {
				searchRequest.addAggregation(aggregatedFacet.getFacet());
			}
		}
		return getSearchResponse(searchRequest.setQuery(searchQuery.getQuery()).execute());
	}

	private SearchResponse getSearchResponse(ActionFuture<SearchResponse> response) {
		return searchTimeout == null ? response.actionGet() : response.actionGet(searchTimeout);
	}

	private <T> boolean createIndexIfNotCreated(Class<T> clazz) {
		return indexExists(getPersistentEntityFor(clazz).getIndexName()) || createIndexWithSettings(clazz);
	}

	private <T> boolean createIndexWithSettings(Class<T> clazz) {
		if (clazz.isAnnotationPresent(Setting.class)) {
			String settingPath = clazz.getAnnotation(Setting.class).settingPath();
			if (isNotBlank(settingPath)) {
				String settings = readFileFromClasspath(settingPath);
				if (isNotBlank(settings)) {
					return createIndex(getPersistentEntityFor(clazz).getIndexName(), settings);
				}
			} else {
				logger.info("settingPath in @Setting has to be defined. Using default instead.");
			}
		}
		return createIndex(getPersistentEntityFor(clazz).getIndexName(), getDefaultSettings(getPersistentEntityFor(clazz)));
	}

	@Override
	public boolean createIndex(String indexName, Object settings) {
		CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(indexName);
		if (settings instanceof String) {
			// 直接使用string的方法已经是被废弃的了.我们使用他推荐的这种.
//			createIndexRequestBuilder.setSettings(String.valueOf(settings));
			createIndexRequestBuilder.setSettings(String.valueOf(settings), XContentType.JSON);
		} else if (settings instanceof Map) {
			createIndexRequestBuilder.setSettings((Map) settings);
		} else if (settings instanceof XContentBuilder) {
			createIndexRequestBuilder.setSettings((XContentBuilder) settings);
		}
		return createIndexRequestBuilder.execute().actionGet().isAcknowledged();
	}

	@Override
	public <T> boolean createIndex(Class<T> clazz, Object settings) {
		return createIndex(getPersistentEntityFor(clazz).getIndexName(), settings);
	}

	private <T> Map getDefaultSettings(ElasticsearchPersistentEntity<T> persistentEntity) {

		if (persistentEntity.isUseServerConfiguration())
			return new HashMap();

		return new MapBuilder<String, String>().put("index.number_of_shards", String.valueOf(persistentEntity.getShards()))
				.put("index.number_of_replicas", String.valueOf(persistentEntity.getReplicas()))
				.put("index.refresh_interval", persistentEntity.getRefreshInterval())
				.put("index.store.type", persistentEntity.getIndexStoreType()).map();
	}

	@Override
	public <T> Map getSetting(Class<T> clazz) {
		return getSetting(getPersistentEntityFor(clazz).getIndexName());
	}

	@Override
	public Map getSetting(String indexName) {
		Assert.notNull(indexName, "No index defined for getSettings");
		return client.admin().indices().getSettings(new GetSettingsRequest())
				.actionGet().getIndexToSettings().get(indexName).getAsMap();
	}

	private <T> SearchRequestBuilder prepareSearch(Query query, Class<T> clazz) {
		setPersistentEntityIndexAndType(query, clazz);
		return prepareSearch(query);
	}

	private SearchRequestBuilder prepareSearch(Query query) {
		Assert.notNull(query.getIndices(), "No index defined for Query");
		Assert.notNull(query.getTypes(), "No type defined for Query");

		int startRecord = 0;
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(toArray(query.getIndices()))
				.setSearchType(query.getSearchType()).setTypes(toArray(query.getTypes()));

		if (query.getSourceFilter() != null) {
			SourceFilter sourceFilter = query.getSourceFilter();
			searchRequestBuilder.setFetchSource(sourceFilter.getIncludes(), sourceFilter.getExcludes());
		}

		if (query.getPageable() != null) {
			startRecord = query.getPageable().getPageNumber() * query.getPageable().getPageSize();
			searchRequestBuilder.setSize(query.getPageable().getPageSize());
		}
		searchRequestBuilder.setFrom(startRecord);

		if (!query.getFields().isEmpty()) {
			searchRequestBuilder.storedFields(toArray(query.getFields()));
		}

		if (query.getSort() != null) {
			for (Sort.Order order : query.getSort()) {
				searchRequestBuilder.addSort(order.getProperty(), order.getDirection() == Sort.Direction.DESC ? SortOrder.DESC
						: SortOrder.ASC);
			}
		}

		if (query.getMinScore() > 0) {
			searchRequestBuilder.setMinScore(query.getMinScore());
		}
		return searchRequestBuilder;
	}

	private IndexRequestBuilder prepareIndex(IndexQuery query) {
		try {
			String indexName = isBlank(query.getIndexName()) ? retrieveIndexNameFromPersistentEntity(query.getObject()
					.getClass())[0] : query.getIndexName();
			String type = isBlank(query.getType()) ? retrieveTypeFromPersistentEntity(query.getObject().getClass())[0]
					: query.getType();

			IndexRequestBuilder indexRequestBuilder = null;

			if (query.getObject() != null) {
				String id = isBlank(query.getId()) ? getPersistentEntityId(query.getObject()) : query.getId();
				// If we have a query id and a document id, do not ask ES to generate one.
				if (id != null) {
					indexRequestBuilder = client.prepareIndex(indexName, type, id);
				} else {
					indexRequestBuilder = client.prepareIndex(indexName, type);
				}
				// 这里修改了一些
//				indexRequestBuilder.setSource(resultsMapper.getEntityMapper().mapToString(query.getObject()));
				indexRequestBuilder.setSource(resultsMapper.getEntityMapper().mapToString(query.getObject()), Requests.INDEX_CONTENT_TYPE);
			} else if (query.getSource() != null) {
				indexRequestBuilder = client.prepareIndex(indexName, type, query.getId()).setSource(query.getSource());
			} else {
				throw new ElasticsearchException("object or source is null, failed to index the document [id: " + query.getId() + "]");
			}
			if (query.getVersion() != null) {
				indexRequestBuilder.setVersion(query.getVersion());
				indexRequestBuilder.setVersionType(EXTERNAL);
			}

			if (query.getParentId() != null) {
				indexRequestBuilder.setParent(query.getParentId());
			}

			return indexRequestBuilder;
		} catch (IOException e) {
			throw new ElasticsearchException("failed to index the document [id: " + query.getId() + "]", e);
		}
	}

	@Override
	public void refresh(String indexName) {
		Assert.notNull(indexName, "No index defined for refresh()");
		client.admin().indices().refresh(refreshRequest(indexName)).actionGet();
	}

	@Override
	public <T> void refresh(Class<T> clazz) {
		refresh(getPersistentEntityFor(clazz).getIndexName());
	}

	@Override
	public Boolean addAlias(AliasQuery query) {
		Assert.notNull(query.getIndexName(), "No index defined for Alias");
		Assert.notNull(query.getAliasName(), "No alias defined");
		
//		AliasAction aliasAction = new AliasAction(ADD, query.getIndexName(), query.getAliasName());
//		if (query.getFilterBuilder() != null) {
//			aliasAction.filter(query.getFilterBuilder());
//		} else if (query.getFilter() != null) {
//			aliasAction.filter(query.getFilter());
//		} else if (isNotBlank(query.getRouting())) {
//			aliasAction.routing(query.getRouting());
//		} else if (isNotBlank(query.getSearchRouting())) {
//			aliasAction.searchRouting(query.getSearchRouting());
//		} else if (isNotBlank(query.getIndexRouting())) {
//			aliasAction.indexRouting(query.getIndexRouting());
//		}
		AliasActions aliasAction = AliasActions.add().index(query.getIndexName()).alias(query.getAliasName());
		if (query.getFilterBuilder() != null) {
			aliasAction.filter(query.getFilterBuilder());
		} else if (query.getFilter() != null) {
			aliasAction.filter(query.getFilter());
		} else if (isNotBlank(query.getRouting())) {
			aliasAction.routing(query.getRouting());
		} else if (isNotBlank(query.getSearchRouting())) {
			aliasAction.searchRouting(query.getSearchRouting());
		} else if (isNotBlank(query.getIndexRouting())) {
			aliasAction.indexRouting(query.getIndexRouting());
		}
		
		return client.admin().indices().prepareAliases().addAliasAction(aliasAction).execute().actionGet().isAcknowledged();
	}

	@Override
	public Boolean removeAlias(AliasQuery query) {
		Assert.notNull(query.getIndexName(), "No index defined for Alias");
		Assert.notNull(query.getAliasName(), "No alias defined");
		return client.admin().indices().prepareAliases().removeAlias(query.getIndexName(), query.getAliasName())
				.execute().actionGet().isAcknowledged();
	}

	@Override
	public List<AliasMetaData> queryForAlias(String indexName) {
		return client.admin().indices().getAliases(new GetAliasesRequest().indices(indexName))
				.actionGet().getAliases().get(indexName);
	}

	@Override
	public ElasticsearchPersistentEntity getPersistentEntityFor(Class clazz) {
		Assert.isTrue(clazz.isAnnotationPresent(Document.class), "Unable to identify index name. " + clazz.getSimpleName()
				+ " is not a Document. Make sure the document class is annotated with @Document(indexName=\"foo\")");
		return elasticsearchConverter.getMappingContext().getPersistentEntity(clazz);
	}

	private String getPersistentEntityId(Object entity) {

		ElasticsearchPersistentEntity<?> persistentEntity = getPersistentEntityFor(entity.getClass());
		Object identifier = persistentEntity.getIdentifierAccessor(entity).getIdentifier();

		return identifier == null ? null : String.valueOf(identifier);
	}

	private void setPersistentEntityId(Object entity, String id) {

		ElasticsearchPersistentEntity<?> persistentEntity = getPersistentEntityFor(entity.getClass());
		PersistentProperty<?> idProperty = persistentEntity.getIdProperty();

		// Only deal with String because ES generated Ids are strings !
		if (idProperty != null && idProperty.getType().isAssignableFrom(String.class)) {
			persistentEntity.getPropertyAccessor(entity).setProperty(idProperty,id);
		}
	}

	private void setPersistentEntityIndexAndType(Query query, Class clazz) {
		if (query.getIndices().isEmpty()) {
			query.addIndices(retrieveIndexNameFromPersistentEntity(clazz));
		}
		if (query.getTypes().isEmpty()) {
			query.addTypes(retrieveTypeFromPersistentEntity(clazz));
		}
	}

	private String[] retrieveIndexNameFromPersistentEntity(Class clazz) {
		if (clazz != null) {
			return new String[]{getPersistentEntityFor(clazz).getIndexName()};
		}
		return null;
	}

	private String[] retrieveTypeFromPersistentEntity(Class clazz) {
		if (clazz != null) {
			return new String[]{getPersistentEntityFor(clazz).getIndexType()};
		}
		return null;
	}

	private List<String> extractIds(SearchResponse response) {
		List<String> ids = new ArrayList<String>();
		for (SearchHit hit : response.getHits()) {
			if (hit != null) {
				ids.add(hit.getId());
			}
		}
		return ids;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		if (elasticsearchConverter instanceof ApplicationContextAware) {
			((ApplicationContextAware) elasticsearchConverter).setApplicationContext(context);
		}
	}

	private static String[] toArray(List<String> values) {
		String[] valuesAsArray = new String[values.size()];
		return values.toArray(valuesAsArray);
	}

	protected ResultsMapper getResultsMapper() {
		return resultsMapper;
	}

	public static String readFileFromClasspath(String url) {
		StringBuilder stringBuilder = new StringBuilder();

		BufferedReader bufferedReader = null;

		try {
			ClassPathResource classPathResource = new ClassPathResource(url);
			InputStreamReader inputStreamReader = new InputStreamReader(classPathResource.getInputStream());
			bufferedReader = new BufferedReader(inputStreamReader);
			String line;

			String lineSeparator = System.getProperty("line.separator");
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line).append(lineSeparator);
			}
		} catch (Exception e) {
			logger.debug(String.format("Failed to load file from url: %s: %s", url, e.getMessage()));
			return null;
		} finally {
			if (bufferedReader != null)
				try {
					bufferedReader.close();
				} catch (IOException e) {
					logger.debug(String.format("Unable to close buffered reader.. %s", e.getMessage()));
				}
		}

		return stringBuilder.toString();
	}

	public SearchResponse suggest(SuggestBuilder suggestion, String... indices) {
		return client.prepareSearch(indices).suggest(suggestion).get();
	}

	public SearchResponse suggest(SuggestBuilder suggestion, Class clazz) {
		return suggest(suggestion, retrieveIndexNameFromPersistentEntity(clazz));
	}
}
