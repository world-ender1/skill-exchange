package com.skillexchange.user;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * API "của tôi" (TK 4.3). Quyền User: mọi người đã đăng nhập, kể cả PENDING_VERIFICATION (D12) và admin.
 * SecurityConfig đã chặn người chưa đăng nhập (401) và tài khoản bị khóa (403) trước khi tới đây.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    /** Giới hạn của D21. */
    private static final int MAX_SKILLS = 10;
    private static final int MAX_SLOTS = 21;

    private final UserProfileService userProfileService;
    private final AvailabilityService availabilityService;
    private final SkillProfileService skillProfileService;

    public MeController(UserProfileService userProfileService,
                        AvailabilityService availabilityService,
                        SkillProfileService skillProfileService) {
        this.userProfileService = userProfileService;
        this.availabilityService = availabilityService;
        this.skillProfileService = skillProfileService;
    }

    // ---------- Hồ sơ (bước 8) ----------

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

    // ---------- Lịch rảnh, kỹ năng dạy, kỹ năng muốn học (bước 9) ----------
    // Body là mảng JSON. Annotation đặt trên kiểu phần tử (List<@NotNull @Valid X>) nghĩa là
    // "kiểm từng phần tử"; @Size trên tham số là "kiểm cả danh sách" (D21).
    // Tên tham số (availability, teachSkills, learnSkills) hiện ra trong errors[].field khi quá số lượng.

    @GetMapping("/availability")
    public List<AvailabilitySlotResponse> getAvailability(@AuthenticationPrincipal Jwt jwt) {
        return availabilityService.getAvailability(currentUserId(jwt));
    }

    @PutMapping("/availability")
    public List<AvailabilitySlotResponse> replaceAvailability(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody
            @Size(max = MAX_SLOTS, message = "Tối đa 21 khung giờ")
            List<@NotNull(message = "Phần tử không được null") @Valid AvailabilitySlotRequest> availability) {
        return availabilityService.replaceAvailability(currentUserId(jwt), availability);
    }

    @GetMapping("/teach-skills")
    public List<TeachSkillResponse> getTeachSkills(@AuthenticationPrincipal Jwt jwt) {
        return skillProfileService.getTeachSkills(currentUserId(jwt));
    }

    @PutMapping("/teach-skills")
    public List<TeachSkillResponse> replaceTeachSkills(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody
            @Size(max = MAX_SKILLS, message = "Tối đa 10 kỹ năng dạy")
            List<@NotNull(message = "Phần tử không được null") @Valid TeachSkillRequest> teachSkills) {
        return skillProfileService.replaceTeachSkills(currentUserId(jwt), teachSkills);
    }

    @GetMapping("/learn-skills")
    public List<LearnSkillResponse> getLearnSkills(@AuthenticationPrincipal Jwt jwt) {
        return skillProfileService.getLearnSkills(currentUserId(jwt));
    }

    @PutMapping("/learn-skills")
    public List<LearnSkillResponse> replaceLearnSkills(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody
            @Size(max = MAX_SKILLS, message = "Tối đa 10 kỹ năng muốn học")
            List<@NotNull(message = "Phần tử không được null") @Valid LearnSkillRequest> learnSkills) {
        return skillProfileService.replaceLearnSkills(currentUserId(jwt), learnSkills);
    }

    /**
     * "Tôi là ai" = claim sub của vé. Vé có chữ ký, sửa sub thì chữ ký sai → 401 (TC63).
     * Không bao giờ lấy id từ body hay URL: đó là dữ liệu người gọi tự điền, muốn ghi gì cũng được.
     */
    private static Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
