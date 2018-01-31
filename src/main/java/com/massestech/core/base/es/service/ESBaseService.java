package com.massestech.core.base.es.service;

public interface ESBaseService<T> {
    public T create(T entity);

    public T getOne(String id);

    public void delete(String id);

    public void update(T entity);
}
