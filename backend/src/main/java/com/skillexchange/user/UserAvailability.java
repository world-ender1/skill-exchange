package com.skillexchange.user;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Một khung giờ rảnh – bảng user_availability (V1__init.sql).
 * Khóa chính gồm cả 3 cột (user_id, day_of_week, period), nên dùng @IdClass (D30):
 * mỗi field có @Id, và lớp Key liệt kê đúng các field đó.
 */
@Entity
@Table(name = "user_availability")
@IdClass(UserAvailability.Key.class)
public class UserAvailability {

    /** Khóa ghép: tên và kiểu phải trùng với các field @Id bên dưới. */
    public record Key(Long userId, Short dayOfWeek, Period period) implements Serializable {
    }

    @Id
    @Column(name = "user_id")
    private Long userId;

    // SMALLINT → Short (D30). 1 = Thứ Hai ... 7 = Chủ nhật.
    @Id
    @Column(name = "day_of_week")
    private Short dayOfWeek;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Period period;

    protected UserAvailability() {
    }

    public UserAvailability(Long userId, Short dayOfWeek, Period period) {
        this.userId = userId;
        this.dayOfWeek = dayOfWeek;
        this.period = period;
    }

    public Short getDayOfWeek() {
        return dayOfWeek;
    }

    public Period getPeriod() {
        return period;
    }
}
