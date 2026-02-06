package com.gt.codinnator.domain.room.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RoomReqDto {
    private String name;     // 방 이름
    private String gitToken; // 깃 토큰
    private String branch; // 브랜치
}