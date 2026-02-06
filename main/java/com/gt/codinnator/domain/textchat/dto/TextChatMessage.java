package com.gt.codinnator.domain.textchat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TextChatMessage {
    private String roomId;   // 방 번호
    private String sender;   // 보낸 사람
    private String message;  // 내용
    private MessageType type; // ENTER(입장), TALK(채팅)
    private String imageUrl;  // 프로필 이미지 URL (GitHub 아바타)
    private Long timestamp;   // 메시지 전송 시간 (Unix timestamp)

    public enum MessageType { ENTER, TALK }
}