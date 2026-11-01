package com.skillexchange.user;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserLearnSkillRepository extends JpaRepository<UserLearnSkill, UserLearnSkill.Key> {

    @Query("SELECT l.skillId FROM UserLearnSkill l WHERE l.userId = :userId")
    List<Long> findSkillIdsByUserId(@Param("userId") Long userId);

    /** Như UserTeachSkillRepository.findResponsesByUserId, với targetLevel (có thể null). */
    @Query("""
            SELECT new com.skillexchange.user.LearnSkillResponse(s.id, s.name, c.id, c.name, s.active, l.targetLevel)
            FROM UserLearnSkill l
            JOIN Skill s ON s.id = l.skillId
            JOIN s.category c
            WHERE l.userId = :userId
            ORDER BY s.name, s.id
            """)
    List<LearnSkillResponse> findResponsesByUserId(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM UserLearnSkill l WHERE l.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
