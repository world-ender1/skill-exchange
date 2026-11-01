package com.skillexchange.common;

/**
 * Xung đột với dữ liệu đã có hoặc sai trạng thái (TK 4.1): GlobalExceptionHandler trả 409,
 * message dùng làm "detail" nên phải là câu cho người dùng đọc, không chứa dữ liệu nhạy cảm.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
