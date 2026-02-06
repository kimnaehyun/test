package com.gt.codinnator.domain.user.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

@Component
public class EncryptionUtils {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private final SecretKeySpec keySpec;
    private final IvParameterSpec ivSpec;

    public EncryptionUtils(@Value("${encryption.secret-key}") String secretKey) {
        // 키 길이는 반드시 16, 24, 32바이트 중 하나여야 합니다 (AES-256은 32바이트).
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
        this.ivSpec = new IvParameterSpec(Arrays.copyOfRange(keyBytes, 0, 16));
    }

    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}