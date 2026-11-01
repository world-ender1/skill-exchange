package com.skillexchange.auth;

import com.skillexchange.user.User;
import com.skillexchange.user.UserStatus;

/** 201 của đăng ký: {id, email, status} (D29). Không có password_hash. */
public record RegisterResponse(Long id, String email, UserStatus status) {

    static RegisterResponse from(User user) {
        return new RegisterResponse(user.getId(), user.getEmail(), user.getStatus());
    }
}
