package com.message.mesh.service;

import com.message.mesh.domain.AuditEvent;
import com.message.mesh.dto.AuditEventDto;
import com.message.mesh.dto.PagedResponse;
import com.message.mesh.repository.AuditEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService")
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    @DisplayName("record persists an audit event with the supplied fields")
    void recordPersistsEvent() {
        auditService.record("alice", "LOGIN", "user", "42", "details");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent saved = captor.getValue();
        assertThat(saved.getActorUsername()).isEqualTo("alice");
        assertThat(saved.getAction()).isEqualTo("LOGIN");
        assertThat(saved.getTargetType()).isEqualTo("user");
        assertThat(saved.getTargetId()).isEqualTo("42");
        assertThat(saved.getDetails()).isEqualTo("details");
    }

    @Test
    @DisplayName("record truncates details longer than 512 characters")
    void recordTruncatesLongDetails() {
        String longDetails = "x".repeat(600);

        auditService.record("alice", "ACTION", "user", "1", longDetails);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        assertThat(captor.getValue().getDetails()).hasSize(512);
    }

    @Test
    @DisplayName("record tolerates null details")
    void recordAllowsNullDetails() {
        auditService.record("alice", "ACTION", "user", "1", null);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        assertThat(captor.getValue().getDetails()).isNull();
    }

    @Test
    @DisplayName("list maps the page and normalizes blank filters to null")
    void listMapsPageAndNormalizesFilters() {
        AuditEvent event = AuditEvent.builder()
                .actorUsername("alice").action("LOGIN").targetType("user").targetId("1").build();
        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditEvent> page = new PageImpl<>(List.of(event), pageable, 1);
        when(auditEventRepository.search(isNull(), isNull(), any(), any(), eq(pageable))).thenReturn(page);

        PagedResponse<AuditEventDto> result =
                auditService.list("  ", "", Instant.now(), Instant.now(), pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).actorUsername()).isEqualTo("alice");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("list passes through non-blank filters")
    void listPassesThroughFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditEvent> page = new PageImpl<>(List.of(), pageable, 0);
        when(auditEventRepository.search(eq("alice"), eq("LOGIN"), isNull(), isNull(), eq(pageable)))
                .thenReturn(page);

        PagedResponse<AuditEventDto> result =
                auditService.list("alice", "LOGIN", null, null, pageable);

        assertThat(result.content()).isEmpty();
    }
}
