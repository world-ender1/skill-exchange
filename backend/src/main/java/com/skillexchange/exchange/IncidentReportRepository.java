package com.skillexchange.exchange;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Truy cập incident_reports. Bước 7: hai câu đọc cho chi tiết phiên (D92); gửi báo cáo ở bước 8. */
public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {

    /**
     * Báo cáo theo phiên và trạng thái. Với OPEN có tối đa một dòng (chỉ mục uq_reports_one_open của DB),
     * nên findFirst... trả đúng báo cáo đang mở, nếu có.
     */
    Optional<IncidentReport> findFirstBySessionIdAndStatus(Long sessionId, ReportStatus status);

    /**
     * Báo cáo RESOLVED mới nhất của phiên (resolution của SessionDetail, D92). Spring Data đọc tên hàm:
     * findFirst = LIMIT 1, OrderByResolvedAtDescIdDesc = ORDER BY resolved_at DESC, id DESC. Xếp theo
     * resolved_at chứ không theo id: báo cáo tạo trước có thể được chốt sau; id là thứ tự phụ khi trùng giờ.
     */
    Optional<IncidentReport> findFirstBySessionIdAndStatusOrderByResolvedAtDescIdDesc(Long sessionId,
                                                                                      ReportStatus status);
}
