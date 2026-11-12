package com.skillexchange.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.skillexchange.evaluation.PairedStatistics.ConfidenceInterval;
import com.skillexchange.evaluation.PairedStatistics.WilcoxonResult;
import com.skillexchange.evaluation.ResultWriter.GitInfo;
import com.skillexchange.evaluation.ResultWriter.MachineInfo;
import com.skillexchange.evaluation.ResultWriter.RunInfo;

/**
 * Chạy thực nghiệm bản nhỏ (N = 50) vào @TempDir: TC245, TC246 và các phần của D59–D62, D70, D72.
 * Danh sách giá trị hợp lệ chép từ D72 và kế hoạch tuần 5 bước 7, không lấy từ code, để test không tự đúng.
 */
class ExperimentRunnerTest {

    private static final double EPS = 1e-9;

    private static final Set<String> VAI_TRO_SEED = Set.of("chon", "bao_cao");
    private static final Set<String> MUC_NHIEU = Set.of("khong", "mac_dinh", "cao");
    private static final Set<String> PHUONG_AN = Set.of("B0", "B1", "R", "P", "P_bo_level", "P_bo_schedule",
            "P_bo_mode", "P_w_ky_nang", "P_w_lich", "P_w_uy_tin", "P_sf_jaccard", "P_sf_tran3");
    private static final List<String> CHI_SO_DAY_DU = List.of("precision_at_5", "precision_at_5_nghiem_ngat",
            "ndcg_at_5_tat_ca", "ndcg_at_5_loai_suy_bien", "so_ca_idcg_0", "so_nguoi_bi_loai", "coverage",
            "ty_le_du_5_ung_vien", "ty_le_co_nhan_2");
    private static final List<String> CHI_SO_LATENCY = List.of("latency_tb_ms", "latency_p95_ms");
    private static final List<String> CHI_SO_IT_KHUNG = List.of("precision_at_5_it_khung",
            "precision_at_5_nghiem_ngat_it_khung", "ndcg_at_5_tat_ca_it_khung", "ndcg_at_5_loai_suy_bien_it_khung",
            "ty_le_top5_it_khung", "ty_le_danh_sach_it_khung");
    private static final Set<String> CHI_SO = union(CHI_SO_DAY_DU, CHI_SO_LATENCY, CHI_SO_IT_KHUNG);

    private static final Set<String> BAN_CHINH = Set.of("B0", "B1", "R", "P", "P_bo_level", "P_bo_schedule", "P_bo_mode");
    private static final Set<String> THEO_NHIEU = Set.of("B1", "P", "P_bo_level", "P_bo_schedule", "P_bo_mode");

    private static final List<String> METRICS_COLUMNS = List.of("seed", "vai_tro_seed", "n", "muc_nhieu",
            "phuong_an", "chi_so", "gia_tri", "so_nguoi_tinh");
    private static final List<String> PER_USER_COLUMNS = List.of("seed", "n", "muc_nhieu", "phuong_an", "user_id",
            "so_ung_vien", "precision_at_5", "precision_at_5_nghiem_ngat", "ndcg_at_5", "so_muc_nhan_ly_tuong");

    @TempDir
    Path temp;

    // ---------- Cấu hình nhỏ ----------

    /** Báo cáo bản nhỏ: N = 50, seed 3; chọn ky_nang và jaccard để có cả hai phương án đã chọn (D70 a). */
    private static ExperimentConfig smallReport(List<Long> seeds, WeightSets.Named weights, ScheduleFitFormula formula) {
        return ExperimentConfig.baoCao(seeds, 50, List.of(50), 50, 3, 2000, weights, formula);
    }

    private static ExperimentConfig smallReport() {
        return smallReport(List.of(3L), WeightSets.KY_NANG, ScheduleFitFormula.JACCARD);
    }

