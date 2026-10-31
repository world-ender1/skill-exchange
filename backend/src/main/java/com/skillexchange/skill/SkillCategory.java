package com.skillexchange.skill;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Nhóm kỹ năng – bảng skill_categories (V1__init.sql). */
@Entity
@Table(name = "skill_categories")
public class SkillCategory {

    // BIGINT GENERATED ALWAYS AS IDENTITY → Long + IDENTITY (DB tự sinh id).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // JPA cần constructor không tham số để tạo object khi đọc từ DB.
    protected SkillCategory() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
