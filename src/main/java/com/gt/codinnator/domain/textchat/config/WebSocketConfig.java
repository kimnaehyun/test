//package com.gt.codinnator.domain.textchat.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.messaging.simp.config.MessageBrokerRegistry;
//import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
//import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
//import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
//
//@Configuration
//@EnableWebSocketMessageBroker
//public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
//
//    @Override
//    public void configureMessageBroker(MessageBrokerRegistry config) {
//        // 메시지를 구독하는 요청 설정 (받는 곳)
//        config.enableSimpleBroker("/sub");
//        // 메시지를 보내는 요청 설정 (보내는 곳)..............................
//        config.setApplicationDestinationPrefixes("/pub");
//    }
//
//    @Override
//    public void registerStompEndpoints(StompEndpointRegistry registry) {
//        // 채팅 연결 주소: ws://localhost:8080/ws-chat
//        registry.addEndpoint("/ws-chat").setAllowedOriginPatterns("*").withSockJS();
//    }
//}