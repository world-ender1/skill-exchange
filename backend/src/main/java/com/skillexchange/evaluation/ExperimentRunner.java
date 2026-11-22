package com.skillexchange.evaluation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import com.skillexchange.evaluation.ExperimentConfig.Phase;
import com.skillexchange.evaluation.PairedStatistics.ConfidenceInterval;
import com.skillexchange.evaluation.PairedStatistics.WilcoxonResult;
import com.skillexchange.evaluation.ResultWriter.MetricRow;
import com.skillexchange.evaluation.ResultWriter.TestRow;
import com.skillexchange.matching.MatchProfile;
import com.skillexchange.matching.MatchingWeights;
import com.skillexchange.matching.SkillSimilarity;

/**
 * Chạy các tổ hợp của D70 và ghi kết quả qua ResultWriter (D61, D62, D72).
 *
 * Đơn vị tính là "ô" = (seed, N, mức nhiễu, phương án). Mục (a)–(e) của D70 có thể cần cùng một ô
 * (ví dụ P ở N = 500, nhiễu mặc định có mặt ở (a), (b), (c), (d)): ô chỉ tính một lần và mang theo
 * các phần cần ghi, nên metrics.csv không có dòng lặp (kế hoạch tuần 5, bước 7).
 * Mỗi bộ dữ liệu (seed, N, mức nhiễu) sinh và gán nhãn một lần, dùng cho mọi ô của nó.
 */
public final class ExperimentRunner {

    // ---------- Tên phương án (D72) ----------
    private static final String B0 = "B0";
    private static final String B1 = "B1";
    private static final String R = "R";
    private static final String P = "P";
    /** P chấm điểm như thường nhưng xếp chung một danh sách theo D chiều u học từ v (D97). */
    private static final String P_MOT_DS = "P_mot_ds";
    /** Hai nhóm của lõi, dùng ở chỉ số trong từng nhóm (D99). */
    private static final String HAI_CHIEU = "hai_chieu";
    private static final String MOT_CHIEU = "mot_chieu";

    private static final SkillCatalog CATALOG = SkillCatalog.V2;
    /** Thứ tự ghi các mức nhiễu (kế hoạch bước 7). */
    private static final List<NoiseLevel> NOISE_ORDER = List.of(NoiseLevel.KHONG, NoiseLevel.MAC_DINH, NoiseLevel.CAO);
    /** Mỗi phép so sánh của kiểm định một Random mới cùng seed này (D62, kế hoạch bước 7). */
    private static final long BOOTSTRAP_SEED = 3_000_000;
    /** Nhóm "khai ít khung" của D70 (d): lịch tự khai từ 1 đến 3 khung. */
    private static final int FEW_SLOTS_MAX = 3;

    /** Các phần một ô cần ghi: chỉ số đầy đủ ((a)–(c)), latency ((e)), nhóm khai ít khung ((d)), từng nhóm hai chiều / một chiều (D99). */
    private enum Part { CHI_SO, LATENCY, IT_KHUNG, NHOM }

    /** Một ô của một seed. parts là EnumSet sửa được, chỉ dùng trong lúc lập danh sách ô. */
    private record Cell(int n, NoiseLevel noise, String phuongAn, EnumSet<Part> parts) {
    }

    /** Trọng số và công thức ScheduleFit của một biến thể P. */
    private record PVariant(MatchingWeights weights, ScheduleFitFormula formula) {
    }

    /** Tỷ lệ vị trí do ứng viên khai 1–3 khung chiếm, gộp mọi vị trí của mọi người (D70 d). NaN khi không có vị trí. */
    record FewSlotShares(double top5, double wholeList) {
    }

    private final ExperimentConfig config;
    private final ResultWriter writer;
    private final Rankers rankers = new Rankers(CATALOG.relations());
    private final GroundTruthLabeler labeler = new GroundTruthLabeler(new SkillSimilarity(CATALOG.relations()));
    /** Mọi biến thể P theo tên phuong_an. LinkedHashMap chỉ để tra, không duyệt khi rút số ngẫu nhiên. */
    private final Map<String, PVariant> pVariants = new LinkedHashMap<>();
    /** Chỉ số từng người của các ô dùng cho kiểm định, gộp các seed theo thứ tự chạy (D62). */
    private final Map<String, List<UserMetrics>> forPairedTests = new LinkedHashMap<>();
    /**
     * Giai đoạn bổ sung: chỉ số từng người theo khóa "seed|n|phương án" (chỉ số trong nhóm thêm "@nhóm"), để kiểm định
     * cả gộp các seed lẫn riêng từng seed (D100). Map riêng, không đụng forPairedTests nên giai đoạn báo cáo không đổi.
     */
    private final Map<String, List<UserMetrics>> supplementUsers = new LinkedHashMap<>();

