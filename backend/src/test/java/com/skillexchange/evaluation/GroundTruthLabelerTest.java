package com.skillexchange.evaluation;

import static com.skillexchange.evaluation.Relevance.LOAI;
import static com.skillexchange.evaluation.Relevance.NHAN_0;
import static com.skillexchange.evaluation.Relevance.NHAN_1;
import static com.skillexchange.evaluation.Relevance.NHAN_2;
import static com.skillexchange.user.LearningMode.ANY;
import static com.skillexchange.user.LearningMode.OFFLINE;
import static com.skillexchange.user.LearningMode.ONLINE;
import static com.skillexchange.user.Period.EVENING;
import static com.skillexchange.user.Period.MORNING;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.skillexchange.matching.SkillSimilarity;
import com.skillexchange.matching.Slot;
import com.skillexchange.user.LearningMode;

/**
 * Gán nhãn Ground Truth theo thác nước, trên giá trị thật: TC211–TC223 (spec 5.1, A.3; D53–D55).
 *
 * Dữ liệu mẫu theo docs/tuan_5/kiem_thu/ca_kiem_thu.md, "Dữ liệu mẫu dùng chung":
 * - u muốn học Java (mục tiêu 3) và SQL (mục tiêu 2); lịch Tối T2, Tối T4, Tối T6; ONLINE.
 * - Ứng viên gốc: lịch Tối T4, Tối T6, Sáng T7 (trùng u đúng 2 khung); ANY; trình độ tự khai = thật.
 * Cặp liên quan theo V2: Java–Kotlin, SQL–MySQL.
 */
class GroundTruthLabelerTest {

    private static final SkillCatalog CATALOG = SkillCatalog.V2;
    private static final long JAVA = CATALOG.idOf("Java");
    private static final long KOTLIN = CATALOG.idOf("Kotlin");
    private static final long PYTHON = CATALOG.idOf("Python");
    private static final long SQL = CATALOG.idOf("SQL");
    private static final long MYSQL = CATALOG.idOf("MySQL");
    private static final long PIANO = CATALOG.idOf("Piano");

    // Ngày 1 = Thứ Hai ... 7 = Chủ nhật.
    private static final Slot MON_EVENING = new Slot(1, EVENING);  // Tối T2
    private static final Slot WED_EVENING = new Slot(3, EVENING);  // Tối T4
    private static final Slot FRI_EVENING = new Slot(5, EVENING);  // Tối T6
    private static final Slot SAT_MORNING = new Slot(6, MORNING);  // Sáng T7
    private static final Slot MON_MORNING = new Slot(1, MORNING);  // Sáng T2
    private static final Slot SUN_MORNING = new Slot(7, MORNING);  // Sáng CN

    private static final List<Slot> U_SLOTS = List.of(MON_EVENING, WED_EVENING, FRI_EVENING);
    private static final List<Slot> BASE_SLOTS = List.of(WED_EVENING, FRI_EVENING, SAT_MORNING);
    private static final List<SimLearn> U_LEARN = List.of(new SimLearn(JAVA, 3), new SimLearn(SQL, 2));

    private static final SimUser U = learner(U_LEARN, U_SLOTS, ONLINE);
    private static final SimUser V1 = candidate(11, same(JAVA, 3));

    private final GroundTruthLabeler labeler = new GroundTruthLabeler(new SkillSimilarity(CATALOG.relations()));

    // ---------- Ví dụ A.3 ----------

    @Test
    @DisplayName("TC211 v1 dạy Java (3): nhãn 2")
    void tc211_v1JavaMeetsTarget() {
        assertThat(labeler.label(U, V1)).isEqualTo(NHAN_2);
    }

    @Test
    @DisplayName("TC212 v2 dạy Java (3), MySQL (1): nhãn 2, một kỹ năng đạt là đủ")
    void tc212_oneSkillMeetingTargetIsEnough() {
        SimUser v2 = candidate(12, same(JAVA, 3), same(MYSQL, 1));

        assertThat(labeler.label(U, v2)).isEqualTo(NHAN_2);
    }

