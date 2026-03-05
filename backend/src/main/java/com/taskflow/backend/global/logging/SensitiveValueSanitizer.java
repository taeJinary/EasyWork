package com.taskflow.backend.global.logging;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.util.StringUtils;

public final class SensitiveValueSanitizer {

    private static final int HASH_BYTE_LENGTH = 8;

    private SensitiveValueSanitizer() {
    }

    public static String shortHash(String value) {
        if (!StringUtils.hasText(value)) {
            return "empty";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, HASH_BYTE_LENGTH);
        } catch (NoSuchAlgorithmException exception) {
            return "unavailable";
        }
    }
}
