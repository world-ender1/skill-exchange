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
        throw new UnsupportedOperationException("Bước 7");
    }
}
