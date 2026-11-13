package com.skillexchange.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body của POST /api/admin/skill-categories và PUT /api/admin/skill-categories/{id}: {name} (D73). */
public record CategoryRequest(

        @NotBlank(message = "Tên không được để trống")
        @Size(max = 100, message = "Tên dài tối đa 100 ký tự")
        String name) {

    /** Cắt khoảng trắng hai đầu TRƯỚC khi @Valid kiểm, nên độ dài 100 tính sau khi cắt (D74, D15). */
    public CategoryRequest {
        name = name == null ? null : name.strip();
    }
}
