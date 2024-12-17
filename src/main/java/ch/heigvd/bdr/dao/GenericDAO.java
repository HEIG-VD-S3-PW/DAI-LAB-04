package ch.heigvd.bdr.dao;

import java.util.List;

interface GenericDAO<T, ID> {
  T create(T entity) throws Exception;

  T findById(ID id) throws Exception;

  List<T> findAll() throws Exception;

  T update(T entity) throws Exception;

  boolean delete(ID id) throws Exception;
}
