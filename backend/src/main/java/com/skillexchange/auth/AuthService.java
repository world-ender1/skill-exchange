package com.skillexchange.auth;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillexchange.common.ConflictException;
import com.skillexchange.common.FieldValidationException;
import com.skillexchange.common.InvalidRequestException;
import com.skillexchange.user.User;
import com.skillexchange.user.UserRepository;
import com.skillexchange.user.UserStatus;

@Service
public class AuthService {

    /** BCrypt chỉ dùng 72 byte đầu của mật khẩu (D10). */
    private static final int BCRYPT_MAX_BYTES = 72;

    /** D26: mọi kiểu link hỏng dùng chung một câu, người ngoài không phân biệt được lý do. */
    static final String INVALID_VERIFY_LINK = "Link xác thực không hợp lệ hoặc đã hết hạn";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailDomainProperties emailDomains;
    private final TokenService tokenService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailDomainProperties emailDomains,
                       TokenService tokenService,
                       ApplicationEventPublisher events,
                       Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailDomains = emailDomains;
        this.tokenService = tokenService;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Đăng ký (D8, D9, D10). Email trong request đã được chuẩn hóa (RegisterRequest).
     * - Email mới: tạo tài khoản PENDING_VERIFICATION.
     * - Email đang PENDING_VERIFICATION: ghi đè mật khẩu và họ tên (D8), trả 201 như đăng ký mới.
     * - Email đã ACTIVE hoặc LOCKED: 409.
     * Tài khoản mới: tạo token và gửi thư xác thực sau khi commit (D27).
     * Đăng ký lại: chỉ gửi thư mới nếu đã qua 60 giây kể từ thư trước (D7).
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
            // D8 + D7: ghi đè luôn xảy ra; thư mới chỉ khi đã qua 60 giây kể từ thư trước.
            if (tokenService.canIssueVerifyEmailToken(user)) {
                sendVerificationMail(user);
            }
        } else {
            user = userRepository.save(User.register(request.email(), passwordHash, request.fullName()));
            sendVerificationMail(user);
        }
        return RegisterResponse.from(user);
    }

    /**
     * Gửi lại link xác thực (D7). Controller luôn trả 204: chỉ gửi thư khi tài khoản đang
     * PENDING_VERIFICATION và đã qua 60 giây. Mọi trường hợp khác im lặng không làm gì,
     * để người ngoài không dò được email nào đã đăng ký hay bị khóa.
     */
    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmail(email)
                .filter(user -> user.getStatus() == UserStatus.PENDING_VERIFICATION)
                .filter(tokenService::canIssueVerifyEmailToken)
                .ifPresent(this::sendVerificationMail);
    }

    /**
     * Xác thực email bằng token trong link (D26). Thành công: ACTIVE, ghi email_verified_at và used_at
     * bằng cùng một mốc từ Clock (D18). Thất bại vì bất kỳ lý do gì: 400 với cùng một thông báo.
     */
    @Transactional
    public void verifyEmail(String rawToken) {
        UserToken token = tokenService.findUsableVerifyEmailToken(rawToken)
                .filter(t -> t.getUser().getStatus() == UserStatus.PENDING_VERIFICATION)
                .orElseThrow(() -> new InvalidRequestException(INVALID_VERIFY_LINK));

        var now = clock.instant();
        token.getUser().verifyEmail(now);
        token.markUsed(now);
    }

    /**
     * Tạo token mới và "đặt lịch" gửi thư. publishEvent chỉ ghi nhận sự kiện;
     * VerificationMailer thật sự gửi sau khi transaction này commit.
     */
    private void sendVerificationMail(User user) {
        String rawToken = tokenService.issueVerifyEmailToken(user);
        events.publishEvent(new VerificationMailRequested(user.getEmail(), rawToken));
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
