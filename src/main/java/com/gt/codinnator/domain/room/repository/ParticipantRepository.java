package com.gt.codinnator.domain.room.repository;

import com.gt.codinnator.domain.room.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    // 특정 방에 참여 중인 유저 목록을 찾기 위한 메서드
    List<Participant> findByRoomId(Long roomId);

    // ✅ 특정 유저가 참여 중인 방 목록을 찾기 위한 메서드
    List<Participant> findByUserId(Long userId);

    // ✅ 특정 방(roomId)에 특정 유저(userId)가 이미 등록되어 있는지 확인
    boolean existsByRoomIdAndUserId(Long roomId, Long userId);
}
