package com.skillexchange.evaluation;

/**
 * Một kỹ năng người giả lập dạy được, có hai lớp trình độ (spec 5.1, D53):
 * - trueLevel: trình độ thật, chỉ bộ gán nhãn đọc;
 * - declaredLevel: trình độ tự khai (có thể lệch do nhiễu), thuật toán đọc qua SimUser.toMatchProfile.
 */
public record SimTeach(long skillId, int trueLevel, int declaredLevel) {

    public SimTeach {
        requireLevel("trueLevel", trueLevel);
        requireLevel("declaredLevel", declaredLevel);
    }

    /** Thang trình độ 1–4 (spec 4.6). */
    private static void requireLevel(String field, int level) {
        if (level < 1 || level > 4) {
            throw new IllegalArgumentException(field + " phải trong 1–4: " + level);
        }
    }
}
