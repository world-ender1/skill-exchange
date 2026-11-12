package com.skillexchange.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import com.skillexchange.matching.MatchResult;
import com.skillexchange.matching.MatchingEngine;
import com.skillexchange.matching.MatchingResult;
import com.skillexchange.matching.MatchingWeights;
import com.skillexchange.matching.RankingComparator;
import com.skillexchange.matching.SkillRelation;
import com.skillexchange.matching.SkillSimilarity;

/**
 * Dựng các phương án B0, B1, R, P từ lõi matching, không có code xếp hạng riêng (D56, D65).
 *
 * Mỗi hàm tạo MatchingEngine một lần rồi trả một Ranker dùng lại cho mọi người dùng
 * (kế hoạch tuần 5, bước 6). Bước 7 gọi mỗi hàm một lần cho mỗi cấu hình.
 */
public final class Rankers {

    /** B0, B1 chỉ nhìn kỹ năng: với bộ này D = SkillFit (D65). */
    static final MatchingWeights SKILL_ONLY = new MatchingWeights(1, 0, 0, 0, 0);

    /** Luồng số ngẫu nhiên của R: new Random(seed + 2_000_000 + userId) cho từng người (D64). */
    private static final long RANDOM_STREAM_OFFSET = 2_000_000;

    private final List<SkillRelation> relations;

    /** relations: các cặp kỹ năng liên quan, thường là SkillCatalog.V2.relations(). */
    public Rankers(List<SkillRelation> relations) {
        this.relations = List.copyOf(relations);
    }

    /**
     * B0: không có quan hệ liên quan nào, nên chỉ giữ ứng viên trùng chính xác và Sim chỉ là 0 hoặc 1.
     * SkillFit × |W_u| = số kỹ năng trùng, mà |W_u| như nhau với mọi ứng viên của u,
     * nên xếp theo SkillFit cũng là xếp theo số kỹ năng trùng (spec 5.2).
     */
    public Ranker b0() {
        MatchingEngine engine = new MatchingEngine(new SkillSimilarity(List.of()), SKILL_ONLY);
        return (caller, profiles) -> bySkillFit(engine.match(caller, profiles));
    }

    /** B1: quan hệ liên quan thật, xếp theo SkillFit. Tập ứng viên bằng tập của P, vì lọc cứng không phụ thuộc trọng số. */
    public Ranker b1() {
        MatchingEngine engine = new MatchingEngine(new SkillSimilarity(relations), SKILL_ONLY);
        return (caller, profiles) -> bySkillFit(engine.match(caller, profiles));
    }

    /** R: tập ứng viên của B1, xếp theo mã rồi xáo theo seed của bộ dữ liệu (D64, D65). */
    public Ranker random(long seed) {
        Ranker b1 = b1();
        return (caller, profiles) -> {
            List<Long> ids = new ArrayList<>(b1.rank(caller, profiles));
            // Xếp theo mã trước khi xáo: kết quả chỉ phụ thuộc tập ứng viên và seed, không phụ thuộc thứ tự của B1.
            Collections.sort(ids);
            // Random mới cho từng người gọi: kết quả của một người không đổi theo việc ai được gọi trước,
            // nên lượt làm nóng của D61 hay chạy lại cũng không làm đổi danh sách.
            Collections.shuffle(ids, new Random(seed + RANDOM_STREAM_OFFSET + caller.userId()));
            return List.copyOf(ids);
        };
    }

    /**
     * P với một bộ trọng số và một công thức ScheduleFit: nhóm hai chiều rồi nhóm một chiều (D56).
     * MIN là công thức của lõi nên trả đúng kết quả lõi, không qua Rescorer: P đo đúng thuật toán đang chạy,
     * và latency của P là của lõi (D61). Công thức khác thì tính lại điểm bằng Rescorer (D68).
     */
    public Ranker p(MatchingWeights weights, ScheduleFitFormula formula) {
        Objects.requireNonNull(formula, "formula");
        MatchingEngine engine = new MatchingEngine(new SkillSimilarity(relations), weights);
        if (formula == ScheduleFitFormula.MIN) {
            return (caller, profiles) -> twoWayThenOneWay(engine.match(caller, profiles));
        }
        Rescorer rescorer = new Rescorer(weights, formula);
        return (caller, profiles) -> {
            MatchingResult core = engine.match(caller, profiles);
            return twoWayThenOneWay(rescorer.rescore(caller, profiles, core));
        };
    }

    private static List<Long> twoWayThenOneWay(MatchingResult result) {
        List<Long> ids = new ArrayList<>();
        for (MatchResult r : result.twoWay()) {
            ids.add(r.userId());
        }
        for (MatchResult r : result.oneWay()) {
            ids.add(r.userId());
        }
        return List.copyOf(ids);
    }

    /**
     * B0, B1: gộp hai nhóm của lõi rồi xếp chung theo SkillFit chiều u học từ v (D65).
     * Lõi đặt score của nhóm hai chiều là MutualScore, nên dựng lại MatchResult với score = forward().d(),
     * mà với SKILL_ONLY thì forward().d() = SkillFit.
     */
    private static List<Long> bySkillFit(MatchingResult result) {
        List<MatchResult> merged = new ArrayList<>();
        for (MatchResult r : result.twoWay()) {
            merged.add(withScore(r, r.forward().d()));
        }
        for (MatchResult r : result.oneWay()) {
            merged.add(withScore(r, r.forward().d()));
        }
        // Cùng comparator với lõi (D36): bằng điểm (chênh dưới 1e-9) thì mã nhỏ trước.
        merged.sort(new RankingComparator());

        List<Long> ids = new ArrayList<>();
        for (MatchResult r : merged) {
            ids.add(r.userId());
        }
        return List.copyOf(ids);
    }

    private static MatchResult withScore(MatchResult r, double score) {
        return new MatchResult(r.userId(), r.type(), score, r.forward(), r.backward(),
                r.youLearn(), r.theyLearn(), r.commonSlots());
    }
}
