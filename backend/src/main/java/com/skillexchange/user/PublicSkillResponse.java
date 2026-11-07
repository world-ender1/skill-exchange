package com.skillexchange.user;

/**
 * Một kỹ năng dạy trong hồ sơ công khai và kết quả tìm kiếm (TK 4.3, D45d).
 * Khác TeachSkillResponse của /api/me: không có categoryId, isActive. Người xem không cần biết
 * kỹ năng đã bị ẩn; kỹ năng ẩn vẫn hiện như thường (D46c).
 */
public record PublicSkillResponse(Long skillId, String skillName, String categoryName, Short level) {
}
