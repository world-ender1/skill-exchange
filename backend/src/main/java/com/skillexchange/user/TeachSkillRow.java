package com.skillexchange.user;

/**
 * Một dòng kỹ năng dạy kèm userId, để nạp kỹ năng của nhiều người trong một câu rồi gom theo người.
 * Repository tạo thẳng record này bằng "SELECT new ...".
 */
public record TeachSkillRow(Long userId, Long skillId, String skillName, String categoryName, Short level) {

    PublicSkillResponse toResponse() {
        return new PublicSkillResponse(skillId, skillName, categoryName, level);
    }
}
