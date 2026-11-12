package com.skillexchange.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Kiểm định theo cặp người dùng cho hiệu chỉ số "P − phương án khác" (D62).
 *
 * Mỗi phần tử của diffs là hiệu của một người dùng, ví dụ NDCG@5 của P trừ NDCG@5 của B1.
 * Không dùng thư viện thống kê: Φ tính bằng xấp xỉ erf của Abramowitz–Stegun 7.1.26.
 */
public final class PairedStatistics {

    /** Hiệu có giá trị tuyệt đối dưới ngưỡng này coi là 0; hai hạng lệch nhau dưới ngưỡng này coi là bằng nhau. */
    private static final double TOLERANCE = 1e-9;

    /** Phân vị 2.5% và 97.5% viết theo phần nghìn, để tính vị trí bằng số nguyên. */
    private static final int LOW_PERMILLE = 25;
    private static final int HIGH_PERMILLE = 975;

    // Hệ số của xấp xỉ erf, Abramowitz–Stegun 7.1.26 (sai số tuyệt đối dưới 1.5e-7).
    private static final double AS_P = 0.3275911;
    private static final double AS_A1 = 0.254829592;
    private static final double AS_A2 = -0.284496736;
    private static final double AS_A3 = 1.421413741;
    private static final double AS_A4 = -1.453152027;
    private static final double AS_A5 = 1.061405429;

    private PairedStatistics() {
    }

    /**
     * Kết quả kiểm định Wilcoxon theo cặp.
     *
     * @param n     số cặp còn lại sau khi bỏ hiệu có giá trị tuyệt đối dưới 1e-9
     * @param wPlus tổng hạng của các hiệu dương
     * @param z     thống kê z, không hiệu chỉnh liên tục; dương khi phần tử thứ nhất của hiệu tốt hơn
     * @param p     p-value hai phía, 2(1 − Φ(|z|))
     */
    public record WilcoxonResult(int n, double wPlus, double z, double p) {
    }

    /** Khoảng tin cậy 95% của trung bình hiệu, từ phân vị 2.5% và 97.5% của bootstrap. */
    public record ConfidenceInterval(double low, double high) {
    }

    /** Wilcoxon theo cặp (D62); Optional.empty() khi không còn cặp nào khác nhau. */
    public static Optional<WilcoxonResult> wilcoxon(double[] diffs) {
        checkFinite(diffs);

        // Bỏ hiệu bằng 0 (trong dung sai): cặp hòa không cho biết bên nào tốt hơn.
        List<Double> nonZero = new ArrayList<>();
        for (double diff : diffs) {
            if (Math.abs(diff) >= TOLERANCE) {
                nonZero.add(diff);
            }
        }
        int n = nonZero.size();
        if (n == 0) {
            return Optional.empty();
        }

        // Xếp theo giá trị tuyệt đối tăng dần; vị trí i (đếm từ 0) ứng với hạng i + 1.
        nonZero.sort(Comparator.comparingDouble(diff -> Math.abs(diff)));

        double wPlus = 0;
        double tieSum = 0; // Σ(t³ − t) trên các nhóm hạng bằng nhau
        int groupStart = 0;
        while (groupStart < n) {
            // Nhóm gồm phần tử đầu và mọi phần tử kế tiếp lệch nó dưới 1e-9.
            // So với phần tử đầu nhóm, không so nối tiếp, để nhóm không "trôi" dần qua nhiều bậc 1e-9.
            double groupHead = Math.abs(nonZero.get(groupStart));
            int groupEnd = groupStart + 1; // không tính
            while (groupEnd < n && Math.abs(nonZero.get(groupEnd)) - groupHead < TOLERANCE) {
                groupEnd++;
            }

            // Các hạng groupStart + 1 .. groupEnd, mọi phần tử trong nhóm nhận hạng trung bình.
            double averageRank = (groupStart + 1 + groupEnd) / 2.0;
            for (int i = groupStart; i < groupEnd; i++) {
                if (nonZero.get(i) > 0) {
                    wPlus += averageRank;
                }
            }
            double t = groupEnd - groupStart;
            tieSum += t * t * t - t;

            groupStart = groupEnd;
        }

        // Tính bằng double: n(n+1)(2n+1) tràn int khi n vượt khoảng 1000.
        double size = n;
        double expected = size * (size + 1) / 4;
        double variance = size * (size + 1) * (2 * size + 1) / 24 - tieSum / 48;
        // variance > 0 khi n ≥ 1 (xấu nhất còn 3n(n+1)²/48), nên không cần nhánh riêng.
        double z = (wPlus - expected) / Math.sqrt(variance);
        double p = 2 * (1 - standardNormalCdf(Math.abs(z)));
        return Optional.of(new WilcoxonResult(n, wPlus, z, p));
    }

    /** Bootstrap ghép cặp: lấy mẫu lại có hoàn lại n người, times lần (D62). */
    public static ConfidenceInterval bootstrapMeanCi(double[] diffs, int times, Random random) {
        checkFinite(diffs);
        if (diffs.length == 0) {
            throw new IllegalArgumentException("Bootstrap cần ít nhất một hiệu");
        }
        if (times < 1) {
            throw new IllegalArgumentException("Số lần bootstrap phải từ 1 trở lên: " + times);
        }

        // Giữ mọi hiệu, kể cả 0: bootstrap lấy mẫu lại người dùng, không phải lấy mẫu lại cặp khác nhau.
        int n = diffs.length;
        double[] means = new double[times];
        for (int iteration = 0; iteration < times; iteration++) {
            // Thứ tự rút cố định (D64): n lời gọi nextInt(n) liên tiếp, cộng theo thứ tự rút.
            double sum = 0;
            for (int draw = 0; draw < n; draw++) {
                sum += diffs[random.nextInt(n)];
            }
            means[iteration] = sum / n;
        }

        Arrays.sort(means);
        // Vị trí đếm từ 1, mảng đếm từ 0.
        double low = means[percentilePosition(times, LOW_PERMILLE) - 1];
        double high = means[percentilePosition(times, HIGH_PERMILLE) - 1];
        return new ConfidenceInterval(low, high);
    }

    /** Hàm phân phối chuẩn tắc Φ(x); mức package để test kiểm giá trị đã biết. */
    static double standardNormalCdf(double x) {
        return 0.5 * (1 + erf(x / Math.sqrt(2)));
    }

    /** Xấp xỉ erf của Abramowitz–Stegun 7.1.26; công thức chỉ đúng cho x ≥ 0, x âm dùng erf(−x) = −erf(x). */
    private static double erf(double x) {
        double absX = Math.abs(x);
        double t = 1 / (1 + AS_P * absX);
        double polynomial = ((((AS_A5 * t + AS_A4) * t + AS_A3) * t + AS_A2) * t + AS_A1) * t;
        double erfOfAbs = 1 - polynomial * Math.exp(-absX * absX);
        return x < 0 ? -erfOfAbs : erfOfAbs;
    }

    /** Vị trí ⌈permille · times / 1000⌉ (đếm từ 1), tính bằng số nguyên để không lệch vì làm tròn số thực. */
    private static int percentilePosition(int times, int permille) {
        return (int) Math.ceilDiv((long) permille * times, 1000L);
    }

    private static void checkFinite(double[] diffs) {
        for (double diff : diffs) {
            if (!Double.isFinite(diff)) {
                throw new IllegalArgumentException("Hiệu phải là số hữu hạn: " + diff);
            }
        }
    }
}
