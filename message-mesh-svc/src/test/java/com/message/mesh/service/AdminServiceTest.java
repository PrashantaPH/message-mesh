package com.message.mesh.service;

import com.message.mesh.domain.Conversation;
import com.message.mesh.domain.Membership;
import com.message.mesh.domain.User;
import com.message.mesh.dto.AdminStatsDto;
import com.message.mesh.dto.AdminUserDetailDto;
import com.message.mesh.dto.AdminUserDto;
import com.message.mesh.dto.CreateUserRequest;
import com.message.mesh.dto.PagedResponse;
import com.message.mesh.enums.ConversationType;
import com.message.mesh.enums.UserRole;
import com.message.mesh.exception.BadRequestException;
import com.message.mesh.exception.ResourceNotFoundException;
import com.message.mesh.repository.AuditEventRepository;
import com.message.mesh.repository.ConversationRepository;
import com.message.mesh.repository.MembershipRepository;
import com.message.mesh.repository.MessageRepository;
import com.message.mesh.repository.UserRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
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
@DisplayName("AdminService")
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private AuditEventRepository auditEventRepository;
    @Mock
    private PresenceService presenceService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private AdminService adminService;

    private User target;
    private UUID targetId;

    @BeforeEach
    void setUp() {
        targetId = UUID.randomUUID();
        target = User.builder().id(targetId).username("bob").passwordHash("hash")
                .displayName("Bob").role(UserRole.USER).active(true).tokenVersion(0).build();
        when(presenceService.onlineUsernames()).thenReturn(Set.of());
        when(membershipRepository.countByUserId(any())).thenReturn(0L);
    }

    @Test
    @DisplayName("listUsers maps the search page and marks online users")
    void listUsersMapsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> page = new PageImpl<>(List.of(target), pageable, 1);
        when(userRepository.search(eq("bob"), eq(UserRole.USER), eq(true), eq(pageable))).thenReturn(page);
        when(presenceService.onlineUsernames()).thenReturn(Set.of("bob"));

        PagedResponse<AdminUserDto> result =
                adminService.listUsers("bob", UserRole.USER, true, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).online()).isTrue();
    }

    @Test
    @DisplayName("listUsers normalizes a blank query to null")
    void listUsersNormalizesBlankQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.search(eq(null), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PagedResponse<AdminUserDto> result = adminService.listUsers("   ", null, null, pageable);

        assertThat(result.content()).isEmpty();
    }

    @Test
    @DisplayName("updateRole promotes a user and bumps their token version")
    void updateRolePromotes() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AdminUserDto dto = adminService.updateRole(targetId, UserRole.ADMIN, "root");

        assertThat(dto.role()).isEqualTo(UserRole.ADMIN);
        assertThat(target.getTokenVersion()).isEqualTo(1);
        verify(auditService).record(eq("root"), eq("ADMIN_ROLE_CHANGED"), eq("user"), anyString(), anyString());
    }

    @Test
    @DisplayName("updateRole forbids revoking your own admin role")
    void updateRoleForbidsSelfDemotion() {
        target.setUsername("root");
        target.setRole(UserRole.ADMIN);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> adminService.updateRole(targetId, UserRole.USER, "root"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("your own admin role");
    }

    @Test
    @DisplayName("updateRole forbids demoting the last remaining admin")
    void updateRoleForbidsLastAdminDemotion() {
        target.setRole(UserRole.ADMIN);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRepository.findAll()).thenReturn(List.of(target));

        assertThatThrownBy(() -> adminService.updateRole(targetId, UserRole.USER, "root"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("last remaining administrator");
    }

    @Test
    @DisplayName("updateStatus deactivates a user and bumps the token version")
    void updateStatusDeactivates() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AdminUserDto dto = adminService.updateStatus(targetId, false, "root");

        assertThat(dto.active()).isFalse();
        assertThat(target.getTokenVersion()).isEqualTo(1);
        verify(auditService).record(eq("root"), eq("ADMIN_USER_DEACTIVATED"), eq("user"), anyString(), any());
    }

    @Test
    @DisplayName("updateStatus forbids deactivating your own account")
    void updateStatusForbidsSelfDeactivation() {
        target.setUsername("root");
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> adminService.updateStatus(targetId, false, "root"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("your own account");
    }

    @Test
    @DisplayName("updateStatus forbids deactivating the last active admin")
    void updateStatusForbidsLastActiveAdmin() {
        target.setRole(UserRole.ADMIN);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRepository.findAll()).thenReturn(List.of(target));

        assertThatThrownBy(() -> adminService.updateStatus(targetId, false, "root"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("last remaining administrator");
    }

    @Test
    @DisplayName("resetPassword re-hashes the password and revokes sessions")
    void resetPasswordRehashes() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(passwordEncoder.encode("newpass")).thenReturn("newhash");

        adminService.resetPassword(targetId, "newpass", "root");

        assertThat(target.getPasswordHash()).isEqualTo("newhash");
        assertThat(target.getTokenVersion()).isEqualTo(1);
        verify(auditService).record(eq("root"), eq("ADMIN_PASSWORD_RESET"), eq("user"), anyString(), any());
    }

    @Test
    @DisplayName("deleteUser removes the account and its memberships")
    void deleteUserRemoves() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        adminService.deleteUser(targetId, "root");

        verify(membershipRepository).deleteByUserId(targetId);
        verify(userRepository).delete(target);
        verify(auditService).record(eq("root"), eq("ADMIN_USER_DELETED"), eq("user"), anyString(), anyString());
    }

    @Test
    @DisplayName("deleteUser forbids deleting your own account")
    void deleteUserForbidsSelf() {
        target.setUsername("root");
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> adminService.deleteUser(targetId, "root"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("your own account");
    }

    @Test
    @DisplayName("deleteUser forbids deleting the last remaining admin")
    void deleteUserForbidsLastAdmin() {
        target.setRole(UserRole.ADMIN);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRepository.findAll()).thenReturn(List.of(target));

        assertThatThrownBy(() -> adminService.deleteUser(targetId, "root"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("last remaining administrator");
    }

    @Test
    @DisplayName("createUser provisions a new account with the requested role")
    void createUserProvisions() {
        CreateUserRequest req = new CreateUserRequest("carol", "secret123", "Carol", UserRole.ADMIN);
        when(userRepository.existsByUsername("carol")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AdminUserDto dto = adminService.createUser(req, "root");

        assertThat(dto.username()).isEqualTo("carol");
        assertThat(dto.role()).isEqualTo(UserRole.ADMIN);
        verify(auditService).record(eq("root"), eq("ADMIN_USER_CREATED"), eq("user"), anyString(), anyString());
    }

    @Test
    @DisplayName("createUser rejects a duplicate username")
    void createUserRejectsDuplicate() {
        CreateUserRequest req = new CreateUserRequest("bob", "secret123", "Bob", UserRole.USER);
        when(userRepository.existsByUsername("bob")).thenReturn(true);

        assertThatThrownBy(() -> adminService.createUser(req, "root"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("revokeSessions bumps the token version")
    void revokeSessionsBumpsVersion() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adminService.revokeSessions(targetId, "root");

        assertThat(target.getTokenVersion()).isEqualTo(1);
        verify(auditService).record(eq("root"), eq("ADMIN_SESSIONS_REVOKED"), eq("user"), anyString(), any());
    }

    @Test
    @DisplayName("getUserDetail returns the user with their conversations newest-first")
    void getUserDetailReturnsConversations() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        UUID convId = UUID.randomUUID();
        when(membershipRepository.findByUserId(targetId)).thenReturn(List.of(
                Membership.builder().userId(targetId).conversationId(convId).build()));
        Conversation conv = Conversation.builder().id(convId).type(ConversationType.GROUP)
                .title("Team").createdAt(java.time.Instant.now()).build();
        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conv));
        when(membershipRepository.countByConversationId(convId)).thenReturn(3L);
        when(messageRepository.countByConversationId(convId)).thenReturn(10L);

        AdminUserDetailDto detail = adminService.getUserDetail(targetId);

        assertThat(detail.user().username()).isEqualTo("bob");
        assertThat(detail.conversations()).hasSize(1);
        assertThat(detail.conversations().get(0).title()).isEqualTo("Team");
    }

    @Test
    @DisplayName("getUser throws for an unknown id")
    void getUserThrowsWhenMissing() {
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getUserDetail(targetId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("stats aggregates platform-wide counts")
    void statsAggregatesCounts() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByActive(true)).thenReturn(8L);
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(2L);
        when(userRepository.countByCreatedAtAfter(any())).thenReturn(3L);
        when(presenceService.onlineUsernames()).thenReturn(Set.of("a", "b"));
        when(conversationRepository.count()).thenReturn(5L);
        when(conversationRepository.countByType(ConversationType.GROUP)).thenReturn(2L);
        when(conversationRepository.countByType(ConversationType.DIRECT)).thenReturn(3L);
        when(conversationRepository.countByDeleted(true)).thenReturn(1L);
        when(messageRepository.count()).thenReturn(100L);
        when(messageRepository.countByCreatedAtAfter(any())).thenReturn(20L);
        when(auditEventRepository.count()).thenReturn(50L);

        AdminStatsDto stats = adminService.stats();

        assertThat(stats.totalUsers()).isEqualTo(10L);
        assertThat(stats.activeUsers()).isEqualTo(8L);
        assertThat(stats.inactiveUsers()).isEqualTo(2L);
        assertThat(stats.adminUsers()).isEqualTo(2L);
        assertThat(stats.onlineUsers()).isEqualTo(2L);
        assertThat(stats.totalMessages()).isEqualTo(100L);
        assertThat(stats.auditEvents()).isEqualTo(50L);
    }
}
