package com.message.mesh.controller;

import com.message.mesh.dto.AddMembersRequest;
import com.message.mesh.dto.ConversationDto;
import com.message.mesh.dto.ConversationMemberDto;
import com.message.mesh.dto.CreateConversationRequest;
import com.message.mesh.dto.MembershipPrefsRequest;
import com.message.mesh.dto.MessageDto;
import com.message.mesh.dto.PagedResponse;
import com.message.mesh.dto.ReadRequest;
import com.message.mesh.dto.RenameConversationRequest;
import com.message.mesh.dto.UpdateMemberRoleRequest;
import com.message.mesh.enums.ConversationType;
import com.message.mesh.enums.MembershipRole;
import com.message.mesh.service.ConversationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationRestController")
class ConversationRestControllerTest {

    @Mock
    private ConversationService conversationService;
    @Mock
    private Principal principal;

    @InjectMocks
    private ConversationRestController controller;

    private final UUID convId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(principal.getName()).thenReturn("alice");
    }

    @Test
    @DisplayName("list delegates to listForUser")
    void listDelegates() {
        List<ConversationDto> expected = List.of();
        when(conversationService.listForUser("alice")).thenReturn(expected);

        assertThat(controller.list(principal)).isSameAs(expected);
    }

    @Test
    @DisplayName("create responds 201 Created")
    void createReturnsCreated() {
        CreateConversationRequest req =
                new CreateConversationRequest(ConversationType.GROUP, "Team", List.of("bob"));
        ConversationDto created = mock(ConversationDto.class);
        when(conversationService.create("alice", req)).thenReturn(created);

        ResponseEntity<ConversationDto> response = controller.create(req, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(created);
    }

    @Test
    @DisplayName("messages delegates with the cursor and limit")
    void messagesDelegates() {
        List<MessageDto> expected = List.of();
        when(conversationService.getMessages("alice", convId, 5L, 25)).thenReturn(expected);

        assertThat(controller.messages(convId, 5L, 25, principal)).isSameAs(expected);
    }

    @Test
    @DisplayName("markRead returns 204 and advances the read marker")
    void markReadReturnsNoContent() {
        ResponseEntity<Void> response = controller.markRead(convId, new ReadRequest(9L), principal);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(conversationService).markRead("alice", convId, 9L);
    }

    @Test
    @DisplayName("members delegates to getMembers")
    void membersDelegates() {
        List<ConversationMemberDto> expected = List.of();
        when(conversationService.getMembers("alice", convId)).thenReturn(expected);

        assertThat(controller.members(convId, principal)).isSameAs(expected);
    }

    @Test
    @DisplayName("searchMessages delegates to the service")
    void searchMessagesDelegates() {
        @SuppressWarnings("unchecked")
        PagedResponse<MessageDto> expected = mock(PagedResponse.class);
        when(conversationService.searchMessages("alice", convId, "hello", 0, 20)).thenReturn(expected);

        assertThat(controller.searchMessages(convId, "hello", 0, 20, principal)).isSameAs(expected);
    }

    @Test
    @DisplayName("rename delegates to renameGroup")
    void renameDelegates() {
        ConversationDto expected = mock(ConversationDto.class);
        when(conversationService.renameGroup("alice", convId, "New Title")).thenReturn(expected);

        assertThat(controller.rename(convId, new RenameConversationRequest("New Title"), principal))
                .isSameAs(expected);
    }

    @Test
    @DisplayName("addMembers delegates to the service")
    void addMembersDelegates() {
        List<ConversationMemberDto> expected = List.of();
        when(conversationService.addMembers("alice", convId, List.of("bob"))).thenReturn(expected);

        assertThat(controller.addMembers(convId, new AddMembersRequest(List.of("bob")), principal))
                .isSameAs(expected);
    }

    @Test
    @DisplayName("removeMember returns 204")
    void removeMemberReturnsNoContent() {
        UUID userId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.removeMember(convId, userId, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(conversationService).removeMember("alice", convId, userId);
    }

    @Test
    @DisplayName("updateMemberRole delegates to the service")
    void updateMemberRoleDelegates() {
        UUID userId = UUID.randomUUID();
        List<ConversationMemberDto> expected = List.of();
        when(conversationService.updateMemberRole("alice", convId, userId, MembershipRole.ADMIN))
                .thenReturn(expected);

        assertThat(controller.updateMemberRole(convId, userId,
                new UpdateMemberRoleRequest(MembershipRole.ADMIN), principal)).isSameAs(expected);
    }

    @Test
    @DisplayName("updateMembership delegates to setMembershipPrefs")
    void updateMembershipDelegates() {
        MembershipPrefsRequest req = new MembershipPrefsRequest(true, null);
        ConversationDto expected = mock(ConversationDto.class);
        when(conversationService.setMembershipPrefs(eq("alice"), eq(convId), eq(req))).thenReturn(expected);

        assertThat(controller.updateMembership(convId, req, principal)).isSameAs(expected);
    }

    @Test
    @DisplayName("delete returns 204 and delegates to deleteConversation")
    void deleteReturnsNoContent() {
        ResponseEntity<Void> response = controller.delete(convId, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(conversationService).deleteConversation("alice", convId);
    }
}
