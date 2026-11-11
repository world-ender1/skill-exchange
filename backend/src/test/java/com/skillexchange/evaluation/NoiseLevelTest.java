package com.skillexchange.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Mức nhiễu (p_level, p_miss, p_extra) của spec 5.4, D63; tên mức là giá trị cột muc_nhieu của D72. */
class NoiseLevelTest {

    @Test
    @DisplayName("D63 ba mức chuẩn: khong (0, 0, 0), mac_dinh (0.3, 0.2, 0.05), cao (0.5, 0.4, 0.1)")
    void d63_threeStandardLevels() {
        assertLevel(NoiseLevel.KHONG, "khong", 0, 0, 0);
        assertLevel(NoiseLevel.MAC_DINH, "mac_dinh", 0.3, 0.2, 0.05);
        assertLevel(NoiseLevel.CAO, "cao", 0.5, 0.4, 0.1);
    }

    @Test
    @DisplayName("D63 tạo được mức riêng cho TC206, TC207; xác suất ngoài [0, 1] hoặc tên rỗng bị từ chối")
    void d63_customLevelsAndValidation() {
        assertLevel(new NoiseLevel("chi_lech_trinh_do", 1, 0, 0), "chi_lech_trinh_do", 1, 0, 0);
        assertLevel(new NoiseLevel("chi_bo_sot", 0, 1, 0), "chi_bo_sot", 0, 1, 0);

        assertThatThrownBy(() -> new NoiseLevel("x", -0.1, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NoiseLevel("x", 0, 1.1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NoiseLevel("x", 0, 0, Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NoiseLevel(" ", 0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    private static void assertLevel(NoiseLevel level, String ten, double pLevel, double pMiss, double pExtra) {
        assertThat(level.ten()).isEqualTo(ten);
        assertThat(level.pLevel()).isEqualTo(pLevel);
        assertThat(level.pMiss()).isEqualTo(pMiss);
        assertThat(level.pExtra()).isEqualTo(pExtra);
    }
}
