package com.skillexchange.matching;

import static com.skillexchange.matching.MatchingFixtures.ENGLISH;
import static com.skillexchange.matching.MatchingFixtures.FIGMA;
import static com.skillexchange.matching.MatchingFixtures.JAVA;
import static com.skillexchange.matching.MatchingFixtures.KOTLIN;
import static com.skillexchange.matching.MatchingFixtures.PYTHON;
import static com.skillexchange.matching.MatchingFixtures.learn;
import static com.skillexchange.matching.MatchingFixtures.slot;
import static com.skillexchange.matching.MatchingFixtures.student;
import static com.skillexchange.matching.MatchingFixtures.teach;
import static com.skillexchange.user.LearningMode.ONLINE;
import static com.skillexchange.user.Period.EVENING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.skillexchange.user.LearningMode;
import com.skillexchange.user.Period;
import com.skillexchange.user.Role;
import com.skillexchange.user.UserStatus;

/**
 * Unit test cho MatchingEngine (spec 4.4, 4.5, D36, D46): D hai chiều, MutualScore, phân loại, xếp hạng,
 * youLearn / theyLearn / commonSlots. Mong đợi viết bằng phép tính (Math.sqrt(...)), không viết số làm tròn.
 */
class MatchingEngineTest {

    private static final double TOLERANCE = 1e-9;

    private final MatchingEngine engine =
            new MatchingEngine(MatchingFixtures.similarity(), MatchingWeights.DEFAULT);

    private static MatchResult only(List<MatchResult> group, long userId) {
        return group.stream().filter(r -> r.userId() == userId).findFirst()
                .orElseThrow(() -> new AssertionError("Không có userId " + userId + " trong nhóm"));
    }

    private static List<Long> ids(List<MatchResult> group) {
        return group.stream().map(MatchResult::userId).toList();
    }

    /** Ứng viên ACTIVE + STUDENT tùy kỹ năng và Reputation; rảnh Tối T2, ONLINE (luôn qua lọc lịch, hình thức với u bên dưới). */
    private static MatchProfile person(long id, List<LearnItem> learn, List<TeachItem> teach, double reputation) {
        return student(id, learn, teach, Set.of(slot(1, EVENING)), ONLINE, reputation);
    }

    // ---------- Ví dụ tính tay ----------

    @Test
    @DisplayName("TC134 An gọi với Bình: D(An, Bình) = 0.6875, D(Bình, An) = 0.925, TWO_WAY, MutualScore = √(0.6875 × 0.925)")
    void tc134_anAndBinhTwoWay() {
        MatchingResult result = engine.match(MatchingFixtures.an(), List.of(MatchingFixtures.binh()));

        assertThat(result.oneWay()).isEmpty();
        MatchResult binh = only(result.twoWay(), MatchingFixtures.BINH_ID);
        assertThat(binh.type()).isEqualTo(MatchType.TWO_WAY);

        ScoreBreakdown forward = binh.forward();
        assertThat(forward.skillFit()).isCloseTo(0.75, within(TOLERANCE));
        assertThat(forward.levelFit()).isCloseTo(0.5, within(TOLERANCE));
        assertThat(forward.scheduleFit()).isCloseTo(2.0 / 3, within(TOLERANCE));
        assertThat(forward.modeFit()).isCloseTo(1.0, within(TOLERANCE));
        assertThat(forward.reputation()).isCloseTo(0.5, within(TOLERANCE));
        assertThat(forward.d()).isCloseTo(0.6875, within(TOLERANCE));

        assertThat(binh.backward().d()).isCloseTo(0.925, within(TOLERANCE));
        // Viết bằng phép tính, không viết 0.7975: số làm tròn tay sai ở chữ số thứ 5.
        assertThat(binh.score()).isCloseTo(Math.sqrt(0.6875 * 0.925), within(TOLERANCE));
    }

