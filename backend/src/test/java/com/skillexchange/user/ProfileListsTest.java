package com.skillexchange.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.Collectors;

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
 * Giờ rảnh, kỹ năng dạy và muốn học (bước 9): TC21–TC30, TC49–TC58, TC67, TC79, TC82–TC84, TC93;
 * phần PUT /api/me/availability của TC62; D19, D20, D21, D28, D41 và "Chốt ở bước này" của bước 9.
 * Ca "admin ẩn kỹ năng" đổi thẳng skills.is_active rồi trả lại trong finally (chưa có API admin).
 */
class ProfileListsTest extends IntegrationTestBase {

    private static final String PASSWORD = "matkhau123";
    private static final String AN = "an@student.example.edu.vn";
    private static final String BINH = "binh@student.example.edu.vn";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---------- Hàm tiện ích ----------

    private User createUser(String email) {
        User user = userRepository.save(User.register(email, passwordEncoder.encode(PASSWORD), "Người dùng"));
        jdbcTemplate.update("UPDATE users SET status = 'ACTIVE' WHERE id = ?", user.getId());
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

    /** Tạo An (ACTIVE) và trả vé của An. */
    private String anToken() throws Exception {
        createUser(AN);
        return tokenOf(AN);
    }

    private ResultActions put(String token, String path, String json) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions get(String token, String path) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private long skillId(String name) {
        return jdbcTemplate.queryForObject("SELECT id FROM skills WHERE name = ?", Long.class, name);
    }

    /** n kỹ năng đầu tiên theo id, dạng [{"skillId":..,<field>:2}, ...]. */
    private String firstSkills(int n, String levelField) {
        List<Long> ids = jdbcTemplate.queryForList("SELECT id FROM skills ORDER BY id LIMIT ?", Long.class, n);
        return ids.stream()
                .map(id -> "{\"skillId\": " + id + ", \"" + levelField + "\": 2}")
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String teach(String skill, int level) {
        return "{\"skillId\": " + skillId(skill) + ", \"level\": " + level + "}";
    }

    private String learn(String skill) {
        return "{\"skillId\": " + skillId(skill) + "}";
    }

    private int count(String table, String email) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " t JOIN users u ON u.id = t.user_id WHERE u.email = ?",
                Integer.class, email);
    }

    private void setSkillActive(String name, boolean active) {
        jdbcTemplate.update("UPDATE skills SET is_active = ? WHERE name = ?", active, name);
    }

