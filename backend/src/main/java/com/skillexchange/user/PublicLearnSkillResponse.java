package com.skillexchange.user;

/**
 * Một kỹ năng muốn học trong hồ sơ công khai (TK 4.3). targetLevel = null nghĩa là "không đặt mục tiêu",
 * nên key luôn có mặt (không dùng @JsonInclude(NON_NULL) như SkillMatchResponse).
 */
public record PublicLearnSkillResponse(Long skillId, String skillName, String categoryName, Short targetLevel) {

    static PublicLearnSkillResponse from(LearnSkillResponse skill) {
        return new PublicLearnSkillResponse(skill.skillId(), skill.skillName(), skill.categoryName(), skill.targetLevel());
    }
}
