package com.skillexchange.skill;

/** Một phần tử của GET /api/skill-categories: {id, name} (D40). Cũng là response thêm/đổi tên nhóm (D73). */
public record SkillCategoryResponse(Long id, String name) {

    public static SkillCategoryResponse from(SkillCategory category) {
        return new SkillCategoryResponse(category.getId(), category.getName());
    }
}
