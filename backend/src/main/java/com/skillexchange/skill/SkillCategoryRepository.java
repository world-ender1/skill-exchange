package com.skillexchange.skill;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillCategoryRepository extends JpaRepository<SkillCategory, Long> {

    // Spring Data tự sinh câu SQL từ tên method: ... ORDER BY name ASC, id ASC (D40).
    List<SkillCategory> findAllByOrderByNameAscIdAsc();
}
