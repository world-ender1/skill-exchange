package com.skillexchange.admin;

import static com.skillexchange.TestAccounts.slot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
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
import com.skillexchange.user.Role;
import com.skillexchange.user.UserRepository;
import com.skillexchange.user.UserStatus;

/**
 * API quản trị danh mục kỹ năng (TK 4.9; kế hoạch tuần 5, bước 9): TC250–TC261; D73–D75.
 *
 * Danh mục do V2 nạp được giữ qua mọi test (IntegrationTestBase chỉ xóa người dùng), nên mỗi test
 * chụp danh mục trước khi chạy và trả lại y như cũ sau khi chạy (CatalogSnapshot, D39).
 * Id kỹ năng luôn tra theo tên: V2 không sinh id theo thứ tự khai báo.
 */
class AdminSkillApiTest extends IntegrationTestBase {

    private static final String ADMIN = "qtv@student.example.edu.vn";
    private static final String STUDENT = "sv@student.example.edu.vn";
    private static final String AN = "an@student.example.edu.vn";
    private static final String BINH = "binh@student.example.edu.vn";

    private static final long MISSING_ID = 99999;
    private static final Set<String> SKILL_KEYS = Set.of("id", "name", "categoryId", "categoryName", "isActive");
    private static final Set<String> CATEGORY_KEYS = Set.of("id", "name");
    private static final Set<String> RELATION_KEYS = Set.of("skillAId", "skillAName", "skillBId", "skillBName");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private TestAccounts accounts;
    private CatalogSnapshot catalog;
    private String adminToken;

    @BeforeEach
    void setUpAdmin() throws Exception {
        catalog = CatalogSnapshot.take(jdbcTemplate);
        accounts = new TestAccounts(mockMvc, jdbcTemplate, userRepository, passwordEncoder);
        accounts.create(ADMIN, "Quản trị viên", UserStatus.ACTIVE, Role.ADMIN);
        adminToken = accounts.login(ADMIN);
    }

    // Chạy cả khi test đỏ, nên một test hỏng không kéo theo test của lớp khác.
    @AfterEach
    void restoreCatalog() {
        catalog.restore();
    }

    // ---------- Dựng request ----------

