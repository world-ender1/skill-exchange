package com.skillexchange.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.skillexchange.user.User;

/**
 * Tạo và tra token dùng một lần (D5, D6, D7). Không tự mở transaction:
 * luôn được gọi từ trong method @Transactional của AuthService.
 */
@Service
public class TokenService {

    static final Duration VERIFY_EMAIL_LIFETIME = Duration.ofHours(24);
    static final Duration MIN_INTERVAL_BETWEEN_MAILS = Duration.ofSeconds(60);
    private static final int TOKEN_BYTES = 32; // 256 bit

    // SecureRandom (không phải Random): số ngẫu nhiên đủ mạnh cho mục đích bảo mật, không đoán trước được.
    private final SecureRandom random = new SecureRandom();
    private final UserTokenRepository tokenRepository;
    private final Clock clock;

    public TokenService(UserTokenRepository tokenRepository, Clock clock) {
        this.tokenRepository = tokenRepository;
        this.clock = clock;
    }

    /**
     * Tạo token xác thực email mới: xóa token chưa dùng cũ (D7), lưu bản băm, trả token gốc để đặt vào link.
     * Token gốc không được lưu hay ghi log ở bất kỳ đâu.
     */
    public String issueVerifyEmailToken(User user) {
        tokenRepository.deleteUnused(user.getId(), TokenPurpose.VERIFY_EMAIL);

        String rawToken = newRawToken();
        tokenRepository.save(UserToken.issue(
                user, TokenPurpose.VERIFY_EMAIL, hash(rawToken), clock.instant(), VERIFY_EMAIL_LIFETIME));
        return rawToken;
    }

    /**
     * Đã đủ 60 giây kể từ token xác thực mới nhất chưa (D7)? Token tạo lúc đăng ký cũng tính.
     * Chưa có token nào (ví dụ dữ liệu cũ) thì được gửi.
     */
    public boolean canIssueVerifyEmailToken(User user) {
        return tokenRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), TokenPurpose.VERIFY_EMAIL)
                .map(latest -> {
                    Duration elapsed = Duration.between(latest.getCreatedAt(), clock.instant());
                    return elapsed.compareTo(MIN_INTERVAL_BETWEEN_MAILS) >= 0; // đúng 60 giây là được
                })
                .orElse(true);
    }

    /** Token xác thực email còn dùng được: đúng mục đích, chưa dùng, chưa hết hạn (D26). */
    public Optional<UserToken> findUsableVerifyEmailToken(String rawToken) {
        return tokenRepository.findByTokenHash(hash(rawToken))
                .filter(token -> token.getPurpose() == TokenPurpose.VERIFY_EMAIL)
                .filter(token -> token.isUsableAt(clock.instant()));
    }

    /** 32 byte ngẫu nhiên → Base64URL không dấu "=", an toàn khi đặt vào URL (D5). */
    private String newRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 dạng hex 64 ký tự (D6). Không dùng BCrypt như mật khẩu: BCrypt mỗi lần băm ra một chuỗi khác
     * (có muối ngẫu nhiên) nên không tra được "token này ứng với dòng nào". Token đã đủ ngẫu nhiên nên SHA-256 là đủ.
     */
    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM thiếu SHA-256", e); // mọi JVM chuẩn đều có, không xảy ra
        }
    }
}
