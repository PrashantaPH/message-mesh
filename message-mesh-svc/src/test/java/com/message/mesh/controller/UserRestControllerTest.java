package com.message.mesh.controller;

import com.message.mesh.domain.User;
import com.message.mesh.dto.AuthResponse;
import com.message.mesh.dto.ChangePasswordRequest;
import com.message.mesh.dto.UpdateProfileRequest;
import com.message.mesh.dto.UserDto;
import com.message.mesh.enums.UserRole;
import com.message.mesh.exception.ResourceNotFoundException;
import com.message.mesh.repository.UserRepository;
import com.message.mesh.service.PresenceService;
import com.message.mesh.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserRestController")
class UserRestControllerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PresenceService presenceService;
    @Mock
    private ProfileService profileService;
    @Mock
    private Principal principal;

    @InjectMocks
    private UserRestController controller;

    @BeforeEach
    void setUp() {
        when(principal.getName()).thenReturn("alice");
    }

    private User user(String username) {
        return User.builder().id(UUID.randomUUID()).username(username).passwordHash("x")
                .displayName(username).role(UserRole.USER).active(true).build();
    }

    @Test
    @DisplayName("me returns the authenticated user's profile")
    void meReturnsProfile() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user("alice")));

        UserDto dto = controller.me(principal);

        assertThat(dto.username()).isEqualTo("alice");
    }

    @Test
    @DisplayName("me throws when the authenticated user no longer exists")
    void meThrowsWhenMissing() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.me(principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateMe delegates to the profile service")
    void updateMeDelegates() {
        UserDto expected = new UserDto(UUID.randomUUID(), "alice", "New Name", UserRole.USER);
        when(profileService.updateProfile("alice", "New Name")).thenReturn(expected);

        UserDto dto = controller.updateMe(new UpdateProfileRequest("New Name"), principal);

        assertThat(dto).isSameAs(expected);
    }

    @Test
    @DisplayName("changePassword returns a freshly issued token")
    void changePasswordDelegates() {
        AuthResponse expected = new AuthResponse("new-jwt",
                new UserDto(UUID.randomUUID(), "alice", "Alice", UserRole.USER));
        when(profileService.changePassword("alice", "old", "new-secret")).thenReturn(expected);

        AuthResponse response =
                controller.changePassword(new ChangePasswordRequest("old", "new-secret"), principal);

        assertThat(response).isSameAs(expected);
    }

    @Test
    @DisplayName("deleteMe returns 204 No Content")
    void deleteMeReturnsNoContent() {
        ResponseEntity<Void> response = controller.deleteMe(principal);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(profileService).deleteAccount("alice");
    }

    @Test
    @DisplayName("list excludes the calling user")
    void listExcludesCaller() {
        when(userRepository.findAll()).thenReturn(List.of(user("alice"), user("bob")));

        List<UserDto> users = controller.list(principal);

        assertThat(users).extracting(UserDto::username).containsExactly("bob");
    }

    @Test
    @DisplayName("online returns the presence roster")
    void onlineReturnsRoster() {
        when(presenceService.onlineUsernames()).thenReturn(Set.of("bob", "carol"));

        Set<String> online = controller.online();

        assertThat(online).containsExactlyInAnyOrder("bob", "carol");
    }
}
