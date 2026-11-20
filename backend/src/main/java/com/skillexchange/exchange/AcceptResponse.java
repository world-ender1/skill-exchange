package com.skillexchange.exchange;

/**
 * Response của POST /api/requests/{id}/accept (D90, bảng 2.2, R38): chỉ hai mã, đủ để giao diện chuyển từ
 * hộp yêu cầu (S08) sang trang phiên vừa tạo (S09). Không phải RequestItem.
 */
public record AcceptResponse(long requestId, long sessionId) {
}
