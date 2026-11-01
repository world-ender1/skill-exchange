package com.skillexchange.common;

/**
 * Dữ liệu sai mà chỉ Service mới kiểm được (cần cấu hình hoặc tính toán, ví dụ đuôi email trường,
 * giới hạn 72 byte). GlobalExceptionHandler trả 400 cùng dạng với lỗi @Valid: errors[{field, message}].
 */
public class FieldValidationException extends RuntimeException {

    private final String field;

    public FieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
