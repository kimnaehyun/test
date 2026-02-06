package com.gt.codinnator.domain.user.service;

import com.gt.codinnator.domain.user.entity.User;
import com.gt.codinnator.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 1. Github에서 가져온 정보
        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "github"
        String accessToken = userRequest.getAccessToken().getTokenValue(); // git_token에 저장할 값

        // 2. GitHub 유저 속성 (id, login, email, avatar_url 등)
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String gitId = String.valueOf(attributes.get("id"));
        String name = (String) attributes.get("login"); // GitHub 닉네임
        String email = (String) attributes.get("email");
        String imgUrl = (String) attributes.get("avatar_url");

        // 3. DB 저장 또는 업데이트
        User user = saveOrUpdate(gitId, name, email, imgUrl, accessToken);

        // 4. 세션이나 컨텍스트에 담을 유저 객체 반환
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id" // GitHub의 고유 식별자 키 이름
        );
    }

    private User saveOrUpdate(String gitId, String name, String email, String imgUrl, String token) {
        User user = userRepository.findByGitId(gitId)
                .map(entity -> entity.update(name, email, imgUrl, token)) // 이미 있으면 업데이트
                .orElse(User.builder() // 없으면 신규 가입
                        .gitId(gitId)
                        .name(name)
                        .email(email)
                        .imgUrl(imgUrl)
                        .gitToken(token)
                        .build());

        return userRepository.save(user);
    }
}