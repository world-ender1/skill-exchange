package com.skillexchange.user;

/** Một kỹ năng muốn học trả về (D28): như TeachSkillResponse, với targetLevel có thể null. */
public record LearnSkillResponse(
        Long skillId,
        String skillName,
        Long categoryId,
        String categoryName,
        boolean isActive,
        Short targetLevel) {
}
