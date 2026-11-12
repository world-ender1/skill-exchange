package com.skillexchange.evaluation;

import java.util.List;
import java.util.Set;

import com.skillexchange.matching.LearnItem;
import com.skillexchange.matching.MatchProfile;
import com.skillexchange.matching.MatchResult;
import com.skillexchange.matching.Slot;
import com.skillexchange.matching.TeachItem;
import com.skillexchange.user.LearningMode;
import com.skillexchange.user.Period;
import com.skillexchange.user.Role;
import com.skillexchange.user.UserStatus;

/**
 * Dữ liệu dựng tay cho test các phương án (bước 6). Kỹ năng gọi bằng tên trong SkillCatalog.V2,
 * nên cặp liên quan là của V2: Java–Kotlin, SQL–MySQL...
 */
final class EvaluationFixtures {

    static final SkillCatalog CATALOG = SkillCatalog.V2;

    private EvaluationFixtures() {
    }

    static LearnItem learn(String skill, Integer target) {
        return new LearnItem(CATALOG.idOf(skill), skill, target);
    }

    static TeachItem teach(String skill, int level) {
        return new TeachItem(CATALOG.idOf(skill), skill, level);
    }

    /** Tối của ngày day (1 = Thứ Hai ... 7 = Chủ nhật). */
    static Slot evening(int day) {
        return new Slot(day, Period.EVENING);
    }

    static Slot morning(int day) {
        return new Slot(day, Period.MORNING);
    }

    /** Sinh viên ACTIVE, Reputation 0.5 như người giả lập (spec 5.4). slots rỗng, mode null = chưa khai. */
    static MatchProfile profile(long id, List<LearnItem> learn, List<TeachItem> teach,
            Set<Slot> slots, LearningMode mode) {
        return new MatchProfile(id, UserStatus.ACTIVE, Role.STUDENT, learn, teach, slots, mode, SimUser.REPUTATION);
    }

    /** Bộ dữ liệu giả lập đã đổi sang hồ sơ tự khai, xếp theo mã. */
    static List<MatchProfile> generatedProfiles(int n, long seed, NoiseLevel noise) {
        return SyntheticDataGenerator.generate(n, seed, noise).stream()
                .map(user -> user.toMatchProfile(CATALOG))
                .toList();
    }

    static List<Long> ids(List<MatchResult> results) {
        return results.stream().map(MatchResult::userId).toList();
    }
}
