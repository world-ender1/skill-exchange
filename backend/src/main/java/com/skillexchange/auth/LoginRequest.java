package com.skillexchange.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Body của POST /api/auth/login: {email, password}.
 * Chỉ kiểm "có gửi hay không" (D24). Không kiểm định dạng email hay độ dài mật khẩu:
 * sai thì đơn giản là không khớp tài khoản nào → 401 (D13).
 */
public record LoginRequest(

        @NotBlank(message = "Email không được để trống")
        String email,

        // @NotEmpty chứ không @NotBlank: mật khẩu toàn khoảng trắng vẫn là mật khẩu hợp lệ (D10).
        @NotEmpty(message = "Mật khẩu không được để trống")
        String password) {

    public LoginRequest {
        email = Emails.normalize(email); // D9: cùng cách chuẩn hóa như lúc đăng ký
    }
}
