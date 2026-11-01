package com.skillexchange.auth;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillexchange.common.ConflictException;
import com.skillexchange.common.FieldValidationException;
import com.skillexchange.user.User;
import com.skillexchange.user.UserRepository;
import com.skillexchange.user.UserStatus;

@Service
public class AuthService {

    /** BCrypt chỉ dùng 72 byte đầu của mật khẩu (D10). */
    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailDomainProperties emailDomains;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailDomainProperties emailDomains) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailDomains = emailDomains;
    }

    /**
     * Đăng ký (D8, D9, D10). Email trong request đã được chuẩn hóa (RegisterRequest).
     * - Email mới: tạo tài khoản PENDING_VERIFICATION.
     * - Email đang PENDING_VERIFICATION: ghi đè mật khẩu và họ tên (D8), trả 201 như đăng ký mới.
     * - Email đã ACTIVE hoặc LOCKED: 409.
     * Bước 5 sẽ thêm: tạo token và gửi thư sau khi commit.
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        checkSchoolEmail(request.email());
        checkPasswordByteLimit(request.password());

        Optional<User> existing = userRepository.findByEmail(request.email());
        if (existing.isPresent() && existing.get().getStatus() != UserStatus.PENDING_VERIFICATION) {
            // Cùng một thông báo cho ACTIVE và LOCKED: không cho người ngoài biết tài khoản bị khóa.
            throw new ConflictException("Email này đã được đăng ký");
        }

        // Băm sau khi đã chắc chắn sẽ lưu: BCrypt cố ý chậm (~0,1 giây), không băm thừa khi trả 409.
        String passwordHash = passwordEncoder.encode(request.password());

        User user;
        if (existing.isPresent()) {
            user = existing.get();
            // Entity đang được JPA quản lý trong transaction: đổi field là đủ,
            // Hibernate tự UPDATE khi commit (dirty checking), không cần gọi save().
            user.overwritePendingRegistration(passwordHash, request.fullName());
        } else {
            user = userRepository.save(User.register(request.email(), passwordHash, request.fullName()));
        }
        return RegisterResponse.from(user);
    }

    private void checkSchoolEmail(String email) {
        if (!emailDomains.isSchoolEmail(email)) {
            throw new FieldValidationException("email",
                    "Chỉ nhận email trường có đuôi: " + String.join(", ", emailDomains.allowedEmailDomains()));
        }
    }

    private static void checkPasswordByteLimit(String password) {
        // Chữ có dấu chiếm 2–3 byte UTF-8: 72 ký tự có thể vượt 72 byte (D10).
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new FieldValidationException("password",
                    "Mật khẩu quá dài: tối đa 72 byte (chữ có dấu tính 2–3 byte)");
        }
    }
}
