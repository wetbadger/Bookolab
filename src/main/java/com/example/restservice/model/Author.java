package com.example.restservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
public class Author implements UserDetails {
    @Id
    @GeneratedValue
    private Long id;

    private LocalDateTime bannedUntil;

    @Column(unique = true, nullable = false, length = 30)
    private String username;

    @Column(nullable = false)
    private String password;

    private boolean enabled;
    // private String verificationCode;
    // private LocalDateTime verificationCodeExpiresAt;

    public Author(String username, String password) {
        this.username = username;
        this.password = password;
        this.enabled = true;
    }

    public Author() {
        this.enabled = true;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        if (bannedUntil != null && bannedUntil.isAfter(LocalDateTime.now())) {
            return false; // They are temporarily banned
        }
        return this.enabled; // Fall back to their permanent ban status
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // Add standard getters/setters for it at the bottom:
    public LocalDateTime getBannedUntil() { return bannedUntil; }
    public void setBannedUntil(LocalDateTime bannedUntil) { this.bannedUntil = bannedUntil; }
}
