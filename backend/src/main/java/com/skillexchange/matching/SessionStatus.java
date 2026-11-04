package com.skillexchange.matching;

/** Trạng thái phiên, khớp CHECK của cột exchange_sessions.status trong V1__init.sql. */
public enum SessionStatus {
    SCHEDULED,
    IN_PROGRESS,
    WAITING_CONFIRMATION,
    COMPLETED,
    CANCELLED
}