    public ExperimentRunner(ExperimentConfig config, ResultWriter writer) {
        this.config = config;
        this.writer = writer;
        pVariants.put(P, new PVariant(MatchingWeights.DEFAULT, ScheduleFitFormula.MIN));
        for (WeightSets.Named named : WeightSets.SENSITIVITY) {
            pVariants.put(named.phuongAn(), new PVariant(named.weights(), ScheduleFitFormula.MIN));
        }
        for (WeightSets.Named named : WeightSets.ABLATION) {
            pVariants.put(named.phuongAn(), new PVariant(named.weights(), ScheduleFitFormula.MIN));
        }
        for (ScheduleFitFormula formula : List.of(ScheduleFitFormula.JACCARD, ScheduleFitFormula.TRAN_3)) {
            pVariants.put(scheduleVariantName(formula), new PVariant(MatchingWeights.DEFAULT, formula));
        }
        for (WeightSets.Named named : WeightSets.ABLATION_PROPORTIONAL) {
            pVariants.put(named.phuongAn(), new PVariant(named.weights(), ScheduleFitFormula.MIN));
        }
    }

    /**
     * Tạo thư mục kết quả, chạy, ghi README. Thư mục đã có thì IllegalStateException trước khi tính gì (TC244).
     * Lỗi giữa chừng: README ghi "hỏng" kèm lý do, thư mục được giữ lại, rồi ném lại lỗi (D72).
     */
    public static void execute(Path runDir, ResultWriter.RunInfo info, Consumer<String> console) {
        try (ResultWriter writer = ResultWriter.create(runDir, info, console)) {
            try {
                new ExperimentRunner(info.config(), writer).run();
                writer.finish();
            } catch (RuntimeException e) {
                writer.fail(e.toString());
                throw e;
            }
        }
    }

    public void run() {
        writer.log("Bắt đầu giai đoạn " + config.phase().cliName() + ", seed " + config.seeds());
        for (long seed : config.seeds()) {
            List<Cell> cells = cellsFor(seed);
            // cellsFor đã xếp theo (N, mức nhiễu): các ô cùng bộ dữ liệu nằm liền nhau.
            int start = 0;
            while (start < cells.size()) {
                int end = start;
                while (end < cells.size() && sameDataset(cells.get(end), cells.get(start))) {
                    end++;
                }
                runDataset(seed, cells.subList(start, end));
                start = end;
            }
        }
        if (config.phase() == Phase.BAO_CAO) {
            writePairedTests();
        }
        if (config.phase() == Phase.BO_SUNG) {
            writeSupplementTests();
        }
        writer.log("Xong");
    }

    // ---------- Lập danh sách ô (D70) ----------

