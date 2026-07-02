package com.message.mesh.service;

import com.message.mesh.domain.ChatMessage;
import com.message.mesh.domain.Conversation;
import com.message.mesh.domain.Membership;
import com.message.mesh.domain.User;
import com.message.mesh.dto.ConversationDto;
import com.message.mesh.dto.ConversationMemberDto;
import com.message.mesh.dto.CreateConversationRequest;
import com.message.mesh.dto.MembershipPrefsRequest;
import com.message.mesh.dto.MessageDto;
import com.message.mesh.dto.PagedResponse;
import com.message.mesh.enums.ConversationType;
import com.message.mesh.enums.MembershipRole;
import com.message.mesh.enums.MessageStatus;
import com.message.mesh.exception.BadRequestException;
import com.message.mesh.exception.ResourceNotFoundException;
import com.message.mesh.repository.ConversationRepository;
import com.message.mesh.repository.MembershipRepository;
import com.message.mesh.repository.MessageRepository;
import com.message.mesh.repository.UserRepository;
import com.message.mesh.service.relay.MessageRelay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ConversationService")
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MessageRelay relay;
    @Mock
    private AuditService auditService;
    @Mock
    private MessageDtoAssembler messageDtoAssembler;

    @InjectMocks
    private ConversationService conversationService;

    private User alice;
    private User bob;
    private UUID conversationId;

    @BeforeEach
    void setUp() {
        alice = User.builder().id(UUID.randomUUID()).username("alice").displayName("Alice").build();
        bob = User.builder().id(UUID.randomUUID()).username("bob").displayName("Bob").build();
        conversationId = UUID.randomUUID();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        // Defaults for toDto helper
        when(messageRepository.findMaxSeq(any())).thenReturn(0L);
        when(messageRepository.findFirstByConversationIdOrderBySeqDesc(any())).thenReturn(Optional.empty());
        when(membershipRepository.countByConversationId(any())).thenReturn(2L);
    }

    @Test
    @DisplayName("create rejects a DIRECT conversation that does not resolve to exactly two members")
    void createRejectsInvalidDirect() {
        CreateConversationRequest req = new CreateConversationRequest(
                ConversationType.DIRECT, null, List.of("alice"));

        assertThatThrownBy(() -> conversationService.create("alice", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exactly 2 distinct members");
    }

    @Test
    @DisplayName("create reuses an existing DIRECT conversation for the same pair")
    void createReusesExistingDirect() {
        CreateConversationRequest req = new CreateConversationRequest(
                ConversationType.DIRECT, null, List.of("bob"));
        Conversation existing = Conversation.builder().id(conversationId)
                .type(ConversationType.DIRECT).createdAt(Instant.now()).build();
        when(conversationRepository.findExistingDirect(alice.getId(), bob.getId()))
                .thenReturn(Optional.of(existing));
        when(membershipRepository.findByUserIdAndConversationId(any(), eq(conversationId)))
                .thenReturn(Optional.of(Membership.builder().userId(alice.getId())
                        .conversationId(conversationId).lastReadSeq(0L).build()));
        when(membershipRepository.findByConversationId(conversationId)).thenReturn(List.of(
                Membership.builder().userId(alice.getId()).conversationId(conversationId).build(),
                Membership.builder().userId(bob.getId()).conversationId(conversationId).build()));
        when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));

        ConversationDto dto = conversationService.create("alice", req);

        assertThat(dto.id()).isEqualTo(conversationId);
        assertThat(dto.title()).isEqualTo("Bob");
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("create makes the creator ADMIN of a new GROUP and notifies members")
    void createGroupMakesCreatorAdmin() {
        CreateConversationRequest req = new CreateConversationRequest(
                ConversationType.GROUP, "Team", List.of("bob"));
        when(membershipRepository.findByUserIdAndConversationId(any(), any()))
                .thenReturn(Optional.of(Membership.builder().lastReadSeq(0L).build()));

        ConversationDto dto = conversationService.create("alice", req);

        assertThat(dto.title()).isEqualTo("Team");
        assertThat(dto.type()).isEqualTo(ConversationType.GROUP);
        verify(conversationRepository).save(any(Conversation.class));
        // creator (ADMIN) + bob (MEMBER) memberships saved
        verify(membershipRepository, org.mockito.Mockito.times(2)).save(any(Membership.class));
        verify(relay, org.mockito.Mockito.atLeastOnce())
                .relayToUser(anyString(), eq(com.message.mesh.constant.AppConstants.QUEUE_CONVERSATIONS), any());
    }

    @Test
    @DisplayName("listForUser maps every conversation the user belongs to")
    void listForUserMapsConversations() {
        Conversation conv = Conversation.builder().id(conversationId)
                .type(ConversationType.GROUP).title("Team").build();
        when(conversationRepository.findAllForUser(alice.getId())).thenReturn(List.of(conv));
        when(membershipRepository.findByUserIdAndConversationId(alice.getId(), conversationId))
                .thenReturn(Optional.of(Membership.builder().lastReadSeq(0L).build()));

        List<ConversationDto> result = conversationService.listForUser("alice");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Team");
    }

    @Test
    @DisplayName("getMessages enforces membership and returns assembled DTOs")
    void getMessagesReturnsHistory() {
        when(membershipRepository.existsByUserIdAndConversationId(alice.getId(), conversationId))
                .thenReturn(true);
        ChatMessage msg = ChatMessage.builder().id(UUID.randomUUID()).conversationId(conversationId)
                .senderUsername("alice").seq(1L).body("hi").status(MessageStatus.SENT).build();
        when(messageRepository.findByConversationIdAndSeqGreaterThanOrderBySeqAsc(
                eq(conversationId), eq(0L), any())).thenReturn(List.of(msg));
        MessageDto dto = MessageDto.from(msg);
        when(messageDtoAssembler.toDtos(List.of(msg))).thenReturn(List.of(dto));

        List<MessageDto> result = conversationService.getMessages("alice", conversationId, 0L, 50);

        assertThat(result).containsExactly(dto);
    }

    @Test
    @DisplayName("getMessages rejects a non-member")
    void getMessagesRejectsNonMember() {
        when(membershipRepository.existsByUserIdAndConversationId(alice.getId(), conversationId))
                .thenReturn(false);

        assertThatThrownBy(() -> conversationService.getMessages("alice", conversationId, 0L, 50))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("searchMessages returns an empty page for a blank query")
    void searchMessagesBlankQuery() {
        when(membershipRepository.existsByUserIdAndConversationId(alice.getId(), conversationId))
                .thenReturn(true);

        PagedResponse<MessageDto> result =
                conversationService.searchMessages("alice", conversationId, "   ", 0, 20);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        verify(messageRepository, never())
                .findByConversationIdAndDeletedFalseAndBodyContainingIgnoreCaseOrderBySeqDesc(any(), any(), any());
    }

    @Test
    @DisplayName("searchMessages executes a full-text query when a term is supplied")
    void searchMessagesWithTerm() {
        when(membershipRepository.existsByUserIdAndConversationId(alice.getId(), conversationId))
                .thenReturn(true);
        ChatMessage msg = ChatMessage.builder().id(UUID.randomUUID()).conversationId(conversationId)
                .senderUsername("alice").seq(1L).body("hello world").status(MessageStatus.SENT).build();
        Page<ChatMessage> page = new PageImpl<>(List.of(msg), PageRequest.of(0, 20), 1);
        when(messageRepository.findByConversationIdAndDeletedFalseAndBodyContainingIgnoreCaseOrderBySeqDesc(
                eq(conversationId), eq("hello"), any())).thenReturn(page);
        when(messageDtoAssembler.toDtos(any())).thenReturn(List.of(MessageDto.from(msg)));

        PagedResponse<MessageDto> result =
                conversationService.searchMessages("alice", conversationId, " hello ", 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getMembers lists admins first, then members ordered by display name")
    void getMembersSortsAdminsFirst() {
        when(membershipRepository.existsByUserIdAndConversationId(alice.getId(), conversationId))
                .thenReturn(true);
        User zoe = User.builder().id(UUID.randomUUID()).username("zoe").displayName("Zoe").build();
        when(membershipRepository.findByConversationId(conversationId)).thenReturn(List.of(
                Membership.builder().userId(bob.getId()).role(MembershipRole.MEMBER).build(),
                Membership.builder().userId(zoe.getId()).role(MembershipRole.ADMIN).build()));
        when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));
        when(userRepository.findById(zoe.getId())).thenReturn(Optional.of(zoe));

        List<ConversationMemberDto> members = conversationService.getMembers("alice", conversationId);

        assertThat(members).hasSize(2);
        assertThat(members.get(0).role()).isEqualTo(MembershipRole.ADMIN);
        assertThat(members.get(0).username()).isEqualTo("zoe");
    }

    @Test
    @DisplayName("markRead advances the read marker when the sequence is newer")
    void markReadAdvancesMarker() {
        Membership membership = Membership.builder().userId(alice.getId())
                .conversationId(conversationId).lastReadSeq(2L).build();
        when(membershipRepository.findByUserIdAndConversationId(alice.getId(), conversationId))
                .thenReturn(Optional.of(membership));

        conversationService.markRead("alice", conversationId, 5L);

        assertThat(membership.getLastReadSeq()).isEqualTo(5L);
        verify(membershipRepository).save(membership);
    }

    @Test
    @DisplayName("markRead does not move the marker backwards")
    void markReadIgnoresOlderSequence() {
        Membership membership = Membership.builder().userId(alice.getId())
                .conversationId(conversationId).lastReadSeq(10L).build();
        when(membershipRepository.findByUserIdAndConversationId(alice.getId(), conversationId))
                .thenReturn(Optional.of(membership));

        conversationService.markRead("alice", conversationId, 5L);

        assertThat(membership.getLastReadSeq()).isEqualTo(10L);
        verify(membershipRepository, never()).save(any());
    }

    @Test
    @DisplayName("markRead rejects a non-member")
    void markReadRejectsNonMember() {
        when(membershipRepository.findByUserIdAndConversationId(alice.getId(), conversationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.markRead("alice", conversationId, 1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("renameGroup updates the title for a group admin and broadcasts the event")
    void renameGroupSucceedsForAdmin() {
        Conversation group = Conversation.builder().id(conversationId)
                .type(ConversationType.GROUP).title("Old").build();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(group));
        when(membershipRepository.findByUserIdAndConversationId(alice.getId(), conversationId))
                .thenReturn(Optional.of(Membership.builder().userId(alice.getId())
                        .conversationId(conversationId).role(MembershipRole.ADMIN).lastReadSeq(0L).build()));

        ConversationDto dto = conversationService.renameGroup("alice", conversationId, "New");

        assertThat(group.getTitle()).isEqualTo("New");
        assertThat(dto.title()).isEqualTo("New");
        verify(auditService).record(eq("alice"), eq("CONVERSATION_RENAMED"), eq("conversation"),
                anyString(), anyString());
        verify(relay).broadcast(anyString(), any());
    }

    @Test
    @DisplayName("renameGroup rejects a non-admin")
    void renameGroupRejectsNonAdmin() {
        Conversation group = Conversation.builder().id(conversationId)
                .type(ConversationType.GROUP).title("Old").build();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(group));
        when(membershipRepository.findByUserIdAndConversationId(alice.getId(), conversationId))
                .thenReturn(Optional.of(Membership.builder().role(MembershipRole.MEMBER).build()));

        assertThatThrownBy(() -> conversationService.renameGroup("alice", conversationId, "New"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only group admins");
    }

    @Test
    @DisplayName("renameGroup rejects a non-group conversation")
    void renameGroupRejectsDirect() {
        Conversation direct = Conversation.builder().id(conversationId)
                .type(ConversationType.DIRECT).build();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(direct));

        assertThatThrownBy(() -> conversationService.renameGroup("alice", conversationId, "New"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only valid for group");
    }

    @Test
    @DisplayName("addMembers adds a new user and skips those already present")
    void addMembersAddsNewOnly() {
        Conversation group = Conversation.builder().id(conversationId)
                .type(ConversationType.GROUP).title("Team").build();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(group));
        when(membershipRepository.findByUserIdAndConversationId(alice.getId(), conversationId))
                .thenReturn(Optional.of(Membership.builder().userId(alice.getId())
                        .conversationId(conversationId).role(MembershipRole.ADMIN).build()));
        when(membershipRepository.existsByUserIdAndConversationId(bob.getId(), conversationId))
                .thenReturn(false);
        // getMembers at the end
        when(membershipRepository.existsByUserIdAndConversationId(alice.getId(), conversationId))
                .thenReturn(true);
        when(membershipRepository.findByConversationId(conversationId)).thenReturn(List.of(
                Membership.builder().userId(bob.getId()).role(MembershipRole.MEMBER).build()));
        when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));

        List<ConversationMemberDto> members =
                conversationService.addMembers("alice", conversationId, List.of("bob"));

        assertThat(members).extracting(ConversationMemberDto::username).contains("bob");
        verify(membershipRepository).save(any(Membership.class));
        verify(auditService).record(eq("alice"), eq("MEMBER_ADDED"), eq("conversation"),
                anyString(), anyString());
    }

    @Test
    @DisplayName("removeMember lets a member leave (remove themselves)")
    void removeMemberSelfLeave() {
        Conversation group = Conversation.builder().id(conversationId)
                .type(ConversationType.GROUP).build();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(group));
        Membership membership = Membership.builder().userId(alice.getId())
                .conversationId(conversationId).role(MembershipRole.MEMBER).build();
        when(membershipRepository.findByUserIdAndConversationId(alice.getId(), conversationId))
                .thenReturn(Optional.of(membership));
        when(userRepository.findById(alice.getId())).thenReturn(Optional.of(alice));

        conversationService.removeMember("alice", conversationId, alice.getId());

        verify(membershipRepository).delete(membership);
        verify(auditService).record(eq("alice"), eq("MEMBER_LEFT"), eq("conversation"),
                anyString(), anyString());
    }

    @Test
    @DisplayName("removeMember blocks a non-admin from removing someone else")
    void removeMemberBlocksNonAdmin() {
        Conversation group = Conversation.builder().id(conversationId)
                .type(ConversationType.GROUP).build();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(group));
        when(membershipRepository.findByUserIdAndConversationId(alice.getId(), conversationId))
                .thenReturn(Optional.of(Membership.builder().userId(alice.getId())
                        .role(MembershipRole.MEMBER).build()));

        assertThatThrownBy(() ->
                conversationService.removeMember("alice", conversationId, bob.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only group admins");
    }

    @Test
    @DisplayName("removeMember refuses to remove the last admin")
    void removeMemberBlocksLastAdmin() {
        Conversation group = Conversation.builder().id(conversationId)
                .type(ConversationType.GROUP).build();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(group));
        when(membershipRepository.findByUserIdAndConversationId(alice.getId(), conversationId))
                .thenReturn(Optional.of(Membership.builder().userId(alice.getId())
                        .role(MembershipRole.ADMIN).build()));
        when(membershipRepository.countByConversationIdAndRole(conversationId, MembershipRole.ADMIN))
                .thenReturn(1L);

        assertThatThrownBy(() ->
                conversationService.removeMember("alice", conversationId, alice.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("last admin");
    }

    @Test
    @DisplayName("updateMemberRole refuses to demote the last admin")
    void updateMemberRoleBlocksLastAdminDemotion() {
        Conversation group = Conversation.builder().id(conversationId)
                .type(ConversationType.GROUP).build();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(group));
        when(membershipRepository.findByUserIdAndConversationId(alice.getId(), conversationId))
                .thenReturn(Optional.of(Membership.builder().userId(alice.getId())
                        .role(MembershipRole.ADMIN).build()));
        when(membershipRepository.countByConversationIdAndRole(conversationId, MembershipRole.ADMIN))
                .thenReturn(1L);

        assertThatThrownBy(() -> conversationService.updateMemberRole(
                "alice", conversationId, alice.getId(), MembershipRole.MEMBER))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("last admin");
    }

    @Test
    @DisplayName("setMembershipPrefs updates mute and archive flags")
    void setMembershipPrefsUpdatesFlags() {
        Membership membership = Membership.builder().userId(alice.getId())
                .conversationId(conversationId).lastReadSeq(0L).build();
        when(membershipRepository.findByUserIdAndConversationId(alice.getId(), conversationId))
                .thenReturn(Optional.of(membership));
        Conversation group = Conversation.builder().id(conversationId)
                .type(ConversationType.GROUP).title("Team").build();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(group));

        ConversationDto dto = conversationService.setMembershipPrefs(
                "alice", conversationId, new MembershipPrefsRequest(true, true));

        assertThat(membership.isMuted()).isTrue();
        assertThat(membership.isArchived()).isTrue();
        assertThat(dto.muted()).isTrue();
        assertThat(dto.archived()).isTrue();
    }

    @Test
    @DisplayName("deleteConversation soft-deletes the whole group for an admin")
    void deleteConversationSoftDeletesForGroupAdmin() {
        Conversation group = Conversation.builder().id(conversationId)
                .type(ConversationType.GROUP).build();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(group));
        when(membershipRepository.findByUserIdAndConversationId(alice.getId(), conversationId))
                .thenReturn(Optional.of(Membership.builder().role(MembershipRole.ADMIN).build()));

        conversationService.deleteConversation("alice", conversationId);

        assertThat(group.isDeleted()).isTrue();
        verify(conversationRepository).save(group);
        verify(auditService).record(eq("alice"), eq("CONVERSATION_DELETED"), eq("conversation"),
                anyString(), any());
    }

    @Test
    @DisplayName("deleteConversation removes only the membership for a non-admin member")
    void deleteConversationRemovesMembershipForMember() {
        Conversation group = Conversation.builder().id(conversationId)
                .type(ConversationType.GROUP).build();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(group));
        Membership membership = Membership.builder().role(MembershipRole.MEMBER).build();
        when(membershipRepository.findByUserIdAndConversationId(alice.getId(), conversationId))
                .thenReturn(Optional.of(membership));

        conversationService.deleteConversation("alice", conversationId);

        assertThat(group.isDeleted()).isFalse();
        verify(membershipRepository).delete(membership);
        verify(auditService).record(eq("alice"), eq("MEMBER_LEFT"), eq("conversation"),
                anyString(), any());
    }

    @Test
    @DisplayName("requireUser surfaces a not-found error for an unknown username")
    void unknownUserThrowsNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.listForUser("ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
