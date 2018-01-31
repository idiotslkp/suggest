package com.massestech.core.base.es.dal.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;

@NoRepositoryBean
public interface ESBaseRepository<T, ID extends Serializable> extends ElasticsearchRepository<T, ID> {

}
