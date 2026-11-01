package com.skillexchange.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit test thuần Java cho UserToken.isUsableAt (D26), không cần DB.
 * Thêm ở bước 11: làm hỏng điều kiện "chưa dùng" mà test tích hợp vẫn xanh, vì verify-email còn
 * chặn lớp thứ hai (user đã ACTIVE). Hàm này sẽ dùng lại cho token đặt lại mật khẩu (D2), nên kiểm riêng.
 */
class UserTokenTest {

    private static final Instant NOW = Instant.parse("2026-11-02T08:00:00Z");
    private static final Duration LIFETIME = Duration.ofHours(24);

    private UserToken newToken() {
        return UserToken.issue(null, TokenPurpose.VERIFY_EMAIL, "hash", NOW, LIFETIME);
    }

    @Test
    @DisplayName("D26 token mới, chưa hết hạn: dùng được")
    void d26_freshTokenIsUsable() {
        assertThat(newToken().isUsableAt(NOW.plusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("D26 token đã dùng: không dùng lại được dù còn hạn")
    void d26_usedTokenIsNotUsable() {
        UserToken token = newToken();
        token.markUsed(NOW.plusSeconds(1));

        assertThat(token.isUsableAt(NOW.plusSeconds(2))).isFalse();
    }

    @Test
    @DisplayName("D26 đúng lúc expires_at: đã hết hạn")
    void d26_expiredExactlyAtExpiresAt() {
        assertThat(newToken().isUsableAt(NOW.plus(LIFETIME))).isFalse();
    }
}
