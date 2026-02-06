// =========================================
// [파일 목적] AI 서버와 통신하여 테스트 코드 및 보고서 생성
// 
// <핵심 책임>
// 1. AI 서버(FastAPI)에 HTTP 요청을 보내 테스트 코드 생성
// 2. AI 서버에 테스트 실행 결과를 보내 분석 보고서 생성
// 3. RestTemplate 설정 및 HTTP 통신 관리
// 4. 인코딩 문제(한글, 특수문자) 해결
// 
// <아키텍처>
// Spring Boot 백엔드 <-> Python FastAPI AI 서버
// - 요청: text/plain (코드 문자열)
// - 응답: text/plain (테스트 코드) 또는 JSON (보고서)
// 
// <의존성>
// - FileRepository: fileId로 파일 메타데이터 조회
// - FileService: 파일 내용 읽기
// - RestTemplate: HTTP 클라이언트
// =========================================

package com.gt.codinnator.domain.ai.service;

import com.gt.codinnator.domain.editor.entity.FileNode;
import com.gt.codinnator.domain.editor.repository.FileRepository;
import com.gt.codinnator.domain.editor.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;


/**
 * AI 기반 테스트 생성 및 분석 서비스
 * 
 * <p>주요 기능:
 * - JUnit5 테스트 코드 자동 생성
 * - 테스트 실행 결과 분석 (에러 진단 및 해결 방법 제시)
 * 
 * <p>AI 서버 통신:
 * - /generate-testcode: 테스트 생성 (text/plain -> text/plain)
 * - /generate-result: 결과 분석 (text/plain -> JSON)
 * 
 * <p>설정:
 * - AI 서버 주소는 application.yml의 ai.service.base-url에서 관리
 * - 기본값: http://127.0.0.1:8000 (로컬 개발 환경)
 */
@Service
@RequiredArgsConstructor
public class TestCodeGenerationService {

    // =========================================
    // [의존성 주입] 생성자 기반 불변 주입
    // =========================================
    
    private final FileRepository fileRepository;
    private final FileService fileService;

    /**
     * AI 서버 기본 URL (외부 설정 주입)
     * 
     * <p>설정 예시:
     * <pre>
     * # application.yml
     * ai:
     *   service:
     *     base-url: http://127.0.0.1:8000
     * </pre>
     * 
     * <p>환경별 설정:
     * - 로컬 개발: http://127.0.0.1:8000
     * - 도커 환경: http://ai-service:8000
     * - 운영 환경: https://ai.yourdomain.com
     */
    @Value("${ai.service.base-url:http://127.0.0.1:8000}")
    private String aiBaseUrl;


    // =========================================
    // [RestTemplate 설정] UTF-8 인코딩 보장
    // 
    // <필요성>
    // - 한글, 이모지 등 멀티바이트 문자 처리
    // - AI 서버와의 text/plain 통신에서 깨짐 방지
    // 
    // <설계 선택>
    // - 매 호출마다 새 RestTemplate 생성 (상태 없음)
    // - 향후 @Bean으로 싱글톤 등록 고려 가능
    // =========================================
    
    /**
     * UTF-8 인코딩을 지원하는 RestTemplate 생성
     * 
     * <p>설정 내용:
     * - StringHttpMessageConverter를 UTF-8로 명시
     * - text/plain, application/json 요청/응답 처리
     * 
     * <p>대안:
     * - WebClient(Spring 5+)로 교체 시 reactive 지원 가능
     * - 비동기 처리가 필요하면 WebClient 권장
     * 
     * @return UTF-8 인코딩이 설정된 RestTemplate 인스턴스
     */
    private RestTemplate createRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // UTF-8 StringHttpMessageConverter를 최우선 순위로 추가
        // 이렇게 하면 text/plain 응답을 UTF-8로 디코딩함
        restTemplate.getMessageConverters().add(
                0,  // 첫 번째 위치에 삽입 (우선순위 최상)
                new StringHttpMessageConverter(StandardCharsets.UTF_8)
        );
        
