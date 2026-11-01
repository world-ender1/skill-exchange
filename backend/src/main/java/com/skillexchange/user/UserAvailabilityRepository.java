package com.skillexchange.user;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAvailabilityRepository extends JpaRepository<UserAvailability, UserAvailability.Key> {

    List<UserAvailability> findByUserId(Long userId);

    /**
     * Xóa cả lịch của một người bằng MỘT câu DELETE (D30), trước khi chèn lịch mới.
     * Không dùng deleteAll(list): nó xóa từng dòng, và Hibernate có thể chèn dòng mới
     * trước khi xóa dòng cũ → trùng khóa chính.
     * flushAutomatically: ghi các thay đổi đang chờ xuống DB trước khi xóa;
     * clearAutomatically: quên các Entity cũ trong bộ nhớ sau khi xóa.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM UserAvailability a WHERE a.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
