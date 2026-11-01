package com.skillexchange.user;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillexchange.common.FieldValidationException;

/** Lịch rảnh của tôi (TK 4.3, D21, D28, D41). userId luôn lấy từ vé (MeController). */
@Service
public class AvailabilityService {

    /** D41: theo ngày, rồi theo buổi trong ngày (thứ tự khai báo của enum Period, không theo chữ cái). */
    private static final Comparator<UserAvailability> DAY_THEN_PERIOD =
            Comparator.comparing(UserAvailability::getDayOfWeek)
                    .thenComparing(UserAvailability::getPeriod);

    private final UserAvailabilityRepository availabilityRepository;

    public AvailabilityService(UserAvailabilityRepository availabilityRepository) {
        this.availabilityRepository = availabilityRepository;
    }

    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponse> getAvailability(Long userId) {
        return availabilityRepository.findByUserId(userId).stream()
                .sorted(DAY_THEN_PERIOD)
                .map(AvailabilitySlotResponse::from)
                .toList();
    }

    /**
     * Thay cả lịch (D30): kiểm xong mới xóa rồi chèn, tất cả trong MỘT transaction.
     * Nếu lỗi giữa chừng (ví dụ chèn thất bại), transaction rollback: lịch cũ còn nguyên,
     * không bao giờ có trạng thái "đã xóa lịch cũ mà chưa có lịch mới".
     */
    @Transactional
    public List<AvailabilitySlotResponse> replaceAvailability(Long userId, List<AvailabilitySlotRequest> slots) {
        checkNoDuplicateSlots(slots);

        availabilityRepository.deleteAllByUserId(userId);
        availabilityRepository.saveAll(slots.stream()
                .map(slot -> new UserAvailability(userId, slot.dayOfWeek().shortValue(), slot.periodValue()))
                .toList());

        // Đọc lại từ DB để PUT trả đúng cùng dạng, cùng thứ tự với GET (D29, D41).
        return getAvailability(userId);
    }

    /** D21: trùng khung giờ trong cùng request → 400, báo ở lần xuất hiện thứ hai. */
    private static void checkNoDuplicateSlots(List<AvailabilitySlotRequest> slots) {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < slots.size(); i++) {
            AvailabilitySlotRequest slot = slots.get(i);
            if (!seen.add(slot.dayOfWeek() + "-" + slot.period())) {
                throw new FieldValidationException("[" + i + "]", "Khung giờ này bị gửi trùng");
            }
        }
    }
}
