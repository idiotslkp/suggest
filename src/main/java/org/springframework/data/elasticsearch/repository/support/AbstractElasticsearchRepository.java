/*
 * Copyright 2013-2017 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.support;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.index.query.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 * Elasticsearch specific repository implementation. Likely to be used as target within
 * {@link ElasticsearchRepositoryFactory}
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Ryan Henszey
 * @author Kevin Leturc
 * @author Mark Paluch
 */
public abstract class AbstractElasticsearchRepository<T, ID extends Serializable> implements
		ElasticsearchRepository<T, ID> {

	static final Logger LOGGER = LoggerFactory.getLogger(AbstractElasticsearchRepository.class);
	protected ElasticsearchOperations elasticsearchOperations;
	protected Class<T> entityClass;
	protected ElasticsearchEntityInformation<T, ID> entityInformation;

	public AbstractElasticsearchRepository() {
	}

	public AbstractElasticsearchRepository(ElasticsearchOperations elasticsearchOperations) {
		
		Assert.notNull(elasticsearchOperations, "ElasticsearchOperations must not be null!");
		
		this.setElasticsearchOperations(elasticsearchOperations);
	}

	public AbstractElasticsearchRepository(ElasticsearchEntityInformation<T, ID> metadata,
										   ElasticsearchOperations elasticsearchOperations) {
		this(elasticsearchOperations);
		
		Assert.notNull(metadata, "ElasticsearchEntityInformation must not be null!");
		
		this.entityInformation = metadata;
		setEntityClass(this.entityInformation.getJavaType());
		try {
			if (createIndexAndMapping()) {
				createIndex();
				putMapping();
			}
		} catch (ElasticsearchException exception) {
			LOGGER.error("failed to load elasticsearch nodes : " + exception.getDetailedMessage());
		}
	}

	private void createIndex() {
		elasticsearchOperations.createIndex(getEntityClass());
	}

	private void putMapping() {
		elasticsearchOperations.putMapping(getEntityClass());
	}

	private boolean createIndexAndMapping() {
		return elasticsearchOperations.getPersistentEntityFor(getEntityClass()).isCreateIndexAndMapping();
	}

	@Override
	public T findOne(ID id) {
		GetQuery query = new GetQuery();
		query.setId(stringIdRepresentation(id));
		return elasticsearchOperations.queryForObject(query, getEntityClass());
	}

	@Override
	public Iterable<T> findAll() {
		int itemCount = (int) this.count();
		if (itemCount == 0) {
			return new PageImpl<T>(Collections.<T>emptyList());
		}
		return this.findAll(new PageRequest(0, Math.max(1, itemCount)));
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		SearchQuery query = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).withPageable(pageable).build();
		return elasticsearchOperations.queryForPage(query, getEntityClass());
	}

	@Override
	public Iterable<T> findAll(Sort sort) {
		int itemCount = (int) this.count();
		if (itemCount == 0) {
			return new PageImpl<T>(Collections.<T>emptyList());
		}
		SearchQuery query = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
				.withPageable(new PageRequest(0, itemCount, sort)).build();
		return elasticsearchOperations.queryForPage(query, getEntityClass());
	}

	@Override
	public Iterable<T> findAll(Iterable<ID> ids) {
		Assert.notNull(ids, "ids can't be null.");
		SearchQuery query = new NativeSearchQueryBuilder()
				.withIds(stringIdsRepresentation(ids))
				.build();
		return elasticsearchOperations.multiGet(query, getEntityClass());
	}

	@Override
	public long count() {
		SearchQuery query = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
		return elasticsearchOperations.count(query, getEntityClass());
	}

	@Override
	public <S extends T> S save(S entity) {
		Assert.notNull(entity, "Cannot save 'null' entity.");
		elasticsearchOperations.index(createIndexQuery(entity));
		elasticsearchOperations.refresh(entityInformation.getIndexName());
		return entity;
	}

	public <S extends T> List<S> save(List<S> entities) {
		Assert.notNull(entities, "Cannot insert 'null' as a List.");
		Assert.notEmpty(entities, "Cannot insert empty List.");
		List<IndexQuery> queries = new ArrayList<IndexQuery>();
		for (S s : entities) {
			queries.add(createIndexQuery(s));
		}
		elasticsearchOperations.bulkIndex(queries);
		elasticsearchOperations.refresh(entityInformation.getIndexName());
		return entities;
	}

	@Override
	public <S extends T> S index(S entity) {
		return save(entity);
	}

	@Override
	public <S extends T> Iterable<S> save(Iterable<S> entities) {
		Assert.notNull(entities, "Cannot insert 'null' as a List.");
		List<IndexQuery> queries = new ArrayList<IndexQuery>();
		for (S s : entities) {
			queries.add(createIndexQuery(s));
		}
		elasticsearchOperations.bulkIndex(queries);
		elasticsearchOperations.refresh(entityInformation.getIndexName());
		return entities;
	}

	@Override
	public boolean exists(ID id) {
		return findOne(id) != null;
	}

	@Override
	public Iterable<T> search(QueryBuilder query) {
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(query).build();
		int count = (int) elasticsearchOperations.count(searchQuery, getEntityClass());
		if (count == 0) {
			return new PageImpl<T>(Collections.<T>emptyList());
		}
		searchQuery.setPageable(new PageRequest(0, count));
		return elasticsearchOperations.queryForPage(searchQuery, getEntityClass());
	}

	@Override
	public Page<T> search(QueryBuilder query, Pageable pageable) {
		SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(query).withPageable(pageable).build();
		return elasticsearchOperations.queryForPage(searchQuery, getEntityClass());
	}

	@Override
	public Page<T> search(SearchQuery query) {
		return elasticsearchOperations.queryForPage(query, getEntityClass());
	}

	@Override
	public Page<T> searchSimilar(T entity, String[] fields, Pageable pageable) {
		Assert.notNull(entity, "Cannot search similar records for 'null'.");
		Assert.notNull(pageable, "'pageable' cannot be 'null'");
		MoreLikeThisQuery query = new MoreLikeThisQuery();
		query.setId(stringIdRepresentation(extractIdFromBean(entity)));
		query.setPageable(pageable);
		if (fields != null) {
			query.addFields(fields);
		}
		return elasticsearchOperations.moreLikeThis(query, getEntityClass());
	}

	@Override
	public void delete(ID id) {
		Assert.notNull(id, "Cannot delete entity with id 'null'.");
		elasticsearchOperations.delete(entityInformation.getIndexName(), entityInformation.getType(),
				stringIdRepresentation(id));
		elasticsearchOperations.refresh(entityInformation.getIndexName());
	}

	@Override
	public void delete(T entity) {
		Assert.notNull(entity, "Cannot delete 'null' entity.");
		delete(extractIdFromBean(entity));
		elasticsearchOperations.refresh(entityInformation.getIndexName());
	}

	@Override
	public void delete(Iterable<? extends T> entities) {
		Assert.notNull(entities, "Cannot delete 'null' list.");
		for (T entity : entities) {
			delete(entity);
		}
	}

	@Override
	public void deleteAll() {
		DeleteQuery deleteQuery = new DeleteQuery();
		deleteQuery.setQuery(matchAllQuery());
		elasticsearchOperations.delete(deleteQuery, getEntityClass());
		elasticsearchOperations.refresh(entityInformation.getIndexName());
	}

	@Override
	public void refresh() {
		elasticsearchOperations.refresh(getEntityClass());
	}

	private IndexQuery createIndexQuery(T entity) {
		IndexQuery query = new IndexQuery();
		query.setObject(entity);
		query.setId(stringIdRepresentation(extractIdFromBean(entity)));
		query.setVersion(extractVersionFromBean(entity));
		query.setParentId(extractParentIdFromBean(entity));
		return query;
	}

	@SuppressWarnings("unchecked")
	private Class<T> resolveReturnedClassFromGenericType() {
		ParameterizedType parameterizedType = resolveReturnedClassFromGenericType(getClass());
		return (Class<T>) parameterizedType.getActualTypeArguments()[0];
	}

	private ParameterizedType resolveReturnedClassFromGenericType(Class<?> clazz) {
		Object genericSuperclass = clazz.getGenericSuperclass();
		if (genericSuperclass instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
			Type rawtype = parameterizedType.getRawType();
			if (SimpleElasticsearchRepository.class.equals(rawtype)) {
				return parameterizedType;
			}
		}
		return resolveReturnedClassFromGenericType(clazz.getSuperclass());
	}

	@Override
	public Class<T> getEntityClass() {
		if (!isEntityClassSet()) {
			try {
				this.entityClass = resolveReturnedClassFromGenericType();
			} catch (Exception e) {
				throw new InvalidDataAccessApiUsageException("Unable to resolve EntityClass. Please use according setter!", e);
			}
		}
		return entityClass;
	}

	private boolean isEntityClassSet() {
		return entityClass != null;
	}

	public final void setEntityClass(Class<T> entityClass) {
		Assert.notNull(entityClass, "EntityClass must not be null.");
		this.entityClass = entityClass;
	}

	public final void setElasticsearchOperations(ElasticsearchOperations elasticsearchOperations) {
		Assert.notNull(elasticsearchOperations, "ElasticsearchOperations must not be null.");
		this.elasticsearchOperations = elasticsearchOperations;
	}

	protected ID extractIdFromBean(T entity) {
		if (entityInformation != null) {
			return entityInformation.getId(entity);
		}
		return null;
	}

	private List<String> stringIdsRepresentation(Iterable<ID> ids) {
		Assert.notNull(ids, "ids can't be null.");
		List<String> stringIds = new ArrayList<String>();
		for (ID id : ids) {
			stringIds.add(stringIdRepresentation(id));
		}
		return stringIds;
	}

	protected abstract String stringIdRepresentation(ID id);

	private Long extractVersionFromBean(T entity) {
		if (entityInformation != null) {
			return entityInformation.getVersion(entity);
		}
		return null;
	}

	private String extractParentIdFromBean(T entity) {
		if (entityInformation != null) {
			return entityInformation.getParentId(entity);
		}
		return null;
	}
}
