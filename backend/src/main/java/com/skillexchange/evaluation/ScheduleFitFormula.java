package com.skillexchange.evaluation;

import java.util.Set;

import com.skillexchange.matching.Slot;

/**
 * Ba công thức ScheduleFit được so sánh ở thực nghiệm (spec 5.4, D68).
 * Lõi matching chỉ có MIN; hai công thức còn lại được tính lại trong evaluation, không sửa lõi.
 *
 * Cả ba đều > 0 khi và chỉ khi có khung trùng, nên lọc cứng và phân loại hai nhóm của lõi vẫn đúng.
 * Cả ba đều đối xứng: đổi vai người học, người dạy không đổi kết quả.
 */
public enum ScheduleFitFormula {
    /** Số khung trùng / số khung của bên ít khung hơn: công thức của lõi (spec 4.3). */
    MIN,
    /** Số khung trùng / số khung của hợp hai lịch. */
    JACCARD,
    /** min(số khung trùng, 3) / 3: trùng từ 3 khung trở lên là đủ. */
    TRAN_3;

    /** Chưa khai lịch thì trung tính, như lõi (spec 4.3). */
    private static final double UNDECLARED = 0.5;
    private static final int CAP = 3;

    public double scheduleFit(Set<Slot> learnerSlots, Set<Slot> teacherSlots) {
        // Kiểm trước khi chia: hai tập rỗng thì min, hợp đều bằng 0.
        if (learnerSlots.isEmpty() || teacherSlots.isEmpty()) {
            return UNDECLARED;
        }
        int learnerCount = learnerSlots.size();
        int teacherCount = teacherSlots.size();
        int common = countCommon(learnerSlots, teacherSlots);

        return switch (this) {
            case MIN -> (double) common / Math.min(learnerCount, teacherCount);
            // |A ∪ B| = |A| + |B| - |A ∩ B|: khung trùng bị đếm hai lần nên trừ đi một lần.
            case JACCARD -> (double) common / (learnerCount + teacherCount - common);
            case TRAN_3 -> (double) Math.min(common, CAP) / CAP;
        };
    }

    /** Chỉ đếm, không rút số ngẫu nhiên, nên duyệt Set ở đây không ảnh hưởng tái lập (D64). */
    private static int countCommon(Set<Slot> a, Set<Slot> b) {
        int common = 0;
        for (Slot slot : a) {
            if (b.contains(slot)) {
                common++;
            }
        }
        return common;
    }
}
