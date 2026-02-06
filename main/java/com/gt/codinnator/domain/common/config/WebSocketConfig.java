package com.gt.codinnator.domain.common.config;

import com.gt.codinnator.domain.editor.config.handler.CodeHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketConfigurer, WebSocketMessageBrokerConfigurer {

    private final CodeHandler yjsWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(yjsWebSocketHandler, "/ws/code/**")
                .setAllowedOrigins("http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "http://i14e205.p.ssafy.io",
                        "https://i14e205.p.ssafy.io");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지를 구독하는 요청 설정 (받는 곳)
        config.enableSimpleBroker("/sub");
        // 메시지를 보내는 요청 설정 (보내는 곳)
        config.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 채팅 연결 주소: ws://localhost:8080/ws-chat
        registry.addEndpoint("/ws-chat", "/ws-voice").setAllowedOriginPatterns("*").withSockJS();
    }
}
