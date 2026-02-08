package com.gt.codinnator.domain.editor.controller;

import com.gt.codinnator.domain.editor.dto.FileRequestDto;
import com.gt.codinnator.domain.editor.service.CodeService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/room/editor")
@AllArgsConstructor
public class CodeController {
    private final CodeService codeService;

    // 코드 편집하다가 새롭게 파일 추가
    @PostMapping("/{roomId}/new-file")
    public ResponseEntity<Void> createFile(@PathVariable Long roomId, @RequestBody FileRequestDto fileRequestDto) throws IOException {
        codeService.newFile(roomId, fileRequestDto);
        return ResponseEntity.ok().build();
    }
    // 코드 편집 중 파일 삭제
    @DeleteMapping("/{roomId}/delete-file/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long roomId, @PathVariable Long fileId) throws IOException {
        codeService.deleteFile(roomId, fileId);
        return ResponseEntity.ok().build();
    }
}
