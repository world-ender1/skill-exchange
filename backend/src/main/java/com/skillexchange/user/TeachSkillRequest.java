package com.skillexchange.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Một phần tử của body PUT /api/me/teach-skills: {skillId, level}; level bắt buộc (TC52). */
public record TeachSkillRequest(

        @NotNull(message = "Thiếu kỹ năng")
        Long skillId,

        @NotNull(message = "Kỹ năng dạy phải có trình độ")
        @Min(value = 1, message = "Trình độ từ 1 đến 4")
        @Max(value = 4, message = "Trình độ từ 1 đến 4")
        Integer level) {
}
