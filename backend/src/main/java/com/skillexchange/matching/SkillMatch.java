package com.skillexchange.matching;

import java.util.Objects;

/**
 * Một kỹ năng muốn học (want) và kỹ năng dạy được chọn để đáp ứng nó (teach), theo D46(a).
 * Là một dòng của youLearn / theyLearn trong response (TK 4.5).
 */
public record SkillMatch(LearnItem want, TeachItem teach, double sim) {

    public SkillMatch {
        Objects.requireNonNull(want, "want");
        Objects.requireNonNull(teach, "teach");
    }

    /** Trùng chính xác (Sim = 1.0) hay chỉ là kỹ năng liên quan (Sim = 0.5). */
    public boolean exact() {
        return want.skillId() == teach.skillId();
    }
}
