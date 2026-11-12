package com.skillexchange.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.skillexchange.matching.MatchProfile;
import com.skillexchange.matching.MatchResult;
import com.skillexchange.matching.MatchType;
import com.skillexchange.matching.MatchingResult;
import com.skillexchange.matching.MatchingWeights;
import com.skillexchange.matching.RankingComparator;
import com.skillexchange.matching.ScoreBreakdown;

/**
 * Tính lại điểm với một công thức ScheduleFit khác, từ đầu ra của lõi (D68). Lõi không bị sửa:
 * lớp này giữ nguyên bốn thành phần còn lại, lọc cứng và phân loại hai nhóm mà lõi đã làm,
 * chỉ thay ScheduleFit, tính lại D và MutualScore, rồi xếp lại từng nhóm.
 */
public final class Rescorer {

    private final MatchingWeights weights;
    private final ScheduleFitFormula formula;
    /** Cùng comparator với lõi (D36): khác nhau chỉ ở điểm, không ở cách xếp. */
    private final RankingComparator ranking = new RankingComparator();

    /** weights phải là bộ đã dùng để tạo MatchingEngine sinh ra kết quả lõi. */
    public Rescorer(MatchingWeights weights, ScheduleFitFormula formula) {
        this.weights = Objects.requireNonNull(weights, "weights");
        this.formula = Objects.requireNonNull(formula, "formula");
    }

    /**
     * core là kết quả lõi cho caller trên chính danh sách profiles; profiles dùng để lấy lịch tự khai của ứng viên.
     * Trả hai nhóm mới, mỗi nhóm đã xếp lại; core không bị đổi (record bất biến).
     */
    public MatchingResult rescore(MatchProfile caller, List<MatchProfile> profiles, MatchingResult core) {
        // Map chỉ để tra theo mã, không duyệt, nên thứ tự của HashMap không ảnh hưởng tái lập (D64).
        Map<Long, MatchProfile> byId = new HashMap<>();
        for (MatchProfile profile : profiles) {
            byId.put(profile.userId(), profile);
        }

        List<MatchResult> twoWay = new ArrayList<>();
        for (MatchResult result : core.twoWay()) {
            twoWay.add(rescoreOne(caller, candidateOf(byId, result), result));
        }
        List<MatchResult> oneWay = new ArrayList<>();
        for (MatchResult result : core.oneWay()) {
            oneWay.add(rescoreOne(caller, candidateOf(byId, result), result));
        }

        // Điểm đổi thì thứ tự có thể đổi: phải xếp lại, nếu không danh sách vẫn là thứ tự của MIN (TC243).
        // Xếp riêng từng nhóm như lõi (spec 4.5): nhóm một chiều không chen vào nhóm hai chiều.
        twoWay.sort(ranking);
        oneWay.sort(ranking);
        return new MatchingResult(twoWay, oneWay);
    }

    private static MatchProfile candidateOf(Map<Long, MatchProfile> byId, MatchResult result) {
        MatchProfile candidate = byId.get(result.userId());
        if (candidate == null) {
            throw new IllegalArgumentException(
                    "Không có hồ sơ của ứng viên " + result.userId() + ": core phải tính trên chính danh sách profiles");
        }
        return candidate;
    }

    private MatchResult rescoreOne(MatchProfile caller, MatchProfile candidate, MatchResult result) {
        // Chiều xuôi: caller học từ candidate.
        double forwardSchedule = formula.scheduleFit(caller.slots(), candidate.slots());
        ScoreBreakdown forward = withSchedule(result.forward(), forwardSchedule);

        if (result.type() == MatchType.ONE_WAY) {
            // Một chiều: điểm xếp hạng là D(u, v), không có chiều ngược (D49), như lõi.
            return new MatchResult(result.userId(), MatchType.ONE_WAY, forward.d(),
                    forward, null, result.youLearn(), null, result.commonSlots());
        }

        // Chiều ngược: candidate học từ caller. Ba công thức đều đối xứng, nhưng vẫn truyền đúng vai
        // để code không sai âm thầm nếu sau này thêm một công thức không đối xứng.
        double backwardSchedule = formula.scheduleFit(candidate.slots(), caller.slots());
        ScoreBreakdown backward = withSchedule(result.backward(), backwardSchedule);
        // Trung bình nhân như lõi (phụ lục A.1).
        double mutualScore = Math.sqrt(forward.d() * backward.d());
        return new MatchResult(result.userId(), MatchType.TWO_WAY, mutualScore,
                forward, backward, result.youLearn(), result.theyLearn(), result.commonSlots());
    }

    /**
     * Giữ bốn thành phần của lõi, thay ScheduleFit, tính lại D. Cộng đúng thứ tự của ScoreCalculator.breakdown:
     * phép cộng số thực không kết hợp, nên cùng thứ tự thì MIN cho ra đúng từng bit điểm của lõi (TC242 a).
     */
    private ScoreBreakdown withSchedule(ScoreBreakdown core, double schedule) {
        double d = weights.skillFit() * core.skillFit()
                + weights.levelFit() * core.levelFit()
                + weights.scheduleFit() * schedule
                + weights.modeFit() * core.modeFit()
                + weights.reputation() * core.reputation();
        return new ScoreBreakdown(core.skillFit(), core.levelFit(), schedule, core.modeFit(), core.reputation(), d);
    }
}
