package com.skillexchange.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Một phần tử của body PUT /api/me/learn-skills: {skillId, targetLevel?}; targetLevel null = không đặt mục tiêu. */
public record LearnSkillRequest(

        @NotNull(message = "Thiếu kỹ năng")
        Long skillId,

        @Min(value = 1, message = "Trình độ mục tiêu từ 1 đến 4")
        @Max(value = 4, message = "Trình độ mục tiêu từ 1 đến 4")
        Integer targetLevel) {
}