    @Test
    @DisplayName("TC213 v3 dạy Kotlin (1), MySQL (3): nhãn 1, vì MySQL đạt mục tiêu SQL nên không phải mọi cặp thấp hơn từ 2 bậc")
    void tc213_relatedOnlyButOnePairReachesTarget() {
        SimUser v3 = candidate(13, same(KOTLIN, 1), same(MYSQL, 3));

        assertThat(labeler.label(U, v3)).isEqualTo(NHAN_1);
    }

    @Test
    @DisplayName("TC214 v4 dạy Kotlin (1): nhãn 0, cặp duy nhất Java–Kotlin thấp hơn mục tiêu 2 bậc")
    void tc214_relatedOnlyAndTwoLevelsBelow() {
        SimUser v4 = candidate(14, same(KOTLIN, 1));

        assertThat(labeler.label(U, v4)).isEqualTo(NHAN_0);
    }

    @Test
    @DisplayName("TC215 v5 dạy Java (3), chưa khai lịch (rỗng ở cả hai lớp): nhãn 1, không bị loại")
    void tc215_candidateWithoutScheduleIsNotExcluded() {
        SimUser v5 = candidate(15, List.of(same(JAVA, 3)), List.of(), List.of(), ANY);

        assertThat(labeler.label(U, v5)).isEqualTo(NHAN_1);
    }

    @Test
    @DisplayName("TC216 v6 dạy Java, tự khai 3, thật 2: nhãn 1, nhãn dùng trình độ thật")
    void tc216_labelUsesTrueLevel() {
        SimUser v6 = candidate(16, new SimTeach(JAVA, 2, 3));

        assertThat(labeler.label(U, v6)).isEqualTo(NHAN_1);
    }

    @Test
    @DisplayName("TC217 v7 dạy Java (3), lịch thật trùng u đúng 1 khung: nhãn 1")
    void tc217_oneCommonSlotIsNotEnoughForLabelTwo() {
        List<Slot> slots = List.of(WED_EVENING, SAT_MORNING);
        SimUser v7 = candidate(17, List.of(same(JAVA, 3)), slots, slots, ANY);

        assertThat(labeler.label(U, v7)).isEqualTo(NHAN_1);
    }

    @Test
    @DisplayName("TC218 (a) v8 chưa khai hình thức; (b) u chưa khai hình thức, v1: cả hai nhãn 1, không bị loại")
    void tc218_undeclaredModeGivesAtMostLabelOne() {
        SimUser v8 = candidate(18, List.of(same(JAVA, 3)), BASE_SLOTS, BASE_SLOTS, null);
        SimUser uWithoutMode = learner(U_LEARN, U_SLOTS, null);

        assertThat(labeler.label(U, v8)).isEqualTo(NHAN_1);
        assertThat(labeler.label(uWithoutMode, V1)).isEqualTo(NHAN_1);
    }

    @Test
    @DisplayName("TC219 v1 với (a) u ANY, v ONLINE; (b) u OFFLINE, v OFFLINE; (c) u ONLINE, v ANY: cả ba nhãn 2")
    void tc219_compatibleModesIncludingAny() {
        SimUser vOnline = candidate(11, List.of(same(JAVA, 3)), BASE_SLOTS, BASE_SLOTS, ONLINE);
        SimUser vOffline = candidate(11, List.of(same(JAVA, 3)), BASE_SLOTS, BASE_SLOTS, OFFLINE);

        assertThat(labeler.label(learner(U_LEARN, U_SLOTS, ANY), vOnline)).as("(a)").isEqualTo(NHAN_2);
        assertThat(labeler.label(learner(U_LEARN, U_SLOTS, OFFLINE), vOffline)).as("(b)").isEqualTo(NHAN_2);
        assertThat(labeler.label(U, V1)).as("(c)").isEqualTo(NHAN_2);
    }

