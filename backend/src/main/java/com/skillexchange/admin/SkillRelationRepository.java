package com.skillexchange.admin;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Đọc ghi bảng skill_relations bằng SQL thuần (kế hoạch tuần 5, bước 9).
 *
 * Không làm Entity: bảng chỉ có hai cột khóa, và MatchingDataRepository đã đọc bảng này bằng SQL,
 * nên giữ một cách truy cập duy nhất. Mỗi cặp lưu đúng một dòng với skill_a_id < skill_b_id (CHECK của V1);
 * các câu lệnh dùng LEAST/GREATEST như V2 để nhận hai mã theo thứ tự bất kỳ.
 */
@Repository
public class SkillRelationRepository {

    private final JdbcTemplate jdbcTemplate;

    public SkillRelationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Mọi cặp kèm tên hai kỹ năng, xếp theo skillAId rồi skillBId (D73). Một câu SQL, không N+1. */
    public List<SkillRelationResponse> findAll() {
        return jdbcTemplate.query("""
                SELECT r.skill_a_id, a.name AS skill_a_name, r.skill_b_id, b.name AS skill_b_name
                FROM skill_relations r
                JOIN skills a ON a.id = r.skill_a_id
                JOIN skills b ON b.id = r.skill_b_id
                ORDER BY r.skill_a_id, r.skill_b_id
                """,
                (rs, rowNum) -> new SkillRelationResponse(
                        rs.getLong("skill_a_id"), rs.getString("skill_a_name"),
                        rs.getLong("skill_b_id"), rs.getString("skill_b_name")));
    }

    public boolean exists(long firstId, long secondId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM skill_relations
                WHERE skill_a_id = LEAST(?, ?) AND skill_b_id = GREATEST(?, ?)
                """, Integer.class, firstId, secondId, firstId, secondId);
        return count != null && count > 0;
    }

    /** Kỹ năng có nằm trong ít nhất một cặp không, ở vị trí A hay B (luật đổi nhóm, D74/R42). */
    public boolean existsForSkill(long skillId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM skill_relations
                WHERE skill_a_id = ? OR skill_b_id = ?
                """, Integer.class, skillId, skillId);
        return count != null && count > 0;
    }

    /** Hai request thêm cùng cặp chạy song song: request sau vấp PRIMARY KEY → DuplicateKeyException → 409. */
    public void insert(long firstId, long secondId) {
        jdbcTemplate.update("""
                INSERT INTO skill_relations (skill_a_id, skill_b_id)
                VALUES (LEAST(?, ?), GREATEST(?, ?))
                """, firstId, secondId, firstId, secondId);
    }

    /** @return số dòng đã xóa: 0 nghĩa là không có cặp này. */
    public int delete(long firstId, long secondId) {
        return jdbcTemplate.update("""
                DELETE FROM skill_relations
                WHERE skill_a_id = LEAST(?, ?) AND skill_b_id = GREATEST(?, ?)
                """, firstId, secondId, firstId, secondId);
    }
}