    private static MockHttpServletRequestBuilder postJson(String path, String json) {
        return post(path).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private static MockHttpServletRequestBuilder putJson(String path, String json) {
        return put(path).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    /** token null = không gửi header Authorization. */
    private ResultActions send(MockHttpServletRequestBuilder request, String token) throws Exception {
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return send(request, adminToken);
    }

    private static String nameJson(String name) {
        return "{\"name\": \"" + name + "\"}";
    }

    private static String createSkillJson(String name, long categoryId) {
        return "{\"name\": \"" + name + "\", \"categoryId\": " + categoryId + "}";
    }

    private static String updateSkillJson(String name, long categoryId, boolean isActive) {
        return "{\"name\": \"" + name + "\", \"categoryId\": " + categoryId + ", \"isActive\": " + isActive + "}";
    }

    private static String relationJson(long skillAId, long skillBId) {
        return "{\"skillAId\": " + skillAId + ", \"skillBId\": " + skillBId + "}";
    }

    // ---------- Đọc dữ liệu ----------

    private long skillId(String name) {
        return accounts.skillId(name);
    }

    private long categoryId(String name) {
        return jdbcTemplate.queryForObject("SELECT id FROM skill_categories WHERE name = ?", Long.class, name);
    }

    private String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }

    private List<Map<String, Object>> adminSkills() throws Exception {
        return JsonPath.read(body(asAdmin(get("/api/admin/skills")).andExpect(status().isOk())), "$");
    }

    private List<Map<String, Object>> relations() throws Exception {
        return JsonPath.read(body(asAdmin(get("/api/admin/skill-relations")).andExpect(status().isOk())), "$");
    }

    /** Tên trong GET /api/skills (chỉ kỹ năng đang hoạt động), gọi bằng vé của admin. */
    private List<String> activeSkillNames() throws Exception {
        return JsonPath.read(body(asAdmin(get("/api/skills")).andExpect(status().isOk())), "$[*].name");
    }

    private static Map<String, Object> byName(List<Map<String, Object>> items, String name) {
        return items.stream()
                .filter(item -> name.equals(item.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Không có " + name + " trong " + items));
    }

    private static long asLong(Object number) {
        return ((Number) number).longValue();
    }

    // ================= Quyền =================

    /**
     * Mỗi lần gọi dựng 8 request mới (MockMvc không cho dùng lại một request đã gửi).
     * Dữ liệu hợp lệ trên danh mục V2, theo thứ tự này admin nhận đúng mã thành công của từng API.
     */
    private List<MockHttpServletRequestBuilder> eightApis(long music, long programming, long python, long kotlin) {
        return List.of(
                get("/api/admin/skills"),
                postJson("/api/admin/skill-categories", nameJson("Khoa học")),
                putJson("/api/admin/skill-categories/" + music, nameJson("Âm nhạc")),
                postJson("/api/admin/skills", createSkillJson("Rust", programming)),
                putJson("/api/admin/skills/" + python, updateSkillJson("Python", programming, true)),
                get("/api/admin/skill-relations"),
                postJson("/api/admin/skill-relations", relationJson(kotlin, python)),
                delete("/api/admin/skill-relations/" + kotlin + "/" + python));
    }

    private List<Integer> statusesOf(List<MockHttpServletRequestBuilder> requests, String token) throws Exception {
        List<Integer> statuses = new ArrayList<>();
        for (MockHttpServletRequestBuilder request : requests) {
            statuses.add(send(request, token).andReturn().getResponse().getStatus());
        }
        return statuses;
    }

    @Test
    @DisplayName("TC250 8 API danh mục: sinh viên 403, không token 401, admin thành công, admin bị LOCKED 403")
    void tc250_onlyActiveAdminCanManageCatalog() throws Exception {
        accounts.activeStudent(STUDENT, "Sinh viên");
        String studentToken = accounts.login(STUDENT);
        long music = categoryId("Âm nhạc");
        long programming = categoryId("Lập trình");
        long python = skillId("Python");
        long kotlin = skillId("Kotlin");

        assertThat(statusesOf(eightApis(music, programming, python, kotlin), studentToken)).containsOnly(403);
        assertThat(statusesOf(eightApis(music, programming, python, kotlin), null)).containsOnly(401);

        // Chặt hơn "không phải 401/403" của ca: khi API chưa có, admin nhận 404 – cũng "không phải 401/403".
        assertThat(statusesOf(eightApis(music, programming, python, kotlin), adminToken))
                .containsExactly(200, 201, 200, 201, 200, 200, 201, 204);

        // Vé vẫn còn hạn, nhưng AccountStatusFilter đọc lại trạng thái từ DB ở mỗi request (D13).
        jdbcTemplate.update("UPDATE users SET status = 'LOCKED' WHERE email = ?", ADMIN);
        assertThat(statusesOf(eightApis(music, programming, python, kotlin), adminToken)).containsOnly(403);
    }

    // ================= Kỹ năng =================

    @Test
    @DisplayName("TC251 GET /api/admin/skills: 26 kỹ năng kể cả Kotlin bị ẩn, đủ 5 trường, xếp theo name rồi id")
    void tc251_listsAllSkillsIncludingHidden() throws Exception {
        jdbcTemplate.update("UPDATE skills SET is_active = false WHERE name = 'Kotlin'");

        List<Map<String, Object>> skills = adminSkills();

        assertThat(skills).hasSize(26);
        assertThat(skills).allSatisfy(skill -> assertThat(skill.keySet()).isEqualTo(SKILL_KEYS));
        assertThat(byName(skills, "Kotlin").get("isActive")).isEqualTo(false);
        assertThat(byName(skills, "Java").get("isActive")).isEqualTo(true);
        assertThat(byName(skills, "Kotlin").get("categoryName")).isEqualTo("Lập trình");
        assertThat(asLong(byName(skills, "Kotlin").get("categoryId"))).isEqualTo(categoryId("Lập trình"));

        // Thứ tự theo collation của PostgreSQL (⚠3 của bộ ca): so với chính DB, và so các tên không dấu.
        List<String> names = skills.stream().map(skill -> (String) skill.get("name")).toList();
        List<String> expectedOrder = jdbcTemplate.queryForList("SELECT name FROM skills ORDER BY name, id", String.class);
        assertThat(names).containsExactlyElementsOf(expectedOrder);
        List<String> asciiNames = names.stream().filter(name -> name.chars().allMatch(ch -> ch < 128)).toList();
        assertThat(asciiNames).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
    }

    @Test
    @DisplayName("TC254 POST /api/admin/skills: Rust 201; \"java\" 409; nhóm không có 404; thiếu categoryId 400; tên rỗng 400")
    void tc254_createSkill() throws Exception {
        long programming = categoryId("Lập trình");

        String created = body(asAdmin(postJson("/api/admin/skills", createSkillJson("Rust", programming)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Rust"))
                .andExpect(jsonPath("$.categoryId").value(programming))
                .andExpect(jsonPath("$.categoryName").value("Lập trình"))
                .andExpect(jsonPath("$.isActive").value(true)));
        Map<String, Object> rust = JsonPath.read(created, "$");
        assertThat(rust.keySet()).isEqualTo(SKILL_KEYS);
        assertThat(asLong(rust.get("id"))).isEqualTo(skillId("Rust"));
        assertThat(activeSkillNames()).contains("Rust");

        asAdmin(postJson("/api/admin/skills", createSkillJson("java", programming)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Tên kỹ năng đã tồn tại"));
        asAdmin(postJson("/api/admin/skills", createSkillJson("Go", MISSING_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Không tìm thấy nhóm kỹ năng"));
        asAdmin(postJson("/api/admin/skills", "{\"name\": \"Go\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("categoryId")));
        asAdmin(postJson("/api/admin/skills", createSkillJson("", programming)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")));

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM skills", Integer.class)).isEqualTo(27);
    }

    @Test
    @DisplayName("TC255 ẩn Kotlin: mất khỏi /api/skills, còn trong /api/admin/skills và hồ sơ người dạy; hiện lại thì có lại")
    void tc255_hideAndShowSkill() throws Exception {
        accounts.activeStudent(STUDENT, "Sinh viên");
        String studentToken = accounts.login(STUDENT);
        accounts.putTeachSkills(studentToken, accounts.teach("Kotlin", 2));
        long kotlin = skillId("Kotlin");
        long programming = categoryId("Lập trình");

        asAdmin(putJson("/api/admin/skills/" + kotlin, updateSkillJson("Kotlin", programming, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(kotlin))
                .andExpect(jsonPath("$.isActive").value(false));

        assertThat(activeSkillNames()).doesNotContain("Kotlin");
        assertThat(byName(adminSkills(), "Kotlin").get("isActive")).isEqualTo(false);
        send(get("/api/me/teach-skills"), studentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].skillName", hasItem("Kotlin")));

        asAdmin(putJson("/api/admin/skills/" + kotlin, updateSkillJson("Kotlin", programming, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));
        assertThat(activeSkillNames()).contains("Kotlin");
    }

    @Test
    @DisplayName("TC256 PUT /api/admin/skills/{Python}: \"PYTHON\" 200; \"java\" 409; thiếu isActive 400; id không có 404")
    void tc256_updateSkillName() throws Exception {
        long python = skillId("Python");
        long programming = categoryId("Lập trình");

        // Đổi tên chính mình khác hoa thường thì được (D74): không tự trùng với chính mình.
        asAdmin(putJson("/api/admin/skills/" + python, updateSkillJson("PYTHON", programming, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("PYTHON"));

        asAdmin(putJson("/api/admin/skills/" + python, updateSkillJson("java", programming, true)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Tên kỹ năng đã tồn tại"));
        asAdmin(putJson("/api/admin/skills/" + python,
                        "{\"name\": \"Python\", \"categoryId\": " + programming + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("isActive")));
        asAdmin(putJson("/api/admin/skills/" + MISSING_ID, updateSkillJson("Go", programming, true)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Không tìm thấy kỹ năng"));

        assertThat(jdbcTemplate.queryForObject("SELECT name FROM skills WHERE id = ?", String.class, python))
                .isEqualTo("PYTHON");
    }

    @Test
    @DisplayName("TC257 đổi nhóm sang Dữ liệu: Java (liên quan Kotlin) 409; Python 200; đổi tên Java giữ nhóm 200")
    void tc257_categoryChangeBlockedByRelations() throws Exception {
        long java = skillId("Java");
        long python = skillId("Python");
        long programming = categoryId("Lập trình");
        long data = categoryId("Dữ liệu");

        asAdmin(putJson("/api/admin/skills/" + java, updateSkillJson("Java", data, true)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Kỹ năng đang có quan hệ liên quan, hãy gỡ quan hệ trước khi đổi nhóm"));
        assertThat(jdbcTemplate.queryForObject("SELECT category_id FROM skills WHERE id = ?", Long.class, java))
                .isEqualTo(programming);

        asAdmin(putJson("/api/admin/skills/" + python, updateSkillJson("Python", data, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(data))
                .andExpect(jsonPath("$.categoryName").value("Dữ liệu"));

        asAdmin(putJson("/api/admin/skills/" + java, updateSkillJson("Java SE", programming, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Java SE"));
    }

    // ================= Nhóm kỹ năng =================

    @Test
    @DisplayName("TC252 POST /api/admin/skill-categories: cắt khoảng trắng 201; \"lập trình\" 409; toàn khoảng trắng, 101 ký tự, {} 400")
    void tc252_createCategory() throws Exception {
        String created = body(asAdmin(postJson("/api/admin/skill-categories", nameJson("  Khoa học  ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Khoa học")));
        Map<String, Object> category = JsonPath.read(created, "$");
        assertThat(category.keySet()).isEqualTo(CATEGORY_KEYS);
        assertThat(asLong(category.get("id"))).isEqualTo(categoryId("Khoa học"));

        asAdmin(postJson("/api/admin/skill-categories", nameJson("lập trình")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Tên nhóm kỹ năng đã tồn tại"));
        asAdmin(postJson("/api/admin/skill-categories", nameJson("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")));
        asAdmin(postJson("/api/admin/skill-categories", nameJson("a".repeat(101))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
        asAdmin(postJson("/api/admin/skill-categories", "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")));

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM skill_categories", Integer.class)).isEqualTo(7);
    }

    @Test
    @DisplayName("TC253 PUT /api/admin/skill-categories/{id}: Âm nhạc → Nhạc cụ 200; → DỮ LIỆU 409; LẬP TRÌNH 200; 404; abc 400")
    void tc253_renameCategory() throws Exception {
        long music = categoryId("Âm nhạc");
        long programming = categoryId("Lập trình");

        asAdmin(putJson("/api/admin/skill-categories/" + music, nameJson("Nhạc cụ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(music))
                .andExpect(jsonPath("$.name").value("Nhạc cụ"));
        asAdmin(get("/api/skill-categories"))
                .andExpect(jsonPath("$[*].name", hasItem("Nhạc cụ")))
                .andExpect(jsonPath("$[*].name", not(hasItem("Âm nhạc"))));
        asAdmin(get("/api/skills").param("categoryId", String.valueOf(music)))
                .andExpect(jsonPath("$[*].categoryName", hasItem("Nhạc cụ")))
                .andExpect(jsonPath("$[*].categoryName", not(hasItem("Âm nhạc"))));

        // "DỮ LIỆU" trùng "Dữ liệu" khi so không phân biệt hoa thường: cần lower() hiểu chữ có dấu.
        asAdmin(putJson("/api/admin/skill-categories/" + music, nameJson("DỮ LIỆU")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Tên nhóm kỹ năng đã tồn tại"));
        asAdmin(putJson("/api/admin/skill-categories/" + programming, nameJson("LẬP TRÌNH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("LẬP TRÌNH"));
        asAdmin(putJson("/api/admin/skill-categories/" + MISSING_ID, nameJson("Khoa học")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Không tìm thấy nhóm kỹ năng"));
        asAdmin(putJson("/api/admin/skill-categories/abc", nameJson("Khoa học")))
                .andExpect(status().isBadRequest());
    }

    // ================= Quan hệ liên quan =================

    @Test
    @DisplayName("TC258 POST /api/admin/skill-relations: mã lớn trước 201; gửi lại 409; khác nhóm 400; trùng mã 400; mã không có 404; với kỹ năng ẩn 201")
    void tc258_createRelation() throws Exception {
        long kotlin = skillId("Kotlin");
        long python = skillId("Python");
        long low = Math.min(kotlin, python);
        long high = Math.max(kotlin, python);
        String lowName = low == kotlin ? "Kotlin" : "Python";
        String highName = high == kotlin ? "Kotlin" : "Python";

        String created = body(asAdmin(postJson("/api/admin/skill-relations", relationJson(high, low)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.skillAId").value(low))
                .andExpect(jsonPath("$.skillAName").value(lowName))
                .andExpect(jsonPath("$.skillBId").value(high))
                .andExpect(jsonPath("$.skillBName").value(highName)));
        assertThat(JsonPath.<Map<String, Object>>read(created, "$").keySet()).isEqualTo(RELATION_KEYS);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM skill_relations WHERE skill_a_id = ? AND skill_b_id = ?",
                Integer.class, low, high)).isEqualTo(1);

        asAdmin(postJson("/api/admin/skill-relations", relationJson(low, high)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Hai kỹ năng đã được đánh dấu liên quan"));

        long java = skillId("Java");
        asAdmin(postJson("/api/admin/skill-relations", relationJson(java, skillId("SQL"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Hai kỹ năng phải cùng nhóm"));
        asAdmin(postJson("/api/admin/skill-relations", relationJson(java, java)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Hai kỹ năng phải khác nhau"));
        asAdmin(postJson("/api/admin/skill-relations", relationJson(java, MISSING_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Không tìm thấy kỹ năng"));

        // D75: kỹ năng bị ẩn vẫn được tạo quan hệ mới.
        jdbcTemplate.update("UPDATE skills SET is_active = false WHERE name = 'C++'");
        asAdmin(postJson("/api/admin/skill-relations", relationJson(java, skillId("C++"))))
                .andExpect(status().isCreated());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM skill_relations", Integer.class)).isEqualTo(11);
    }

    @Test
    @DisplayName("TC259 GET /api/admin/skill-relations: 9 cặp của V2, đủ 4 trường, skillAId < skillBId, xếp theo skillAId rồi skillBId")
    void tc259_listRelations() throws Exception {
        List<Map<String, Object>> relations = relations();

        assertThat(relations).hasSize(9);
        assertThat(relations).allSatisfy(relation -> {
            assertThat(relation.keySet()).isEqualTo(RELATION_KEYS);
            long a = asLong(relation.get("skillAId"));
            long b = asLong(relation.get("skillBId"));
            assertThat(a).isLessThan(b);
            assertThat(relation.get("skillAName")).isEqualTo(
                    jdbcTemplate.queryForObject("SELECT name FROM skills WHERE id = ?", String.class, a));
            assertThat(relation.get("skillBName")).isEqualTo(
                    jdbcTemplate.queryForObject("SELECT name FROM skills WHERE id = ?", String.class, b));
        });

        List<List<Long>> pairs = relations.stream()
                .map(relation -> List.of(asLong(relation.get("skillAId")), asLong(relation.get("skillBId"))))
                .toList();
        List<List<Long>> expected = jdbcTemplate.query(
                "SELECT skill_a_id, skill_b_id FROM skill_relations ORDER BY skill_a_id, skill_b_id",
                (rs, rowNum) -> List.of(rs.getLong(1), rs.getLong(2)));
        assertThat(pairs).containsExactlyElementsOf(expected);
    }

    /** An: học Java (3), Tiếng Anh; dạy Python (4); Tối T2, T4, T6; ONLINE – như MatchesApiTest. Trả vé. */
    private String an() throws Exception {
        accounts.activeStudent(AN, "An");
        String token = accounts.login(AN);
        accounts.putProfile(token, "An", "ONLINE", null, null);
        accounts.putLearnSkills(token, accounts.learn("Java", 3), accounts.learn("Tiếng Anh", null));
        accounts.putTeachSkills(token, accounts.teach("Python", 4));
        accounts.putAvailability(token, slot(1, "EVENING"), slot(3, "EVENING"), slot(5, "EVENING"));
        return token;
    }

    /** Bình: học Python (2); dạy Kotlin (2), Tiếng Anh (3); Tối T4, T6, Sáng T7, Sáng CN; ANY. Trả id. */
    private long binh() throws Exception {
        long id = accounts.activeStudent(BINH, "Bình");
        String token = accounts.login(BINH);
        accounts.putProfile(token, "Bình", "ANY", null, null);
        accounts.putLearnSkills(token, accounts.learn("Python", 2));
        accounts.putTeachSkills(token, accounts.teach("Kotlin", 2), accounts.teach("Tiếng Anh", 3));
        accounts.putAvailability(token, slot(3, "EVENING"), slot(5, "EVENING"),
                slot(6, "MORNING"), slot(7, "MORNING"));
        return id;
    }

    /** forward.skillFit của người có id userId trong /api/matches?type=TWO_WAY của chủ vé. */
    private double forwardSkillFit(String token, long userId) throws Exception {
        String matches = body(send(get("/api/matches").param("type", "TWO_WAY"), token).andExpect(status().isOk()));
        List<Number> fits = JsonPath.read(matches, "$.items[?(@.userId == " + userId + ")].forward.skillFit");
        assertThat(fits).as("userId %d trong %s", userId, matches).hasSize(1);
        return fits.get(0).doubleValue();
    }

    @Test
    @DisplayName("TC260 DELETE cặp Java–Kotlin (mã lớn trước): 204, gọi lại 404; skillFit của Bình với An 0.75 → 0.5")
    void tc260_deleteRelationAffectsMatching() throws Exception {
        String anToken = an();
        long binhId = binh();
        long java = skillId("Java");
        long kotlin = skillId("Kotlin");
        long low = Math.min(java, kotlin);
        long high = Math.max(java, kotlin);

        // Trước khi xóa: Java (An học) ~ Kotlin (Bình dạy) = 0.5, Tiếng Anh = 1 → (0.5 + 1) / 2.
        assertThat(forwardSkillFit(anToken, binhId)).isEqualTo((0.5 + 1.0) / 2);

        asAdmin(delete("/api/admin/skill-relations/" + high + "/" + low))
                .andExpect(status().isNoContent());
        asAdmin(delete("/api/admin/skill-relations/" + high + "/" + low))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Không tìm thấy quan hệ liên quan"));

        List<Map<String, Object>> relations = relations();
        assertThat(relations).hasSize(8);
        assertThat(relations).noneSatisfy(relation -> assertThat(List.of(
                asLong(relation.get("skillAId")), asLong(relation.get("skillBId")))).isEqualTo(List.of(low, high)));

        // Sau khi xóa: Java ~ Kotlin = 0 (không còn liên quan), Tiếng Anh = 1 → (0 + 1) / 2.
        assertThat(forwardSkillFit(anToken, binhId)).isEqualTo((0.0 + 1.0) / 2);
    }

    @Test
    @DisplayName("TC261 POST quan hệ {99999, 99999} và PUT /skills/99999 tên rỗng: 400, không phải 404")
    void tc261_badDataCheckedBeforeNotFound() throws Exception {
        asAdmin(postJson("/api/admin/skill-relations", relationJson(MISSING_ID, MISSING_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Hai kỹ năng phải khác nhau"));
        asAdmin(putJson("/api/admin/skills/" + MISSING_ID, updateSkillJson("", categoryId("Lập trình"), true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")));
    }

    // ================= Phần D chưa có ca =================

    @Test
    @DisplayName("D74 PUT kỹ năng với categoryId không có: 404; ẩn, đổi tên Java đang có quan hệ: 200, quan hệ giữ nguyên")
    void d74_updateSkillCategoryNotFoundAndRelationsKept() throws Exception {
        long java = skillId("Java");
        long programming = categoryId("Lập trình");

        asAdmin(putJson("/api/admin/skills/" + java, updateSkillJson("Java", MISSING_ID, true)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Không tìm thấy nhóm kỹ năng"));

        asAdmin(putJson("/api/admin/skills/" + java, updateSkillJson("Java 21", programming, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
        // D75: quan hệ liên quan giữ nguyên khi ẩn; GET quan hệ trả tên mới.
        assertThat(relations()).anySatisfy(relation -> {
            assertThat(relation.get("skillAName")).isIn("Java 21", "Kotlin");
            assertThat(relation.get("skillBName")).isIn("Java 21", "Kotlin");
        });
    }

    @Test
    @DisplayName("D74 tên dài đúng 100 ký tự sau khi cắt: 201; lỗi theo trường có câu chữ đã chốt")
    void d74_nameLengthCountedAfterStripAndMessages() throws Exception {
        String hundred = "b".repeat(100);
        asAdmin(postJson("/api/admin/skill-categories", nameJson("  " + hundred + "  ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(hundred));

        asAdmin(postJson("/api/admin/skill-categories", nameJson(" ")))
                .andExpect(jsonPath("$.errors[0].message").value("Tên không được để trống"));
        asAdmin(postJson("/api/admin/skill-categories", nameJson("c".repeat(101))))
                .andExpect(jsonPath("$.errors[0].message").value("Tên dài tối đa 100 ký tự"));
        asAdmin(postJson("/api/admin/skills", "{\"name\": \"Go\"}"))
                .andExpect(jsonPath("$.errors[0].message").value("Chưa chọn nhóm kỹ năng"));
        asAdmin(putJson("/api/admin/skills/" + skillId("Python"),
                        "{\"name\": \"Python\", \"categoryId\": " + categoryId("Lập trình") + "}"))
                .andExpect(jsonPath("$.errors[0].message").value("Thiếu trạng thái hiện/ẩn"));
        asAdmin(postJson("/api/admin/skill-relations", "{\"skillAId\": " + skillId("Java") + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("skillBId"))
                .andExpect(jsonPath("$.errors[0].message").value("Chưa chọn kỹ năng"));
    }

    @Test
    @DisplayName("D74 DELETE cặp không có, kể cả hai mã bằng nhau hay mã không phải số: 404, 404, 400")
    void d74_deleteMissingRelation() throws Exception {
        long java = skillId("Java");
        long sql = skillId("SQL");

        asAdmin(delete("/api/admin/skill-relations/" + java + "/" + sql))
                .andExpect(status().isNotFound());
        asAdmin(delete("/api/admin/skill-relations/" + java + "/" + java))
                .andExpect(status().isNotFound());
        asAdmin(delete("/api/admin/skill-relations/abc/" + java))
                .andExpect(status().isBadRequest());

        assertThat(relations()).hasSize(9);
    }

    @Test
    @DisplayName("D73 không có API xóa kỹ năng hay nhóm: DELETE trả 405")
    void d73_noDeleteForSkillsOrCategories() throws Exception {
        asAdmin(delete("/api/admin/skills/" + skillId("Java")))
                .andExpect(status().isMethodNotAllowed());
        asAdmin(delete("/api/admin/skill-categories/" + categoryId("Lập trình")))
                .andExpect(status().isMethodNotAllowed());

        assertThat(adminSkills()).hasSize(26);
    }

    /**
     * Nhiều request thêm cùng một tên chạy song song: service đều kiểm "chưa có" rồi cùng INSERT,
     * request sau vấp UNIQUE của DB. Kết quả mong đợi luôn là một 201, còn lại 409, không 500.
     * Test không ép được lần nào cũng xảy ra tranh chấp (tùy lịch chạy luồng), nhưng mong đợi đúng trong cả hai trường hợp.
     */
    @Test
    @DisplayName("D74 năm request cùng lúc thêm kỹ năng \"Rust\": một 201, bốn 409, không có 500")
    void d74_concurrentCreateGivesConflictNotServerError() throws Exception {
        long programming = categoryId("Lập trình");
        int threads = 5;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Callable<Integer> call = () -> {
                    start.await(); // mọi luồng cùng xuất phát
                    return asAdmin(postJson("/api/admin/skills", createSkillJson("Rust", programming)))
                            .andReturn().getResponse().getStatus();
                };
                results.add(pool.submit(call));
            }
            start.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get());
            }
            assertThat(statuses).containsExactlyInAnyOrder(201, 409, 409, 409, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM skills WHERE name = 'Rust'", Integer.class))
                .isEqualTo(1);
    }
}
