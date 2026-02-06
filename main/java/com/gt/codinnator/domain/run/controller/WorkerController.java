package com.gt.codinnator.domain.run.controller;

import com.gt.codinnator.domain.run.dto.CodeRunRequest;
import com.gt.codinnator.domain.run.service.ProjectExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 테스트 실행 컨트롤러
 * - 기존: projectZip + testCode (multipart)
 * - 변경: roomId + testCode + packageName (JSON)
 */

@RestController
@RequestMapping("/api/v1/room")
@RequiredArgsConstructor
public class WorkerController {

    private final ProjectExecutionService executionService;

    @PostMapping(
            value = "/{roomId}/code-run",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<String> executeProject(
            @PathVariable Long roomId,
            @RequestBody CodeRunRequest request
    ) {
        try {
            // roomId 기반으로 서버에 저장된 프로젝트를 가져와 테스트 실행
            String result = executionService.processAndTest(roomId, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("실행 중 오류 발생: " + e.getMessage());
        }
    }
}
