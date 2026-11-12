package com.skillexchange.evaluation;

import static com.skillexchange.evaluation.EvaluationFixtures.CATALOG;
import static com.skillexchange.evaluation.EvaluationFixtures.evening;
import static com.skillexchange.evaluation.EvaluationFixtures.generatedProfiles;
import static com.skillexchange.evaluation.EvaluationFixtures.ids;
import static com.skillexchange.evaluation.EvaluationFixtures.learn;
import static com.skillexchange.evaluation.EvaluationFixtures.morning;
import static com.skillexchange.evaluation.EvaluationFixtures.profile;
import static com.skillexchange.evaluation.EvaluationFixtures.teach;
import static com.skillexchange.evaluation.ScheduleFitFormula.JACCARD;
import static com.skillexchange.evaluation.ScheduleFitFormula.MIN;
import static com.skillexchange.evaluation.ScheduleFitFormula.TRAN_3;
import static com.skillexchange.user.LearningMode.ANY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.skillexchange.matching.MatchProfile;
import com.skillexchange.matching.MatchResult;
import com.skillexchange.matching.MatchType;
import com.skillexchange.matching.MatchingEngine;
import com.skillexchange.matching.MatchingResult;
import com.skillexchange.matching.MatchingWeights;
import com.skillexchange.matching.SkillSimilarity;
import com.skillexchange.matching.Slot;

/** Công thức ScheduleFit khác và tính lại điểm từ đầu ra của lõi: TC242, TC243 (D68; spec 5.4). */
class RescorerTest {

    private static final double EPS = 1e-9;

    // ---------- TC242 (b)–(d): ba công thức trên hai tập khung ----------

    @Test
    @DisplayName("TC242 (b) u 4 khung, v 3 khung, trùng 2: MIN 2/3, JACCARD 2/5, TRAN_3 2/3")
    void tc242b_fourAndThreeSlotsTwoCommon() {
        Set<Slot> u = Set.of(evening(1), evening(2), evening(3), evening(4));
        Set<Slot> v = Set.of(evening(1), evening(2), morning(1));

        assertThat(MIN.scheduleFit(u, v)).isCloseTo(2.0 / 3, within(EPS));
        assertThat(JACCARD.scheduleFit(u, v)).isCloseTo(2.0 / 5, within(EPS));   // hợp = 4 + 3 - 2 = 5
        assertThat(TRAN_3.scheduleFit(u, v)).isCloseTo(2.0 / 3, within(EPS));
    }

    @Test
    @DisplayName("TC242 (c) u 6 khung, v 5 khung, trùng 5: MIN 1, JACCARD 5/6, TRAN_3 1")
    void tc242c_sixAndFiveSlotsFiveCommon() {
        Set<Slot> u = Set.of(evening(1), evening(2), evening(3), evening(4), evening(5), evening(6));
        Set<Slot> v = Set.of(evening(1), evening(2), evening(3), evening(4), evening(5));

        assertThat(MIN.scheduleFit(u, v)).isCloseTo(1.0, within(EPS));
        assertThat(JACCARD.scheduleFit(u, v)).isCloseTo(5.0 / 6, within(EPS));
        assertThat(TRAN_3.scheduleFit(u, v)).isCloseTo(1.0, within(EPS)); // min(5, 3) / 3
    }

    @Test
    @DisplayName("TC242 (d) Một bên chưa khai lịch: cả ba công thức bằng 0.5")
    void tc242d_undeclaredScheduleIsNeutral() {
        Set<Slot> declared = Set.of(evening(1), evening(2), evening(3));
        Set<Slot> none = Set.of();

        for (ScheduleFitFormula formula : ScheduleFitFormula.values()) {
            assertThat(formula.scheduleFit(declared, none)).as(formula + " người dạy chưa khai").isEqualTo(0.5);
            assertThat(formula.scheduleFit(none, declared)).as(formula + " người học chưa khai").isEqualTo(0.5);
            assertThat(formula.scheduleFit(none, none)).as(formula + " cả hai chưa khai").isEqualTo(0.5);
        }
    }

