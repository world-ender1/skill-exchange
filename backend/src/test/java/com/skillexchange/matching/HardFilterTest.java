package com.skillexchange.matching;

import static com.skillexchange.matching.MatchingFixtures.JAVA;
import static com.skillexchange.matching.MatchingFixtures.KOTLIN;
import static com.skillexchange.matching.MatchingFixtures.PYTHON;
import static com.skillexchange.matching.MatchingFixtures.learn;
import static com.skillexchange.matching.MatchingFixtures.slot;
import static com.skillexchange.matching.MatchingFixtures.student;
import static com.skillexchange.matching.MatchingFixtures.teach;
import static com.skillexchange.user.LearningMode.ANY;
import static com.skillexchange.user.LearningMode.OFFLINE;
import static com.skillexchange.user.LearningMode.ONLINE;
import static com.skillexchange.user.Period.EVENING;
import static com.skillexchange.user.Period.MORNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.skillexchange.user.LearningMode;
import com.skillexchange.user.Role;
import com.skillexchange.user.UserStatus;

/**
 * Unit test cho lọc cứng (spec 4.2, D43). Mỗi ca "bị loại" có một ứng viên đối chứng hợp lệ trong cùng danh sách,
 * để một hàm loại tất cả mọi người không pass được. Mỗi ca "còn lại" thì khẳng định ứng viên vẫn có mặt.
 */
class HardFilterTest {

    private static final double TOLERANCE = 1e-9;
    private static final long CALLER_ID = 100;
    private static final long CONTROL_ID = 200;

    private final ScoreCalculator calculator = new ScoreCalculator(MatchingFixtures.similarity());
    private final HardFilter filter = new HardFilter(calculator);

    /**
     * u: muốn học Java (3), dạy Kotlin (2); Tối T2, Tối T4; ONLINE.
     * u dạy Kotlin (liên quan Java) nên u qua được mọi điều kiện khác khi so với chính mình: chỉ điều kiện (1) loại u.
     */
    private static MatchProfile caller(LearningMode mode) {
        return student(CALLER_ID,
                List.of(learn(JAVA, 3)),
                List.of(teach(KOTLIN, 2)),
                Set.of(slot(1, EVENING), slot(3, EVENING)),
                mode,
                0.5);
    }

    private static MatchProfile caller() {
        return caller(ONLINE);
    }

    /** Ứng viên với tài khoản, kỹ năng dạy, lịch, hình thức tùy ý; không muốn học gì. */
    private static MatchProfile candidate(long id, UserStatus status, Role role,
            List<TeachItem> teaches, Set<Slot> slots, LearningMode mode) {
        return new MatchProfile(id, status, role, List.of(), teaches, slots, mode, 0.5);
    }

    /** Ứng viên hợp lệ: dạy Java (3), Tối T2, ONLINE. Đổi đúng một phần để tạo ca bị loại. */
    private static MatchProfile validCandidate(long id) {
        return candidate(id, UserStatus.ACTIVE, Role.STUDENT,
                List.of(teach(JAVA, 3)), Set.of(slot(1, EVENING)), ONLINE);
    }

    private static List<Long> ids(List<MatchProfile> profiles) {
        return profiles.stream().map(MatchProfile::userId).toList();
    }

    @Test
    @DisplayName("TC127 danh sách ứng viên có chính u → u bị loại, đối chứng còn")
    void tc127_callerIsNeverItsOwnCandidate() {
        MatchProfile u = caller();

        List<MatchProfile> result = filter.filter(u, List.of(u, validCandidate(CONTROL_ID)));

        assertThat(ids(result)).containsExactly(CONTROL_ID);
    }

    @Test
    @DisplayName("TC128 ứng viên PENDING_VERIFICATION khớp kỹ năng → bị loại")
    void tc128_pendingVerificationIsRejected() {
        MatchProfile pending = candidate(1, UserStatus.PENDING_VERIFICATION, Role.STUDENT,
                List.of(teach(JAVA, 3)), Set.of(slot(1, EVENING)), ONLINE);

        List<MatchProfile> result = filter.filter(caller(), List.of(pending, validCandidate(CONTROL_ID)));

        assertThat(ids(result)).containsExactly(CONTROL_ID);
    }

    @Test
    @DisplayName("TC129 ứng viên LOCKED khớp kỹ năng → bị loại")
    void tc129_lockedIsRejected() {
        MatchProfile locked = candidate(1, UserStatus.LOCKED, Role.STUDENT,
                List.of(teach(JAVA, 3)), Set.of(slot(1, EVENING)), ONLINE);

        List<MatchProfile> result = filter.filter(caller(), List.of(locked, validCandidate(CONTROL_ID)));

        assertThat(ids(result)).containsExactly(CONTROL_ID);
    }

    @Test
    @DisplayName("TC130 ứng viên ACTIVE nhưng role = ADMIN, khớp kỹ năng → bị loại")
    void tc130_adminIsRejected() {
        MatchProfile admin = candidate(1, UserStatus.ACTIVE, Role.ADMIN,
                List.of(teach(JAVA, 3)), Set.of(slot(1, EVENING)), ONLINE);

        List<MatchProfile> result = filter.filter(caller(), List.of(admin, validCandidate(CONTROL_ID)));

        assertThat(ids(result)).containsExactly(CONTROL_ID);
    }

