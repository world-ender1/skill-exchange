package com.skillexchange.skill;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Kỹ năng – bảng skills (V1__init.sql). */
@Entity
@Table(name = "skills")
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nhiều kỹ năng thuộc một nhóm; cột khóa ngoại là category_id.
    // LAZY: chỉ đọc nhóm khi cần (query danh sách dùng JOIN FETCH để lấy luôn).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private SkillCategory category;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // D30: cột có DEFAULT trong DB phải gán mặc định ở field Java,
    // vì Hibernate luôn ghi đủ mọi cột khi INSERT.
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected Skill() {
    }

    public Long getId() {
        return id;
    }

    public SkillCategory getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }
}
