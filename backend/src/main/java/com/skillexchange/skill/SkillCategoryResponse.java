package com.skillexchange.skill;

/** Một phần tử của GET /api/skill-categories: {id, name} (D40). */
public record SkillCategoryResponse(Long id, String name) {

    static SkillCategoryResponse from(SkillCategory category) {
        return new SkillCategoryResponse(category.getId(), category.getName());
    }
}
