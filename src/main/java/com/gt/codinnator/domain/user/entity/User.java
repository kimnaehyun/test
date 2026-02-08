package com.gt.codinnator.domain.user.entity;

import com.gt.codinnator.domain.user.utils.GitTokenConverter;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "User")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String name;

    private String gitId; // GitHub의 고유 ID

    @Column(columnDefinition = "TEXT")
    @Convert(converter = GitTokenConverter.class) // 자동 암호화
    private String gitToken;

    private String email;

    private String imgUrl;

    private LocalDateTime createdAt;

    private String isDeleted = "F";

    @Builder
    public User(String name, String gitId, String gitToken, String email, String imgUrl) {
        this.name = name;
        this.gitId = gitId;
        this.gitToken = gitToken;
        this.email = email;
        this.imgUrl = imgUrl;
        this.createdAt = LocalDateTime.now();
    }

    // 기존 유저 정보 업데이트 로직
    public User update(String name, String email, String imgUrl, String gitToken) {
        this.name = name;
        this.email = email;
        this.imgUrl = imgUrl;
        this.gitToken = gitToken;
        return this;
    }
}