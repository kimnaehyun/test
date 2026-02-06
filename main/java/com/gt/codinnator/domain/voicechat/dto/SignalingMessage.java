package com.gt.codinnator.domain.voicechat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignalingMessage {
    private VoiceType type;
    private String roomId;
    private String senderId;
    private String receiverId;
    private Object data; // 실제 신호 데이터
}
