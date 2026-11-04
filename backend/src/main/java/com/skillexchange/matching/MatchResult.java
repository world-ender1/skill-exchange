package com.skillexchange.matching;

import java.util.List;
import java.util.Objects;

/**
 * Kết quả matching cho một ứng viên (spec 4.4, 4.5; TK 4.5).
 *
 * - score: MutualScore nếu TWO_WAY, D(u,v) nếu ONE_WAY. Chưa làm tròn (xếp hạng dùng điểm gốc).
 * - forward: u học từ ứng viên. backward: ứng viên học từ u.
 * - backward và theyLearn là null khi ONE_WAY; response bỏ hẳn hai key này (D49).
 */
public record MatchResult(
        long userId,
        MatchType type,
        double score,
        ScoreBreakdown forward,
        ScoreBreakdown backward,
        List<SkillMatch> youLearn,
        List<SkillMatch> theyLearn,
        List<Slot> commonSlots) {

    public MatchResult {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(forward, "forward");
        youLearn = List.copyOf(youLearn);
        theyLearn = theyLearn == null ? null : List.copyOf(theyLearn);
        commonSlots = List.copyOf(commonSlots);
    }
}
