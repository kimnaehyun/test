package com.gt.codinnator.domain.run.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedTest {

    UserValidator validator = new UserValidator();

    @Test
    @DisplayName("입력이 null이면 false를 반환한다")
    void null_input_test() {
        // Given
        String username = null;

        // When
        boolean result = validator.validateUsername(username);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("아이디가 너무 짧으면(2글자) false를 반환한다")
    void short_length_test() {
        // Given
        String username = "ab";

        // When
        boolean result = validator.validateUsername(username);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("숫자로 시작하면 false를 반환한다")
    void start_with_digit_test() {
        // Given
        String username = "1user";

        // When
        boolean result = validator.validateUsername(username);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("조건을 만족하는 정상적인 아이디는 true를 반환한다")
    void valid_username_test() {
        // Given
        String username = "User1";

        // When
        boolean result = validator.validateUsername(username);

        // Then
        assertTrue(result);
    }
}