    @Test
    @DisplayName("TC135 ví dụ A.3: D(u, v) = 0.94 (code ra 0.9400000000000001, so bằng dung sai)")
    void tc135_appendixA3() {
        MatchingResult result = engine.match(MatchingFixtures.a3Learner(), List.of(MatchingFixtures.a3Teacher()));

        // v không muốn học gì nên cặp này là ONE_WAY; điểm xếp hạng = D(u, v).
        MatchResult v = only(result.oneWay(), MatchingFixtures.A3_V_ID);
        assertThat(v.forward().d()).isCloseTo(0.94, within(TOLERANCE));
        assertThat(v.score()).isCloseTo(0.94, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC142 trọng số (1, 0, 0, 0, 0): D(An, Bình) = SkillFit = 0.75")
    void tc142_customWeightsChangeScore() {
        MatchingEngine skillOnly = new MatchingEngine(MatchingFixtures.similarity(),
                new MatchingWeights(1, 0, 0, 0, 0));

        MatchingResult result = skillOnly.match(MatchingFixtures.an(), List.of(MatchingFixtures.binh()));

        // Ghi cứng 0.55 trong code (không đọc từ weights) thì ra 0.6875 như TC134.
        assertThat(only(result.twoWay(), MatchingFixtures.BINH_ID).forward().d()).isCloseTo(0.75, within(TOLERANCE));
    }

    // ---------- Phân loại ----------

    @Test
    @DisplayName("TC136 ứng viên muốn học thứ u không dạy, và ứng viên có W_v rỗng → cả hai ONE_WAY, điểm = D(u, v)")
    void tc136_oneWayCandidates() {
        MatchProfile u = MatchingFixtures.an(); // dạy Python
        MatchProfile wantsFigma = person(11, List.of(learn(FIGMA, null)), List.of(teach(JAVA, 3)), 0.5);
        MatchProfile wantsNothing = person(12, List.of(), List.of(teach(ENGLISH, 3)), 0.5);

        MatchingResult result = engine.match(u, List.of(wantsFigma, wantsNothing));

        assertThat(result.twoWay()).isEmpty();
        assertThat(ids(result.oneWay())).containsExactlyInAnyOrder(11L, 12L);
        for (MatchResult r : result.oneWay()) {
            assertThat(r.type()).isEqualTo(MatchType.ONE_WAY);
            assertThat(r.score()).isEqualTo(r.forward().d());
            // ONE_WAY không có chiều ngược (D49).
            assertThat(r.backward()).isNull();
            assertThat(r.theyLearn()).isNull();
        }
    }

    @Test
    @DisplayName("TC137 v chỉ dạy Kotlin (liên quan Java, Sim 0.5) và muốn học Python u dạy → vẫn TWO_WAY")
    void tc137_relatedSkillIsStillTwoWay() {
        MatchProfile u = person(100, List.of(learn(JAVA, 3)), List.of(teach(PYTHON, 3)), 0.5);
        MatchProfile v = person(11, List.of(learn(PYTHON, 2)), List.of(teach(KOTLIN, 2)), 0.5);

        MatchingResult result = engine.match(u, List.of(v));

        assertThat(ids(result.twoWay())).containsExactly(11L);
        assertThat(result.oneWay()).isEmpty();
    }

    @Test
    @DisplayName("TC143 u có W_u rỗng → cả hai nhóm rỗng, không ném lỗi")
    void tc143_noWantsNoSuggestions() {
        MatchProfile u = person(100, List.of(), List.of(teach(PYTHON, 3)), 0.5);

        MatchingResult result = engine.match(u, List.of(MatchingFixtures.binh(), MatchingFixtures.an()));

        assertThat(result.twoWay()).isEmpty();
        assertThat(result.oneWay()).isEmpty();
    }

    // ---------- Xếp hạng ----------

    @Test
    @DisplayName("TC140 hai nhóm xếp riêng: TWO_WAY theo MutualScore, ONE_WAY theo D; ONE_WAY có D cao hơn mọi MutualScore vẫn ở nhóm riêng")
    void tc140_groupsAreRankedSeparately() {
        MatchProfile u = person(100, List.of(learn(JAVA, 3)), List.of(teach(PYTHON, 3)), 0.5);
        // Ba ứng viên hai chiều chỉ khác Reputation → MutualScore tăng theo Reputation.
        MatchProfile twoLow = person(11, List.of(learn(PYTHON, 2)), List.of(teach(JAVA, 3)), 0.2);
        MatchProfile twoHigh = person(12, List.of(learn(PYTHON, 2)), List.of(teach(JAVA, 3)), 0.8);
        MatchProfile twoMid = person(13, List.of(learn(PYTHON, 2)), List.of(teach(JAVA, 3)), 0.5);
        // Một chiều: 21 khớp hoàn hảo (D = 1.0), 22 chỉ dạy Kotlin mức 1.
        MatchProfile oneTop = person(21, List.of(), List.of(teach(JAVA, 5)), 1.0);
        MatchProfile oneLow = person(22, List.of(), List.of(teach(KOTLIN, 1)), 0.3);

        // Thứ tự vào cố ý sai, để "giữ nguyên thứ tự" không pass được.
        MatchingResult result = engine.match(u, List.of(oneLow, twoLow, oneTop, twoHigh, twoMid));

        assertThat(ids(result.twoWay())).containsExactly(12L, 13L, 11L);
        assertThat(ids(result.oneWay())).containsExactly(21L, 22L);
        // Hai nhóm không trộn: ứng viên một chiều điểm cao nhất vẫn không vào nhóm hai chiều.
        assertThat(result.oneWay().get(0).score()).isGreaterThan(result.twoWay().get(0).score());
        for (MatchResult r : result.twoWay()) {
            assertThat(r.score()).isCloseTo(Math.sqrt(r.forward().d() * r.backward().d()), within(TOLERANCE));
        }
    }

    // ---------- youLearn, theyLearn, commonSlots (D46) ----------

    @Test
    @DisplayName("TC144 An với Bình: youLearn [Java ← Kotlin, Tiếng Anh ← Tiếng Anh], theyLearn [Python ← Python], commonSlots [Tối T4, Tối T6]")
    void tc144_explanationLists() {
        MatchResult binh = only(engine.match(MatchingFixtures.an(), List.of(MatchingFixtures.binh())).twoWay(),
                MatchingFixtures.BINH_ID);

        assertThat(binh.youLearn()).hasSize(2);
        SkillMatch java = binh.youLearn().get(0);
        assertThat(java.want().skillId()).isEqualTo(JAVA);
        assertThat(java.teach().skillId()).isEqualTo(KOTLIN);
        assertThat(java.exact()).isFalse();
        assertThat(java.teach().level()).isEqualTo(2);
        SkillMatch english = binh.youLearn().get(1);
        assertThat(english.want().skillId()).isEqualTo(ENGLISH);
        assertThat(english.teach().skillId()).isEqualTo(ENGLISH);
        assertThat(english.exact()).isTrue();
        assertThat(english.teach().level()).isEqualTo(3);

        assertThat(binh.theyLearn()).hasSize(1);
        SkillMatch python = binh.theyLearn().get(0);
        assertThat(python.want().skillId()).isEqualTo(PYTHON);
        assertThat(python.teach().skillId()).isEqualTo(PYTHON);
        assertThat(python.exact()).isTrue();
        assertThat(python.teach().level()).isEqualTo(4);

        assertThat(binh.commonSlots()).containsExactly(slot(3, EVENING), slot(5, EVENING));
    }

    @Test
    @DisplayName("D46 youLearn xếp theo tên kỹ năng muốn học, không theo thứ tự nhập; bỏ kỹ năng Sim = 0")
    void d46_youLearnSortedByNameAndSkipsUnmatched() {
        // Nhập Tiếng Anh, Figma, Java: Figma không ai dạy → bỏ; còn lại xếp "Java" < "Tiếng Anh".
        // u chưa khai lịch để Bình (không rảnh Tối T2) không bị lọc cứng loại.
        MatchProfile u = student(100,
                List.of(learn(ENGLISH, null), learn(FIGMA, null), learn(JAVA, 3)),
                List.of(), Set.of(), ONLINE, 0.5);

        MatchResult binh = only(engine.match(u, List.of(MatchingFixtures.binh())).oneWay(), MatchingFixtures.BINH_ID);

        assertThat(binh.youLearn()).extracting(m -> m.want().skillId()).containsExactly(JAVA, ENGLISH);
    }

    @Test
    @DisplayName("D46 commonSlots xếp theo ngày rồi buổi Sáng → Chiều → Tối, không theo chữ cái")
    void d46_commonSlotsSortedByDayThenPeriod() {
        Set<Slot> shared = Set.of(slot(2, EVENING), slot(1, Period.EVENING), slot(1, Period.MORNING),
                slot(1, Period.AFTERNOON));
        MatchProfile u = student(100, List.of(learn(JAVA, 3)), List.of(), shared, ONLINE, 0.5);
        MatchProfile v = student(11, List.of(), List.of(teach(JAVA, 3)), shared, ONLINE, 0.5);

        MatchResult r = only(engine.match(u, List.of(v)).oneWay(), 11);

        // Theo chữ cái thì AFTERNOON < EVENING < MORNING: test này bắt lỗi đó.
        assertThat(r.commonSlots()).containsExactly(
                slot(1, Period.MORNING), slot(1, Period.AFTERNOON), slot(1, Period.EVENING), slot(2, EVENING));
    }

    // ---------- Kiểm thử thuộc tính ----------

    @Test
    @DisplayName("TC141 50 hồ sơ ngẫu nhiên (seed 42): mọi điểm trong [0, 1], không NaN; không ai vi phạm lọc cứng; không sót ai hợp lệ")
    void tc141_randomProfilesProperties() {
        Random random = new Random(42);
        List<MatchProfile> profiles = new ArrayList<>();
        for (long id = 1; id <= 50; id++) {
            profiles.add(randomProfile(random, id));
        }
        SkillSimilarity similarity = MatchingFixtures.similarity();
        RankingComparator ranking = new RankingComparator();
        int totalResults = 0;

        for (MatchProfile u : profiles) {
            MatchingResult result = engine.match(u, profiles);

            List<MatchResult> all = new ArrayList<>(result.twoWay());
            all.addAll(result.oneWay());
            totalResults += all.size();
            for (MatchResult r : all) {
                MatchProfile v = profiles.get((int) r.userId() - 1);
                assertThat(passesHardFilter(u, v, similarity)).as("u=%d v=%d vi phạm lọc cứng", u.userId(), v.userId()).isTrue();
                assertInUnitInterval(r.score(), "score");
                assertBreakdownValid(r.forward());
                if (r.type() == MatchType.TWO_WAY) {
                    assertBreakdownValid(r.backward());
                } else {
                    assertThat(r.backward()).isNull();
                }
            }

            // Chiều ngược: mọi ứng viên hợp lệ đều có mặt (hàm trả rỗng không pass được).
            long expectedCount = profiles.stream().filter(v -> passesHardFilter(u, v, similarity)).count();
            assertThat(all).hasSize((int) expectedCount);

            assertSorted(result.twoWay(), ranking);
            assertSorted(result.oneWay(), ranking);
        }
        // Dữ liệu ngẫu nhiên phải thật sự sinh ra gợi ý, nếu không test trên chỉ kiểm danh sách rỗng.
        assertThat(totalResults).isGreaterThan(50);
    }

    /** Năm điều kiện spec 4.2, viết lại độc lập trong test (không gọi HardFilter). */
    private static boolean passesHardFilter(MatchProfile u, MatchProfile v, SkillSimilarity similarity) {
        if (u.userId() == v.userId()) {
            return false;
        }
        if (v.status() != UserStatus.ACTIVE || v.role() != Role.STUDENT) {
            return false;
        }
        boolean skill = u.learn().stream().anyMatch(want ->
                v.teach().stream().anyMatch(t -> similarity.sim(want.skillId(), t.skillId()) > 0));
        if (!skill) {
            return false;
        }
        if (!u.slots().isEmpty() && !v.slots().isEmpty()) {
            Set<Slot> common = new HashSet<>(u.slots());
            common.retainAll(v.slots());
            if (common.isEmpty()) {
                return false;
            }
        }
        boolean conflict = (u.mode() == LearningMode.ONLINE && v.mode() == LearningMode.OFFLINE)
                || (u.mode() == LearningMode.OFFLINE && v.mode() == LearningMode.ONLINE);
        return !conflict;
    }

    private static void assertBreakdownValid(ScoreBreakdown b) {
        assertInUnitInterval(b.skillFit(), "skillFit");
        assertInUnitInterval(b.levelFit(), "levelFit");
        assertInUnitInterval(b.scheduleFit(), "scheduleFit");
        assertInUnitInterval(b.modeFit(), "modeFit");
        assertInUnitInterval(b.reputation(), "reputation");
        assertInUnitInterval(b.d(), "d");
    }

    private static void assertInUnitInterval(double value, String name) {
        assertThat(Double.isNaN(value)).as(name + " là NaN").isFalse();
        // Dung sai nhỏ cho phép 1.0000000000000002 do cộng số thực.
        assertThat(value).as(name).isBetween(-TOLERANCE, 1 + TOLERANCE);
    }

    private static void assertSorted(List<MatchResult> group, RankingComparator ranking) {
        for (int i = 1; i < group.size(); i++) {
            assertThat(ranking.compare(group.get(i - 1), group.get(i))).isLessThanOrEqualTo(0);
        }
    }

    /** Hồ sơ ngẫu nhiên: có cả tài khoản không ACTIVE, admin, thiếu kỹ năng / lịch / hình thức. */
    private static MatchProfile randomProfile(Random random, long id) {
        UserStatus[] statuses = {UserStatus.ACTIVE, UserStatus.ACTIVE, UserStatus.ACTIVE, UserStatus.ACTIVE,
                UserStatus.ACTIVE, UserStatus.ACTIVE, UserStatus.ACTIVE, UserStatus.ACTIVE,
                UserStatus.PENDING_VERIFICATION, UserStatus.LOCKED};
        UserStatus status = statuses[random.nextInt(statuses.length)];
        Role role = random.nextInt(10) == 0 ? Role.ADMIN : Role.STUDENT;

        List<LearnItem> learn = new ArrayList<>();
        for (long skillId : randomSkillIds(random)) {
            Integer target = random.nextInt(4) == 0 ? null : 1 + random.nextInt(5);
            learn.add(learn(skillId, target));
        }
        List<TeachItem> teach = new ArrayList<>();
        for (long skillId : randomSkillIds(random)) {
            teach.add(teach(skillId, 1 + random.nextInt(5)));
        }
        Set<Slot> slots = new HashSet<>();
        int slotCount = random.nextInt(7); // 0 = chưa khai báo lịch
        for (int i = 0; i < slotCount; i++) {
            slots.add(slot(1 + random.nextInt(7), Period.values()[random.nextInt(3)]));
        }
        LearningMode[] modes = {LearningMode.ONLINE, LearningMode.OFFLINE, LearningMode.ANY, null};
        LearningMode mode = modes[random.nextInt(modes.length)];

        List<Integer> ratings = new ArrayList<>();
        int ratingCount = random.nextInt(4);
        for (int i = 0; i < ratingCount; i++) {
            ratings.add(1 + random.nextInt(5));
        }
        List<SessionOutcome> sessions = new ArrayList<>();
        int sessionCount = random.nextInt(4);
        for (int i = 0; i < sessionCount; i++) {
            SessionStatus s = SessionStatus.values()[random.nextInt(SessionStatus.values().length)];
            boolean started = s != SessionStatus.SCHEDULED && (s != SessionStatus.CANCELLED || random.nextBoolean());
            sessions.add(new SessionOutcome(s, started));
        }
        double reputation = ReputationCalculator.compute(ratings, sessions);

        return new MatchProfile(id, status, role, learn, teach, slots, mode, reputation);
    }

    /** 0–3 kỹ năng khác nhau trong 10 kỹ năng của MatchingFixtures. */
    private static Set<Long> randomSkillIds(Random random) {
        Set<Long> ids = new HashSet<>();
        int count = random.nextInt(4);
        while (ids.size() < count) {
            ids.add(1L + random.nextInt(10));
        }
        return ids;
    }
}