    private Path runInto(String name, ExperimentConfig config) {
        Path dir = temp.resolve(name);
        RunInfo info = new RunInfo("thu", "lenh thu", config, new GitInfo("abc1234", true, false),
                new MachineInfo("CPU thử", 8L * 1024 * 1024 * 1024, "21"));
        ExperimentRunner.execute(dir, info, line -> { });
        return dir;
    }

    // ---------- TC245, TC246 ----------

    @Test
    @DisplayName("TC245 Chạy thử N = 50, một seed: đủ 5 file, đúng số cột, mọi giá trị thuộc danh sách của D72")
    void tc245_smallRunProducesValidFiles() throws IOException {
        Path dir = runInto("run", smallReport());

        for (String file : List.of("README.md", "metrics.csv", "per_user.csv", "kiem_dinh.csv", "log.txt")) {
            assertThat(dir.resolve(file)).as(file).isRegularFile();
        }
        assertThat(Files.readString(dir.resolve("README.md")))
                .contains("Lệnh", "Seed", "abc1234", "CPU", "RAM", "Java", "Thời gian chạy");
        assertThat(Files.readString(dir.resolve("log.txt"))).isNotBlank();

        List<String> metricLines = Files.readAllLines(dir.resolve("metrics.csv"));
        assertThat(metricLines.get(0).split(",", -1)).containsExactlyElementsOf(METRICS_COLUMNS);
        List<Map<String, String>> metrics = readCsv(dir.resolve("metrics.csv"));
        assertThat(metrics).isNotEmpty();
        for (Map<String, String> row : metrics) {
            assertThat(VAI_TRO_SEED).as("vai_tro_seed").contains(row.get("vai_tro_seed"));
            assertThat(MUC_NHIEU).as("muc_nhieu").contains(row.get("muc_nhieu"));
            assertThat(PHUONG_AN).as("phuong_an").contains(row.get("phuong_an"));
            assertThat(CHI_SO).as("chi_so").contains(row.get("chi_so"));
        }

        List<String> perUserLines = Files.readAllLines(dir.resolve("per_user.csv"));
        assertThat(perUserLines.get(0).split(",", -1)).containsExactlyElementsOf(PER_USER_COLUMNS);
        for (Map<String, String> row : readCsv(dir.resolve("per_user.csv"))) {
            assertThat(MUC_NHIEU).contains(row.get("muc_nhieu"));
            assertThat(PHUONG_AN).contains(row.get("phuong_an"));
        }
        assertThat(readCsv(dir.resolve("kiem_dinh.csv"))).isNotEmpty();
    }

    @Test
    @DisplayName("TC246 Chạy hai lần cùng tham số: metrics.csv giống hệt trừ dòng latency; per_user.csv, kiem_dinh.csv giống hệt")
    void tc246_sameParametersSameResults() throws IOException {
        Path first = runInto("a", smallReport());
        Path second = runInto("b", smallReport());

        List<String> firstMetrics = withoutLatency(Files.readAllLines(first.resolve("metrics.csv")));
        List<String> secondMetrics = withoutLatency(Files.readAllLines(second.resolve("metrics.csv")));
        assertThat(firstMetrics).hasSizeGreaterThan(1).isEqualTo(secondMetrics);
        assertThat(Files.readAllLines(second.resolve("per_user.csv")))
                .isEqualTo(Files.readAllLines(first.resolve("per_user.csv")));
        assertThat(Files.readAllLines(second.resolve("kiem_dinh.csv")))
                .isEqualTo(Files.readAllLines(first.resolve("kiem_dinh.csv")));
    }

    private static List<String> withoutLatency(List<String> lines) {
        return lines.stream().filter(line -> !line.contains(",latency_")).toList();
    }

    // ---------- D70: các ô của từng giai đoạn ----------

