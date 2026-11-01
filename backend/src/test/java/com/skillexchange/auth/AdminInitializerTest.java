package com.skillexchange.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.jayway.jsonpath.JsonPath;
import com.skillexchange.BackendApplication;
import com.skillexchange.IntegrationTestBase;
import com.skillexchange.user.User;
import com.skillexchange.user.UserRepository;

/**
 * Admin đầu tiên tạo lúc khởi động (bước 7): TC81; D4 và phần "Chốt ở bước này" của bước 7.
 * Cấu hình admin của test nằm ở application-test.yml (email viết hoa, có khoảng trắng).
 */
class AdminInitializerTest extends IntegrationTestBase {

    private static final String ADMIN_EMAIL = "admin@student.example.edu.vn";
    private static final String ADMIN_PASSWORD = "matkhau-admin-test";

    @Autowired
    private ApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    /**
     * Giả lập "khởi động": chạy mọi ApplicationRunner như Spring làm sau khi tạo xong context.
     * (IntegrationTestBase đã TRUNCATE users, nên admin tạo lúc context khởi động đã bị xóa.)
     */
    private void runStartupRunners() throws Exception {
        for (ApplicationRunner runner : context.getBeansOfType(ApplicationRunner.class).values()) {
            runner.run(new DefaultApplicationArguments());
        }
    }

    private Map<String, Object> adminRow() {
        return jdbcTemplate.queryForMap(
                "SELECT role, status, email_verified_at, password_hash FROM users WHERE email = ?", ADMIN_EMAIL);
    }

    @Test
    @DisplayName("TC81 khởi động hai lần với ADMIN_EMAIL viết hoa, có khoảng trắng: đúng 1 admin ACTIVE, đã xác thực")
    void tc81_createsAdminOnceWithNormalizedEmail() throws Exception {
        runStartupRunners();
        runStartupRunners();

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
        Map<String, Object> admin = adminRow();
        assertThat(admin.get("role")).isEqualTo("ADMIN");
        assertThat(admin.get("status")).isEqualTo("ACTIVE");
        assertThat(admin.get("email_verified_at")).isNotNull();
    }

    @Test
    @DisplayName("D4 admin đăng nhập được bằng ADMIN_PASSWORD, vé có role = ADMIN; mật khẩu lưu dạng băm")
    void adminCanLogIn() throws Exception {
        runStartupRunners();

        assertThat((String) adminRow().get("password_hash")).startsWith("$2").isNotEqualTo(ADMIN_PASSWORD);

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.accessToken");
        assertThat(jwtDecoder.decode(token).getClaimAsString("role")).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("D4 email admin đã thuộc một tài khoản sinh viên: không tạo thêm, không tự nâng quyền")
    void existingStudentWithAdminEmailIsNotPromoted() throws Exception {
        userRepository.save(User.register(ADMIN_EMAIL, passwordEncoder.encode("matkhau123"), "Người đăng ký trước"));

        runStartupRunners();

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
        assertThat(adminRow().get("role")).isEqualTo("STUDENT");
        assertThat(adminRow().get("status")).isEqualTo("PENDING_VERIFICATION");
    }

    // ---------- Thiếu hoặc sai cấu hình: ứng dụng dừng lúc khởi động ----------

    /**
     * Khởi động cả ứng dụng với cấu hình admin bị ghi đè. server.port=0: cổng ngẫu nhiên,
     * không đụng backend đang chạy ở 8080. Tham số sau (properties) đè giá trị của profile test.
     */
    private static void startAppWith(String property) {
        try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(BackendApplication.class)
                .profiles("test")
                .run("--server.port=0", "--" + property)) {
            // Khởi động được là sai: test bên dưới mong một exception.
        }
    }

    /**
     * Lỗi gốc khiến ứng dụng không khởi động được. Lấy "cause sâu nhất" (hoặc chính nó nếu không có),
     * để test không phụ thuộc việc Spring có bọc lỗi của runner trong một exception khác hay không.
     */
    private static Throwable startupError(String property) {
        Throwable thrown = catchThrowable(() -> startAppWith(property));
        assertThat(thrown).as("ứng dụng phải dừng lúc khởi động").isNotNull();
        return NestedExceptionUtils.getMostSpecificCause(thrown);
    }

    @Test
    @DisplayName("D4 thiếu ADMIN_EMAIL: ứng dụng dừng lúc khởi động, lỗi nêu tên biến")
    void missingAdminEmailStopsStartup() {
        assertThat(startupError("app.admin.email=")).hasMessageContaining("ADMIN_EMAIL");
    }

    @Test
    @DisplayName("D4 thiếu ADMIN_PASSWORD: ứng dụng dừng lúc khởi động, lỗi nêu tên biến")
    void missingAdminPasswordStopsStartup() {
        assertThat(startupError("app.admin.password=")).hasMessageContaining("ADMIN_PASSWORD");
    }

    @Test
    @DisplayName("D4 + D10 mật khẩu admin ngắn hơn 8 ký tự: dừng khởi động, lỗi không in mật khẩu")
    void shortAdminPasswordStopsStartupWithoutLeakingIt() {
        assertThat(startupError("app.admin.password=bi-mat"))
                .hasMessageContaining("ADMIN_PASSWORD")
                .hasMessageNotContaining("bi-mat");
    }

    @Test
    @DisplayName("D4 ADMIN_EMAIL sai định dạng: dừng khởi động, lỗi nêu tên biến")
    void invalidAdminEmailStopsStartup() {
        assertThat(startupError("app.admin.email=khong-phai-email")).hasMessageContaining("ADMIN_EMAIL");
    }
}
