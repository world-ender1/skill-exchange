package com.skillexchange.evaluation;

import static com.skillexchange.evaluation.PairedStatistics.bootstrapMeanCi;
import static com.skillexchange.evaluation.PairedStatistics.standardNormalCdf;
import static com.skillexchange.evaluation.PairedStatistics.wilcoxon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.skillexchange.evaluation.PairedStatistics.ConfidenceInterval;
import com.skillexchange.evaluation.PairedStatistics.WilcoxonResult;

/**
 * Kiểm định theo cặp: TC231–TC233 (D62).
 *
 * n, W+ và z là phép tính chính xác nên so dung sai 1e-9 (D36); p dùng xấp xỉ Φ nên so dung sai 1e-3 như bộ ca.
 */
class PairedStatisticsTest {

    private static final double TOLERANCE = 1e-9;
    private static final double P_TOLERANCE = 1e-3;

    /** Seed của luồng bootstrap (D62). */
    private static final long BOOTSTRAP_SEED = 3_000_000;
    private static final int BOOTSTRAP_TIMES = 2000;

    /**
     * Random theo kịch bản: lần lặp k (tính từ 0) của bootstrap rút toàn chỉ số k.
     * Với n người và bound = n, lời gọi thứ c trả c / n: n lời gọi đầu trả 0, n lời gọi kế trả 1...
     * Nhờ vậy trung bình của lần lặp k đúng bằng diffs[k], biết trước được mọi giá trị.
     */
    private static Random scriptedRandom() {
        return new Random() {
            private int calls = 0;

            @Override
            public int nextInt(int bound) {
                return calls++ / bound;
            }
        };
    }

    /** Mảng 0.0, 1.0, ..., count − 1. */
    private static double[] zeroTo(int count) {
        double[] values = new double[count];
        for (int i = 0; i < count; i++) {
            values[i] = i;
        }
        return values;
    }

    // ---------- Bộ ca: Wilcoxon ----------

    @Test
    @DisplayName("TC231 hiệu [0.1, 0.2, 0.3, 0.4, −0.5]: n = 5, W+ = 10, z = 2.5 / √13.75, p ≈ 0.5002")
    void tc231_wilcoxonWithoutTies() {
        WilcoxonResult result = wilcoxon(new double[] {0.1, 0.2, 0.3, 0.4, -0.5}).orElseThrow();

        assertThat(result.n()).isEqualTo(5);
        // Hạng theo giá trị tuyệt đối 1..5; −0.5 có hạng 5 nên W+ = 1 + 2 + 3 + 4.
        assertThat(result.wPlus()).isCloseTo(10.0, within(TOLERANCE));
        // Kỳ vọng n(n+1)/4 = 7.5; phương sai n(n+1)(2n+1)/24 = 13.75.
        assertThat(result.z()).isCloseTo(2.5 / Math.sqrt(13.75), within(TOLERANCE));
        assertThat(result.p()).isCloseTo(0.5002, within(P_TOLERANCE));
    }

    @Test
    @DisplayName("TC232 (a) hiệu [0, 0.2, −0.2, 0.5]: bỏ hiệu 0, n = 3, hạng 1.5, 1.5, 3, W+ = 4.5, z = 1.5 / √3.375, p ≈ 0.414")
    void tc232a_zeroDroppedAndTiesAveraged() {
        WilcoxonResult result = wilcoxon(new double[] {0, 0.2, -0.2, 0.5}).orElseThrow();

        assertThat(result.n()).isEqualTo(3);
        assertThat(result.wPlus()).isCloseTo(4.5, within(TOLERANCE));
        // Phương sai 3·4·7/24 = 3.5, trừ hiệu chỉnh hạng bằng nhau (2³ − 2)/48 = 0.125.
        // Quên hiệu chỉnh thì z = 1.5 / √3.5 ≈ 0.8018 và p ≈ 0.423.
        assertThat(result.z()).isCloseTo(1.5 / Math.sqrt(3.375), within(TOLERANCE));
        assertThat(result.p()).isCloseTo(0.414, within(P_TOLERANCE));
    }

    @Test
    @DisplayName("TC232 (b) hiệu [0, 0, 0]: \"không có cặp khác nhau\", không lỗi, không NaN")
    void tc232b_noDifferentPairs() {
        assertThat(wilcoxon(new double[] {0, 0, 0})).isEmpty();
    }

    // ---------- Bộ ca: bootstrap ----------

