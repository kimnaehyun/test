package com.gt.codinnator.domain.editor.controller;


import com.gt.codinnator.domain.editor.dto.FileResponseDto;
import com.gt.codinnator.domain.editor.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/room")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @PostMapping("/{roomId}/uploads")
    public ResponseEntity<Void> uploadProject(@RequestParam("file") List<MultipartFile> file, @PathVariable Long roomId) throws IOException {
        fileService.uploadProject(file, roomId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/{roomId}/files")
    public ResponseEntity<List<FileResponseDto>> getFileList(@PathVariable Long roomId) {
        return ResponseEntity.ok(fileService.getFileTree(roomId));
    }

    @GetMapping("/{roomId}/{fileId}")
    public ResponseEntity<String> getFileContent(@PathVariable Long roomId, @PathVariable Long fileId) throws IOException {
        return ResponseEntity.ok(fileService.getFileContent(roomId, fileId));
    }
}