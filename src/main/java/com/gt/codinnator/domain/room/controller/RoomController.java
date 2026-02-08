package com.gt.codinnator.domain.room.controller;

import com.gt.codinnator.domain.room.dto.RoomReqDto;
import com.gt.codinnator.domain.room.service.RoomService;
import com.gt.codinnator.domain.user.dto.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/room")
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<Long> createRoom(
            @RequestBody RoomReqDto dto,
            @AuthenticationPrincipal UserPrincipal userPrincipal // 1. 인증된 유저 정보 주입
    ) {
        // 2. 서비스 호출 시 유저의 ID(PK)를 함께 전달
        Long roomId = roomService.createRoom(dto, userPrincipal.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(roomId);
    }
}