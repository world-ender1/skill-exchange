package com.skillexchange.exchange;

import org.springframework.data.jpa.repository.JpaRepository;

/** Truy cập exchange_requests. Truy vấn danh sách (JOIN tên người, tên kỹ năng) thêm ở bước 5–6. */
public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, Long> {
}
