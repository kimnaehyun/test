package com.gt.codinnator.domain.git.controller;

import com.gt.codinnator.domain.git.dto.ChangeFileDto;
import com.gt.codinnator.domain.git.dto.CommitRequestDto;
import com.gt.codinnator.domain.git.service.GitService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/room/git")
public class GitController {
    private final GitService gitService;

    // 저장 버튼 (add 눌렀을 때 바뀐 파일들 하드디스크에 저장하기 & git add 실행하기)
    @PostMapping("{roomId}/add")
    public ResponseEntity<Void> gitAdd(@PathVariable Long roomId, @RequestBody List<ChangeFileDto> changeFiles) throws Exception {
        gitService.saveAndGitAdd(roomId, changeFiles);
        return ResponseEntity.ok().build();
    }
    // 커밋 버튼 (커밋 메시지 받아서 git commit 실행하기)
    @PostMapping("{roomId}/commit")
    public ResponseEntity<Void> gitCommit(@PathVariable Long roomId, @RequestBody CommitRequestDto commitRequestDto) throws Exception {
        gitService.commitMessage(roomId, commitRequestDto.getMessage());
        return ResponseEntity.ok().build();
    }

    // 푸시 버튼 (원격 저장소에 푸시하기)
    @GetMapping("/{roomId}/push")
    public ResponseEntity<Void> gitPush(@PathVariable Long roomId) throws Exception {
        gitService.pushToRemote(roomId);
        return ResponseEntity.ok().build();
    }
}
