package com.skillexchange.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Một phần tử của body PUT /api/me/availability: {dayOfWeek, period}. */
public record AvailabilitySlotRequest(

        @NotNull(message = "Thiếu ngày trong tuần")
        @Min(value = 1, message = "Ngày trong tuần từ 1 (Thứ Hai) đến 7 (Chủ nhật)")
        @Max(value = 7, message = "Ngày trong tuần từ 1 (Thứ Hai) đến 7 (Chủ nhật)")
        Integer dayOfWeek,

        // Chuỗi thay vì enum, để giá trị sai thành lỗi theo trường (như learningMode ở bước 8).
        @NotNull(message = "Thiếu buổi")
        @Pattern(regexp = "MORNING|AFTERNOON|EVENING", message = "Buổi phải là MORNING, AFTERNOON hoặc EVENING")
        String period) {

    /** Đã qua @Pattern nên chắc chắn hợp lệ. */
    Period periodValue() {
        return Period.valueOf(period);
    }
}
