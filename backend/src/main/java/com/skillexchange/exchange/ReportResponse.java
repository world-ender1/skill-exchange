package com.skillexchange.exchange;

import java.time.Instant;

/**
 * Response 201 của POST /api/sessions/{id}/reports (D94, bảng 2.2): đúng bốn khóa. Không trả lại lý do: lý do dành
 * cho admin xử lý (A02, tuần 7), chi tiết phiên cũng không cho người tham gia thấy (openReport của SessionDetail).
 */
public record ReportResponse(long id, long sessionId, ReportStatus status, Instant createdAt) {

    static ReportResponse from(IncidentReport report) {
        return new ReportResponse(report.getId(), report.getSessionId(), report.getStatus(), report.getCreatedAt());
    }
}
