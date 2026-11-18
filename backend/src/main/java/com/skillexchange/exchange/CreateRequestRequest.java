package com.skillexchange.exchange;

import java.time.Instant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body của POST /api/requests (D89, bảng 2.1). Các annotation ở đây là bước (1) của D89: Spring kiểm hết
 * cùng lúc và trả mọi lỗi trong errors[]. Bước (2)–(8) cần DB hoặc đồng hồ nên nằm ở ExchangeRequestService.
 *
 * Không có senderId: người gửi luôn là chủ của vé (controller), Jackson bỏ qua khóa lạ trong JSON.
 * type, mode là enum: giá trị lạ ("ANY") làm Jackson không đọc được body → 400 "Nội dung JSON không đọc được".
 * proposedStartAt là Instant: Jackson nhận chuỗi ISO-8601 có múi giờ ("...+07:00" hay "...Z"); thiếu múi giờ
 * thì không biết là giờ nào nên cũng là JSON không đọc được (D88).
 */
public record CreateRequestRequest(

        @NotNull(message = "Chưa chọn người nhận")
        Long receiverId,

        @NotNull(message = "Chưa chọn loại yêu cầu")
        RequestType type,

        @NotNull(message = "Chưa chọn kỹ năng bạn muốn học")
        Long senderLearnSkillId,

        // Bắt buộc hay cấm tùy type: bước (2) ở service, vì một annotation trên trường không thấy được trường khác.
        Long receiverLearnSkillId,

        @NotNull(message = "Chưa chọn thời điểm")
        Instant proposedStartAt,

        // Integer chứ không Short (cột SMALLINT): 40000 vẫn đọc được để báo lỗi theo ô, thay vì tràn Short.
        // @Min, @Max bỏ qua null, nên thiếu trường chỉ ra một lỗi (của @NotNull).
        @NotNull(message = "Thời lượng từ 15 đến 480 phút")
        @Min(value = 15, message = "Thời lượng từ 15 đến 480 phút")
        @Max(value = 480, message = "Thời lượng từ 15 đến 480 phút")
        Integer durationMinutes,

        @NotNull(message = "Chưa chọn hình thức")
        MeetingMode mode,

        @NotBlank(message = "Chưa nhập địa điểm hoặc link")
        @Size(max = 500, message = "Địa điểm hoặc link dài tối đa 500 ký tự")
        String locationOrLink,

        @Size(max = 1000, message = "Lời nhắn dài tối đa 1000 ký tự")
        String message) {

    /** Cắt khoảng trắng hai đầu trước khi @Valid kiểm, nên độ dài đếm sau khi cắt (D88). */
    public CreateRequestRequest {
        locationOrLink = locationOrLink == null ? null : locationOrLink.strip();
        message = stripToNull(message);
    }

    /** Lời nhắn không bắt buộc: rỗng sau khi cắt thì lưu NULL (D88). */
    private static String stripToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
