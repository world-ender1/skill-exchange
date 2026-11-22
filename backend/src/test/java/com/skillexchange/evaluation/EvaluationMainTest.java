package com.skillexchange.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.skillexchange.evaluation.EvaluationMain.Options;
import com.skillexchange.evaluation.ExperimentConfig.Phase;
import com.skillexchange.evaluation.ResultWriter.GitInfo;
import com.skillexchange.evaluation.ResultWriter.MachineInfo;

/** Tham số dòng lệnh, ma trận D70 và điều kiện dừng của D71. Không chạy thực nghiệm thật (chậm). */
class EvaluationMainTest {

    private static final GitInfo CLEAN = new GitInfo("abc1234", true, false);
    private static final GitInfo DIRTY = new GitInfo("abc1234", true, true);
    private static final MachineInfo MACHINE = new MachineInfo("CPU thử", 1L << 33, "21");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);

    @TempDir
    Path temp;

    // ---------- Đọc tham số (D71) ----------

    @Test
    @DisplayName("D71 Giai đoạn chọn chỉ cần --giai-doan và --ten")
    void d71_parseSelectionPhase() {
        Options options = EvaluationMain.parse(args("--giai-doan", "chon", "--ten", "chon-cau-hinh"));

        assertThat(options.phase()).isEqualTo(Phase.CHON);
        assertThat(options.ten()).isEqualTo("chon-cau-hinh");
        assertThat(options.weights()).isNull();
        assertThat(options.formula()).isNull();
        assertThat(options.out()).isNull();
    }

    @Test
    @DisplayName("D71 Giai đoạn báo cáo đọc --trong-so, --schedule, --out")
    void d71_parseReportPhase() {
        Options options = EvaluationMain.parse(args("--giai-doan", "bao-cao", "--ten", "bao_cao",
                "--trong-so", "ky_nang", "--schedule", "tran3", "--out", "tam/run"));

        assertThat(options.phase()).isEqualTo(Phase.BAO_CAO);
        assertThat(options.weights()).isSameAs(WeightSets.KY_NANG);
        assertThat(options.formula()).isEqualTo(ScheduleFitFormula.TRAN_3);
        assertThat(options.out()).isEqualTo(Path.of("tam/run"));
    }

    @Test
    @DisplayName("D71 --trong-so nhận đúng 4 tên của bộ độ nhạy, --schedule đúng 3 tên công thức")
    void d71_parseAllNames() {
        String[][] weights = {{"mac_dinh", "P"}, {"ky_nang", "P_w_ky_nang"}, {"lich", "P_w_lich"}, {"uy_tin", "P_w_uy_tin"}};
        for (String[] pair : weights) {
            Options options = EvaluationMain.parse(args("--giai-doan", "bao-cao", "--ten", "t",
                    "--trong-so", pair[0], "--schedule", "min"));
            assertThat(options.weights().phuongAn()).as(pair[0]).isEqualTo(pair[1]);
        }
        String[][] formulas = {{"min", "MIN"}, {"jaccard", "JACCARD"}, {"tran3", "TRAN_3"}};
        for (String[] pair : formulas) {
            Options options = EvaluationMain.parse(args("--giai-doan", "bao-cao", "--ten", "t",
                    "--trong-so", "mac_dinh", "--schedule", pair[0]));
            assertThat(options.formula().name()).as(pair[0]).isEqualTo(pair[1]);
        }
    }

    @Test
    @DisplayName("D71 Tham số sai bị từ chối: thiếu, lạ, lặp, thiếu giá trị, tên không hợp lệ, --trong-so/--schedule sai giai đoạn")
    void d71_invalidArgumentsAreRejected() {
        String[][] invalid = {
                {},
                {"--ten", "thu"},
                {"--giai-doan", "chon"},
                {"--giai-doan", "xyz", "--ten", "thu"},
                {"--giai-doan", "chon", "--ten"},
                {"--giai-doan", "chon", "--ten", "thu", "--la", "x"},
                {"--giai-doan", "chon", "--giai-doan", "chon", "--ten", "thu"},
                {"--giai-doan", "chon", "--ten", "thử"},
                {"--giai-doan", "chon", "--ten", "a b"},
                {"--giai-doan", "chon", "--ten", "../ngoai"},
                {"--giai-doan", "chon", "--ten", "thu", "--trong-so", "mac_dinh"},
                {"--giai-doan", "chon", "--ten", "thu", "--schedule", "min"},
                {"--giai-doan", "bao-cao", "--ten", "thu"},
                {"--giai-doan", "bao-cao", "--ten", "thu", "--trong-so", "ky_nang"},
                {"--giai-doan", "bao-cao", "--ten", "thu", "--schedule", "min"},
                {"--giai-doan", "bao-cao", "--ten", "thu", "--trong-so", "abc", "--schedule", "min"},
                {"--giai-doan", "bao-cao", "--ten", "thu", "--trong-so", "mac_dinh", "--schedule", "abc"},
        };
        for (String[] input : invalid) {
            assertThatThrownBy(() -> EvaluationMain.parse(input)).as(String.join(" ", input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ---------- Ma trận D70 ----------

    @Test
    @DisplayName("D70 Giai đoạn chọn: seed 1–2, N = 500")
    void d70_selectionMatrix() {
        ExperimentConfig config = EvaluationMain.configFor(EvaluationMain.parse(args("--giai-doan", "chon", "--ten", "t")));

        assertThat(config.phase()).isEqualTo(Phase.CHON);
        assertThat(config.seeds()).containsExactly(1L, 2L);
        assertThat(config.mainN()).isEqualTo(500);
    }

    @Test
    @DisplayName("D70 Giai đoạn báo cáo: seed 3–5, N = 500, quy mô 50/100/500/1000, latency N = 1000 seed 3, bootstrap 2000, cấu hình đã chọn")
    void d70_reportMatrix() {
        ExperimentConfig config = EvaluationMain.configFor(EvaluationMain.parse(args("--giai-doan", "bao-cao",
                "--ten", "t", "--trong-so", "lich", "--schedule", "jaccard")));

        assertThat(config.phase()).isEqualTo(Phase.BAO_CAO);
        assertThat(config.seeds()).containsExactly(3L, 4L, 5L);
        assertThat(config.mainN()).isEqualTo(500);
        assertThat(config.scaleNs()).containsExactly(50, 100, 500, 1000);
        assertThat(config.latencyN()).isEqualTo(1000);
        assertThat(config.latencySeed()).isEqualTo(3);
        assertThat(config.bootstrapTimes()).isEqualTo(2000);
        assertThat(config.selectedWeights()).isSameAs(WeightSets.LICH);
        assertThat(config.selectedFormula()).isEqualTo(ScheduleFitFormula.JACCARD);
    }

    @Test
    @DisplayName("D97 Giai đoạn bổ sung chỉ cần --giai-doan, --ten; không nhận --trong-so, --schedule; dùng seed, N, quy mô, bootstrap như báo cáo")
    void d97_parseSupplementPhase() {
        Options options = EvaluationMain.parse(args("--giai-doan", "bo-sung", "--ten", "bo-sung"));

        assertThat(options.phase()).isEqualTo(Phase.BO_SUNG);
        assertThat(options.weights()).isNull();
        assertThat(options.formula()).isNull();
        ExperimentConfig config = EvaluationMain.configFor(options);
        assertThat(config.phase()).isEqualTo(Phase.BO_SUNG);
        assertThat(config.seeds()).containsExactly(3L, 4L, 5L);
        assertThat(config.mainN()).isEqualTo(500);
        assertThat(config.scaleNs()).containsExactly(50, 100, 500, 1000);
        assertThat(config.bootstrapTimes()).isEqualTo(2000);
        assertThatThrownBy(() -> EvaluationMain.parse(args("--giai-doan", "bo-sung", "--ten", "t", "--trong-so", "mac_dinh")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("D97 Giai đoạn bổ sung cũng dừng khi backend/ có thay đổi chưa commit hoặc không rõ commit (số đi vào báo cáo)")
    void d97_supplementNeedsCleanKnownCommit() {
        assertThat(EvaluationMain.refusal(Phase.BO_SUNG, DIRTY)).isPresent();
        assertThat(EvaluationMain.refusal(Phase.BO_SUNG, GitInfo.UNKNOWN)).isPresent();
        assertThat(EvaluationMain.refusal(Phase.BO_SUNG, CLEAN)).isEmpty();
    }

    // ---------- Điều kiện dừng và thư mục (D71, D72) ----------

    @Test
    @DisplayName("TC247 Giai đoạn báo cáo dừng khi backend/ có thay đổi chưa commit hoặc không rõ commit; giai đoạn chọn vẫn chạy")
    void tc247_reportPhaseNeedsCleanKnownCommit() {
        assertThat(EvaluationMain.refusal(Phase.BAO_CAO, DIRTY)).isPresent();
        assertThat(EvaluationMain.refusal(Phase.BAO_CAO, GitInfo.UNKNOWN)).isPresent();
        // Không rõ commit thì dừng, không phụ thuộc cờ dirty.
        assertThat(EvaluationMain.refusal(Phase.BAO_CAO, new GitInfo("không rõ", false, false))).isPresent();
        assertThat(EvaluationMain.refusal(Phase.BAO_CAO, CLEAN)).isEmpty();
        assertThat(EvaluationMain.refusal(Phase.CHON, DIRTY)).isEmpty();
        assertThat(EvaluationMain.refusal(Phase.CHON, GitInfo.UNKNOWN)).isEmpty();
    }

    @Test
    @DisplayName("TC247 Chạy giai đoạn báo cáo khi có thay đổi chưa commit: mã thoát 1, báo lý do, không tạo thư mục kết quả")
    void tc247_dirtyReportRunCreatesNothing() {
        Path backend = temp.resolve("backend");
        Path out = temp.resolve("out");
        List<String> console = new ArrayList<>();

        int exit = EvaluationMain.run(args("--giai-doan", "bao-cao", "--ten", "thu", "--trong-so", "mac_dinh",
                "--schedule", "min", "--out", out.toString()), backend, TODAY, DIRTY, MACHINE, console::add);

        assertThat(exit).isEqualTo(1);
        assertThat(out).doesNotExist();
        assertThat(String.join("\n", console)).contains("chưa commit");
    }

    @Test
    @DisplayName("D71 Tham số sai: mã thoát 1, không tạo thư mục kết quả mặc định")
    void d71_invalidArgumentsCreateNothing() {
        Path backend = temp.resolve("backend");
        List<String> console = new ArrayList<>();

        int exit = EvaluationMain.run(args("--giai-doan", "chon"), backend, TODAY, CLEAN, MACHINE, console::add);

        assertThat(exit).isEqualTo(1);
        assertThat(temp.resolve("thuc_nghiem")).doesNotExist();
        assertThat(console).isNotEmpty();
    }

    @Test
    @DisplayName("TC244 Chạy vào --out đã tồn tại: mã thoát 1, không tạo hay sửa file nào trong thư mục đó")
    void tc244_existingOutDirectoryIsRefused() throws IOException {
        Path out = temp.resolve("out");
        Files.createDirectory(out);
        Path old = out.resolve("README.md");
        Files.writeString(old, "cu\n");

        int exit = EvaluationMain.run(args("--giai-doan", "chon", "--ten", "thu", "--out", out.toString()),
                temp.resolve("backend"), TODAY, CLEAN, MACHINE, line -> { });

        assertThat(exit).isEqualTo(1);
        try (Stream<Path> files = Files.list(out)) {
            assertThat(files).containsExactly(old);
        }
        assertThat(Files.readString(old)).isEqualTo("cu\n");
    }

    @Test
    @DisplayName("D72 Thư mục mặc định: <backend>/../thuc_nghiem/ket_qua/<yyyy-MM-dd>_<tên>; --out thay cả thư mục lượt chạy")
    void d72_defaultAndOverriddenRunDirectory() {
        Path backend = temp.resolve("backend");
        Options byDefault = EvaluationMain.parse(args("--giai-doan", "chon", "--ten", "thu"));
        Options withOut = EvaluationMain.parse(args("--giai-doan", "chon", "--ten", "thu", "--out", temp.resolve("x").toString()));

        assertThat(EvaluationMain.runDir(byDefault, backend, TODAY))
                .isEqualTo(temp.resolve("thuc_nghiem").resolve("ket_qua").resolve("2026-11-13_thu"));
        assertThat(EvaluationMain.runDir(withOut, backend, TODAY)).isEqualTo(temp.resolve("x"));
    }

    private static String[] args(String... values) {
        return values;
    }
}
