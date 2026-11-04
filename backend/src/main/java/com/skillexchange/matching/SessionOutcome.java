package com.skillexchange.matching;

import java.util.Objects;

/**
 * Dữ liệu thô của một phiên mà người dùng đã tham gia (D43).
 * started = exchange_sessions.started_at IS NOT NULL. ReputationCalculator tự quyết phiên nào vào mẫu số.
 */
public record SessionOutcome(SessionStatus status, boolean started) {

    public SessionOutcome {
        Objects.requireNonNull(status, "status");
    }
}
