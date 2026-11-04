package com.skillexchange.matching;

import static com.skillexchange.user.Period.EVENING;
import static com.skillexchange.user.Period.MORNING;

import java.util.List;
import java.util.Set;

import com.skillexchange.user.LearningMode;
import com.skillexchange.user.Role;
import com.skillexchange.user.UserStatus;

/**
 * Dữ liệu mẫu dùng chung cho unit test lõi matching, lấy từ docs/tuan_4/kiem_thu/ca_kiem_thu.md
 * ("Dữ liệu mẫu dùng chung"), spec 4.6 và phụ lục A.3.
 *
 * Dựng một lần ở đây để các test không tự "chỉnh nhẹ" dữ liệu cho dễ pass.
 * Id kỹ năng là số tự đặt cho unit test, không phải id trong DB; quan hệ liên quan giống V2__seed_skills.sql.
 */
final class MatchingFixtures {

    private MatchingFixtures() {
    }

    // ---------- Kỹ năng ----------
    static final long JAVA = 1;
    static final long KOTLIN = 2;
    static final long PYTHON = 3;
    static final long ENGLISH = 4;
    static final long SQL = 5;
    static final long MYSQL = 6;
    static final long FIGMA = 7;
    static final long JAPANESE = 8;
    static final long CHINESE = 9;
    static final long KOREAN = 10;

    static final List<SkillRelation> RELATIONS = List.of(
            new SkillRelation(JAVA, KOTLIN),
            new SkillRelation(SQL, MYSQL),
            new SkillRelation(JAPANESE, CHINESE),
            new SkillRelation(JAPANESE, KOREAN));

    static SkillSimilarity similarity() {
        return new SkillSimilarity(RELATIONS);
    }

    // ---------- Dựng từng phần ----------
    static LearnItem learn(long skillId, Integer targetLevel) {
        return new LearnItem(skillId, nameOf(skillId), targetLevel);
    }

    static TeachItem teach(long skillId, int level) {
        return new TeachItem(skillId, nameOf(skillId), level);
    }

    static Slot slot(int dayOfWeek, com.skillexchange.user.Period period) {
        return new Slot(dayOfWeek, period);
    }

    /** Sinh viên ACTIVE với các phần còn lại tùy ý. */
    static MatchProfile student(long userId, List<LearnItem> learn, List<TeachItem> teach,
            Set<Slot> slots, LearningMode mode, double reputation) {
        return new MatchProfile(userId, UserStatus.ACTIVE, Role.STUDENT, learn, teach, slots, mode, reputation);
    }

    static String nameOf(long skillId) {
        return switch ((int) skillId) {
            case 1 -> "Java";
            case 2 -> "Kotlin";
            case 3 -> "Python";
            case 4 -> "Tiếng Anh";
            case 5 -> "SQL";
            case 6 -> "MySQL";
            case 7 -> "Figma";
            case 8 -> "Tiếng Nhật";
            case 9 -> "Tiếng Trung";
            case 10 -> "Tiếng Hàn";
            default -> "Kỹ năng " + skillId;
        };
    }

    // ---------- An và Bình (ca kiểm thử tuần 4, "Dữ liệu mẫu dùng chung") ----------
    static final long AN_ID = 1;
    static final long BINH_ID = 2;

    /** Muốn học Java (3), Tiếng Anh (không mục tiêu); dạy Python (4); Tối T2, T4, T6; ONLINE; chưa có lịch sử. */
    static MatchProfile an() {
        return student(AN_ID,
                List.of(learn(JAVA, 3), learn(ENGLISH, null)),
                List.of(teach(PYTHON, 4)),
                Set.of(slot(1, EVENING), slot(3, EVENING), slot(5, EVENING)),
                LearningMode.ONLINE,
                0.5);
    }

    /** Muốn học Python (2); dạy Kotlin (2), Tiếng Anh (3); Tối T4, T6, Sáng T7, CN; ANY; chưa có lịch sử. */
    static MatchProfile binh() {
        return student(BINH_ID,
                List.of(learn(PYTHON, 2)),
                List.of(teach(KOTLIN, 2), teach(ENGLISH, 3)),
                Set.of(slot(3, EVENING), slot(5, EVENING), slot(6, MORNING), slot(7, MORNING)),
                LearningMode.ANY,
                0.5);
    }

    // ---------- Ví dụ LevelFit ở spec 4.6 ----------
    /** u muốn học Java (3), SQL (2), Figma (không mục tiêu). */
    static List<LearnItem> spec46Wants() {
        return List.of(learn(JAVA, 3), learn(SQL, 2), learn(FIGMA, null));
    }

    /** v dạy Java (3), MySQL (1). */
    static List<TeachItem> spec46Teaches() {
        return List.of(teach(JAVA, 3), teach(MYSQL, 1));
    }

    // ---------- Ví dụ D(u, v) ở phụ lục A.3 ----------
    static final long A3_U_ID = 10;
    static final long A3_V_ID = 11;

    /** u chỉ muốn học Java (mục tiêu 3), 4 khung giờ, ONLINE. */
    static MatchProfile a3Learner() {
        return student(A3_U_ID,
                List.of(learn(JAVA, 3)),
                List.of(),
                Set.of(slot(1, EVENING), slot(2, EVENING), slot(3, EVENING), slot(4, EVENING)),
                LearningMode.ONLINE,
                0.5);
    }

    /** v dạy Java (3), 3 khung trùng 2 với u, ONLINE, Reputation 0.8. */
    static MatchProfile a3Teacher() {
        return student(A3_V_ID,
                List.of(),
                List.of(teach(JAVA, 3)),
                Set.of(slot(1, EVENING), slot(2, EVENING), slot(6, MORNING)),
                LearningMode.ONLINE,
                0.8);
    }
}
