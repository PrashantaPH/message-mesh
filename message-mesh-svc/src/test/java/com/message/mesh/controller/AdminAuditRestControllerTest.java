package com.message.mesh.controller;

import com.message.mesh.dto.AuditEventDto;
import com.message.mesh.dto.PagedResponse;
import com.message.mesh.service.AuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAuditRestController")
class AdminAuditRestControllerTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AdminAuditRestController controller;

    @Test
    @DisplayName("list parses valid ISO-8601 instants for the from/to range")
    void listParsesValidInstants() {
        @SuppressWarnings("unchecked")
        PagedResponse<AuditEventDto> expected = mock(PagedResponse.class);
        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
        when(auditService.list(eq("alice"), eq("LOGIN"), from.capture(), to.capture(), any(Pageable.class)))
                .thenReturn(expected);

        PagedResponse<AuditEventDto> result = controller.list(
                "alice", "LOGIN", "2024-01-01T00:00:00Z", "2024-02-01T00:00:00Z", 0, 20);

        assertThat(result).isSameAs(expected);
        assertThat(from.getValue()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
        assertThat(to.getValue()).isEqualTo(Instant.parse("2024-02-01T00:00:00Z"));
    }

    @Test
    @DisplayName("list treats an unparseable instant as null")
    void listTreatsInvalidInstantAsNull() {
        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        when(auditService.list(isNull(), isNull(), from.capture(), isNull(), any(Pageable.class)))
                .thenReturn(mock(PagedResponse.class));

        controller.list(null, null, "not-a-date", null, 0, 20);

        assertThat(from.getValue()).isNull();
    }

    @Test
    @DisplayName("list clamps pagination bounds")
    void listClampsPagination() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(auditService.list(isNull(), isNull(), isNull(), isNull(), pageable.capture()))
                .thenReturn(mock(PagedResponse.class));

        controller.list(null, null, null, null, -2, 500);

        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }
}
