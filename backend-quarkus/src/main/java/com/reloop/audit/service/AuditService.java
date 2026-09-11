package com.reloop.audit.service;

import com.reloop.audit.domain.AuditLog;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Append-only audit trail writer. Records actor-visible state transitions for
 * the money- and inventory-critical flows (fulfillment, disputes).
 */
@ApplicationScoped
public class AuditService {

    private static final Logger log = Logger.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    @Inject
    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void record(String entityName, String entityId, String action, Long actorId,
                       String actorEmail, String fromState, String toState) {
        auditLogRepository.persist(new AuditLog(
                entityName, entityId, action, actorId, actorEmail, null,
                UUID.randomUUID(), toJson(fromState), toJson(toState)));
        log.debugf("Audit: %s %s [%s] %s -> %s", action, entityName, entityId, fromState, toState);
    }

    /** audit_logs.from_state/to_state are jsonb columns: wrap raw strings as JSON. */
    private static String toJson(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        if (raw.startsWith("{") || raw.startsWith("[")) return raw; // already structured
        return "\"" + raw.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @ApplicationScoped
    public static class AuditLogRepository implements PanacheRepository<AuditLog> {
    }
}
