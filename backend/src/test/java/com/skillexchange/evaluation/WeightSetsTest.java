package com.skillexchange.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.skillexchange.matching.MatchingWeights;

/** Bộ trọng số của thực nghiệm: TC241 (D66, D67; spec 5.2, 5.4). */
class WeightSetsTest {

    private static final double EPS = 1e-9;

    @Test
    @DisplayName("TC241 Ba bộ bỏ thành phần đúng D66")
    void tc241_ablationWeights() {
        assertWeights(WeightSets.BO_LEVEL.weights(), 0.60, 0, 0.20, 0.10, 0.10);
        assertWeights(WeightSets.BO_SCHEDULE.weights(), 0.5875, 0.2375, 0, 0.0875, 0.0875);
        assertWeights(WeightSets.BO_MODE.weights(), 0.5625, 0.2125, 0.1625, 0, 0.0625);
    }

    @Test
    @DisplayName("TC241 Bốn bộ độ nhạy đúng spec 5.4, bộ mặc định là của hệ thống")
    void tc241_sensitivityWeights() {
        assertThat(WeightSets.MAC_DINH.weights()).isSameAs(MatchingWeights.DEFAULT);
        assertWeights(WeightSets.MAC_DINH.weights(), 0.55, 0.20, 0.15, 0.05, 0.05);
        assertWeights(WeightSets.KY_NANG.weights(), 0.65, 0.15, 0.10, 0.05, 0.05);
        assertWeights(WeightSets.LICH.weights(), 0.50, 0.15, 0.25, 0.05, 0.05);
        assertWeights(WeightSets.UY_TIN.weights(), 0.45, 0.15, 0.15, 0.05, 0.20);
    }

    @Test
    @DisplayName("TC241 Bảy bộ có tên phuong_an của D72, theo thứ tự cố định")
    void tc241_namesMatchD72() {
        // Nạp được lớp WeightSets nghĩa là mọi new MatchingWeights(...) đều qua kiểm tra của lõi (D47).
        assertThat(WeightSets.ABLATION).extracting(WeightSets.Named::phuongAn)
                .containsExactly("P_bo_level", "P_bo_schedule", "P_bo_mode");
        assertThat(WeightSets.SENSITIVITY).extracting(WeightSets.Named::phuongAn)
                .containsExactly("P", "P_w_ky_nang", "P_w_lich", "P_w_uy_tin");
    }

    @Test
    @DisplayName("D66 Bộ bỏ thành phần = bộ mặc định, trọng số bị bỏ chia đều cho 4 thành phần còn lại")
    void d66_ablationFollowsEqualSplitRule() {
        double[] base = toArray(MatchingWeights.DEFAULT);
        // Thứ tự thành phần: SkillFit, LevelFit, ScheduleFit, ModeFit, Reputation.
        List<WeightSets.Named> ablation = WeightSets.ABLATION;
        int[] dropped = {1, 2, 3}; // BO_LEVEL, BO_SCHEDULE, BO_MODE

        for (int k = 0; k < dropped.length; k++) {
            double[] expected = base.clone();
            double share = base[dropped[k]] / 4;
            for (int i = 0; i < expected.length; i++) {
                expected[i] = i == dropped[k] ? 0 : base[i] + share;
            }
            assertWeights(ablation.get(k).weights(), expected);
        }
    }

    private static void assertWeights(MatchingWeights actual, double... expected) {
        double[] values = toArray(actual);
        List<String> names = List.of("skillFit", "levelFit", "scheduleFit", "modeFit", "reputation");
        for (int i = 0; i < values.length; i++) {
            assertThat(values[i]).as(names.get(i)).isCloseTo(expected[i], within(EPS));
        }
    }

    private static double[] toArray(MatchingWeights w) {
        return new double[] {w.skillFit(), w.levelFit(), w.scheduleFit(), w.modeFit(), w.reputation()};
    }
}
