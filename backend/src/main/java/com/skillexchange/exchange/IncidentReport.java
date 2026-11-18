package com.skillexchange.exchange;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Báo cáo sự cố – bảng incident_reports (TK 3.3, FR-09).
 *
 * Nằm ở package exchange chứ không ở review như tiêu đề TK 4.8: xác nhận hoàn thành phải kiểm "phiên có báo cáo
 * OPEN không" trong cùng transaction với việc đổi trạng thái phiên (R33, D93). Phần admin xử lý (A02) ở tuần 7.
 * Bảng không có cột version: mỗi phiên tối đa một báo cáo OPEN là chỉ mục một phần của DB (uq_reports_one_open).
 */
@Entity
@Table(name = "incident_reports")
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, updatable = false)
    private Long sessionId;

    @Column(name = "reporter_id", nullable = false, updatable = false)
    private Long reporterId;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    // DEFAULT 'OPEN' trong DB → gán ở field (D30).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportStatus status = ReportStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReportResolution resolution;

    @Column(name = "admin_note", columnDefinition = "text")
    private String adminNote;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    // Lấy từ Clock khi tạo (D88).
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected IncidentReport() {
        // JPA cần constructor không tham số để dựng object khi đọc từ DB.
    }

    /** Báo cáo mới ở trạng thái OPEN. reason đã được service cắt khoảng trắng và kiểm độ dài (D88). */
    public static IncidentReport open(Long sessionId, Long reporterId, String reason, Instant createdAt) {
        IncidentReport report = new IncidentReport();
        report.sessionId = sessionId;
        report.reporterId = reporterId;
        report.reason = reason;
        report.createdAt = createdAt;
        return report;
    }

    public Long getId() {
        return id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public String getReason() {
        return reason;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public ReportResolution getResolution() {
        return resolution;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public Long getResolvedBy() {
        return resolvedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
