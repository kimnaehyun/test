package com.gt.codinnator.domain.voicechat.controller;

import com.gt.codinnator.domain.voicechat.dto.SignalingMessage;
import com.gt.codinnator.domain.voicechat.dto.VoiceType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequiredArgsConstructor
public class SignallingController {
    private final SimpMessageSendingOperations messagingTemplate;
    private static final Map<String, Set<String>> roomParticipants = new ConcurrentHashMap<>();

    @MessageMapping("/voice/message") // pub
    public void messageHandler(@Payload SignalingMessage message, StompHeaderAccessor headerAccessor) {
        String roomId = message.getRoomId();
        String senderId = message.getSenderId();

        switch (message.getType()){
            case JOIN ->{
                headerAccessor.getSessionAttributes().put("userId", senderId);
                headerAccessor.getSessionAttributes().put("roomId", roomId);

                // 명단 추가
                roomParticipants.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(senderId);
                // 입장 알림
                messagingTemplate.convertAndSend("/sub/voice/room/"+roomId, message);
                // 현재 명단 보여줌
                SignalingMessage listMessage = new SignalingMessage();
                listMessage.setType(VoiceType.PEER_LIST);
                listMessage.setRoomId(roomId);
                listMessage.setData(roomParticipants.get(roomId));

                messagingTemplate.convertAndSend("/sub/voice/room/"+roomId, listMessage);
            }
            case OFFER, ANSWER, ICE, MIC, IDENTITY -> { messagingTemplate.convertAndSend("/sub/voice/room/"+ roomId, message);}
            default -> {
                break;
            }
        }
    }

    // 퇴장 처리
    @EventListener
    public void disconnectHandler(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        String roomId = (String) headerAccessor.getSessionAttributes().get("roomId");

        if(userId != null && roomId != null) {
            Set<String> participants = roomParticipants.get(roomId);
            if (participants != null) {
                participants.remove(userId);
                if (participants.isEmpty()) {
                    roomParticipants.remove(roomId);
                }
            }

            SignalingMessage leaveMessage = new SignalingMessage();
            leaveMessage.setType(VoiceType.LEAVE);
            leaveMessage.setSenderId(userId);
            leaveMessage.setRoomId(roomId);

            messagingTemplate.convertAndSend("/sub/voice/room/" + roomId, leaveMessage);
        }
    }
}
