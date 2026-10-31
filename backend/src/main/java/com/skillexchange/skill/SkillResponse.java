package com.skillexchange.skill;

/** Một phần tử của GET /api/skills: {id, name, categoryId, categoryName} (D40). */
public record SkillResponse(Long id, String name, Long categoryId, String categoryName) {

    static SkillResponse from(Skill skill) {
        SkillCategory category = skill.getCategory();
        return new SkillResponse(skill.getId(), skill.getName(), category.getId(), category.getName());
    }
}
