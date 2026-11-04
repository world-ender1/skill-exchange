package com.skillexchange.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
