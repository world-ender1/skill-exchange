package com.skillexchange.evaluation;

import static com.skillexchange.evaluation.EvaluationFixtures.CATALOG;
import static com.skillexchange.evaluation.EvaluationFixtures.evening;
import static com.skillexchange.evaluation.EvaluationFixtures.generatedProfiles;
import static com.skillexchange.evaluation.EvaluationFixtures.ids;
import static com.skillexchange.evaluation.EvaluationFixtures.learn;
import static com.skillexchange.evaluation.EvaluationFixtures.morning;
import static com.skillexchange.evaluation.EvaluationFixtures.profile;
import static com.skillexchange.evaluation.EvaluationFixtures.teach;
import static com.skillexchange.evaluation.ScheduleFitFormula.JACCARD;
import static com.skillexchange.evaluation.ScheduleFitFormula.MIN;
import static com.skillexchange.evaluation.ScheduleFitFormula.TRAN_3;
import static com.skillexchange.user.LearningMode.ANY;
import static com.skillexchange.user.LearningMode.OFFLINE;
import static com.skillexchange.user.LearningMode.ONLINE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.skillexchange.matching.LearnItem;
import com.skillexchange.matching.MatchProfile;
import com.skillexchange.matching.MatchResult;
import com.skillexchange.matching.MatchingEngine;
import com.skillexchange.matching.MatchingResult;
import com.skillexchange.matching.MatchingWeights;
import com.skillexchange.matching.SkillSimilarity;
import com.skillexchange.matching.TeachItem;

/** Các phương án B0, B1, R, P dựng từ lõi matching: TC234–TC240 (spec 5.2; D56, D64, D65). */
class RankersTest {

    private final Rankers rankers = new Rankers(CATALOG.relations());

    // ---------- Dữ liệu dựng tay ----------

