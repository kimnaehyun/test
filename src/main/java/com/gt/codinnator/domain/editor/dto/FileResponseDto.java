package com.gt.codinnator.domain.editor.dto;

import com.gt.codinnator.domain.editor.entity.FileNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class FileResponseDto {
    private Long fileId;
    private String name;
    private String type;
    private List<FileResponseDto> children;

    public static FileResponseDto from(FileNode entity) {
        FileResponseDto dto = new FileResponseDto();
        dto.fileId = entity.getFileId();
        dto.name = entity.getFileName();
        dto.type = entity.getType();

        dto.children = entity.getChildList().stream()
                .map(FileResponseDto::from)
                .collect(Collectors.toList());

        return dto;
    }
}