package com.skillexchange.evaluation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.skillexchange.evaluation.ExperimentConfig.Phase;
import com.skillexchange.evaluation.ResultWriter.GitInfo;
import com.skillexchange.evaluation.ResultWriter.MachineInfo;
import com.skillexchange.evaluation.ResultWriter.RunInfo;

/**
 * Điểm vào chạy thực nghiệm từ dòng lệnh (D71). Chạy từ backend/:
 * <pre>
 * .\mvnw.cmd -q compile
 * java -cp target/classes com.skillexchange.evaluation.EvaluationMain --giai-doan chon --ten chon-cau-hinh
 * java -cp target/classes com.skillexchange.evaluation.EvaluationMain --giai-doan bao-cao --ten bao-cao \
 *      --trong-so mac_dinh --schedule min
 * java -cp target/classes com.skillexchange.evaluation.EvaluationMain --giai-doan bo-sung --ten bo-sung
 * </pre>
 * Thứ tự (kế hoạch tuần 5, bước 7): kiểm tham số → kiểm git (giai đoạn báo cáo, bổ sung) → tạo thư mục → chạy.
 * Mọi lỗi trước khi tạo thư mục đều không để lại file nào.
 */
public final class EvaluationMain {

    private static final String COMMAND_PREFIX = "java -cp target/classes com.skillexchange.evaluation.EvaluationMain ";
    private static final Set<String> KNOWN_OPTIONS = Set.of("--giai-doan", "--ten", "--trong-so", "--schedule", "--out");
    /** Tên dùng làm một phần tên thư mục: chỉ chữ không dấu, số, "-", "_", để không thoát ra thư mục khác. */
    private static final String NAME_PATTERN = "[A-Za-z0-9_-]+";
    private static final String USAGE = "Cách dùng: --giai-doan chon|bao-cao|bo-sung --ten <tên> "
            + "[--trong-so mac_dinh|ky_nang|lich|uy_tin --schedule min|jaccard|tran3 (chỉ bao-cao)] [--out <thư mục>]";

    /** Tham số đã kiểm tra. weights, formula null ở giai đoạn chọn và bổ sung; out null = thư mục mặc định. */
    record Options(Phase phase, String ten, WeightSets.Named weights, ScheduleFitFormula formula, Path out) {
    }

    private EvaluationMain() {
    }

    public static void main(String[] args) {
        Path backendDir = Path.of("").toAbsolutePath();
        // Thư mục mặc định và "git status -- ." đều tính từ backend/, chạy chỗ khác sẽ ghi sai chỗ, kiểm sai phạm vi.
        if (!Files.isRegularFile(backendDir.resolve("pom.xml"))) {
            System.out.println("Hãy chạy từ thư mục backend/ (D71); đang ở " + backendDir);
            System.exit(1);
        }
        int exit = run(args, backendDir, LocalDate.now(), GitInfo.read(backendDir), MachineInfo.current(),
                System.out::println);
        System.exit(exit);
    }

    /** Toàn bộ lượt chạy, trả mã thoát (0 xong, 1 lỗi). Nhận git, máy, ngày từ ngoài để test không phụ thuộc môi trường. */
    static int run(String[] args, Path backendDir, LocalDate today, GitInfo git, MachineInfo machine,
            Consumer<String> console) {
        Options options;
        try {
            options = parse(args);
        } catch (IllegalArgumentException e) {
            console.accept("Lỗi tham số: " + e.getMessage());
            console.accept(USAGE);
            return 1;
        }

        Optional<String> refusal = refusal(options.phase(), git);
        if (refusal.isPresent()) {
            console.accept("Dừng: " + refusal.get());
            return 1;
        }

        Path runDir = runDir(options, backendDir, today);
        RunInfo info = new RunInfo(options.ten(), COMMAND_PREFIX + String.join(" ", args), configFor(options), git, machine);
        try {
            ExperimentRunner.execute(runDir, info, console);
        } catch (RuntimeException e) {
            console.accept("Lỗi: " + e.getMessage());
            return 1;
        }
        console.accept("Đã ghi kết quả vào " + runDir);
        return 0;
    }

