package com.gt.codinnator.domain.user.utils;

import com.gt.codinnator.domain.user.dto.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collections;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    private Key signingKey; // Key 객체로 관리

    private final long tokenValidityInMilliseconds = 1000L * 60 * 60 * 24; // 24시간

    // 객체 생성 후 secretKey를 Key 객체로 변환
    @PostConstruct
    protected void init() {
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    // 토큰 생성
    public String createToken(String gitId) {
        Claims claims = Jwts.claims().setSubject(gitId);
        Date now = new Date();
        Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(signingKey, SignatureAlgorithm.HS256) // 수정한 Key 객체 사용
                .compact();
    }

    // JwtTokenProvider 클래스 내부에 추가
    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey) // parserBuilder 사용 권장
                    .build()
                    .parseClaimsJws(token);
            return !claims.getBody().getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        String gitId = claims.getSubject();

        // 1. UserPrincipal 객체를 직접 생성하여 Principal로 설정
        // UserPrincipal(Long id, String email) 생성자 규격에 맞춤
        UserPrincipal userPrincipal = new UserPrincipal(
                Long.valueOf(gitId), // String을 Long으로 변환
                null // 토큰에 이메일 정보가 없다면 우선 null 처리 (또는 claims에서 추출)
        );

        // 2. 첫 번째 인자에 gitId 대신 userPrincipal 객체를 전달
        return new UsernamePasswordAuthenticationToken(
                userPrincipal,
                token,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}