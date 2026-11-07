package com.skillexchange.user;

import static com.skillexchange.TestAccounts.slot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.skillexchange.IntegrationTestBase;
import com.skillexchange.TestAccounts;

/**
 * GET /api/users/{id} (kế hoạch tuần 4, bước 9): TC158–TC162, TC76; TK 4.3, D38, D46(c).
 * An là người xem, Bình là người được xem (cùng hồ sơ với MatchesApiTest).
 */
class PublicProfileTest extends IntegrationTestBase {

    private static final String AN = "an@student.example.edu.vn";
    private static final String BINH = "binh@student.example.edu.vn";
    private static final String NOT_FOUND_DETAIL = "Không tìm thấy người dùng";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private TestAccounts accounts;

    @BeforeEach
    void setUpAccounts() {
        accounts = new TestAccounts(mockMvc, jdbcTemplate, userRepository, passwordEncoder);
    }

    // ---------- Dữ liệu mẫu ----------

    private String an() throws Exception {
        accounts.activeStudent(AN, "An");
        return accounts.login(AN);
    }

    /**
     * Bình: ANY, có bio, phone, contactInfo; dạy Kotlin (2), Tiếng Anh (3); học Python (2), Java (không đặt);
     * lịch khai lộn thứ tự để kiểm D41. Trả id.
     */
    private long binh() throws Exception {
        long id = accounts.activeStudent(BINH, "Bình");
        String token = accounts.login(BINH);
        accounts.putProfile(token, "Bình", "ANY", "0901234567", "zalo-binh-rieng-tu");
        // putProfile không có bio; đặt thẳng bằng SQL.
        jdbcTemplate.update("UPDATE users SET bio = ? WHERE id = ?", "Thích Kotlin", id);
        accounts.putTeachSkills(token, accounts.teach("Tiếng Anh", 3), accounts.teach("Kotlin", 2));
        accounts.putLearnSkills(token, accounts.learn("Python", 2), accounts.learn("Java", null));
        accounts.putAvailability(token, slot(7, "MORNING"), slot(3, "EVENING"), slot(3, "MORNING"));
        return id;
    }

