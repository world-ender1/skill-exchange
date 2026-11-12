package com.skillexchange.evaluation;

import static com.skillexchange.evaluation.Relevance.LOAI;
import static com.skillexchange.evaluation.Relevance.NHAN_0;
import static com.skillexchange.evaluation.Relevance.NHAN_1;
import static com.skillexchange.evaluation.Relevance.NHAN_2;
import static com.skillexchange.evaluation.RankingMetrics.dcgAt5;
import static com.skillexchange.evaluation.RankingMetrics.idcgAt5;
import static com.skillexchange.evaluation.RankingMetrics.ndcgAt5;
import static com.skillexchange.evaluation.RankingMetrics.precisionAt5;
import static com.skillexchange.evaluation.RankingMetrics.relsInList;
import static com.skillexchange.evaluation.RankingMetrics.strictPrecisionAt5;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Chỉ số của một người dùng: TC224–TC228 (spec 5.3, A.3; D55, D57, D58).
 *
 * "Danh sách" là rel theo thứ tự thuật toán xếp; "tập lý tưởng" là rel của mọi ứng viên qua Bước 1 thật.
 * Số mong đợi viết bằng biểu thức như trong bộ ca, không chép số đã làm tròn (D36).
 */
class RankingMetricsTest {

    private static final double TOLERANCE = 1e-9;

    /** log2 tính riêng trong test, theo đúng chữ của công thức DCG. */
    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    // ---------- Bộ ca ----------