    // ---------- TC242 (a): MIN tính lại bằng đúng lõi ----------

    @Test
    @DisplayName("TC242 (a) N = 200, seed 1, nhiễu mặc định: tính lại với MIN ra đúng D, MutualScore và thứ tự của lõi, ở mọi bộ trọng số")
    void tc242a_minReproducesCoreScores() {
        List<MatchProfile> profiles = generatedProfiles(200, 1, NoiseLevel.MAC_DINH);
        List<WeightSets.Named> allWeights = new ArrayList<>(WeightSets.SENSITIVITY);
        allWeights.addAll(WeightSets.ABLATION);
        int twoWayChecked = 0;
        int oneWayChecked = 0;

        // Chạy mọi bộ trọng số: Rescorer phải dùng trọng số của mình, không ghi cứng bộ mặc định.
        for (WeightSets.Named named : allWeights) {
            MatchingEngine engine = new MatchingEngine(new SkillSimilarity(CATALOG.relations()), named.weights());
            Rescorer rescorer = new Rescorer(named.weights(), MIN);

            for (MatchProfile caller : profiles) {
                MatchingResult core = engine.match(caller, profiles);
                MatchingResult rescored = rescorer.rescore(caller, profiles, core);

                assertSameScores(named.phuongAn(), rescored.twoWay(), core.twoWay());
                assertSameScores(named.phuongAn(), rescored.oneWay(), core.oneWay());
                twoWayChecked += core.twoWay().size();
                oneWayChecked += core.oneWay().size();
            }
        }
        // Dữ liệu phải có đủ hai nhóm, nếu không phép so ở trên không kiểm gì.
        assertThat(twoWayChecked).isPositive();
        assertThat(oneWayChecked).isPositive();
    }

    private static void assertSameScores(String phuongAn, List<MatchResult> rescored, List<MatchResult> core) {
        assertThat(ids(rescored)).as(phuongAn + " thứ tự").isEqualTo(ids(core));
        for (int i = 0; i < core.size(); i++) {
            MatchResult expected = core.get(i);
            MatchResult actual = rescored.get(i);
            String where = phuongAn + " ứng viên " + expected.userId();

            assertThat(actual.type()).as(where).isEqualTo(expected.type());
            assertThat(actual.score()).as(where + " score").isCloseTo(expected.score(), within(EPS));
            assertThat(actual.forward().scheduleFit()).as(where + " forward ScheduleFit")
                    .isCloseTo(expected.forward().scheduleFit(), within(EPS));
            assertThat(actual.forward().d()).as(where + " forward D").isCloseTo(expected.forward().d(), within(EPS));
            if (expected.type() == MatchType.TWO_WAY) {
                assertThat(actual.backward().d()).as(where + " backward D")
                        .isCloseTo(expected.backward().d(), within(EPS));
            } else {
                assertThat(actual.backward()).as(where + " backward").isNull();
            }
        }
    }

    // ---------- TC243: đổi công thức thì thứ tự đổi, lõi không đổi ----------

    /**
     * u muốn học Java, rảnh tối T2–T7 (6 khung), dạy Piano.
     * a (mã 2) dạy Java, rảnh tối T2 (1 khung, trùng 1).
     * b (mã 3) dạy Java cùng trình độ và hình thức, rảnh tối T2–T5 và sáng T2, T3 (6 khung, trùng 4).
     * a, b chỉ muốn học Guitar, nên cả hai là ứng viên một chiều: chỉ ScheduleFit khác nhau.
     */
    private static final MatchProfile U = profile(1,
            List.of(learn("Java", null)), List.of(teach("Piano", 3)),
            Set.of(evening(1), evening(2), evening(3), evening(4), evening(5), evening(6)), ANY);
    private static final MatchProfile A = profile(2,
            List.of(learn("Guitar", null)), List.of(teach("Java", 3)),
            Set.of(evening(1)), ANY);
    private static final MatchProfile B = profile(3,
            List.of(learn("Guitar", null)), List.of(teach("Java", 3)),
            Set.of(evening(1), evening(2), evening(3), evening(4), morning(1), morning(2)), ANY);
    private static final List<MatchProfile> TC243_PROFILES = List.of(U, A, B);

