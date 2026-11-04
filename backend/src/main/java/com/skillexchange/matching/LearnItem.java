package com.skillexchange.matching;

import java.util.Objects;

/**
 * Một kỹ năng người dùng muốn học, đầu vào của lõi matching.
 * targetLevel là Integer vì mục tiêu không bắt buộc (spec 4.6): null = không đặt mục tiêu.
 * Mang theo tên kỹ năng để lõi tự xếp youLearn/theyLearn theo tên (D46, kế hoạch tuần 4 bước 1).
 */
public record LearnItem(long skillId, String skillName, Integer targetLevel) {

    public LearnItem {
        Objects.requireNonNull(skillName, "skillName");
    }
}