    @Test
    @DisplayName("TC220 Loại: (a) v9 OFFLINE gặp u ONLINE; (b) v = u; (c) v chỉ dạy Python; (d) lịch thật không trùng dù lịch tự khai trùng")
    void tc220_excludedAtStepOne() {
        SimUser v9 = candidate(19, List.of(same(JAVA, 3)), BASE_SLOTS, BASE_SLOTS, OFFLINE);
        SimUser pythonOnly = candidate(20, same(PYTHON, 4));
        // Lịch thật Sáng T2 không trùng u; nhiễu thêm Tối T4 vào lịch tự khai.
        SimUser fakeOverlap = candidate(21, List.of(same(JAVA, 3)),
                List.of(MON_MORNING), List.of(MON_MORNING, WED_EVENING), ANY);

        assertThat(labeler.label(U, v9)).as("(a)").isEqualTo(LOAI);
        assertThat(labeler.label(U, U)).as("(b)").isEqualTo(LOAI);
        assertThat(labeler.label(U, pythonOnly)).as("(c)").isEqualTo(LOAI);
        assertThat(labeler.label(U, fakeOverlap)).as("(d) Bước 1 dùng lịch thật").isEqualTo(LOAI);
    }

    @Test
    @DisplayName("TC221 (a) u bỏ trống mục tiêu Java, v1: nhãn 1; (b) u chỉ học Java, bỏ trống mục tiêu, v dạy Kotlin (1): nhãn 1, không phải 0")
    void tc221_blankTargetIsNeitherMetNorBelow() {
        SimUser uBlankJava = learner(List.of(new SimLearn(JAVA, null), new SimLearn(SQL, 2)), U_SLOTS, ONLINE);
        SimUser uOnlyBlankJava = learner(List.of(new SimLearn(JAVA, null)), U_SLOTS, ONLINE);
        SimUser kotlinOne = candidate(22, same(KOTLIN, 1));

        assertThat(labeler.label(uBlankJava, V1)).as("(a)").isEqualTo(NHAN_1);
        assertThat(labeler.label(uOnlyBlankJava, kotlinOne)).as("(b)").isEqualTo(NHAN_1);
    }

    @Test
    @DisplayName("TC222 v1 đổi trình độ tự khai Java thành 1, lịch tự khai thành Sáng CN (giữ giá trị thật): vẫn nhãn 2")
    void tc222_labelIgnoresDeclaredValues() {
        SimUser v1Misreported = candidate(11, List.of(new SimTeach(JAVA, 3, 1)),
                BASE_SLOTS, List.of(SUN_MORNING), ANY);

        assertThat(labeler.label(U, v1Misreported)).isEqualTo(NHAN_2);
    }

    // ---------- Phần D chưa có ca ----------

    @Test
    @DisplayName("D54 v dạy Kotlin (2), thấp hơn mục tiêu Java 1 bậc: nhãn 1, nhãn 0 cần thấp hơn từ 2 bậc")
    void d54_oneLevelBelowIsNotLabelZero() {
        SimUser kotlinTwo = candidate(23, same(KOTLIN, 2));

        assertThat(labeler.label(U, kotlinTwo)).isEqualTo(NHAN_1);
    }

    @Test
    @DisplayName("D53 u chưa khai lịch, v1: không bị loại, nhãn 1 vì nhãn 2 cần cả hai đã khai lịch")
    void d53_learnerWithoutScheduleIsNotExcluded() {
        SimUser uWithoutSchedule = learner(U_LEARN, List.of(), ONLINE);

        assertThat(labeler.label(uWithoutSchedule, V1)).isEqualTo(NHAN_1);
    }

    @Test
    @DisplayName("D55 rel của nhãn 0, 1, 2 là 0, 1, 2; Loại không có rel")
    void d55_relevanceValues() {
        assertThat(NHAN_0.rel()).isEqualTo(0);
        assertThat(NHAN_1.rel()).isEqualTo(1);
        assertThat(NHAN_2.rel()).isEqualTo(2);
        assertThatThrownBy(LOAI::rel).isInstanceOf(IllegalStateException.class);
    }

    // ---------- Dữ liệu sinh ngẫu nhiên ----------

