package com.skillexchange.exchange;

/** Trạng thái phiên – cột exchange_sessions.status (TK 5.2). COMPLETED, CANCELLED là trạng thái cuối. */
public enum SessionStatus {
    SCHEDULED,
    IN_PROGRESS,
    WAITING_CONFIRMATION,
    COMPLETED,
    CANCELLED
}
