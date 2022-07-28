package dot.cpp.core.services;

import dot.cpp.repository.models.BaseEntity;
import dot.cpp.repository.repository.BaseRepository;
import java.util.List;

public class EntityService<T extends BaseEntity> {
  private final BaseRepository<T> repository;

  public EntityService(BaseRepository<T> repository) {
    this.repository = repository;
  }

  public T findById(String id) {
    return repository.findById(id);
  }

  public T findByField(String field, String value) {
    return repository.findByField(field, value);
  }

  public List<T> listByField(String field, String value) {
    return repository.listByField(field, value);
  }

  public List<T> listAll() {
    return repository.listAll();
  }

  public List<T> listAllPaginated(int pageSize, int pageNum) {
    return repository.listAllPaginated(pageSize, pageNum);
  }

  public void save(T entity) {
    repository.save(entity);
    //TODO exceptions
  }

  public void delete(T entity) {
    repository.delete(entity);
    //TODO exceptions
  }
}
