package com.message.mesh.repository;

import com.message.mesh.domain.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    Page<AuditEvent> findByActorUsernameContainingIgnoreCase(String actorUsername, Pageable pageable);

    Page<AuditEvent> findByActionContainingIgnoreCase(String action, Pageable pageable);

    Page<AuditEvent> findByActorUsernameContainingIgnoreCaseAndActionContainingIgnoreCase(
            String actorUsername, String action, Pageable pageable);

    /**
     * Audit search with optional actor/action text filters and an optional
     * created-at date range. Any parameter left {@code null} is ignored.
     */
    @Query("""
            select e from AuditEvent e
            where (:actor is null or lower(e.actorUsername) like lower(concat('%', :actor, '%')))
              and (:action is null or lower(e.action) like lower(concat('%', :action, '%')))
              and (:from is null or e.createdAt >= :from)
              and (:to is null or e.createdAt <= :to)
            """)
    Page<AuditEvent> search(@Param("actor") String actor,
                            @Param("action") String action,
                            @Param("from") Instant from,
                            @Param("to") Instant to,
                            Pageable pageable);
}
