package com.message.mesh.service;

import com.message.mesh.domain.User;
import com.message.mesh.dto.AuthResponse;
import com.message.mesh.dto.UserDto;
import com.message.mesh.enums.UserRole;
import com.message.mesh.exception.BadRequestException;
import com.message.mesh.exception.ResourceNotFoundException;
import com.message.mesh.repository.MembershipRepository;
import com.message.mesh.repository.UserRepository;
import com.message.mesh.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService")
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ProfileService profileService;

    private User user(UserRole role) {
        return User.builder()
                .username("alice").passwordHash("hash").displayName("Alice")
                .role(role).active(true).tokenVersion(1).build();
    }

    @Test
    @DisplayName("updateProfile changes the display name and records an audit entry")
    void updateProfileUpdatesDisplayName() {
        User user = user(UserRole.USER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDto dto = profileService.updateProfile("alice", "Alice Cooper");

        assertThat(dto.displayName()).isEqualTo("Alice Cooper");
        verify(userRepository).save(user);
        verify(auditService).record(eq("alice"), eq("PROFILE_UPDATED"), eq("user"), anyString(), anyString());
    }

    @Test
    @DisplayName("updateProfile throws when the user does not exist")
    void updateProfileThrowsWhenMissing() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.updateProfile("ghost", "X"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("changePassword re-hashes, bumps the token version and returns a fresh token")
    void changePasswordSucceeds() {
        User user = user(UserRole.USER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "hash")).thenReturn(true);
        when(passwordEncoder.encode("newpass")).thenReturn("newhash");
        when(jwtUtil.generateToken("alice", 2)).thenReturn("fresh-token");

        AuthResponse response = profileService.changePassword("alice", "old", "newpass");

        assertThat(response.token()).isEqualTo("fresh-token");
        assertThat(user.getPasswordHash()).isEqualTo("newhash");
        assertThat(user.getTokenVersion()).isEqualTo(2);
        verify(auditService).record(eq("alice"), eq("PASSWORD_CHANGED"), eq("user"), anyString(), any());
    }

    @Test
    @DisplayName("changePassword rejects an incorrect current password")
    void changePasswordRejectsWrongCurrent() {
        User user = user(UserRole.USER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> profileService.changePassword("alice", "wrong", "newpass"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("incorrect");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteAccount removes the user and their memberships")
    void deleteAccountRemovesUser() {
        User user = user(UserRole.USER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        profileService.deleteAccount("alice");

        verify(membershipRepository).deleteByUserId(user.getId());
        verify(userRepository).delete(user);
        verify(auditService).record(eq("alice"), eq("ACCOUNT_DELETED"), eq("user"), anyString(), any());
    }

    @Test
    @DisplayName("deleteAccount blocks the last remaining administrator")
    void deleteAccountBlocksLastAdmin() {
        User admin = user(UserRole.ADMIN);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(admin));
        when(userRepository.findAll()).thenReturn(List.of(admin));

        assertThatThrownBy(() -> profileService.deleteAccount("alice"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("last remaining administrator");

        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteAccount allows an admin when another admin remains")
    void deleteAccountAllowsWhenAnotherAdminExists() {
        User admin = user(UserRole.ADMIN);
        User other = User.builder().username("bob").role(UserRole.ADMIN).build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(admin));
        when(userRepository.findAll()).thenReturn(List.of(admin, other));

        profileService.deleteAccount("alice");

        verify(userRepository).delete(admin);
    }
}