    @Test
    @DisplayName("D70 Giai đoạn chọn: P với 4 bộ trọng số và 3 công thức, nhiễu mặc định; không latency, không _it_khung, không kiểm định")
    void d70_selectionPhase() throws IOException {
        Path dir = runInto("chon", ExperimentConfig.chon(List.of(1L), 50));
        List<Map<String, String>> metrics = readCsv(dir.resolve("metrics.csv"));

        assertThat(column(metrics, "phuong_an")).containsExactlyInAnyOrder(
                "P", "P_w_ky_nang", "P_w_lich", "P_w_uy_tin", "P_sf_jaccard", "P_sf_tran3");
        assertThat(column(metrics, "vai_tro_seed")).containsExactly("chon");
        assertThat(column(metrics, "muc_nhieu")).containsExactly("mac_dinh");
        assertThat(column(metrics, "chi_so")).containsExactlyInAnyOrderElementsOf(CHI_SO_DAY_DU);
        assertThat(readCsv(dir.resolve("per_user.csv"))).hasSize(6 * 50);
        assertThat(Files.readAllLines(dir.resolve("kiem_dinh.csv"))).hasSize(1); // chỉ dòng tên cột
    }

    @Test
    @DisplayName("D70 Giai đoạn báo cáo: đúng phương án ở từng ô; cấu hình đã chọn thành hai phương án riêng; P_sf_tran3 chỉ có _it_khung; không dòng lặp")
    void d70_reportPhaseCells() throws IOException {
        Path dir = runInto("bao_cao", smallReport());
        List<Map<String, String>> metrics = readCsv(dir.resolve("metrics.csv"));

        Set<String> keys = new HashSet<>();
        for (Map<String, String> row : metrics) {
            String key = row.get("seed") + "|" + row.get("n") + "|" + row.get("muc_nhieu") + "|"
                    + row.get("phuong_an") + "|" + row.get("chi_so");
            assertThat(keys.add(key)).as("dòng lặp %s", key).isTrue();
        }
        assertThat(column(metrics, "vai_tro_seed")).containsExactly("bao_cao");

        // (a) bảng chính, cộng P_w_ky_nang và P_sf_jaccard (hai chiều đã chọn, chạy riêng).
        assertThat(phuongAnWith(metrics, "mac_dinh", "precision_at_5"))
                .containsExactlyInAnyOrderElementsOf(union(BAN_CHINH, List.of("P_w_ky_nang", "P_sf_jaccard")));
        // (c) theo mức nhiễu.
        assertThat(phuongAnWith(metrics, "khong", "precision_at_5")).containsExactlyInAnyOrderElementsOf(THEO_NHIEU);
        assertThat(phuongAnWith(metrics, "cao", "precision_at_5")).containsExactlyInAnyOrderElementsOf(THEO_NHIEU);
        // (d) nhóm khai ít khung: đủ 6 chỉ số cho P và hai công thức khác.
        for (String chiSo : CHI_SO_IT_KHUNG) {
            assertThat(phuongAnWith(metrics, "mac_dinh", chiSo)).as(chiSo)
                    .containsExactlyInAnyOrder("P", "P_sf_jaccard", "P_sf_tran3");
        }
        // P_sf_tran3 không được chọn: seed báo cáo chỉ có chỉ số _it_khung của nó (spec 5.4).
        assertThat(rows(metrics, row -> row.get("phuong_an").equals("P_sf_tran3")))
                .allMatch(row -> row.get("chi_so").endsWith("_it_khung"));
        // (e) latency: B0, B1, R, P ở N = latencyN, seed latencySeed, giá trị dương.
        for (String chiSo : CHI_SO_LATENCY) {
            List<Map<String, String>> latency = rows(metrics, row -> row.get("chi_so").equals(chiSo));
            assertThat(column(latency, "phuong_an")).containsExactlyInAnyOrder("B0", "B1", "R", "P");
            assertThat(latency).allMatch(row -> Double.parseDouble(row.get("gia_tri")) > 0
                    && row.get("seed").equals("3") && row.get("so_nguoi_tinh").equals("50"));
        }
        // per_user.csv chỉ có ô có chỉ số đầy đủ.
        assertThat(column(readCsv(dir.resolve("per_user.csv")), "phuong_an")).doesNotContain("P_sf_tran3");
    }

