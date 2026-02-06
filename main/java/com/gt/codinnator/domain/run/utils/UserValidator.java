package com.gt.codinnator.domain.run.utils;

public class UserValidator {

    /**
     * 아이디 유효성을 검사합니다.
     * 1. null이 아니어야 함 (null_input_test 대응)
     * 2. 길이가 3글자 이상이어야 함 (short_length_test 대응)
     * 3. 숫자로 시작하면 안 됨 (start_with_digit_test 대응)
     */
    public boolean validateUsername(String username) {
        // 1. null 체크
        if (username == null) {
            return false;
        }

        // 2. 길이 체크 (2글자 이하는 false)
        if (username.length() <= 2) {
            return false;
        }

        // 3. 첫 글자가 숫자인지 체크
        if (Character.isDigit(username.charAt(0))) {
            return false;
        }

        // 모든 조건을 만족하면 true
        return true;
    }
}