    @Test
    @DisplayName("TC131 ứng viên dạy Python, u chỉ muốn Java (SkillFit = 0) → bị loại")
    void tc131_noMatchingSkillIsRejected() {
        MatchProfile python = candidate(1, UserStatus.ACTIVE, Role.STUDENT,
                List.of(teach(PYTHON, 4)), Set.of(slot(1, EVENING)), ONLINE);

        List<MatchProfile> result = filter.filter(caller(), List.of(python, validCandidate(CONTROL_ID)));

        assertThat(ids(result)).containsExactly(CONTROL_ID);
    }

    @Test
    @DisplayName("TC132 ứng viên khai Sáng T2 khi u khai Tối T2, Tối T4 (không trùng khung nào) → bị loại")
    void tc132_noCommonSlotIsRejected() {
        MatchProfile morning = candidate(1, UserStatus.ACTIVE, Role.STUDENT,
                List.of(teach(JAVA, 3)), Set.of(slot(1, MORNING)), ONLINE);

        List<MatchProfile> result = filter.filter(caller(), List.of(morning, validCandidate(CONTROL_ID)));

        assertThat(ids(result)).containsExactly(CONTROL_ID);
    }

    @Test
    @DisplayName("TC132 ứng viên OFFLINE khi u ONLINE → bị loại")
    void tc132_conflictingModeIsRejected() {
        MatchProfile offline = candidate(1, UserStatus.ACTIVE, Role.STUDENT,
                List.of(teach(JAVA, 3)), Set.of(slot(1, EVENING)), OFFLINE);

        List<MatchProfile> result = filter.filter(caller(), List.of(offline, validCandidate(CONTROL_ID)));

        assertThat(ids(result)).containsExactly(CONTROL_ID);
    }

    @Test
    @DisplayName("TC133 chưa khai lịch, chưa khai hình thức, ANY khi u OFFLINE → cả ba còn; ScheduleFit 0.5, ModeFit 0.5, ModeFit 1.0")
    void tc133_neutralCandidatesAreKept() {
        MatchProfile u = caller(OFFLINE);
        MatchProfile noSlots = candidate(1, UserStatus.ACTIVE, Role.STUDENT,
                List.of(teach(JAVA, 3)), Set.of(), OFFLINE);
        MatchProfile noMode = candidate(2, UserStatus.ACTIVE, Role.STUDENT,
                List.of(teach(JAVA, 3)), Set.of(slot(1, EVENING)), null);
        MatchProfile any = candidate(3, UserStatus.ACTIVE, Role.STUDENT,
                List.of(teach(JAVA, 3)), Set.of(slot(1, EVENING)), ANY);

        List<MatchProfile> result = filter.filter(u, List.of(noSlots, noMode, any));

        // Hàm "loại tất cả" vẫn pass các ca bị loại ở trên, nhưng đỏ ở đây.
        assertThat(ids(result)).containsExactly(1L, 2L, 3L);
        assertThat(calculator.scheduleFit(u.slots(), noSlots.slots())).isCloseTo(0.5, within(TOLERANCE));
        assertThat(calculator.modeFit(u.mode(), noMode.mode())).isCloseTo(0.5, within(TOLERANCE));
        assertThat(calculator.modeFit(u.mode(), any.mode())).isCloseTo(1.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("Spec 4.2 (4) u chưa khai lịch, ứng viên có lịch → còn (chỉ loại khi CẢ HAI đã khai mà không trùng)")
    void spec42_callerWithoutSlotsKeepsCandidate() {
        MatchProfile u = student(CALLER_ID, List.of(learn(JAVA, 3)), List.of(), Set.of(), null, 0.5);

        List<MatchProfile> result = filter.filter(u, List.of(validCandidate(CONTROL_ID)));

        assertThat(ids(result)).containsExactly(CONTROL_ID);
    }

    @Test
    @DisplayName("Spec 4.2 (3) chỉ xét chiều u học từ v: ứng viên không muốn học gì của u vẫn còn (gợi ý một chiều)")
    void spec42_oneWayCandidateIsKept() {
        // validCandidate không muốn học gì, nên SkillFit(v, u) = 0. Lọc đòi cả hai chiều thì ca này đỏ.
        MatchProfile oneWay = validCandidate(CONTROL_ID);

        assertThat(filter.accepts(caller(), oneWay)).isTrue();
    }

    @Test
    @DisplayName("Spec 4.2 (3) kỹ năng liên quan (Sim 0.5) đủ để qua: u muốn Java, ứng viên dạy Kotlin → còn")
    void spec42_relatedSkillPasses() {
        MatchProfile kotlin = candidate(CONTROL_ID, UserStatus.ACTIVE, Role.STUDENT,
                List.of(teach(KOTLIN, 2)), Set.of(slot(1, EVENING)), ONLINE);

        assertThat(filter.accepts(caller(), kotlin)).isTrue();
    }

    @Test
    @DisplayName("Spec 4.2 W_u rỗng → không ai qua lọc (không có gợi ý)")
    void spec42_callerWithoutWantsGetsNothing() {
        MatchProfile u = student(CALLER_ID, List.of(), List.of(teach(KOTLIN, 2)),
                Set.of(slot(1, EVENING)), ONLINE, 0.5);

        assertThat(filter.filter(u, List.of(validCandidate(CONTROL_ID)))).isEmpty();
    }

    @Test
    @DisplayName("Kế hoạch bước 6: filter giữ đúng thứ tự ban đầu của ứng viên")
    void plan_filterKeepsOriginalOrder() {
        List<MatchProfile> candidates = List.of(validCandidate(3), validCandidate(1), validCandidate(2));

        List<MatchProfile> result = filter.filter(caller(), candidates);

        assertThat(ids(result)).containsExactly(3L, 1L, 2L);
    }
}