    @Test
    @DisplayName("D70 Cấu hình đã chọn là mặc định (mac_dinh, min): bảng chính không thêm phương án nào")
    void d70_defaultSelectionAddsNothing() throws IOException {
        Path dir = runInto("mac_dinh", smallReport(List.of(3L), WeightSets.MAC_DINH, ScheduleFitFormula.MIN));
        List<Map<String, String>> metrics = readCsv(dir.resolve("metrics.csv"));

        assertThat(phuongAnWith(metrics, "mac_dinh", "precision_at_5")).containsExactlyInAnyOrderElementsOf(BAN_CHINH);
    }

    // ---------- D59, D72: metrics.csv khớp per_user.csv ----------

    @Test
    @DisplayName("D59 Chỉ số gộp của metrics.csv khớp per_user.csv: Precision@5 trên mọi người, NDCG@5 bản (2) trên người có ≥ 2 mức nhãn")
    void d59_metricsMatchPerUserRows() throws IOException {
        Path dir = runInto("run", smallReport());
        List<Map<String, String>> metrics = readCsv(dir.resolve("metrics.csv"));
        List<Map<String, String>> perUser = rows(readCsv(dir.resolve("per_user.csv")),
                row -> row.get("muc_nhieu").equals("mac_dinh") && row.get("phuong_an").equals("P"));
        assertThat(perUser).hasSize(50);

        Map<String, String> precision = cell(metrics, "mac_dinh", "P", "precision_at_5");
        assertThat(Double.parseDouble(precision.get("gia_tri")))
                .isCloseTo(mean(perUser, "precision_at_5"), within(EPS));
        assertThat(precision.get("so_nguoi_tinh")).isEqualTo("50");

        List<Map<String, String>> nonDegenerate = rows(perUser,
                row -> Integer.parseInt(row.get("so_muc_nhan_ly_tuong")) >= 2);
        Map<String, String> ndcg2 = cell(metrics, "mac_dinh", "P", "ndcg_at_5_loai_suy_bien");
        assertThat(ndcg2.get("so_nguoi_tinh")).isEqualTo(String.valueOf(nonDegenerate.size()));
        if (!nonDegenerate.isEmpty()) {
            assertThat(Double.parseDouble(ndcg2.get("gia_tri"))).isCloseTo(mean(nonDegenerate, "ndcg_at_5"), within(EPS));
        }
        Map<String, String> excluded = cell(metrics, "mac_dinh", "P", "so_nguoi_bi_loai");
        assertThat(excluded.get("gia_tri")).isEqualTo(String.valueOf(50 - nonDegenerate.size()));
    }

    // ---------- D62: kiểm định ----------

