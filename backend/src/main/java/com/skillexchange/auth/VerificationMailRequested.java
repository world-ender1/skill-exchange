package com.skillexchange.auth;

/**
 * Sự kiện "cần gửi thư xác thực". AuthService phát ra trong transaction;
 * VerificationMailer chỉ nhận sau khi transaction commit thành công (D27).
 */
record VerificationMailRequested(String email, String rawToken) {

    // Không in token khi record bị ghi log nhầm (toString mặc định của record in mọi thành phần).
    @Override
    public String toString() {
        return "VerificationMailRequested[email=" + email + "]";
    }
}
