package com.skillexchange.matching;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Lõi matching (D34): lọc cứng, chấm điểm hai chiều, phân loại TWO_WAY / ONE_WAY, xếp hạng.
 * Không dùng Spring, không gọi Repository: MatchingService (API) và module evaluation (tuần 5)
 * cùng gọi lớp này với dữ liệu của mình.
 */
public class MatchingEngine {

    /** youLearn / theyLearn: tên kỹ năng muốn học, rồi skillId (D46b, D41). */
    private static final Comparator<SkillMatch> BY_WANTED_SKILL =
            Comparator.comparing((SkillMatch m) -> m.want().skillName())
                    .thenComparingLong(m -> m.want().skillId());

    /** commonSlots: ngày, rồi buổi theo thứ tự khai báo của enum Period (Sáng → Chiều → Tối), như D41. */
    private static final Comparator<Slot> BY_DAY_THEN_PERIOD =
            Comparator.comparingInt(Slot::dayOfWeek).thenComparing(Slot::period);

    private final ScoreCalculator scores;
    private final HardFilter hardFilter;
    private final MatchingWeights weights;
    /** Một comparator duy nhất cho cả hai nhóm (D36). */
    private final RankingComparator ranking = new RankingComparator();

    public MatchingEngine(SkillSimilarity similarity, MatchingWeights weights) {
        this.scores = new ScoreCalculator(similarity);
        this.hardFilter = new HardFilter(scores);
        this.weights = weights;
    }

    /** Bước 7. Gợi ý cho caller từ danh sách ứng viên; caller có trong danh sách cũng không sao (bị lọc). */
    public MatchingResult match(MatchProfile caller, List<MatchProfile> candidates) {
        List<MatchResult> twoWay = new ArrayList<>();
        List<MatchResult> oneWay = new ArrayList<>();

        for (MatchProfile candidate : hardFilter.filter(caller, candidates)) {
            MatchResult result = score(caller, candidate);
            if (result.type() == MatchType.TWO_WAY) {
                twoWay.add(result);
            } else {
                oneWay.add(result);
            }
        }

        // Hai nhóm xếp riêng (spec 4.5): nhóm một chiều không chen vào nhóm hai chiều dù điểm cao hơn.
        twoWay.sort(ranking);
        oneWay.sort(ranking);
        return new MatchingResult(twoWay, oneWay);
    }

    /** Chấm một ứng viên đã qua lọc cứng, nên chiều caller học từ candidate chắc chắn có SkillFit > 0. */
    private MatchResult score(MatchProfile caller, MatchProfile candidate) {
        ScoreBreakdown forward = scores.breakdown(caller, candidate, weights);
        List<SkillMatch> youLearn = explain(caller.learn(), candidate.teach());
        List<Slot> commonSlots = commonSlots(caller, candidate);

        // Hai chiều khi ứng viên cũng học được gì đó từ caller (spec 4.5). Các điều kiện lọc khác đối xứng nên không kiểm lại.
        boolean twoWay = scores.skillFit(candidate.learn(), caller.teach()) > 0;
        if (!twoWay) {
            // Mentoring một chiều: điểm xếp hạng là D(u, v); không có chiều ngược (D49).
            return new MatchResult(candidate.userId(), MatchType.ONE_WAY, forward.d(),
                    forward, null, youLearn, null, commonSlots);
        }

        ScoreBreakdown backward = scores.breakdown(candidate, caller, weights);
        List<SkillMatch> theyLearn = explain(candidate.learn(), caller.teach());
        // Trung bình nhân: một chiều yếu kéo cả cặp xuống, không bị chiều mạnh bù lại (phụ lục A.1).
        double mutualScore = Math.sqrt(forward.d() * backward.d());
        return new MatchResult(candidate.userId(), MatchType.TWO_WAY, mutualScore,
                forward, backward, youLearn, theyLearn, commonSlots);
    }

    /** Mỗi kỹ năng muốn học có Sim > 0 một dòng, kèm kỹ năng dạy đã chọn (D46a, dùng lại bestTeachFor của LevelFit). */
    private List<SkillMatch> explain(List<LearnItem> wants, List<TeachItem> teaches) {
        List<SkillMatch> matches = new ArrayList<>();
        for (LearnItem want : wants) {
            Optional<SkillMatch> match = scores.bestTeachFor(want, teaches);
            match.ifPresent(matches::add);
        }
        matches.sort(BY_WANTED_SKILL);
        return matches;
    }

    private static List<Slot> commonSlots(MatchProfile a, MatchProfile b) {
        List<Slot> common = new ArrayList<>();
        for (Slot slot : a.slots()) {
            if (b.slots().contains(slot)) {
                common.add(slot);
            }
        }
        // Set không có thứ tự: phải sắp lại thì response mới ổn định.
        common.sort(BY_DAY_THEN_PERIOD);
        return common;
    }
}