    @Test
    @DisplayName("D62 kiem_dinh.csv: 4 phép so × 2 chỉ số, gộp các seed, chỉ người bản (2), hiệu = P − so_voi, mỗi phép một Random(3_000_000)")
    void d62_pairedTestsFromPerUserRows() throws IOException {
        Path dir = runInto("hai_seed", smallReport(List.of(3L, 4L), WeightSets.MAC_DINH, ScheduleFitFormula.MIN));
        List<Map<String, String>> perUser = rows(readCsv(dir.resolve("per_user.csv")),
                row -> row.get("muc_nhieu").equals("mac_dinh"));
        List<Map<String, String>> tests = readCsv(dir.resolve("kiem_dinh.csv"));

        assertThat(tests).hasSize(8);
        int index = 0;
        for (String soVoi : List.of("B1", "P_bo_level", "P_bo_schedule", "P_bo_mode")) {
            for (String chiSo : List.of("ndcg_at_5", "precision_at_5_nghiem_ngat")) {
                Map<String, String> row = tests.get(index++);
                String where = "P − " + soVoi + ", " + chiSo;
                assertThat(row.get("n")).isEqualTo("50");
                assertThat(row.get("muc_nhieu")).isEqualTo("mac_dinh");
                assertThat(row.get("seeds")).isEqualTo("3;4");
                assertThat(row.get("phuong_an")).isEqualTo("P");
                assertThat(row.get("so_voi")).as(where).isEqualTo(soVoi);
                assertThat(row.get("chi_so")).as(where).isEqualTo(chiSo);

                double[] diffs = diffs(perUser, soVoi, chiSo);
                assertThat(row.get("so_cap")).as(where).isEqualTo(String.valueOf(diffs.length));
                if (diffs.length == 0) {
                    assertThat(row.get("ghi_chu")).isEqualTo("khong_co_nguoi_tinh");
                    continue;
                }
                double mean = 0;
                for (double d : diffs) {
                    mean += d;
                }
                assertThat(Double.parseDouble(row.get("hieu_trung_binh"))).as(where).isCloseTo(mean / diffs.length, within(EPS));
                // Random mới cho từng phép: dùng chung một Random thì phép thứ hai trở đi ra khoảng khác.
                ConfidenceInterval ci = PairedStatistics.bootstrapMeanCi(diffs, 2000, new Random(3_000_000));
                assertThat(Double.parseDouble(row.get("ci_thap"))).as(where).isEqualTo(ci.low());
                assertThat(Double.parseDouble(row.get("ci_cao"))).as(where).isEqualTo(ci.high());

                Optional<WilcoxonResult> wilcoxon = PairedStatistics.wilcoxon(diffs);
                if (wilcoxon.isEmpty()) {
                    assertThat(row.get("n_wilcoxon")).isEqualTo("0");
                    assertThat(row.get("p")).isEmpty();
                    assertThat(row.get("ghi_chu")).isEqualTo("khong_co_cap_khac_nhau");
                } else {
                    assertThat(row.get("n_wilcoxon")).isEqualTo(String.valueOf(wilcoxon.get().n()));
                    assertThat(Double.parseDouble(row.get("w_cong"))).isEqualTo(wilcoxon.get().wPlus());
                    assertThat(Double.parseDouble(row.get("z"))).isEqualTo(wilcoxon.get().z());
                    assertThat(Double.parseDouble(row.get("p"))).isEqualTo(wilcoxon.get().p());
                }
            }
        }
    }

    /** Hiệu P − soVoi của từng người thuộc bản (2), theo thứ tự seed rồi mã (thứ tự dòng của per_user.csv). */
    private static double[] diffs(List<Map<String, String>> perUser, String soVoi, String chiSo) {
        Map<String, Map<String, String>> other = new LinkedHashMap<>();
        for (Map<String, String> row : rows(perUser, row -> row.get("phuong_an").equals(soVoi))) {
            other.put(row.get("seed") + "|" + row.get("user_id"), row);
        }
        List<Double> result = new ArrayList<>();
        for (Map<String, String> p : rows(perUser, row -> row.get("phuong_an").equals("P"))) {
            if (Integer.parseInt(p.get("so_muc_nhan_ly_tuong")) < 2) {
                continue;
            }
            Map<String, String> o = other.get(p.get("seed") + "|" + p.get("user_id"));
            result.add(Double.parseDouble(p.get(chiSo)) - Double.parseDouble(o.get(chiSo)));
        }
        return result.stream().mapToDouble(Double::doubleValue).toArray();
    }

    // ---------- D61, D70 (d): hàm phụ ----------

    @Test
    @DisplayName("D61 Latency: trung bình và p95 theo thứ hạng gần nhất (phần tử thứ ⌈95n/100⌉), đơn vị ms")
    void d61_latencyStatistics() {
        long[] twenty = millis(20);
        long[] hundred = millis(100);

        assertThat(ExperimentRunner.meanMs(new long[] {1_000_000, 3_000_000})).isCloseTo(2.0, within(EPS));
        assertThat(ExperimentRunner.p95Ms(twenty)).isCloseTo(19.0, within(EPS));     // ⌈19⌉ = 19
        assertThat(ExperimentRunner.p95Ms(hundred)).isCloseTo(95.0, within(EPS));
        assertThat(ExperimentRunner.p95Ms(new long[] {5_000_000})).isCloseTo(5.0, within(EPS));
        assertThat(ExperimentRunner.p95Ms(millis(21))).isCloseTo(20.0, within(EPS)); // ⌈19.95⌉ = 20
    }

