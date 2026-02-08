package com.gt.codinnator.domain.user.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;

@Converter
@RequiredArgsConstructor
public class GitTokenConverter implements AttributeConverter<String, String> {

    private final EncryptionUtils encryptionUtils;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        // DB에 저장할 때 암호화
        return (attribute != null) ? encryptionUtils.encrypt(attribute) : null;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        // DB에서 읽어올 때 복호화
        return (dbData != null) ? encryptionUtils.decrypt(dbData) : null;
    }
}