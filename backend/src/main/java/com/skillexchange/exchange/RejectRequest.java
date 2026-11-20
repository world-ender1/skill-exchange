package com.skillexchange.exchange;

import jakarta.validation.constraints.Size;

/**
 * Body của POST /api/requests/{id}/reject (D90). Cả body cũng không bắt buộc: gửi không có body thì controller
 * nhận null, gửi {} thì note null. @Valid kiểm độ dài trước khi vào service, nên note quá dài là 400 kể cả khi
 * yêu cầu không tồn tại (400 trước 404, D87).
 */
public record RejectRequest(

        @Size(max = 1000, message = "Ghi chú dài tối đa 1000 ký tự")
        String note) {

    /** Cắt khoảng trắng trước khi @Valid kiểm, nên độ dài đếm sau khi cắt (D88). */
    public RejectRequest {
        note = stripToNull(note);
    }

    /** Ghi chú không bắt buộc: rỗng sau khi cắt thì lưu NULL (D88), như lời nhắn của CreateRequestRequest. */
    private static String stripToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
