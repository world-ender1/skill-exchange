package com.skillexchange.matching;

import static com.skillexchange.matching.MatchingFixtures.FIGMA;
import static com.skillexchange.matching.MatchingFixtures.JAVA;
import static com.skillexchange.matching.MatchingFixtures.KOTLIN;
import static com.skillexchange.matching.MatchingFixtures.learn;
import static com.skillexchange.matching.MatchingFixtures.teach;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit test cho SkillFit(u, v) = trung bình trên s ∈ W_u của max_{t ∈ T_v} Sim(s, t) (spec 4.1).
 * Mong đợi lấy từ docs/tuan_4/kiem_thu/ca_kiem_thu.md; số thực so bằng dung sai 1e-9 (D36).
 */
class SkillFitTest {

    private static final double TOLERANCE = 1e-9;

    private final ScoreCalculator calculator = new ScoreCalculator(MatchingFixtures.similarity());

    @Test
    @DisplayName("TC104 muốn học Java, Figma; ứng viên chỉ dạy Java → 0.5")
    void tc104_oneOfTwoWantsMatched() {
        double fit = calculator.skillFit(
                List.of(learn(JAVA, null), learn(FIGMA, null)),
                List.of(teach(JAVA, 3)));

        assertThat(fit).isCloseTo(0.5, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC105 muốn học Java; ứng viên dạy Kotlin và Java → 1.0 (lấy max, không trung bình trên T_v)")
    void tc105_takesBestTeachSkillNotAverage() {
        double fit = calculator.skillFit(
                List.of(learn(JAVA, null)),
                List.of(teach(KOTLIN, 4), teach(JAVA, 1)));

        assertThat(fit).isCloseTo(1.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC106 An học từ Bình: Java ← Kotlin 0.5, Tiếng Anh ← Tiếng Anh 1.0 → 0.75")
    void tc106_anLearnsFromBinh() {
        double fit = calculator.skillFit(MatchingFixtures.an().learn(), MatchingFixtures.binh().teach());

        assertThat(fit).isCloseTo(0.75, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC107 W_u rỗng → 0.0, không ném lỗi, không NaN")
    void tc107_emptyWantsIsZeroNotNaN() {
        double fit = calculator.skillFit(List.of(), List.of(teach(JAVA, 3)));

        assertThat(Double.isNaN(fit)).isFalse();
        assertThat(fit).isEqualTo(0.0);
    }

    @Test
    @DisplayName("TC107 bên kia không dạy gì → 0.0")
    void tc107_emptyTeachesIsZero() {
        double fit = calculator.skillFit(List.of(learn(JAVA, 3)), List.of());

        assertThat(fit).isEqualTo(0.0);
    }
}