    private ResultActions view(String token, String path) throws Exception {
        return mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private String categoryOf(String skill) {
        return jdbcTemplate.queryForObject(
                "SELECT c.name FROM skills s JOIN skill_categories c ON c.id = s.category_id WHERE s.name = ?",
                String.class, skill);
    }

    // ---------- Ca kiểm thử ----------

    @Test
    @DisplayName("TC158 An xem hồ sơ Bình: đủ trường TK 4.3, danh sách theo thứ tự D41")
    void tc158_publicProfileHasAllFieldsInOrder() throws Exception {
        String token = an();
        long binhId = binh();

        view(token, "/api/users/" + binhId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(binhId))
                .andExpect(jsonPath("$.fullName").value("Bình"))
                .andExpect(jsonPath("$.bio").value("Thích Kotlin"))
                .andExpect(jsonPath("$.learningMode").value("ANY"))
                // Dạy: Kotlin trước Tiếng Anh (theo tên), dù khai Tiếng Anh trước.
                .andExpect(jsonPath("$.teachSkills", hasSize(2)))
                .andExpect(jsonPath("$.teachSkills[0].skillId").value(accounts.skillId("Kotlin")))
                .andExpect(jsonPath("$.teachSkills[0].skillName").value("Kotlin"))
                .andExpect(jsonPath("$.teachSkills[0].categoryName").value(categoryOf("Kotlin")))
                .andExpect(jsonPath("$.teachSkills[0].level").value(2))
                .andExpect(jsonPath("$.teachSkills[1].skillName").value("Tiếng Anh"))
                .andExpect(jsonPath("$.teachSkills[1].level").value(3))
                // Học: Java trước Python; Java không đặt mục tiêu → targetLevel có key, giá trị null.
                .andExpect(jsonPath("$.learnSkills", hasSize(2)))
                .andExpect(jsonPath("$.learnSkills[0].skillId").value(accounts.skillId("Java")))
                .andExpect(jsonPath("$.learnSkills[0].skillName").value("Java"))
                .andExpect(jsonPath("$.learnSkills[0].categoryName").value(categoryOf("Java")))
                .andExpect(jsonPath("$.learnSkills[0].targetLevel").isEmpty())
                .andExpect(jsonPath("$.learnSkills[1].skillName").value("Python"))
                .andExpect(jsonPath("$.learnSkills[1].targetLevel").value(2))
                // Lịch: theo ngày, rồi buổi trong ngày.
                .andExpect(jsonPath("$.availability", hasSize(3)))
                .andExpect(jsonPath("$.availability[0].dayOfWeek").value(3))
                .andExpect(jsonPath("$.availability[0].period").value("MORNING"))
                .andExpect(jsonPath("$.availability[1].dayOfWeek").value(3))
                .andExpect(jsonPath("$.availability[1].period").value("EVENING"))
                .andExpect(jsonPath("$.availability[2].dayOfWeek").value(7));

        String body = view(token, "/api/users/" + binhId).andReturn().getResponse().getContentAsString();
        Map<String, Object> learnRow = JsonPath.read(body, "$.learnSkills[0]");
        assertThat(learnRow).containsKey("targetLevel");
        // Hồ sơ công khai không có isActive, categoryId (khác /api/me/teach-skills).
        Map<String, Object> teachRow = JsonPath.read(body, "$.teachSkills[0]");
        assertThat(teachRow).containsOnlyKeys("skillId", "skillName", "categoryName", "level");
    }

    @Test
    @DisplayName("TC159 An xem hồ sơ của chính mình qua /users/{id}: 200")
    void tc159_ownProfileIsVisible() throws Exception {
        String token = an();
        view(token, "/api/users/" + accounts.idOf(AN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("An"));
    }

    @Test
    @DisplayName("TC160 id không tồn tại, PENDING_VERIFICATION, LOCKED, admin: cả 4 là 404 cùng detail")
    void tc160_hiddenAccountsAreAll404WithSameDetail() throws Exception {
        String token = an();
        long pending = accounts.create("cho@student.example.edu.vn", "Chờ", UserStatus.PENDING_VERIFICATION, Role.STUDENT);
        long locked = accounts.create("khoa@student.example.edu.vn", "Khóa", UserStatus.LOCKED, Role.STUDENT);
        long admin = accounts.create("qtv@student.example.edu.vn", "Quản trị", UserStatus.ACTIVE, Role.ADMIN);

        for (long id : new long[] {999_999L, pending, locked, admin}) {
            view(token, "/api/users/" + id)
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value(NOT_FOUND_DETAIL));
        }
    }

    @Test
    @DisplayName("TC161 /users/abc và /users/99999999999999999999 (vượt Long): 400 tiếng Việt, không 500")
    void tc161_nonNumericIdIs400() throws Exception {
        String token = an();
        view(token, "/api/users/abc")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Tham số id có giá trị không hợp lệ: abc"));
        view(token, "/api/users/99999999999999999999")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Tham số id có giá trị không hợp lệ: 99999999999999999999"));
    }

    @Test
    @DisplayName("TC162 ẩn Kotlin rồi xem hồ sơ Bình: Kotlin vẫn có trong teachSkills (D46c)")
    void tc162_hiddenSkillStillShown() throws Exception {
        String token = an();
        long binhId = binh();
        jdbcTemplate.update("UPDATE skills SET is_active = false WHERE name = 'Kotlin'");
        try {
            view(token, "/api/users/" + binhId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.teachSkills", hasSize(2)))
                    .andExpect(jsonPath("$.teachSkills[0].skillName").value("Kotlin"));
        } finally {
            // skills không bị TRUNCATE giữa các test: phải trả lại.
            jdbcTemplate.update("UPDATE skills SET is_active = true WHERE name = 'Kotlin'");
        }
    }

    @Test
    @DisplayName("TC76 hồ sơ công khai của người có phone, contactInfo: không có email, phone, contactInfo")
    void tc76_noPrivateFields() throws Exception {
        String token = an();
        long binhId = binh();

        String body = view(token, "/api/users/" + binhId)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> profile = JsonPath.read(body, "$");
        assertThat(profile).containsOnlyKeys(
                "id", "fullName", "bio", "learningMode", "teachSkills", "learnSkills", "availability");
        assertThat(body).doesNotContain("0901234567", "zalo-binh-rieng-tu", BINH);
    }
}
