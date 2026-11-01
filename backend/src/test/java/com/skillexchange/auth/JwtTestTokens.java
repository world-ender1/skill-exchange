package com.skillexchange.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Tạo vé "xấu" cho test (bước 6). Vé hợp lệ lấy bằng cách gọi API đăng nhập thật;
 * vé hết hạn tạo bằng cách vặn đồng hồ giả. Ở đây chỉ có hai kiểu vé giả mạo.
 */
final class JwtTestTokens {

    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private JwtTestTokens() {
    }

    /**
     * Sửa phần thân (payload) của vé thật, giữ nguyên chữ ký cũ (TC63).
     * Ví dụ đổi "role":"STUDENT" thành "role":"ADMIN": chữ ký không còn khớp nội dung.
     */
    static String tamperPayload(String token, String from, String to) {
        String[] parts = token.split("\\.");
        String payload = new String(DECODER.decode(parts[1]), StandardCharsets.UTF_8);
        if (!payload.contains(from)) {
            throw new IllegalArgumentException("Payload không có chuỗi " + from + ": " + payload);
        }
        String forged = ENCODER.encodeToString(payload.replace(from, to).getBytes(StandardCharsets.UTF_8));
        return parts[0] + "." + forged + "." + parts[2];
    }

    /** Vé đúng định dạng, đúng nội dung, nhưng ký bằng một khóa khác JWT_SECRET (TC63). */
    static String signedWithOtherKey(long userId, String role, Instant issuedAt) {
        byte[] otherSecret = "khoa-khac-hoan-toan-voi-JWT_SECRET-it-nhat-32-byte".getBytes(StandardCharsets.UTF_8);
        NimbusJwtEncoder encoder = NimbusJwtEncoder
                .withSecretKey(new SecretKeySpec(otherSecret, "HmacSHA256"))
                .algorithm(MacAlgorithm.HS256)
                .build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(Duration.ofHours(24)))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
