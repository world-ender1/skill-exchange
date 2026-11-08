package com.skillexchange.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit test thuần Java cho MatchingWeights (D35, D47). Không Spring, không DB. */
class MatchingWeightsTest {

    @Test
    @DisplayName("TC126 bộ mặc định đúng spec 4.4 và tổng bằng 1")
    void tc126_defaultWeightsMatchSpec() {
        MatchingWeights w = MatchingWeights.DEFAULT;

        assertThat(w.skillFit()).isEqualTo(0.55);
        assertThat(w.levelFit()).isEqualTo(0.20);
        assertThat(w.scheduleFit()).isEqualTo(0.15);
        assertThat(w.modeFit()).isEqualTo(0.05);
        assertThat(w.reputation()).isEqualTo(0.05);
        double sum = w.skillFit() + w.levelFit() + w.scheduleFit() + w.modeFit() + w.reputation();
        assertThat(sum).isCloseTo(1.0, within(1e-9));
    }

    @Test
    @DisplayName("TC126 tổng 0.95 bị từ chối")
    void tc126_sumNotOneIsRejected() {
        assertThatThrownBy(() -> new MatchingWeights(0.5, 0.2, 0.15, 0.05, 0.05))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("TC126 tổng bằng 1 nhưng có trọng số âm bị từ chối (D47)")
    void tc126_negativeWeightIsRejected() {
        assertThatThrownBy(() -> new MatchingWeights(0.6, 0.2, 0.15, 0.1, -0.05))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("D47 trọng số lớn hơn 1 bị từ chối dù tổng bằng 1")
    void d47_weightAboveOneIsRejected() {
        assertThatThrownBy(() -> new MatchingWeights(1.2, -0.05, -0.05, -0.05, -0.05))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("D47 bộ biên (1, 0, 0, 0, 0) hợp lệ (TC142 dùng bộ này)")
    void d47_boundaryWeightsAreAccepted() {
        MatchingWeights w = new MatchingWeights(1, 0, 0, 0, 0);

        assertThat(w.skillFit()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("D47 tổng lệch dưới 1e-9 vẫn hợp lệ (sai số làm tròn số thực)")
    void d47_sumWithinToleranceIsAccepted() {
        // 0.1 + 0.2 + ... trong double không ra đúng 1.0; sai số rất nhỏ phải được chấp nhận.
        MatchingWeights w = new MatchingWeights(0.1, 0.2, 0.3, 0.2, 0.2);

        assertThat(w.reputation()).isEqualTo(0.2);
    }

    @ParameterizedTest(name = "NaN ở trọng số thứ {0} (đếm từ 0)")
    @ValueSource(ints = {0, 1, 2, 3, 4})
    @DisplayName("D47 trọng số NaN bị từ chối, ở vị trí nào cũng vậy")
    void d47_nanWeightIsRejected(int position) {
        // NaN so với số nào cũng ra false, nên "value < 0 || value > 1" không bắt được NaN.
        // Tuần 5 tính trọng số bằng phép chia, mà 0.0 / 0 trong Java ra NaN chứ không ném lỗi.
        double[] w = {0.55, 0.20, 0.15, 0.05, 0.05};
        w[position] = Double.NaN;

        assertThatThrownBy(() -> new MatchingWeights(w[0], w[1], w[2], w[3], w[4]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phải trong [0, 1]");
    }
}
