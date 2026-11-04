package com.skillexchange.matching;

import java.util.List;

/** Giai đoạn 1 của matching: loại ứng viên vi phạm một trong năm điều kiện cứng (spec 4.2, D43). */
public class HardFilter {

    private final ScoreCalculator scores;

    public HardFilter(ScoreCalculator scores) {
        this.scores = scores;
    }

    /** Bước 6. Ứng viên có được đưa vào danh sách tính điểm cho caller không. */
    public boolean accepts(MatchProfile caller, MatchProfile candidate) {
        throw new UnsupportedOperationException("Bước 6");
    }

    /** Bước 6. Giữ lại các ứng viên qua được lọc cứng, đúng thứ tự ban đầu. */
    public List<MatchProfile> filter(MatchProfile caller, List<MatchProfile> candidates) {
        throw new UnsupportedOperationException("Bước 6");
    }
}
