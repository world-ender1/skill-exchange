package com.skillexchange.user;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Một kỹ năng tôi dạy được – bảng user_teach_skills, khóa ghép (user_id, skill_id) (D30).
 * Chỉ giữ skillId, không map @ManyToOne tới Skill: khi cần tên kỹ năng thì
 * Repository JOIN và trả thẳng DTO trong một câu SQL.
 */
@Entity
@Table(name = "user_teach_skills")
@IdClass(UserTeachSkill.Key.class)
public class UserTeachSkill {

    public record Key(Long userId, Long skillId) implements Serializable {
    }

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "skill_id")
    private Long skillId;

    // SMALLINT NOT NULL CHECK (level BETWEEN 1 AND 4) → Short (D30)
    @Column(nullable = false)
    private Short level;

    protected UserTeachSkill() {
    }

    public UserTeachSkill(Long userId, Long skillId, Short level) {
        this.userId = userId;
        this.skillId = skillId;
        this.level = level;
    }
}
