package com.skillexchange.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body của POST /api/admin/skills: {name, categoryId}. Kỹ năng mới luôn đang hoạt động (D73). */
public record CreateSkillRequest(

        @NotBlank(message = "Tên không được để trống")
        @Size(max = 100, message = "Tên dài tối đa 100 ký tự")
        String name,

        // Long (không phải long): thiếu trường thì Jackson để null, @NotNull báo 400 theo trường categoryId.
        @NotNull(message = "Chưa chọn nhóm kỹ năng")
        Long categoryId) {

    public CreateSkillRequest {
        name = name == null ? null : name.strip();
    }
}