    @Test
    @DisplayName("TC223 N = 200, seed 1, nhiễu mặc định: mỗi cặp đúng một kết quả; nhãn 2 có kỹ năng trùng chính xác, nhãn 0 không; Loại đúng các cặp vi phạm Bước 1")
    void tc223_allPairsOfGeneratedData() {
        List<SimUser> users = SyntheticDataGenerator.generate(200, 1, NoiseLevel.MAC_DINH);
        Map<Relevance, Integer> counts = new EnumMap<>(Relevance.class);

        for (SimUser u : users) {
            for (SimUser v : users) {
                Relevance label = labeler.label(u, v);
                String pair = "u = " + u.id() + ", v = " + v.id();

                assertThat(label).as(pair).isNotNull();
                assertThat(label == LOAI).as(pair).isEqualTo(violatesStepOne(u, v));
                if (label == NHAN_2) {
                    assertThat(hasExactMatch(u, v)).as(pair).isTrue();
                }
                if (label == NHAN_0) {
                    assertThat(hasExactMatch(u, v)).as(pair).isFalse();
                }
                counts.merge(label, 1, Integer::sum);
            }
        }
        // Mỗi kết quả phải xuất hiện, để các điều kiện "mọi cặp nhãn ..." ở trên không đúng vì không có cặp nào.
        assertThat(counts).containsKeys(LOAI, NHAN_0, NHAN_1, NHAN_2);
    }

    // ---------- Hàm tiện ích ----------

    /** u của ví dụ A.3 (mã 1), đổi được phần học, lịch, hình thức. u dạy Piano: không ứng viên nào học từ u. */
    private static SimUser learner(List<SimLearn> learn, List<Slot> slots, LearningMode mode) {
        return new SimUser(1, List.of(same(PIANO, 3)), learn, slots, slots, mode);
    }

    /** Ứng viên gốc với phần dạy cho trước. Phần học để trống: nhãn chỉ xét hướng u học từ v. */
    private static SimUser candidate(long id, SimTeach... teach) {
        return candidate(id, List.of(teach), BASE_SLOTS, BASE_SLOTS, ANY);
    }

    private static SimUser candidate(long id, List<SimTeach> teach,
            List<Slot> trueSlots, List<Slot> declaredSlots, LearningMode mode) {
        return new SimUser(id, teach, List.of(), trueSlots, declaredSlots, mode);
    }

    /** Kỹ năng dạy có trình độ tự khai bằng trình độ thật. */
    private static SimTeach same(long skillId, int level) {
        return new SimTeach(skillId, level, level);
    }

    /** Cặp liên quan dạng tập hai mã, dựng thẳng từ danh mục, không qua SkillSimilarity. */
    private static final Set<Set<Long>> RELATED_PAIRS = CATALOG.relations().stream()
            .map(relation -> Set.of(relation.skillA(), relation.skillB()))
            .collect(toSet());

    /** Bước 1 của spec 5.1 viết lại độc lập với GroundTruthLabeler, trên giá trị thật. */
    private static boolean violatesStepOne(SimUser u, SimUser v) {
        if (u.id() == v.id()) {
            return true;
        }
        boolean sameOrRelated = false;
        for (SimLearn want : u.learn()) {
            for (SimTeach offer : v.teach()) {
                // Kiểm "trùng" trước: Set.of(a, a) ném lỗi khi hai mã bằng nhau.
                if (want.skillId() == offer.skillId()
                        || RELATED_PAIRS.contains(Set.of(want.skillId(), offer.skillId()))) {
                    sameOrRelated = true;
                }
            }
        }
        if (!sameOrRelated) {
            return true;
        }
        boolean bothHaveSchedule = !u.trueSlots().isEmpty() && !v.trueSlots().isEmpty();
        if (bothHaveSchedule && Collections.disjoint(u.trueSlots(), v.trueSlots())) {
            return true;
        }
        return (u.mode() == ONLINE && v.mode() == OFFLINE) || (u.mode() == OFFLINE && v.mode() == ONLINE);
    }

    private static boolean hasExactMatch(SimUser u, SimUser v) {
        for (SimLearn want : u.learn()) {
            for (SimTeach offer : v.teach()) {
                if (want.skillId() == offer.skillId()) {
                    return true;
                }
            }
        }
        return false;
    }
}