    @Test
    @DisplayName("TC243 MIN: a (1.0) trước b (4/6)")
    void tc243_min() {
        List<MatchResult> list = rescoreTc243(MIN);

        assertThat(ids(list)).containsExactly(2L, 3L);
        assertThat(list.get(0).forward().scheduleFit()).isCloseTo(1.0, within(EPS));
        assertThat(list.get(1).forward().scheduleFit()).isCloseTo(4.0 / 6, within(EPS));
    }

    @Test
    @DisplayName("TC243 JACCARD: b (4/8) trước a (1/6)")
    void tc243_jaccard() {
        List<MatchResult> list = rescoreTc243(JACCARD);

        assertThat(ids(list)).containsExactly(3L, 2L);
        assertThat(list.get(0).forward().scheduleFit()).isCloseTo(4.0 / 8, within(EPS));
        assertThat(list.get(1).forward().scheduleFit()).isCloseTo(1.0 / 6, within(EPS));
    }

    @Test
    @DisplayName("TC243 TRAN_3: b (1.0) trước a (1/3)")
    void tc243_cappedAtThree() {
        List<MatchResult> list = rescoreTc243(TRAN_3);

        assertThat(ids(list)).containsExactly(3L, 2L);
        assertThat(list.get(0).forward().scheduleFit()).isCloseTo(1.0, within(EPS));
        assertThat(list.get(1).forward().scheduleFit()).isCloseTo(1.0 / 3, within(EPS));
    }

    @Test
    @DisplayName("TC243 Lõi không đổi: sau khi tính lại, gọi lõi trực tiếp vẫn ra thứ tự của MIN; Rankers.p dùng đúng Rescorer")
    void tc243_coreUnchanged() {
        MatchingEngine engine = new MatchingEngine(new SkillSimilarity(CATALOG.relations()), MatchingWeights.DEFAULT);
        Rankers rankers = new Rankers(CATALOG.relations());

        rescoreTc243(JACCARD);
        rescoreTc243(TRAN_3);

        MatchingResult core = engine.match(U, TC243_PROFILES);
        assertThat(ids(core.oneWay())).containsExactly(2L, 3L);
        assertThat(core.oneWay().get(0).forward().scheduleFit()).isCloseTo(1.0, within(EPS));
        // Đường mà bước 7 dùng: P với công thức khác phải ra cùng thứ tự với Rescorer.
        assertThat(rankers.p(MatchingWeights.DEFAULT, MIN).rank(U, TC243_PROFILES)).containsExactly(2L, 3L);
        assertThat(rankers.p(MatchingWeights.DEFAULT, JACCARD).rank(U, TC243_PROFILES)).containsExactly(3L, 2L);
        assertThat(rankers.p(MatchingWeights.DEFAULT, TRAN_3).rank(U, TC243_PROFILES)).containsExactly(3L, 2L);
    }

    /** Lõi chấm bằng bộ mặc định (ScheduleFit = MIN), rồi tính lại với formula. a, b ở nhóm một chiều. */
    private static List<MatchResult> rescoreTc243(ScheduleFitFormula formula) {
        MatchingEngine engine = new MatchingEngine(new SkillSimilarity(CATALOG.relations()), MatchingWeights.DEFAULT);
        MatchingResult core = engine.match(U, TC243_PROFILES);
        assertThat(core.twoWay()).isEmpty();

        MatchingResult rescored = new Rescorer(MatchingWeights.DEFAULT, formula).rescore(U, TC243_PROFILES, core);
        assertThat(rescored.twoWay()).isEmpty();
        return rescored.oneWay();
    }
}
