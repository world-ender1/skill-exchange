package com.skillexchange.evaluation;

import java.util.List;
import java.util.Set;

/**
 * Chỉ số của một người dùng với một phương án: một dòng của per_user.csv (D72).
 *
 * @param userId             mã người dùng u
 * @param candidateCount     số ứng viên trong danh sách (so_ung_vien)
 * @param precisionAt5       Precision@5 (nhãn 1 và 2)
 * @param strictPrecisionAt5 Precision@5 nghiêm ngặt (chỉ nhãn 2, D58)
 * @param ndcgAt5            NDCG@5, bằng 0 khi IDCG@5 = 0
 * @param idealLevelCount    số giá trị rel khác nhau trong tập lý tưởng, 0–3 (so_muc_nhan_ly_tuong)
 * @param idcgZero           IDCG@5 = 0: tập lý tưởng không có ứng viên nhãn 1 hoặc 2
 * @param hasLabel2InList    có ít nhất một ứng viên nhãn 2 trong cả danh sách, không chỉ 5 đầu (D60)
 */
public record UserMetrics(
        long userId,
        int candidateCount,
        double precisionAt5,
        double strictPrecisionAt5,
        double ndcgAt5,
        int idealLevelCount,
        boolean idcgZero,
        boolean hasLabel2InList) {

    /** Bản (2) của D59 cần tập lý tưởng có ít nhất 2 mức nhãn khác nhau. */
    private static final int MIN_LEVELS_FOR_NDCG = 2;

    /**
     * @param listRels  rel của cả danh sách ứng viên theo thứ tự xếp hạng (LOAI đã đổi thành 0)
     * @param idealRels rel của mọi ứng viên trong tập lý tưởng của u
     */
    public static UserMetrics of(long userId, List<Integer> listRels, List<Integer> idealRels) {
        // ndcgAt5 kiểm rel trong 0–2 và danh sách khớp tập lý tưởng, nên gọi trước các phép tính còn lại.
        double ndcg = RankingMetrics.ndcgAt5(listRels, idealRels);
        return new UserMetrics(
                userId,
                listRels.size(),
                RankingMetrics.precisionAt5(listRels),
                RankingMetrics.strictPrecisionAt5(listRels),
                ndcg,
                // Chỉ cần số phần tử khác nhau, không duyệt Set nên thứ tự của Set không ảnh hưởng (D64).
                Set.copyOf(idealRels).size(),
                RankingMetrics.idcgAt5(idealRels) == 0,
                listRels.contains(Relevance.NHAN_2.rel()));
    }

    /** Thuộc bản (2) của D59: tập lý tưởng có ít nhất 2 mức nhãn khác nhau. */
    public boolean nonDegenerate() {
        // Chỉ một mức nhãn (hoặc rỗng) thì thứ tự nào cũng "lý tưởng", NDCG không phân biệt được phương án (R24).
        return idealLevelCount >= MIN_LEVELS_FOR_NDCG;
    }
}
