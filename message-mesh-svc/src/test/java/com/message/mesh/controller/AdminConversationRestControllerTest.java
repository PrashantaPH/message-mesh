package com.message.mesh.controller;

import com.message.mesh.dto.AdminConversationDto;
import com.message.mesh.dto.MessageDto;
import com.message.mesh.dto.PagedResponse;
import com.message.mesh.enums.ConversationType;
import com.message.mesh.service.AdminConversationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

import java.security.Principal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminConversationRestController")
class AdminConversationRestControllerTest {

    @Mock
    private AdminConversationService adminConversationService;
    @Mock
    private Principal principal;

    @InjectMocks
    private AdminConversationRestController controller;

    private final UUID convId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(principal.getName()).thenReturn("root");
    }

    @Test
    @DisplayName("list clamps pagination and delegates to the service")
    void listClampsPagination() {
        @SuppressWarnings("unchecked")
        PagedResponse<AdminConversationDto> expected = mock(PagedResponse.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(adminConversationService.listConversations(eq("team"), eq(ConversationType.GROUP), eq(false),
                pageable.capture())).thenReturn(expected);

        PagedResponse<AdminConversationDto> result =
                controller.list("team", ConversationType.GROUP, false, -1, 999);

        assertThat(result).isSameAs(expected);
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("delete delegates to softDelete")
    void deleteDelegates() {
        AdminConversationDto expected = mock(AdminConversationDto.class);
        when(adminConversationService.softDelete(convId, "root")).thenReturn(expected);

        assertThat(controller.delete(convId, principal)).isSameAs(expected);
    }

    @Test
    @DisplayName("restore delegates to restore")
    void restoreDelegates() {
        AdminConversationDto expected = mock(AdminConversationDto.class);
        when(adminConversationService.restore(convId, "root")).thenReturn(expected);

        assertThat(controller.restore(convId, principal)).isSameAs(expected);
    }

    @Test
    @DisplayName("messages clamps pagination and delegates to listMessages")
    void messagesDelegates() {
        @SuppressWarnings("unchecked")
        PagedResponse<MessageDto> expected = mock(PagedResponse.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(adminConversationService.listMessages(eq(convId), pageable.capture())).thenReturn(expected);

        PagedResponse<MessageDto> result = controller.messages(convId, -3, 250);

        assertThat(result).isSameAs(expected);
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("deleteMessage delegates to the service")
    void deleteMessageDelegates() {
        UUID messageId = UUID.randomUUID();
        MessageDto expected = mock(MessageDto.class);
        when(adminConversationService.deleteMessage(convId, messageId, "root")).thenReturn(expected);

        assertThat(controller.deleteMessage(convId, messageId, principal)).isSameAs(expected);
    }
}
