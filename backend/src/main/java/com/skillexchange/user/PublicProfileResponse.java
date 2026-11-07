package com.skillexchange.user;

import java.util.List;

/**
 * Hồ sơ công khai GET /api/users/{id} (TK 4.3). Record RIÊNG, không dùng chung với MyProfileResponse:
 * thêm một trường vào hồ sơ "của tôi" (ví dụ phone) không thể vô tình lộ ra đây (NFR-02, TC76).
 */
public record PublicProfileResponse(
        Long id,
        String fullName,
        String bio,
        LearningMode learningMode,
        List<PublicSkillResponse> teachSkills,
        List<PublicLearnSkillResponse> learnSkills,
        List<AvailabilitySlotResponse> availability) {
}
