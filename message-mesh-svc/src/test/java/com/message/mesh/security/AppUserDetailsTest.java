package com.message.mesh.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AppUserDetails")
class AppUserDetailsTest {

    @Test
    @DisplayName("exposes the account fields and standard UserDetails flags")
    void exposesFields() {
        UUID id = UUID.randomUUID();
        AppUserDetails details = new AppUserDetails(id, "alice", "hash", true, 4,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        assertThat(details.getId()).isEqualTo(id);
        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getTokenVersion()).isEqualTo(4);
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
        assertThat(details.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("reports a deactivated account as disabled")
    void deactivatedAccountIsDisabled() {
        AppUserDetails details = new AppUserDetails(UUID.randomUUID(), "bob", "hash", false, 0, List.of());

        assertThat(details.isEnabled()).isFalse();
    }
}
