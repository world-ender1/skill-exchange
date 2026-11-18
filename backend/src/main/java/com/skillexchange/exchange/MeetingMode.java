package com.skillexchange.exchange;

/**
 * Hình thức của một buổi học – cột mode của exchange_requests và exchange_sessions.
 * Khác LearningMode của hồ sơ: buổi học phải chốt một hình thức nên không có ANY (TK 3.3, spec 6.4).
 */
public enum MeetingMode {
    ONLINE,
    OFFLINE
}