    private List<Cell> cellsFor(long seed) {
        int mainN = config.mainN();
        NoiseLevel md = NoiseLevel.MAC_DINH;
        List<String> baseline = List.of(B0, B1, R, P);
        List<String> ablation = WeightSets.ABLATION.stream().map(WeightSets.Named::phuongAn).toList();
        // LinkedHashMap giữ thứ tự thêm vào: ô thêm trước thì ghi trước trong cùng bộ dữ liệu.
        Map<String, Cell> cells = new LinkedHashMap<>();

        if (config.phase() == Phase.CHON) {
            // P với 4 bộ trọng số (MIN) và với 3 công thức (bộ mặc định); P có trong cả hai nhóm.
            for (WeightSets.Named named : WeightSets.SENSITIVITY) {
                add(cells, mainN, md, named.phuongAn(), Part.CHI_SO);
            }
            add(cells, mainN, md, scheduleVariantName(ScheduleFitFormula.JACCARD), Part.CHI_SO);
            add(cells, mainN, md, scheduleVariantName(ScheduleFitFormula.TRAN_3), Part.CHI_SO);
        } else if (config.phase() == Phase.BO_SUNG) {
            // (f) D97: B1, P và P một danh sách ở mọi quy mô, để xem vì sao P thua B1 ở N nhỏ.
            for (int n : config.scaleNs()) {
                for (String name : List.of(B1, P, P_MOT_DS)) {
                    add(cells, n, md, name, Part.CHI_SO);
                }
            }
            // (g) D98: bỏ từng thành phần, chia lại theo tỉ lệ.
            for (WeightSets.Named named : WeightSets.ABLATION_PROPORTIONAL) {
                add(cells, mainN, md, named.phuongAn(), Part.CHI_SO);
            }
            // (h) D99: chỉ số trong từng nhóm hai chiều / một chiều.
            for (String name : List.of(B1, P, P_MOT_DS)) {
                add(cells, mainN, md, name, Part.NHOM);
            }
        } else {
            // (a) bảng chính, cộng cấu hình đã chọn dưới dạng hai phương án riêng.
            for (String name : baseline) {
                add(cells, mainN, md, name, Part.CHI_SO);
            }
            for (String name : ablation) {
                add(cells, mainN, md, name, Part.CHI_SO);
            }
            for (String name : selectedVariantNames()) {
                add(cells, mainN, md, name, Part.CHI_SO);
            }
            // (b) theo quy mô.
            for (int n : config.scaleNs()) {
                for (String name : baseline) {
                    add(cells, n, md, name, Part.CHI_SO);
                }
            }
            // (c) theo mức nhiễu.
            for (NoiseLevel noise : NOISE_ORDER) {
                add(cells, mainN, noise, B1, Part.CHI_SO);
                add(cells, mainN, noise, P, Part.CHI_SO);
                for (String name : ablation) {
                    add(cells, mainN, noise, name, Part.CHI_SO);
                }
            }
            // (d) nhóm khai ít khung: P với 3 công thức.
            add(cells, mainN, md, P, Part.IT_KHUNG);
            add(cells, mainN, md, scheduleVariantName(ScheduleFitFormula.JACCARD), Part.IT_KHUNG);
            add(cells, mainN, md, scheduleVariantName(ScheduleFitFormula.TRAN_3), Part.IT_KHUNG);
            // (e) latency.
            if (seed == config.latencySeed()) {
                for (String name : baseline) {
                    add(cells, config.latencyN(), md, name, Part.LATENCY);
                }
            }
        }

        List<Cell> ordered = new ArrayList<>(cells.values());
        // List.sort giữ nguyên thứ tự của các phần tử bằng nhau, nên trong một bộ dữ liệu vẫn theo thứ tự thêm vào.
        ordered.sort(Comparator.comparingInt(Cell::n).thenComparingInt(cell -> NOISE_ORDER.indexOf(cell.noise())));
        return ordered;
    }

    /** Ô đã có thì chỉ thêm phần cần ghi; chưa có thì tạo. */
    private static void add(Map<String, Cell> cells, int n, NoiseLevel noise, String phuongAn, Part part) {
        String key = n + "|" + noise.ten() + "|" + phuongAn;
        Cell existing = cells.get(key);
        if (existing != null) {
            existing.parts().add(part);
        } else {
            cells.put(key, new Cell(n, noise, phuongAn, EnumSet.of(part)));
        }
    }

    /** Cấu hình đã chọn ở D69, mỗi chiều khác mặc định thành một phương án riêng (người dùng chốt 2026-11-13). */
    private List<String> selectedVariantNames() {
        List<String> names = new ArrayList<>();
        if (config.selectedWeights() != WeightSets.MAC_DINH) {
            names.add(config.selectedWeights().phuongAn());
        }
        if (config.selectedFormula() != ScheduleFitFormula.MIN) {
            names.add(scheduleVariantName(config.selectedFormula()));
        }
        return names;
    }

    private static String scheduleVariantName(ScheduleFitFormula formula) {
        return "P_sf_" + formula.ten();
    }

    private static boolean sameDataset(Cell a, Cell b) {
        return a.n() == b.n() && a.noise().equals(b.noise());
    }

    // ---------- Chạy một bộ dữ liệu ----------

    private void runDataset(long seed, List<Cell> cells) {
        int n = cells.get(0).n();
        NoiseLevel noise = cells.get(0).noise();
        long start = System.nanoTime();
        Dataset data = new Dataset(SyntheticDataGenerator.generate(n, seed, noise), labeler);
        writer.log(String.format("seed %d, N = %d, nhiễu %s: sinh dữ liệu và gán nhãn %.1f giây",
                seed, n, noise.ten(), (System.nanoTime() - start) / 1e9));

        for (Cell cell : cells) {
            long cellStart = System.nanoTime();
            runCell(seed, data, cell);
            writer.log(String.format("  %s %s: %.1f giây", cell.phuongAn(), cell.parts(), (System.nanoTime() - cellStart) / 1e9));
        }
    }

