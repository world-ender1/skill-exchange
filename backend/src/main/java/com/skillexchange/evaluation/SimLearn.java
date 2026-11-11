package com.skillexchange.evaluation;

/**
 * Một kỹ năng người giả lập muốn học. target null = bỏ trống mục tiêu (spec 4.6).
 * Mục tiêu không có nhiễu nên chỉ có một lớp (D53).
 */
public record SimLearn(long skillId, Integer target) {

    public SimLearn {
        if (target != null && (target < 1 || target > 4)) {
            throw new IllegalArgumentException("target phải trong 1–4 hoặc null: " + target);
        }
    }
}
