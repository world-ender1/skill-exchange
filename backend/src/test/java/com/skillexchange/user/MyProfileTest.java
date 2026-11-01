package com.skillexchange.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.skillexchange.IntegrationTestBase;

/**
 * Hồ sơ của tôi (bước 8): TC16–TC20, TC38 (phần PUT /api/me), TC45–TC48, TC59, TC66;
 * phần /api/me của TC62, TC73; D15, D16, D29 và "Chốt ở bước này" của bước 8.
 */
class MyProfileTest extends IntegrationTestBase {

    private static final String PASSWORD = "matkhau123";
    private static final String AN = "an@student.example.edu.vn";
    private static final String BINH = "binh@student.example.edu.vn";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---------- Hàm tiện ích ----------

    private User createUser(String email, String fullName, String status) {
        User user = userRepository.save(User.register(email, passwordEncoder.encode(PASSWORD), fullName));
        jdbcTemplate.update("UPDATE users SET status = ? WHERE id = ?", status, user.getId());
        return user;
    }

    private String tokenOf(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private ResultActions getMe(String token) throws Exception {
        return mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private ResultActions putMe(String token, String json) throws Exception {
        return mockMvc.perform(put("/api/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private Map<String, Object> row(String email) {
        return jdbcTemplate.queryForMap(
                "SELECT email, full_name, bio, learning_mode, phone, contact_info, role, status FROM users WHERE email = ?",
                email);
    }

    /** Hồ sơ đầy đủ, hợp lệ của An. */
    private static final String FULL_PROFILE = """
            {"fullName": "Nguyễn Văn An", "bio": "Thích Python", "learningMode": "ONLINE",
             "phone": "0901 234 567", "contactInfo": "zalo: an"}""";

    // ---------- Xem hồ sơ ----------

    @Test
    @DisplayName("TC16 GET /api/me: 200, hồ sơ của chính người gọi, có email, phone, contactInfo, role, status")
    void tc16_getMyProfile() throws Exception {
        User an = createUser(AN, "Nguyễn Văn An", "ACTIVE");
        jdbcTemplate.update("UPDATE users SET phone = '0901', contact_info = 'zalo: an' WHERE id = ?", an.getId());
        createUser(BINH, "Trần Bình", "ACTIVE");

        getMe(tokenOf(AN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(an.getId()))
                .andExpect(jsonPath("$.email").value(AN))
                .andExpect(jsonPath("$.fullName").value("Nguyễn Văn An"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.phone").value("0901"))
                .andExpect(jsonPath("$.contactInfo").value("zalo: an"))
                // Các trường chưa khai báo vẫn có mặt với giá trị null (D29 liệt kê đủ 9 trường).
                .andExpect(jsonPath("$.bio").value((Object) null))
                .andExpect(jsonPath("$.learningMode").value((Object) null));
    }

    @Test
    @DisplayName("TC73 response của GET /api/me và PUT /api/me không có password_hash, token_hash")
    void tc73_profileResponseHasNoSecrets() throws Exception {
        createUser(AN, "Nguyễn Văn An", "ACTIVE");
        String token = tokenOf(AN);

        String getBody = getMe(token).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String putBody = putMe(token, FULL_PROFILE).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        for (String body : new String[] {getBody, putBody}) {
            assertThat(body).doesNotContainIgnoringCase("password")
                    .doesNotContainIgnoringCase("hash")
                    .doesNotContain("$2a$")
                    .doesNotContain("$2b$");
        }
    }

    @Test
    @DisplayName("TC62 GET /api/me, PUT /api/me không có token: 401 application/problem+json")
    void tc62_meWithoutTokenIs401Problem() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(put("/api/me").contentType(MediaType.APPLICATION_JSON).content(FULL_PROFILE))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("D29 admin gọi GET /api/me: 200, role = ADMIN")
    void adminCanReadOwnProfile() throws Exception {
        User admin = createUser("admin@student.example.edu.vn", "Quản trị viên", "ACTIVE");
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", admin.getId());

        getMe(tokenOf("admin@student.example.edu.vn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    // ---------- Sửa hồ sơ: hợp lệ ----------

    @Test
    @DisplayName("TC17 PUT /api/me đủ trường: 200, body là hồ sơ vừa lưu; GET lại thấy đúng")
    void tc17_updateMyProfile() throws Exception {
        User an = createUser(AN, "An", "ACTIVE");
        String token = tokenOf(AN);

        putMe(token, FULL_PROFILE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(an.getId()))
                .andExpect(jsonPath("$.email").value(AN))
                .andExpect(jsonPath("$.fullName").value("Nguyễn Văn An"))
                .andExpect(jsonPath("$.bio").value("Thích Python"))
                .andExpect(jsonPath("$.learningMode").value("ONLINE"))
                .andExpect(jsonPath("$.phone").value("0901 234 567"))
                .andExpect(jsonPath("$.contactInfo").value("zalo: an"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        getMe(token)
                .andExpect(jsonPath("$.fullName").value("Nguyễn Văn An"))
                .andExpect(jsonPath("$.bio").value("Thích Python"))
                .andExpect(jsonPath("$.learningMode").value("ONLINE"))
                .andExpect(jsonPath("$.phone").value("0901 234 567"))
                .andExpect(jsonPath("$.contactInfo").value("zalo: an"));
    }

    @Test
    @DisplayName("TC18 learningMode = ONLINE, OFFLINE, ANY: cả 3 lần 200 và lưu đúng")
    void tc18_allLearningModes() throws Exception {
        createUser(AN, "An", "ACTIVE");
        String token = tokenOf(AN);

        for (String mode : new String[] {"ONLINE", "OFFLINE", "ANY"}) {
            putMe(token, "{\"fullName\": \"An\", \"learningMode\": \"" + mode + "\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.learningMode").value(mode));
            assertThat(row(AN).get("learning_mode")).isEqualTo(mode);
        }
    }

    @Test
    @DisplayName("TC19 learningMode null, phone rỗng, không gửi bio: cả ba lưu NULL")
    void tc19_missingNullOrEmptyBecomesNull() throws Exception {
        createUser(AN, "An", "ACTIVE");
        String token = tokenOf(AN);
        putMe(token, "{\"fullName\": \"An\", \"bio\": \"abc\", \"learningMode\": \"ONLINE\", \"phone\": \"0901\"}")
                .andExpect(status().isOk());

        putMe(token, "{\"fullName\": \"Nguyễn An\", \"learningMode\": null, \"phone\": \"\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value((Object) null))
                .andExpect(jsonPath("$.learningMode").value((Object) null))
                .andExpect(jsonPath("$.phone").value((Object) null));

        Map<String, Object> row = row(AN);
        assertThat(row.get("full_name")).isEqualTo("Nguyễn An");
        assertThat(row.get("learning_mode")).isNull();
        assertThat(row.get("phone")).isNull();
        assertThat(row.get("bio")).isNull();
    }

    @Test
    @DisplayName("D15 + D16 chuỗi chỉ có khoảng trắng lưu NULL; giá trị khác bỏ khoảng trắng hai đầu")
    void blankBecomesNullAndValuesAreStripped() throws Exception {
        createUser(AN, "An", "ACTIVE");

        putMe(tokenOf(AN), """
                {"fullName": "  Nguyễn An  ", "bio": "   ", "learningMode": " ", "phone": " 0901 ", "contactInfo": "  zalo: an  "}""")
                .andExpect(status().isOk());

        Map<String, Object> row = row(AN);
        assertThat(row.get("full_name")).isEqualTo("Nguyễn An");
        assertThat(row.get("bio")).isNull();
        assertThat(row.get("learning_mode")).isNull();
        assertThat(row.get("phone")).isEqualTo("0901");
        assertThat(row.get("contact_info")).isEqualTo("zalo: an");
    }

    @Test
    @DisplayName("TC20 tài khoản PENDING_VERIFICATION gọi PUT /api/me hợp lệ: 200, dữ liệu được lưu")
    void tc20_pendingAccountCanUpdate() throws Exception {
        createUser(AN, "An", "PENDING_VERIFICATION");

        putMe(tokenOf(AN), FULL_PROFILE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"));
        assertThat(row(AN).get("bio")).isEqualTo("Thích Python");
    }

    @Test
    @DisplayName("TC48 PUT /api/me có thêm email, role, status: 200, các trường đó trong DB không đổi")
    void tc48_unknownFieldsAreIgnored() throws Exception {
        createUser(AN, "An", "ACTIVE");

        putMe(tokenOf(AN), """
                {"fullName": "Nguyễn An", "email": "khac@student.example.edu.vn", "role": "ADMIN", "status": "LOCKED"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(AN));

        Map<String, Object> row = row(AN);
        assertThat(row.get("full_name")).isEqualTo("Nguyễn An");
        assertThat(row.get("email")).isEqualTo(AN);
        assertThat(row.get("role")).isEqualTo("STUDENT");
        assertThat(row.get("status")).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("TC66 A gọi GET, PUT /api/me (kể cả gửi kèm id của B): chỉ đọc, sửa hồ sơ của A; B không đổi")
    void tc66_onlyOwnProfile() throws Exception {
        User an = createUser(AN, "An", "ACTIVE");
        User binh = createUser(BINH, "Trần Bình", "ACTIVE");
        String tokenAn = tokenOf(AN);

        getMe(tokenAn).andExpect(jsonPath("$.id").value(an.getId()));
        putMe(tokenAn, "{\"id\": " + binh.getId() + ", \"fullName\": \"Bị sửa\", \"bio\": \"hack\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(an.getId()));

        assertThat(row(AN).get("full_name")).isEqualTo("Bị sửa");
        assertThat(row(BINH).get("full_name")).isEqualTo("Trần Bình");
        assertThat(row(BINH).get("bio")).isNull();
    }

    // ---------- Sửa hồ sơ: dữ liệu sai ----------

    private void expectFieldError(String token, String json, String field) throws Exception {
        putMe(token, json)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field", hasItem(field)));
    }

    @Test
    @DisplayName("TC38 PUT /api/me với fullName dài 101 ký tự: 400")
    void tc38_fullNameTooLong() throws Exception {
        createUser(AN, "An", "ACTIVE");
        expectFieldError(tokenOf(AN), "{\"fullName\": \"" + "a".repeat(101) + "\"}", "fullName");
    }

    @Test
    @DisplayName("TC45 fullName rỗng, rồi chỉ có khoảng trắng, rồi không gửi: 400, errors[].field = fullName")
    void tc45_fullNameRequired() throws Exception {
        createUser(AN, "An", "ACTIVE");
        String token = tokenOf(AN);

        expectFieldError(token, "{\"fullName\": \"\"}", "fullName");
        expectFieldError(token, "{\"fullName\": \"   \"}", "fullName");
        expectFieldError(token, "{\"bio\": \"abc\"}", "fullName");
        assertThat(row(AN).get("full_name")).isEqualTo("An");
    }

    @Test
    @DisplayName("TC46 learningMode = BOTH: 400, errors[].field = learningMode")
    void tc46_invalidLearningMode() throws Exception {
        createUser(AN, "An", "ACTIVE");
        expectFieldError(tokenOf(AN), "{\"fullName\": \"An\", \"learningMode\": \"BOTH\"}", "learningMode");
    }

    @Test
    @DisplayName("TC47 bio 1001 ký tự, contactInfo 501 ký tự, phone có chữ, phone 21 ký tự: mỗi trường hợp 400")
    void tc47_fieldLimits() throws Exception {
        createUser(AN, "An", "ACTIVE");
        String token = tokenOf(AN);

        expectFieldError(token, "{\"fullName\": \"An\", \"bio\": \"" + "a".repeat(1001) + "\"}", "bio");
        expectFieldError(token, "{\"fullName\": \"An\", \"contactInfo\": \"" + "a".repeat(501) + "\"}", "contactInfo");
        expectFieldError(token, "{\"fullName\": \"An\", \"phone\": \"abc123\"}", "phone");
        expectFieldError(token, "{\"fullName\": \"An\", \"phone\": \"" + "1".repeat(21) + "\"}", "phone");
    }

    @Test
    @DisplayName("D16 giới hạn đúng biên: bio 1000, contactInfo 500, phone 20 ký tự có + - và khoảng trắng: 200")
    void fieldLimitsAtBoundary() throws Exception {
        createUser(AN, "An", "ACTIVE");

        putMe(tokenOf(AN), "{\"fullName\": \"" + "a".repeat(100) + "\", \"bio\": \"" + "a".repeat(1000)
                + "\", \"contactInfo\": \"" + "a".repeat(500) + "\", \"phone\": \"+84-901 234 567 8900\"}")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC59 PUT /api/me với JSON hỏng: 400 Problem Details, không phải 500")
    void tc59_brokenJson() throws Exception {
        createUser(AN, "An", "ACTIVE");

        putMe(tokenOf(AN), "{\"fullName\":")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("D24 PUT /api/me với body là null: 400")
    void nullBodyIs400() throws Exception {
        createUser(AN, "An", "ACTIVE");

        putMe(tokenOf(AN), "null")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
