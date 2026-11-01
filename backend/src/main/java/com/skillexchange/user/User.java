package com.skillexchange.user;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tài khoản – bảng users (V1__init.sql). */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    // D30: cột có DEFAULT trong DB phải gán mặc định ở field,
    // vì Hibernate luôn ghi đủ mọi cột khi INSERT (DEFAULT của DB không được dùng).
    // EnumType.STRING: lưu tên hằng ("STUDENT"), khớp CHECK IN (...) của DB.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.STUDENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    // D18: mốc xác thực lấy từ bean Clock (truyền vào verifyEmail), không tự lấy giờ.
    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "learning_mode", length = 10)
    private LearningMode learningMode;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(length = 20)
    private String phone;

    @Column(name = "contact_info", columnDefinition = "text")
    private String contactInfo;

    // D18: created_at, updated_at do Hibernate ghi.
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    /** Tài khoản sinh viên mới, chưa xác thực email. passwordHash phải là bản đã băm. */
    public static User register(String email, String passwordHash, String fullName) {
        User user = new User();
        user.email = email;
        user.passwordHash = passwordHash;
        user.fullName = fullName;
        return user;
    }

    /** D8: đăng ký lại email chưa xác thực thì ghi đè mật khẩu và họ tên. */
    public void overwritePendingRegistration(String passwordHash, String fullName) {
        if (status != UserStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Chỉ ghi đè được tài khoản chưa xác thực email");
        }
        this.passwordHash = passwordHash;
        this.fullName = fullName;
    }

    /** Đánh dấu đã xác thực email (bước 5 gọi với thời điểm lấy từ Clock). */
    public void verifyEmail(Instant verifiedAt) {
        this.status = UserStatus.ACTIVE;
        this.emailVerifiedAt = verifiedAt;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public LearningMode getLearningMode() {
        return learningMode;
    }

    public String getBio() {
        return bio;
    }

    public String getPhone() {
        return phone;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
