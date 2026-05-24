package ai.gameclaw.persistence;

public interface TenantAwareRepository<T, ID> {

    T save(String tenantId, T entity);

    T findById(String tenantId, ID id);

    Iterable<T> findAll(String tenantId);
}
