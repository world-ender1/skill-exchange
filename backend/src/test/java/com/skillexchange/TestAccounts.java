package com.skillexchange;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.skillexchange.user.Role;
import com.skillexchange.user.User;
import com.skillexchange.user.UserRepository;
import com.skillexchange.user.UserStatus;

/**
 * Tạo tài khoản và khai hồ sơ cho test tích hợp tuần 4 (kế hoạch tuần 4, bước 8).
 * Tài khoản tạo thẳng bằng Repository rồi đổi trạng thái/vai trò bằng SQL (bỏ qua bước xác thực email,
 * đã có test riêng ở tuần 3); hồ sơ, kỹ năng, lịch khai qua API tuần 3 như người dùng thật.
 * Lớp thường, không phải bean: mỗi test tự tạo trong @BeforeEach.
 */
public class TestAccounts {

    public static final String PASSWORD = "matkhau123";

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TestAccounts(MockMvc mockMvc, JdbcTemplate jdbcTemplate,
                        UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------- Tài khoản ----------

    /** Tạo tài khoản với trạng thái và vai trò cho trước; trả id. */
    public long create(String email, String fullName, UserStatus status, Role role) {
        User user = userRepository.save(User.register(email, passwordEncoder.encode(PASSWORD), fullName));
        jdbcTemplate.update("UPDATE users SET status = ?, role = ? WHERE id = ?",
                status.name(), role.name(), user.getId());
        return user.getId();
    }

    public long activeStudent(String email, String fullName) {
        return create(email, fullName, UserStatus.ACTIVE, Role.STUDENT);
    }

    /** Đăng nhập thật qua API, trả vé. */
    public String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    public long idOf(String email) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    // ---------- Hồ sơ qua API tuần 3 ----------

    /** PUT /api/me. learningMode, phone, contactInfo được phép null (= chưa khai báo). */
    public void putProfile(String token, String fullName, String learningMode, String phone, String contactInfo)
            throws Exception {
        String json = "{\"fullName\": " + quoted(fullName)
                + ", \"learningMode\": " + quoted(learningMode)
                + ", \"phone\": " + quoted(phone)
                + ", \"contactInfo\": " + quoted(contactInfo) + "}";
        putOk(token, "/api/me", json);
    }

    public void putTeachSkills(String token, String... items) throws Exception {
        putOk(token, "/api/me/teach-skills", array(items));
    }

    public void putLearnSkills(String token, String... items) throws Exception {
        putOk(token, "/api/me/learn-skills", array(items));
    }

    public void putAvailability(String token, String... slots) throws Exception {
        putOk(token, "/api/me/availability", array(slots));
    }

    // ---------- Phần tử JSON ----------

    public long skillId(String name) {
        return jdbcTemplate.queryForObject("SELECT id FROM skills WHERE name = ?", Long.class, name);
    }

    public String teach(String skill, int level) {
        return "{\"skillId\": " + skillId(skill) + ", \"level\": " + level + "}";
    }

    /** targetLevel null = không đặt mục tiêu. */
    public String learn(String skill, Integer targetLevel) {
        return "{\"skillId\": " + skillId(skill) + ", \"targetLevel\": " + targetLevel + "}";
    }

    /** dayOfWeek: 1 = Thứ Hai ... 7 = Chủ nhật. */
    public static String slot(int dayOfWeek, String period) {
        return "{\"dayOfWeek\": " + dayOfWeek + ", \"period\": \"" + period + "\"}";
    }

    private void putOk(String token, String path, String json) throws Exception {
        mockMvc.perform(put(path)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    private static String array(String... items) {
        return Arrays.stream(items).collect(Collectors.joining(", ", "[", "]"));
    }

    private static String quoted(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
