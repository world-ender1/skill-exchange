package com.skillexchange.matching;

import static com.skillexchange.matching.SessionStatus.CANCELLED;
import static com.skillexchange.matching.SessionStatus.COMPLETED;
import static com.skillexchange.matching.SessionStatus.IN_PROGRESS;
import static com.skillexchange.matching.SessionStatus.SCHEDULED;
import static com.skillexchange.matching.SessionStatus.WAITING_CONFIRMATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit test cho Reputation (spec 4.3, D43): 0.7 × RatingNorm + 0.3 × CompletionRate.
 * RatingNorm = (trung bình − 1) / 4; CompletionRate = số phiên COMPLETED / số phiên đã bắt đầu và đã kết thúc.
 * Thiếu dữ liệu ở thành phần nào thì thành phần đó = 0.5.
 */
class ReputationCalculatorTest {

    private static final double TOLERANCE = 1e-9;

    private static SessionOutcome started(SessionStatus status) {
        return new SessionOutcome(status, true);
    }

    private static SessionOutcome notStarted(SessionStatus status) {
        return new SessionOutcome(status, false);
    }

    @Test
    @DisplayName("TC120 người mới: không đánh giá, không phiên → 0.5")
    void tc120_newcomerIsHalf() {
        double reputation = ReputationCalculator.compute(List.of(), List.of());

        assertThat(Double.isNaN(reputation)).isFalse();
        assertThat(reputation).isCloseTo(0.5, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC121 một đánh giá 5 sao, không phiên → 0.7 × 1.0 + 0.3 × 0.5 = 0.85")
    void tc121_onlyOneFiveStarRating() {
        double reputation = ReputationCalculator.compute(List.of(5), List.of());

        assertThat(reputation).isCloseTo(0.85, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC122 không đánh giá; 3 COMPLETED, 1 CANCELLED đã bắt đầu → 0.35 + 0.3 × 0.75 = 0.575")
    void tc122_onlySessions() {
        List<SessionOutcome> sessions = List.of(
                started(COMPLETED), started(COMPLETED), started(COMPLETED), started(CANCELLED));

        double reputation = ReputationCalculator.compute(List.of(), sessions);

        assertThat(reputation).isCloseTo(0.575, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC123 ví dụ Bình: đánh giá 5, 4; mẫu số chỉ gồm COMPLETED và CANCELLED đã bắt đầu → 0.7625")
    void tc123_binhExampleOnlyFinishedSessionsCount() {
        List<SessionOutcome> sessions = List.of(
                started(COMPLETED),
                started(CANCELLED),
                started(IN_PROGRESS),
                started(WAITING_CONFIRMATION),
                notStarted(SCHEDULED),
                notStarted(CANCELLED));

        double reputation = ReputationCalculator.compute(List.of(5, 4), sessions);

        // Tính cả phiên đang diễn ra vào mẫu số (quy tắc cũ) thì CompletionRate = 1/3, ra 0.7125.
        assertThat(reputation).isCloseTo(0.7 * 0.875 + 0.3 * 0.5, within(TOLERANCE));
        assertThat(reputation).isCloseTo(0.7625, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC124 không đánh giá; chỉ phiên chưa kết thúc hoặc hủy trước khi bắt đầu → mẫu số 0, 0.5, không NaN")
    void tc124_zeroDenominatorIsHalfNotNaN() {
        List<SessionOutcome> sessions = List.of(
                notStarted(SCHEDULED),
                started(IN_PROGRESS),
                started(WAITING_CONFIRMATION),
                notStarted(CANCELLED));

        double reputation = ReputationCalculator.compute(List.of(), sessions);

        assertThat(Double.isNaN(reputation)).isFalse();
        assertThat(reputation).isCloseTo(0.5, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC125 ví dụ A.3: đánh giá 5, 5, 4, 4, 3; 4 COMPLETED, 1 CANCELLED đã bắt đầu → 0.8")
    void tc125_appendixA3Example() {
        List<SessionOutcome> sessions = List.of(
                started(COMPLETED), started(COMPLETED), started(COMPLETED), started(COMPLETED),
                started(CANCELLED));

        double reputation = ReputationCalculator.compute(List.of(5, 5, 4, 4, 3), sessions);

        // Code ra 0.7999999999999999: so bằng dung sai, không dùng isEqualTo (D36).
        assertThat(reputation).isCloseTo(0.8, within(TOLERANCE));
    }

    @Test
    @DisplayName("Spec 4.3 thang 1–5: một đánh giá 1 sao → RatingNorm 0, Reputation = 0.15")
    void spec43_oneStarIsZeroRatingNorm() {
        // Chia cho 5 hoặc quên trừ 1 thì RatingNorm của 1 sao không ra 0: test này bắt lỗi đó.
        double reputation = ReputationCalculator.compute(List.of(1), List.of());

        assertThat(reputation).isCloseTo(0.15, within(TOLERANCE));
    }

    /** Mỗi trạng thái phiên đứng riêng (một phiên, không đánh giá): RatingNorm = 0.5 nên phần đầu luôn là 0.35. */
    static Stream<Arguments> singleSessions() {
        return Stream.of(
                Arguments.of(started(COMPLETED), 0.35 + 0.3 * 1.0),
                Arguments.of(started(CANCELLED), 0.35 + 0.3 * 0.0),
                // Ba trạng thái dưới không vào mẫu số → CompletionRate = 0.5 → 0.5.
                Arguments.of(notStarted(CANCELLED), 0.5),
                Arguments.of(notStarted(SCHEDULED), 0.5),
                Arguments.of(started(IN_PROGRESS), 0.5),
                Arguments.of(started(WAITING_CONFIRMATION), 0.5));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("singleSessions")
    @DisplayName("D43 mỗi trạng thái phiên tính đúng vào mẫu số CompletionRate")
    void d43_eachSessionStatusAlone(SessionOutcome session, double expected) {
        double reputation = ReputationCalculator.compute(List.of(), List.of(session));

        assertThat(Double.isNaN(reputation)).isFalse();
        assertThat(reputation).isCloseTo(expected, within(TOLERANCE));
    }
}
