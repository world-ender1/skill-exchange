package com.skillexchange.matching;

/**
 * Một cặp kỹ năng được admin đánh dấu liên quan (bảng skill_relations, Sim = 0.5).
 * Thứ tự hai id không quan trọng: SkillSimilarity tra được cả hai chiều.
 */
public record SkillRelation(long skillA, long skillB) {
}
