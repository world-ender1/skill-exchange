package com.skillexchange.matching;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Tra Sim(s, t) (spec 4.1): trùng chính xác 1.0, được đánh dấu liên quan 0.5, còn lại 0.0.
 * Quan hệ không bắc cầu: Trung–Nhật và Nhật–Hàn liên quan không có nghĩa Trung–Hàn liên quan.
 */
public class SkillSimilarity {

    private static final double EXACT = 1.0;
    private static final double RELATED = 0.5;
    private static final double UNRELATED = 0.0;

    /**
     * Mỗi cặp liên quan lưu một lần ở dạng chuẩn (id nhỏ, id lớn), nên tra (a, b) hay (b, a) đều ra cùng một khóa.
     * Không dựa vào CHECK skill_a_id < skill_b_id của DB: module evaluation (tuần 5) có thể đưa cặp theo thứ tự bất kỳ.
     */
    private final Set<SkillRelation> relations = new HashSet<>();

    public SkillSimilarity(Collection<SkillRelation> relations) {
        for (SkillRelation relation : relations) {
            this.relations.add(normalized(relation.skillA(), relation.skillB()));
        }
    }

    public double sim(long wantSkillId, long teachSkillId) {
        if (wantSkillId == teachSkillId) {
            return EXACT;
        }
        if (relations.contains(normalized(wantSkillId, teachSkillId))) {
            return RELATED;
        }
        return UNRELATED;
    }

    private static SkillRelation normalized(long a, long b) {
        return new SkillRelation(Math.min(a, b), Math.max(a, b));
    }
}
