package com.skillexchange.auth;

import jakarta.validation.constraints.NotBlank;

/** Body của POST /api/auth/verify-email: {token} lấy từ link trong thư (D5). */
public record VerifyEmailRequest(
        @NotBlank(message = "Thiếu mã xác thực trong link") String token) {
}
