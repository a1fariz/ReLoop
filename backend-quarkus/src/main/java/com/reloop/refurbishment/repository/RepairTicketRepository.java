package com.reloop.refurbishment.repository;

import com.reloop.common.dto.Page;
import com.reloop.common.jpa.ReloopRepository;
import com.reloop.refurbishment.domain.RepairTicket;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RepairTicketRepository implements ReloopRepository<RepairTicket, UUID> {

    public List<RepairTicket> findByUnitId(UUID unitId) {
        return list("unitId", Sort.descending("createdAt"), unitId);
    }

    public Page<RepairTicket> findPagedByTechnicianId(Long technicianId, int page, int size) {
        var query = find("technicianId = ?1", Sort.descending("createdAt"), technicianId);
        long total = query.count();
        List<RepairTicket> items = query.page(io.quarkus.panache.common.Page.of(page, size)).list();
        return new Page<>(items, total, page, size);
    }

    public Page<RepairTicket> findPagedByStatus(RepairTicket.Status status, int page, int size) {
        var query = find("status = ?1", Sort.descending("createdAt"), status);
        long total = query.count();
        List<RepairTicket> items = query.page(io.quarkus.panache.common.Page.of(page, size)).list();
        return new Page<>(items, total, page, size);
    }

    public Page<RepairTicket> findPagedAll(int page, int size) {
        var query = findAll(Sort.descending("createdAt"));
        long total = query.count();
        List<RepairTicket> items = query.page(io.quarkus.panache.common.Page.of(page, size)).list();
        return new Page<>(items, total, page, size);
    }
}
