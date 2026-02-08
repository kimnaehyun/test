package com.gt.codinnator.domain.run.service;

import com.gt.codinnator.domain.run.dto.TestResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class GradleTestRunner {

    /**
     * ✅ worker 컨테이너(리눅스)에서 실행되는 것을 전제로:
     * - docker run 제거
     * - ./gradlew clean test --tests <클래스명> 로 직접 실행
     * - gradle stdout/stderr를 String으로 모아서 반환(프론트 터미널 출력용)
     */
    public TestResultDto runTest(Path projectPath, String testClassName) {
        StringBuilder gradleLog = new StringBuilder();

        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

            ProcessBuilder pb;

            if (isWindows) {
                // (로컬 윈도우 테스트용)
                pb = new ProcessBuilder(
                        "cmd.exe", "/c",
                        "gradlew.bat", "clean", "test",
                        "--tests", testClassName,
                        "--rerun-tasks"
                );
            } else {
                // ✅ worker 컨테이너(리눅스)에서는 gradlew 직접 실행
                File gradlew = projectPath.resolve("gradlew").toFile();
                if (!gradlew.exists()) {
                    return new TestResultDto(false,
                            "gradlew 파일을 찾을 수 없습니다. projectPath=" + projectPath);
                }
                gradlew.setExecutable(true);

                pb = new ProcessBuilder(
                        "./gradlew", "clean", "test",
                        "--tests", testClassName,
                        "--rerun-tasks",
                        "--no-daemon"
                );
            }

            pb.directory(projectPath.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // ✅ 실행 로그를 "모아서" 반환 + 서버 로그에도 찍기
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    gradleLog.append(line).append("\n");
                    log.info("[Gradle] {}", line);
                }
            }

            // 타임아웃: 프로젝트 크면 1분은 짧을 수 있어서 3분 권장
            boolean finished = process.waitFor(3, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                return new TestResultDto(false,
                        "테스트 실행 시간 초과(Timeout)\n\n===== Gradle Output =====\n" + gradleLog);
            }

            // JUnit 리포트 파싱
            Path reportDir = projectPath.resolve("build/test-results/test");
            TestResultDto junit = parseJUnitXml(reportDir);

            // ✅ 최종 메시지: gradle 로그 + junit 결과 합쳐서 프론트 터미널로 보냄
            String finalMessage =
                    "===== Gradle Output =====\n" + gradleLog +
                    "\n===== JUnit Report =====\n" + junit.getMessage();

            return new TestResultDto(junit.isSuccess(), finalMessage);

        } catch (Exception e) {
            log.error("테스트 실행 실패", e);
            return new TestResultDto(false,
                    "시스템 오류: " + e.getMessage() +
                    "\n\n===== Gradle Output =====\n" + gradleLog);
        }
    }

    private TestResultDto parseJUnitXml(Path reportDirPath) {
        try {
            if (!Files.exists(reportDirPath) || !Files.isDirectory(reportDirPath)) {
                return new TestResultDto(false,
                        "JUnit 리포트 폴더가 없습니다: " + reportDirPath);
            }

            File reportFile = Files.list(reportDirPath)
                    .filter(p -> p.toString().endsWith(".xml"))
                    .map(Path::toFile)
                    .findFirst()
                    .orElseThrow(() -> new IOException("리포트 XML 파일을 찾을 수 없습니다."));

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(reportFile);

            Node testSuite = doc.getElementsByTagName("testsuite").item(0);
            int failures = Integer.parseInt(
                    testSuite.getAttributes().getNamedItem("failures").getNodeValue()
            );

            if (failures == 0) {
                return new TestResultDto(true, "모든 테스트 통과!");
            }

            StringBuilder errorDetail = new StringBuilder("테스트 실패:\n");
            var testCases = doc.getElementsByTagName("testcase");

            for (int i = 0; i < testCases.getLength(); i++) {
                Node testCase = testCases.item(i);
                var failureNodes = testCase.getChildNodes();

                for (int j = 0; j < failureNodes.getLength(); j++) {
                    Node child = failureNodes.item(j);
                    if ("failure".equals(child.getNodeName())) {
                        String fullStackTrace = child.getTextContent();
                        String testName = testCase.getAttributes().getNamedItem("name").getNodeValue();

                        errorDetail.append("[").append(testName).append("] 실패 상세:\n")
                                .append(fullStackTrace).append("\n\n");
                    }
                }
            }

            return new TestResultDto(false, errorDetail.toString());
        } catch (Exception e) {
            return new TestResultDto(false, "리포트 분석 중 오류: " + e.getMessage());
        }
    }
}
