package com.skillexchange.matching;

import static com.skillexchange.matching.MatchingFixtures.slot;
import static com.skillexchange.user.LearningMode.ANY;
import static com.skillexchange.user.LearningMode.OFFLINE;
import static com.skillexchange.user.LearningMode.ONLINE;
import static com.skillexchange.user.Period.AFTERNOON;
import static com.skillexchange.user.Period.EVENING;
import static com.skillexchange.user.Period.MORNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.skillexchange.user.LearningMode;

/**
 * Unit test cho ScheduleFit và ModeFit (spec 4.3, 4.6).
 * Hai thành phần có giá trị trung tính 0.5 khi một bên chưa khai báo: không phạt người mới.
 * Chưa khai báo lịch = tập rỗng; chưa khai báo hình thức = null.
 */
class ScheduleAndModeFitTest {

    private static final double TOLERANCE = 1e-9;

    private final ScoreCalculator calculator = new ScoreCalculator(MatchingFixtures.similarity());

    // ---------- ScheduleFit ----------

    @Test
    @DisplayName("TC116 ví dụ spec 4.6: u 6 khung, v 3 khung, trùng 2 → 2/3")
    void tc116_sixAndThreeSlotsOverlapTwo() {
        Set<Slot> u = Set.of(slot(1, MORNING), slot(1, EVENING), slot(2, EVENING),
                slot(3, AFTERNOON), slot(4, EVENING), slot(5, MORNING));
        Set<Slot> v = Set.of(slot(1, EVENING), slot(4, EVENING), slot(7, MORNING));

        // Chia cho max(6, 3) thì ra 1/3, chia cho hợp (Jaccard) thì ra 2/7: test này bắt cả hai lỗi.
        assertThat(calculator.scheduleFit(u, v)).isCloseTo(2.0 / 3, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC116 ví dụ A.3: u 4 khung, v 3 khung, trùng 2 → 2/3")
    void tc116_fourAndThreeSlotsOverlapTwo() {
        double fit = calculator.scheduleFit(
                MatchingFixtures.a3Learner().slots(), MatchingFixtures.a3Teacher().slots());

        assertThat(fit).isCloseTo(2.0 / 3, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC116 An (3 khung) và Bình (4 khung) trùng Tối T4, T6 → 2/3, đổi chiều cùng kết quả")
    void tc116_anAndBinhBothDirections() {
        Set<Slot> an = MatchingFixtures.an().slots();
        Set<Slot> binh = MatchingFixtures.binh().slots();

        assertThat(calculator.scheduleFit(an, binh)).isCloseTo(2.0 / 3, within(TOLERANCE));
        assertThat(calculator.scheduleFit(binh, an)).isCloseTo(2.0 / 3, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC117 u 1 khung, v 5 khung, trùng 1 → 1.0 (chia cho min, nhược điểm đã biết)")
    void tc117_oneSlotFullyCoveredIsOne() {
        Set<Slot> u = Set.of(slot(2, EVENING));
        Set<Slot> v = Set.of(slot(1, EVENING), slot(2, EVENING), slot(3, EVENING),
                slot(4, EVENING), slot(5, EVENING));

        assertThat(calculator.scheduleFit(u, v)).isCloseTo(1.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC118 u chưa khai báo lịch, v có 3 khung → 0.5 (cả hai chiều)")
    void tc118_oneSideUndeclaredIsHalf() {
        Set<Slot> v = Set.of(slot(1, EVENING), slot(3, EVENING), slot(5, EVENING));

        assertThat(calculator.scheduleFit(Set.of(), v)).isCloseTo(0.5, within(TOLERANCE));
        assertThat(calculator.scheduleFit(v, Set.of())).isCloseTo(0.5, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC118 cả hai chưa khai báo lịch → 0.5, không NaN")
    void tc118_bothUndeclaredIsHalf() {
        // min(0, 0) = 0: không có nhánh riêng thì 0.0 / 0 ra NaN.
        assertThat(calculator.scheduleFit(Set.of(), Set.of())).isCloseTo(0.5, within(TOLERANCE));
    }

    @Test
    @DisplayName("Kế hoạch bước 4: cả hai đã khai báo, không trùng khung nào → 0.0 (công thức tự cho ra)")
    void plan_declaredButNoOverlapIsZero() {
        Set<Slot> u = Set.of(slot(1, EVENING));
        Set<Slot> v = Set.of(slot(1, MORNING));

        assertThat(calculator.scheduleFit(u, v)).isEqualTo(0.0);
    }

    // ---------- ModeFit ----------

    /** 9 cặp không thứ tự của spec 4.3 (bỏ ONLINE + OFFLINE). null = chưa khai báo. */
    static Stream<Arguments> modePairs() {
        return Stream.of(
                Arguments.of(ONLINE, ONLINE, 1.0),
                Arguments.of(OFFLINE, OFFLINE, 1.0),
                Arguments.of(ANY, ANY, 1.0),
                Arguments.of(ONLINE, ANY, 1.0),
                Arguments.of(OFFLINE, ANY, 1.0),
                Arguments.of(ONLINE, null, 0.5),
                Arguments.of(OFFLINE, null, 0.5),
                // Dễ sai nhất: ANY hợp với mọi thứ, nhưng bên kia chưa khai báo thì vẫn 0.5 (spec 4.3).
                Arguments.of(ANY, null, 0.5),
                Arguments.of(null, null, 0.5));
    }

    @ParameterizedTest(name = "{0} + {1} → {2}")
    @MethodSource("modePairs")
    @DisplayName("TC119 ModeFit theo chiều u → v")
    void tc119_modeFitForward(LearningMode u, LearningMode v, double expected) {
        assertThat(calculator.modeFit(u, v)).isCloseTo(expected, within(TOLERANCE));
    }

    @ParameterizedTest(name = "{1} + {0} → {2}")
    @MethodSource("modePairs")
    @DisplayName("TC119 ModeFit đổi chiều v → u cho cùng kết quả")
    void tc119_modeFitReversed(LearningMode u, LearningMode v, double expected) {
        assertThat(calculator.modeFit(v, u)).isCloseTo(expected, within(TOLERANCE));
    }

    @Test
    @DisplayName("Kế hoạch bước 4: ONLINE + OFFLINE (lẽ ra đã bị lọc cứng loại) → ném IllegalArgumentException, cả hai chiều")
    void plan_conflictingModesThrow() {
        assertThatThrownBy(() -> calculator.modeFit(ONLINE, OFFLINE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.modeFit(OFFLINE, ONLINE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
