package com.skillexchange.matching;

import java.util.List;

/**
 * Lõi matching (D34): lọc cứng, chấm điểm hai chiều, phân loại TWO_WAY / ONE_WAY, xếp hạng.
 * Không dùng Spring, không gọi Repository: MatchingService (API) và module evaluation (tuần 5)
 * cùng gọi lớp này với dữ liệu của mình.
 */
public class MatchingEngine {

    private final ScoreCalculator scores;
    private final HardFilter hardFilter;
    private final MatchingWeights weights;

    public MatchingEngine(SkillSimilarity similarity, MatchingWeights weights) {
        this.scores = new ScoreCalculator(similarity);
        this.hardFilter = new HardFilter(scores);
        this.weights = weights;
    }

    /** Bước 7. Gợi ý cho caller từ danh sách ứng viên; caller có trong danh sách cũng không sao (bị lọc). */
    public MatchingResult match(MatchProfile caller, List<MatchProfile> candidates) {
        throw new UnsupportedOperationException("Bước 7");
    }
}
