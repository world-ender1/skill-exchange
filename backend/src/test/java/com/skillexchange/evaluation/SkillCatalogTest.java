package com.skillexchange.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.LongStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.skillexchange.evaluation.SkillCatalog.Skill;
import com.skillexchange.matching.SkillRelation;

/**
 * Danh mục kỹ năng của module thực nghiệm: TC203 (D63).
 *
 * Danh mục là bản chép tay của V2__seed_skills.sql. Ngoài danh sách cặp chép từ ca kiểm thử,
 * test đọc lại chính file SQL để so: bản chép mà lệch V2 thì test đỏ ngay.
 * Khác com.skillexchange.skill.SkillCatalogTest (tuần 3): test đó gọi API trên DB, test này không dùng DB.
 */
class SkillCatalogTest {

    private final SkillCatalog catalog = SkillCatalog.V2;

    @Test
    @DisplayName("TC203 danh mục có 6 nhóm, 26 kỹ năng, 9 cặp liên quan; mã kỹ năng là 1..26")
    void tc203_counts() {
        assertThat(catalog.categories()).hasSize(6);
        assertThat(catalog.skills()).hasSize(26);
        assertThat(catalog.relations()).hasSize(9);
        assertThat(catalog.skills()).extracting(Skill::id)
                .containsExactlyElementsOf(LongStream.rangeClosed(1, 26).boxed().toList());
    }

    @Test
    @DisplayName("TC203 9 cặp liên quan đúng như ca kiểm thử, hai kỹ năng của mỗi cặp cùng nhóm")
    void tc203_relationPairsInSameCategory() {
        // Cặp không có chiều (SkillSimilarity tra được cả hai chiều), nên so dạng tập hai tên.
        assertThat(relationNamePairs()).containsExactlyInAnyOrder(
                Set.of("Java", "Kotlin"),
                Set.of("JavaScript", "TypeScript"),
                Set.of("SQL", "MySQL"),
                Set.of("Excel", "Power BI"),
                Set.of("Photoshop", "Illustrator"),
                Set.of("Figma", "Canva"),
                Set.of("Guitar", "Ukulele"),
                Set.of("Tiếng Nhật", "Tiếng Trung"),
                Set.of("Tiếng Nhật", "Tiếng Hàn"));

        for (SkillRelation relation : catalog.relations()) {
            assertThat(categoryOf(relation.skillA()))
                    .as("nhóm của cặp %s", relation)
                    .isEqualTo(categoryOf(relation.skillB()));
        }
    }

    @Test
    @DisplayName("TC203 tên nhóm, tên kỹ năng (theo thứ tự mã 1..26) và các cặp khớp V2__seed_skills.sql")
    void tc203_matchesV2SeedFile() {
        String sql = readV2Sql();
        String categorySection = section(sql, "-- ---------- Nhóm kỹ năng ----------", "-- ---------- Kỹ năng ----------");
        String skillSection = section(sql, "-- ---------- Kỹ năng ----------", "-- ---------- Kỹ năng liên quan ----------");
        String relationSection = section(sql, "-- ---------- Kỹ năng liên quan ----------", "-- ---------- Kiểm tra số dòng ----------");

        List<String> v2Categories = new ArrayList<>();
        for (List<String> tuple : tuples(ONE_VALUE, categorySection)) {
            v2Categories.add(tuple.get(0));
        }
        // Mỗi bộ ('nhóm', 'kỹ năng'); thứ tự trong file = thứ tự mã 1..26 (chốt ở kế hoạch bước 1).
        List<List<String>> v2Skills = tuples(TWO_VALUES, skillSection);
        List<Set<String>> v2Relations = new ArrayList<>();
        for (List<String> tuple : tuples(TWO_VALUES, relationSection)) {
            v2Relations.add(Set.of(tuple.get(0), tuple.get(1)));
        }

        // Tự kiểm bộ đọc file: đọc sai thì báo ở đây, không để test so hai danh sách rỗng với nhau.
        assertThat(v2Categories).hasSize(6);
        assertThat(v2Skills).hasSize(26);
        assertThat(v2Relations).hasSize(9);

        assertThat(catalog.categories()).containsExactlyElementsOf(v2Categories);
        assertThat(catalog.skills())
                .extracting(skill -> List.of(skill.category(), skill.name()))
                .containsExactlyElementsOf(v2Skills);
        assertThat(relationNamePairs()).containsExactlyInAnyOrderElementsOf(v2Relations);
    }

    @Test
    @DisplayName("D63 nameOf và idOf tra được hai chiều; mã hoặc tên không có trong danh mục thì báo lỗi")
    void d63_lookupBothWays() {
        assertThat(catalog.nameOf(1)).isEqualTo("Java");
        assertThat(catalog.idOf("Java")).isEqualTo(1);
        assertThat(catalog.nameOf(26)).isEqualTo("Làm việc nhóm");
        assertThat(catalog.idOf("Làm việc nhóm")).isEqualTo(26);
        for (Skill skill : catalog.skills()) {
            assertThat(catalog.idOf(catalog.nameOf(skill.id()))).isEqualTo(skill.id());
        }

        assertThatThrownBy(() -> catalog.nameOf(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog.nameOf(27)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog.idOf("Rust")).isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- Hàm tiện ích ----------

    private List<Set<String>> relationNamePairs() {
        return catalog.relations().stream()
                .map(relation -> Set.of(catalog.nameOf(relation.skillA()), catalog.nameOf(relation.skillB())))
                .toList();
    }

    private String categoryOf(long skillId) {
        return catalog.skills().stream()
                .filter(skill -> skill.id() == skillId)
                .findFirst()
                .orElseThrow()
                .category();
    }

    /** ('Lập trình') */
    private static final Pattern ONE_VALUE = Pattern.compile("\\('([^']+)'\\)");
    /** ('Lập trình',   'Java') */
    private static final Pattern TWO_VALUES = Pattern.compile("\\('([^']+)',\\s*'([^']+)'\\)");

    /** Thư mục resources của main nằm trên classpath khi chạy test, nên đọc được file migration. */
    private static String readV2Sql() {
        try (InputStream in = SkillCatalogTest.class.getResourceAsStream("/db/migration/V2__seed_skills.sql")) {
            assertThat(in).as("không tìm thấy V2__seed_skills.sql trên classpath").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Phần văn bản nằm giữa hai dòng tiêu đề của file SQL. */
    private static String section(String sql, String header, String nextHeader) {
        int start = sql.indexOf(header);
        int end = sql.indexOf(nextHeader, start + 1);
        assertThat(start).as("tiêu đề %s", header).isNotNegative();
        assertThat(end).as("tiêu đề %s", nextHeader).isGreaterThan(start);
        return sql.substring(start + header.length(), end);
    }

    /** Mọi bộ giá trị trong ngoặc khớp mẫu, theo thứ tự xuất hiện. */
    private static List<List<String>> tuples(Pattern pattern, String text) {
        List<List<String>> result = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            List<String> values = new ArrayList<>();
            for (int group = 1; group <= matcher.groupCount(); group++) {
                values.add(matcher.group(group));
            }
            result.add(values);
        }
        return result;
    }
}
