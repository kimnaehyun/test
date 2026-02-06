package com.gt.codinnator.domain.editor.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileRequestDto {
    Long parentId;
    String fileName;
    String type;
    String content;
}
