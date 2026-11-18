package com.skillexchange.exchange;

/** Trạng thái yêu cầu – cột exchange_requests.status (TK 5.1). ACCEPTED, REJECTED, CANCELLED là trạng thái cuối. */
public enum RequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED
}
