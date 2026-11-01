package com.skillexchange.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body của PUT /api/me (D15, D16). PUT thay TOÀN BỘ hồ sơ: không gửi một trường = xóa trường đó.
 *
 * Cố ý không có id, email, role, status: Jackson bỏ qua khóa lạ trong JSON, nên gửi kèm
 * "email" hay "id" của người khác cũng không có tác dụng (TC48, TC66).
 * Người được sửa luôn là chủ của vé (MeController), không phải ai đó do body chỉ định.
 */
public record UpdateProfileRequest(

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 100, message = "Họ tên dài tối đa 100 ký tự")
        String fullName,

        @Size(max = 1000, message = "Giới thiệu dài tối đa 1000 ký tự")
        String bio,

        // Nhận chuỗi thay vì enum: giá trị sai (ví dụ "BOTH") thành lỗi theo trường learningMode,
        // không thành lỗi "JSON không đọc được" chung chung. @Pattern bỏ qua null (= chưa khai báo).
        @Pattern(regexp = "ONLINE|OFFLINE|ANY", message = "Hình thức học phải là ONLINE, OFFLINE hoặc ANY")
        String learningMode,

        @Size(max = 20, message = "Số điện thoại dài tối đa 20 ký tự")
        @Pattern(regexp = "[0-9 +\\-]*", message = "Số điện thoại chỉ gồm chữ số, khoảng trắng, + và -")
        String phone,

        @Size(max = 500, message = "Thông tin liên hệ dài tối đa 500 ký tự")
        String contactInfo) {

    /** Chuẩn hóa trước khi @Valid kiểm: bỏ khoảng trắng hai đầu; trường tùy chọn còn rỗng thì thành null (D15). */
    public UpdateProfileRequest {
        fullName = fullName == null ? null : fullName.strip();
        bio = stripToNull(bio);
        learningMode = stripToNull(learningMode);
        phone = stripToNull(phone);
        contactInfo = stripToNull(contactInfo);
    }

    private static String stripToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    /** Đã qua @Pattern nên chắc chắn là một trong ba giá trị (hoặc null). */
    LearningMode learningModeValue() {
        return learningMode == null ? null : LearningMode.valueOf(learningMode);
    }
}
