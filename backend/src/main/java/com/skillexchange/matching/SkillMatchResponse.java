package com.skillexchange.matching;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Một dòng của youLearn hoặc theyLearn (TK 4.5, D46): kỹ năng muốn học, kỹ năng dạy đã chọn, quan hệ, trình độ.
 * youLearn ghi trình độ của ứng viên (theirLevel), theyLearn ghi trình độ của người gọi (yourLevel).
 * Hai trường trình độ có NON_NULL nên mỗi dòng chỉ có đúng key của danh sách mình.
 */
public record SkillMatchResponse(
        String want,
        String teach,
        Relation relation,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer theirLevel,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer yourLevel) {

    /** EXACT: cùng kỹ năng (Sim = 1.0); RELATED: kỹ năng liên quan (Sim = 0.5). */
    public enum Relation {
        EXACT,
        RELATED
    }

    /** Người gọi học từ ứng viên: kỹ năng dạy là của ứng viên. */
    static SkillMatchResponse youLearn(SkillMatch match) {
        return new SkillMatchResponse(match.want().skillName(), match.teach().skillName(), relation(match),
                match.teach().level(), null);
    }

    /** Ứng viên học từ người gọi: kỹ năng dạy là của người gọi. */
    static SkillMatchResponse theyLearn(SkillMatch match) {
        return new SkillMatchResponse(match.want().skillName(), match.teach().skillName(), relation(match),
                null, match.teach().level());
    }

    private static Relation relation(SkillMatch match) {
        return match.exact() ? Relation.EXACT : Relation.RELATED;
    }
}
