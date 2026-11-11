package com.skillexchange.evaluation;

import static com.skillexchange.evaluation.NoiseLevel.CAO;
import static com.skillexchange.evaluation.NoiseLevel.KHONG;
import static com.skillexchange.evaluation.NoiseLevel.MAC_DINH;
import static com.skillexchange.evaluation.SyntheticDataGenerator.generate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.LongStream;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.skillexchange.matching.LearnItem;
import com.skillexchange.matching.MatchProfile;
import com.skillexchange.matching.Slot;
import com.skillexchange.matching.TeachItem;
import com.skillexchange.user.LearningMode;
import com.skillexchange.user.Role;
import com.skillexchange.user.UserStatus;

/**
 * Bộ sinh dữ liệu giả lập: TC201, TC202, TC204–TC209 (spec 5.4; D53, D63, D64).
 *
 * Các ca kiểm tính chất của dữ liệu (khoảng giá trị, tỷ lệ, quan hệ giữa các mức nhiễu),
 * không chép dữ liệu cụ thể của một seed vào test.
 * Ca nào có vòng lặp "với mọi ..." thì đếm thêm số phần tử đã xét, để test không xanh vì không xét gì.
 */
class SyntheticDataGeneratorTest {

    /** Mức riêng cho TC206 và TC207 (spec 5.4 cho phép mọi bộ xác suất trong [0, 1]). */
    private static final NoiseLevel ONLY_LEVEL = new NoiseLevel("chi_lech_trinh_do", 1, 0, 0);
    private static final NoiseLevel ONLY_MISS = new NoiseLevel("chi_bo_sot", 0, 1, 0);

    @Test
    @DisplayName("TC201 cùng N, seed, mức nhiễu thì hai lần sinh giống hệt nhau từng người; seed 2 cho bộ khác")
    void tc201_sameSeedSameData() {
        List<SimUser> first = generate(100, 1, MAC_DINH);
        List<SimUser> second = generate(100, 1, MAC_DINH);
        List<SimUser> otherSeed = generate(100, 2, MAC_DINH);

        // equals của record so mọi trường, kể cả từng phần tử của các List bên trong.
        assertThat(second).isEqualTo(first);
        assertThat(otherSeed).isNotEqualTo(first);
    }

    @Test
    @DisplayName("TC202 seed 1: giá trị thật giống nhau ở ba mức nhiễu; trình độ lệch ở mức mặc định cũng lệch cùng hướng ở mức cao")
    void tc202_trueValuesSharedAcrossNoiseLevels() {
        List<SimUser> none = generate(100, 1, KHONG);
        List<SimUser> standard = generate(100, 1, MAC_DINH);
        List<SimUser> high = generate(100, 1, CAO);

        assertThat(trueValues(standard)).isEqualTo(trueValues(none));
        assertThat(trueValues(high)).isEqualTo(trueValues(none));

        int shiftedAtStandard = 0;
        for (int i = 0; i < standard.size(); i++) {
            List<SimTeach> standardTeach = standard.get(i).teach();
            List<SimTeach> highTeach = high.get(i).teach();
            for (int k = 0; k < standardTeach.size(); k++) {
                SimTeach item = standardTeach.get(k);
                if (item.declaredLevel() != item.trueLevel()) {
                    shiftedAtStandard++;
                    // Lệch đúng 1 bậc nên "cùng hướng" nghĩa là cùng trình độ tự khai.
                    assertThat(highTeach.get(k).declaredLevel())
                            .as("người %d, kỹ năng %d", standard.get(i).id(), item.skillId())
                            .isEqualTo(item.declaredLevel());
                }
            }
        }
        assertThat(shiftedAtStandard).isPositive();
    }

