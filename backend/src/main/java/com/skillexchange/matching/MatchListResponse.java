package com.skillexchange.matching;

import java.util.List;

/**
 * Response của /api/matches (TK 4.5): total là số ứng viên của cả nhóm, items là top limit.
 * needsLearnSkills = true khi người gọi chưa khai kỹ năng muốn học: giao diện dẫn sang S04.
 */
public record MatchListResponse(
        MatchType type,
        boolean needsLearnSkills,
        int total,
        List<MatchItemResponse> items) {

    static MatchListResponse needsLearnSkills(MatchType type) {
        return new MatchListResponse(type, true, 0, List.of());
    }
}