    @Test
    @DisplayName("TC233 (a) mọi hiệu bằng 0.1, bootstrap 2000 lần: khoảng tin cậy [0.1, 0.1]")
    void tc233a_constantDiffsGiveDegenerateInterval() {
        double[] diffs = {0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1};

        ConfidenceInterval ci = bootstrapMeanCi(diffs, BOOTSTRAP_TIMES, new Random(BOOTSTRAP_SEED));

        assertThat(ci.low()).isCloseTo(0.1, within(TOLERANCE));
        assertThat(ci.high()).isCloseTo(0.1, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC233 (b) cùng dữ liệu, chạy hai lần với seed 3_000_000: cùng khoảng tin cậy")
    void tc233b_sameSeedSameInterval() {
        double[] diffs = {0.3, -0.1, 0.0, 0.25, 0.5, -0.4, 0.1, 0.2};

        ConfidenceInterval first = bootstrapMeanCi(diffs, BOOTSTRAP_TIMES, new Random(BOOTSTRAP_SEED));
        ConfidenceInterval second = bootstrapMeanCi(diffs, BOOTSTRAP_TIMES, new Random(BOOTSTRAP_SEED));

        assertThat(second).isEqualTo(first);
        assertThat(first.low()).isLessThan(first.high());
    }

    // ---------- Phần quyết định chưa có ca ----------

    @Test
    @DisplayName("D62 bỏ hiệu có giá trị tuyệt đối dưới 1e-9: 1e-10, −5e-10 bị bỏ; 2e-8 được giữ")
    void d62_dropThresholdIsOneNanoUnit() {
        WilcoxonResult result = wilcoxon(new double[] {1e-10, -5e-10, 0.3, -2e-8}).orElseThrow();

        // Còn −2e-8 (hạng 1) và 0.3 (hạng 2): W+ = 2; kỳ vọng 2·3/4 = 1.5; phương sai 2·3·5/24 = 1.25.
        assertThat(result.n()).isEqualTo(2);
        assertThat(result.wPlus()).isCloseTo(2.0, within(TOLERANCE));
        assertThat(result.z()).isCloseTo(0.5 / Math.sqrt(1.25), within(TOLERANCE));
    }

    @Test
    @DisplayName("D62 hiệu 0.6 − 0.4 (= 0.19999999999999996) và −0.2 bằng nhau trong dung sai 1e-9 nên cùng hạng 1.5")
    void d62_tiesUseToleranceNotExactEquality() {
        // Bẫy số thực: hai hiệu Precision@5 bằng nhau về toán học nhưng khác nhau trong double.
        assertThat(0.6 - 0.4).isNotEqualTo(0.2);

        WilcoxonResult result = wilcoxon(new double[] {0.6 - 0.4, -0.2, 0.5}).orElseThrow();

        // So bằng == thì hạng là 1, 2, 3: W+ = 4 và không có hiệu chỉnh hạng bằng nhau.
        assertThat(result.wPlus()).isCloseTo(4.5, within(TOLERANCE));
        assertThat(result.z()).isCloseTo(1.5 / Math.sqrt(3.375), within(TOLERANCE));
    }

    @Test
    @DisplayName("D62 nhóm hạng bằng nhau so với phần tử đầu nhóm: 0.1 và 0.1 + 0.6e-9 cùng nhóm, 0.1 + 1.2e-9 sang nhóm mới")
    void d62_tieGroupComparesWithGroupHead() {
        WilcoxonResult result = wilcoxon(new double[] {0.1, -(0.1 + 0.6e-9), 0.1 + 1.2e-9}).orElseThrow();

        // Hạng 1.5, 1.5, 3: W+ = 1.5 + 3 = 4.5; một nhóm t = 2 nên phương sai 3.5 − 6/48 = 3.375.
        // So nối tiếp thì cả ba vào một nhóm (mỗi bước chỉ lệch 0.6e-9): hạng 2, 2, 2, W+ = 4, phương sai 3.5 − 24/48 = 3.
        assertThat(result.wPlus()).isCloseTo(4.5, within(TOLERANCE));
        assertThat(result.z()).isCloseTo(1.5 / Math.sqrt(3.375), within(TOLERANCE));
    }

    @Test
    @DisplayName("D62 hiệu đổi dấu của TC231: z = −2.5 / √13.75 (giữ dấu), p vẫn ≈ 0.5002 (p dùng |z|)")
    void d62_negativeZKeepsSignAndSameP() {
        WilcoxonResult result = wilcoxon(new double[] {-0.1, -0.2, -0.3, -0.4, 0.5}).orElseThrow();

        // Chỉ 0.5 dương, hạng 5: W+ = 5, nhỏ hơn kỳ vọng 7.5 nên z âm (P kém hơn phương án so sánh).
        assertThat(result.wPlus()).isCloseTo(5.0, within(TOLERANCE));
        assertThat(result.z()).isCloseTo(-2.5 / Math.sqrt(13.75), within(TOLERANCE));
        // Dùng z thay |z| thì p = 2(1 − Φ(−0.674)) ≈ 1.5, ra ngoài [0, 1].
        assertThat(result.p()).isCloseTo(0.5002, within(P_TOLERANCE));
    }

    @Test
    @DisplayName("D62 Φ xấp xỉ: Φ(0) = 0.5, Φ(1) ≈ 0.841345, Φ(±1.959964) ≈ 0.975 / 0.025 (dung sai 1e-6)")
    void d62_standardNormalCdfKnownValues() {
        double z975 = 1.959963984540054;

        assertThat(standardNormalCdf(0)).isCloseTo(0.5, within(1e-6));
        assertThat(standardNormalCdf(1)).isCloseTo(0.8413447460685429, within(1e-6));
        assertThat(standardNormalCdf(z975)).isCloseTo(0.975, within(1e-6));
        // erf là hàm lẻ: quên xử lý x âm thì Φ(−1.96) sai hẳn.
        assertThat(standardNormalCdf(-z975)).isCloseTo(0.025, within(1e-6));
    }

    @Test
    @DisplayName("D62 60 hiệu dương: z ≈ 6.7, p rất nhỏ nhưng không âm (xấp xỉ Φ không đẩy p ra ngoài [0, 1])")
    void d62_pStaysInRangeForLargeZ() {
        double[] diffs = new double[60];
        for (int i = 0; i < diffs.length; i++) {
            diffs[i] = i + 1;
        }

        WilcoxonResult result = wilcoxon(diffs).orElseThrow();

        // W+ = 1 + ... + 60 = 1830; kỳ vọng 60·61/4 = 915; phương sai 60·61·121/24.
        assertThat(result.z()).isCloseTo(915 / Math.sqrt(60.0 * 61 * 121 / 24), within(TOLERANCE));
        assertThat(result.p()).isBetween(0.0, 1e-6);
    }

    @Test
    @DisplayName("D62 phân vị bootstrap: phần tử thứ ⌈0.025·k⌉ và ⌈0.975·k⌉ sau khi xếp; k = 50 → thứ 2 và thứ 49")
    void d62_bootstrapPercentileRank() {
        // 50 lần lặp, lần k có trung bình đúng bằng k: các trung bình đã xếp là 0, 1, ..., 49.
        // ⌈1.25⌉ = 2 → giá trị 1.0 (lấy sàn hoặc làm tròn ra 0.0); ⌈48.75⌉ = 49 → giá trị 48.0.
        // Lấy ⌈q·k⌉ làm chỉ số đếm từ 0 thì ra [2.0, 49.0].
        ConfidenceInterval ci = bootstrapMeanCi(zeroTo(50), 50, scriptedRandom());

        assertThat(ci.low()).isCloseTo(1.0, within(TOLERANCE));
        assertThat(ci.high()).isCloseTo(48.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("D62 bootstrap giữ hiệu bằng 0: lấy mẫu lại người dùng, chỉ Wilcoxon mới bỏ hiệu 0")
    void d62_bootstrapKeepsZeroDiffs() {
        // 4 lần lặp, lần k chỉ rút người thứ k: trung bình đã xếp là 0, 0, 0, 0.4.
        // Bỏ hiệu 0 thì chỉ còn một người và khoảng tin cậy thành [0.4, 0.4].
        ConfidenceInterval ci = bootstrapMeanCi(new double[] {0.0, 0.0, 0.0, 0.4}, 4, scriptedRandom());

        assertThat(ci.low()).isCloseTo(0.0, within(TOLERANCE));
        assertThat(ci.high()).isCloseTo(0.4, within(TOLERANCE));
    }

    @Test
    @DisplayName("D62 dữ liệu sai bị từ chối: hiệu NaN hoặc vô cực; bootstrap với mảng rỗng hoặc times < 1")
    void d62_invalidInputIsRejected() {
        double[] withNaN = {0.1, Double.NaN};
        double[] withInfinity = {Double.POSITIVE_INFINITY, 0.2};

        assertThatThrownBy(() -> wilcoxon(withNaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wilcoxon(withInfinity)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bootstrapMeanCi(withNaN, 10, new Random(BOOTSTRAP_SEED)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bootstrapMeanCi(new double[0], 10, new Random(BOOTSTRAP_SEED)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bootstrapMeanCi(new double[] {0.1}, 0, new Random(BOOTSTRAP_SEED)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("D62 Wilcoxon với mảng rỗng: \"không có cặp khác nhau\", không lỗi")
    void d62_emptyDiffsHaveNoDifferentPairs() {
        assertThat(wilcoxon(new double[0])).isEmpty();
    }
}
