package com.nexgen.user_service.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hashes bearer artifacts before they are written to Redis. This limits the
 * value of a cache dump: the raw token cannot be replayed from the stored hash.
 */
public final class TokenHashing {

    private TokenHashing() {
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required by the JVM specification.
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
