package com.skillexchange.matching;

import static com.skillexchange.matching.MatchingFixtures.CHINESE;
import static com.skillexchange.matching.MatchingFixtures.JAPANESE;
import static com.skillexchange.matching.MatchingFixtures.JAVA;
import static com.skillexchange.matching.MatchingFixtures.KOREAN;
import static com.skillexchange.matching.MatchingFixtures.KOTLIN;
import static com.skillexchange.matching.MatchingFixtures.PYTHON;
import static com.skillexchange.matching.MatchingFixtures.learn;
import static com.skillexchange.matching.MatchingFixtures.teach;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit test cho LevelFit (spec 4.3, 4.6) và cách chọn kỹ năng dạy cho mỗi kỹ năng muốn học (D46a).
 * "Thấp hơn k bậc" = mục tiêu − trình độ = k. Đạt hoặc vượt → 1.0; thấp 1 bậc hoặc không mục tiêu → 0.5;
 * thấp từ 2 bậc → 0.0. Trung bình chỉ trên các kỹ năng muốn học có Sim > 0.
 */
class LevelFitTest {

    private static final double TOLERANCE = 1e-9;

    private final ScoreCalculator calculator = new ScoreCalculator(MatchingFixtures.similarity());

    private double levelFitOfOne(Integer targetLevel, int teachLevel) {
        return calculator.levelFit(List.of(learn(JAVA, targetLevel)), List.of(teach(JAVA, teachLevel)));
    }

    @ParameterizedTest(name = "mục tiêu 3, trình độ {0} → {1}")
    @CsvSource({
            "4, 1.0",
            "3, 1.0",
            "2, 0.5",
            "1, 0.0"
    })
    @DisplayName("TC108 bảng mục tiêu 3 với trình độ 4, 3, 2, 1")
    void tc108_targetThreeAgainstEachLevel(int teachLevel, double expected) {
        assertThat(levelFitOfOne(3, teachLevel)).isCloseTo(expected, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC109 mục tiêu 4, trình độ 1 (thấp 3 bậc) → 0.0")
    void tc109_threeLevelsBelowIsZero() {
        assertThat(levelFitOfOne(4, 1)).isCloseTo(0.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC109 không đặt mục tiêu, trình độ 1 → 0.5")
    void tc109_noTargetIsHalf() {
        assertThat(levelFitOfOne(null, 1)).isCloseTo(0.5, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC110 ví dụ spec 4.6: Java đạt 1.0, SQL ← MySQL thấp 1 bậc 0.5, Figma không tính → 0.75")
    void tc110_spec46Example() {
        double fit = calculator.levelFit(MatchingFixtures.spec46Wants(), MatchingFixtures.spec46Teaches());

        // Chia cả Figma vào mẫu số thì ra 0.5: test này bắt lỗi đó.
        assertThat(fit).isCloseTo(0.75, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC111 An học từ Bình: Java ← Kotlin mức 2 (mục tiêu 3) 0.5, Tiếng Anh không mục tiêu 0.5 → 0.5")
    void tc111_anLearnsFromBinh() {
        double fit = calculator.levelFit(MatchingFixtures.an().learn(), MatchingFixtures.binh().teach());

        assertThat(fit).isCloseTo(0.5, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC112 kỹ năng liên quan so trình độ bình thường: Java (2) ← Kotlin (2) → 1.0")
    void tc112_relatedSkillIsNotPenalisedFurther() {
        double fit = calculator.levelFit(List.of(learn(JAVA, 2)), List.of(teach(KOTLIN, 2)));

        assertThat(fit).isCloseTo(1.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC113 Sim đứng trước trình độ: Java (3), dạy Java (1) và Kotlin (4) → chọn Java, 0.0")
    void tc113_higherSimBeatsHigherLevel() {
        List<TeachItem> teaches = List.of(teach(KOTLIN, 4), teach(JAVA, 1));

        Optional<SkillMatch> chosen = calculator.bestTeachFor(learn(JAVA, 3), teaches);
        double fit = calculator.levelFit(List.of(learn(JAVA, 3)), teaches);

        assertThat(chosen).map(m -> m.teach().skillId()).contains(JAVA);
        assertThat(fit).isCloseTo(0.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC114 bằng Sim thì chọn trình độ cao hơn: Tiếng Nhật (3), dạy Tiếng Trung (1) và Tiếng Hàn (3) → 1.0")
    void tc114_equalSimPicksHigherLevel() {
        List<TeachItem> teaches = List.of(teach(CHINESE, 1), teach(KOREAN, 3));

        Optional<SkillMatch> chosen = calculator.bestTeachFor(learn(JAPANESE, 3), teaches);
        double fit = calculator.levelFit(List.of(learn(JAPANESE, 3)), teaches);

        assertThat(chosen).map(m -> m.teach().skillId()).contains(KOREAN);
        assertThat(fit).isCloseTo(1.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC115 bằng Sim và trình độ thì chọn skillId nhỏ hơn (D46a)")
    void tc115_equalSimAndLevelPicksSmallerSkillId() {
        // Riêng ca này: kỹ năng muốn học 100 liên quan với cả 9 và 7. Đặt 9 trước để "lấy cái đầu tiên" là sai.
        ScoreCalculator own = new ScoreCalculator(new SkillSimilarity(List.of(
                new SkillRelation(100, 9),
                new SkillRelation(100, 7))));
        LearnItem want = new LearnItem(100, "Kỹ năng 100", 2);
        List<TeachItem> teaches = List.of(new TeachItem(9, "Kỹ năng 9", 3), new TeachItem(7, "Kỹ năng 7", 3));

        Optional<SkillMatch> chosen = own.bestTeachFor(want, teaches);

        assertThat(chosen).map(m -> m.teach().skillId()).contains(7L);
        assertThat(own.levelFit(List.of(want), teaches)).isCloseTo(1.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("D46 bestTeachFor rỗng khi không kỹ năng dạy nào có Sim > 0")
    void d46_noRelatedTeachSkillGivesEmpty() {
        Optional<SkillMatch> chosen = calculator.bestTeachFor(learn(JAVA, 3), List.of(teach(PYTHON, 4)));

        assertThat(chosen).isEmpty();
    }

    @Test
    @DisplayName("D46 bestTeachFor ghi lại Sim của cặp được chọn")
    void d46_chosenMatchCarriesSim() {
        Optional<SkillMatch> chosen = calculator.bestTeachFor(learn(JAVA, 3), List.of(teach(KOTLIN, 2)));

        assertThat(chosen).map(SkillMatch::sim).contains(0.5);
    }

    @Test
    @DisplayName("Kế hoạch bước 3: không kỹ năng muốn học nào có Sim > 0 → 0.0, không NaN")
    void plan_noMatchingSkillIsZeroNotNaN() {
        double fit = calculator.levelFit(List.of(learn(JAVA, 3)), List.of(teach(PYTHON, 4)));

        assertThat(Double.isNaN(fit)).isFalse();
        assertThat(fit).isEqualTo(0.0);
    }
}
