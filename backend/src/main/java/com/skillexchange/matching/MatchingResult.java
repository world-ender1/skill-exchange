package com.skillexchange.matching;

import java.util.List;

/**
 * Toàn bộ gợi ý cho một người dùng: hai nhóm xếp hạng riêng (spec 4.5),
 * mỗi nhóm đã sắp xếp theo RankingComparator (D36).
 */
public record MatchingResult(List<MatchResult> twoWay, List<MatchResult> oneWay) {

    public MatchingResult {
        twoWay = List.copyOf(twoWay);
        oneWay = List.copyOf(oneWay);
    }
}
