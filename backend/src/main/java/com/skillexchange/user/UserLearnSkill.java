package com.skillexchange.user;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Một kỹ năng tôi muốn học – bảng user_learn_skills, khóa ghép (user_id, skill_id) (D30). */
@Entity
@Table(name = "user_learn_skills")
@IdClass(UserLearnSkill.Key.class)
public class UserLearnSkill {

    public record Key(Long userId, Long skillId) implements Serializable {
    }

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "skill_id")
    private Long skillId;

    // NULL = không đặt mục tiêu trình độ
    @Column(name = "target_level")
    private Short targetLevel;

    protected UserLearnSkill() {
    }

    public UserLearnSkill(Long userId, Long skillId, Short targetLevel) {
        this.userId = userId;
        this.skillId = skillId;
        this.targetLevel = targetLevel;
    }
}
