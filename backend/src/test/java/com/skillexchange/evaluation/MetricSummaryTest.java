package com.skillexchange.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Chỉ số theo từng người và gộp trên mọi người dùng: TC227, TC229, TC230 (spec 5.3; D58–D60).
 *
 * Mỗi người dựng bằng UserMetrics.of(mã, danh sách rel, tập lý tưởng).
 */
class MetricSummaryTest {

    private static final double TOLERANCE = 1e-9;

    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    // Ba người của TC229. Dựng trong hàm, không dùng hằng static: lỗi khi dựng sẽ báo ở từng test,
    // thay vì thành ExceptionInInitializerError của cả lớp test.
    private static UserMetrics userA() {
        return UserMetrics.of(1, List.of(1, 2), List.of(2, 1));
    }

    private static UserMetrics userB() {
        return UserMetrics.of(2, List.of(1, 1, 1), List.of(1, 1, 1));
    }

    private static UserMetrics userC() {
        return UserMetrics.of(3, List.of(), List.of());
    }

    /** NDCG@5 của A: DCG của [1, 2] chia DCG của [2, 1]. */
    private static final double NDCG_A = (1 + 3 / log2(3)) / (3 + 1 / log2(3));

    // ---------- Bộ ca ----------

    @Test
    @DisplayName("TC227 hai người có IDCG@5 = 0 ([0, 0] với {0, 0}; rỗng với rỗng): đếm 2 ca, NDCG@5 bản (1) = 0, không NaN")
    void tc227_zeroIdcgCasesAreCounted() {
        UserMetrics zeros = UserMetrics.of(1, List.of(0, 0), List.of(0, 0));
        UserMetrics empty = UserMetrics.of(2, List.of(), List.of());

        assertThat(zeros.idcgZero()).isTrue();
        assertThat(empty.idcgZero()).isTrue();

        MetricSummary summary = MetricSummary.of(List.of(zeros, empty));

        assertThat(summary.idcgZeroCount()).isEqualTo(2);
        assertThat(summary.ndcgAt5AllUsers()).isNotNaN().isCloseTo(0.0, within(TOLERANCE));
        assertThat(summary.precisionAt5()).isCloseTo(0.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC229 A {2, 1} xếp [1, 2], B {1, 1, 1}, C rỗng: bản (1) = (nA + 1 + 0) / 3, bản (2) = nA, loại 2 người")
    void tc229_twoAverages() {
        UserMetrics a = userA();
        UserMetrics b = userB();
        UserMetrics c = userC();
        assertThat(a.ndcgAt5()).isCloseTo(NDCG_A, within(TOLERANCE));
        assertThat(NDCG_A).isCloseTo(0.7967, within(1e-4));
        assertThat(b.ndcgAt5()).isCloseTo(1.0, within(TOLERANCE));
        assertThat(c.ndcgAt5()).isCloseTo(0.0, within(TOLERANCE));

        MetricSummary summary = MetricSummary.of(List.of(a, b, c));

        assertThat(summary.userCount()).isEqualTo(3);
        assertThat(summary.ndcgAt5AllUsers()).isCloseTo((NDCG_A + 1 + 0) / 3, within(TOLERANCE));
        // B chỉ có một mức nhãn: thứ tự nào cũng "lý tưởng", NDCG = 1 kéo trung bình lên (R24).
        assertThat(summary.ndcgAt5NonDegenerate()).isCloseTo(NDCG_A, within(TOLERANCE));
        assertThat(summary.nonDegenerateCount()).isEqualTo(1);
        assertThat(summary.excludedCount()).isEqualTo(2);
        assertThat(summary.idcgZeroCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC230 bốn người có 0, 1, 5, 6 ứng viên, chỉ người 5 ứng viên có nhãn 2: Coverage 3/4, đủ 5 là 2/4, có nhãn 2 là 1/4")
    void tc230_coverageAndExtraCoverage() {
        List<Integer> five = List.of(1, 1, 1, 1, 2);
        List<Integer> six = List.of(1, 1, 1, 1, 1, 1);
        List<UserMetrics> users = List.of(
                UserMetrics.of(1, List.of(), List.of()),
                UserMetrics.of(2, List.of(1), List.of(1)),
                UserMetrics.of(3, five, five),
                UserMetrics.of(4, six, six));

        assertThat(users).extracting(UserMetrics::candidateCount).containsExactly(0, 1, 5, 6);
        assertThat(users).extracting(UserMetrics::hasLabel2InList).containsExactly(false, false, true, false);

        MetricSummary summary = MetricSummary.of(users);

        assertThat(summary.coverage()).isCloseTo(3.0 / 4, within(TOLERANCE));
        assertThat(summary.atLeastFiveRate()).isCloseTo(2.0 / 4, within(TOLERANCE));
        assertThat(summary.hasLabel2Rate()).isCloseTo(1.0 / 4, within(TOLERANCE));
    }

    // ---------- Phần quyết định chưa có ca ----------

    @Test
    @DisplayName("D58 Precision@5 (hai bản) lấy trung bình trên mọi người, kể cả người bị loại khỏi bản (2)")
    void d58_precisionAveragesOverAllUsers() {
        // A: [1, 2] → 2/5, nghiêm ngặt 1/5. B: [1, 1, 1] → 3/5, nghiêm ngặt 0. C: rỗng → 0.
        MetricSummary summary = MetricSummary.of(List.of(userA(), userB(), userC()));

        // Chỉ tính trên bản (2) (một mình A) thì ra 2/5 và 1/5.
        assertThat(summary.precisionAt5()).isCloseTo((2.0 / 5 + 3.0 / 5 + 0) / 3, within(TOLERANCE));
        assertThat(summary.strictPrecisionAt5()).isCloseTo((1.0 / 5 + 0 + 0) / 3, within(TOLERANCE));
    }

    @Test
    @DisplayName("D59 không còn ai thuộc bản (2) (B, C của TC229): NDCG@5 bản (2) là NaN, số người tính 0; bản (1) vẫn tính")
    void d59_noNonDegenerateUserGivesNaN() {
        MetricSummary summary = MetricSummary.of(List.of(userB(), userC()));

        assertThat(summary.ndcgAt5NonDegenerate()).isNaN();
        assertThat(summary.nonDegenerateCount()).isZero();
        assertThat(summary.excludedCount()).isEqualTo(2);
        assertThat(summary.ndcgAt5AllUsers()).isCloseTo((1.0 + 0) / 2, within(TOLERANCE));
    }

    @Test
    @DisplayName("D59 số mức nhãn của tập lý tưởng: rỗng 0, {1, 1, 1} 1, {1, 0} 2, {2, 1, 0, 0} 3; từ 2 mức trở lên thì thuộc bản (2)")
    void d59_idealLevelCount() {
        UserMetrics none = UserMetrics.of(1, List.of(), List.of());
        UserMetrics one = UserMetrics.of(2, List.of(1), List.of(1, 1, 1));
        UserMetrics zeroAndOne = UserMetrics.of(3, List.of(0, 1), List.of(1, 0));
        UserMetrics three = UserMetrics.of(4, List.of(2), List.of(2, 1, 0, 0));

        assertThat(List.of(none, one, zeroAndOne, three))
                .extracting(UserMetrics::idealLevelCount).containsExactly(0, 1, 2, 3);
        // Nhãn 0 cũng là một mức: {1, 0} thì xếp nhãn 1 lên trước mới đúng.
        assertThat(List.of(none, one, zeroAndOne, three))
                .extracting(UserMetrics::nonDegenerate).containsExactly(false, false, true, true);
    }

    @Test
    @DisplayName("D60 nhãn 2 chỉ ở vị trí thứ 6: vẫn tính là có nhãn 2 trong danh sách, nhưng Precision@5 nghiêm ngặt = 0")
    void d60_label2BeyondTopFiveCounts() {
        List<Integer> list = List.of(1, 1, 1, 1, 1, 2);
        UserMetrics user = UserMetrics.of(1, list, list);

        assertThat(user.hasLabel2InList()).isTrue();
        assertThat(user.strictPrecisionAt5()).isCloseTo(0.0, within(TOLERANCE));
        assertThat(MetricSummary.of(List.of(user)).hasLabel2Rate()).isCloseTo(1.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("D60 danh sách người dùng rỗng bị từ chối: không có mẫu số")
    void d60_emptyUserListIsRejected() {
        assertThatThrownBy(() -> MetricSummary.of(List.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
