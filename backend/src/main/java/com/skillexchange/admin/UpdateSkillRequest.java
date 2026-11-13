package com.skillexchange.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body của PUT /api/admin/skills/{id}: {name, categoryId, isActive}, bắt buộc đủ ba trường (D73).
 * Ẩn/hiện dùng chung endpoint này, không có endpoint riêng.
 */
public record UpdateSkillRequest(

        @NotBlank(message = "Tên không được để trống")
        @Size(max = 100, message = "Tên dài tối đa 100 ký tự")
        String name,

        @NotNull(message = "Chưa chọn nhóm kỹ năng")
        Long categoryId,

        // Boolean (không phải boolean): kiểu nguyên thủy sẽ lặng lẽ nhận false khi thiếu trường,
        // tức là thiếu isActive lại thành "ẩn kỹ năng" thay vì 400.
        @NotNull(message = "Thiếu trạng thái hiện/ẩn")
        Boolean isActive) {

    public UpdateSkillRequest {
        name = name == null ? null : name.strip();
    }
}
