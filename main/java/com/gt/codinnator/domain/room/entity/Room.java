package com.gt.codinnator.domain.room.entity;

import com.gt.codinnator.domain.user.utils.GitTokenConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert // null인 필드는 SQL 인서트문에서 제외하여 DB Default값 적용
@ToString
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "title", length = 20)
    private String title;

    @Column(name = "room_url", length = 1000)
    private String roomUrl;

    @CreationTimestamp // 생성 시 자동 시간 입력 (SQL의 CURRENT_TIMESTAMP 대응)
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Lob // SQL의 TEXT 타입 대응
    @Column(name = "git_token", columnDefinition = "TEXT")
    @Convert(converter = GitTokenConverter.class) // 자동 암호화
    private String gitToken;

    @Column(name = "is_deleted", length = 1)
    @ColumnDefault("'F'") // DB 스키마와 일치감을 위해 추가
    private String isDeleted;

    @Column(name = "git_url", length = 1000)
    private String gitUrl;

    @Column(name = "branch", length = 100)
    private String branch;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "file_id")
    private Long fileId;

    @Builder
    public Room(String title, String roomUrl, String gitToken, String gitUrl, String branch, Long userId, Long fileId, String isDeleted) {
        this.title = title;
        this.roomUrl = roomUrl;
        this.gitToken = gitToken;
        this.gitUrl = gitUrl;
        this.branch = branch;
        this.userId = userId;
        this.fileId = fileId;
        this.isDeleted = isDeleted; // 필요 시 빌더를 통해 상태 변경 가능
    }
}