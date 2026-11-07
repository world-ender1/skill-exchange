package com.skillexchange.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.skillexchange.IntegrationTestBase;
import com.skillexchange.TestAccounts;

/**
 * GET /api/users/search (kế hoạch tuần 4, bước 9): TC163–TC172; TK 4.3, D37, D45, D46(c).
 * Kỹ năng dạy chèn thẳng bằng SQL (nhanh hơn PUT qua API, và luật PUT đã có test ở tuần 3).
 */
class UserSearchTest extends IntegrationTestBase {

    private static final String CALLER = "goi@student.example.edu.vn";
    private static final String MISSING_BOTH = "Cần ít nhất một tham số: q hoặc skillId";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private TestAccounts accounts;
    private String token;
    private int emailCounter;

    @BeforeEach
    void setUpCaller() throws Exception {
        accounts = new TestAccounts(mockMvc, jdbcTemplate, userRepository, passwordEncoder);
        // Tên người gọi không chứa "an", "test", "nguyen" để không lẫn vào kết quả tìm theo tên.
        accounts.activeStudent(CALLER, "Người gọi");
        token = accounts.login(CALLER);
    }

    // ---------- Dữ liệu mẫu ----------

    /** Sinh viên ACTIVE mới với email tự sinh; trả id. */
    private long student(String fullName) {
        return accounts.activeStudent("sv" + (++emailCounter) + "@student.example.edu.vn", fullName);
    }

    private long account(String fullName, UserStatus status, Role role) {
        return accounts.create("tk" + (++emailCounter) + "@student.example.edu.vn", fullName, status, role);
    }

    private void teaches(long userId, String skill, int level) {
        jdbcTemplate.update("INSERT INTO user_teach_skills (user_id, skill_id, level) VALUES (?, ?, ?)",
                userId, accounts.skillId(skill), level);
    }

    private void learns(long userId, String skill) {
        jdbcTemplate.update("INSERT INTO user_learn_skills (user_id, skill_id, target_level) VALUES (?, ?, NULL)",
                userId, accounts.skillId(skill));
    }

