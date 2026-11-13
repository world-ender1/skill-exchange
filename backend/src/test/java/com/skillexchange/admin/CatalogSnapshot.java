package com.skillexchange.admin;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Ảnh chụp danh mục kỹ năng (skill_categories, skills, skill_relations) để test API quản trị
 * trả danh mục về y như cũ sau khi chạy (D39; kế hoạch tuần 5, bước 9).
 *
 * IntegrationTestBase chỉ xóa dữ liệu người dùng; danh mục do V2 nạp được giữ qua mọi test,
 * nên test nào đổi danh mục mà không trả lại sẽ làm hỏng test của lớp khác (ví dụ TC12 đếm 6 nhóm).
 */
final class CatalogSnapshot {

    private record CategoryRow(long id, String name) {
    }

    private record SkillRow(long id, long categoryId, String name, boolean active) {
    }

    private record RelationRow(long skillAId, long skillBId) {
    }

    private final JdbcTemplate jdbcTemplate;
    private final List<CategoryRow> categories;
    private final List<SkillRow> skills;
    private final List<RelationRow> relations;

    private CatalogSnapshot(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.categories = jdbcTemplate.query("SELECT id, name FROM skill_categories",
                (rs, rowNum) -> new CategoryRow(rs.getLong("id"), rs.getString("name")));
        this.skills = jdbcTemplate.query("SELECT id, category_id, name, is_active FROM skills",
                (rs, rowNum) -> new SkillRow(rs.getLong("id"), rs.getLong("category_id"),
                        rs.getString("name"), rs.getBoolean("is_active")));
        this.relations = jdbcTemplate.query("SELECT skill_a_id, skill_b_id FROM skill_relations",
                (rs, rowNum) -> new RelationRow(rs.getLong("skill_a_id"), rs.getLong("skill_b_id")));
    }

    static CatalogSnapshot take(JdbcTemplate jdbcTemplate) {
        return new CatalogSnapshot(jdbcTemplate);
    }

    /**
     * Trả danh mục về đúng ảnh chụp. Thứ tự các bước tránh vi phạm khóa ngoại và UNIQUE:
     * gỡ người dùng (họ có thể đang dạy kỹ năng mới) → gỡ quan hệ → xóa kỹ năng mới →
     * trả kỹ năng cũ về nhóm cũ → xóa nhóm mới (lúc này không còn kỹ năng nào trỏ tới) → trả tên nhóm.
     */
    void restore() {
        jdbcTemplate.execute("TRUNCATE users RESTART IDENTITY CASCADE");
        jdbcTemplate.update("DELETE FROM skill_relations");

        long maxSkillId = skills.stream().mapToLong(SkillRow::id).max().orElse(0);
        long maxCategoryId = categories.stream().mapToLong(CategoryRow::id).max().orElse(0);

        // IDENTITY chỉ tăng, nên dòng do test thêm luôn có id lớn hơn mọi id trong ảnh chụp.
        jdbcTemplate.update("DELETE FROM skills WHERE id > ?", maxSkillId);
        jdbcTemplate.batchUpdate("UPDATE skills SET category_id = ?, name = ?, is_active = ? WHERE id = ?",
                skills, skills.size(), (ps, skill) -> {
                    ps.setLong(1, skill.categoryId());
                    ps.setString(2, skill.name());
                    ps.setBoolean(3, skill.active());
                    ps.setLong(4, skill.id());
                });

        jdbcTemplate.update("DELETE FROM skill_categories WHERE id > ?", maxCategoryId);
        jdbcTemplate.batchUpdate("UPDATE skill_categories SET name = ? WHERE id = ?",
                categories, categories.size(), (ps, category) -> {
                    ps.setString(1, category.name());
                    ps.setLong(2, category.id());
                });

        jdbcTemplate.batchUpdate("INSERT INTO skill_relations (skill_a_id, skill_b_id) VALUES (?, ?)",
                relations, relations.size(), (ps, relation) -> {
                    ps.setLong(1, relation.skillAId());
                    ps.setLong(2, relation.skillBId());
                });

        // Đặt lại bộ đếm để lần chạy sau vẫn sinh id 27, 7... như trên DB mới (không cho id trôi mãi).
        jdbcTemplate.execute("ALTER TABLE skills ALTER COLUMN id RESTART WITH " + (maxSkillId + 1));
        jdbcTemplate.execute("ALTER TABLE skill_categories ALTER COLUMN id RESTART WITH " + (maxCategoryId + 1));
    }
}
