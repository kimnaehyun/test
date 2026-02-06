package com.gt.codinnator.domain.textchat.controller;

import com.gt.codinnator.domain.textchat.dto.TextChatMessage;
import com.gt.codinnator.domain.textchat.service.TextChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TextChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final TextChatService chatService; // DB 저장을 위한 서비스

    /**
     * 클라이언트가 /pub/chat/message로 메시지를 보내면 호출됩니다.
     */
    @MessageMapping("/chat/message") // 클라이언트가 /pub/chat/message로 보낼 때
    public void message(TextChatMessage message) {
        // 0. 서버에서 타임스탬프 설정 (클라이언트가 보내지 않은 경우)
        if (message.getTimestamp() == null) {
            message.setTimestamp(System.currentTimeMillis());
        }

        // 1. 입장 메시지 처리
        if (TextChatMessage.MessageType.ENTER.equals(message.getType())) {
            message.setMessage(message.getSender() + "님이 입장하셨습니다.");
        }

        // 2. 해당 방을 구독 중인 모든 클라이언트에게 메시지 전송 (/sub/chat/room/{roomId})
        messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), message);

        // 3. 채팅 메시지인 경우 DB에 저장 (Service 레이어 호출)
        if (TextChatMessage.MessageType.TALK.equals(message.getType())) {
            chatService.saveMessage(message);
        }
    }

    /**
     * 특정 채팅방의 이전 대화 내역 조회 API
     */
    @GetMapping("/chat/room/{roomId}/history")
    @ResponseBody
    public List<TextChatMessage> getChatHistory(@PathVariable String roomId) {
        return chatService.getChatHistory(roomId);
    }

}