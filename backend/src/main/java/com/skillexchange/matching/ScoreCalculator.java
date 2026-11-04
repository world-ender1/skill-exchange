package com.skillexchange.matching;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.skillexchange.user.LearningMode;

/**
 * Các thành phần điểm của spec 4.3 và điểm tổng D(u,v) của spec 4.4.
 * Mỗi hàm chỉ nhận phần dữ liệu nó cần (danh sách kỹ năng, tập khung giờ...) để unit test viết gọn.
 */
public class ScoreCalculator {

    private final SkillSimilarity similarity;

    public ScoreCalculator(SkillSimilarity similarity) {
        this.similarity = similarity;
    }

    /** Bước 2. Trung bình Sim tốt nhất cho từng kỹ năng muốn học; wants rỗng thì 0.0. */
    public double skillFit(List<LearnItem> wants, List<TeachItem> teaches) {
        // W_u rỗng: không có gì để trung bình. Trả 0 trước khi chia, vì 0.0 / 0 trong Java ra NaN chứ không ném lỗi.
        if (wants.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (LearnItem want : wants) {
            total += bestSim(want, teaches);
        }
        return total / wants.size();
    }

    /** max_{t ∈ T_v} Sim(s, t); không dạy gì thì 0.0. */
    private double bestSim(LearnItem want, List<TeachItem> teaches) {
        double best = 0.0;
        for (TeachItem teach : teaches) {
            best = Math.max(best, similarity.sim(want.skillId(), teach.skillId()));
        }
        return best;
    }

    /**
     * Bước 3. Kỹ năng dạy được chọn cho một kỹ năng muốn học (D46a): Sim cao nhất,
     * bằng thì trình độ cao hơn, bằng nữa thì skillId nhỏ hơn. Không có kỹ năng nào Sim > 0 thì rỗng.
     */
    public Optional<SkillMatch> bestTeachFor(LearnItem want, List<TeachItem> teaches) {
        SkillMatch best = null;
        for (TeachItem teach : teaches) {
            double sim = similarity.sim(want.skillId(), teach.skillId());
            if (sim <= 0.0) {
                continue;
            }
            SkillMatch candidate = new SkillMatch(want, teach, sim);
            if (best == null || isBetter(candidate, best)) {
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }

    /** Thứ tự chọn của D46(a): Sim cao hơn → trình độ cao hơn → skillId nhỏ hơn. */
    private static boolean isBetter(SkillMatch a, SkillMatch b) {
        // Sim chỉ nhận đúng 1.0, 0.5 hoặc 0.0 (không qua phép tính), nên so bằng == được, không cần dung sai.
        if (a.sim() != b.sim()) {
            return a.sim() > b.sim();
        }
        if (a.teach().level() != b.teach().level()) {
            return a.teach().level() > b.teach().level();
        }
        return a.teach().skillId() < b.teach().skillId();
    }

    /** Bước 3. Trung bình LevelFit trên các kỹ năng muốn học có Sim > 0. */
    public double levelFit(List<LearnItem> wants, List<TeachItem> teaches) {
        double total = 0.0;
        int counted = 0;
        for (LearnItem want : wants) {
            Optional<SkillMatch> match = bestTeachFor(want, teaches);
            // Kỹ năng Sim = 0 bị bỏ khỏi cả tử lẫn mẫu (spec 4.3): nó đã bị phạt ở SkillFit rồi.
            if (match.isPresent()) {
                total += levelScore(want.targetLevel(), match.get().teach().level());
                counted++;
            }
        }
        // Không kỹ năng nào khớp (chỉ xảy ra trước lọc cứng): 0.0 thay vì 0.0 / 0 = NaN (kế hoạch bước 3).
        return counted == 0 ? 0.0 : total / counted;
    }

    /** LevelFit của một cặp (spec 4.3, 4.6). gap = mục tiêu − trình độ = "thấp hơn gap bậc". */
    private static double levelScore(Integer targetLevel, int teachLevel) {
        if (targetLevel == null) {
            return 0.5;
        }
        int gap = targetLevel - teachLevel;
        if (gap <= 0) {
            return 1.0;
        }
        return gap == 1 ? 0.5 : 0.0;
    }

    /** Bước 4. Số khung trùng / min(số khung hai bên); một bên chưa khai báo (tập rỗng) thì 0.5. */
    public double scheduleFit(Set<Slot> learnerSlots, Set<Slot> teacherSlots) {
        throw new UnsupportedOperationException("Bước 4");
    }

    /** Bước 4. null = chưa khai báo. */
    public double modeFit(LearningMode learnerMode, LearningMode teacherMode) {
        throw new UnsupportedOperationException("Bước 4");
    }

    /** Bước 7. Đủ năm thành phần và D theo hướng learner học từ teacher. */
    public ScoreBreakdown breakdown(MatchProfile learner, MatchProfile teacher, MatchingWeights weights) {
        throw new UnsupportedOperationException("Bước 7");
    }
}
