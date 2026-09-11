package com.reloop.ownership.repository;

import com.reloop.common.dto.Page;
import com.reloop.common.jpa.ReloopRepository;
import com.reloop.ownership.domain.OwnershipTransfer;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OwnershipTransferRepository implements ReloopRepository<OwnershipTransfer, UUID> {

    public List<OwnershipTransfer> findByUnitIdOrderByTransferredAtDesc(UUID unitId) {
        return list("unitId", Sort.descending("transferredAt"), unitId);
    }

    public Page<OwnershipTransfer> findPagedByToOwnerId(Long toOwnerId, int page, int size) {
        var query = find("toOwnerId = ?1", Sort.descending("transferredAt"), toOwnerId);
        long total = query.count();
        List<OwnershipTransfer> items = query.page(io.quarkus.panache.common.Page.of(page, size)).list();
        return new Page<>(items, total, page, size);
    }

    public Page<OwnershipTransfer> findPagedAll(int page, int size) {
        var query = findAll(Sort.descending("transferredAt"));
        long total = query.count();
        List<OwnershipTransfer> items = query.page(io.quarkus.panache.common.Page.of(page, size)).list();
        return new Page<>(items, total, page, size);
    }

    public Optional<OwnershipTransfer> findLatestByUnitId(UUID unitId) {
        return find("unitId", Sort.descending("transferredAt"), unitId).firstResultOptional();
    }
}
