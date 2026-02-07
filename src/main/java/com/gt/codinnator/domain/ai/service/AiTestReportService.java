package com.gt.codinnator.domain.ai.service;

import com.gt.codinnator.domain.ai.entity.AiTestReport;
import com.gt.codinnator.domain.ai.repository.AiTestReportRepository;
import com.gt.codinnator.domain.room.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiTestReportService {

    private final AiTestReportRepository aiTestReportRepository;
    private final ParticipantRepository participantRepository;

    /**
     * 스택트레이스/로그가 너무 길면 DB 용량이 급격히 늘 수 있어서 제한.
     * (프론트 contributionStore의 MAX_STACK과 동일하게 맞춰두면 UI/DB가 일관됨)
     */
    private static final int MAX_STACKTRACE = 8000;

    /**
     * AI 분석 결과를 DB에 저장
     */
    @Transactional
    public AiTestReport saveReport(
            Long userId,
            Long roomId,
            String fileName,
            String stacktrace,
            TestCodeGenerationService.AiReport aiReport
    ) {
        String trimmedStack = trimStacktrace(stacktrace);
        String content = buildContent(fileName, trimmedStack, aiReport);

        AiTestReport entity = AiTestReport.builder()
                .userId(userId)
                .roomId(roomId)
                .fileName(fileName)
                .content(content)
                .stacktrace(trimmedStack)
                .displayName(aiReport == null ? null : aiReport.display_name())
                .error(aiReport == null ? null : aiReport.error())
                .resolution(aiReport == null ? null : aiReport.resolution())
                .build();

        return aiTestReportRepository.save(entity);
    }

    /**
     * (레거시) 본인이 생성한 report만 조회
     */
    @Transactional(readOnly = true)
    public List<AiTestReport> getMyReports(Long userId) {
        return aiTestReportRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * ✅ 내가 "참여한 방" 기준으로 report 조회
     * - 같은 방에서 다른 사람이 만든 report도 내 마이페이지에서 보이게 함
     * - Participant 테이블에 등록된 roomId 목록을 기반으로 한번에 조회한다
     */
    @Transactional(readOnly = true)
    public List<AiTestReport> getMyRoomReports(Long userId) {
        List<Long> roomIds = participantRepository.findByUserId(userId).stream()
                .map(p -> p.getRoomId())
                .distinct()
                .toList();

        if (roomIds.isEmpty()) {
            return List.of();
        }

        return aiTestReportRepository.findByRoomIdInOrderByCreatedAtDesc(roomIds);
    }

    private String trimStacktrace(String stacktrace) {
        if (stacktrace == null) return null;
        if (stacktrace.length() <= MAX_STACKTRACE) return stacktrace;
        return stacktrace.substring(0, MAX_STACKTRACE) + "\n...(truncated)";
    }

    private String buildContent(String fileName, String trimmedStack, TestCodeGenerationService.AiReport aiReport) {
        String dn = aiReport == null ? "" : String.valueOf(aiReport.display_name());
        String err = aiReport == null ? "" : String.valueOf(aiReport.error());
        String res = aiReport == null ? "" : String.valueOf(aiReport.resolution());

        // DB에 단일 컬럼(TEXT)로도 확인할 수 있게 간단한 리포트 텍스트로 저장
        return "fileName: " + (fileName == null ? "" : fileName) + "\n\n"
                + "display_name: " + dn + "\n\n"
                + "error: " + err + "\n\n"
                + "resolution: " + res + "\n\n"
                + "stacktrace:\n" + (trimmedStack == null ? "" : trimmedStack);
    }
}
