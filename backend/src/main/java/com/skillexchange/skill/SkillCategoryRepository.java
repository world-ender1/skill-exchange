package com.skillexchange.skill;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkillCategoryRepository extends JpaRepository<SkillCategory, Long> {

    // Spring Data tự sinh câu SQL từ tên method: ... ORDER BY name ASC, id ASC (D40).
    List<SkillCategory> findAllByOrderByNameAscIdAsc();

    /**
     * Số nhóm KHÁC exceptId có cùng tên, so không phân biệt hoa thường (D74).
     * exceptId = null khi thêm mới; khi đổi tên thì bỏ qua chính nhóm đó, nên "Lập trình" → "LẬP TRÌNH" được.
     * lower() của PostgreSQL đổi được chữ có dấu vì DB tạo với LC_CTYPE en_US.utf8.
     */
    @Query("""
            SELECT COUNT(c) FROM SkillCategory c
            WHERE LOWER(c.name) = LOWER(:name)
              AND (:exceptId IS NULL OR c.id <> :exceptId)
            """)
    long countSameName(@Param("name") String name, @Param("exceptId") Long exceptId);
}
