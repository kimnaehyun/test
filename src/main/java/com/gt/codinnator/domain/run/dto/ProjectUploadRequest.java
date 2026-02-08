package com.gt.codinnator.domain.run.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor  // 기본 생성자 (Jackson 등에서 필요할 수 있음)
@AllArgsConstructor // 모든 필드를 포함한 생성자
public class ProjectUploadRequest {
    private MultipartFile projectZip;
    private String testCode;
    private String packageName;
}