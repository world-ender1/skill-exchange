package com.skillexchange.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * API "của tôi" (TK 4.3). Quyền User: mọi người đã đăng nhập, kể cả PENDING_VERIFICATION (D12) và admin.
 * SecurityConfig đã chặn người chưa đăng nhập (401) và tài khoản bị khóa (403) trước khi tới đây.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserProfileService userProfileService;

    public MeController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    // @AuthenticationPrincipal: Spring đưa vào chiếc vé (Jwt) đã được kiểm chữ ký ở tầng filter.
    @GetMapping
    public MyProfileResponse getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return userProfileService.getMyProfile(currentUserId(jwt));
    }

    @PutMapping
    public MyProfileResponse updateMyProfile(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody UpdateProfileRequest request) {
        return userProfileService.updateMyProfile(currentUserId(jwt), request);
    }

    /**
     * "Tôi là ai" = claim sub của vé. Vé có chữ ký, sửa sub thì chữ ký sai → 401 (TC63).
     * Không bao giờ lấy id từ body hay URL: đó là dữ liệu người gọi tự điền, muốn ghi gì cũng được.
     */
    private static Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
