package com.skillexchange.evaluation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.skillexchange.evaluation.ExperimentConfig.Phase;
import com.skillexchange.matching.MatchingWeights;

/**
 * Ghi một thư mục kết quả của lượt chạy (D72; docs/To_chuc_ket_qua.md §3.2):
 * README.md, metrics.csv, per_user.csv, kiem_dinh.csv, log.txt.
 *
 * Thư mục lượt chạy đã có thì từ chối ngay, không ghi gì (TC244). CSV và log ghi dần trong lúc chạy;
 * README.md ghi lúc close(), nên lượt chạy hỏng vẫn có README kèm lý do (D72).
 */
public final class ResultWriter implements AutoCloseable {

    // ---------- Thông tin của lượt chạy ----------

    /** Commit đang chạy và backend/ có thay đổi chưa commit không (D71). known = false khi không đọc được git. */
    public record GitInfo(String commit, boolean known, boolean dirty) {

        /** Không đọc được git: coi như có thay đổi, để giai đoạn báo cáo chắc chắn dừng. */
        public static final GitInfo UNKNOWN = new GitInfo("không rõ", false, true);

        /** Gọi git trong thư mục dir (backend/); git lỗi hoặc không cài thì UNKNOWN. */
        public static GitInfo read(Path dir) {
            Optional<String> commit = runGit(dir, "rev-parse", "--short", "HEAD");
            // "-- ." chỉ xét thư mục hiện tại (backend/): sửa tài liệu ngoài backend/ không làm dừng lượt chạy.
            Optional<String> status = runGit(dir, "status", "--porcelain", "--", ".");
            if (commit.isEmpty() || status.isEmpty()) {
                return UNKNOWN;
            }
            return new GitInfo(commit.get().strip(), true, !status.get().isBlank());
        }

