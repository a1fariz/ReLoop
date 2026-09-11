package com.reloop.users.repository;

import com.reloop.common.jpa.ReloopRepository;
import com.reloop.users.domain.UserKycLog;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class UserKycLogRepository implements ReloopRepository<UserKycLog, UUID> {

    /** Returns the paged query: call count() before list() — count() ignores paging. */
    public PanacheQuery<UserKycLog> findByUserIdPaged(Long userId, int page, int size) {
        return find("userId = ?1", Sort.descending("createdAt"), userId)
                .page(Page.of(page, size));
    }
}