    @Test
    @DisplayName("TC234 u chỉ học Java; a dạy Java, b chỉ dạy Kotlin: B0 = [a]; B1, R, P có cả a và b")
    void tc234_relatedSkillOnlyInExpandedSet() {
        MatchProfile u = profile(1, List.of(learn("Java", null)), List.of(), Set.of(), null);
        MatchProfile a = profile(2, List.of(), List.of(teach("Java", 2)), Set.of(), null);
        MatchProfile b = profile(3, List.of(), List.of(teach("Kotlin", 2)), Set.of(), null);
        List<MatchProfile> all = List.of(u, a, b);

        assertThat(rankers.b0().rank(u, all)).containsExactly(2L);
        assertThat(rankers.b1().rank(u, all)).containsExactlyInAnyOrder(2L, 3L);
        assertThat(rankers.random(1).rank(u, all)).containsExactlyInAnyOrder(2L, 3L);
        assertThat(rankers.p(MatchingWeights.DEFAULT, MIN).rank(u, all)).containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    @DisplayName("TC235 B0 = [3, 1, 2]: mã 3 trùng 2 kỹ năng; mã 1 và 2 cùng trùng 1 nên mã nhỏ trước")
    void tc235_b0RanksByExactMatchCount() {
        // Mục tiêu 3 và trình độ khác nhau để P xếp mã 2 (LevelFit 1) trước mã 1 (LevelFit 0):
        // B0 dùng nhầm D của P thì ra [3, 2, 1] và test đỏ.
        MatchProfile u = profile(10, List.of(learn("Java", 3), learn("SQL", 3)), List.of(), Set.of(), null);
        MatchProfile m1 = profile(1, List.of(), List.of(teach("SQL", 1)), Set.of(), null);
        MatchProfile m2 = profile(2, List.of(), List.of(teach("Java", 4)), Set.of(), null);
        MatchProfile m3 = profile(3, List.of(), List.of(teach("Java", 4), teach("SQL", 4)), Set.of(), null);
        List<MatchProfile> all = List.of(u, m1, m2, m3);

        assertThat(rankers.b0().rank(u, all)).containsExactly(3L, 1L, 2L);
        assertThat(rankers.p(MatchingWeights.DEFAULT, MIN).rank(u, all)).containsExactly(3L, 2L, 1L);
    }

    @Test
    @DisplayName("TC236 B1 = [3, 1, 2]: SkillFit 0.75, 0.5, 0.5, bằng điểm thì mã nhỏ trước")
    void tc236_b1RanksBySkillFit() {
        // Như TC235: P xếp mã 2 (LevelFit 1) trước mã 1 (LevelFit 0), còn B1 chỉ nhìn SkillFit.
        MatchProfile u = profile(10, List.of(learn("Java", 3), learn("SQL", 3)), List.of(), Set.of(), null);
        MatchProfile m1 = profile(1, List.of(), List.of(teach("Java", 1)), Set.of(), null);                       // (1 + 0) / 2
        MatchProfile m2 = profile(2, List.of(), List.of(teach("Kotlin", 4), teach("MySQL", 4)), Set.of(), null);  // (0.5 + 0.5) / 2
        MatchProfile m3 = profile(3, List.of(), List.of(teach("Java", 4), teach("MySQL", 4)), Set.of(), null);    // (1 + 0.5) / 2
        List<MatchProfile> all = List.of(u, m1, m2, m3);

        assertThat(rankers.b1().rank(u, all)).containsExactly(3L, 1L, 2L);
        assertThat(rankers.p(MatchingWeights.DEFAULT, MIN).rank(u, all)).containsExactly(3L, 2L, 1L);
    }

    @Test
    @DisplayName("D65 B1 xếp ứng viên hai chiều theo SkillFit chiều u học từ v, không theo MutualScore, không đặt nhóm hai chiều lên trước")
    void d65_b1UsesForwardSkillFitForTwoWayCandidates() {
        // u học Java, SQL, Piano; dạy Python.
        MatchProfile u = profile(10, List.of(learn("Java", null), learn("SQL", null), learn("Piano", null)),
                List.of(teach("Python", 3)), Set.of(), null);
        // Mã 1, một chiều: SkillFit = (1 + 1 + 0) / 3.
        MatchProfile m1 = profile(1, List.of(learn("Guitar", null)),
                List.of(teach("Java", 3), teach("SQL", 3)), Set.of(), null);
        // Mã 2, hai chiều (học Python của u): SkillFit = (1 + 0.5 + 0) / 3 = 0.5, chiều ngược 1,
        // nên MutualScore = sqrt(0.5) ≈ 0.707 > 2/3. Dùng nhầm MutualScore hoặc giữ thứ tự hai nhóm của lõi thì ra [2, 1].
        MatchProfile m2 = profile(2, List.of(learn("Python", null)),
                List.of(teach("Java", 3), teach("MySQL", 3)), Set.of(), null);
        List<MatchProfile> all = List.of(u, m1, m2);

        assertThat(rankers.b1().rank(u, all)).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("TC238 Lọc lịch và hình thức chung cho mọi phương án: a (OFFLINE) và b (không trùng khung) không có trong B0, B1, R, P")
    void tc238_sharedScheduleAndModeFilter() {
        MatchProfile u = profile(1, List.of(learn("Java", null)), List.of(), Set.of(evening(1), evening(2)), ONLINE);
        MatchProfile a = profile(2, List.of(), List.of(teach("Java", 3)), Set.of(evening(1)), OFFLINE);
        MatchProfile b = profile(3, List.of(), List.of(teach("Java", 3)), Set.of(morning(1)), ANY);
        // c hợp lệ: danh sách không rỗng, nên test không qua được chỉ vì phương án trả danh sách rỗng.
        MatchProfile c = profile(4, List.of(), List.of(teach("Java", 3)), Set.of(evening(2)), ONLINE);
        List<MatchProfile> all = List.of(u, a, b, c);

        for (Map.Entry<String, Ranker> entry : allRankers(1).entrySet()) {
            assertThat(entry.getValue().rank(u, all)).as(entry.getKey()).containsExactly(4L);
        }
    }

    @Test
    @DisplayName("TC240 5 vị trí đầu của P = 3 ứng viên hai chiều theo thứ tự lõi, rồi 2 ứng viên một chiều đầu tiên")
    void tc240_twoWayGroupBeforeOneWayGroup() {
        // u học Java (mục tiêu 3), dạy Python; không khai lịch, hình thức.
        MatchProfile u = profile(1, List.of(learn("Java", 3)), List.of(teach("Python", 3)), Set.of(), null);
        List<MatchProfile> all = List.of(u,
                // Hai chiều (học Python của u), chỉ dạy Kotlin nên SkillFit 0.5.
                learner(2, "Kotlin", 2, "Python"),
                learner(3, "Kotlin", 3, "Python"),
                learner(4, "Kotlin", 1, "Python"),
                // Một chiều (học Piano, u không dạy). Mã 5 dạy đúng Java đạt mục tiêu: D cao nhất.
                learner(5, "Java", 3, "Piano"),
                learner(6, "Java", 2, "Piano"),
                learner(7, "Kotlin", 3, "Piano"),
                learner(8, "Kotlin", 1, "Piano"));

        MatchingResult core = new MatchingEngine(new SkillSimilarity(CATALOG.relations()), MatchingWeights.DEFAULT)
                .match(u, all);
        // Điều kiện của ca: 3 hai chiều, 4 một chiều, và D của một ứng viên một chiều cao hơn mọi MutualScore.
        assertThat(core.twoWay()).hasSize(3);
        assertThat(core.oneWay()).hasSize(4);
        double bestMutual = core.twoWay().stream().mapToDouble(MatchResult::score).max().orElseThrow();
        assertThat(core.oneWay().get(0).score()).isGreaterThan(bestMutual);

        List<Long> expectedTop5 = new ArrayList<>(ids(core.twoWay()));
        expectedTop5.addAll(ids(core.oneWay()).subList(0, 2));
        List<Long> p = rankers.p(MatchingWeights.DEFAULT, MIN).rank(u, all);

        assertThat(p.subList(0, 5)).isEqualTo(expectedTop5).containsExactly(3L, 2L, 4L, 5L, 6L);
        // Mọi biến thể của P giữ cấu trúc hai nhóm (D56): 3 vị trí đầu luôn là nhóm hai chiều.
        for (Map.Entry<String, Ranker> entry : pVariants().entrySet()) {
            assertThat(entry.getValue().rank(u, all).subList(0, 3)).as(entry.getKey())
                    .containsExactlyInAnyOrder(2L, 3L, 4L);
        }
    }

    /** Ứng viên chỉ dạy một kỹ năng và chỉ muốn học một kỹ năng (không đặt mục tiêu). */
    private static MatchProfile learner(long id, String teaches, int level, String wants) {
        List<LearnItem> learn = List.of(learn(wants, null));
        List<TeachItem> teach = List.of(teach(teaches, level));
        return profile(id, learn, teach, Set.of(), null);
    }

    // ---------- Dữ liệu giả lập N = 200 ----------

    @Test
    @DisplayName("TC237 N = 200, seed 1: tập ứng viên của B1, R, P bằng nhau; B0 là tập con của B1, mỗi ứng viên B0 trùng chính xác ít nhất một kỹ năng")
    void tc237_candidateSets() {
        List<MatchProfile> profiles = generatedProfiles(200, 1, NoiseLevel.MAC_DINH);
        Map<Long, MatchProfile> byId = byId(profiles);
        Map<String, Ranker> sameSetAsB1 = new LinkedHashMap<>();
        sameSetAsB1.put("R", rankers.random(1));
        sameSetAsB1.putAll(pVariants());
        Ranker b0 = rankers.b0();
        Ranker b1 = rankers.b1();

        for (MatchProfile u : profiles) {
            List<Long> b1List = b1.rank(u, profiles);
            Set<Long> b1Set = new HashSet<>(b1List);
            assertThat(b1List).as("B1 không lặp, người %d", u.userId()).hasSameSizeAs(b1Set);

            for (Map.Entry<String, Ranker> entry : sameSetAsB1.entrySet()) {
                assertThat(entry.getValue().rank(u, profiles)).as("%s, người %d", entry.getKey(), u.userId())
                        .containsExactlyInAnyOrderElementsOf(b1List);
            }

            List<Long> b0List = b0.rank(u, profiles);
            assertThat(b1Set).as("B0 ⊆ B1, người %d", u.userId()).containsAll(b0List);
            for (long v : b0List) {
                assertThat(hasExactSkill(u, byId.get(v))).as("B0 người %d, ứng viên %d", u.userId(), v).isTrue();
            }
        }
    }

    @Test
    @DisplayName("D65 N = 200, seed 1: B0 gồm đúng các ứng viên của B1 trùng chính xác ít nhất một kỹ năng")
    void d65_b0IsExactlyTheExactMatchesOfB1() {
        List<MatchProfile> profiles = generatedProfiles(200, 1, NoiseLevel.MAC_DINH);
        Map<Long, MatchProfile> byId = byId(profiles);
        Ranker b0 = rankers.b0();
        Ranker b1 = rankers.b1();
        boolean relationAddsCandidates = false;

        for (MatchProfile u : profiles) {
            List<Long> b1List = b1.rank(u, profiles);
            List<Long> exactInB1 = b1List.stream().filter(v -> hasExactSkill(u, byId.get(v))).toList();

            assertThat(b0.rank(u, profiles)).as("người %d", u.userId()).containsExactlyInAnyOrderElementsOf(exactInB1);
            relationAddsCandidates |= exactInB1.size() < b1List.size();
        }
        // Quan hệ liên quan phải thêm được ứng viên ở ít nhất một người, nếu không B0 = B1 và ca không kiểm gì.
        assertThat(relationAddsCandidates).isTrue();
    }

    @Test
    @DisplayName("D56 N = 200, seed 1: P (MIN) đúng như lõi trả về với cùng bộ trọng số, nhóm hai chiều rồi nhóm một chiều, ở cả 7 bộ")
    void d56_pIsExactlyCoreOutput() {
        List<MatchProfile> profiles = generatedProfiles(200, 1, NoiseLevel.MAC_DINH);
        List<WeightSets.Named> weightSets = new ArrayList<>(WeightSets.SENSITIVITY);
        weightSets.addAll(WeightSets.ABLATION);

        // Chạy mọi bộ: P phải dùng bộ trọng số được truyền vào, không ghi cứng bộ mặc định.
        for (WeightSets.Named named : weightSets) {
            MatchingEngine engine = new MatchingEngine(new SkillSimilarity(CATALOG.relations()), named.weights());
            Ranker p = rankers.p(named.weights(), MIN);

            for (MatchProfile u : profiles) {
                MatchingResult core = engine.match(u, profiles);
                List<Long> expected = new ArrayList<>(ids(core.twoWay()));
                expected.addAll(ids(core.oneWay()));

                assertThat(p.rank(u, profiles)).as("%s, người %d", named.phuongAn(), u.userId()).isEqualTo(expected);
            }
        }
    }

    @Test
    @DisplayName("TC239 N = 200: R seed 1 chạy hai lần cùng thứ tự; tập của R bằng B1; R seed 2 khác thứ tự ở ít nhất một người")
    void tc239_randomIsReproducible() {
        // Cùng một bộ dữ liệu (seed 1) cho cả ba lượt; chỉ đổi seed của luồng R (kế hoạch bước 6).
        List<MatchProfile> profiles = generatedProfiles(200, 1, NoiseLevel.MAC_DINH);
        Ranker b1 = rankers.b1();
        Ranker r1 = rankers.random(1);
        Ranker r1Again = rankers.random(1);
        Ranker r2 = rankers.random(2);

        Map<Long, List<Long>> firstRun = new LinkedHashMap<>();
        for (MatchProfile u : profiles) {
            firstRun.put(u.userId(), r1.rank(u, profiles));
        }

        int differentWithSeed2 = 0;
        // Lượt hai đi ngược thứ tự người dùng: kết quả của một người không được phụ thuộc người gọi trước nó.
        List<MatchProfile> reversed = new ArrayList<>(profiles);
        Collections.reverse(reversed);
        for (MatchProfile u : reversed) {
            List<Long> first = firstRun.get(u.userId());
            assertThat(r1.rank(u, profiles)).as("cùng Ranker gọi lại, người %d", u.userId()).isEqualTo(first);
            assertThat(r1Again.rank(u, profiles)).as("Ranker mới cùng seed, người %d", u.userId()).isEqualTo(first);
            assertThat(first).as("tập R = B1, người %d", u.userId())
                    .containsExactlyInAnyOrderElementsOf(b1.rank(u, profiles));
            if (!r2.rank(u, profiles).equals(first)) {
                differentWithSeed2++;
            }
        }
        assertThat(differentWithSeed2).isPositive();
    }

    @Test
    @DisplayName("D64 R xáo ứng viên của B1 (xếp theo mã) bằng new Random(seed + 2_000_000 + userId)")
    void d64_randomUsesPerUserStream() {
        List<MatchProfile> profiles = generatedProfiles(200, 1, NoiseLevel.MAC_DINH);
        Ranker b1 = rankers.b1();
        Ranker r1 = rankers.random(1);
        int checkedWithManyCandidates = 0;

        for (MatchProfile u : profiles) {
            List<Long> expected = new ArrayList<>(b1.rank(u, profiles));
            Collections.sort(expected);
            Collections.shuffle(expected, new Random(1 + 2_000_000 + u.userId()));

            assertThat(r1.rank(u, profiles)).as("người %d", u.userId()).isEqualTo(expected);
            if (expected.size() >= 3) {
                checkedWithManyCandidates++;
            }
        }
        assertThat(checkedWithManyCandidates).isPositive();
    }

    // ---------- Tiện ích ----------

    /** Mọi phương án của bước 6, có tên để thông báo lỗi chỉ ra phương án nào sai. */
    @Test
    @DisplayName("D97 N = 200, seed 1: P một danh sách có đúng tập ứng viên của P, xếp chung theo D chiều u học từ v, không đặt nhóm hai chiều lên trước")
    void d97_pSingleListRanksByForwardD() {
        List<MatchProfile> profiles = generatedProfiles(200, 1, NoiseLevel.MAC_DINH);
        MatchingEngine engine = new MatchingEngine(new SkillSimilarity(CATALOG.relations()), MatchingWeights.DEFAULT);
        Ranker single = rankers.pSingleList(MatchingWeights.DEFAULT);
        Ranker p = rankers.p(MatchingWeights.DEFAULT, MIN);
        int differentFromP = 0;

        for (MatchProfile u : profiles) {
            MatchingResult core = engine.match(u, profiles);
            Map<Long, Double> forwardD = new LinkedHashMap<>();
            for (MatchResult r : core.twoWay()) {
                forwardD.put(r.userId(), r.forward().d());
            }
            for (MatchResult r : core.oneWay()) {
                forwardD.put(r.userId(), r.forward().d());
            }
            List<Long> actual = single.rank(u, profiles);
            List<Long> pList = p.rank(u, profiles);

            assertThat(actual).as("người %d", u.userId()).containsExactlyInAnyOrderElementsOf(pList);
            // Kiểm từng cặp kề nhau: D không tăng (dung sai 1e-9, D36); bằng điểm thì mã nhỏ trước.
            for (int i = 1; i < actual.size(); i++) {
                double before = forwardD.get(actual.get(i - 1));
                double now = forwardD.get(actual.get(i));
                assertThat(now).as("người %d, vị trí %d", u.userId(), i).isLessThanOrEqualTo(before + 1e-9);
                if (Math.abs(before - now) <= 1e-9) {
                    assertThat(actual.get(i)).isGreaterThan(actual.get(i - 1));
                }
            }
            if (!actual.equals(pList)) {
                differentFromP++;
            }
        }
        // Có người mà thứ tự khác P: so P với P một danh sách mới đo được ảnh hưởng của việc chia hai nhóm.
        assertThat(differentFromP).isPositive();
    }

    @Test
    @DisplayName("D99 N = 200, seed 1: twoWayIds đúng là các ứng viên lõi xếp vào nhóm hai chiều, như nhau với mọi bộ trọng số")
    void d99_twoWayIdsMatchCoreGroupForEveryWeightSet() {
        List<MatchProfile> profiles = generatedProfiles(200, 1, NoiseLevel.MAC_DINH);
        for (WeightSets.Named named : List.of(WeightSets.MAC_DINH, WeightSets.KY_NANG, WeightSets.BO_LEVEL, WeightSets.BO_LEVEL_TI_LE)) {
            MatchingEngine engine = new MatchingEngine(new SkillSimilarity(CATALOG.relations()), named.weights());
            for (MatchProfile u : profiles) {
                assertThat(rankers.twoWayIds(u, profiles)).as("%s, người %d", named.phuongAn(), u.userId())
                        .containsExactlyInAnyOrderElementsOf(ids(engine.match(u, profiles).twoWay()));
            }
        }
    }

    private Map<String, Ranker> allRankers(long seed) {
        Map<String, Ranker> all = new LinkedHashMap<>();
        all.put("B0", rankers.b0());
        all.put("B1", rankers.b1());
        all.put("R", rankers.random(seed));
        all.putAll(pVariants());
        return all;
    }

    /** P với 7 bộ trọng số (công thức MIN) và với 2 công thức còn lại (bộ mặc định). */
    private Map<String, Ranker> pVariants() {
        Map<String, Ranker> variants = new LinkedHashMap<>();
        List<WeightSets.Named> weightSets = new ArrayList<>(WeightSets.SENSITIVITY);
        weightSets.addAll(WeightSets.ABLATION);
        for (WeightSets.Named named : weightSets) {
            variants.put(named.phuongAn(), rankers.p(named.weights(), MIN));
        }
        variants.put("P_sf_jaccard", rankers.p(MatchingWeights.DEFAULT, JACCARD));
        variants.put("P_sf_tran3", rankers.p(MatchingWeights.DEFAULT, TRAN_3));
        return variants;
    }

    private static Map<Long, MatchProfile> byId(List<MatchProfile> profiles) {
        Map<Long, MatchProfile> byId = new LinkedHashMap<>();
        for (MatchProfile profile : profiles) {
            byId.put(profile.userId(), profile);
        }
        return byId;
    }

    /** v dạy đúng một kỹ năng mà u muốn học (Sim = 1.0). */
    private static boolean hasExactSkill(MatchProfile u, MatchProfile v) {
        for (LearnItem want : u.learn()) {
            for (TeachItem t : v.teach()) {
                if (want.skillId() == t.skillId()) {
                    return true;
                }
            }
        }
        return false;
    }
}
