// =========================================
// [파일 목적] Spring Boot CORS(Cross-Origin Resource Sharing) 설정
// - 브라우저에서 다른 출처(포트/도메인)의 API를 호출할 수 있도록 허용
// - 개발 환경에서 프론트엔드(5173)와 백엔드(8080/8081) 간 통신 활성화
// - OAuth2/JWT 인증 지원을 위한 credentials 허용
// =========================================

package com.gt.codinnator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * CORS 정책 설정 클래스
 * 
 * <p>목적:
 * - 웹 브라우저의 보안 정책(Same-Origin Policy)을 제어
 * - 프론트엔드 개발 서버에서 백엔드 API 호출을 허용
 * - OAuth2/JWT 인증을 위한 쿠키/토큰 전송 허용
 * 
 * <p>작동 방식:
 * - Spring Boot가 시작될 때 이 설정을 자동으로 적용
 * - 모든 엔드포인트(/**)에 대해 CORS 허용 정책 적용
 * 
 * <p>주의사항:
 * - 운영 환경 배포 시 allowedOrigins를 실제 프론트 도메인으로 변경 필수
 * - 보안상 와일드카드(*) 사용은 개발 환경에서만 권장
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // =========================================
    // [CORS 설정] 허용할 출처/메서드/헤더 정의
    // - 입력: 없음 (Spring이 자동 호출)
    // - 출력: 없음 (내부적으로 CORS 정책 등록)
    // - 부작용: 글로벌 CORS 설정 변경
    // =========================================
    
    /**
     * CORS 매핑 규칙을 등록하는 메서드
     * 
     * <p>호출 시점:
     * - Spring Boot 애플리케이션 시작 시 자동 호출
     * - WebMvcConfigurer 인터페이스의 콜백 메서드
     * 
     * <p>설정 내용:
     * - 경로: 모든 엔드포인트(/**)에 적용
     * - 허용 출처: 개발 환경 및 배포 환경 주소
     * - 허용 메서드: REST API의 기본 HTTP 메서드 5개
     * - 허용 헤더: 모든 헤더 (*) - 커스텀 헤더 제한 없음
     * - Credentials: OAuth2/JWT 인증을 위해 활성화
     * 
     * @param registry CORS 설정을 등록할 레지스트리 객체 (Spring이 주입)
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // 모든 API 경로에 대해
                
                // ✅ 허용할 프론트엔드 주소 (개발 + 배포 환경)
                .allowedOrigins(
                    "http://localhost:5173",           // Vite 개발 서버 (로컬)
                    "http://127.0.0.1:5173",           // IP 주소 버전
                    "http://i14e205.p.ssafy.io:5173",  // 배포 환경 (필요시 추가)
                    "http://i14e205.p.ssafy.io"        // 배포 환경 (포트 없는 버전)
                )
                
                // ✅ 허용할 HTTP 메서드
                .allowedMethods(
                    "GET",       // 조회
                    "POST",      // 생성
                    "PUT",       // 전체 수정
                    "PATCH",     // 부분 수정
                    "DELETE",    // 삭제
                    "OPTIONS"    // Preflight 요청 (브라우저가 자동으로 보냄)
                )
                
                // ✅ 허용할 요청 헤더 (모든 헤더 허용)
                // Authorization, X-Custom-Header 등 커스텀 헤더도 사용 가능
                .allowedHeaders("*")
                
                // ✅ OAuth2/JWT 인증을 위한 Credentials 허용 (중요!)
                // 이 설정이 없으면 쿠키나 Authorization 헤더가 전송되지 않음
                .allowCredentials(true)
                
                // ✅ Preflight 요청 캐시 시간 (초)
                // OPTIONS 요청을 1시간 동안 캐시하여 성능 향상
                .maxAge(3600);
    }
}


// =========================================
// [변경 내역]
// 
// ✅ 2025-01-31: OAuth2/JWT 인증 지원을 위한 설정 추가
//    - allowCredentials(true) 활성화
//    - maxAge(3600) 추가
//    - 배포 환경 주소 추가
//    - PATCH 메서드 추가
// 
// =========================================


// =========================================
// [추가 개선 제안 TODO]
// 
// 1. 환경별 설정 분리
//    - application-dev.yml: 개발 환경 CORS 설정
//    - application-prod.yml: 운영 환경 CORS 설정
//    - @Value 어노테이션으로 동적 주입
// 
// 예시:
// @Value("${cors.allowed-origins}")
// private String[] allowedOrigins;
// 
// .allowedOrigins(allowedOrigins)
// 
// 2. 보안 강화
//    - allowedOrigins를 환경변수에서 읽어오기
//    - 운영 환경에서는 특정 도메인만 허용
//    - allowCredentials(true) 사용 시 allowedOrigins에 "*" 금지
//      (현재는 구체적인 도메인만 지정하므로 안전함)
// 
// 3. 로깅 추가
//    - CORS 요청 실패 시 디버깅 정보 기록
//    - 허용되지 않은 Origin 접근 시도 모니터링
//    - Spring Security와 함께 사용 시 CORS 필터 순서 확인
// 
// =========================================
