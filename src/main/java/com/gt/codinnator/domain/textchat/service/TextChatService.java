package com.gt.codinnator.domain.textchat.service;

import com.gt.codinnator.domain.textchat.dto.TextChatMessage;
import com.gt.codinnator.domain.textchat.entity.TextChatMessageEntity;
import com.gt.codinnator.domain.textchat.repository.TextChatRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableAsync
public class TextChatService {

    private final TextChatRepository chatRepository;

    /**
     * 메시지 저장 (비동기 처리를 권장하지만, 일단은 기본형으로 작성합니다)
     */
    @Transactional
    @Async
    public void saveMessage(TextChatMessage dto) {
        TextChatMessageEntity entity = TextChatMessageEntity.builder()
                .roomId(dto.getRoomId())
                .sender(dto.getSender())
                .message(dto.getMessage())
                .build();

        chatRepository.save(entity);
        log.info("Message saved for room: {}", dto.getRoomId());
    }

    /**
     * 이전 채팅 내역 조회 (방 입장 시 호출)
     */
    @Transactional(readOnly = true)
    public List<TextChatMessage> getChatHistory(String roomId) {
        return chatRepository.findTop50ByRoomIdOrderByCreatedAtDesc(roomId)
                .stream()
                .map(entity -> {
                    TextChatMessage dto = new TextChatMessage();
                    dto.setRoomId(entity.getRoomId());
                    dto.setSender(entity.getSender());
                    dto.setMessage(entity.getMessage());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}