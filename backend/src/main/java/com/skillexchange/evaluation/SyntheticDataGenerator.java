package com.skillexchange.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import com.skillexchange.matching.Slot;
import com.skillexchange.user.LearningMode;
import com.skillexchange.user.Period;

/**
 * Sinh bộ người dùng giả lập theo bảng tham số ở spec 5.4 (D63), tái lập được theo seed (D64).
 *
 * Hai luồng số ngẫu nhiên tách riêng:
 * - luồng dữ liệu thật new Random(seed): sinh giá trị thật, nên cùng seed thì giá trị thật giống nhau ở mọi mức nhiễu;
 * - luồng nhiễu new Random(seed + 1_000_000): sinh giá trị tự khai từ giá trị thật.
 * Thứ tự và cách rút từng số chốt ở kế hoạch tuần 5, bước 2. Đổi một lần rút là đổi dữ liệu của mọi seed.
 *
 * Chỉ duyệt List có thứ tự cố định khi rút số, không duyệt Set hay Map (D64).
 */
public final class SyntheticDataGenerator {

    private static final long NOISE_STREAM_OFFSET = 1_000_000;

    // ---------- Tham số của spec 5.4 ----------
    private static final int MIN_TEACH = 2;
    private static final int MAX_TEACH = 5;
    private static final int MIN_LEARN = 1;
    private static final int MAX_LEARN = 3;
    private static final int MIN_SLOTS = 3;
    private static final int MAX_SLOTS = 9;
    private static final int MAX_LEVEL = 4;
    private static final double P_BLANK_TARGET = 0.3;
    private static final double P_DECLARE_SCHEDULE = 0.85;
    private static final double P_DECLARE_MODE = 0.85;
    /** Hình thức học: [0, 0.35) ONLINE, [0.35, 0.60) OFFLINE, [0.60, 1) ANY. */
    private static final double ONLINE_BELOW = 0.35;
    private static final double OFFLINE_BELOW = 0.60;
    /** Trình độ lệch thì lên hay xuống với xác suất bằng nhau. */
    private static final double P_SHIFT_UP = 0.5;

    private static final Comparator<Slot> DAY_THEN_PERIOD =
            Comparator.comparingInt(Slot::dayOfWeek).thenComparing(Slot::period);

    /** 21 khung theo ngày rồi buổi: thứ tự duyệt cố định khi chọn khung và khi rút nhiễu. */
    private static final List<Slot> ALL_SLOTS = allSlots();

    /** Giá trị thật của một người, trước khi xáo để gán mã. */
    private record TrueProfile(
            List<Long> teachSkills,
            List<Integer> trueLevels,
            List<SimLearn> learn,
            List<Slot> slots,
            LearningMode mode) {
    }

    private SyntheticDataGenerator() {
    }

    /** n người, mã 1..n, xếp theo mã. */
    public static List<SimUser> generate(int n, long seed, NoiseLevel noise) {
        if (n < 1) {
            throw new IllegalArgumentException("n phải từ 1 trở lên: " + n);
        }
        Objects.requireNonNull(noise, "noise");

        Random trueRandom = new Random(seed);
        List<Long> zipfOrder = zipfOrder(trueRandom);
        List<TrueProfile> profiles = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            profiles.add(trueProfile(trueRandom, zipfOrder));
        }
        // Xáo rồi gán mã theo vị trí, để quy tắc bằng điểm (xếp theo mã) không gắn với thứ tự sinh (spec 5.4).
        Collections.shuffle(profiles, trueRandom);

