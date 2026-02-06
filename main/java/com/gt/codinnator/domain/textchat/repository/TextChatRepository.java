package com.gt.codinnator.domain.textchat.repository;

import com.gt.codinnator.domain.textchat.entity.TextChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TextChatRepository extends JpaRepository<TextChatMessageEntity, Long> {
    // 특정 방의 메시지를 최신순으로 가져오기
    List<TextChatMessageEntity> findTop50ByRoomIdOrderByCreatedAtDesc(String roomId);
}