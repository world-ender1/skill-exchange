package com.skillexchange.evaluation;

import java.util.List;

import com.skillexchange.matching.MatchProfile;

/**
 * Một phương án xếp hạng đã cấu hình xong (B0, B1, R hoặc một biến thể của P), tạo bằng Rankers.
 * Một Ranker dùng lại được cho mọi người dùng; gọi lại với cùng đầu vào thì ra cùng kết quả.
 */
@FunctionalInterface
public interface Ranker {

    /**
     * Mã các ứng viên của caller, đã xếp hạng, đủ cả danh sách (không cắt 5): chỉ số tự lấy 5 vị trí đầu,
     * còn so_ung_vien và tỷ lệ có nhãn 2 cần cả danh sách (D60, D72). caller có trong profiles cũng được.
     */
    List<Long> rank(MatchProfile caller, List<MatchProfile> profiles);
}
