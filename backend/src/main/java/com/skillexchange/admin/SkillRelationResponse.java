package com.skillexchange.admin;

/**
 * Một cặp kỹ năng liên quan: {skillAId, skillAName, skillBId, skillBName}, luôn skillAId < skillBId
 * như cách bảng skill_relations lưu (CHECK skill_a_id < skill_b_id, D73).
 */
public record SkillRelationResponse(long skillAId, String skillAName, long skillBId, String skillBName) {
}