    /** Đọc tham số dạng "--tên giá-trị". Sai thì IllegalArgumentException kèm lý do. */
    static Options parse(String[] args) {
        // Map chỉ để tra theo tên, không duyệt.
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            String name = args[i];
            if (!KNOWN_OPTIONS.contains(name)) {
                throw new IllegalArgumentException("Tham số lạ: " + name);
            }
            // Giá trị bắt đầu bằng "--" là tham số kế tiếp: người dùng quên giá trị của name.
            if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                throw new IllegalArgumentException("Thiếu giá trị cho " + name);
            }
            if (values.put(name, args[i + 1]) != null) {
                throw new IllegalArgumentException("Tham số lặp: " + name);
            }
        }

        Phase phase = phaseOf(required(values, "--giai-doan"));
        String ten = required(values, "--ten");
        if (!ten.matches(NAME_PATTERN)) {
            throw new IllegalArgumentException("--ten chỉ gồm chữ không dấu, số, '-', '_': " + ten);
        }

        WeightSets.Named weights = null;
        ScheduleFitFormula formula = null;
        if (phase != Phase.BAO_CAO) {
            // Giai đoạn chọn so mọi bộ và mọi công thức; giai đoạn bổ sung dùng bộ mặc định (D97).
            // Chỉ định một cấu hình ở hai giai đoạn này là nhầm giai đoạn.
            if (values.containsKey("--trong-so") || values.containsKey("--schedule")) {
                throw new IllegalArgumentException("--trong-so và --schedule chỉ dùng ở giai đoạn bao-cao (D71)");
            }
        } else {
            // Bắt buộc ghi rõ cấu hình đã chọn ở D69, không có giá trị mặc định để không quên.
            weights = weightsOf(required(values, "--trong-so"));
            formula = ScheduleFitFormula.fromTen(required(values, "--schedule"));
        }
        Path out = values.containsKey("--out") ? Path.of(values.get("--out")) : null;
        return new Options(phase, ten, weights, formula, out);
    }

    /** Lý do không được chạy, nếu có. Giai đoạn báo cáo và bổ sung cần commit rõ ràng vì số đi vào báo cáo (D71, TC247, D97). */
    static Optional<String> refusal(Phase phase, GitInfo git) {
        if (phase == Phase.CHON) {
            return Optional.empty();
        }
        if (!git.known()) {
            return Optional.of("không đọc được commit bằng git; giai đoạn báo cáo phải truy được commit (D71)");
        }
        if (git.dirty()) {
            return Optional.of("backend/ có thay đổi chưa commit; commit hết rồi chạy lại (D71)");
        }
        return Optional.empty();
    }

    /** Ma trận đúng của D70 cho giai đoạn đã chọn. */
    static ExperimentConfig configFor(Options options) {
        return switch (options.phase()) {
            case CHON -> ExperimentConfig.chon();
            case BAO_CAO -> ExperimentConfig.baoCao(options.weights(), options.formula());
            case BO_SUNG -> ExperimentConfig.boSung();
        };
    }

    /** --out là chính thư mục lượt chạy; mặc định <backend>/../thuc_nghiem/ket_qua/<yyyy-MM-dd>_<tên> (D72). */
    static Path runDir(Options options, Path backendDir, LocalDate today) {
        if (options.out() != null) {
            return options.out();
        }
        return backendDir.resolve("..").resolve("thuc_nghiem").resolve("ket_qua")
                .resolve(today + "_" + options.ten()).normalize();
    }

    private static String required(Map<String, String> values, String name) {
        String value = values.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Thiếu " + name);
        }
        return value;
    }

    private static Phase phaseOf(String value) {
        for (Phase phase : Phase.values()) {
            if (phase.cliName().equals(value)) {
                return phase;
            }
        }
        throw new IllegalArgumentException("--giai-doan phải là chon, bao-cao hoặc bo-sung: " + value);
    }

    /** Tên trên dòng lệnh của 4 bộ độ nhạy (D71). */
    private static WeightSets.Named weightsOf(String value) {
        return switch (value) {
            case "mac_dinh" -> WeightSets.MAC_DINH;
            case "ky_nang" -> WeightSets.KY_NANG;
            case "lich" -> WeightSets.LICH;
            case "uy_tin" -> WeightSets.UY_TIN;
            default -> throw new IllegalArgumentException("--trong-so phải là mac_dinh, ky_nang, lich hoặc uy_tin: " + value);
        };
    }
}
