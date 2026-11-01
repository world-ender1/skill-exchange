package com.skillexchange.common;

/**
 * Yêu cầu không thực hiện được nhưng không gắn với một trường cụ thể (ví dụ link xác thực hỏng, D26).
 * GlobalExceptionHandler trả 400, message dùng làm "detail" nên phải là câu cho người dùng đọc.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