        Random noiseRandom = new Random(seed + NOISE_STREAM_OFFSET);
        List<SimUser> users = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            users.add(withNoise(i + 1, profiles.get(i), noise, noiseRandom));
        }
        return List.copyOf(users);
    }

    // ---------- Luồng dữ liệu thật ----------

    /** Hoán vị ngẫu nhiên của 26 mã kỹ năng: kỹ năng ở vị trí r - 1 có hạng r, trọng số 1/r (Zipf s = 1). */
    private static List<Long> zipfOrder(Random random) {
        List<Long> ids = new ArrayList<>();
        for (SkillCatalog.Skill skill : SkillCatalog.V2.skills()) {
            ids.add(skill.id());
        }
        Collections.shuffle(ids, random);
        return ids;
    }

    private static TrueProfile trueProfile(Random random, List<Long> zipfOrder) {
        int teachCount = between(random, MIN_TEACH, MAX_TEACH);
        List<Long> teachSkills = pickZipf(random, zipfOrder, teachCount);
        List<Integer> trueLevels = new ArrayList<>();
        for (int k = 0; k < teachCount; k++) {
            trueLevels.add(between(random, 1, MAX_LEVEL));
        }

        int learnCount = between(random, MIN_LEARN, MAX_LEARN);
        // Chỉ học kỹ năng mình không dạy (D19); danh sách xếp theo mã.
        List<Long> notTaught = new ArrayList<>();
        for (SkillCatalog.Skill skill : SkillCatalog.V2.skills()) {
            if (!teachSkills.contains(skill.id())) {
                notTaught.add(skill.id());
            }
        }
        List<SimLearn> learn = new ArrayList<>();
        for (long skillId : pickUniform(random, notTaught, learnCount)) {
            Integer target = null;
            if (random.nextDouble() >= P_BLANK_TARGET) { // bỏ trống khi số < 0.3; chỉ rút trình độ khi không bỏ trống
                target = between(random, 1, MAX_LEVEL);
            }
            learn.add(new SimLearn(skillId, target));
        }

        List<Slot> slots = new ArrayList<>();
        if (random.nextDouble() < P_DECLARE_SCHEDULE) {
            int slotCount = between(random, MIN_SLOTS, MAX_SLOTS);
            slots.addAll(pickUniform(random, ALL_SLOTS, slotCount));
            slots.sort(DAY_THEN_PERIOD);
        }

        LearningMode mode = null;
        if (random.nextDouble() < P_DECLARE_MODE) {
            mode = modeFor(random.nextDouble());
        }
        return new TrueProfile(teachSkills, trueLevels, learn, slots, mode);
    }

    /** Chọn count kỹ năng không lặp, xác suất tỷ lệ với 1/hạng. Mỗi lần chọn rút một nextDouble(). */
    private static List<Long> pickZipf(Random random, List<Long> zipfOrder, int count) {
        List<Integer> remainingRanks = new ArrayList<>(); // hạng 1..26 còn lại, tăng dần
        for (int rank = 1; rank <= zipfOrder.size(); rank++) {
            remainingRanks.add(rank);
        }
        List<Long> picked = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            double total = 0;
            for (int rank : remainingRanks) {
                total += 1.0 / rank;
            }
            double point = random.nextDouble() * total;
            // Sai số làm tròn có thể làm tổng cộng dồn không vượt point: khi đó lấy phần tử cuối.
            int chosen = remainingRanks.size() - 1;
            double cumulative = 0;
            for (int i = 0; i < remainingRanks.size(); i++) {
                cumulative += 1.0 / remainingRanks.get(i);
                if (cumulative > point) {
                    chosen = i;
                    break;
                }
            }
            int rank = remainingRanks.remove(chosen); // remove(int): bỏ theo vị trí, trả về phần tử
            picked.add(zipfOrder.get(rank - 1));
        }
        return picked;
    }

    /** Chọn count phần tử không lặp, đều nhau: mỗi lần rút nextInt(số còn lại) trên danh sách còn lại. */
    private static <T> List<T> pickUniform(Random random, List<T> from, int count) {
        List<T> remaining = new ArrayList<>(from);
        List<T> picked = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            picked.add(remaining.remove(random.nextInt(remaining.size())));
        }
        return picked;
    }

    private static LearningMode modeFor(double u) {
        if (u < ONLINE_BELOW) {
            return LearningMode.ONLINE;
        }
        if (u < OFFLINE_BELOW) {
            return LearningMode.OFFLINE;
        }
        return LearningMode.ANY;
    }

    // ---------- Luồng nhiễu ----------

    /**
     * Giá trị tự khai của một người. Số được rút đủ kể cả khi xác suất bằng 0 hay khi không cần dùng,
     * để mọi mức nhiễu dùng chung một dãy số: mức cao hơn chỉ thêm thay đổi (D64, TC202).
     */
    private static SimUser withNoise(long id, TrueProfile profile, NoiseLevel noise, Random random) {
        List<SimTeach> teach = new ArrayList<>();
        for (int k = 0; k < profile.teachSkills().size(); k++) {
            int trueLevel = profile.trueLevels().get(k);
            boolean shifted = random.nextDouble() < noise.pLevel();
            boolean up = random.nextDouble() < P_SHIFT_UP;
            int declaredLevel = shifted ? shiftOneStep(trueLevel, up) : trueLevel;
            teach.add(new SimTeach(profile.teachSkills().get(k), trueLevel, declaredLevel));
        }

        boolean declaresSchedule = !profile.slots().isEmpty();
        List<Slot> declaredSlots = new ArrayList<>();
        for (Slot slot : ALL_SLOTS) {
            double u = random.nextDouble(); // rút cho mọi người, mọi khung
            if (!declaresSchedule) {
                continue; // D53: người không khai lịch không có khung nào ở cả hai lớp
            }
            boolean keep = profile.slots().contains(slot)
                    ? u >= noise.pMiss()   // khung thật bị bỏ sót khi u < pMiss
                    : u < noise.pExtra();  // khung không thật được thêm khi u < pExtra
            if (keep) {
                declaredSlots.add(slot);
            }
        }
        if (declaresSchedule) {
            Slot fallback = profile.slots().get(random.nextInt(profile.slots().size()));
            // Nhiễu xóa hết lịch thì giữ lại một khung thật, để người này không thành "chưa khai báo" (spec 5.4).
            if (declaredSlots.isEmpty()) {
                declaredSlots.add(fallback);
            }
        }
        return new SimUser(id, teach, profile.learn(), profile.slots(), declaredSlots, profile.mode());
    }

    /** Trình độ 1 chỉ lệch lên, 4 chỉ lệch xuống (spec 5.4); ở giữa thì theo hướng đã rút. */
    private static int shiftOneStep(int level, boolean up) {
        if (level == 1) {
            return 2;
        }
        if (level == MAX_LEVEL) {
            return MAX_LEVEL - 1;
        }
        return up ? level + 1 : level - 1;
    }

    // ---------- Tiện ích ----------

    /** Số nguyên đều trong [min, max]. */
    private static int between(Random random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private static List<Slot> allSlots() {
        List<Slot> slots = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            for (Period period : Period.values()) { // thứ tự khai báo của enum = thứ tự trong ngày
                slots.add(new Slot(day, period));
            }
        }
        return List.copyOf(slots);
    }
}
