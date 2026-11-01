package com.skillexchange.user;

/** Một khung giờ rảnh trả về: {dayOfWeek, period} (D28). */
public record AvailabilitySlotResponse(Short dayOfWeek, Period period) {

    static AvailabilitySlotResponse from(UserAvailability slot) {
        return new AvailabilitySlotResponse(slot.getDayOfWeek(), slot.getPeriod());
    }
}
