package com.skillexchange.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit test cho thứ tự xếp hạng duy nhất (D36): điểm cao trước; chênh dưới 1e-9 thì coi là bằng
 * và xếp theo userId tăng dần.
 */
class RankingComparatorTest {

    private final RankingComparator comparator = new RankingComparator();

    /** Kết quả chỉ có userId và điểm; các phần khác không ảnh hưởng thứ tự. */
    private static MatchResult result(long userId, double score) {
        ScoreBreakdown any = new ScoreBreakdown(score, 0, 0, 0, 0, score);
        return new MatchResult(userId, MatchType.ONE_WAY, score, any, null, List.of(), null, List.of());
    }

    private List<Long> sortedIds(MatchResult... results) {
        List<MatchResult> list = new ArrayList<>(List.of(results));
        list.sort(comparator);
        return list.stream().map(MatchResult::userId).toList();
    }

    @Test
    @DisplayName("TC138 điểm 0.5 và 0.5 + 1e-12 (dưới ngưỡng) với userId 9 và 4 → thứ tự 4, 9")
    void tc138_tinyDifferenceFallsBackToUserId() {
        // userId 9 có điểm nhỉnh hơn 1e-12 và đứng trước trong danh sách: so điểm tuyệt đối thì 9 đứng đầu.
        assertThat(sortedIds(result(9, 0.5 + 1e-12), result(4, 0.5))).containsExactly(4L, 9L);
    }

    @Test
    @DisplayName("TC139 điểm 0.5 (userId 4) và 0.5 + 1e-6 (userId 9), chênh trên ngưỡng → thứ tự 9, 4")
    void tc139_realDifferenceWins() {
        assertThat(sortedIds(result(4, 0.5), result(9, 0.5 + 1e-6))).containsExactly(9L, 4L);
    }

    @Test
    @DisplayName("D36 điểm cao đứng trước, không phụ thuộc thứ tự ban đầu")
    void d36_higherScoreFirst() {
        assertThat(sortedIds(result(1, 0.2), result(2, 0.9), result(3, 0.5))).containsExactly(2L, 3L, 1L);
    }

    @Test
    @DisplayName("D36 bằng điểm tuyệt đối thì userId tăng dần")
    void d36_exactTieByUserId() {
        assertThat(sortedIds(result(7, 0.6), result(3, 0.6), result(5, 0.6))).containsExactly(3L, 5L, 7L);
    }

    @Test
    @DisplayName("D36 compare đối xứng dấu: compare(a, b) = −compare(b, a)")
    void d36_compareIsAntisymmetric() {
        MatchResult low = result(1, 0.3);
        MatchResult high = result(2, 0.8);

        assertThat(comparator.compare(high, low)).isNegative();
        assertThat(comparator.compare(low, high)).isPositive();
        assertThat(comparator.compare(low, low)).isZero();
    }
}
