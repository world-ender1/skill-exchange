package com.skillexchange.matching;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.skillexchange.user.LearningMode;
import com.skillexchange.user.Role;
import com.skillexchange.user.UserStatus;

/**
 * Hồ sơ một người dùng dưới dạng lõi matching cần (D34, D43). Không phải Entity:
 * lõi không biết tới JPA, nên module evaluation (tuần 5) dựng được hồ sơ giả lập mà không cần DB.
 *
 * - status, role: để HardFilter tự loại người không phải ACTIVE + STUDENT (D43).
 * - slots rỗng = chưa khai báo lịch; mode null = chưa khai báo hình thức học (spec 4.3).
 * - reputation: đã tính sẵn bằng ReputationCalculator.
 */
public record MatchProfile(
        long userId,
        UserStatus status,
        Role role,
        List<LearnItem> learn,
        List<TeachItem> teach,
        Set<Slot> slots,
        LearningMode mode,
        double reputation) {

    public MatchProfile {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(role, "role");
        // Chép sang danh sách không sửa được: record bất biến thật sự, bên ngoài sửa list gốc cũng không ảnh hưởng.
        learn = List.copyOf(learn);
        teach = List.copyOf(teach);
        slots = Set.copyOf(slots);
    }
}
