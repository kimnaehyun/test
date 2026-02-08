package com.gt.codinnator.domain.editor.service;

import com.gt.codinnator.domain.editor.dto.FileResponseDto;
import com.gt.codinnator.domain.editor.entity.FileNode;
import com.gt.codinnator.domain.editor.repository.FileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;

    @Value("${file.path}")
    private String filePath;

    // 파일 전체 조회
    public List<FileResponseDto> getFileTree(Long roomId) {
        List<FileNode> rootNodes = fileRepository.findAllByRoomIdAndParentIdIsNull(roomId);

        // Entity -> DTO 변환
        return rootNodes.stream()
                .map(FileResponseDto::from)
                .collect(Collectors.toList());
    }

    // 파일 상세 조회
    public String getFileContent(Long roomId, Long fileId) throws IOException {
        FileNode node = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 파일입니다."));

        if(!node.getRoomId().equals(roomId)) {
            throw new IllegalArgumentException("존재하지 않는 방");
        }
        Path path = Paths.get(node.getFilePath());

        try {
            // 1. 국룰 UTF-8 시도
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (MalformedInputException e1) {
            try {
                // 2. 실패시 한국 윈도우 국룰 MS949 시도 (EUC-KR보다 더 많은 한글 지원)
                return Files.readString(path, Charset.forName("MS949"));
            } catch (MalformedInputException e2) {
                // 3. 정 안되면 깨지더라도 읽기는 한다 (ISO-8859-1)
                return Files.readString(path, StandardCharsets.ISO_8859_1);
            }
        }
    }

    @Transactional
    public void uploadProject(List<MultipartFile> files, Long roomId) throws IOException {
        // 기존 폴더 삭제
        fileRepository.deleteByRoomId(roomId);

        Path root = Paths.get(filePath, String.valueOf(roomId));

        if (Files.exists(root)) {
            // root 하위 파일/폴더 전부 삭제
            Files.walk(root)
                    .sorted((a, b) -> b.compareTo(a)) // 하위부터 지워야 함
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        Files.createDirectories(root);

        for (MultipartFile mf : files) {
            if (mf.isEmpty()) continue;

            String original = mf.getOriginalFilename();
            if (original == null || original.isBlank()) continue;

            String rel = original.replace("\\", "/");
            String lower = rel.toLowerCase();

            if (lower.endsWith(".zip")) {
                unzipFile(mf, root.toFile()); // ✅ MultipartFile 그대로 사용
                continue;
            }

            Path target = root.resolve(rel).normalize();
            if (!target.startsWith(root)) {
                throw new IOException("Security Error: Invalid path " + rel);
            }

            String name = target.getFileName().toString();

            if (name.endsWith(".class")) continue;
            if (rel.startsWith(".idea/") || rel.contains("/.idea/")) continue;
            if (rel.startsWith("build/") || rel.contains("/build/")) continue;
            if (rel.startsWith("out/") || rel.contains("/out/")) continue;
            if (rel.startsWith(".gradle/") || rel.contains("/.gradle/")) continue;
            // Mac 사용자가 올릴 경우 생기는 쓰레기 파일 차단
            if (name.equals(".DS_Store") || rel.contains("__MACOSX")) continue;

            // 4. 물리적 저장
            Files.createDirectories(target.getParent());
            mf.transferTo(target.toFile());
        }

        // 5. DB 동기화
        File rootDir = root.toFile();
        File[] top = rootDir.listFiles();
        if (top != null) {
            // 기존 DB 데이터가 꼬이지 않게 해당 방의 파일 정보를 리셋하거나, 중복 체크 로직 필요
            // fileRepository.deleteByRoomId(roomId); // 필요시 초기화
            for (File f : top) {
                saveDirectory(f, null, roomId);
            }
        }
    }


    private void saveDirectory(File currentFile, FileNode parentNode, Long roomId) {
        // 1. 숨김 파일(.git 등) 무시
        if (currentFile.isHidden()) return;

        // 2. 컴파일된 바이너리 파일(.class) 무시 -> 이거 때문에 외계어가 뜬 겁니다!
        if (currentFile.getName().endsWith(".class")) return;

        // 3. 빌드 결과물 폴더(bin, build, out, .gradle) 통째로 무시 (선택사항이지만 강력 추천)
        if (currentFile.isDirectory()) {
            String name = currentFile.getName();
            if (name.equals("bin") || name.equals("build") || name.equals("out") || name.equals(".gradle") || name.equals(".idea")) {
                return;
            }
        }

        FileNode myNode = FileNode.builder()
                .fileName(currentFile.getName())
                .filePath(currentFile.getAbsolutePath())
                .type(currentFile.isDirectory() ? "DIR" : "FILE")
                .roomId(roomId)
                .parentId(parentNode)
                .build();
        fileRepository.save(myNode); //insert


        if (currentFile.isDirectory()) {
            File[] children = currentFile.listFiles();
            if (children != null) {
                for (File child : children) {
                    saveDirectory(child, myNode, roomId);
                }
            }
        }
    }

    // [유틸] 압축 해제 로직
    private void unzipFile(MultipartFile zipFile, File destDir) throws IOException {
        byte[] buffer = new byte[1024];

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream(), Charset.forName("MS949"))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = new File(destDir, zipEntry.getName());

                if (zipEntry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
        }
    }
}