package com.reloop.common.jpa;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

/**
 * Spring-Data-like save() over Panache: persist for new entities (identifier is
 * assigned eagerly so callers can read getId() right after save), merge for
 * detached ones, no-op for managed ones.
 */
public interface ReloopRepository<T, ID> extends PanacheRepositoryBase<T, ID> {

    default T save(T entity) {
        if (isPersistent(entity)) {
            return getEntityManager().merge(entity);
        }
        persist(entity);
        return entity;
    }
}
