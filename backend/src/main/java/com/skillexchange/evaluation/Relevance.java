package com.skillexchange.evaluation;

/**
 * Kết quả gán nhãn một cặp theo hướng u học từ v (spec 5.1).
 *
 * LOAI: vi phạm Bước 1, không có nhãn và không vào tập lý tưởng dùng tính IDCG.
 * Nếu thuật toán vẫn đưa ứng viên này vào danh sách thì nơi tính chỉ số coi rel = 0 (D55);
 * rel() của LOAI ném lỗi để nơi đó phải xử lý rõ ràng, không lặng lẽ thành 0.
 */
public enum Relevance {
    LOAI,
    NHAN_0,
    NHAN_1,
    NHAN_2;

    /** Độ liên quan dùng trong DCG và Precision@5: 0, 1, 2. */
    public int rel() {
        return switch (this) {
            case LOAI -> throw new IllegalStateException("Ứng viên bị loại ở Bước 1 không có rel (D55)");
            case NHAN_0 -> 0;
            case NHAN_1 -> 1;
            case NHAN_2 -> 2;
        };
    }
}
