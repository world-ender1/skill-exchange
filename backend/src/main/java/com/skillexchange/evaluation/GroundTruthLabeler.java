package com.skillexchange.evaluation;

import com.skillexchange.matching.SkillSimilarity;
import com.skillexchange.user.LearningMode;

/**
 * Gán nhãn Ground Truth cho hướng u học từ v, theo thứ tự thác nước (spec 5.1; D53–D55).
 *
 * Chỉ đọc giá trị thật (trueLevel, trueSlots) cùng kỹ năng, mục tiêu và hình thức; không đọc giá trị tự khai.
 * Không gọi ScoreCalculator, HardFilter hay hàm điểm nào của lõi: nhãn phải độc lập với thuật toán đang được đánh giá.
 * SkillSimilarity chỉ tra danh mục (trùng, liên quan, không liên quan), không phải hàm điểm, nên được dùng.
 */
public class GroundTruthLabeler {

    // SkillSimilarity trả đúng một trong ba hằng 1.0, 0.5, 0.0, nên so bằng == là chính xác.
    private static final double EXACT = 1.0;
    private static final double RELATED = 0.5;

    /** Nhãn 2 cần lịch thật trùng ít nhất 2 khung. */
    private static final int MIN_COMMON_SLOTS_FOR_LABEL_2 = 2;
    /** Nhãn 0 cần mọi cặp liên quan thấp hơn mục tiêu từ 2 bậc trở lên. */
    private static final int MIN_GAP_FOR_LABEL_0 = 2;

    private final SkillSimilarity similarity;

    public GroundTruthLabeler(SkillSimilarity similarity) {
        this.similarity = similarity;
    }

    public Relevance label(SimUser u, SimUser v) {
        if (excludedAtStepOne(u, v)) {
            return Relevance.LOAI;
        }
        if (highlyRelevant(u, v)) {
            return Relevance.NHAN_2;
        }
        if (qualityIrrelevant(u, v)) {
            return Relevance.NHAN_0;
        }
        return Relevance.NHAN_1;
    }

    // ---------- Bước 1: loại trừ cứng ----------

    /**
     * Không xét "chưa xác thực / bị khóa": mọi người giả lập là ACTIVE, STUDENT (spec 5.4).
     * Chưa khai lịch hoặc hình thức thì không bị loại vì phần đó (D53).
     */
    private boolean excludedAtStepOne(SimUser u, SimUser v) {
        if (u.id() == v.id()) {
            return true;
        }
        if (!hasSameOrRelatedSkill(u, v)) {
            return true;
        }
        if (bothHaveSchedule(u, v) && commonTrueSlots(u, v) == 0) {
            return true;
        }
        return modesConflict(u.mode(), v.mode());
    }

    /** Có kỹ năng u muốn học mà v dạy đúng nó hoặc dạy kỹ năng liên quan (chỉ hướng u học từ v). */
    private boolean hasSameOrRelatedSkill(SimUser u, SimUser v) {
        for (SimLearn want : u.learn()) {
            for (SimTeach offer : v.teach()) {
                if (similarity.sim(want.skillId(), offer.skillId()) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean modesConflict(LearningMode a, LearningMode b) {
        return (a == LearningMode.ONLINE && b == LearningMode.OFFLINE)
                || (a == LearningMode.OFFLINE && b == LearningMode.ONLINE);
    }

    // ---------- Bước 2: nhãn 2 ----------

    private boolean highlyRelevant(SimUser u, SimUser v) {
        return hasExactSkillMeetingTarget(u, v)
                && bothHaveSchedule(u, v)
                && commonTrueSlots(u, v) >= MIN_COMMON_SLOTS_FOR_LABEL_2
                && modesFullyCompatible(u.mode(), v.mode());
    }

    /**
     * Có ít nhất một kỹ năng v dạy đúng kỹ năng u muốn học, với trình độ thật đạt mục tiêu (D54).
     * Một kỹ năng đạt là đủ; mục tiêu bỏ trống không tính là đạt.
     */
    private boolean hasExactSkillMeetingTarget(SimUser u, SimUser v) {
        for (SimLearn want : u.learn()) {
            for (SimTeach offer : v.teach()) {
                boolean exact = similarity.sim(want.skillId(), offer.skillId()) == EXACT;
                if (exact && want.target() != null && offer.trueLevel() >= want.target()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Điều kiện ModeFit = 1.0 ở spec 4.3: cả hai đã khai và cùng hình thức, hoặc một bên ANY (D53). */
    private static boolean modesFullyCompatible(LearningMode a, LearningMode b) {
        if (a == null || b == null) {
            return false;
        }
        return a == b || a == LearningMode.ANY || b == LearningMode.ANY;
    }

    // ---------- Bước 3: nhãn 0 ----------

    /**
     * Không có cặp trùng chính xác, và mọi cặp liên quan đều thấp hơn mục tiêu từ 2 bậc (D54).
     * Đã qua Bước 1 nên có ít nhất một cặp liên quan: vòng lặp không bao giờ trả true vì không xét cặp nào.
     */
    private boolean qualityIrrelevant(SimUser u, SimUser v) {
        for (SimLearn want : u.learn()) {
            for (SimTeach offer : v.teach()) {
                double sim = similarity.sim(want.skillId(), offer.skillId());
                if (sim == EXACT) {
                    return false;
                }
                if (sim == RELATED && !farBelowTarget(offer.trueLevel(), want.target())) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Mục tiêu bỏ trống không tính là thấp hơn (D54). */
    private static boolean farBelowTarget(int trueLevel, Integer target) {
        return target != null && target - trueLevel >= MIN_GAP_FOR_LABEL_0;
    }

    // ---------- Dùng chung ----------

    /** "Đã khai lịch" = lịch thật khác rỗng; hai lớp cùng rỗng hoặc cùng có khung (D53). */
    private static boolean bothHaveSchedule(SimUser u, SimUser v) {
        return !u.trueSlots().isEmpty() && !v.trueSlots().isEmpty();
    }

    /** Số khung trùng trên lịch thật. */
    private static int commonTrueSlots(SimUser u, SimUser v) {
        int count = 0;
        for (var slot : u.trueSlots()) {
            if (v.trueSlots().contains(slot)) {
                count++;
            }
        }
        return count;
    }
}
