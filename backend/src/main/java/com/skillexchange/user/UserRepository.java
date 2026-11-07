package com.skillexchange.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    // Email luôn được chuẩn hóa (chữ thường, bỏ khoảng trắng) trước khi lưu và trước khi tìm (D9),
    // nên so sánh bằng "=" là đủ.
    Optional<User> findByEmail(String email);

    /**
     * Tìm người cho GET /api/users/search (TK 4.3, D37, D45): sinh viên ACTIVE, trừ người gọi,
     * xếp theo họ tên rồi id, tối đa 20 người.
     *
     * nativeQuery: ILIKE (so khớp không phân biệt hoa thường) là cú pháp riêng của PostgreSQL, JPQL không có.
     * namePattern đã qua LikePatterns (ký tự %, _, \ của người dùng đã được escape), nên cần ESCAPE '\'.
     * Tham số null = không lọc theo điều kiện đó. CAST để PostgreSQL biết kiểu của tham số khi giá trị là
     * null; thiếu CAST, driver báo "could not determine data type of parameter".
     * skillId so trùng chính xác trong danh sách DẠY; không lọc skills.is_active (spec 4.1, TC168).
     */
    @Query(nativeQuery = true, value = """
            SELECT u.*
            FROM users u
            WHERE u.status = 'ACTIVE' AND u.role = 'STUDENT'
              AND u.id <> :callerId
              AND (CAST(:namePattern AS text) IS NULL
                   OR u.full_name ILIKE CAST(:namePattern AS text) ESCAPE '\\')
              AND (CAST(:skillId AS bigint) IS NULL
                   OR EXISTS (SELECT 1 FROM user_teach_skills t
                              WHERE t.user_id = u.id AND t.skill_id = CAST(:skillId AS bigint)))
            ORDER BY u.full_name, u.id
            LIMIT 20
            """)
    List<User> searchActiveStudents(@Param("callerId") long callerId,
                                    @Param("namePattern") String namePattern,
                                    @Param("skillId") Long skillId);
}
