package com.skillexchange.user;

/**
 * Một kỹ năng dạy trả về (D28). Có tên, nhóm và isActive vì kỹ năng đã bị ẩn
 * không còn trong GET /api/skills, frontend không tự tra tên được.
 * Repository tạo thẳng record này bằng "SELECT new ...", nên thứ tự và kiểu tham số phải khớp câu query.
 */
public record TeachSkillResponse(
        Long skillId,
        String skillName,
        Long categoryId,
        String categoryName,
        boolean isActive,
        Short level) {
}
