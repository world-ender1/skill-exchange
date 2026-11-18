package com.skillexchange.exchange;

import org.springframework.data.jpa.repository.JpaRepository;

/** Truy cập exchange_sessions. Truy vấn danh sách và chi tiết thêm ở bước 7. */
public interface ExchangeSessionRepository extends JpaRepository<ExchangeSession, Long> {
}