    /** 1..count ms, xáo thứ tự để chắc hàm tự xếp. */
    private static long[] millis(int count) {
        List<Long> values = new ArrayList<>();
        for (long ms = 1; ms <= count; ms++) {
            values.add(ms * 1_000_000);
        }
        Collections.shuffle(values, new Random(7));
        return values.stream().mapToLong(Long::longValue).toArray();
    }

    @Test
    @DisplayName("D70 (d) Tỷ lệ vị trí do ứng viên khai 1–3 khung chiếm: gộp mọi vị trí của mọi người, top 5 và cả danh sách")
    void d70d_fewSlotSharesPoolAllPositions() {
        Set<Long> fewSlots = Set.of(2L, 6L);
        List<List<Long>> lists = List.of(
                List.of(1L, 2L, 3L, 4L, 5L, 6L), // top 5: 1 vị trí ít khung (mã 2); cả danh sách: 2 (mã 2, 6)
                List.of(2L),                     // top 5: 1/1
                List.of());                      // không có vị trí nào

        ExperimentRunner.FewSlotShares shares = ExperimentRunner.fewSlotShares(lists, fewSlots);

        assertThat(shares.top5()).isCloseTo(2.0 / 6, within(EPS));
        assertThat(shares.wholeList()).isCloseTo(3.0 / 7, within(EPS));
        ExperimentRunner.FewSlotShares empty = ExperimentRunner.fewSlotShares(List.of(List.of(), List.of()), fewSlots);
        assertThat(empty.top5()).isNaN();
        assertThat(empty.wholeList()).isNaN();
    }

    // ---------- Đọc CSV ----------

    /** Mỗi dòng (trừ dòng tên cột) thành Map tên cột → giá trị. split(",", -1) để giữ ô trống ở cuối dòng. */
    private static List<Map<String, String>> readCsv(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        String[] header = lines.get(0).split(",", -1);
        List<Map<String, String>> result = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] values = line.split(",", -1);
            assertThat(values).as("số cột của dòng %s", line).hasSize(header.length);
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 0; i < header.length; i++) {
                row.put(header[i], values[i]);
            }
            result.add(row);
        }
        return result;
    }

    private static List<Map<String, String>> rows(List<Map<String, String>> all, Predicate<Map<String, String>> filter) {
        return all.stream().filter(filter).toList();
    }

    private static Set<String> column(List<Map<String, String>> rows, String name) {
        Set<String> values = new HashSet<>();
        for (Map<String, String> row : rows) {
            values.add(row.get(name));
        }
        return values;
    }

    private static Set<String> phuongAnWith(List<Map<String, String>> metrics, String mucNhieu, String chiSo) {
        return column(rows(metrics, row -> row.get("muc_nhieu").equals(mucNhieu) && row.get("chi_so").equals(chiSo)),
                "phuong_an");
    }

    private static Map<String, String> cell(List<Map<String, String>> metrics, String mucNhieu, String phuongAn,
            String chiSo) {
        List<Map<String, String>> found = rows(metrics, row -> row.get("muc_nhieu").equals(mucNhieu)
                && row.get("phuong_an").equals(phuongAn) && row.get("chi_so").equals(chiSo));
        assertThat(found).as("%s %s %s", mucNhieu, phuongAn, chiSo).hasSize(1);
        return found.get(0);
    }

    private static double mean(List<Map<String, String>> rows, String column) {
        double sum = 0;
        for (Map<String, String> row : rows) {
            sum += Double.parseDouble(row.get(column));
        }
        return sum / rows.size();
    }

    @SafeVarargs
    private static Set<String> union(java.util.Collection<String>... parts) {
        Set<String> all = new HashSet<>();
        for (java.util.Collection<String> part : parts) {
            all.addAll(part);
        }
        return all;
    }
}