    @Test
    @DisplayName("TC224 ví dụ A.3: danh sách [2, 0, 1, 0, 0], tập lý tưởng là đúng 5 nhãn đó")
    void tc224_handExampleA3() {
        List<Integer> list = List.of(2, 0, 1, 0, 0);
        List<Integer> ideal = List.of(2, 0, 1, 0, 0);

        double dcg = 3 / log2(2) + 1 / log2(4);
        double idcg = 3 + 1 / log2(3);
        // Dùng log2(i) thay vì log2(i + 1) thì vị trí 1 chia cho log2(1) = 0: DCG ra vô cùng.
        assertThat(dcgAt5(list)).isCloseTo(dcg, within(TOLERANCE));
        assertThat(dcg).isCloseTo(3.5, within(TOLERANCE));
        assertThat(idcgAt5(ideal)).isCloseTo(idcg, within(TOLERANCE));
        assertThat(ndcgAt5(list, ideal)).isCloseTo(3.5 / (3 + 1 / log2(3)), within(TOLERANCE));
        assertThat(precisionAt5(list)).isCloseTo(2.0 / 5, within(TOLERANCE));
        assertThat(strictPrecisionAt5(list)).isCloseTo(1.0 / 5, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC225 danh sách [2, 1] ngắn hơn 5: vị trí trống tính là không phù hợp; xếp đúng thì NDCG@5 = 1")
    void tc225_shortListDividesByFive() {
        List<Integer> list = List.of(2, 1);
        List<Integer> ideal = List.of(2, 1);

        // Chia cho số ứng viên (2) thay vì 5 thì Precision@5 ra 1.0.
        assertThat(precisionAt5(list)).isCloseTo(2.0 / 5, within(TOLERANCE));
        assertThat(strictPrecisionAt5(list)).isCloseTo(1.0 / 5, within(TOLERANCE));
        assertThat(ndcgAt5(list, ideal)).isCloseTo(1.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC226 danh sách [1, 1], tập lý tưởng {2, 1, 1}: IDCG@5 tính trên tập lý tưởng, không trên danh sách")
    void tc226_idcgUsesIdealSetNotList() {
        List<Integer> list = List.of(1, 1);
        List<Integer> ideal = List.of(2, 1, 1);

        double dcg = 1 + 1 / log2(3);
        double idcg = 3 + 1 / log2(3) + 1 / log2(4);
        assertThat(dcgAt5(list)).isCloseTo(dcg, within(TOLERANCE));
        assertThat(idcgAt5(ideal)).isCloseTo(idcg, within(TOLERANCE));
        // IDCG tính trên chính danh sách thì NDCG@5 ra 1.0: ứng viên nhãn 2 bị lọc nhầm không bị phạt.
        assertThat(ndcgAt5(list, ideal)).isCloseTo(dcg / idcg, within(TOLERANCE));
        assertThat(precisionAt5(list)).isCloseTo(2.0 / 5, within(TOLERANCE));
        assertThat(strictPrecisionAt5(list)).isCloseTo(0.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC227 IDCG@5 = 0: (a) [0, 0] với {0, 0}; (b) rỗng với rỗng → NDCG@5 = 0, không NaN; Precision@5 = 0")
    void tc227_zeroIdcgGivesZeroNotNaN() {
        List<Integer> zeros = List.of(0, 0);
        List<Integer> empty = List.of();

        assertThat(idcgAt5(zeros)).isCloseTo(0.0, within(TOLERANCE));
        assertThat(ndcgAt5(zeros, zeros)).isNotNaN().isCloseTo(0.0, within(TOLERANCE));
        assertThat(precisionAt5(zeros)).isCloseTo(0.0, within(TOLERANCE));
        assertThat(strictPrecisionAt5(zeros)).isCloseTo(0.0, within(TOLERANCE));

        assertThat(idcgAt5(empty)).isCloseTo(0.0, within(TOLERANCE));
        assertThat(ndcgAt5(empty, empty)).isNotNaN().isCloseTo(0.0, within(TOLERANCE));
        assertThat(precisionAt5(empty)).isCloseTo(0.0, within(TOLERANCE));
        assertThat(strictPrecisionAt5(empty)).isCloseTo(0.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("TC228 danh sách [x, v1], x trượt Bước 1 thật nên rel = 0; tập lý tưởng {2}")
    void tc228_excludedCandidateInListHasRelZero() {
        List<Integer> list = relsInList(List.of(LOAI, NHAN_2));
        List<Integer> ideal = List.of(2);

        assertThat(list).containsExactly(0, 2);
        assertThat(dcgAt5(list)).isCloseTo(3 / log2(3), within(TOLERANCE));
        assertThat(ndcgAt5(list, ideal)).isCloseTo((3 / log2(3)) / 3, within(TOLERANCE));
    }

    // ---------- Phần quyết định chưa có ca ----------

    @Test
    @DisplayName("D55 relsInList giữ thứ tự: nhãn 0, 1, 2 thành 0, 1, 2; Loại thành 0")
    void d55_relsInListKeepsOrder() {
        assertThat(relsInList(List.of(NHAN_1, LOAI, NHAN_2, NHAN_0, LOAI))).containsExactly(1, 0, 2, 0, 0);
        assertThat(relsInList(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Spec 5.3 danh sách dài hơn 5: chỉ tính 5 vị trí đầu, nhãn 2 ở vị trí 6–7 không được cộng")
    void spec53_onlyFirstFivePositionsCount() {
        List<Integer> list = List.of(0, 0, 0, 0, 0, 2, 2);
        List<Integer> ideal = List.of(0, 0, 0, 0, 0, 2, 2);

        assertThat(precisionAt5(list)).isCloseTo(0.0, within(TOLERANCE));
        assertThat(strictPrecisionAt5(list)).isCloseTo(0.0, within(TOLERANCE));
        assertThat(dcgAt5(list)).isCloseTo(0.0, within(TOLERANCE));
        assertThat(ndcgAt5(list, ideal)).isCloseTo(0.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("D57 tập lý tưởng 8 phần tử, chưa xếp: IDCG@5 xếp giảm dần rồi lấy 5 đầu [2, 2, 2, 1, 1]")
    void d57_idcgSortsDescendingAndTakesFive() {
        List<Integer> ideal = List.of(1, 0, 2, 1, 2, 2, 0, 1);

        double expected = 3 / log2(2) + 3 / log2(3) + 3 / log2(4) + 1 / log2(5) + 1 / log2(6);
        // Không xếp thì ra DCG của [1, 0, 2, 1, 2]; không cắt ở 5 thì cộng thêm 1 / log2(7).
        assertThat(idcgAt5(ideal)).isCloseTo(expected, within(TOLERANCE));
    }

    @Test
    @DisplayName("D57 danh sách có nhiều ứng viên rel 1 hoặc 2 hơn tập lý tưởng: ndcgAt5 báo lỗi, vì NDCG@5 sẽ vượt 1")
    void d57_listWithPositiveRelMissingFromIdealIsRejected() {
        assertThatThrownBy(() -> ndcgAt5(List.of(2), List.of(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ndcgAt5(List.of(1, 1), List.of(1, 2)))
                .isInstanceOf(IllegalArgumentException.class);
        // Phần tử sau vị trí 5 vẫn được kiểm: cả danh sách phải khớp tập lý tưởng.
        assertThatThrownBy(() -> ndcgAt5(List.of(0, 0, 0, 0, 0, 2), List.of(0, 0, 0, 0, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("D55 ứng viên rel 0 không có trong tập lý tưởng (trượt Bước 1 thật) được chấp nhận")
    void d55_relZeroOutsideIdealIsAllowed() {
        // Ba ứng viên bị loại theo giá trị thật nhưng lọt qua lọc trên dữ liệu tự khai; ứng viên nhãn 1 bị lọc nhầm.
        assertThat(ndcgAt5(List.of(0, 0, 0), List.of(1))).isCloseTo(0.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("D57 rel ngoài 0–2 bị từ chối ở mọi hàm")
    void d57_relOutsideZeroToTwoIsRejected() {
        assertThatThrownBy(() -> precisionAt5(List.of(3))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> strictPrecisionAt5(List.of(-1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> dcgAt5(List.of(1, 3))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> idcgAt5(List.of(-1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ndcgAt5(List.of(0), List.of(3))).isInstanceOf(IllegalArgumentException.class);
    }
}
