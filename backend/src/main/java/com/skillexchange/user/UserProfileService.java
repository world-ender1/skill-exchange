package com.skillexchange.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillexchange.common.UnauthorizedException;

/**
 * Xem và sửa hồ sơ của tôi (D15, D29). Nhận userId đã lấy từ vé, không bao giờ từ body,
 * nên không có cách nào sửa hồ sơ của người khác qua đây (TC66).
 */
@Service
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(Long userId) {
        return MyProfileResponse.from(findUser(userId));
    }

    @Transactional
    public MyProfileResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        // Entity đang được quản lý trong transaction: đổi field là đủ, Hibernate tự UPDATE khi commit
        // và @UpdateTimestamp tự ghi updated_at (D18).
        user.updateProfile(request.fullName(), request.bio(), request.learningModeValue(),
                request.phone(), request.contactInfo());
        return MyProfileResponse.from(user);
    }

    private User findUser(Long userId) {
        // AccountStatusFilter đã kiểm user còn tồn tại; đây chỉ là phòng hờ user bị xóa giữa chừng.
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Tài khoản không còn tồn tại"));
    }
}
