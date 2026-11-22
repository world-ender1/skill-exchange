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

    // ---------- Bỏ từng thành phần, chia lại theo tỉ lệ (D98, bổ sung theo nhận xét báo cáo tuần 5) ----------
    // Mỗi trọng số còn lại chia cho (1 − trọng số bị bỏ), nên tỷ lệ giữa các thành phần còn lại giữ nguyên như
    // bộ mặc định. Hiệu đo được khi so với P chỉ còn một thay đổi: bỏ thành phần đó (cách chia đều lẫn hai thay đổi).

    public static final Named BO_LEVEL_TI_LE = proportionalWithout("P_bo_level_tl", 1);
    public static final Named BO_SCHEDULE_TI_LE = proportionalWithout("P_bo_schedule_tl", 2);
    public static final Named BO_MODE_TI_LE = proportionalWithout("P_bo_mode_tl", 3);

    /** Ba bộ bỏ thành phần chia theo tỉ lệ, cùng thứ tự với ABLATION. */
    public static final List<Named> ABLATION_PROPORTIONAL = List.of(BO_LEVEL_TI_LE, BO_SCHEDULE_TI_LE, BO_MODE_TI_LE);

    /**
     * Bộ mặc định bỏ thành phần thứ index (0 = SkillFit, ..., 4 = Reputation), chia lại theo tỉ lệ.
     * Tính từ MatchingWeights.DEFAULT chứ không chép số, để đổi bộ mặc định thì bộ này đổi theo.
     */
    static Named proportionalWithout(String phuongAn, int index) {
        MatchingWeights d = MatchingWeights.DEFAULT;
        double[] w = {d.skillFit(), d.levelFit(), d.scheduleFit(), d.modeFit(), d.reputation()};
        double rest = 1 - w[index];
        for (int i = 0; i < w.length; i++) {
            w[i] = i == index ? 0 : w[i] / rest;
        }
        return new Named(phuongAn, new MatchingWeights(w[0], w[1], w[2], w[3], w[4]));
    }

    private WeightSets() {
    }
}
