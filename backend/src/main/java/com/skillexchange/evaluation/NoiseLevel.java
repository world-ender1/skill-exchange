package com.skillexchange.evaluation;

/**
 * Một mức nhiễu dùng khi sinh giá trị tự khai từ giá trị thật (spec 5.4, D63):
 * - pLevel: xác suất trình độ tự khai lệch 1 bậc so với trình độ thật, xét từng kỹ năng dạy;
 * - pMiss: xác suất bỏ sót, xét từng khung thật;
 * - pExtra: xác suất thêm, xét từng khung không thật.
 *
 * Là record chứ không phải enum: ngoài ba mức chuẩn, TC206 và TC207 cần mức (1, 0, 0) và (0, 1, 0).
 * ten của ba mức chuẩn là giá trị cột muc_nhieu trong file kết quả (D72).
 */
public record NoiseLevel(String ten, double pLevel, double pMiss, double pExtra) {

    public static final NoiseLevel KHONG = new NoiseLevel("khong", 0, 0, 0);
    public static final NoiseLevel MAC_DINH = new NoiseLevel("mac_dinh", 0.3, 0.2, 0.05);
    public static final NoiseLevel CAO = new NoiseLevel("cao", 0.5, 0.4, 0.1);

    public NoiseLevel {
        if (ten == null || ten.isBlank()) {
            throw new IllegalArgumentException("Mức nhiễu phải có tên");
        }
        requireProbability("pLevel", pLevel);
        requireProbability("pMiss", pMiss);
        requireProbability("pExtra", pExtra);
    }

    private static void requireProbability(String field, double p) {
        // Viết "nằm trong [0, 1]" rồi phủ định, để NaN cũng bị từ chối: mọi phép so sánh với NaN đều false.
        if (!(p >= 0 && p <= 1)) {
            throw new IllegalArgumentException(field + " phải nằm trong [0, 1]: " + p);
        }
    }
}
