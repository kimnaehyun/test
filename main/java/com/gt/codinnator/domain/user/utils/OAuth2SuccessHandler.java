package com.gt.codinnator.domain.user.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    // GitHub 토큰을 가져오기 위한 서비스 주입
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String gitId = String.valueOf(oAuth2User.getAttributes().get("id"));

        // GitHub Access Token 추출
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );

        // JWT 토큰 생성
        String token = tokenProvider.createToken(gitId);
        String githubAccessToken = client.getAccessToken().getTokenValue();

        // 프론트엔드 주소로 토큰을 쿼리 파라미터에 담아 보냄
        // ex) http://localhost:3000/login/success?token={토큰}
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/home")
                .queryParam("token", token)
                .queryParam("gitToken", githubAccessToken) // GitHub Access Token
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}