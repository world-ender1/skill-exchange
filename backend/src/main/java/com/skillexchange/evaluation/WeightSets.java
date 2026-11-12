package com.skillexchange.evaluation;

import java.util.List;

import com.skillexchange.matching.MatchingWeights;

/**
 * Các bộ trọng số của thực nghiệm, mỗi bộ mang tên phuong_an dùng trong file kết quả (D72).
 * Thứ tự năm số: SkillFit, LevelFit, ScheduleFit, ModeFit, Reputation (như MatchingWeights).
 *
 * Mọi bộ đi qua constructor của MatchingWeights, nên số sai (âm, tổng khác 1) làm lớp này
 * không nạp được ngay khi chạy, không đợi tới lúc tính điểm.
 */
public final class WeightSets {

    /** Một bộ trọng số và tên của nó trong cột phuong_an. */
    public record Named(String phuongAn, MatchingWeights weights) {
    }

    // ---------- Độ nhạy (spec 5.4, D67) ----------

    /** Chính bộ mặc định của hệ thống, không chép lại số: P ở thực nghiệm là P đang chạy (D56). */
    public static final Named MAC_DINH = new Named("P", MatchingWeights.DEFAULT);
    public static final Named KY_NANG = new Named("P_w_ky_nang", new MatchingWeights(0.65, 0.15, 0.10, 0.05, 0.05));
    public static final Named LICH = new Named("P_w_lich", new MatchingWeights(0.50, 0.15, 0.25, 0.05, 0.05));
    /** Mọi người giả lập có Reputation 0.5, nên bộ này chỉ giảm tỷ trọng các thành phần khác (D67). */
    public static final Named UY_TIN = new Named("P_w_uy_tin", new MatchingWeights(0.45, 0.15, 0.15, 0.05, 0.20));

    // ---------- Bỏ từng thành phần (spec 5.2, D66) ----------
    // Trọng số bị bỏ chia đều cho 4 thành phần còn lại, kể cả Reputation. Ghi số như D66 để đối chiếu
    // bằng mắt; WeightSetsTest kiểm lại các số này bằng quy tắc chia đều từ bộ mặc định.

    public static final Named BO_LEVEL = new Named("P_bo_level", new MatchingWeights(0.60, 0, 0.20, 0.10, 0.10));
    public static final Named BO_SCHEDULE = new Named("P_bo_schedule", new MatchingWeights(0.5875, 0.2375, 0, 0.0875, 0.0875));
    public static final Named BO_MODE = new Named("P_bo_mode", new MatchingWeights(0.5625, 0.2125, 0.1625, 0, 0.0625));

    /** Bốn bộ độ nhạy, bộ mặc định đầu tiên. */
    public static final List<Named> SENSITIVITY = List.of(MAC_DINH, KY_NANG, LICH, UY_TIN);
    /** Ba bộ bỏ thành phần, theo thứ tự LevelFit, ScheduleFit, ModeFit. */
    public static final List<Named> ABLATION = List.of(BO_LEVEL, BO_SCHEDULE, BO_MODE);

    private WeightSets() {
    }
}
