package com.message.mesh.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

/**
 * Application-specific {@link UserDetails} that carries the persistent
 * {@code tokenVersion} used for forced token revocation, alongside the account's
 * enabled (active) state and granted authorities.
 */
public class AppUserDetails implements UserDetails {

    private final UUID id;
    private final String username;
    private final String password;
    private final boolean active;
    private final int tokenVersion;
    private final Collection<? extends GrantedAuthority> authorities;

    public AppUserDetails(UUID id,
                          String username,
                          String password,
                          boolean active,
                          int tokenVersion,
                          Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.active = active;
        this.tokenVersion = tokenVersion;
        this.authorities = authorities;
    }

    public UUID getId() {
        return id;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
