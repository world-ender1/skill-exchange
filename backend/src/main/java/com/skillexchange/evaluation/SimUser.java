package com.skillexchange.evaluation;

import java.util.List;
import java.util.Set;

import com.skillexchange.matching.LearnItem;
import com.skillexchange.matching.MatchProfile;
import com.skillexchange.matching.Slot;
import com.skillexchange.matching.TeachItem;
import com.skillexchange.user.LearningMode;
import com.skillexchange.user.Role;
import com.skillexchange.user.UserStatus;

/**
 * Một người dùng giả lập có hai lớp dữ liệu (spec 5.1, 5.4; D53):
 * - lớp thật: trueLevel của từng kỹ năng dạy và trueSlots, chỉ bộ gán nhãn đọc;
 * - lớp tự khai: declaredLevel và declaredSlots, là thứ thuật toán thấy qua toMatchProfile.
 * Kỹ năng, mục tiêu và hình thức học không có nhiễu nên chỉ có một lớp. mode null = chưa khai hình thức.
 *
 * Các danh sách là List có thứ tự cố định (D64): bộ sinh dữ liệu duyệt chúng khi rút số ngẫu nhiên,
 * nên không dùng Set (thứ tự duyệt của Set.copyOf đổi giữa các lần chạy JVM).
 */
public record SimUser(
        long id,
        List<SimTeach> teach,
        List<SimLearn> learn,
        List<Slot> trueSlots,
        List<Slot> declaredSlots,
        LearningMode mode) {

    /** Người giả lập không có lịch sử đánh giá hay phiên, nên Reputation = 0.5 (spec 5.4). */
    static final double REPUTATION = 0.5;

    public SimUser {
        // Chép sang List không sửa được: bên ngoài sửa list gốc thì người giả lập vẫn giữ nguyên.
        teach = List.copyOf(teach);
        learn = List.copyOf(learn);
        trueSlots = List.copyOf(trueSlots);
        declaredSlots = List.copyOf(declaredSlots);
        // D53: việc có khai lịch hay không không có nhiễu, nên hai lớp cùng rỗng hoặc cùng có khung.
        if (trueSlots.isEmpty() != declaredSlots.isEmpty()) {
            throw new IllegalArgumentException(
                    "Người " + id + ": lịch thật và lịch tự khai phải cùng rỗng hoặc cùng có khung (D53)");
        }
    }

    /**
     * Hồ sơ cho lõi matching. Chỉ mang giá trị tự khai, vì thuật toán không được thấy giá trị thật (spec 5.1).
     * Tên kỹ năng tra từ danh mục, vì lõi dùng tên để xếp youLearn/theyLearn (D46).
     */
    public MatchProfile toMatchProfile(SkillCatalog catalog) {
        List<TeachItem> teachItems = teach.stream()
                .map(item -> new TeachItem(item.skillId(), catalog.nameOf(item.skillId()), item.declaredLevel()))
                .toList();
        List<LearnItem> learnItems = learn.stream()
                .map(item -> new LearnItem(item.skillId(), catalog.nameOf(item.skillId()), item.target()))
                .toList();
        return new MatchProfile(
                id,
                UserStatus.ACTIVE,
                Role.STUDENT,
                learnItems,
                teachItems,
                Set.copyOf(declaredSlots),
                mode,
                REPUTATION);
    }
}
