package com.massestech.core.base.es.service;

import com.massestech.core.base.es.dal.repository.ESBaseRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class ESBaseServiceImpl<T> implements ESBaseService<T> {

    @Autowired
    protected ESBaseRepository<T, String> baseRepository;

    @Override
    public T create(T entity) {
        return baseRepository.save(entity);
    }

    @Override
    public T getOne(String id) {
        return baseRepository.findOne(id);
    }

    @Override
    public void delete(String id) {
        baseRepository.delete(id);
    }

    @Override
    public void update(T entity) {
        baseRepository.save(entity);
    }

}
