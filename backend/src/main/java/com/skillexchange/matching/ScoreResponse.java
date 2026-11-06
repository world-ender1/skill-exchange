package com.skillexchange.matching;

/** Các thành phần điểm của một chiều, đã làm tròn 3 chữ số (TK 4.5: forward, backward). */
public record ScoreResponse(
        double d,
        double skillFit,
        double levelFit,
        double scheduleFit,
        double modeFit,
        double reputation) {

    static ScoreResponse from(ScoreBreakdown breakdown) {
        return new ScoreResponse(
                Rounding.round3(breakdown.d()),
                Rounding.round3(breakdown.skillFit()),
                Rounding.round3(breakdown.levelFit()),
                Rounding.round3(breakdown.scheduleFit()),
                Rounding.round3(breakdown.modeFit()),
                Rounding.round3(breakdown.reputation()));
    }
}
