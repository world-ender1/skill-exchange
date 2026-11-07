package com.skillexchange.common;

/**
 * Không tìm thấy tài nguyên, hoặc có nhưng người gọi không được biết là có (TK 4.3: tài khoản bị khóa,
 * chưa xác thực, admin đều trả 404 như không tồn tại). GlobalExceptionHandler trả 404, message làm "detail".
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
