package com.nexgen.order_service.support;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.StringJoiner;

/**
 * Builds real HS256 tokens so slice tests exercise the resource-server decoder
 * instead of bypassing it with Spring Security's {@code jwt()} post-processor.
 */
public final class JwtTestSupport {

    public static final String SECRET = "01234567890123456789012345678901";

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private JwtTestSupport() {
    }

    public static String accessToken(String subject, String... roles) {
        return token("nexgen-user-service", "nexgen-api", "access", subject, roles);
    }

    public static String refreshToken(String subject) {
        return token("nexgen-user-service", "nexgen-api", "refresh", subject);
    }

    public static String token(String issuer, String audience, String tokenType,
                               String subject, String... roles) {
        long now = Instant.now().getEpochSecond();
        StringJoiner roleValues = new StringJoiner("\",\"", "\"", "\"");
        for (String role : roles) {
            roleValues.add(role);
        }
        String rolesJson = roles.length == 0 ? "" : roleValues.toString();

        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = encode(String.format(
                "{\"sub\":\"%s\",\"iss\":\"%s\",\"aud\":\"%s\",\"token_type\":\"%s\","
                        + "\"roles\":[%s],\"iat\":%d,\"exp\":%d}",
                subject, issuer, audience, tokenType, rolesJson, now, now + 300));

        String signingInput = header + "." + payload;
        return signingInput + "." + encode(hmac(signingInput));
    }

    private static byte[] hmac(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign test JWT", exception);
        }
    }

    private static String encode(byte[] bytes) {
        return ENCODER.encodeToString(bytes);
    }

    private static String encode(String value) {
        return encode(value.getBytes(StandardCharsets.UTF_8));
    }
}
