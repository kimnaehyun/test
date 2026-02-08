package com.gt.codinnator.domain.ai.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * AI 분석 결과(보고서) 저장용 엔티티
 *
 * <p>마이페이지(잔디)에서 조회하기 위해 DB에 영속화한다.
 * - createdAt을 기준으로 날짜/시간을 계산할 수 있음
 */
@Entity
@Table(name = "aireport")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiTestReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    /** 보고서를 생성한 유저 (JWT 인증 기준) */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    /**
     * 레거시/호환용 컬럼.
     * 기존 DB에 content 컬럼이 이미 있을 수 있어, insert 실패를 피하기 위해 함께 저장한다.
     * (없으면 ddl-auto=update에서 자동 추가됨)
     */
    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    /** 테스트 실행 출력(에러 로그/스택트레이스). 길어질 수 있어서 TEXT/LONGTEXT 권장 */
    @Lob
    @Column(name = "stacktrace", columnDefinition = "LONGTEXT")
    private String stacktrace;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Lob
    @Column(name = "error", columnDefinition = "LONGTEXT")
    private String error;

    @Lob
    @Column(name = "resolution", columnDefinition = "LONGTEXT")
    private String resolution;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public AiTestReport(
            Long roomId,
            Long userId,
            String fileName,
            String content,
            String stacktrace,
            String displayName,
            String error,
            String resolution
    ) {
        this.roomId = roomId;
        this.userId = userId;
        this.fileName = fileName;
        this.content = content;
        this.stacktrace = stacktrace;
        this.displayName = displayName;
        this.error = error;
        this.resolution = resolution;
    }
}
