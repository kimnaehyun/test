package com.gt.codinnator.domain.git.service;

import com.gt.codinnator.domain.editor.service.CodeService;
import com.gt.codinnator.domain.git.dto.ChangeFileDto;
import com.gt.codinnator.domain.room.entity.Room;
import com.gt.codinnator.domain.room.repository.RoomRepository;
import com.gt.codinnator.domain.user.entity.User;
import com.gt.codinnator.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GitService {
    private final CodeService codeService;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    @Value("${file.path}")
    private String filePath;

    @Transactional
    public void saveAndGitAdd(Long roomId, List<ChangeFileDto> changeFiles) throws IOException, GitAPIException {
      Room room = roomRepository.findById(roomId)
              .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        codeService.saveChangeFiles(roomId, changeFiles); // 하드디스크 파일 저장
        add(roomId); // git add
    }

    /**
     * git add 실행
     * @param roomId
     * @throws IOException
     * @throws GitAPIException
     */
    public void add(Long roomId) throws IOException, GitAPIException {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        String fullPath = String.format("%s/%d", filePath, roomId);
        Git git = Git.open(new File(fullPath));
        git.add().addFilepattern(".").call();
    }

    /**
     * git commit 실행
     * @param roomId
     * @param message
     * @throws Exception
     */
    public void commitMessage(Long roomId, String message) throws Exception {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));
        User user = userRepository.findByGitId(String.valueOf(room.getUserId()))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        String fullPath = String.format("%s/%d", filePath, roomId);
        Git git = Git.open(new File(fullPath));// 위에 로직으로 바꿀 예정
        git.commit()
                .setMessage(message)
                .setAuthor(user.getGitId(), user.getEmail() == null ? "user@codinnator.com" : user.getEmail())
                .call();
    }

    /**
     * git push 실행
     * @param roomId
     * @throws Exception
     */
    public void pushToRemote(Long roomId) throws Exception {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        String fullPath = String.format("%s/%d", filePath, roomId);
        Git git = Git.open(new File(fullPath));

        User user = userRepository.findByGitId(String.valueOf(room.getUserId()))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        // 토큰이 Oauth 토큰인지 확인 필요 or PAT 토큰인지
        git.push()
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(user.getGitId(), user.getGitToken()))
                .setRemote("origin")
                .setRefSpecs(new RefSpec("HEAD:refs/heads/"+room.getBranch()))
                .call();
    }
}