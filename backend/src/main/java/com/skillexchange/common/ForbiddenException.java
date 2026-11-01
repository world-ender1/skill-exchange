package com.skillexchange.common;

import org.springframework.security.access.AccessDeniedException;

/**
 * 403 kèm thông báo cho người dùng đọc (ví dụ tài khoản bị khóa, D13).
 * Kế thừa AccessDeniedException nên dùng được cả ở tầng filter (AccessDeniedHandler)
 * lẫn trong service; GlobalExceptionHandler bắt riêng class này để dùng message làm "detail".
 */
public class ForbiddenException extends AccessDeniedException {

    public ForbiddenException(String message) {
        super(message);
    }
}