    private void runCell(long seed, Dataset data, Cell cell) {
        Ranker ranker = rankerFor(cell.phuongAn(), seed);
        List<UserMetrics> perUser = new ArrayList<>();
        List<List<Long>> lists = new ArrayList<>();
        for (int u : data.eligible) {
            List<Long> list = ranker.rank(data.profiles.get(u), data.profiles);
            lists.add(list);
            perUser.add(UserMetrics.of(data.users.get(u).id(), data.relsOf(u, list), data.idealRels.get(u)));
        }

        if (cell.parts().contains(Part.CHI_SO)) {
            writeSummary(seed, cell, MetricSummary.of(perUser));
            for (UserMetrics m : perUser) {
                writer.perUser(seed, cell.n(), cell.noise().ten(), cell.phuongAn(), m);
            }
            keepForPairedTests(cell, perUser);
            if (config.phase() == Phase.BO_SUNG) {
                supplementUsers.put(supplementKey(seed, cell.n(), cell.phuongAn()), perUser);
            }
        }
        if (cell.parts().contains(Part.LATENCY)) {
            writeLatency(seed, data, cell, ranker);
        }
        if (cell.parts().contains(Part.IT_KHUNG)) {
            writeFewSlots(seed, data, cell, perUser, lists);
        }
        if (cell.parts().contains(Part.NHOM)) {
            writeGroups(seed, data, cell, lists);
        }
    }

    private Ranker rankerFor(String phuongAn, long seed) {
        return switch (phuongAn) {
            case B0 -> rankers.b0();
            case B1 -> rankers.b1();
            // R xáo theo seed của bộ dữ liệu (D64).
            case R -> rankers.random(seed);
            case P_MOT_DS -> rankers.pSingleList(MatchingWeights.DEFAULT);
            default -> {
                PVariant variant = pVariants.get(phuongAn);
                if (variant == null) {
                    throw new IllegalStateException("Không có phương án " + phuongAn);
                }
                yield rankers.p(variant.weights(), variant.formula());
            }
        };
    }

    // ---------- Ghi chỉ số ----------

    /** Chỉ số đầy đủ theo đúng thứ tự cột chi_so của D72. */
    private void writeSummary(long seed, Cell cell, MetricSummary s) {
        int all = s.userCount();
        metric(seed, cell, "precision_at_5", ResultWriter.number(s.precisionAt5()), all);
        metric(seed, cell, "precision_at_5_nghiem_ngat", ResultWriter.number(s.strictPrecisionAt5()), all);
        metric(seed, cell, "ndcg_at_5_tat_ca", ResultWriter.number(s.ndcgAt5AllUsers()), all);
        // Bản (2) tính trên ít người hơn: so_nguoi_tinh là số người thuộc bản (2) (D59).
        metric(seed, cell, "ndcg_at_5_loai_suy_bien", ResultWriter.number(s.ndcgAt5NonDegenerate()), s.nonDegenerateCount());
        metric(seed, cell, "so_ca_idcg_0", ResultWriter.count(s.idcgZeroCount()), all);
        metric(seed, cell, "so_nguoi_bi_loai", ResultWriter.count(s.excludedCount()), all);
        metric(seed, cell, "coverage", ResultWriter.number(s.coverage()), all);
        metric(seed, cell, "ty_le_du_5_ung_vien", ResultWriter.number(s.atLeastFiveRate()), all);
        metric(seed, cell, "ty_le_co_nhan_2", ResultWriter.number(s.hasLabel2Rate()), all);
    }

    /**
     * D61: một lượt làm nóng trên mọi người (không ghi), rồi đo từng người. Chỉ đo Ranker.rank: lọc, chấm điểm,
     * xếp hạng; không gồm gán nhãn và tính chỉ số. Cộng dồn số ứng viên và ghi ra log, để JIT không bỏ được lời gọi.
     */
    private void writeLatency(long seed, Dataset data, Cell cell, Ranker ranker) {
        long candidates = 0;
        for (int u : data.eligible) {
            candidates += ranker.rank(data.profiles.get(u), data.profiles).size();
        }
        long[] nanos = new long[data.eligible.size()];
        for (int k = 0; k < nanos.length; k++) {
            MatchProfile caller = data.profiles.get(data.eligible.get(k));
            long start = System.nanoTime();
            List<Long> list = ranker.rank(caller, data.profiles);
            nanos[k] = System.nanoTime() - start;
            candidates += list.size();
        }
        metric(seed, cell, "latency_tb_ms", ResultWriter.number(meanMs(nanos)), nanos.length);
        metric(seed, cell, "latency_p95_ms", ResultWriter.number(p95Ms(nanos)), nanos.length);
        writer.log("  latency " + cell.phuongAn() + ": tổng số ứng viên hai lượt = " + candidates);
    }

