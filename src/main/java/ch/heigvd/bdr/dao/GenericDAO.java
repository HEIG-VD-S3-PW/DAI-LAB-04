package ch.heigvd.bdr.dao;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Generic interface for all DAOs
 * @param <T>: entity
 * @param <ID>: id
 */
interface GenericDAO<T, ID> {
  T create(T entity) throws ClassNotFoundException, IOException, SQLException;

  T findById(ID id) throws ClassNotFoundException, IOException, SQLException;

  List<T> findAll() throws ClassNotFoundException, IOException, SQLException;

  T update(T entity) throws ClassNotFoundException, IOException, SQLException;

  boolean delete(ID id) throws ClassNotFoundException, IOException, SQLException;
}
