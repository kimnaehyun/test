package com.gt.codinnator.domain.room.controller;

import com.gt.codinnator.domain.room.entity.Participant;
import com.gt.codinnator.domain.room.repository.ParticipantRepository;
import com.gt.codinnator.domain.user.dto.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ✅ 방 참가자(Participant) 등록용 컨트롤러
 *
 * - 같은 방에서 생성된 AI 리포트를 "참가자 전체"가 마이페이지에서 볼 수 있게 하려면
 *   "내가 참가한 방 목록"을 DB에서 뽑을 수 있어야 함.
 * - 기존에는 방장만 Participant에 들어갔어서, 초대받은 유저는 방 참가 이력이 DB에 없었음.
 * - 이 API를 방 입장(또는 참여하기 버튼) 시 1회 호출해서 참가자 테이블에 upsert(중복 방지)한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/room")
public class ParticipantController {

    private final ParticipantRepository participantRepository;

    /**
     * ✅ 내 계정을 현재 roomId의 참가자로 등록
     * - 중복 호출되어도 exists 체크로 1회만 저장됨(멱등)
     */
    @PostMapping("/{roomId}/participants/me")
    public ResponseEntity<Void> joinRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = principal.getId();

        if (!participantRepository.existsByRoomIdAndUserId(roomId, userId)) {
            participantRepository.save(
                    Participant.builder()
                            .roomId(roomId)
                            .userId(userId)
                            .build()
            );
        }

        return ResponseEntity.ok().build();
    }
}
