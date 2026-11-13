package com.skillexchange.admin;

import com.skillexchange.skill.Skill;
import com.skillexchange.skill.SkillCategory;

/**
 * Một kỹ năng ở màn hình quản trị: {id, name, categoryId, categoryName, isActive} (D73).
 * Khác SkillResponse của /api/skills ở trường isActive, vì admin thấy cả kỹ năng bị ẩn để hiện lại được.
 */
public record AdminSkillResponse(Long id, String name, Long categoryId, String categoryName, boolean isActive) {

    /** Gọi trong transaction: đọc skill.getCategory() (LAZY) ở ngoài transaction sẽ lỗi. */
    static AdminSkillResponse from(Skill skill) {
        SkillCategory category = skill.getCategory();
        return new AdminSkillResponse(skill.getId(), skill.getName(),
                category.getId(), category.getName(), skill.isActive());
    }
}
