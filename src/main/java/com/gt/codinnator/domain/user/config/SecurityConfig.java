package com.gt.codinnator.domain.user.config;

import com.gt.codinnator.domain.user.service.CustomOAuth2UserService;
import com.gt.codinnator.domain.user.utils.JwtAuthenticationFilter;
import com.gt.codinnator.domain.user.utils.JwtTokenProvider;
import com.gt.codinnator.domain.user.utils.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity // Spring Security 설정을 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    // 유저 정보 처리 서비스
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable()) // 로컬 개발 시에는 편의상 disable (배포 시 보안 검토 필요)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 사용 안함
                .headers(headers -> headers.frameOptions(frame -> frame.disable())) // H2 콘솔 등을 쓸 경우 대비
                .authorizeHttpRequests(auth -> auth
                                .requestMatchers("/ws/**").permitAll()
                                .requestMatchers("/ws-chat/**", "/ws-voice/**").permitAll()
                                .requestMatchers("/", "/login/**", "/oauth2/**").permitAll()
                                .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/") // 로그아웃 성공 시 이동할 페이지
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            // API 요청은 401 반환 (OAuth2 리다이렉트 아님)
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Unauthorized\"}");
                        })
                )
                .oauth2Login(oauth2 -> oauth2
                                .defaultSuccessUrl("/") // 로그인 성공 후 이동할 페이지
                         .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService) // GitHub에서 가져온 유저 정보를 처리할 서비스 등록
                         )
                        .successHandler(oAuth2SuccessHandler) // 성공 시 JWT 핸들러 실행
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // [2] 기존 CorsConfig의 내용을 여기서 빈(Bean)으로 등록
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 1. 허용할 출처 (CorsConfig에 있던 것들)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://i14e205.p.ssafy.io:5173",
                "http://i14e205.p.ssafy.io",
                "https://i14e205.p.ssafy.io" // HTTPS도 혹시 모르니 추가
        ));

        // 2. 허용할 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 3. 허용할 헤더
        configuration.setAllowedHeaders(List.of("*"));

        // 4. 자격 증명 허용 (쿠키, Authorization 헤더 등)
        configuration.setAllowCredentials(true);

        // 5. 브라우저가 응답 헤더를 읽을 수 있도록 허용 (JWT 토큰 등)
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Set-Cookie"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}