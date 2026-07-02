package com.message.mesh.security;

import com.message.mesh.domain.User;
import com.message.mesh.enums.UserRole;
import com.message.mesh.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    @DisplayName("loadUserByUsername maps the user to AppUserDetails with a role authority")
    void loadsUserWithAuthority() {
        User user = User.builder().id(UUID.randomUUID()).username("alice").passwordHash("hash")
                .displayName("Alice").role(UserRole.ADMIN).active(true).tokenVersion(3).build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).extracting("authority").contains("ROLE_ADMIN");
        assertThat(details).isInstanceOf(AppUserDetails.class);
        assertThat(((AppUserDetails) details).getTokenVersion()).isEqualTo(3);
    }

    @Test
    @DisplayName("loadUserByUsername throws when the user is not found")
    void throwsWhenMissing() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
