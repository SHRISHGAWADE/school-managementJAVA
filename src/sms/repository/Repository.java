package sms.repository;

import java.util.*;
import sms.model.Identifiable;

public interface Repository<T extends Identifiable> {
  void saveAll(List<T> items);

  List<T> findAll();

  default Optional<T> findById(int id) {
    return findAll().stream().filter(item -> item.getId() == id).findFirst();
  }
}
