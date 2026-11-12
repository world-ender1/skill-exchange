package com.skillexchange.evaluation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Chỉ số xếp hạng của một người dùng tại k = 5 (spec 5.3; D55, D57, D58).
 *
 * Hàm thuần trên số nguyên rel (0, 1, 2), không nhận Relevance, để test ghi thẳng ví dụ A.3.
 * - Danh sách: rel của cả danh sách ứng viên, theo thứ tự xếp hạng; Precision@5 và DCG@5 chỉ đọc 5 phần tử đầu.
 * - Tập lý tưởng: rel của mọi ứng viên qua Bước 1 thật (D57), thứ tự không quan trọng.
 */
public final class RankingMetrics {

    /** Số vị trí đầu được tính. */
    public static final int K = 5;

    private static final int MIN_REL = 0;
    private static final int MAX_REL = 2;

    private RankingMetrics() {
    }

    /** Đổi nhãn của danh sách sang rel, giữ thứ tự; ứng viên LOAI có mặt trong danh sách nhận rel = 0 (D55). */
    public static List<Integer> relsInList(List<Relevance> labels) {
        List<Integer> rels = new ArrayList<>(labels.size());
        for (Relevance label : labels) {
            // LOAI.rel() cố ý ném lỗi (bước 3), nên đổi LOAI ở đây, chỗ duy nhất được phép coi nó là 0.
            rels.add(label == Relevance.LOAI ? 0 : label.rel());
        }
        return List.copyOf(rels);
    }

    /** Số ứng viên nhãn 1 hoặc 2 trong 5 vị trí đầu, chia 5; vị trí trống tính là không phù hợp. */
    public static double precisionAt5(List<Integer> listRels) {
        return (double) countInTopK(listRels, 1) / K;
    }

    /** Như Precision@5 nhưng chỉ đếm nhãn 2 (D58). */
    public static double strictPrecisionAt5(List<Integer> listRels) {
        return (double) countInTopK(listRels, 2) / K;
    }

    /** Tổng (2^rel - 1) / log2(i + 1) trên 5 vị trí đầu, i tính từ 1. */
    public static double dcgAt5(List<Integer> listRels) {
        checkRels(listRels);
        double dcg = 0;
        int positions = Math.min(K, listRels.size());
        for (int i = 1; i <= positions; i++) {
            int rel = listRels.get(i - 1);
            // Mẫu số là log2(i + 1): vị trí 1 chia cho log2(2) = 1, không chia cho 0.
            dcg += (Math.pow(2, rel) - 1) / log2(i + 1);
        }
        return dcg;
    }

    /** DCG@5 của tập lý tưởng xếp theo rel giảm dần (D57). */
    public static double idcgAt5(List<Integer> idealRels) {
        checkRels(idealRels);
        List<Integer> best = new ArrayList<>(idealRels);
        best.sort(Comparator.reverseOrder());
        return dcgAt5(best);
    }

    /** DCG@5 / IDCG@5; IDCG@5 = 0 thì quy ước 0 (spec 5.3). */
    public static double ndcgAt5(List<Integer> listRels, List<Integer> idealRels) {
        checkRels(listRels);
        checkRels(idealRels);
        checkListMatchesIdeal(listRels, idealRels);

        // IDCG lấy từ tập lý tưởng, không từ danh sách: ứng viên tốt bị lọc nhầm vẫn làm giảm NDCG (D57).
        double idcg = idcgAt5(idealRels);
        // IDCG là tổng các số không âm; bằng đúng 0 khi và chỉ khi mọi rel đều 0, nên so == 0 là chính xác.
        if (idcg == 0) {
            return 0.0;
        }
        return dcgAt5(listRels) / idcg;
    }

    // ---------- Dùng chung ----------

    /** Số phần tử có rel ≥ minRel trong 5 vị trí đầu. */
    private static int countInTopK(List<Integer> listRels, int minRel) {
        checkRels(listRels);
        int count = 0;
        int positions = Math.min(K, listRels.size());
        for (int i = 0; i < positions; i++) {
            if (listRels.get(i) >= minRel) {
                count++;
            }
        }
        return count;
    }

    private static void checkRels(List<Integer> rels) {
        for (int rel : rels) {
            if (rel < MIN_REL || rel > MAX_REL) {
                throw new IllegalArgumentException("rel phải trong 0–2, nhận " + rel);
            }
        }
    }

    /**
     * Ứng viên rel 1 hoặc 2 trong danh sách đã qua Bước 1 thật, nên phải có trong tập lý tưởng (D57).
     * Ứng viên rel 0 thì có thể nằm ngoài: đó là ứng viên trượt Bước 1 thật mà vẫn lọt vào danh sách (D55).
     * Kiểm tra này giữ NDCG@5 ≤ 1 và bắt lỗi ghép nhầm danh sách với tập lý tưởng của người khác.
     */
    private static void checkListMatchesIdeal(List<Integer> listRels, List<Integer> idealRels) {
        for (int rel = 1; rel <= MAX_REL; rel++) {
            long inList = countEqual(listRels, rel);
            long inIdeal = countEqual(idealRels, rel);
            if (inList > inIdeal) {
                throw new IllegalArgumentException("Danh sách có " + inList + " ứng viên rel = " + rel
                        + " nhưng tập lý tưởng chỉ có " + inIdeal + " (D57)");
            }
        }
    }

    private static long countEqual(List<Integer> rels, int rel) {
        return rels.stream().filter(r -> r == rel).count();
    }

    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }
}
