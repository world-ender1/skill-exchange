package com.skillexchange.matching;

import java.util.Collection;

/**
 * Tra Sim(s, t) (spec 4.1): trùng chính xác 1.0, được đánh dấu liên quan 0.5, còn lại 0.0.
 * Quan hệ không bắc cầu: Trung–Nhật và Nhật–Hàn liên quan không có nghĩa Trung–Hàn liên quan.
 */
public class SkillSimilarity {

    public SkillSimilarity(Collection<SkillRelation> relations) {
        // Bước 2: lưu các cặp liên quan sao cho tra được cả hai chiều.
    }

    public double sim(long wantSkillId, long teachSkillId) {
        throw new UnsupportedOperationException("Bước 2");
    }
}
