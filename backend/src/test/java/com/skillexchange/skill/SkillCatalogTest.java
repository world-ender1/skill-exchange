package com.skillexchange.skill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.skillexchange.IntegrationTestBase;

/**
 * Danh mục kỹ năng: TC12–TC15, TC92 (D3, D40).
 * Chưa có JWT nên dùng @WithMockUser để giả một người đã đăng nhập.
 */
@WithMockUser
class SkillCatalogTest extends IntegrationTestBase {

    private static final Set<String> CATEGORY_KEYS = Set.of("id", "name");
    private static final Set<String> SKILL_KEYS = Set.of("id", "name", "categoryId", "categoryName");

    // ---------- Hàm tiện ích ----------

    /** Gọi API, kiểm tra 200, trả về mảng JSON dạng danh sách Map. */
    private List<Map<String, Object>> getList(MockHttpServletRequestBuilder request) throws Exception {
        String json = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$");
    }

    private static List<String> names(List<Map<String, Object>> items) {
        return items.stream().map(item -> (String) item.get("name")).toList();
    }

    private long categoryIdOf(String name) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM skill_categories WHERE name = ?", Long.class, name);
    }

    // ---------- Các ca kiểm thử ----------

    @Test
    @DisplayName("TC12 GET /api/skill-categories trả 6 nhóm của danh mục mẫu")
    void tc12_listCategories() throws Exception {
        List<Map<String, Object>> categories = getList(get("/api/skill-categories"));

        assertThat(names(categories)).containsExactlyInAnyOrder(
                "Lập trình", "Dữ liệu", "Thiết kế", "Ngoại ngữ", "Âm nhạc", "Kỹ năng mềm");
    }

    @Test
    @DisplayName("TC13 kỹ năng bị ẩn không có trong GET /api/skills, kỹ năng còn hoạt động đều có")
    void tc13_hiddenSkillNotListed() throws Exception {
        jdbcTemplate.update("UPDATE skills SET is_active = false WHERE name = 'Canva'");
        try {
            List<String> skillNames = names(getList(get("/api/skills")));

            assertThat(skillNames).doesNotContain("Canva");
            List<String> activeNames = jdbcTemplate.queryForList(
                    "SELECT name FROM skills WHERE is_active", String.class);
            assertThat(activeNames).hasSize(25);
            assertThat(skillNames).containsExactlyInAnyOrderElementsOf(activeNames);
        } finally {
            // D39: test đổi danh mục phải tự trả lại như cũ.
            jdbcTemplate.update("UPDATE skills SET is_active = true WHERE name = 'Canva'");
        }
    }

    @Test
    @DisplayName("TC14 GET /api/skills?categoryId=<Lập trình> chỉ trả kỹ năng của nhóm đó")
    void tc14_filterByCategory() throws Exception {
        long programmingId = categoryIdOf("Lập trình");

        List<Map<String, Object>> skills = getList(get("/api/skills").param("categoryId", String.valueOf(programmingId)));

        assertThat(names(skills)).containsExactlyInAnyOrder(
                "Java", "Kotlin", "Python", "JavaScript", "TypeScript", "C++");
        assertThat(skills).allSatisfy(skill -> {
            assertThat(((Number) skill.get("categoryId")).longValue()).isEqualTo(programmingId);
            assertThat(skill.get("categoryName")).isEqualTo("Lập trình");
        });
    }

    @Test
    @DisplayName("TC15 GET /api/skills?q=java tìm chuỗi con, không phân biệt hoa thường")
    void tc15_searchByName() throws Exception {
        assertThat(names(getList(get("/api/skills").param("q", "java"))))
                .containsExactlyInAnyOrder("Java", "JavaScript");
        assertThat(names(getList(get("/api/skills").param("q", "SCRIPT"))))
                .containsExactlyInAnyOrder("JavaScript", "TypeScript");
    }

    @Test
    @DisplayName("TC15 (D40) q rỗng thì không lọc theo tên")
    void tc15_emptyQueryMeansNoFilter() throws Exception {
        assertThat(getList(get("/api/skills").param("q", ""))).hasSize(26);
    }

    @Test
    @DisplayName("TC92 nhóm có đúng khóa id, name; xếp theo tên như ORDER BY name của PostgreSQL")
    void tc92_categoryShapeAndOrder() throws Exception {
        List<Map<String, Object>> categories = getList(get("/api/skill-categories"));

        assertThat(categories).allSatisfy(c -> assertThat(c.keySet()).isEqualTo(CATEGORY_KEYS));
        List<String> expectedOrder = jdbcTemplate.queryForList(
                "SELECT name FROM skill_categories ORDER BY name, id", String.class);
        assertThat(names(categories)).containsExactlyElementsOf(expectedOrder);
    }

    @Test
    @DisplayName("TC92 kỹ năng có đúng khóa id, name, categoryId, categoryName; xếp theo tên")
    void tc92_skillShapeAndOrder() throws Exception {
        List<Map<String, Object>> skills = getList(get("/api/skills"));

        assertThat(skills).allSatisfy(s -> assertThat(s.keySet()).isEqualTo(SKILL_KEYS));

        List<String> actualNames = names(skills);
        List<String> expectedOrder = jdbcTemplate.queryForList(
                "SELECT name FROM skills WHERE is_active ORDER BY name, id", String.class);
        assertThat(actualNames).containsExactlyElementsOf(expectedOrder);

        // So trên các tên không dấu: thứ tự phải giống thứ tự chữ cái thông thường.
        List<String> asciiNames = actualNames.stream()
                .filter(name -> name.chars().allMatch(ch -> ch < 128))
                .toList();
        assertThat(asciiNames).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
        assertThat(actualNames.indexOf("Java")).isLessThan(actualNames.indexOf("Python"));
    }

    @Test
    @DisplayName("TC92 categoryId không tồn tại: 200, danh sách rỗng")
    void tc92_unknownCategoryReturnsEmptyList() throws Exception {
        assertThat(getList(get("/api/skills").param("categoryId", "999999"))).isEmpty();
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Chưa đăng nhập thì không gọi được API danh mục (401 đầy đủ kiểm ở TC62, bước 3)")
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/skills")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/skill-categories")).andExpect(status().isUnauthorized());
    }
}
