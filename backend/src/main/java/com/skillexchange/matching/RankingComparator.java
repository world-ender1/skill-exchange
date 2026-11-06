package com.skillexchange.matching;

import java.util.Comparator;

/**
 * Thứ tự xếp hạng duy nhất của dự án (D36): điểm cao trước; chênh dưới 1e-9 thì coi là bằng
 * và xếp theo userId tăng dần. Dùng cho cả hai nhóm, và cho B0, B1 ở tuần 5.
 * Hạn chế đã biết: không có tính bắc cầu (D48).
 */
public class RankingComparator implements Comparator<MatchResult> {

    public static final double EPSILON = 1e-9;

    @Override
    public int compare(MatchResult a, MatchResult b) {
        // Chênh từ 1e-9 trở lên: điểm cao đứng trước, nên so b với a (đảo chiều so với tăng dần).
        if (Math.abs(a.score() - b.score()) >= EPSILON) {
            return Double.compare(b.score(), a.score());
        }
        // Coi là bằng điểm (spec 4.4): userId tăng dần để kết quả ổn định, tái lập được.
        return Long.compare(a.userId(), b.userId());
    }
}
