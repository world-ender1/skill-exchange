package com.skillexchange.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body của POST /api/auth/register (D25). Kiểm định dạng và độ dài ở đây;
 * đuôi email trường và giới hạn 72 byte kiểm ở AuthService (cần cấu hình / tính byte).
 */
public record RegisterRequest(

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 255, message = "Email dài tối đa 255 ký tự")
        String email,

        // Không cắt khoảng trắng: khoảng trắng là một phần của mật khẩu.
        @NotNull(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 72, message = "Mật khẩu phải dài từ 8 đến 72 ký tự")
        String password,

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 100, message = "Họ tên dài tối đa 100 ký tự")
        String fullName) {

    /**
     * Constructor rút gọn của record: chạy khi Jackson tạo object từ JSON, TRƯỚC khi @Valid kiểm tra.
     * Nhờ vậy các annotation ở trên kiểm trên giá trị đã chuẩn hóa (D9, D16).
     */
    public RegisterRequest {
        email = Emails.normalize(email);
        fullName = fullName == null ? null : fullName.strip();
    }
}
