package com.skillexchange.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.skillexchange.IntegrationTestBase;
import com.skillexchange.user.User;
import com.skillexchange.user.UserRepository;

/**
 * Đăng ký tài khoản (bước 4): TC01, TC02, TC32–TC38 (phần đăng ký), TC60, TC85, TC86;
 * D8 phần 409 và ghi đè. Phần token, thư, 60 giây làm ở bước 5.
 * Đuôi email trường trong cấu hình test: student.example.edu.vn.
 */
class RegistrationTest extends IntegrationTestBase {

    private static final String DOMAIN = "@student.example.edu.vn";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---------- Hàm tiện ích ----------

    /** Tạo JSON từ các cặp khóa–giá trị; giá trị null thì bỏ hẳn khóa (giả lập "không gửi"). */
    private static String json(String email, String password, String fullName) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("email", email);
        fields.put("password", password);
        fields.put("fullName", fullName);

        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> field : fields.entrySet()) {
            if (field.getValue() != null) {
                parts.add("\"" + field.getKey() + "\":\"" + field.getValue() + "\"");
            }
        }
        return "{" + String.join(",", parts) + "}";
    }

    private ResultActions register(String email, String password, String fullName) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(email, password, fullName)));
    }

    private ResultActions expectFieldError(ResultActions result, String field) throws Exception {
        return result.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field", everyItem(is(field))));
    }

    private String passwordHashOf(String email) {
        return jdbcTemplate.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, email);
    }

    private long userCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM users", Long.class);
    }

    /** Tạo sẵn tài khoản đã xác thực bằng Repository (TC34, TC35), không qua API. */
    private void createActiveUser(String email, String rawPassword) {
        User user = User.register(email, passwordEncoder.encode(rawPassword), "An Cũ");
        user.verifyEmail(Instant.parse("2026-10-01T00:00:00Z"));
        userRepository.save(user);
    }

    // ---------- Đăng ký thành công ----------

    @Test
    @DisplayName("TC01 đăng ký hợp lệ: 201 {id, email, status}; DB có 1 user STUDENT, PENDING_VERIFICATION")
    void tc01_registerSuccess() throws Exception {
        register("an" + DOMAIN, "matkhau123", "Nguyễn An")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("an" + DOMAIN))
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.length()").value(3));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT role, status, email_verified_at, full_name FROM users WHERE email = ?", "an" + DOMAIN);
        assertThat(userCount()).isEqualTo(1);
        assertThat(row.get("role")).isEqualTo("STUDENT");
        assertThat(row.get("status")).isEqualTo("PENDING_VERIFICATION");
        assertThat(row.get("email_verified_at")).isNull();
        assertThat(row.get("full_name")).isEqualTo("Nguyễn An");
    }

    @Test
    @DisplayName("TC02 email có khoảng trắng và chữ hoa được chuẩn hóa trước khi lưu")
    void tc02_emailIsNormalized() throws Exception {
        register("  An@Student.Example.EDU.vn ", "matkhau123", "Nguyễn An")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("an" + DOMAIN));

        assertThat(jdbcTemplate.queryForList("SELECT email FROM users", String.class))
                .containsExactly("an" + DOMAIN);
    }

    @Test
    @DisplayName("TC60 password_hash là BCrypt, khác mật khẩu gốc, so khớp được với mật khẩu gốc")
    void tc60_passwordIsHashedWithBcrypt() throws Exception {
        register("an" + DOMAIN, "matkhau123", "Nguyễn An").andExpect(status().isCreated());

        String hash = passwordHashOf("an" + DOMAIN);
        assertThat(hash).isNotEqualTo("matkhau123").matches("^\\$2[ab]\\$.*");
        assertThat(passwordEncoder.matches("matkhau123", hash)).isTrue();
    }

    // ---------- Đuôi email trường ----------

    @Test
    @DisplayName("TC32 email @gmail.com: 400, errors[].field = email, không tạo user")
    void tc32_gmailRejected() throws Exception {
        expectFieldError(register("an@gmail.com", "matkhau123", "Nguyễn An"), "email");
        assertThat(userCount()).isZero();
    }

    @Test
    @DisplayName("TC33 tên miền con chưa có trong cấu hình: 400")
    void tc33_subdomainRejected() throws Exception {
        expectFieldError(register("an@abc.student.example.edu.vn", "matkhau123", "Nguyễn An"), "email");
        assertThat(userCount()).isZero();
    }

    // ---------- Email đã có trong hệ thống (D8) ----------

    @Test
    @DisplayName("TC34 đăng ký lại email của tài khoản ACTIVE: 409, vẫn 1 user, mật khẩu cũ không đổi")
    void tc34_activeEmailConflict() throws Exception {
        createActiveUser("an" + DOMAIN, "matkhaucu1");
        String oldHash = passwordHashOf("an" + DOMAIN);

        register("an" + DOMAIN, "matkhaumoi1", "Kẻ Giả")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        assertThat(userCount()).isEqualTo(1);
        assertThat(passwordHashOf("an" + DOMAIN)).isEqualTo(oldHash);
    }

    @Test
    @DisplayName("TC35 email của TC34 viết hoa khác: vẫn 409")
    void tc35_activeEmailConflictIgnoresCase() throws Exception {
        createActiveUser("an" + DOMAIN, "matkhaucu1");

        register("An@Student.Example.edu.vn", "matkhaumoi1", "Kẻ Giả")
                .andExpect(status().isConflict());
        assertThat(userCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("D8 email của tài khoản LOCKED: 409")
    void d8_lockedEmailConflict() throws Exception {
        createActiveUser("an" + DOMAIN, "matkhaucu1");
        jdbcTemplate.update("UPDATE users SET status = 'LOCKED' WHERE email = ?", "an" + DOMAIN);

        register("an" + DOMAIN, "matkhaumoi1", "Kẻ Giả").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("D8 đăng ký lại email PENDING_VERIFICATION: 201 cùng id, ghi đè mật khẩu và họ tên")
    void d8_pendingEmailIsOverwritten() throws Exception {
        String firstBody = register("an" + DOMAIN, "matkhau111", "Kẻ Giả")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String secondBody = register("an" + DOMAIN, "matkhau222", "An Thật")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                .andReturn().getResponse().getContentAsString();

        Number firstId = JsonPath.read(firstBody, "$.id");
        Number secondId = JsonPath.read(secondBody, "$.id");
        assertThat(secondId.longValue()).isEqualTo(firstId.longValue());
        assertThat(userCount()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT full_name FROM users", String.class)).isEqualTo("An Thật");
        String hash = passwordHashOf("an" + DOMAIN);
        assertThat(passwordEncoder.matches("matkhau222", hash)).isTrue();
        assertThat(passwordEncoder.matches("matkhau111", hash)).isFalse();
    }

    // ---------- Dữ liệu sai ----------

    @Test
    @DisplayName("TC36 thiếu fullName và email sai định dạng: 400, errors có cả fullName và email")
    void tc36_missingNameAndBadEmail() throws Exception {
        register("an", "matkhau123", null)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("fullName", "email")));
        assertThat(userCount()).isZero();
    }

    @Test
    @DisplayName("TC37 mật khẩu 7 và 73 ký tự: 400 (field password); 8 và 72 ký tự ASCII được nhận")
    void tc37_passwordLength() throws Exception {
        expectFieldError(register("a7" + DOMAIN, "a".repeat(7), "An"), "password");
        expectFieldError(register("a73" + DOMAIN, "a".repeat(73), "An"), "password");

        register("a8" + DOMAIN, "a".repeat(8), "An").andExpect(status().isCreated());
        register("a72" + DOMAIN, "a".repeat(72), "An").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC85 25 chữ 'ệ' (75 byte UTF-8): 400 field password, không 500; 24 chữ 'ệ' (72 byte) được nhận")
    void tc85_passwordByteLimit() throws Exception {
        expectFieldError(register("e25" + DOMAIN, "ệ".repeat(25), "An"), "password");
        assertThat(userCount()).isZero();

        register("e24" + DOMAIN, "ệ".repeat(24), "An").andExpect(status().isCreated());
        assertThat(passwordEncoder.matches("ệ".repeat(24), passwordHashOf("e24" + DOMAIN))).isTrue();
    }

    @Test
    @DisplayName("TC38 (phần đăng ký) fullName 101 ký tự: 400; 100 ký tự được nhận")
    void tc38_fullNameLength() throws Exception {
        expectFieldError(register("n101" + DOMAIN, "matkhau123", "a".repeat(101)), "fullName");

        register("n100" + DOMAIN, "matkhau123", "a".repeat(100)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("D16 fullName chỉ có khoảng trắng: 400; có khoảng trắng hai đầu thì lưu bản đã cắt")
    void d16_fullNameIsStripped() throws Exception {
        expectFieldError(register("blank" + DOMAIN, "matkhau123", "   "), "fullName");

        register("strip" + DOMAIN, "matkhau123", "  Nguyễn An  ").andExpect(status().isCreated());
        assertThat(jdbcTemplate.queryForObject("SELECT full_name FROM users", String.class)).isEqualTo("Nguyễn An");
    }

    @Test
    @DisplayName("TC86 email dài 256 ký tự (đúng đuôi trường): 400")
    void tc86_emailTooLong() throws Exception {
        String longEmail = "a".repeat(256 - DOMAIN.length()) + DOMAIN;
        assertThat(longEmail).hasSize(256);

        expectFieldError(register(longEmail, "matkhau123", "An"), "email");
        assertThat(userCount()).isZero();
    }
}
