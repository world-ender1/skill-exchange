package com.skillexchange.auth;

import java.time.Duration;
import java.time.Instant;

import com.skillexchange.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Token dùng một lần (link xác thực email) – bảng user_tokens (V1__init.sql).
 * Chỉ lưu bản băm SHA-256 của token (D6); token gốc chỉ nằm trong thư.
 */
@Entity
@Table(name = "user_tokens")
public class UserToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TokenPurpose purpose;

    @Column(name = "token_hash", nullable = false, unique = true, length = 100)
    private String tokenHash;

    // D18: ba mốc dưới đều gán từ Clock (không @CreationTimestamp), để test vặn được đồng hồ.
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserToken() {
    }

    static UserToken issue(User user, TokenPurpose purpose, String tokenHash, Instant now, Duration lifetime) {
        UserToken token = new UserToken();
        token.user = user;
        token.purpose = purpose;
        token.tokenHash = tokenHash;
        token.createdAt = now;
        token.expiresAt = now.plus(lifetime);
        return token;
    }

    /** Chưa dùng và chưa hết hạn. Đúng lúc expires_at là đã hết hạn (chốt bước 5). */
    boolean isUsableAt(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    void markUsed(Instant now) {
        this.usedAt = now;
    }

    User getUser() {
        return user;
    }

    TokenPurpose getPurpose() {
        return purpose;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
