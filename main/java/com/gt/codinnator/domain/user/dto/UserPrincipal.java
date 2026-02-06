package com.gt.codinnator.domain.user.dto;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;

public class UserPrincipal implements UserDetails {
    private Long id;
    private String email;

    public UserPrincipal(Long id, String email) {
        this.id = id;
        this.email = email;
    }

    public Long getId() { return id; }

    @Override
    public String getUsername() { return email; }

    @Override
    public String getPassword() { return null; } // 소셜 로그인이나 JWT 사용 시 보통 null

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // 필요 시 권한(ROLE_USER 등) 추가
    }

    // 나머지 UserDetails 오버라이드 메서드 (true 반환)
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}