    /** D70 (d): chỉ số của nhóm u khai 1–3 khung, và tỷ lệ vị trí do ứng viên khai 1–3 khung chiếm. */
    private void writeFewSlots(long seed, Dataset data, Cell cell, List<UserMetrics> perUser, List<List<Long>> lists) {
        List<UserMetrics> group = new ArrayList<>();
        for (int k = 0; k < data.eligible.size(); k++) {
            if (hasFewSlots(data.users.get(data.eligible.get(k)))) {
                group.add(perUser.get(k));
            }
        }
        if (group.isEmpty()) {
            // Nhóm rỗng: không xác định, ghi NaN có chủ đích như bản (2) của D59 (MetricSummary từ chối danh sách rỗng).
            for (String chiSo : List.of("precision_at_5_it_khung", "precision_at_5_nghiem_ngat_it_khung",
                    "ndcg_at_5_tat_ca_it_khung", "ndcg_at_5_loai_suy_bien_it_khung")) {
                metric(seed, cell, chiSo, ResultWriter.number(Double.NaN), 0);
            }
        } else {
            MetricSummary s = MetricSummary.of(group);
            metric(seed, cell, "precision_at_5_it_khung", ResultWriter.number(s.precisionAt5()), s.userCount());
            metric(seed, cell, "precision_at_5_nghiem_ngat_it_khung", ResultWriter.number(s.strictPrecisionAt5()), s.userCount());
            metric(seed, cell, "ndcg_at_5_tat_ca_it_khung", ResultWriter.number(s.ndcgAt5AllUsers()), s.userCount());
            metric(seed, cell, "ndcg_at_5_loai_suy_bien_it_khung", ResultWriter.number(s.ndcgAt5NonDegenerate()),
                    s.nonDegenerateCount());
        }

        // Tập chỉ để tra contains, không duyệt, nên thứ tự của HashSet không ảnh hưởng tái lập (D64).
        Set<Long> fewSlotIds = new HashSet<>();
        for (SimUser user : data.users) {
            if (hasFewSlots(user)) {
                fewSlotIds.add(user.id());
            }
        }
        FewSlotShares shares = fewSlotShares(lists, fewSlotIds);
        metric(seed, cell, "ty_le_top5_it_khung", ResultWriter.number(shares.top5()), data.eligible.size());
        metric(seed, cell, "ty_le_danh_sach_it_khung", ResultWriter.number(shares.wholeList()), data.eligible.size());
    }

    private static boolean hasFewSlots(SimUser user) {
        int slots = user.declaredSlots().size();
        return slots >= 1 && slots <= FEW_SLOTS_MAX;
    }

    private void metric(long seed, Cell cell, String chiSo, String giaTri, int soNguoiTinh) {
        writer.metric(new MetricRow(seed, config.phase().vaiTroSeed(), cell.n(), cell.noise().ten(), cell.phuongAn(),
                chiSo, giaTri, soNguoiTinh));
    }

    // ---------- Kiểm định (D62) ----------

    /** Giữ chỉ số từng người của P, B1 và 3 phương án bỏ thành phần ở N = mainN, nhiễu mặc định. */
    private void keepForPairedTests(Cell cell, List<UserMetrics> perUser) {
        boolean pairedCell = config.phase() == Phase.BAO_CAO
                && cell.n() == config.mainN()
                && cell.noise().equals(NoiseLevel.MAC_DINH)
                && (cell.phuongAn().equals(P) || comparedWithP().contains(cell.phuongAn()));
        if (pairedCell) {
            forPairedTests.computeIfAbsent(cell.phuongAn(), name -> new ArrayList<>()).addAll(perUser);
        }
    }

    /** Thứ tự dòng của kiem_dinh.csv: B1 rồi 3 phương án bỏ thành phần (kế hoạch bước 7). */
    private static List<String> comparedWithP() {
        List<String> names = new ArrayList<>();
        names.add(B1);
        for (WeightSets.Named named : WeightSets.ABLATION) {
            names.add(named.phuongAn());
        }
        return names;
    }

