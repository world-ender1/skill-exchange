package com.skillexchange.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.skillexchange.IntegrationTestBase;
import com.skillexchange.user.User;
import com.skillexchange.user.UserRepository;

/**
 * Đăng nhập nhận vé JWT (bước 6): TC08–TC11, TC44, TC65, TC68, TC71, TC90,
 * phần login của TC73, phần đăng nhập của TC77; D12, D13, D14.
 */
class LoginTest extends IntegrationTestBase {

    private static final String EMAIL = "an@student.example.edu.vn";
    private static final String PASSWORD = "matkhau123";
    private static final String WRONG_CREDENTIALS = "Email hoặc mật khẩu không đúng";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    // ---------- Hàm tiện ích ----------

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    /** Tạo user thẳng bằng Repository rồi đặt trạng thái bằng SQL (không qua API đăng ký). */
    private User createUser(String status) {
        User user = userRepository.save(User.register(EMAIL, passwordEncoder.encode(PASSWORD), "Nguyễn Văn An"));
        jdbcTemplate.update("UPDATE users SET status = ? WHERE id = ?", status, user.getId());
        return user;
    }

    private String accessTokenOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    // ---------- Đăng nhập thành công ----------

    @Test
    @DisplayName("TC08 đăng nhập đúng email, mật khẩu của tài khoản ACTIVE: 200 có accessToken")
    void tc08_loginActiveAccount() throws Exception {
        createUser("ACTIVE");

        login(EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    @DisplayName("TC09 vé có sub = id, role = STUDENT, exp - iat = 86400 giây, không có claim nào khác")
    void tc09_tokenClaims() throws Exception {
        User user = createUser("ACTIVE");

        Jwt jwt = jwtDecoder.decode(accessTokenOf(login(EMAIL, PASSWORD)));

        assertThat(jwt.getSubject()).isEqualTo(String.valueOf(user.getId()));
        assertThat(jwt.getClaimAsString("role")).isEqualTo("STUDENT");
        assertThat(jwt.getIssuedAt()).isEqualTo(START); // giờ lấy từ bean Clock (đồng hồ giả)
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofSeconds(86_400));
        // Không có email, mật khẩu, số điện thoại trong vé (ai cũng giải mã được phần thân vé).
        assertThat(jwt.getClaims().keySet()).containsExactlyInAnyOrder("sub", "role", "iat", "exp");
    }

    @Test
    @DisplayName("TC10 tài khoản PENDING_VERIFICATION đăng nhập đúng mật khẩu: 200 có accessToken")
    void tc10_loginPendingAccount() throws Exception {
        createUser("PENDING_VERIFICATION");

        login(EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    @DisplayName("TC11 đăng nhập với email viết hoa, có khoảng trắng: 200")
    void tc11_loginWithUppercaseEmail() throws Exception {
        createUser("ACTIVE");

        login("  AN@Student.Example.EDU.VN ", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    @DisplayName("D14 vé của admin có role = ADMIN và qua được cổng /api/admin/** (đường dẫn chưa có nên 404, không phải 403)")
    void adminTokenHasAdminRole() throws Exception {
        User user = createUser("ACTIVE");
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", user.getId());

        String token = accessTokenOf(login(EMAIL, PASSWORD));

        assertThat(jwtDecoder.decode(token).getClaimAsString("role")).isEqualTo("ADMIN");
        mockMvc.perform(get("/api/admin/skills").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("D14 vé của sinh viên gọi /api/admin/**: 403")
    void studentTokenCannotCallAdminApi() throws Exception {
        createUser("ACTIVE");
        String token = accessTokenOf(login(EMAIL, PASSWORD));

        mockMvc.perform(get("/api/admin/skills").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // ---------- Đăng nhập thất bại ----------

    @Test
    @DisplayName("TC44 đúng email, sai mật khẩu: 401, không có accessToken")
    void tc44_wrongPassword() throws Exception {
        createUser("ACTIVE");

        login(EMAIL, "saimatkhau")
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(WRONG_CREDENTIALS))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    @DisplayName("TC68 tài khoản LOCKED đăng nhập đúng mật khẩu: 403, không có accessToken")
    void tc68_lockedAccountWithCorrectPassword() throws Exception {
        createUser("LOCKED");

        login(EMAIL, PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Tài khoản đã bị khóa"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    @DisplayName("TC71 sai mật khẩu và email không tồn tại: cùng 401, cùng nội dung response")
    void tc71_wrongPasswordAndUnknownEmailLookTheSame() throws Exception {
        createUser("ACTIVE");

        String wrongPassword = login(EMAIL, "saimatkhau")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String unknownEmail = login("khongco@student.example.edu.vn", "saimatkhau")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(unknownEmail).isEqualTo(wrongPassword);
    }

    @Test
    @DisplayName("TC90 tài khoản LOCKED đăng nhập sai mật khẩu: 401, cùng detail với TC44")
    void tc90_lockedAccountWithWrongPassword() throws Exception {
        createUser("LOCKED");

        login(EMAIL, "saimatkhau")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(WRONG_CREDENTIALS));
    }

    @Test
    @DisplayName("D13 mật khẩu dài quá 72 byte: 401 như sai mật khẩu, không phải 500")
    void passwordOver72BytesIs401() throws Exception {
        createUser("ACTIVE");

        login(EMAIL, PASSWORD + "a".repeat(80))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(WRONG_CREDENTIALS));
    }

    @Test
    @DisplayName("D24 body đăng nhập thiếu email và mật khẩu: 400 kèm errors")
    void missingFieldsIs400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("email", "password")));
    }

    // ---------- Không lộ dữ liệu, đường dẫn công khai ----------

    @Test
    @DisplayName("TC73 response của login (thành công và thất bại) không có password_hash, token_hash")
    void tc73_loginResponseHasNoSecrets() throws Exception {
        createUser("ACTIVE");

        for (String password : new String[] {PASSWORD, "saimatkhau"}) {
            String body = login(EMAIL, password).andReturn().getResponse().getContentAsString();
            assertThat(body).doesNotContainIgnoringCase("password")
                    .doesNotContainIgnoringCase("hash")
                    .doesNotContain("$2a$")
                    .doesNotContain("$2b$");
        }
    }

    @Test
    @DisplayName("TC65 4 API /api/auth/* không có token: không trả 401, mỗi API trả mã theo nghiệp vụ")
    void tc65_authApisArePublic() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"binh@student.example.edu.vn\",\"password\":\"matkhau123\",\"fullName\":\"Bình\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"khong-ton-tai\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"binh@student.example.edu.vn\"}"))
                .andExpect(status().isNoContent());

        createUser("ACTIVE");
        login(EMAIL, PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("D14 /api/auth/** bỏ qua header Authorization: vé hỏng gửi kèm vẫn đăng nhập được")
    void authPathsIgnoreAuthorizationHeader() throws Exception {
        createUser("ACTIVE");

        mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ve-cu-da-hong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());
    }

    // ---------- Đăng ký lại rồi đăng nhập (phần dời từ bước 5) ----------

    @Test
    @DisplayName("TC77 đăng ký lại email chưa xác thực: mật khẩu cũ 401, mật khẩu mới 200")
    void tc77_loginAfterReRegistrationUsesNewPassword() throws Exception {
        String body = "{\"email\":\"" + EMAIL + "\",\"password\":\"%s\",\"fullName\":\"%s\"}";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted("matkhau111", "An")))
                .andExpect(status().isCreated());
        clock.advance(Duration.ofSeconds(61));
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted("matkhau222", "An Thật")))
                .andExpect(status().isCreated());

        login(EMAIL, "matkhau111").andExpect(status().isUnauthorized());
        login(EMAIL, "matkhau222").andExpect(status().isOk());
    }
}
