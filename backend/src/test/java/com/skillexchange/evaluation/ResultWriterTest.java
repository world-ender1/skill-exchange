package com.skillexchange.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.skillexchange.evaluation.ResultWriter.GitInfo;
import com.skillexchange.evaluation.ResultWriter.MachineInfo;
import com.skillexchange.evaluation.ResultWriter.MetricRow;
import com.skillexchange.evaluation.ResultWriter.RunInfo;
import com.skillexchange.evaluation.ResultWriter.TestRow;

/** Thư mục kết quả của một lượt chạy: TC244 và định dạng file của D72. */
class ResultWriterTest {

    private static final GitInfo CLEAN = new GitInfo("abc1234", true, false);
    private static final MachineInfo MACHINE = new MachineInfo("CPU thử", 16L * 1024 * 1024 * 1024, "21.0.5");
    private static final List<String> FILES = List.of("README.md", "metrics.csv", "per_user.csv", "kiem_dinh.csv", "log.txt");

    @TempDir
    Path temp;

    @Test
    @DisplayName("TC244 Thư mục kết quả đã tồn tại: dừng, báo lỗi, không tạo hay sửa file nào trong đó")
    void tc244_existingRunDirectoryIsRefused() throws IOException {
        Path runDir = temp.resolve("2026-11-13_thu");
        Files.createDirectory(runDir);
        Path old = runDir.resolve("metrics.csv");
        Files.writeString(old, "du lieu cu\n");

        assertThatThrownBy(() -> ResultWriter.create(runDir, info(CLEAN), line -> { }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đã tồn tại");

        try (Stream<Path> files = Files.list(runDir)) {
            assertThat(files).containsExactly(old);
        }
        assertThat(Files.readString(old)).isEqualTo("du lieu cu\n");
    }

    @Test
    @DisplayName("D72 Tạo thư mục cha còn thiếu và đủ 5 file; dòng đầu của ba file CSV đúng tên cột")
    void d72_createsAllFilesWithHeaders() throws IOException {
        Path runDir = temp.resolve("thuc_nghiem").resolve("ket_qua").resolve("2026-11-13_thu");

        try (ResultWriter writer = ResultWriter.create(runDir, info(CLEAN), line -> { })) {
            writer.finish();
        }

        for (String file : FILES) {
            assertThat(runDir.resolve(file)).as(file).isRegularFile();
        }
        assertThat(firstLine(runDir.resolve("metrics.csv")))
                .isEqualTo("seed,vai_tro_seed,n,muc_nhieu,phuong_an,chi_so,gia_tri,so_nguoi_tinh");
        assertThat(firstLine(runDir.resolve("per_user.csv"))).isEqualTo("seed,n,muc_nhieu,phuong_an,user_id,"
                + "so_ung_vien,precision_at_5,precision_at_5_nghiem_ngat,ndcg_at_5,so_muc_nhan_ly_tuong");
        assertThat(firstLine(runDir.resolve("kiem_dinh.csv"))).isEqualTo("n,muc_nhieu,seeds,phuong_an,so_voi,"
                + "chi_so,so_cap,hieu_trung_binh,ci_thap,ci_cao,n_wilcoxon,w_cong,z,p,ghi_chu");
    }

    @Test
    @DisplayName("D72 Mỗi dòng ghi đúng thứ tự cột; ô trống ở cuối dòng vẫn giữ dấu phẩy")
    void d72_rowsFollowColumnOrder() throws IOException {
        Path runDir = temp.resolve("run");

        try (ResultWriter writer = ResultWriter.create(runDir, info(CLEAN), line -> { })) {
            writer.metric(new MetricRow(3, "bao_cao", 50, "mac_dinh", "P", "precision_at_5", "0.4", 50));
            writer.perUser(3, 50, "mac_dinh", "P", new UserMetrics(7, 6, 0.4, 0.2, 0.5, 3, false, true));
            writer.test(new TestRow(500, "mac_dinh", "3;4;5", "P", "B1", "ndcg_at_5", 120,
                    "0.05", "0.01", "0.09", 100, "3000.0", "2.1", "0.03", ""));
            writer.finish();
        }

        assertThat(Files.readAllLines(runDir.resolve("metrics.csv")))
                .element(1).isEqualTo("3,bao_cao,50,mac_dinh,P,precision_at_5,0.4,50");
        assertThat(Files.readAllLines(runDir.resolve("per_user.csv")))
                .element(1).isEqualTo("3,50,mac_dinh,P,7,6,0.4,0.2,0.5,3");
        assertThat(Files.readAllLines(runDir.resolve("kiem_dinh.csv")))
                .element(1).isEqualTo("500,mac_dinh,3;4;5,P,B1,ndcg_at_5,120,0.05,0.01,0.09,100,3000.0,2.1,0.03,");
    }

    @Test
    @DisplayName("D72 Giá trị chứa dấu phẩy bị từ chối, vì CSV không dùng dấu ngoặc")
    void d72_valueWithCommaIsRejected() {
        try (ResultWriter writer = ResultWriter.create(temp.resolve("run"), info(CLEAN), line -> { })) {
            MetricRow bad = new MetricRow(3, "bao_cao", 50, "mac_dinh", "P,x", "precision_at_5", "0.4", 50);

            assertThatThrownBy(() -> writer.metric(bad)).isInstanceOf(IllegalArgumentException.class);
            writer.finish();
        }
    }

    @Test
    @DisplayName("D72 Số thực ghi bằng Double.toString, không làm tròn; NaN ghi NaN; số đếm ghi số nguyên")
    void d72_numberFormat() {
        assertThat(ResultWriter.number(0.1)).isEqualTo("0.1");
        assertThat(ResultWriter.number(0.6 - 0.4)).isEqualTo("0.19999999999999996");
        assertThat(ResultWriter.number(1e-7)).isEqualTo("1.0E-7");
        assertThat(ResultWriter.number(Double.NaN)).isEqualTo("NaN");
        assertThat(ResultWriter.count(12)).isEqualTo("12");
    }

    @Test
    @DisplayName("D72 README.md có tên, trạng thái, lệnh, giai đoạn, seed, commit, thay đổi chưa commit, CPU, RAM, Java, thời gian chạy")
    void d72_readmeHasRunInformation() throws IOException {
        Path runDir = temp.resolve("run");

        try (ResultWriter writer = ResultWriter.create(runDir, info(CLEAN), line -> { })) {
            writer.finish();
        }

        String readme = Files.readString(runDir.resolve("README.md"));
        assertThat(readme).contains("thu", "| Trạng thái | Xong |", "--giai-doan chon --ten thu",
                "| Giai đoạn | chon |", "| Seed | 1 |", "abc1234",
                "| Thay đổi chưa commit trong `backend/` | không |",
                "| CPU | CPU thử |", "| RAM | 16.0 GB |", "| Java | 21.0.5 |", "| Thời gian chạy |");
    }

    @Test
    @DisplayName("D71 README.md ghi 'có' khi backend/ có thay đổi chưa commit, 'không rõ' khi không đọc được git")
    void d71_readmeShowsDirtyAndUnknownGit() throws IOException {
        Path dirty = temp.resolve("dirty");
        Path unknown = temp.resolve("unknown");

        try (ResultWriter writer = ResultWriter.create(dirty, info(new GitInfo("abc1234", true, true)), line -> { })) {
            writer.finish();
        }
        try (ResultWriter writer = ResultWriter.create(unknown, info(GitInfo.UNKNOWN), line -> { })) {
            writer.finish();
        }

        assertThat(Files.readString(dirty.resolve("README.md")))
                .contains("| Thay đổi chưa commit trong `backend/` | có |");
        assertThat(Files.readString(unknown.resolve("README.md")))
                .contains("| Commit | không rõ |", "| Thay đổi chưa commit trong `backend/` | không rõ |");
    }

    @Test
    @DisplayName("D72 Lượt chạy hỏng vẫn giữ thư mục, README.md ghi trạng thái hỏng kèm lý do; đóng khi chưa xong cũng ghi là hỏng")
    void d72_failedRunKeepsDirectoryWithReason() throws IOException {
        Path failed = temp.resolve("failed");
        Path interrupted = temp.resolve("interrupted");

        try (ResultWriter writer = ResultWriter.create(failed, info(CLEAN), line -> { })) {
            writer.log("đã chạy được một ô");
            writer.fail("Lỗi thử");
        }
        try (ResultWriter writer = ResultWriter.create(interrupted, info(CLEAN), line -> { })) {
            writer.log("bắt đầu");
        }

        assertThat(Files.readString(failed.resolve("README.md"))).contains("| Trạng thái | Hỏng: Lỗi thử |");
        assertThat(Files.readString(failed.resolve("log.txt"))).contains("đã chạy được một ô");
        assertThat(Files.readString(interrupted.resolve("README.md"))).contains("| Trạng thái | Hỏng:");
    }

    @Test
    @DisplayName("D72 log.txt ghi từng dòng và đưa cả ra console")
    void d72_logGoesToFileAndConsole() throws IOException {
        Path runDir = temp.resolve("run");
        List<String> console = new ArrayList<>();

        try (ResultWriter writer = ResultWriter.create(runDir, info(CLEAN), console::add)) {
            writer.log("dòng thử");
            writer.finish();
        }

        assertThat(Files.readString(runDir.resolve("log.txt"))).contains("dòng thử");
        assertThat(console).anyMatch(line -> line.contains("dòng thử"));
    }

    private static RunInfo info(GitInfo git) {
        return new RunInfo("thu", "java -cp target/classes com.skillexchange.evaluation.EvaluationMain --giai-doan chon --ten thu",
                ExperimentConfig.chon(List.of(1L), 50), git, MACHINE);
    }

    private static String firstLine(Path file) throws IOException {
        return Files.readAllLines(file).get(0);
    }
}
