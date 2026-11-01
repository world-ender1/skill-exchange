package com.skillexchange.auth;

import java.util.Locale;

/** Chuẩn hóa email dùng chung cho mọi chỗ nhận email: đăng ký, đăng nhập, gửi lại thư, ADMIN_EMAIL (D9). */
public final class Emails {

    private Emails() {
    }

    /** Bỏ khoảng trắng hai đầu, đổi về chữ thường. null giữ nguyên để @NotBlank báo lỗi. */
    public static String normalize(String email) {
        if (email == null) {
            return null;
        }
        // Locale.ROOT: tránh lỗi đổi chữ hoa theo ngôn ngữ máy (ví dụ chữ "I" trong tiếng Thổ Nhĩ Kỳ).
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
