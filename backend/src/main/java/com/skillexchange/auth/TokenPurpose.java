package com.skillexchange.auth;

/** Cột user_tokens.purpose: CHECK (purpose IN ('VERIFY_EMAIL', 'RESET_PASSWORD')). */
public enum TokenPurpose {
    VERIFY_EMAIL,
    RESET_PASSWORD // chưa dùng ở tuần 3 (D2), có sẵn để làm quên mật khẩu sau này
}
