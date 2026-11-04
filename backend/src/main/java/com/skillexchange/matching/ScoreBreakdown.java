package com.skillexchange.matching;

/**
 * Các thành phần điểm của một chiều "learner học từ teacher" và điểm tổng d (spec 4.3, 4.4).
 * Giữ đủ thành phần để giao diện giải thích được điểm (NFR-09, nút "Xem chi tiết điểm").
 */
public record ScoreBreakdown(
        double skillFit,
        double levelFit,
        double scheduleFit,
        double modeFit,
        double reputation,
        double d) {
}
