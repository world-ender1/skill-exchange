package com.skillexchange.matching;

/**
 * Bộ trọng số của D(u,v) (spec 4.4). Một chỗ duy nhất chứa trọng số (D35, NFR-05):
 * thực nghiệm độ nhạy ở tuần 5 tạo bộ khác rồi truyền vào cùng lõi.
 */
public record MatchingWeights(
        double skillFit,
        double levelFit,
        double scheduleFit,
        double modeFit,
        double reputation) {

    /** Trọng số mặc định theo spec 4.4. */
    public static final MatchingWeights DEFAULT = new MatchingWeights(0.55, 0.20, 0.15, 0.05, 0.05);

    /** Sai số cho phép khi so tổng với 1: phép cộng số thực như 0.1 + 0.2 không ra đúng 0.3. */
    private static final double SUM_TOLERANCE = 1e-9;

    /**
     * Constructor rút gọn của record: chạy mỗi khi tạo bộ trọng số, kể cả DEFAULT.
     * Kiểm từng trọng số trước (D47): trọng số âm vẫn có thể cho tổng bằng 1 nhưng làm D ra ngoài [0, 1].
     */
    public MatchingWeights {
        requireUnitInterval("skillFit", skillFit);
        requireUnitInterval("levelFit", levelFit);
        requireUnitInterval("scheduleFit", scheduleFit);
        requireUnitInterval("modeFit", modeFit);
        requireUnitInterval("reputation", reputation);

        double sum = skillFit + levelFit + scheduleFit + modeFit + reputation;
        if (Math.abs(sum - 1.0) > SUM_TOLERANCE) {
            throw new IllegalArgumentException("Tổng trọng số phải bằng 1, đang là " + sum);
        }
    }

    private static void requireUnitInterval(String name, double value) {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("Trọng số " + name + " phải trong [0, 1], đang là " + value);
        }
    }
}