    private void writePairedTests() {
        List<UserMetrics> p = forPairedTests.get(P);
        String seeds = config.seeds().stream().map(String::valueOf).collect(Collectors.joining(";"));
        for (String other : comparedWithP()) {
            List<UserMetrics> o = forPairedTests.get(other);
            checkSameUsers(p, o, other);
            writePairedTest(config.mainN(), seeds, P, other, "ndcg_at_5", p, o, UserMetrics::ndcgAt5);
            writePairedTest(config.mainN(), seeds, P, other, "precision_at_5_nghiem_ngat", p, o, UserMetrics::strictPrecisionAt5);
        }
    }

    /**
     * Một phép so sánh theo cặp, hiệu = a − b. Giai đoạn báo cáo gọi với a = P, n = mainN như trước nên kiem_dinh.csv
     * của nó không đổi; giai đoạn bổ sung gọi với các cặp của D100.
     */
    private void writePairedTest(int n, String seeds, String a, String b, String chiSo, List<UserMetrics> la,
            List<UserMetrics> lb, ToDoubleFunction<UserMetrics> metric) {
        List<Double> diffList = new ArrayList<>();
        for (int i = 0; i < la.size(); i++) {
            // Chỉ người thuộc bản (2) của D59. Tập lý tưởng không phụ thuộc phương án, nên xét theo a là đủ.
            if (la.get(i).nonDegenerate()) {
                diffList.add(metric.applyAsDouble(la.get(i)) - metric.applyAsDouble(lb.get(i)));
            }
        }
        double[] diffs = diffList.stream().mapToDouble(Double::doubleValue).toArray();
        String noise = NoiseLevel.MAC_DINH.ten();

        if (diffs.length == 0) {
            writer.test(new TestRow(n, noise, seeds, a, b, chiSo, 0, "", "", "", 0, "", "", "", "khong_co_nguoi_tinh"));
            return;
        }
        double sum = 0;
        for (double d : diffs) {
            sum += d;
        }
        // Random mới cho từng phép: kết quả một phép không đổi khi thêm, bớt hay đổi thứ tự các phép khác.
        ConfidenceInterval ci = PairedStatistics.bootstrapMeanCi(diffs, config.bootstrapTimes(), new Random(BOOTSTRAP_SEED));
        Optional<WilcoxonResult> wilcoxon = PairedStatistics.wilcoxon(diffs);

        String mean = ResultWriter.number(sum / diffs.length);
        String low = ResultWriter.number(ci.low());
        String high = ResultWriter.number(ci.high());
        if (wilcoxon.isEmpty()) {
            writer.test(new TestRow(n, noise, seeds, a, b, chiSo, diffs.length, mean, low, high,
                    0, "", "", "", "khong_co_cap_khac_nhau"));
        } else {
            WilcoxonResult w = wilcoxon.get();
            writer.test(new TestRow(n, noise, seeds, a, b, chiSo, diffs.length, mean, low, high, w.n(),
                    ResultWriter.number(w.wPlus()), ResultWriter.number(w.z()), ResultWriter.number(w.p()), ""));
        }
    }

    // ---------- Giai đoạn bổ sung (D97–D100) ----------

    private static String supplementKey(long seed, int n, String name) {
        return seed + "|" + n + "|" + name;
    }

    /**
     * D99: chỉ số trong từng nhóm. Nhóm của mỗi ứng viên lấy từ lõi (lọc cứng hai chiều), như nhau với mọi phương án
     * vì không phụ thuộc trọng số; danh sách của phương án lọc theo nhóm, giữ thứ tự. Tập lý tưởng của nhóm là chính các
     * ứng viên của nhóm đó, nên chỉ số đo thứ tự xếp trong nhóm, không đo việc lọc ứng viên.
     */
    private void writeGroups(long seed, Dataset data, Cell cell, List<List<Long>> lists) {
        List<UserMetrics> twoWay = new ArrayList<>();
        List<UserMetrics> oneWay = new ArrayList<>();
        for (int k = 0; k < data.eligible.size(); k++) {
            int u = data.eligible.get(k);
            Set<Long> twoWayIds = rankers.twoWayIds(data.profiles.get(u), data.profiles);
            List<Long> two = new ArrayList<>();
            List<Long> one = new ArrayList<>();
            for (long v : lists.get(k)) {
                (twoWayIds.contains(v) ? two : one).add(v);
            }
            long id = data.users.get(u).id();
            // Người không có ứng viên nào ở nhóm thì không tính cho nhóm đó; mọi phương án có cùng nhóm nên cùng người.
            if (!two.isEmpty()) {
                List<Integer> rels = data.relsOf(u, two);
                twoWay.add(UserMetrics.of(id, rels, rels));
            }
            if (!one.isEmpty()) {
                List<Integer> rels = data.relsOf(u, one);
                oneWay.add(UserMetrics.of(id, rels, rels));
            }
        }
        writeGroup(seed, cell, HAI_CHIEU, twoWay);
        writeGroup(seed, cell, MOT_CHIEU, oneWay);
    }

