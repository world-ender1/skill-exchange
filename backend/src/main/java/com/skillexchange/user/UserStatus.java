package com.skillexchange.user;

/** Cột users.status: CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'LOCKED')). */
public enum UserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    LOCKED
}
