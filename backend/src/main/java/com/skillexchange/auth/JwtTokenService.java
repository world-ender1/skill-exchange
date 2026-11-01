package com.skillexchange.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.skillexchange.user.User;

/**
 * Phát vé JWT khi đăng nhập (D14). Vé ký HS256 bằng JWT_SECRET (JwtEncoder ở SecurityConfig).
 *
 * Phần thân vé chỉ được mã hóa Base64, AI CŨNG ĐỌC ĐƯỢC; chữ ký chỉ chống sửa, không chống đọc.
 * Vì vậy vé chỉ chứa id và vai trò, không chứa email, mật khẩu hay số điện thoại.
 */
@Service
public class JwtTokenService {

    static final Duration TOKEN_LIFETIME = Duration.ofHours(24);

    private final JwtEncoder jwtEncoder;
    private final Clock clock;

    public JwtTokenService(JwtEncoder jwtEncoder, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
    }

    public String issueAccessToken(User user) {
        // Giờ lấy từ bean Clock: bộ giải mã vé cũng kiểm hạn theo đúng đồng hồ này (SecurityConfig).
        Instant issuedAt = clock.instant();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getRole().name())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(TOKEN_LIFETIME))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
