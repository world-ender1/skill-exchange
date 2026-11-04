package com.skillexchange.matching;

import java.util.Objects;

/**
 * Một kỹ năng người dùng có thể dạy, đầu vào của lõi matching.
 * level là int vì kỹ năng dạy bắt buộc có trình độ (spec 4.6, cột level NOT NULL trong V1__init.sql).
 */
public record TeachItem(long skillId, String skillName, int level) {

    public TeachItem {
        Objects.requireNonNull(skillName, "skillName");
    }
}