        private static Optional<String> runGit(Path dir, String... args) {
            List<String> command = new ArrayList<>();
            command.add("git");
            command.addAll(List.of(args));
            try {
                Process process = new ProcessBuilder(command)
                        .directory(dir.toFile())
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();
                // Đọc hết đầu ra trước khi waitFor: đầu ra lớn mà không đọc thì tiến trình con có thể bị treo.
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return process.waitFor() == 0 ? Optional.of(output) : Optional.empty();
            } catch (IOException e) {
                return Optional.empty(); // không tìm thấy lệnh git
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
    }

    /** Cấu hình máy cho README.md (NFR-03, D61). ramBytes ≤ 0 = không đọc được. */
    public record MachineInfo(String cpu, long ramBytes, String java) {

        public static MachineInfo current() {
            String cpu = System.getenv("PROCESSOR_IDENTIFIER"); // biến có sẵn trên Windows (kế hoạch bước 7)
            long ram = -1;
            OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
            // Bản của JDK (com.sun.management) mới có tổng RAM; bản chuẩn của java.lang.management thì không.
            if (os instanceof com.sun.management.OperatingSystemMXBean jdkOs) {
                ram = jdkOs.getTotalMemorySize();
            }
            return new MachineInfo(cpu == null || cpu.isBlank() ? "không rõ" : cpu, ram, Runtime.version().toString());
        }
    }

    /** Mọi thứ README.md cần biết về lượt chạy. command là lệnh đã gõ, để chạy lại được. */
    public record RunInfo(String ten, String command, ExperimentConfig config, GitInfo git, MachineInfo machine) {
    }

    // ---------- Một dòng của từng file CSV ----------

    /** Một dòng của metrics.csv. giaTri đã định dạng bằng number() hoặc count(). */
    public record MetricRow(long seed, String vaiTroSeed, int n, String mucNhieu, String phuongAn,
            String chiSo, String giaTri, int soNguoiTinh) {
    }

    /** Một dòng của kiem_dinh.csv (D62). Ô không có giá trị là chuỗi rỗng. */
    public record TestRow(int n, String mucNhieu, String seeds, String phuongAn, String soVoi, String chiSo,
            int soCap, String hieuTrungBinh, String ciThap, String ciCao, int nWilcoxon,
            String wCong, String z, String p, String ghiChu) {
    }

    private static final List<String> METRICS_COLUMNS = List.of("seed", "vai_tro_seed", "n", "muc_nhieu",
            "phuong_an", "chi_so", "gia_tri", "so_nguoi_tinh");
    private static final List<String> PER_USER_COLUMNS = List.of("seed", "n", "muc_nhieu", "phuong_an", "user_id",
            "so_ung_vien", "precision_at_5", "precision_at_5_nghiem_ngat", "ndcg_at_5", "so_muc_nhan_ly_tuong");
    private static final List<String> TEST_COLUMNS = List.of("n", "muc_nhieu", "seeds", "phuong_an", "so_voi",
            "chi_so", "so_cap", "hieu_trung_binh", "ci_thap", "ci_cao", "n_wilcoxon", "w_cong", "z", "p", "ghi_chu");

    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Path runDir;
    private final RunInfo info;
    private final Consumer<String> console;
    private final BufferedWriter metrics;
    private final BufferedWriter perUser;
    private final BufferedWriter tests;
    private final BufferedWriter log;
    private final ZonedDateTime startedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    private final long startNanos = System.nanoTime();

    private boolean finished;
    private String failure; // null = chưa ghi nhận lỗi
    private boolean closed;

    private ResultWriter(Path runDir, RunInfo info, Consumer<String> console) throws IOException {
        this.runDir = runDir;
        this.info = info;
        this.console = console;
        this.metrics = open("metrics.csv");
        this.perUser = open("per_user.csv");
        this.tests = open("kiem_dinh.csv");
        this.log = open("log.txt");
        writeRow(metrics, METRICS_COLUMNS);
        writeRow(perUser, PER_USER_COLUMNS);
        writeRow(tests, TEST_COLUMNS);
    }

    /** Tạo thư mục lượt chạy (và thư mục cha còn thiếu). Đã có thì IllegalStateException, không ghi gì (D72). */
    public static ResultWriter create(Path runDir, RunInfo info, Consumer<String> console) {
        try {
            Path parent = runDir.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            // createDirectory (không phải createDirectories) báo lỗi khi thư mục đã có, và kiểm + tạo là một
            // thao tác của hệ điều hành: hai lượt chạy cùng lúc không thể cùng ghi vào một thư mục.
            Files.createDirectory(runDir);
            return new ResultWriter(runDir, info, console);
        } catch (FileAlreadyExistsException e) {
            throw new IllegalStateException("Thư mục kết quả đã tồn tại, không ghi đè: " + runDir + " (D72)");
        } catch (IOException e) {
            throw new UncheckedIOException("Không tạo được thư mục kết quả " + runDir, e);
        }
    }

    // ---------- Ghi dữ liệu ----------

    public void metric(MetricRow row) {
        writeRow(metrics, List.of(String.valueOf(row.seed()), row.vaiTroSeed(), String.valueOf(row.n()),
                row.mucNhieu(), row.phuongAn(), row.chiSo(), row.giaTri(), String.valueOf(row.soNguoiTinh())));
    }

    public void perUser(long seed, int n, String mucNhieu, String phuongAn, UserMetrics m) {
        writeRow(perUser, List.of(String.valueOf(seed), String.valueOf(n), mucNhieu, phuongAn,
                String.valueOf(m.userId()), String.valueOf(m.candidateCount()), number(m.precisionAt5()),
                number(m.strictPrecisionAt5()), number(m.ndcgAt5()), String.valueOf(m.idealLevelCount())));
    }

    public void test(TestRow r) {
        writeRow(tests, List.of(String.valueOf(r.n()), r.mucNhieu(), r.seeds(), r.phuongAn(), r.soVoi(), r.chiSo(),
                String.valueOf(r.soCap()), r.hieuTrungBinh(), r.ciThap(), r.ciCao(), String.valueOf(r.nWilcoxon()),
                r.wCong(), r.z(), r.p(), r.ghiChu()));
    }

    /** Một dòng log, kèm giờ. Ghi ngay xuống đĩa để lượt chạy hỏng vẫn còn log tới chỗ hỏng. */
    public void log(String message) {
        String line = LocalTime.now().format(LOG_TIME) + " " + message;
        try {
            log.write(line);
            log.write('\n');
            log.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        console.accept(line);
    }

    /** Đánh dấu lượt chạy xong. Không gọi thì close() ghi README là hỏng. */
    public void finish() {
        finished = true;
    }

    public void fail(String reason) {
        failure = reason;
    }

    /** Đóng các file rồi ghi README.md, kể cả khi lượt chạy hỏng. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            for (BufferedWriter writer : List.of(metrics, perUser, tests, log)) {
                writer.close();
            }
            Files.writeString(runDir.resolve("README.md"), readme(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---------- Định dạng ----------

    /** Số thực: Double.toString đọc lại được đúng giá trị và không phụ thuộc Locale (luôn dấu chấm). */
    public static String number(double value) {
        return Double.toString(value);
    }

    public static String count(int value) {
        return Integer.toString(value);
    }

    private BufferedWriter open(String file) throws IOException {
        // CREATE_NEW: thêm một lớp an toàn, file đã có thì lỗi chứ không ghi đè.
        return Files.newBufferedWriter(runDir.resolve(file), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }

    /** CSV không dùng dấu ngoặc, nên từ chối giá trị có dấu phẩy, ngoặc kép hay xuống dòng thay vì ghi hỏng file. */
    private static void writeRow(BufferedWriter writer, List<String> values) {
        for (String value : values) {
            if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
                throw new IllegalArgumentException("Giá trị CSV không được chứa dấu phẩy, ngoặc kép hay xuống dòng: " + value);
            }
        }
        try {
            // Luôn "\n" (không theo hệ điều hành) để file giống nhau từng byte trên mọi máy.
            writer.write(String.join(",", values));
            writer.write('\n');
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String readme() {
        ExperimentConfig config = info.config();
        String status = failure != null ? "Hỏng: " + failure
                : finished ? "Xong" : "Hỏng: dừng giữa chừng";
        double seconds = (System.nanoTime() - startNanos) / 1e9;

        StringBuilder md = new StringBuilder();
        md.append("# Lượt chạy thực nghiệm ").append(info.ten()).append("\n\n");
        md.append("| Mục | Giá trị |\n| --- | --- |\n");
        row(md, "Trạng thái", status);
        row(md, "Lệnh", "`" + info.command() + "`");
        row(md, "Giai đoạn", config.phase().cliName());
        row(md, "Seed", config.seeds().stream().map(String::valueOf).collect(Collectors.joining(", ")));
        if (config.phase() == Phase.BAO_CAO) {
            row(md, "Cấu hình đã chọn (D69)", "trọng số " + config.selectedWeights().phuongAn() + " "
                    + weightsText(config.selectedWeights().weights()) + ", ScheduleFit " + config.selectedFormula().ten());
        }
        row(md, "Commit", info.git().commit());
        row(md, "Thay đổi chưa commit trong `backend/`",
                !info.git().known() ? "không rõ" : info.git().dirty() ? "có" : "không");
        row(md, "CPU", info.machine().cpu());
        row(md, "RAM", info.machine().ramBytes() > 0
                ? String.format(Locale.ROOT, "%.1f GB", info.machine().ramBytes() / (1024.0 * 1024 * 1024))
                : "không rõ");
        row(md, "Java", info.machine().java());
        row(md, "Bắt đầu", startedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        row(md, "Thời gian chạy", String.format(Locale.ROOT, "%.1f giây", seconds));

        md.append("\n## File\n\n");
        md.append("- `metrics.csv`: chỉ số gộp của từng (seed, N, mức nhiễu, phương án), dạng dài (D72).\n");
        md.append("- `per_user.csv`: chỉ số của từng người dùng, dùng cho kiểm định theo cặp.\n");
        md.append("- `kiem_dinh.csv`: bootstrap và Wilcoxon theo cặp (D62); cột ở kế hoạch tuần 5, bước 7.\n");
        md.append("- `log.txt`: log lượt chạy, có giờ.\n");
        if (config.phase() == Phase.BO_SUNG) {
            md.append("\nGiai đoạn bổ sung theo nhận xét báo cáo tuần 5 (D97–D100): B1, P và P_mot_ds (P xếp một danh sách)")
                    .append(" ở mọi quy mô; bỏ từng thành phần chia lại theo tỉ lệ (_tl); chỉ số trong từng nhóm hai chiều /")
                    .append(" một chiều (_hai_chieu, _mot_chieu). kiem_dinh.csv có cả phép gộp các seed lẫn từng seed riêng.\n");
        }
        if (config.phase() == Phase.BAO_CAO) {
            md.append("\nGhi chú latency (D61): đo trong bộ nhớ, đơn luồng, sau một lượt làm nóng. B0, B1, R chạy qua")
                    .append(" cùng lõi với P (D65) rồi lọc hoặc xếp lại, nên không phương án nào bỏ qua được phần việc")
                    .append(" của lõi; B0 giữ ít ứng viên hơn nên có thể nhanh hơn P. Tổng số ứng viên của từng phương án")
                    .append(" ghi trong `log.txt`.\n");
        }
        return md.toString();
    }

    /** Một dòng bảng Markdown; "|" trong giá trị (ví dụ lý do hỏng) làm vỡ bảng nên đổi thành "/". */
    private static void row(StringBuilder md, String label, String value) {
        String safe = value.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
        md.append("| ").append(label).append(" | ").append(safe).append(" |\n");
    }

    private static String weightsText(MatchingWeights w) {
        return "(" + w.skillFit() + ", " + w.levelFit() + ", " + w.scheduleFit() + ", " + w.modeFit()
                + ", " + w.reputation() + ")";
    }
}
