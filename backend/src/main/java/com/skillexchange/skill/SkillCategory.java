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

    /** Nhóm mới (A01, D73). name đã được cắt khoảng trắng và kiểm trùng ở service. */
    public static SkillCategory create(String name) {
        SkillCategory category = new SkillCategory();
        category.name = name;
        return category;
    }

    /** Đổi tên. Không cần gọi save(): Entity đang được quản lý, Hibernate tự UPDATE khi commit. */
    public void rename(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
