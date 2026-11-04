package com.skillexchange.matching;

import java.util.Objects;

import com.skillexchange.user.Period;

/**
 * Một khung giờ rảnh: ngày trong tuần (1 = Thứ Hai ... 7 = Chủ nhật) và buổi (spec 4.6, 21 khung).
 * Record tự sinh equals/hashCode theo cả hai trường, nên giao hai Set<Slot> bằng retainAll là đúng.
 */
public record Slot(int dayOfWeek, Period period) {

    public Slot {
        Objects.requireNonNull(period, "period");
    }
}
