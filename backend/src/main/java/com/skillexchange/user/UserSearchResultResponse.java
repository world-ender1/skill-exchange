package com.skillexchange.user;

import java.util.List;

/** Một người trong kết quả GET /api/users/search (D45d): chỉ bốn trường, không có thông tin liên hệ. */
public record UserSearchResultResponse(
        Long id,
        String fullName,
        LearningMode learningMode,
        List<PublicSkillResponse> teachSkills) {
}
