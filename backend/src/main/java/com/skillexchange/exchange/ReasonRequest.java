package com.skillexchange.exchange;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body {reason} bắt buộc, dùng chung cho POST /api/sessions/{id}/cancel (bước 7) và /reports (bước 8) (D88,
 * Chốt ở bước 7). @Valid kiểm trước khi vào service, nên lý do sai là 400 kể cả khi phiên không tồn tại
 * (400 trước 404, D87). Không gửi body thì Spring trả 400 "Nội dung JSON không đọc được".
 */
public record ReasonRequest(

        // Chuỗi rỗng hay toàn khoảng trắng đã thành null ở constructor dưới, nên @NotNull bắt được cả ba trường hợp
        // (thiếu khóa, "", "   ") với cùng một câu.
        @NotNull(message = "Chưa nhập lý do")
        @Size(max = 1000, message = "Lý do dài tối đa 1000 ký tự")
        String reason) {

    /** Cắt khoảng trắng trước khi @Valid kiểm, nên độ dài đếm sau khi cắt (D88). */
    public ReasonRequest {
        reason = stripToNull(reason);
    }

    private static String stripToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