    private void writeGroup(long seed, Cell cell, String group, List<UserMetrics> users) {
        supplementUsers.put(supplementKey(seed, cell.n(), cell.phuongAn() + "@" + group), users);
        if (users.isEmpty()) {
            for (String chiSo : List.of("precision_at_5_nghiem_ngat_", "ndcg_at_5_loai_suy_bien_")) {
                metric(seed, cell, chiSo + group, ResultWriter.number(Double.NaN), 0);
            }
            return;
        }
        MetricSummary s = MetricSummary.of(users);
        metric(seed, cell, "precision_at_5_nghiem_ngat_" + group, ResultWriter.number(s.strictPrecisionAt5()), s.userCount());
        metric(seed, cell, "ndcg_at_5_loai_suy_bien_" + group, ResultWriter.number(s.ndcgAt5NonDegenerate()),
                s.nonDegenerateCount());
    }

    /** D100: mỗi phép so sánh gộp các seed như D62, rồi từng seed riêng (mỗi seed là một bộ dữ liệu độc lập). */
    private void writeSupplementTests() {
        for (int n : config.scaleNs()) {
            supplementTest(n, P, B1, "");
            supplementTest(n, P_MOT_DS, B1, "");
            supplementTest(n, P, P_MOT_DS, "");
        }
        for (WeightSets.Named named : WeightSets.ABLATION_PROPORTIONAL) {
            supplementTest(config.mainN(), P, named.phuongAn(), "");
        }
        for (String group : List.of(HAI_CHIEU, MOT_CHIEU)) {
            supplementTest(config.mainN(), P, B1, group);
            supplementTest(config.mainN(), P_MOT_DS, B1, group);
        }
    }

    private void supplementTest(int n, String a, String b, String group) {
        String keyA = group.isEmpty() ? a : a + "@" + group;
        String keyB = group.isEmpty() ? b : b + "@" + group;
        String suffix = group.isEmpty() ? "" : "_" + group;
        List<UserMetrics> allA = new ArrayList<>();
        List<UserMetrics> allB = new ArrayList<>();
        for (long seed : config.seeds()) {
            allA.addAll(supplementUsersOf(seed, n, keyA));
            allB.addAll(supplementUsersOf(seed, n, keyB));
        }
        String allSeeds = config.seeds().stream().map(String::valueOf).collect(Collectors.joining(";"));
        pairedBothMetrics(n, allSeeds, a, b, suffix, allA, allB);
        for (long seed : config.seeds()) {
            pairedBothMetrics(n, String.valueOf(seed), a, b, suffix, supplementUsersOf(seed, n, keyA), supplementUsersOf(seed, n, keyB));
        }
    }

    private List<UserMetrics> supplementUsersOf(long seed, int n, String key) {
        List<UserMetrics> users = supplementUsers.get(supplementKey(seed, n, key));
        if (users == null) {
            throw new IllegalStateException("Thiếu chỉ số của " + key + " ở seed " + seed + ", N = " + n);
        }
        return users;
    }

    private void pairedBothMetrics(int n, String seeds, String a, String b, String suffix, List<UserMetrics> la,
            List<UserMetrics> lb) {
        checkSameUsers(la, lb, b);
        writePairedTest(n, seeds, a, b, "ndcg_at_5" + suffix, la, lb, UserMetrics::ndcgAt5);
        writePairedTest(n, seeds, a, b, "precision_at_5_nghiem_ngat" + suffix, la, lb, UserMetrics::strictPrecisionAt5);
    }

    /** Hai danh sách phải cùng người, cùng thứ tự; lệch là lỗi lập trình, ghép nhầm sẽ cho kiểm định sai âm thầm. */
    private static void checkSameUsers(List<UserMetrics> p, List<UserMetrics> other, String otherName) {
        if (other == null || other.size() != p.size()) {
            throw new IllegalStateException("Thiếu chỉ số của " + otherName + " để ghép cặp với P");
        }
        for (int i = 0; i < p.size(); i++) {
            if (p.get(i).userId() != other.get(i).userId()) {
                throw new IllegalStateException("Ghép cặp lệch người ở vị trí " + i + " với " + otherName);
            }
        }
    }

