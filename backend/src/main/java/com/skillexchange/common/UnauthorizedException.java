package com.skillexchange.common;

import org.springframework.security.core.AuthenticationException;

/**
 * 401 do service ném, kèm thông báo cho người dùng đọc (ví dụ đăng nhập sai, D13).
 * Kế thừa AuthenticationException của Spring Security nên được xử lý như mọi lỗi 401 khác;
 * GlobalExceptionHandler bắt riêng class này để dùng message làm "detail".
 */
public class UnauthorizedException extends AuthenticationException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
