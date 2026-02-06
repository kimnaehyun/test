package com.gt.codinnator.domain.run.service;

import com.gt.codinnator.domain.run.dto.CodeRunRequest;
import com.gt.codinnator.domain.run.dto.TestResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * roomId 기반 실행 서비스
 *
 * 동작:
 * 1) 서버 저장소(file.path/{roomId})에서 프로젝트를 찾는다
 * 2) 임시 workDir(/tmp/codinnator/<uuid>)을 만들고 프로젝트를 복사한다
 * 3) workDir 내부의 "실제 gradle 루트"를 찾아낸다 (build.gradle/gradlew 존재 폴더)
 * 4) AI 테스트코드를 workDir/src/test/java/.../GeneratedTest.java로 저장한다(saveTestCode)
 * 5) gradle test 실행 후 결과를 문자열로 반환한다
 */
@Service
@RequiredArgsConstructor
public class ProjectExecutionService {

    private final GradleTestRunner gradleTestRunner;

    // ✅ application.properties의 file.path 사용 (worker 컨테이너에도 FILE_PATH로 주입해야 함)
    @Value("${file.path}")
    private String filePath;

    // class 이름 추출: public class X / class X
    private static final Pattern CLASS_NAME_PATTERN =
            Pattern.compile("\\bclass\\s+([A-Za-z_][A-Za-z0-9_]*)\\b");

    // package 선언 추출: package com.foo;
    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile("\\bpackage\\s+([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*)\\s*;");

    public String processAndTest(Long roomId, CodeRunRequest request) throws Exception {
        Path workDir = null;

        // 0) 입력 검증
        if (request == null || request.getTestCode() == null || request.getTestCode().trim().isEmpty()) {
            throw new IllegalArgumentException("testCode가 비어있습니다.");
        }

        // 1) 서버에 저장된 프로젝트 루트 찾기: {file.path}/{roomId}
        Path savedProjectRoot = Paths.get(filePath, String.valueOf(roomId));
        if (!Files.exists(savedProjectRoot) || !Files.isDirectory(savedProjectRoot)) {
            throw new IllegalStateException("저장된 프로젝트를 찾을 수 없습니다: " + savedProjectRoot);
        }

        try {
            // 2) 임시 workDir 생성: /tmp/codinnator/<uuid>
            Path rootBase = Paths.get(System.getProperty("java.io.tmpdir"), "codinnator");
            workDir = rootBase.resolve(UUID.randomUUID().toString());
            Files.createDirectories(workDir);

            // 3) savedProjectRoot → workDir 로 전체 복사
            copyDirectory(savedProjectRoot, workDir);

            // 4) gradle 루트 탐색(프로젝트 폴더가 한 단계 더 들어가 있는 경우 대비)
            Path gradleRoot = findGradleRoot(workDir);

            // 5) 테스트 코드에서 package/class 이름 결정
            String finalPackage = pickPackageName(request.getTestCode(), request.getPackageName());
            String className = pickClassName(request.getTestCode());
            String fullTestClassName = finalPackage.isBlank()
                    ? className
                    : finalPackage + "." + className;

            // 6) workDir에 테스트 파일 생성 (임시 저장)
            //    => gradleRoot/src/test/java/.../ClassName.java
            saveTestCode(gradleRoot, finalPackage, className, request.getTestCode());

            // 7) Gradle 실행 (테스트 클래스만 지정 실행)
            TestResultDto result = gradleTestRunner.runTest(gradleRoot, fullTestClassName);

            // 8) 프론트 터미널에 바로 출력할 문자열 형태로 반환
            //    (runner 쪽에서 로그까지 문자열에 포함시키면 터미널 출력이 더 풍부해짐)
            return String.format("테스트 결과: %s\n\n%s",
                    result.isSuccess() ? "성공" : "실패",
                    result.getMessage()
            );
        } finally {
            // 9) 임시 폴더 삭제(서버 용량 관리)
            //    디버깅 중엔 주석 처리해서 workDir 남겨도 됨
            if (workDir != null && Files.exists(workDir)) {
                FileSystemUtils.deleteRecursively(workDir);
            }
        }
    }

    /**
     * 프로젝트 디렉토리 전체 복사 (savedProjectRoot -> workDir)
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(dir);
                Path destDir = target.resolve(relative);
                Files.createDirectories(destDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(file);
                Path destFile = target.resolve(relative);
                Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * gradle 루트 탐색:
     * - build.gradle / settings.gradle / gradlew 중 하나가 존재하는 디렉토리를 찾음
     * - 못 찾으면 workDir 자체를 반환(이후 실행에서 실패할 수 있으니 예외를 던져도 됨)
     */
    private Path findGradleRoot(Path workDir) throws IOException {
        // 1) workDir 자체가 루트인지 먼저 확인
        if (isGradleRoot(workDir)) return workDir;

        // 2) 하위 디렉토리 탐색(깊이 제한 가능)
        final int maxDepth = 6;
        try (var stream = Files.find(workDir, maxDepth,
                (path, attr) -> attr.isDirectory() && isGradleRoot(path))) {
            return stream.findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Gradle 루트를 찾지 못했습니다. (build.gradle/gradlew 없음) workDir=" + workDir));
        }
    }

    private boolean isGradleRoot(Path dir) {
        return Files.exists(dir.resolve("build.gradle"))
                || Files.exists(dir.resolve("settings.gradle"))
                || Files.exists(dir.resolve("gradlew"))
                || Files.exists(dir.resolve("gradlew.bat"));
    }

    /**
     * testCode 안에 package 선언이 있으면 그걸 우선 사용
     * 없으면 request.packageName 사용
     */
    private String pickPackageName(String testCode, String requestPackage) {
        Matcher m = PACKAGE_PATTERN.matcher(testCode);
        if (m.find()) return m.group(1);

        return requestPackage == null ? "" : requestPackage.trim();
    }

    /**
     * testCode 안에서 class 이름을 추출
     * 못 찾으면 GeneratedTest로 fallback
     */
    private String pickClassName(String testCode) {
        Matcher m = CLASS_NAME_PATTERN.matcher(testCode);
        if (m.find()) return m.group(1);
        return "GeneratedTest";
    }

    /**
     * workDir(gradleRoot) 안에 테스트 코드 파일 생성
     * - src/test/java/{packagePath}/{className}.java
     * - package 선언이 없고 finalPackage가 있으면 package line을 prepend
     */
    private void saveTestCode(Path gradleRoot, String finalPackage, String className, String code) throws IOException {
        // 경로 계산
        String packagePath = finalPackage.isBlank() ? "" : finalPackage.replace(".", "/");
        Path testDir = finalPackage.isBlank()
                ? gradleRoot.resolve("src/test/java")
                : gradleRoot.resolve("src/test/java").resolve(packagePath);

        Files.createDirectories(testDir);

        // code에 package 선언이 없는데 finalPackage가 있으면 앞에 붙여줌
        if (!finalPackage.isBlank() && !PACKAGE_PATTERN.matcher(code).find()) {
            code = "package " + finalPackage + ";\n\n" + code;
        }

        Path testFile = testDir.resolve(className + ".java");
        Files.writeString(testFile, code, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