        return restTemplate;
    }


    // =========================================
    // [레거시 API] fileId 기반 테스트 생성
    // 
    // <흐름>
    // 1. DB에서 fileId로 파일 조회
    // 2. .java 파일인지 검증
    // 3. codeOverride가 있으면 사용, 없으면 파일 읽기
    // 4. AI 호출하여 테스트 생성
    // 
    // - 입력: fileId, codeOverride (선택적)
    // - 출력: String (JUnit5 테스트 코드)
    // - 예외: IllegalArgumentException, IOException
    // =========================================
    
    /**
     * DB에 저장된 파일 기준으로 테스트 코드 생성
     * 
     * <p>사용 시나리오:
     * - 파일 트리에서 파일을 선택하여 테스트 생성
     * - DB에 저장된 원본 코드 사용 (에디터 수정 전)
     * 
     * <p>동작:
     * 1. fileId로 FileNode 엔티티 조회
     * 2. 파일명 검증 (.java만 허용)
     * 3. codeOverride가 있으면 우선 사용 (에디터의 임시 코드)
     * 4. 없으면 파일 시스템에서 원본 코드 읽기
     * 5. AI 서버 호출하여 테스트 생성
     * 
     * @param fileId 테스트를 생성할 파일의 DB ID
     * @param codeOverride 에디터에서 수정한 코드 (없으면 null)
     * @return 생성된 JUnit5 테스트 코드 (전체 소스)
     * @throws IllegalArgumentException 파일이 존재하지 않거나 .java가 아닐 때
     * @throws IOException 파일 읽기 실패 시
     */
    public String generateTest(Long fileId, String codeOverride) throws IOException {
        
        // 1. DB에서 파일 메타데이터 조회
        FileNode fileNode = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 파일입니다. fileId=" + fileId
                ));

        // 2. ✅ FileNode에서 roomId 추출
        Long roomId = fileNode.getRoomId();

        // 3. ✅ roomId와 fileId를 모두 전달
        String sourceCode = determineSourceCode(roomId, fileId, codeOverride);

        // 4. 파일명 검증 (.java만 지원)
        validateJavaFile(fileNode.getFileName());

        // 5. AI 서버 호출
        return callAiGenerateTestCode(fileNode.getFileName(), sourceCode);
    }
    
    /**
     * 사용할 소스 코드를 결정 (오버라이드 우선)
     * 
     * <p>우선순위:
     * 1. codeOverride가 비어있지 않으면 사용
     * 2. 없으면 fileId로 파일 시스템에서 읽기
     * 
     * @param roomId 방 ID (권한 검증용)
     * @param fileId 파일 ID
     * @param codeOverride 오버라이드 코드 (선택적)
     * @return 사용할 소스 코드
     * @throws IOException 파일 읽기 실패 시
     */
    private String determineSourceCode(Long roomId, Long fileId, String codeOverride) throws IOException {
        if (codeOverride != null && !codeOverride.isBlank()) {
            // 에디터의 현재 코드 사용
            return codeOverride;
        } else {
            // ✅ roomId와 fileId를 모두 전달
            return fileService.getFileContent(roomId, fileId);
        }
    }
    // =========================================
    // [신규 API] fileName + code 직접 전달 방식
    // 
    // <장점>
    // - DB 조회 불필요 (빠른 응답)
    // - 에디터의 임시 수정 내용도 즉시 반영
    // - 파일 저장 전에도 테스트 생성 가능
    // 
    // - 입력: fileName, code
    // - 출력: String (JUnit5 테스트 코드)
    // - 예외: IllegalArgumentException
    // =========================================
    
    /**
     * 파일명과 코드를 직접 받아 테스트 생성
     * 
     * <p>사용 시나리오:
     * - 프론트엔드 에디터에서 현재 코드를 바로 전달
     * - DB 저장 없이 즉시 테스트 생성
     * 
     * <p>검증:
     * - fileName이 .java로 끝나는지 확인
     * - code가 비어있지 않은지 확인 (AI 호출 시 검증)
     * 
     * @param fileName 소스 파일명 (예: Calculator.java)
     * @param code 테스트할 Java 소스 코드
     * @return 생성된 JUnit5 테스트 코드
     * @throws IllegalArgumentException fileName이 .java가 아닐 때
     */
    public String generateTestCode(String fileName, String code) {
        
        // 파일명 검증
        validateJavaFile(fileName);
        
        // AI 서버 호출
        return callAiGenerateTestCode(fileName, code);
    }
    
    /**
     * 파일명이 .java 확장자인지 검증
     * 
     * <p>검증 규칙:
     * - null 체크
     * - 대소문자 무관하게 .java로 끝나는지 확인
     * 
     * @param fileName 검증할 파일명
     * @throws IllegalArgumentException .java가 아닐 때
     */
    private void validateJavaFile(String fileName) {
        if (fileName == null || !fileName.toLowerCase().endsWith(".java")) {
            throw new IllegalArgumentException(
                    "현재는 .java 파일만 지원합니다. 선택된 파일: " + fileName
            );
        }
    }


    // =========================================
    // [AI 통신 핵심 로직] 테스트 코드 생성 HTTP 호출
    // 
    // <프로토콜>
    // - 메서드: POST
    // - URL: {aiBaseUrl}/generate-testcode?fileName={fileName}
    // - 요청 헤더: Content-Type: text/plain; charset=UTF-8
    // - 요청 본문: Java 소스 코드 (문자열)
    // - 응답 헤더: Content-Type: text/plain; charset=UTF-8
    // - 응답 본문: JUnit5 테스트 코드 (문자열)
    // 
    // <에러 처리>
    // - RestClientException: 네트워크 오류, 타임아웃 등
    // - HttpStatusCodeException: AI 서버의 4xx/5xx 응답
    // =========================================
    
    /**
     * AI 서버의 /generate-testcode 엔드포인트 호출
     * 
     * <p>요청 구조:
     * <pre>
     * POST http://127.0.0.1:8000/generate-testcode?fileName=Calculator.java
     * Content-Type: text/plain; charset=UTF-8
     * 
     * public class Calculator {
     *     public int add(int a, int b) { return a + b; }
     * }
     * </pre>
     * 
     * <p>응답 구조:
     * <pre>
     * import org.junit.jupiter.api.Test;
     * import static org.junit.jupiter.api.Assertions.*;
     * 
     * class CalculatorTest {
     *     @Test
     *     void testAdd() { ... }
     * }
     * </pre>
     * 
     * <p>디버깅:
     * - 요청 전: fileName, code 길이, code 미리보기 출력
     * - 응답 후: HTTP 상태 코드 출력
     * - 에러 시: 스택 트레이스 출력
     * 
     * @param fileName 소스 파일명 (AI가 클래스명 추론에 사용)
     * @param code 테스트할 Java 소스 코드
     * @return AI가 생성한 JUnit5 테스트 코드
     * @throws RuntimeException AI 호출 실패 시 (네트워크, AI 에러 등)
     */
    private String callAiGenerateTestCode(String fileName, String code) {
        
        // 디버깅 정보 출력 (개발 환경)
        // TODO: 운영 환경에서는 로그 레벨을 INFO로 변경
        logAiRequestDebugInfo(fileName, code);

        // 코드가 비어있으면 조기 종료
        validateCodeNotEmpty(code);

        try {
            // 1. RestTemplate 생성 (UTF-8 인코딩 설정됨)
            RestTemplate restTemplate = createRestTemplate();
            
            // 2. HTTP 헤더 구성
            HttpHeaders headers = buildTextPlainHeaders();
            
            // 3. 요청 본문 생성 (HttpEntity로 감싸기)
            HttpEntity<String> requestEntity = new HttpEntity<>(code, headers);
            
            // 4. URL 생성 (쿼리 파라미터로 fileName 전달)
            String url = buildGenerateTestCodeUrl(fileName);
            
            // 디버깅: 최종 요청 URL 및 헤더 출력
            logAiRequestDetails(url, headers);
            
            // 5. HTTP POST 요청 실행
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    String.class
            );
            
            // 디버깅: 응답 상태 코드 출력
            logAiResponseStatus(response);
            
            // 6. 응답 본문 반환 (테스트 코드)
            return response.getBody();

        } catch (Exception e) {
            // 에러 로깅 및 사용자 친화적 예외 변환
            logAiError(e);
            throw new RuntimeException(
                    "AI 테스트 코드 생성 실패: " + e.getMessage(),
                    e
            );
        }
    }
    
    /**
     * AI 요청 디버깅 정보 출력 터미널핑 
     * 
     * @param fileName 파일명
     * @param code 소스 코드
     */
    private void logAiRequestDebugInfo(String fileName, String code) {
        // TODO: SLF4J Logger로 교체
        System.out.println("[AI 요청 준비]");
        System.out.println("  - fileName: " + fileName);
        System.out.println("  - code 길이: " + 
                (code == null ? "null" : code.length() + " chars"));
        
        // 코드 미리보기 (첫 50자만)
        if (code != null && code.length() > 50) {
            System.out.println("  - code 미리보기: " + code.substring(0, 50) + "...");
        } else {
            System.out.println("  - code 미리보기: " + code);
        }
    }
    
    /**
     * 코드가 비어있는지 검증
     * 
     * @param code 검증할 코드
     * @throws IllegalArgumentException 코드가 null이거나 빈 문자열일 때
     */
    private void validateCodeNotEmpty(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "code가 비어있습니다. 백엔드에 전달된 code가 null 또는 빈 문자열입니다."
            );
        }
    }
    
    /**
     * text/plain 요청/응답을 위한 HTTP 헤더 생성
     * 
     * @return UTF-8 인코딩이 명시된 헤더
     */
    private HttpHeaders buildTextPlainHeaders() {
        HttpHeaders headers = new HttpHeaders();
        
        // Content-Type: text/plain; charset=UTF-8
        headers.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));
        
        // Accept: text/plain (응답도 text/plain 예상)
        headers.setAccept(Collections.singletonList(MediaType.TEXT_PLAIN));
        
        return headers;
    }
    
    /**
     * 테스트 생성 API URL 구성
     * 
     * @param fileName 쿼리 파라미터로 전달할 파일명
     * @return 완성된 URL
     */
    private String buildGenerateTestCodeUrl(String fileName) {
        return aiBaseUrl + "/generate-testcode?fileName=" + fileName;
        
        // TODO: URL 인코딩 추가 (파일명에 특수문자가 있을 경우)
        // UriComponentsBuilder 사용 권장
    }
    
    /**
     * 최종 요청 정보 로깅
     * 
     * @param url 요청 URL
     * @param headers 요청 헤더
     */
    private void logAiRequestDetails(String url, HttpHeaders headers) {
        System.out.println("[AI 요청 전송]");
        System.out.println("  - URL: " + url);
        System.out.println("  - Headers: " + headers);
    }
    
    /**
     * AI 응답 상태 로깅
     * 
     * @param response HTTP 응답 객체
     */
    private void logAiResponseStatus(ResponseEntity<String> response) {
        System.out.println("[AI 응답 수신]");
        System.out.println("  - 상태 코드: " + response.getStatusCode());
    }
    
    /**
     * AI 호출 에러 로깅
     * 
     * @param e 발생한 예외
     */
    private void logAiError(Exception e) {
        // 표준 에러 스트림에 출력 (운영 환경에서는 로그 파일로 수집됨)
        System.err.println("[AI 호출 에러] " + e.getMessage());
        e.printStackTrace();
    }


    // =========================================
    // [보고서 생성 API] 테스트 실행 결과 분석
    // 
    // <흐름>
    // 1. 프론트에서 테스트 실행 로그를 전송
    // 2. AI 서버에 text/plain으로 전달
    // 3. AI가 에러 분석 후 JSON 반환
    // 4. JSON을 AiReport DTO로 파싱하여 반환
    // 
    // <프로토콜>
    // - URL: {aiBaseUrl}/generate-result
    // - 요청: text/plain (테스트 실행 로그)
    // - 응답: application/json (에러 진단 및 해결 방법)
    // 
    // - 입력: runOutput (테스트 실행 로그 문자열)
    // - 출력: AiReport (display_name, error, resolution)
    // - 예외: RuntimeException
    // =========================================
    
    /**
     * 테스트 실행 결과를 AI로 분석하여 보고서 생성
     * 
     * <p>사용 시나리오:
     * - 테스트 실패 시 에러 원인 진단
     * - 해결 방법 자동 제시
     * - 초보 개발자를 위한 친절한 가이드
     * 
     * <p>요청 예시:
     * <pre>
     * POST http://127.0.0.1:8000/generate-result
     * Content-Type: text/plain; charset=UTF-8
     * 
     * [ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
     * java.lang.NullPointerException: Cannot invoke "String.length()"
     * at Calculator.divide(Calculator.java:10)
     * </pre>
     * 
     * <p>응답 예시:
     * <pre>
     * {
     *   "display_name": "NullPointerException",
     *   "error": "divide 메서드에서 null 값을 처리하지 않음",
     *   "resolution": "입력 값 null 체크를 추가하세요. if (input == null) throw new IllegalArgumentException();"
     * }
     * </pre>
     * 
     * @param runOutput 테스트 실행 로그 (표준 출력 + 에러 출력)
     * @return AI가 분석한 보고서 (에러명, 원인, 해결책)
     * @throws RuntimeException AI 호출 실패 시
     */
    public AiReport generateReport(String runOutput) {
        try {
            // 1. RestTemplate 생성
            RestTemplate restTemplate = createRestTemplate();
            
            // 2. HTTP 헤더 구성 (요청: text/plain, 응답: JSON)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            // 3. 요청 본문 생성
            HttpEntity<String> requestEntity = new HttpEntity<>(runOutput, headers);
            
            // 4. URL 생성
            String url = aiBaseUrl + "/generate-result";
            
            // 5. HTTP POST 요청 실행 (응답을 AiReport 타입으로 자동 파싱)
            ResponseEntity<AiReport> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    AiReport.class  // Jackson이 JSON -> AiReport 변환
            );
            
            // 6. 파싱된 객체 반환
            return response.getBody();
            
        } catch (Exception e) {
            // 에러 로깅 및 예외 변환
            System.err.println("[보고서 생성 에러] " + e.getMessage());
            e.printStackTrace();
            
            throw new RuntimeException(
                    "AI 보고서 생성 실패: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * AI 분석 보고서 DTO
     * 
     * <p>필드 설명:
     * - display_name: 에러 제목 (간단명료, 예: "NullPointerException")
     * - error: 에러 원인 (한 문장, 예: "null 값 처리 누락")
     * - resolution: 해결 방법 (2-5문장, 구체적 코드 예시 포함)
     * 
     * <p>사용 예시:
     * <pre>
     * AiReport report = service.generateReport(testOutput);
     * System.out.println("에러: " + report.display_name());
     * System.out.println("원인: " + report.error());
     * System.out.println("해결: " + report.resolution());
     * </pre>
     * 
     * <p>참고:
     * - record 타입으로 불변성 보장
     * - Jackson이 JSON 필드명과 자동 매칭
     * - snake_case(display_name) 필드도 Java에서 그대로 사용
     */
    public record AiReport(
            String display_name,  // 에러 제목
            String error,         // 에러 원인
            String resolution     // 해결 방법
    ) {}
}


// =========================================
// [변경 내역 요약]
// 
// 1. 가독성 개선
//    - 파일 최상단에 목적 및 아키텍처 설명 추가
//    - 각 섹션마다 목적/입출력/예외 명시
//    - 함수마다 JavaDoc 주석 추가 (용도, 흐름, 예시)
//    - 복잡한 로직을 여러 private 메서드로 분리
// 
// 2. 재사용성 향상
//    - 중복 로직을 메서드로 추출
//      * validateJavaFile: 파일명 검증 공통화
//      * determineSourceCode: 코드 선택 로직 분리
//      * buildTextPlainHeaders: 헤더 생성 재사용
//      * logXXX 메서드: 로깅 로직 분리
// 
// 3. 책임 분리 (SRP)
//    - 검증 로직: validateXXX 메서드
//    - HTTP 통신: callAiXXX 메서드
//    - 로깅: logXXX 메서드
//    - URL/헤더 구성: buildXXX 메서드
// 
// 4. 에러 처리 개선
//    - 명확한 에러 메시지 (한국어)
//    - 원인 예외 체이닝 (throw new RuntimeException(..., e))
//    - 디버깅 정보 충실하게 출력
// 
// 5. 문서화 강화
//    - JavaDoc으로 API 사용 예시 제공
//    - 프로토콜 상세 설명 (요청/응답 구조)
//    - 설정 방법 및 환경별 주소 예시
// 
// 6. 네이밍 개선
//    - callAiGenerateTestCode: AI 호출임을 명확히
//    - validateCodeNotEmpty: 검증 의도 명확
//    - buildTextPlainHeaders: 생성 역할 명확
// =========================================


// =========================================
// [추가 개선 제안 TODO]
// 
// 1. RestTemplate을 Bean으로 등록
//    - @Configuration 클래스에서 @Bean으로 등록
//    - 싱글톤으로 재사용하여 메모리 절약
//    - ConnectionPooling 설정 추가
// 
// 2. WebClient로 교체
//    - Spring 5+ 권장 방식
//    - Reactive 스타일 (비동기/논블로킹)
//    - Mono<String> 반환으로 비동기 체인 구성
// 
// 3. 타임아웃 및 재시도 로직
//    - AI 서버 응답이 느릴 수 있음 (5-30초)
//    - RestTemplate에 read/connect timeout 설정
//    - Spring Retry로 자동 재시도 구현
// 
// 4. 캐싱 도입
//    - 동일한 코드 재요청 시 AI 호출 생략
//    - @Cacheable 어노테이션 활용
//    - Redis 캐시 서버 연동
// 
// 5. 로깅 체계 개선
//    - System.out -> SLF4J Logger로 교체
//    - @Slf4j 어노테이션 활용
//    - 로그 레벨 구분 (DEBUG, INFO, WARN, ERROR)
// 
// 6. URL 안전성 강화
//    - UriComponentsBuilder로 URL 생성
//    - 파일명의 특수문자 자동 인코딩
//    - Query String Injection 방지
// 
// 7. 테스트 코드 작성
//    - MockRestTemplate으로 AI 서버 모킹
//    - 다양한 에러 시나리오 테스트
//    - @SpringBootTest로 통합 테스트
// 
// 8. 메트릭 및 모니터링
//    - Micrometer로 AI 호출 시간 측정
//    - 성공률, 평균 응답 시간 추적
//    - 실패 시 알림 발송 (Slack, Email)
// =========================================

