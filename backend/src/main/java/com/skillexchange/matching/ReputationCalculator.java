package com.skillexchange.matching;

import java.util.List;

/**
 * Reputation(v) = 0.7 * RatingNorm + 0.3 * CompletionRate (spec 4.3), tính từ dữ liệu thô (D43).
 * Hàm thuần, không giữ trạng thái, nên dùng phương thức static.
 */
public final class ReputationCalculator {

    private ReputationCalculator() {
    }

    /** Bước 5. ratings: các điểm 1–5 người đó nhận được; sessions: các phiên người đó đã tham gia. */
    public static double compute(List<Integer> ratings, List<SessionOutcome> sessions) {
        return 0.7 * ratingNorm(ratings) + 0.3 * completionRate(sessions);
    }

    /** (trung bình − 1) / 4 trên thang 1–5; chưa có đánh giá thì 0.5. */
    private static double ratingNorm(List<Integer> ratings) {
        // Kiểm rỗng trước khi chia: 0.0 / 0 ra NaN chứ không ném lỗi.
        if (ratings.isEmpty()) {
            return 0.5;
        }
        int sum = 0;
        for (int rating : ratings) {
            sum += rating;
        }
        double average = (double) sum / ratings.size();
        return (average - 1) / 4;
    }

    /** Số phiên COMPLETED / số phiên đã bắt đầu và đã kết thúc; chưa có phiên nào như vậy thì 0.5. */
    private static double completionRate(List<SessionOutcome> sessions) {
        int completed = 0;
        int finished = 0;
        for (SessionOutcome session : sessions) {
            if (session.status() == SessionStatus.COMPLETED) {
                // Không cần xét started: CHECK trong V1 bắt COMPLETED phải có started_at (kế hoạch bước 5).
                completed++;
                finished++;
            } else if (session.status() == SessionStatus.CANCELLED && session.started()) {
                // Hủy sau khi đã bắt đầu thì bị tính (spec 4.3); hủy trước khi bắt đầu thì bỏ qua.
                finished++;
            }
            // SCHEDULED, IN_PROGRESS, WAITING_CONFIRMATION: chưa kết thúc, không phạt người đang học dở.
        }
        if (finished == 0) {
            return 0.5;
        }
        return (double) completed / finished;
    }
}
