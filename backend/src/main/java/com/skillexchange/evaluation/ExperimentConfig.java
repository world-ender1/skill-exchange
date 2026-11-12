package com.skillexchange.evaluation;

import java.util.List;
import java.util.Objects;

/**
 * Ma trận chạy thực nghiệm của một giai đoạn (D70). chon() và baoCao(...) ghi đúng số của D70;
 * test dựng bản nhỏ bằng hai factory có tham số (kế hoạch tuần 5, bước 7).
 *
 * @param seeds           seed theo thứ tự chạy, tăng dần, không lặp
 * @param mainN           N của bảng chính, theo mức nhiễu, nhóm ít khung và kiểm định
 * @param scaleNs         các N của mục (b) "theo quy mô"; chỉ dùng ở giai đoạn báo cáo
 * @param latencyN        N đo latency (D61); chỉ dùng ở giai đoạn báo cáo
 * @param latencySeed     seed đo latency, phải có trong seeds; chỉ dùng ở giai đoạn báo cáo
 * @param bootstrapTimes  số lần lấy mẫu lại của bootstrap (D62); chỉ dùng ở giai đoạn báo cáo
 * @param selectedWeights bộ trọng số đã chọn ở D69; chỉ dùng ở giai đoạn báo cáo
 * @param selectedFormula công thức ScheduleFit đã chọn ở D69; chỉ dùng ở giai đoạn báo cáo
 */
public record ExperimentConfig(
        Phase phase,
        List<Long> seeds,
        int mainN,
        List<Integer> scaleNs,
        int latencyN,
        long latencySeed,
        int bootstrapTimes,
        WeightSets.Named selectedWeights,
        ScheduleFitFormula selectedFormula) {

    // ---------- Số của D70, D61, D62 ----------
    private static final List<Long> CHON_SEEDS = List.of(1L, 2L);
    private static final List<Long> BAO_CAO_SEEDS = List.of(3L, 4L, 5L);
    private static final int MAIN_N = 500;
    private static final List<Integer> SCALE_NS = List.of(50, 100, 500, 1000);
    private static final int LATENCY_N = 1000;
    private static final long LATENCY_SEED = 3;
    private static final int BOOTSTRAP_TIMES = 2000;

    /** Hai giai đoạn của D69, D70: tên trên dòng lệnh (D71) và giá trị cột vai_tro_seed (D72). */
    public enum Phase {
        CHON("chon", "chon"),
        BAO_CAO("bao-cao", "bao_cao");

        private final String cliName;
        private final String vaiTroSeed;

        Phase(String cliName, String vaiTroSeed) {
            this.cliName = cliName;
            this.vaiTroSeed = vaiTroSeed;
        }

        public String cliName() {
            return cliName;
        }

        public String vaiTroSeed() {
            return vaiTroSeed;
        }
    }

    public ExperimentConfig {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(selectedWeights, "selectedWeights");
        Objects.requireNonNull(selectedFormula, "selectedFormula");
        seeds = List.copyOf(seeds);
        scaleNs = List.copyOf(scaleNs);

        if (seeds.isEmpty()) {
            throw new IllegalArgumentException("Cần ít nhất một seed");
        }
        // Tăng dần và không lặp: thứ tự ghi kết quả và thứ tự gộp người dùng của kiểm định đều theo seed.
        for (int i = 1; i < seeds.size(); i++) {
            if (seeds.get(i) <= seeds.get(i - 1)) {
                throw new IllegalArgumentException("Seed phải tăng dần, không lặp: " + seeds);
            }
        }
        if (mainN < 1) {
            throw new IllegalArgumentException("mainN phải từ 1 trở lên: " + mainN);
        }
        if (phase == Phase.BAO_CAO) {
            if (scaleNs.isEmpty() || latencyN < 1 || bootstrapTimes < 1) {
                throw new IllegalArgumentException("Giai đoạn báo cáo cần quy mô, latencyN và số lần bootstrap");
            }
            if (!seeds.contains(latencySeed)) {
                throw new IllegalArgumentException("Seed đo latency " + latencySeed + " không có trong " + seeds);
            }
        }
    }

    /** Giai đoạn chọn của D70: seed 1–2, N = 500, nhiễu mặc định. */
    public static ExperimentConfig chon() {
        return chon(CHON_SEEDS, MAIN_N);
    }

    /** Giai đoạn báo cáo của D70: seed 3–5, cấu hình đã chọn ở D69. */
    public static ExperimentConfig baoCao(WeightSets.Named weights, ScheduleFitFormula formula) {
        return baoCao(BAO_CAO_SEEDS, MAIN_N, SCALE_NS, LATENCY_N, LATENCY_SEED, BOOTSTRAP_TIMES, weights, formula);
    }

    /** Giai đoạn chọn với seed và N tùy ý (dùng cho test). Các tham số chỉ dùng ở báo cáo để 0 hoặc mặc định. */
    public static ExperimentConfig chon(List<Long> seeds, int mainN) {
        return new ExperimentConfig(Phase.CHON, seeds, mainN, List.of(), 0, 0, 0,
                WeightSets.MAC_DINH, ScheduleFitFormula.MIN);
    }

    public static ExperimentConfig baoCao(List<Long> seeds, int mainN, List<Integer> scaleNs, int latencyN,
            long latencySeed, int bootstrapTimes, WeightSets.Named weights, ScheduleFitFormula formula) {
        return new ExperimentConfig(Phase.BAO_CAO, seeds, mainN, scaleNs, latencyN, latencySeed, bootstrapTimes,
                weights, formula);
    }
}
