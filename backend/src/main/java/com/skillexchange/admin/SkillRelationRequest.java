package com.skillexchange.admin;

import jakarta.validation.constraints.NotNull;

/** Body của POST /api/admin/skill-relations: {skillAId, skillBId} theo thứ tự bất kỳ (D73). */
public record SkillRelationRequest(

        @NotNull(message = "Chưa chọn kỹ năng")
        Long skillAId,

        @NotNull(message = "Chưa chọn kỹ năng")
        Long skillBId) {
}