    private void expectFieldError(ResultActions result, String field) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field", hasItem(field)));
    }

    // ================= Giờ rảnh =================

    @Test
    @DisplayName("TC21 PUT /api/me/availability với 2 khung: 200; GET và DB có đúng 2 khung")
    void tc21_saveAvailability() throws Exception {
        String token = anToken();

        put(token, "/api/me/availability",
                "[{\"dayOfWeek\": 2, \"period\": \"EVENING\"}, {\"dayOfWeek\": 5, \"period\": \"EVENING\"}]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].dayOfWeek").value(2))
                .andExpect(jsonPath("$[0].period").value("EVENING"))
                .andExpect(jsonPath("$[1].dayOfWeek").value(5));

        get(token, "/api/me/availability")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        assertThat(count("user_availability", AN)).isEqualTo(2);
    }

    @Test
    @DisplayName("TC22 sau TC21 gửi 1 khung: chỉ còn 1 khung (thay, không cộng thêm)")
    void tc22_replaceAvailability() throws Exception {
        String token = anToken();
        put(token, "/api/me/availability",
                "[{\"dayOfWeek\": 2, \"period\": \"EVENING\"}, {\"dayOfWeek\": 5, \"period\": \"EVENING\"}]")
                .andExpect(status().isOk());

        put(token, "/api/me/availability", "[{\"dayOfWeek\": 1, \"period\": \"MORNING\"}]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].dayOfWeek").value(1))
                .andExpect(jsonPath("$[0].period").value("MORNING"));
        assertThat(count("user_availability", AN)).isEqualTo(1);
    }

    @Test
    @DisplayName("TC23 PUT /api/me/availability với []: 200, không còn khung nào")
    void tc23_emptyAvailability() throws Exception {
        String token = anToken();
        put(token, "/api/me/availability", "[{\"dayOfWeek\": 2, \"period\": \"EVENING\"}]").andExpect(status().isOk());

        put(token, "/api/me/availability", "[]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        assertThat(count("user_availability", AN)).isZero();
    }

    @Test
    @DisplayName("TC24 PUT /api/me/availability đủ 21 khung: 200, DB có 21 dòng")
    void tc24_all21Slots() throws Exception {
        String token = anToken();
        StringBuilder json = new StringBuilder("[");
        for (int day = 1; day <= 7; day++) {
            for (String period : new String[] {"MORNING", "AFTERNOON", "EVENING"}) {
                json.append(json.length() > 1 ? ", " : "")
                        .append("{\"dayOfWeek\": ").append(day).append(", \"period\": \"").append(period).append("\"}");
            }
        }
        json.append("]");

        put(token, "/api/me/availability", json.toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(21)));
        assertThat(count("user_availability", AN)).isEqualTo(21);
    }

    @Test
    @DisplayName("D21 PUT /api/me/availability với 22 khung (có trùng): 400")
    void moreThan21SlotsIs400() throws Exception {
        String token = anToken();
        String slot = "{\"dayOfWeek\": 1, \"period\": \"MORNING\"}";
        String json = "[" + String.join(", ", java.util.Collections.nCopies(22, slot)) + "]";

        expectFieldError(put(token, "/api/me/availability", json), "availability");
    }

    @Test
    @DisplayName("TC49 dayOfWeek = 0, rồi 8: 400, errors[].field = [0].dayOfWeek; lịch cũ còn nguyên")
    void tc49_dayOfWeekOutOfRange() throws Exception {
        String token = anToken();
        put(token, "/api/me/availability", "[{\"dayOfWeek\": 3, \"period\": \"MORNING\"}]").andExpect(status().isOk());

        expectFieldError(put(token, "/api/me/availability", "[{\"dayOfWeek\": 0, \"period\": \"MORNING\"}]"),
                "[0].dayOfWeek");
        expectFieldError(put(token, "/api/me/availability", "[{\"dayOfWeek\": 8, \"period\": \"MORNING\"}]"),
                "[0].dayOfWeek");

        get(token, "/api/me/availability")
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].dayOfWeek").value(3));
    }

    @Test
    @DisplayName("TC50 period = NIGHT: 400, errors[].field = [0].period")
    void tc50_invalidPeriod() throws Exception {
        String token = anToken();
        expectFieldError(put(token, "/api/me/availability", "[{\"dayOfWeek\": 1, \"period\": \"NIGHT\"}]"),
                "[0].period");
    }

    @Test
    @DisplayName("TC51 hai lần cùng {2, EVENING}: 400")
    void tc51_duplicateSlot() throws Exception {
        String token = anToken();
        put(token, "/api/me/availability",
                "[{\"dayOfWeek\": 2, \"period\": \"EVENING\"}, {\"dayOfWeek\": 2, \"period\": \"EVENING\"}]")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("[1]"));
        assertThat(count("user_availability", AN)).isZero();
    }

    @Test
    @DisplayName("D24 mảng có phần tử null, hoặc không gửi body: 400")
    void nullElementOrMissingBodyIs400() throws Exception {
        String token = anToken();

        put(token, "/api/me/availability", "[null]").andExpect(status().isBadRequest());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/me/availability")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC62 PUT /api/me/availability không có token: 401 application/problem+json")
    void tc62_availabilityWithoutTokenIs401Problem() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/me/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // ================= Kỹ năng dạy và học: lưu =================

    @Test
    @DisplayName("TC25 PUT /api/me/teach-skills Java level 1, Python level 4: 200, lưu đúng hai trình độ biên")
    void tc25_saveTeachSkills() throws Exception {
        String token = anToken();

        put(token, "/api/me/teach-skills", "[" + teach("Java", 1) + ", " + teach("Python", 4) + "]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT level FROM user_teach_skills WHERE skill_id = ?", Integer.class, skillId("Java"))).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT level FROM user_teach_skills WHERE skill_id = ?", Integer.class, skillId("Python"))).isEqualTo(4);
    }

    @Test
    @DisplayName("TC26 PUT /api/me/learn-skills Tiếng Anh không có targetLevel: 200, target_level NULL")
    void tc26_learnSkillWithoutTarget() throws Exception {
        String token = anToken();

        put(token, "/api/me/learn-skills", "[" + learn("Tiếng Anh") + "]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].targetLevel").value((Object) null));
        assertThat(jdbcTemplate.queryForObject("SELECT target_level FROM user_learn_skills", Integer.class)).isNull();
    }

    @Test
    @DisplayName("TC27 PUT /api/me/learn-skills với targetLevel 1 và 4: 200, lưu đúng")
    void tc27_learnSkillTargetBounds() throws Exception {
        String token = anToken();

        put(token, "/api/me/learn-skills", "[{\"skillId\": " + skillId("Guitar") + ", \"targetLevel\": 1}, "
                + "{\"skillId\": " + skillId("Piano") + ", \"targetLevel\": 4}]")
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT target_level FROM user_learn_skills WHERE skill_id = ?", Integer.class, skillId("Guitar"))).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT target_level FROM user_learn_skills WHERE skill_id = ?", Integer.class, skillId("Piano"))).isEqualTo(4);
    }

    @Test
    @DisplayName("TC28 đang dạy Java, PUT chỉ có Python: chỉ còn Python")
    void tc28_replaceTeachSkills() throws Exception {
        String token = anToken();
        put(token, "/api/me/teach-skills", "[" + teach("Java", 2) + "]").andExpect(status().isOk());

        put(token, "/api/me/teach-skills", "[" + teach("Python", 3) + "]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].skillName").value("Python"))
                .andExpect(jsonPath("$[0].level").value(3));
        assertThat(count("user_teach_skills", AN)).isEqualTo(1);
    }

    @Test
    @DisplayName("TC29 PUT teach-skills và learn-skills với []: cả hai danh sách rỗng")
    void tc29_emptySkillLists() throws Exception {
        String token = anToken();
        put(token, "/api/me/teach-skills", "[" + teach("Java", 2) + "]").andExpect(status().isOk());
        put(token, "/api/me/learn-skills", "[" + learn("Python") + "]").andExpect(status().isOk());

        put(token, "/api/me/teach-skills", "[]").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        put(token, "/api/me/learn-skills", "[]").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        assertThat(count("user_teach_skills", AN)).isZero();
        assertThat(count("user_learn_skills", AN)).isZero();
    }

    @Test
    @DisplayName("TC30 đang dạy Java, admin ẩn Java, PUT lại danh sách dạy vẫn có Java: 200, Java được giữ")
    void tc30_hiddenSkillAlreadyOwnedIsKept() throws Exception {
        String token = anToken();
        put(token, "/api/me/teach-skills", "[" + teach("Java", 2) + "]").andExpect(status().isOk());

        setSkillActive("Java", false);
        try {
            put(token, "/api/me/teach-skills", "[" + teach("Java", 3) + ", " + teach("Python", 1) + "]")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        } finally {
            setSkillActive("Java", true);
        }
        assertThat(jdbcTemplate.queryForObject(
                "SELECT level FROM user_teach_skills WHERE skill_id = ?", Integer.class, skillId("Java"))).isEqualTo(3);
    }

    @Test
    @DisplayName("TC79 GET 3 danh sách sau khi lưu: đúng dữ liệu, có skillName, categoryName; Java bị ẩn vẫn có với isActive = false")
    void tc79_readBackLists() throws Exception {
        String token = anToken();
        put(token, "/api/me/availability",
                "[{\"dayOfWeek\": 2, \"period\": \"EVENING\"}, {\"dayOfWeek\": 5, \"period\": \"EVENING\"}]")
                .andExpect(status().isOk());
        put(token, "/api/me/teach-skills", "[" + teach("Java", 1) + ", " + teach("Python", 4) + "]")
                .andExpect(status().isOk());
        put(token, "/api/me/learn-skills", "[{\"skillId\": " + skillId("Tiếng Anh") + ", \"targetLevel\": 3}]")
                .andExpect(status().isOk());

        get(token, "/api/me/availability")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].dayOfWeek", contains(2, 5)));
        get(token, "/api/me/learn-skills")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skillId").value(skillId("Tiếng Anh")))
                .andExpect(jsonPath("$[0].skillName").value("Tiếng Anh"))
                .andExpect(jsonPath("$[0].categoryName").value("Ngoại ngữ"))
                .andExpect(jsonPath("$[0].isActive").value(true))
                .andExpect(jsonPath("$[0].targetLevel").value(3));

        setSkillActive("Java", false);
        try {
            get(token, "/api/me/teach-skills")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].skillId").value(skillId("Java")))
                    .andExpect(jsonPath("$[0].skillName").value("Java"))
                    .andExpect(jsonPath("$[0].categoryId").isNumber())
                    .andExpect(jsonPath("$[0].categoryName").value("Lập trình"))
                    .andExpect(jsonPath("$[0].isActive").value(false))
                    .andExpect(jsonPath("$[0].level").value(1))
                    .andExpect(jsonPath("$[1].skillName").value("Python"))
                    .andExpect(jsonPath("$[1].isActive").value(true));
        } finally {
            setSkillActive("Java", true);
        }
    }

    @Test
    @DisplayName("TC82 đang học Java: PUT học bỏ Java, rồi PUT dạy có Java: cả hai 200, Java chỉ nằm trong danh sách dạy")
    void tc82_moveSkillFromLearnToTeach() throws Exception {
        String token = anToken();
        put(token, "/api/me/learn-skills", "[" + learn("Java") + "]").andExpect(status().isOk());

        put(token, "/api/me/learn-skills", "[]").andExpect(status().isOk());
        put(token, "/api/me/teach-skills", "[" + teach("Java", 2) + "]").andExpect(status().isOk());

        assertThat(count("user_teach_skills", AN)).isEqualTo(1);
        assertThat(count("user_learn_skills", AN)).isZero();
    }

    @Test
    @DisplayName("TC93 PUT và GET trả lịch theo ngày rồi buổi trong ngày; kỹ năng theo tên rồi id")
    void tc93_listOrder() throws Exception {
        String token = anToken();
        String availability =
                "[{\"dayOfWeek\": 7, \"period\": \"EVENING\"}, {\"dayOfWeek\": 1, \"period\": \"EVENING\"}, "
                        + "{\"dayOfWeek\": 1, \"period\": \"MORNING\"}]";

        for (ResultActions result : List.of(put(token, "/api/me/availability", availability),
                get(token, "/api/me/availability"))) {
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].dayOfWeek", contains(1, 1, 7)))
                    .andExpect(jsonPath("$[*].period", contains("MORNING", "EVENING", "EVENING")));
        }

        String skills = "[" + teach("Python", 2) + ", " + teach("Java", 2) + "]";
        for (ResultActions result : List.of(put(token, "/api/me/teach-skills", skills),
                get(token, "/api/me/teach-skills"))) {
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].skillName", contains("Java", "Python")));
        }
    }

    @Test
    @DisplayName("TC67 A PUT, GET 3 danh sách: chỉ thấy và đổi dữ liệu của A; dữ liệu của B không đổi")
    void tc67_onlyOwnLists() throws Exception {
        createUser(AN);
        createUser(BINH);
        String tokenAn = tokenOf(AN);
        String tokenBinh = tokenOf(BINH);
        put(tokenBinh, "/api/me/availability", "[{\"dayOfWeek\": 4, \"period\": \"AFTERNOON\"}]").andExpect(status().isOk());
        put(tokenBinh, "/api/me/teach-skills", "[" + teach("Guitar", 3) + "]").andExpect(status().isOk());
        put(tokenBinh, "/api/me/learn-skills", "[" + learn("Piano") + "]").andExpect(status().isOk());

        // A chưa có gì: không thấy dữ liệu của B.
        get(tokenAn, "/api/me/availability").andExpect(jsonPath("$", hasSize(0)));
        get(tokenAn, "/api/me/teach-skills").andExpect(jsonPath("$", hasSize(0)));
        get(tokenAn, "/api/me/learn-skills").andExpect(jsonPath("$", hasSize(0)));

        // A thay cả ba danh sách (kể cả bằng [] ): chỉ của A đổi.
        put(tokenAn, "/api/me/availability", "[{\"dayOfWeek\": 1, \"period\": \"MORNING\"}]").andExpect(status().isOk());
        put(tokenAn, "/api/me/teach-skills", "[]").andExpect(status().isOk());
        put(tokenAn, "/api/me/learn-skills", "[" + learn("Excel") + "]").andExpect(status().isOk());

        assertThat(count("user_availability", BINH)).isEqualTo(1);
        assertThat(count("user_teach_skills", BINH)).isEqualTo(1);
        assertThat(count("user_learn_skills", BINH)).isEqualTo(1);
        get(tokenBinh, "/api/me/learn-skills").andExpect(jsonPath("$[0].skillName").value("Piano"));
    }

    // ================= Kỹ năng dạy và học: dữ liệu sai =================

    @Test
    @DisplayName("TC52 teach-skills với level 0, level 5, không có level: cả ba 400, errors[].field = [0].level")
    void tc52_invalidTeachLevel() throws Exception {
        String token = anToken();
        long java = skillId("Java");

        expectFieldError(put(token, "/api/me/teach-skills", "[{\"skillId\": " + java + ", \"level\": 0}]"), "[0].level");
        expectFieldError(put(token, "/api/me/teach-skills", "[{\"skillId\": " + java + ", \"level\": 5}]"), "[0].level");
        expectFieldError(put(token, "/api/me/teach-skills", "[{\"skillId\": " + java + "}]"), "[0].level");
    }

    @Test
    @DisplayName("TC53 learn-skills với targetLevel 0, rồi 5: 400")
    void tc53_invalidTargetLevel() throws Exception {
        String token = anToken();
        long java = skillId("Java");

        expectFieldError(put(token, "/api/me/learn-skills", "[{\"skillId\": " + java + ", \"targetLevel\": 0}]"),
                "[0].targetLevel");
        expectFieldError(put(token, "/api/me/learn-skills", "[{\"skillId\": " + java + ", \"targetLevel\": 5}]"),
                "[0].targetLevel");
    }

    @Test
    @DisplayName("TC54 teach-skills với skillId không có trong danh mục: 400, errors[].field = [1].skillId")
    void tc54_unknownSkill() throws Exception {
        String token = anToken();

        expectFieldError(put(token, "/api/me/teach-skills", "[" + teach("Java", 2) + ", {\"skillId\": 999999, \"level\": 2}]"),
                "[1].skillId");
        assertThat(count("user_teach_skills", AN)).isZero();
    }

    @Test
    @DisplayName("TC55 kỹ năng đang ẩn, người dùng chưa có, PUT learn-skills chứa nó: 400")
    void tc55_hiddenSkillNotOwnedIsRejected() throws Exception {
        String token = anToken();

        setSkillActive("Canva", false);
        try {
            expectFieldError(put(token, "/api/me/learn-skills", "[" + learn("Canva") + "]"), "[0].skillId");
        } finally {
            setSkillActive("Canva", true);
        }
        assertThat(count("user_learn_skills", AN)).isZero();
    }

    @Test
    @DisplayName("D20 đang dạy Java bị ẩn, chuyển Java sang danh sách học là thêm mới: 400")
    void hiddenSkillCannotMoveToOtherList() throws Exception {
        String token = anToken();
        put(token, "/api/me/teach-skills", "[" + teach("Java", 2) + "]").andExpect(status().isOk());
        put(token, "/api/me/teach-skills", "[]").andExpect(status().isOk());

        setSkillActive("Java", false);
        try {
            expectFieldError(put(token, "/api/me/learn-skills", "[" + learn("Java") + "]"), "[0].skillId");
        } finally {
            setSkillActive("Java", true);
        }
    }

    @Test
    @DisplayName("TC56 teach-skills có cùng skillId hai lần: 400, báo ở lần thứ hai ([1].skillId)")
    void tc56_duplicateSkill() throws Exception {
        String token = anToken();

        expectFieldError(put(token, "/api/me/teach-skills", "[" + teach("Java", 2) + ", " + teach("Java", 3) + "]"),
                "[1].skillId");
        assertThat(count("user_teach_skills", AN)).isZero();
    }

    @Test
    @DisplayName("TC57 learn-skills với 11 kỹ năng: 400; ở biên 10 kỹ năng: 200")
    void tc57_learnSkillsLimit() throws Exception {
        String token = anToken();

        expectFieldError(put(token, "/api/me/learn-skills", firstSkills(11, "targetLevel")), "learnSkills");
        put(token, "/api/me/learn-skills", firstSkills(10, "targetLevel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)));
    }

    @Test
    @DisplayName("TC84 teach-skills với 11 kỹ năng: 400; ở biên 10 kỹ năng: 200")
    void tc84_teachSkillsLimit() throws Exception {
        String token = anToken();

        expectFieldError(put(token, "/api/me/teach-skills", firstSkills(11, "level")), "teachSkills");
        put(token, "/api/me/teach-skills", firstSkills(10, "level"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)));
    }

    @Test
    @DisplayName("TC58 đang muốn học Java, PUT teach-skills có Java: 400")
    void tc58_teachSkillAlreadyInLearnList() throws Exception {
        String token = anToken();
        put(token, "/api/me/learn-skills", "[" + learn("Java") + "]").andExpect(status().isOk());

        expectFieldError(put(token, "/api/me/teach-skills", "[" + teach("Python", 2) + ", " + teach("Java", 2) + "]"),
                "[1].skillId");
        assertThat(count("user_teach_skills", AN)).isZero();
    }

    @Test
    @DisplayName("TC83 đang dạy Java, PUT learn-skills có Java: 400")
    void tc83_learnSkillAlreadyInTeachList() throws Exception {
        String token = anToken();
        put(token, "/api/me/teach-skills", "[" + teach("Java", 2) + "]").andExpect(status().isOk());

        expectFieldError(put(token, "/api/me/learn-skills", "[" + learn("Java") + "]"), "[0].skillId");
        assertThat(count("user_learn_skills", AN)).isZero();
    }
}
