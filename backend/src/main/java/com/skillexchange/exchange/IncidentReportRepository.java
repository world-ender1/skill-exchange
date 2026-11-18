package com.skillexchange.exchange;

import org.springframework.data.jpa.repository.JpaRepository;

/** Truy cập incident_reports. Tìm báo cáo OPEN của một phiên thêm ở bước 7–8. */
public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {
}
