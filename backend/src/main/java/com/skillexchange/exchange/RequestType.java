package com.skillexchange.exchange;

/** Loại yêu cầu trao đổi – cột exchange_requests.type (spec 6.4). */
public enum RequestType {
    /** Gửi từ nhóm ghép hai chiều: có kỹ năng người nhận muốn học. */
    TWO_WAY,
    /** Mentoring một chiều: không có kỹ năng chiều ngược lại. */
    ONE_WAY
}
