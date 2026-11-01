package com.skillexchange.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Đọc "app.admin.email", "app.admin.password" (lấy từ ADMIN_EMAIL, ADMIN_PASSWORD trong .env) (D4).
 *
 * Cố ý KHÔNG dùng @Validated như EmailDomainProperties: khi validate thất bại, Spring Boot in
 * cả giá trị bị từ chối ra log, tức là in mật khẩu. AdminInitializer tự kiểm và báo lỗi không kèm giá trị.
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String email, String password) {
}
