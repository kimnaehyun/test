package com.gt.codinnator.domain.run.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 테스트 실행 요청(JSON)
 * - testCode: AI가 생성한 테스트 코드(문자열)
 * - packageName: 테스트 코드 패키지명 (없으면 기본 패키지로 처리) .
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CodeRunRequest {
    private String testCode;
    private String packageName;

}
