package com.skillexchange.matching;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.skillexchange.user.AvailabilitySlotResponse;
import com.skillexchange.user.LearningMode;

/**
 * Một ứng viên trong /api/matches (TK 4.5). Không có email, phone, contactInfo (NFR-02).
 *
 * D49: kết quả ONE_WAY không có KEY backward, theyLearn (không phải key mang giá trị null),
 * nên NON_NULL chỉ đặt trên hai trường này. Các trường khác vẫn có key khi null, ví dụ ratingAverage.
 */
public record MatchItemResponse(
        long userId,
        String fullName,
        MatchType type,
        double score,
        ScoreResponse forward,
        @JsonInclude(JsonInclude.Include.NON_NULL) ScoreResponse backward,
        LearningMode learningMode,
        Double ratingAverage,
        int reviewCount,
        boolean hasPendingRequest,
        List<SkillMatchResponse> youLearn,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<SkillMatchResponse> theyLearn,
        List<AvailabilitySlotResponse> commonSlots) {
}
