package com.gt.codinnator.domain.ai.controller;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.gt.codinnator.domain.ai.entity.AiTestReport;
import com.gt.codinnator.domain.ai.service.AiTestReportService;
import com.gt.codinnator.domain.ai.service.TestCodeGenerationService;
import com.gt.codinnator.domain.user.dto.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/room")
@RequiredArgsConstructor
public class AiFileController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final TestCodeGenerationService testCodeGenerationService;
    private final AiTestReportService aiTestReportService;

    // ===================== [테스트 코드 생성 - 신규 API] =====================
    @PostMapping(
            value = "/generate-testcode",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> generateTestCode(
            @RequestBody GenerateTestCodeRequest request
    ) {
        try {
            System.out.println("[AiFileController] generateTestCode 요청");
            System.out.println("  - roomId: " + request.roomId());
            System.out.println("  - fileName: " + request.fileName());
            System.out.println("  - code 길이: " +
                    (request.code() == null ? "null" : request.code().length() + " chars"));

            String generatedTestCode = testCodeGenerationService.generateTestCode(
                    request.fileName(),
                    request.code()
            );

            GenerateTestCodeResponse response = new GenerateTestCodeResponse(
                    request.roomId(),
                    request.fileName(),
                    generatedTestCode
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            String traceId = UUID.randomUUID().toString();
            System.err.println("[generate-testcode] 에러(traceId=" + traceId + "): " + e.getMessage());
            e.printStackTrace();

            ApiError error = ApiError.of(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "테스트 코드 생성 중 서버 오류",
                    e,
                    traceId,
                    request.roomId(),
                    request.fileName()
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ===================== [AI 분석 결과 저장 - 실패일 때만 저장!] =====================
    @PostMapping(
            value = "/analyze-result",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> analyzeResult(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody AnalyzeResultRequest request
    ) {
        String traceId = UUID.randomUUID().toString();

        try {
            if (principal == null) {
                ApiError error = ApiError.of(
                        HttpStatus.UNAUTHORIZED,
                        "인증이 필요합니다 (토큰 확인)",
                        null,
                        traceId,
                        request.roomId(),
                        request.fileName()
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            System.out.println("[AiFileController] analyzeResult 요청(traceId=" + traceId + ")");
            System.out.println("  - userId: " + principal.getId());
            System.out.println("  - roomId: " + request.roomId());
            System.out.println("  - fileName: " + request.fileName());
            System.out.println("  - runOutput 길이: " +
                    (request.runOutput() == null ? "null" : request.runOutput().length() + " chars"));

            if (request.runOutput() == null || request.runOutput().isBlank()) {
                ApiError error = ApiError.of(
                        HttpStatus.BAD_REQUEST,
                        "runOutput이 비어있습니다",
                        null,
                        traceId,
                        request.roomId(),
                        request.fileName()
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // ✅ 0) 테스트 성공이면: AI 분석/DB 저장 자체를 하지 않음 (오류 잔디에 심지 않기!)
            if (!isTestFailed(request.runOutput())) {
                System.out.println("[AiFileController] 테스트 성공으로 판단 → AI 분석/DB 저장 생략(traceId=" + traceId + ")");
                return ResponseEntity.noContent().build(); // 204
            }

            // 1) AI 서버에 분석 요청 (실패일 때만!)
            TestCodeGenerationService.AiReport aiReport =
                    testCodeGenerationService.generateReport(request.runOutput());

            // 2) DB에 보고서 저장 (실패일 때만!)
            AiTestReport saved = aiTestReportService.saveReport(
                    principal.getId(),
                    request.roomId(),
                    request.fileName(),
                    request.runOutput(),
                    aiReport
            );

            // 3) 응답 DTO 생성 (timestamp는 KST로 내려주기)
            String timestampKst = saved.getCreatedAt()
                    .atZone(KST)
                    .toOffsetDateTime()
                    .toString();

            TestReportResponse response = new TestReportResponse(
                    saved.getReportId(),
                    saved.getRoomId(),
                    timestampKst,
                    saved.getStacktrace(),
                    saved.getDisplayName(),
                    saved.getError(),
                    saved.getResolution()
            );

            System.out.println("[AiFileController] AI 분석 완료(traceId=" + traceId + "): " + aiReport.display_name());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("[analyze-result] 에러(traceId=" + traceId + "): " + e.getMessage());
            e.printStackTrace();

            ApiError error = ApiError.of(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI 분석 처리 중 서버 오류",
                    e,
                    traceId,
                    request.roomId(),
                    request.fileName()
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ===================== [내 보고서 조회 - MyPage] =====================
    @GetMapping(
            value = "/reports/me",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> getMyReports(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String traceId = UUID.randomUUID().toString();

        try {
            if (principal == null) {
                ApiError error = ApiError.of(
                        HttpStatus.UNAUTHORIZED,
                        "인증이 필요합니다 (토큰 확인)",
                        null,
                        traceId,
                        null,
                        null
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            // ✅ 본인이 참여한 방들의 report까지 모두 조회 (같은 방의 다른 유저 report도 포함)
            java.util.List<AiTestReport> reports = aiTestReportService.getMyRoomReports(principal.getId());

            java.util.List<TestReportResponse> response = reports.stream()
                    .map(r -> {
                        String tsKst = r.getCreatedAt()
                                .atZone(KST)
                                .toOffsetDateTime()
                                .toString();

                        return new TestReportResponse(
                                r.getReportId(),
                                r.getRoomId(),
                                tsKst,
                                r.getStacktrace(),
                                r.getDisplayName(),
                                r.getError(),
                                r.getResolution()
                        );
                    })
                    .toList();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("[reports/me] 에러(traceId=" + traceId + "): " + e.getMessage());
            e.printStackTrace();

            ApiError error = ApiError.of(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "보고서 조회 중 서버 오류",
                    e,
                    traceId,
                    null,
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ===================== [레거시 API] =====================
    @PostMapping(
            value = "/files/{fileId}/generate-test",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<String> generateTest(
            @PathVariable Long fileId,
            @RequestBody GenerateTestBody body
    ) {
        try {
            String generatedTestCode = testCodeGenerationService.generateTest(
                    fileId,
                    body.code()
            );

            return ResponseEntity.ok(generatedTestCode);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("테스트 코드 생성 실패: " + e.getMessage());
        }
    }

    // ===================== [테스트 실패 판별 로직] =====================
    private boolean isTestFailed(String runOutput) {
        if (runOutput == null) return false;

        String out = runOutput.toLowerCase();

        // 확실한 성공 문구가 있으면 성공으로 처리
        if (out.contains("build successful")) return false;

        // 대표적인 실패 문구들
        if (out.contains("build failed")) return true;
        if (out.contains("failure:")) return true;
        if (out.contains("there were failing tests")) return true;
        if (out.contains("> task :test failed")) return true;
        if (out.contains("tests failed")) return true;
        if (out.contains("compilation failed")) return true;

        // Failures: N 파싱 (N>0이면 실패)
        Matcher m = Pattern.compile("failures\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE)
                .matcher(runOutput);
        if (m.find()) {
            int n = Integer.parseInt(m.group(1));
            if (n > 0) return true;
        }

        // "FAILED"라는 단어가 너무 광범위해서 여기서는 보수적으로 처리
        // (애매한 경우: 저장하지 않음)
        return false;
    }

    // ===================== [DTOs] =====================

    public record GenerateTestCodeRequest(
            Long roomId,
            String fileName,
            String code
    ) {}

    public record GenerateTestCodeResponse(
            Long roomId,
            String fileName,
            String testCode
    ) {}

    public record GenerateTestBody(String code) {}

    public record AnalyzeResultRequest(
            Long roomId,
            String fileName,
            // ✅ 배포 서버/구버전 호환용: "output"으로 와도 runOutput으로 받게 함
            @JsonAlias({"runOutput", "output"})
            String runOutput
    ) {}

    public record TestReportResponse(
            Long id,
            Long roomId,
            String timestamp,     // ✅ KST(+09:00) 포함 ISO 문자열
            String stacktrace,
            String display_name,
            String error,
            String resolution
    ) {}

    // ===================== [에러 응답 DTO] =====================
    public record ApiError(
            String timestamp,
            int status,
            String error,
            String message,
            String exception,
            String traceId,
            String stacktrace,
            Long roomId,
            String fileName
    ) {
        public static ApiError of(
                HttpStatus status,
                String message,
                Throwable e,
                String traceId,
                Long roomId,
                String fileName
        ) {
            return new ApiError(
                    OffsetDateTime.now().toString(),
                    status.value(),
                    status.getReasonPhrase(),
                    message,
                    (e == null ? null : e.getClass().getName()),
                    traceId,
                    (e == null ? null : stackTraceToString(e, 8000)),
                    roomId,
                    fileName
            );
        }

        private static String stackTraceToString(Throwable t, int maxChars) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            String full = sw.toString();
            if (full.length() <= maxChars) return full;
            return full.substring(0, maxChars) + "\n... (truncated)";
        }
    }
}
