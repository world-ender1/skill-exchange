package com.skillexchange.matching;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Làm tròn số thực trong response /api/matches: 3 chữ số, HALF_UP (TK 4.5, kế hoạch tuần 4 bước 8).
 * Chỉ dùng khi đổi sang DTO; xếp hạng luôn dùng điểm chưa làm tròn (spec 4.4).
 */
final class Rounding {

    private Rounding() {
    }

    static double round3(double value) {
        // BigDecimal.valueOf đi qua Double.toString (số ngắn nhất biểu diễn đúng double),
        // nên 0.6875 được làm tròn như số thập phân 0.6875 → 0.688, không bị lệch nhị phân.
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }
}
