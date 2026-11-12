package com.skillexchange.evaluation;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * Chỉ số gộp trên mọi người dùng đủ điều kiện của một phương án (spec 5.3; D58–D60).
 * Tên cột chi_so của metrics.csv (D72) ghi cạnh từng thành phần.
 *
 * @param userCount            số người dùng đủ điều kiện (so_nguoi_tinh của mọi chỉ số trừ NDCG bản 2)
 * @param precisionAt5         precision_at_5
 * @param strictPrecisionAt5   precision_at_5_nghiem_ngat
 * @param ndcgAt5AllUsers      ndcg_at_5_tat_ca: bản (1) của D59, mọi người dùng
 * @param ndcgAt5NonDegenerate ndcg_at_5_loai_suy_bien: bản (2) của D59; NaN khi không còn ai
 * @param nonDegenerateCount   so_nguoi_tinh của NDCG bản (2)
 * @param idcgZeroCount        so_ca_idcg_0
 * @param coverage             coverage: tỷ lệ người có ít nhất 1 ứng viên
 * @param atLeastFiveRate      ty_le_du_5_ung_vien
 * @param hasLabel2Rate        ty_le_co_nhan_2
 */
public record MetricSummary(
        int userCount,
        double precisionAt5,
        double strictPrecisionAt5,
        double ndcgAt5AllUsers,
        double ndcgAt5NonDegenerate,
        int nonDegenerateCount,
        int idcgZeroCount,
        double coverage,
        double atLeastFiveRate,
        double hasLabel2Rate) {

    /** Gộp theo đúng thứ tự danh sách truyền vào; danh sách chỉ gồm người dùng đủ điều kiện, không rỗng. */
    public static MetricSummary of(List<UserMetrics> users) {
        if (users.isEmpty()) {
            throw new IllegalArgumentException("Cần ít nhất một người dùng đủ điều kiện để làm mẫu số");
        }
        List<UserMetrics> nonDegenerate = users.stream().filter(UserMetrics::nonDegenerate).toList();
        // Không còn ai thì trung bình không xác định: trả NaN có chủ đích, không để 0/0 tự sinh (D59).
        double ndcgNonDegenerate = nonDegenerate.isEmpty()
                ? Double.NaN
                : mean(nonDegenerate, UserMetrics::ndcgAt5);

        return new MetricSummary(
                users.size(),
                // Precision@5 lấy trên mọi người: R24 chỉ nói về NDCG, D72 chỉ có một cột cho mỗi bản Precision.
                mean(users, UserMetrics::precisionAt5),
                mean(users, UserMetrics::strictPrecisionAt5),
                mean(users, UserMetrics::ndcgAt5),
                ndcgNonDegenerate,
                nonDegenerate.size(),
                count(users, UserMetrics::idcgZero),
                share(users, user -> user.candidateCount() >= 1),
                // "Đủ 5 ứng viên" = đủ lấp 5 vị trí của top 5 (D60).
                share(users, user -> user.candidateCount() >= RankingMetrics.K),
                share(users, UserMetrics::hasLabel2InList));
    }

    /** so_nguoi_bi_loai: người có tập lý tưởng ít hơn 2 mức nhãn, không tính vào bản (2). */
    public int excludedCount() {
        return userCount - nonDegenerateCount;
    }

    // ---------- Dùng chung ----------

    /** Trung bình cộng, cộng theo thứ tự danh sách để số thực giống nhau giữa các lần chạy (D64). */
    private static double mean(List<UserMetrics> users, ToDoubleFunction<UserMetrics> metric) {
        double sum = 0;
        for (UserMetrics user : users) {
            sum += metric.applyAsDouble(user);
        }
        return sum / users.size();
    }

    private static int count(List<UserMetrics> users, Predicate<UserMetrics> condition) {
        int count = 0;
        for (UserMetrics user : users) {
            if (condition.test(user)) {
                count++;
            }
        }
        return count;
    }

    private static double share(List<UserMetrics> users, Predicate<UserMetrics> condition) {
        return (double) count(users, condition) / users.size();
    }
}
