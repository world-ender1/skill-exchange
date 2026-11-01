package com.skillexchange.user;

/**
 * Buổi trong ngày, cột user_availability.period: CHECK (period IN ('MORNING', 'AFTERNOON', 'EVENING')).
 * Thứ tự khai báo = thứ tự trong ngày; D41 sắp xếp theo thứ tự này (không theo chữ cái).
 */
public enum Period {
    MORNING,
    AFTERNOON,
    EVENING
}
