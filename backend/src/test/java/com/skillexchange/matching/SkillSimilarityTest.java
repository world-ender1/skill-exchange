package com.skillexchange.matching;

import static com.skillexchange.matching.MatchingFixtures.CHINESE;
import static com.skillexchange.matching.MatchingFixtures.JAVA;
import static com.skillexchange.matching.MatchingFixtures.KOREAN;
import static com.skillexchange.matching.MatchingFixtures.KOTLIN;
import static com.skillexchange.matching.MatchingFixtures.PYTHON;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit test cho Sim(s, t), spec 4.1. Quan hệ lấy từ MatchingFixtures (giống V2__seed_skills.sql). */
class SkillSimilarityTest {

    private final SkillSimilarity similarity = MatchingFixtures.similarity();

    @Test
    @DisplayName("TC101 trùng chính xác: sim(Java, Java) = 1.0")
    void tc101_exactMatchIsOne() {
        assertThat(similarity.sim(JAVA, JAVA)).isEqualTo(1.0);
    }

    @Test
    @DisplayName("TC102 liên quan, tra được cả hai chiều: sim(Java, Kotlin) = sim(Kotlin, Java) = 0.5")
    void tc102_relatedIsHalfInBothDirections() {
        assertThat(similarity.sim(JAVA, KOTLIN)).isEqualTo(0.5);
        assertThat(similarity.sim(KOTLIN, JAVA)).isEqualTo(0.5);
    }

    @Test
    @DisplayName("TC102 cặp khai báo ngược thứ tự (b, a) vẫn tra được")
    void tc102_relationDeclaredInReverseOrder() {
        SkillSimilarity reversed = new SkillSimilarity(java.util.List.of(new SkillRelation(KOTLIN, JAVA)));

        assertThat(reversed.sim(JAVA, KOTLIN)).isEqualTo(0.5);
        assertThat(reversed.sim(KOTLIN, JAVA)).isEqualTo(0.5);
    }

    @Test
    @DisplayName("TC103 cùng nhóm nhưng không đánh dấu liên quan: sim(Java, Python) = 0.0")
    void tc103_sameCategoryWithoutRelationIsZero() {
        assertThat(similarity.sim(JAVA, PYTHON)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("TC103 quan hệ không bắc cầu: sim(Tiếng Trung, Tiếng Hàn) = 0.0")
    void tc103_relationIsNotTransitive() {
        // Trung–Nhật và Nhật–Hàn liên quan, nhưng Trung–Hàn thì không.
        assertThat(similarity.sim(CHINESE, KOREAN)).isEqualTo(0.0);
    }
}