    private ResultActions search(String... params) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/users/search")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(request);
    }

    private List<String> names(String... params) throws Exception {
        String body = search(params).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[*].fullName");
    }

    private List<Integer> ids(String... params) throws Exception {
        String body = search(params).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[*].id");
    }

    private String skill(String name) {
        return String.valueOf(accounts.skillId(name));
    }

    // ---------- Tham số ----------

    @Test
    @DisplayName("TC163 không tham số; q= ; q chỉ có khoảng trắng (không skillId): cả 3 là 400")
    void tc163_missingBothParametersIs400() throws Exception {
        search()
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(MISSING_BOTH));
        search("q", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(MISSING_BOTH));
        search("q", "  ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(MISSING_BOTH));
    }

    @Test
    @DisplayName("TC164 q dài 100 ký tự: 200; 101 ký tự: 400 errors[].field = q")
    void tc164_queryLengthLimit() throws Exception {
        search("q", "a".repeat(100)).andExpect(status().isOk());
        search("q", "a".repeat(101))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("q"));
    }

    @Test
    @DisplayName("D45 q 100 ký tự kèm khoảng trắng hai đầu: 200 (độ dài đếm sau khi cắt)")
    void d45_lengthCountedAfterStrip() throws Exception {
        search("q", "  " + "a".repeat(100) + "  ").andExpect(status().isOk());
    }

    @Test
    @DisplayName("D38 skillId=abc: 400 tiếng Việt có tên tham số")
    void d38_nonNumericSkillIdIs400() throws Exception {
        search("skillId", "abc")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Tham số skillId có giá trị không hợp lệ: abc"));
    }

    // ---------- Tìm theo tên ----------

    @Test
    @DisplayName("TC165 q=AN và q=an: Nguyễn An, Lê Văn Anh; không có Trần Ân")
    void tc165_caseInsensitiveButAccentSensitive() throws Exception {
        student("Nguyễn An");
        student("Lê Văn Anh");
        student("Trần Ân");

        assertThat(names("q", "AN")).containsExactlyInAnyOrder("Nguyễn An", "Lê Văn Anh");
        assertThat(names("q", "an")).containsExactlyInAnyOrder("Nguyễn An", "Lê Văn Anh");
    }

    @Test
    @DisplayName("TC166 q=% rỗng; q=_ chỉ Lê_Test; q=\\ rỗng: ký tự đặc biệt hiểu theo nghĩa đen (D37)")
    void tc166_likeWildcardsAreLiteral() throws Exception {
        student("Lê_Test");
        student("Le Test");

        assertThat(names("q", "%")).isEmpty();
        assertThat(names("q", "_")).containsExactly("Lê_Test");
        assertThat(names("q", "\\")).isEmpty();
    }

    @Test
    @DisplayName("D45 q được cắt khoảng trắng hai đầu trước khi tìm")
    void d45_queryIsStripped() throws Exception {
        student("Nguyễn An");
        assertThat(names("q", "  Nguyễn An  ")).containsExactly("Nguyễn An");
    }

    // ---------- Tìm theo kỹ năng ----------

    @Test
    @DisplayName("TC167 skillId=Java: chỉ người DẠY Java, không người muốn học, không kỹ năng liên quan")
    void tc167_matchesTeachSkillExactly() throws Exception {
        long x = student("X");
        teaches(x, "Java", 3);
        long y = student("Y");
        learns(y, "Java");
        long z = student("Z");
        teaches(z, "Kotlin", 3);

        assertThat(names("skillId", skill("Java"))).containsExactly("X");
    }

    @Test
    @DisplayName("TC168 skillId của kỹ năng bị ẩn vẫn khớp; skillId=999999: 200 với []")
    void tc168_hiddenSkillMatchesAndUnknownSkillIsEmpty() throws Exception {
        long x = student("X");
        teaches(x, "Kotlin", 2);
        jdbcTemplate.update("UPDATE skills SET is_active = false WHERE name = 'Kotlin'");
        try {
            assertThat(names("skillId", skill("Kotlin"))).containsExactly("X");
        } finally {
            jdbcTemplate.update("UPDATE skills SET is_active = true WHERE name = 'Kotlin'");
        }

        search("skillId", "999999")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("TC169 skillId=Java và q=Nguyen: chỉ người thỏa cả hai")
    void tc169_bothParametersMustMatch() throws Exception {
        long x = student("Tran X");
        teaches(x, "Java", 3);
        long b = student("Nguyen B");
        teaches(b, "Python", 3);
        long c = student("Nguyen C");
        teaches(c, "Java", 2);

        assertThat(names("skillId", skill("Java"), "q", "Nguyen")).containsExactly("Nguyen C");
    }

    @Test
    @DisplayName("TC170 không có người gọi, admin, PENDING, LOCKED dù cùng dạy Java")
    void tc170_onlyOtherActiveStudents() throws Exception {
        teaches(accounts.idOf(CALLER), "Java", 4);
        teaches(account("Admin", UserStatus.ACTIVE, Role.ADMIN), "Java", 4);
        teaches(account("Pending", UserStatus.PENDING_VERIFICATION, Role.STUDENT), "Java", 4);
        teaches(account("Locked", UserStatus.LOCKED, Role.STUDENT), "Java", 4);
        teaches(student("Active"), "Java", 4);

        assertThat(names("skillId", skill("Java"))).containsExactly("Active");
    }

    @Test
    @DisplayName("TC171 22 người dạy Java: trả 20, xếp theo fullName rồi id, trùng tên thì id nhỏ trước")
    void tc171_top20OrderedByNameThenId() throws Exception {
        record Person(String name, long id) {
        }
        List<Person> people = new ArrayList<>();
        // Tạo ngược thứ tự tên để id không trùng thứ tự tên. Tên không dấu (⚠2 của bộ ca).
        for (int i = 21; i >= 1; i--) {
            String name = "Hoc vien %02d".formatted(i);
            long id = student(name);
            teaches(id, "Java", 2);
            people.add(new Person(name, id));
        }
        // Người thứ 22 trùng tên với "Hoc vien 05", id lớn hơn.
        long duplicate = student("Hoc vien 05");
        teaches(duplicate, "Java", 2);
        people.add(new Person("Hoc vien 05", duplicate));

        List<Integer> expected = people.stream()
                .sorted(Comparator.comparing(Person::name).thenComparingLong(Person::id))
                .limit(20)
                .map(person -> (int) person.id())
                .toList();

        assertThat(ids("skillId", skill("Java"))).containsExactlyElementsOf(expected);
    }

    // ---------- Dạng response ----------

    @Test
    @DisplayName("TC172 response là mảng; phần tử chỉ có id, fullName, learningMode, teachSkills; không lộ liên hệ")
    void tc172_resultShapeHasNoPrivateFields() throws Exception {
        String email = "lienhe@student.example.edu.vn";
        long id = accounts.activeStudent(email, "Lien He");
        accounts.putProfile(accounts.login(email), "Lien He", "OFFLINE", "0909999999", "zalo-rieng-tu");
        teaches(id, "Java", 3);

        String body = search("skillId", skill("Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> item = JsonPath.read(body, "$[0]");
        assertThat(item).containsOnlyKeys("id", "fullName", "learningMode", "teachSkills");
        assertThat(item.get("learningMode")).isEqualTo("OFFLINE");
        Map<String, Object> teachRow = JsonPath.read(body, "$[0].teachSkills[0]");
        assertThat(teachRow).containsOnlyKeys("skillId", "skillName", "categoryName", "level");
        assertThat(body).doesNotContain("0909999999", "zalo-rieng-tu", email);
    }

    @Test
    @DisplayName("D46c teachSkills có mọi kỹ năng dạy (không chỉ kỹ năng khớp), xếp theo tên rồi skillId")
    void d46c_allTeachSkillsInNameOrder() throws Exception {
        long x = student("X");
        teaches(x, "Python", 4);
        teaches(x, "Java", 2);

        search("skillId", skill("Python"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].teachSkills", hasSize(2)))
                .andExpect(jsonPath("$[0].teachSkills[0].skillName").value("Java"))
                .andExpect(jsonPath("$[0].teachSkills[0].level").value(2))
                .andExpect(jsonPath("$[0].teachSkills[1].skillName").value("Python"));
    }
}
