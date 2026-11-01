package com.skillexchange.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    // Email luôn được chuẩn hóa (chữ thường, bỏ khoảng trắng) trước khi lưu và trước khi tìm (D9),
    // nên so sánh bằng "=" là đủ.
    Optional<User> findByEmail(String email);
}
