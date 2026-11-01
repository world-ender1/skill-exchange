package com.skillexchange.user;

/**
 * Hồ sơ CỦA TÔI (D29): có email, phone, contactInfo vì chỉ chủ tài khoản nhận được response này.
 * Hồ sơ công khai tuần 4 phải dùng DTO khác, không có các trường riêng tư này (NFR-02, D17).
 * Không có passwordHash: DTO chỉ chứa đúng những gì được phép trả về (TC73).
 */
public record MyProfileResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        UserStatus status,
        String bio,
        LearningMode learningMode,
        String phone,
        String contactInfo) {

    static MyProfileResponse from(User user) {
        return new MyProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                user.getBio(),
                user.getLearningMode(),
                user.getPhone(),
                user.getContactInfo());
    }
}
