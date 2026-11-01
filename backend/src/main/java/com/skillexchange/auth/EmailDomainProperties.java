package com.skillexchange.auth;

import java.util.List;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

/**
 * Đọc "app.allowed-email-domains" trong application.yml thành một object Java (D9).
 * @Validated + @NotEmpty: thiếu cấu hình thì ứng dụng dừng lúc khởi động, không âm thầm từ chối mọi email.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record EmailDomainProperties(@NotEmpty List<String> allowedEmailDomains) {

    /** Email (đã chuẩn hóa) có đuôi nằm trong danh sách không. */
    public boolean isSchoolEmail(String email) {
        String domain = email.substring(email.lastIndexOf('@') + 1);
        return allowedEmailDomains.stream()
                .map(allowed -> allowed.strip().toLowerCase(Locale.ROOT))
                .anyMatch(domain::equals);
    }
}