    @Test
    @DisplayName("TC204 N = 1000: số kỹ năng, trình độ, mục tiêu, số khung, mã và trạng thái nằm trong khoảng của spec 5.4")
    void tc204_valuesWithinSpecRanges() {
        List<SimUser> users = generate(1000, 1, MAC_DINH);

        assertThat(users).extracting(SimUser::id)
                .containsExactlyElementsOf(LongStream.rangeClosed(1, 1000).boxed().toList());

        for (SimUser user : users) {
            List<Long> teachSkills = user.teach().stream().map(SimTeach::skillId).toList();
            List<Long> learnSkills = user.learn().stream().map(SimLearn::skillId).toList();
            String who = "người " + user.id();

            assertThat(teachSkills).as(who).hasSizeBetween(2, 5).doesNotHaveDuplicates()
                    .allSatisfy(id -> assertThat(id).isBetween(1L, 26L));
            assertThat(learnSkills).as(who).hasSizeBetween(1, 3).doesNotHaveDuplicates()
                    .doesNotContainAnyElementsOf(teachSkills)
                    .allSatisfy(id -> assertThat(id).isBetween(1L, 26L));

            for (SimTeach item : user.teach()) {
                assertThat(item.trueLevel()).as(who).isBetween(1, 4);
                assertThat(item.declaredLevel()).as(who).isBetween(1, 4);
                assertThat(Math.abs(item.declaredLevel() - item.trueLevel())).as(who).isLessThanOrEqualTo(1);
            }
            for (SimLearn item : user.learn()) {
                if (item.target() != null) {
                    assertThat(item.target()).as(who).isBetween(1, 4);
                }
            }
            if (!user.trueSlots().isEmpty()) {
                assertThat(user.trueSlots()).as(who).hasSizeBetween(3, 9).doesNotHaveDuplicates();
            }
            assertThat(user.declaredSlots()).as(who).doesNotHaveDuplicates();

            MatchProfile profile = user.toMatchProfile(SkillCatalog.V2);
            assertThat(profile.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(profile.role()).isEqualTo(Role.STUDENT);
            assertThat(profile.reputation()).isEqualTo(0.5);
        }
    }

    @Test
    @DisplayName("TC205 nhiễu (0, 0, 0): trình độ tự khai = thật, lịch tự khai = thật")
    void tc205_noNoiseDeclaredEqualsTrue() {
        for (SimUser user : generate(200, 1, KHONG)) {
            for (SimTeach item : user.teach()) {
                assertThat(item.declaredLevel()).as("người %d", user.id()).isEqualTo(item.trueLevel());
            }
            assertThat(user.declaredSlots()).as("người %d", user.id()).isEqualTo(user.trueSlots());
        }
    }

    @Test
    @DisplayName("TC206 nhiễu (1, 0, 0): thật 1 thành 2, thật 4 thành 3, thật 2 hoặc 3 lệch đúng 1 bậc; lịch không đổi")
    void tc206_levelNoiseAlwaysShiftsOneStep() {
        int seenOne = 0;
        int seenFour = 0;
        int seenMiddle = 0;
        for (SimUser user : generate(200, 1, ONLY_LEVEL)) {
            for (SimTeach item : user.teach()) {
                String who = "người " + user.id() + ", kỹ năng " + item.skillId();
                switch (item.trueLevel()) {
                    case 1 -> {
                        seenOne++;
                        assertThat(item.declaredLevel()).as(who).isEqualTo(2);
                    }
                    case 4 -> {
                        seenFour++;
                        assertThat(item.declaredLevel()).as(who).isEqualTo(3);
                    }
                    default -> {
                        seenMiddle++;
                        assertThat(Math.abs(item.declaredLevel() - item.trueLevel())).as(who).isEqualTo(1);
                    }
                }
            }
            assertThat(user.declaredSlots()).as("người %d", user.id()).isEqualTo(user.trueSlots());
        }
        assertThat(seenOne).isPositive();
        assertThat(seenFour).isPositive();
        assertThat(seenMiddle).isPositive();
    }

    @Test
    @DisplayName("TC207 nhiễu (0, 1, 0): người có khai lịch còn đúng 1 khung tự khai, thuộc lịch thật; người không khai vẫn rỗng")
    void tc207_missNoiseKeepsExactlyOneTrueSlot() {
        int declarers = 0;
        int nonDeclarers = 0;
        for (SimUser user : generate(200, 1, ONLY_MISS)) {
            if (user.trueSlots().isEmpty()) {
                nonDeclarers++;
                assertThat(user.declaredSlots()).as("người %d", user.id()).isEmpty();
            } else {
                declarers++;
                assertThat(user.declaredSlots()).as("người %d", user.id()).hasSize(1);
                assertThat(user.trueSlots()).as("người %d", user.id()).contains(user.declaredSlots().get(0));
            }
        }
        assertThat(declarers).isPositive();
        assertThat(nonDeclarers).isPositive();
    }

    @Test
    @DisplayName("TC208 nhiễu cao: người không khai lịch, hình thức vẫn để trống ở cả hai lớp; kỹ năng, mục tiêu, hình thức giống nhau ở hai lớp")
    void tc208_undeclaredStaysEmptyUnderHighNoise() {
        int noSchedule = 0;
        int noMode = 0;
        for (SimUser user : generate(1000, 1, CAO)) {
            String who = "người " + user.id();
            // Lớp tự khai là thứ lõi matching thấy: MatchProfile dựng từ người này.
            MatchProfile profile = user.toMatchProfile(SkillCatalog.V2);

            if (user.trueSlots().isEmpty()) {
                noSchedule++;
                assertThat(user.declaredSlots()).as(who).isEmpty();
                assertThat(profile.slots()).as(who).isEmpty();
            }
            if (user.mode() == null) {
                noMode++;
                assertThat(profile.mode()).as(who).isNull();
            }
            assertThat(profile.teach()).as(who).extracting(TeachItem::skillId)
                    .containsExactlyElementsOf(user.teach().stream().map(SimTeach::skillId).toList());
            assertThat(profile.learn()).as(who).extracting(LearnItem::skillId, LearnItem::targetLevel)
                    .containsExactlyElementsOf(learnTuples(user));
            assertThat(profile.mode()).as(who).isEqualTo(user.mode());
        }
        assertThat(noSchedule).isPositive();
        assertThat(noMode).isPositive();
    }

    @Test
    @DisplayName("TC209 N = 1000: không khai lịch, không khai hình thức 0.15 ± 0.05; mục tiêu bỏ trống 0.30 ± 0.05; kỹ năng nhiều người dạy nhất gấp ≥ 3 kỹ năng ít nhất")
    void tc209_ratesAndZipfSkew() {
        List<SimUser> users = generate(1000, 1, MAC_DINH);

        double noScheduleRate = users.stream().filter(u -> u.trueSlots().isEmpty()).count() / 1000.0;
        double noModeRate = users.stream().filter(u -> u.mode() == null).count() / 1000.0;
        List<SimLearn> allLearn = users.stream().flatMap(u -> u.learn().stream()).toList();
        double blankTargetRate = allLearn.stream().filter(l -> l.target() == null).count() / (double) allLearn.size();

        assertThat(noScheduleRate).isCloseTo(0.15, within(0.05));
        assertThat(noModeRate).isCloseTo(0.15, within(0.05));
        assertThat(blankTargetRate).isCloseTo(0.30, within(0.05));

        long[] teachers = new long[27]; // chỉ số 1..26 = mã kỹ năng
        for (SimUser user : users) {
            for (SimTeach item : user.teach()) {
                teachers[(int) item.skillId()]++;
            }
        }
        long most = LongStream.rangeClosed(1, 26).map(id -> teachers[(int) id]).max().orElseThrow();
        long least = LongStream.rangeClosed(1, 26).map(id -> teachers[(int) id]).min().orElseThrow();
        assertThat(most).as("nhiều nhất %d, ít nhất %d", most, least).isGreaterThanOrEqualTo(3 * least);
    }

    @Test
    @DisplayName("D63 n < 1 bị từ chối")
    void d63_nonPositiveSizeRejected() {
        assertThatThrownBy(() -> generate(0, 1, MAC_DINH)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> generate(-5, 1, MAC_DINH)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Không tự kiểm được trong một JVM: lỗi rút số trong lúc duyệt Set chỉ lộ ra khi so hai lần chạy JVM.
     * Test chỉ in dấu vân tay; người chạy so bằng mắt giữa hai lệnh mvnw riêng (kế hoạch bước 2, "Tự kiểm").
     */
    @Test
    @DisplayName("D64 in dấu vân tay bộ N = 100, seed 1, nhiễu mặc định để so giữa hai lần chạy JVM")
    void d64_printFingerprint() throws NoSuchAlgorithmException {
        List<SimUser> users = generate(100, 1, MAC_DINH);

        // Băm chuỗi toString (chỉ gồm số, tên enum, List theo thứ tự), không dùng hashCode của record:
        // hashCode của enum đổi theo từng lần chạy JVM, nên băm theo hashCode thì hai lần chạy luôn khác.
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(users.toString().getBytes(StandardCharsets.UTF_8));
        String fingerprint = HexFormat.of().formatHex(digest).substring(0, 16);
        System.out.println("[D64] Dấu vân tay N=100 seed=1 mac_dinh: " + fingerprint);

        assertThat(fingerprint).hasSize(16);
    }

    // ---------- Hàm tiện ích ----------

    /** Phần không có nhiễu và phần thật của một người: phải giống nhau ở mọi mức nhiễu (D64). */
    private record TrueValues(
            long id,
            List<Long> teachSkills,
            List<Integer> trueLevels,
            List<SimLearn> learn,
            List<Slot> trueSlots,
            LearningMode mode) {
    }

    private static List<TrueValues> trueValues(List<SimUser> users) {
        return users.stream()
                .map(u -> new TrueValues(
                        u.id(),
                        u.teach().stream().map(SimTeach::skillId).toList(),
                        u.teach().stream().map(SimTeach::trueLevel).toList(),
                        u.learn(),
                        u.trueSlots(),
                        u.mode()))
                .toList();
    }

    private static List<Tuple> learnTuples(SimUser user) {
        return user.learn().stream().map(l -> tuple(l.skillId(), l.target())).toList();
    }
}
