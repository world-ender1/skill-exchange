package com.skillexchange.matching;

import java.util.List;

import com.skillexchange.user.LearningMode;
import com.skillexchange.user.Role;
import com.skillexchange.user.UserStatus;

/** Giai đoạn 1 của matching: loại ứng viên vi phạm một trong năm điều kiện cứng (spec 4.2, D43). */
public class HardFilter {

    private final ScoreCalculator scores;

    public HardFilter(ScoreCalculator scores) {
        this.scores = scores;
    }

    /** Bước 6. Ứng viên có được đưa vào danh sách tính điểm cho caller không. */
    public boolean accepts(MatchProfile caller, MatchProfile candidate) {
        return isSomeoneElse(caller, candidate)
                && isActiveStudent(candidate)
                && hasMatchingSkill(caller, candidate)
                && schedulesCompatible(caller, candidate)
                && modesCompatible(caller, candidate);
    }

    /** Bước 6. Giữ lại các ứng viên qua được lọc cứng, đúng thứ tự ban đầu. */
    public List<MatchProfile> filter(MatchProfile caller, List<MatchProfile> candidates) {
        // stream().filter giữ nguyên thứ tự của danh sách gốc.
        return candidates.stream()
                .filter(candidate -> accepts(caller, candidate))
                .toList();
    }

    /** (1) v ≠ u. */
    private static boolean isSomeoneElse(MatchProfile caller, MatchProfile candidate) {
        return candidate.userId() != caller.userId();
    }

    /** (2) ACTIVE và STUDENT. Truy vấn ở vỏ cũng lọc sẵn, nhưng quy tắc chính thức nằm ở đây (D43). */
    private static boolean isActiveStudent(MatchProfile candidate) {
        return candidate.status() == UserStatus.ACTIVE && candidate.role() == Role.STUDENT;
    }

    /** (3) SkillFit(u, v) > 0, chỉ chiều u học từ v: v không muốn học gì của u vẫn là gợi ý một chiều. */
    private boolean hasMatchingSkill(MatchProfile caller, MatchProfile candidate) {
        // Dùng lại hàm điểm để lọc và chấm điểm không thể lệch nhau.
        return scores.skillFit(caller.learn(), candidate.teach()) > 0;
    }

    /** (4) Chỉ loại khi CẢ HAI đã khai lịch mà không trùng khung nào; một bên chưa khai thì giữ. */
    private static boolean schedulesCompatible(MatchProfile caller, MatchProfile candidate) {
        if (caller.slots().isEmpty() || candidate.slots().isEmpty()) {
            return true;
        }
        for (Slot slot : caller.slots()) {
            if (candidate.slots().contains(slot)) {
                return true;
            }
        }
        return false;
    }

    /**
     * (5) Chỉ loại khi một bên ONLINE, bên kia OFFLINE. Không gọi scores.modeFit, vì hàm đó ném lỗi
     * đúng với cặp này (bước 4): lọc cứng phải tự nhận ra cặp xung đột trước khi tới hàm điểm.
     */
    private static boolean modesCompatible(MatchProfile caller, MatchProfile candidate) {
        LearningMode a = caller.mode();
        LearningMode b = candidate.mode();
        boolean conflict = (a == LearningMode.ONLINE && b == LearningMode.OFFLINE)
                || (a == LearningMode.OFFLINE && b == LearningMode.ONLINE);
        return !conflict;
    }
}
