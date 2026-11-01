package com.skillexchange.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.skillexchange.IntegrationTestBase;
import com.skillexchange.user.User;
import com.skillexchange.user.UserRepository;

/**
 * Kiểm vé ở mỗi request (bước 6): TC63, TC64, TC69, TC70; D12, D13.
 * Các ca này ghi GET /api/me, nhưng /api/me bước 8 mới có: kiểm trên GET /api/skills,
 * cũng là API cần đăng nhập, bộ lọc vé chạy như nhau (xem "Chốt ở bước này" của bước 6).
 */
class TokenValidationTest extends IntegrationTestBase {

    private static final String EMAIL = "an@student.example.edu.vn";
    private static final String PASSWORD = "matkhau123";
    private static final String PROTECTED_PATH = "/api/skills";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User createActiveUser() {
        User user = userRepository.save(User.register(EMAIL, passwordEncoder.encode(PASSWORD), "Nguyễn Văn An"));
        setStatus(user, "ACTIVE");
        return user;
    }

    private void setStatus(User user, String status) {
        jdbcTemplate.update("UPDATE users SET status = ? WHERE id = ?", status, user.getId());
    }

    private String loginToken() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private ResultActions callWith(String token) throws Exception {
        return mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private static void expectProblem(ResultActions result, int status) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status));
    }

    @Test
    @DisplayName("D12 vé hợp lệ gọi API quyền User: 200 (kể cả tài khoản PENDING_VERIFICATION)")
    void validTokenIsAccepted() throws Exception {
        User user = createActiveUser();
        callWith(loginToken()).andExpect(status().isOk());

        setStatus(user, "PENDING_VERIFICATION");
        callWith(loginToken()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC63 vé sai chữ ký (tự sửa role thành ADMIN, hoặc ký bằng khóa khác): 401")
    void tc63_tokenWithWrongSignature() throws Exception {
        User user = createActiveUser();
        String token = loginToken();

        String roleChangedToAdmin = JwtTestTokens.tamperPayload(token, "\"role\":\"STUDENT\"", "\"role\":\"ADMIN\"");
        expectProblem(callWith(roleChangedToAdmin), 401);
        expectProblem(mockMvc.perform(get("/api/admin/skills")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + roleChangedToAdmin)), 401);

        String otherKey = JwtTestTokens.signedWithOtherKey(user.getId(), "STUDENT", START);
        expectProblem(callWith(otherKey), 401);
    }

    @Test
    @DisplayName("TC64 vé đã quá 24 giờ: 401; còn trong hạn: 200")
    void tc64_expiredToken() throws Exception {
        createActiveUser();
        String token = loginToken();

        clock.advance(Duration.ofHours(24).minusSeconds(1));
        callWith(token).andExpect(status().isOk());

        clock.advance(Duration.ofSeconds(2)); // giờ hiện tại = iat + 24 giờ + 1 giây
        expectProblem(callWith(token), 401);
    }

    @Test
    @DisplayName("TC69 có vé rồi bị khóa: 403 ngay, application/problem+json; mở khóa lại thì 200")
    void tc69_lockedAfterLogin() throws Exception {
        User user = createActiveUser();
        String token = loginToken();

        setStatus(user, "LOCKED");
        expectProblem(callWith(token), 403);

        setStatus(user, "ACTIVE");
        callWith(token).andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC70 vé hợp lệ của user đã bị xóa khỏi DB: 401")
    void tc70_deletedUser() throws Exception {
        User user = createActiveUser();
        String token = loginToken();

        jdbcTemplate.update("DELETE FROM users WHERE id = ?", user.getId());

        expectProblem(callWith(token), 401);
    }
}