    // ---------- Hàm phụ (để mức package cho test) ----------

    /** Trung bình, ms. Cộng long (nano giây) nên không sai số làm tròn trong lúc cộng. */
    static double meanMs(long[] nanos) {
        if (nanos.length == 0) {
            throw new IllegalArgumentException("Không có lần đo nào");
        }
        long total = 0;
        for (long value : nanos) {
            total += value;
        }
        return total / (double) nanos.length / 1e6;
    }

    /** p95 theo thứ hạng gần nhất: phần tử thứ ⌈95n/100⌉ (đếm từ 1) sau khi xếp tăng dần (D61), ms. */
    static double p95Ms(long[] nanos) {
        if (nanos.length == 0) {
            throw new IllegalArgumentException("Không có lần đo nào");
        }
        long[] sorted = nanos.clone();
        Arrays.sort(sorted);
        // ⌈95n/100⌉ tính bằng số nguyên: (95n + 99) / 100, không qua số thực nên không lệch vì làm tròn.
        int position = (95 * sorted.length + 99) / 100;
        return sorted[position - 1] / 1e6;
    }

    /** Gộp mọi vị trí của mọi danh sách: top 5 và cả danh sách (D70 d, người dùng chốt 2026-11-13). */
    static FewSlotShares fewSlotShares(List<List<Long>> lists, Set<Long> fewSlotIds) {
        int top5Positions = 0;
        int top5Few = 0;
        int allPositions = 0;
        int allFew = 0;
        for (List<Long> list : lists) {
            for (int i = 0; i < list.size(); i++) {
                boolean few = fewSlotIds.contains(list.get(i));
                allPositions++;
                if (few) {
                    allFew++;
                }
                if (i < RankingMetrics.K) {
                    top5Positions++;
                    if (few) {
                        top5Few++;
                    }
                }
            }
        }
        return new FewSlotShares(ratio(top5Few, top5Positions), ratio(allFew, allPositions));
    }

    private static double ratio(int part, int whole) {
        return whole == 0 ? Double.NaN : (double) part / whole;
    }

    // ---------- Một bộ dữ liệu đã gán nhãn ----------

    /**
     * Người dùng, hồ sơ tự khai cho lõi, và nhãn thật của mọi cặp (u đủ điều kiện, v) tính một lần.
     * Mã người dùng là 1..N theo thứ tự (SyntheticDataGenerator), nên vị trí trong danh sách = mã − 1.
     */
    private static final class Dataset {
        final List<SimUser> users;
        final List<MatchProfile> profiles;
        /** Vị trí của người dùng đủ điều kiện: có ít nhất một kỹ năng muốn học (D60). */
        final List<Integer> eligible = new ArrayList<>();
        final Relevance[][] labels;
        final List<List<Integer>> idealRels = new ArrayList<>();

        Dataset(List<SimUser> users, GroundTruthLabeler labeler) {
            this.users = users;
            this.profiles = users.stream().map(user -> user.toMatchProfile(CATALOG)).toList();
            int n = users.size();
            this.labels = new Relevance[n][];
            for (int u = 0; u < n; u++) {
                if (users.get(u).id() != u + 1) {
                    throw new IllegalStateException("Mã người dùng phải là 1..N theo thứ tự");
                }
                idealRels.add(List.of());
                if (!users.get(u).learn().isEmpty()) {
                    eligible.add(u);
                }
            }
            for (int u : eligible) {
                Relevance[] row = new Relevance[n];
                List<Integer> ideal = new ArrayList<>();
                for (int v = 0; v < n; v++) {
                    row[v] = labeler.label(users.get(u), users.get(v));
                    // Tập lý tưởng: mọi v qua Bước 1 trên giá trị thật (D57).
                    if (row[v] != Relevance.LOAI) {
                        ideal.add(row[v].rel());
                    }
                }
                labels[u] = row;
                idealRels.set(u, List.copyOf(ideal));
            }
        }

        /** rel của danh sách theo thứ tự; ứng viên LOAI lọt vào danh sách thành 0 (D55). */
        List<Integer> relsOf(int u, List<Long> list) {
            List<Relevance> listLabels = new ArrayList<>(list.size());
            for (long v : list) {
                listLabels.add(labels[u][(int) v - 1]);
            }
            return RankingMetrics.relsInList(listLabels);
        }
    }
}
