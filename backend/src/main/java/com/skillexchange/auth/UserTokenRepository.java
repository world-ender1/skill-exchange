package com.skillexchange.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    Optional<UserToken> findByTokenHash(String tokenHash);

    /** Token mới nhất của một mục đích; dùng cho giới hạn 60 giây (D7). */
    Optional<UserToken> findFirstByUserIdAndPurposeOrderByCreatedAtDesc(Long userId, TokenPurpose purpose);

    /**
     * D7: tạo token mới thì xóa các token chưa dùng cùng mục đích. Một câu DELETE thay vì đọc rồi xóa từng dòng.
     * flushAutomatically: ghi các thay đổi đang chờ (ví dụ user vừa tạo) xuống DB trước khi xóa.
     * Cố ý KHÔNG dùng clearAutomatically: nó làm JPA quên mọi Entity trong transaction, kể cả User đang xử lý,
     * khiến thay đổi sau đó trên User không được lưu. Sau câu xóa không ai đọc lại token cũ nên không cần.
     */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM UserToken t WHERE t.user.id = :userId AND t.purpose = :purpose AND t.usedAt IS NULL")
    void deleteUnused(@Param("userId") Long userId, @Param("purpose") TokenPurpose purpose);
}
