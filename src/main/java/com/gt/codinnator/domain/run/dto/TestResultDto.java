package com.gt.codinnator.domain.run.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TestResultDto {
    private final boolean isSuccess;
    private final String message;
}