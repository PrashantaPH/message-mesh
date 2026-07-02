package com.message.mesh.service;

import com.message.mesh.domain.AuditEvent;
import com.message.mesh.dto.AuditEventDto;
import com.message.mesh.dto.PagedResponse;
import com.message.mesh.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Records and queries the audit trail of sensitive actions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    @Transactional
    public void record(String actorUsername, String action, String targetType, String targetId, String details) {
        auditEventRepository.save(AuditEvent.builder()
                .actorUsername(actorUsername)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(truncate(details))
                .build());
        log.info("AUDIT actor='{}' action='{}' targetType='{}' targetId='{}'",
                actorUsername, action, targetType, targetId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditEventDto> list(String actor, String action, Instant from, Instant to, Pageable pageable) {
        String actorFilter = actor != null && !actor.isBlank() ? actor : null;
        String actionFilter = action != null && !action.isBlank() ? action : null;
        Page<AuditEvent> page = auditEventRepository.search(actorFilter, actionFilter, from, to, pageable);
        List<AuditEventDto> content = page.getContent().stream().map(AuditEventDto::from).toList();
        return new PagedResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private String truncate(String details) {
        if (details == null) {
            return null;
        }
        return details.length() > 512 ? details.substring(0, 512) : details;
    }
}
