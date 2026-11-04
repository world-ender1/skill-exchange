package com.skillexchange.matching;

import java.util.List;

/**
 * Reputation(v) = 0.7 * RatingNorm + 0.3 * CompletionRate (spec 4.3), tính từ dữ liệu thô (D43).
 * Hàm thuần, không giữ trạng thái, nên dùng phương thức static.
 */
public final class ReputationCalculator {

    private ReputationCalculator() {
    }

    /** Bước 5. ratings: các điểm 1–5 người đó nhận được; sessions: các phiên người đó đã tham gia. */
    public static double compute(List<Integer> ratings, List<SessionOutcome> sessions) {
        throw new UnsupportedOperationException("Bước 5");
    }
}
