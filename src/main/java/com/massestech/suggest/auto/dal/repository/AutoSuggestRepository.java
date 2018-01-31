package com.massestech.suggest.auto.dal.repository;

import com.massestech.core.base.es.dal.repository.ESBaseRepository;
import org.springframework.stereotype.Repository;
import com.massestech.suggest.auto.dal.model.AutoSuggest;

@Repository
public interface AutoSuggestRepository extends ESBaseRepository<AutoSuggest,String> {

}