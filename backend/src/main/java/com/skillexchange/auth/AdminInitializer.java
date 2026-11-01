package com.skillexchange.auth;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.skillexchange.user.Role;
import com.skillexchange.user.User;
import com.skillexchange.user.UserRepository;

/**
 * Tạo tài khoản admin đầu tiên lúc khởi động (D4).
 *
 * ApplicationRunner: Spring gọi run() MỘT lần, sau khi đã tạo xong mọi bean (DB, Flyway đã chạy)
 * và trước khi ứng dụng được coi là "đã khởi động". Giống đoạn code đặt cuối hàm main.
 * run() ném exception thì ứng dụng dừng: đó là cách "thiếu cấu hình thì không chạy" (bước 7).
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private static final String ADMIN_FULL_NAME = "Quản trị viên";
    private static final int MAX_EMAIL_LENGTH = 255;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;
    private static final int MAX_PASSWORD_BYTES = 72;

    /** Kiểm định dạng ở mức đơn giản: có đúng một @, hai bên không rỗng, không có khoảng trắng. */
    private static final Pattern SIMPLE_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+$");

    private final AdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AdminInitializer(AdminProperties properties,
                            UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            Clock clock) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = validEmail(properties.email());
        String password = validPassword(properties.password());

        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            // Đã có tài khoản với email này: không tạo, không sửa (đổi ADMIN_PASSWORD không đổi mật khẩu trong DB).
            if (existing.get().getRole() != Role.ADMIN) {
                // Không tự nâng quyền: ai đăng ký trước bằng email này không được thành admin.
                log.warn("ADMIN_EMAIL {} đã thuộc một tài khoản không phải admin; không tạo admin", email);
            }
            return;
        }

        userRepository.save(User.createAdmin(email, passwordEncoder.encode(password), ADMIN_FULL_NAME, clock.instant()));
        log.info("Đã tạo tài khoản admin đầu tiên: {}", email); // chỉ ghi email, không bao giờ ghi mật khẩu
    }

    private static String validEmail(String rawEmail) {
        String email = Emails.normalize(rawEmail); // D9: cùng cách chuẩn hóa như đăng ký
        if (email == null || email.isEmpty()) {
            throw new IllegalStateException("Thiếu ADMIN_EMAIL trong .env: cần email của tài khoản admin đầu tiên");
        }
        if (email.length() > MAX_EMAIL_LENGTH || !SIMPLE_EMAIL.matcher(email).matches()) {
            throw new IllegalStateException("ADMIN_EMAIL không phải email hợp lệ (tối đa 255 ký tự)");
        }
        return email;
    }

    /** Cùng luật với mật khẩu sinh viên (D10). Thông báo lỗi KHÔNG chứa giá trị mật khẩu. */
    private static String validPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalStateException("Thiếu ADMIN_PASSWORD trong .env: cần mật khẩu của tài khoản admin đầu tiên");
        }
        boolean lengthOk = password.length() >= MIN_PASSWORD_LENGTH && password.length() <= MAX_PASSWORD_LENGTH;
        boolean bytesOk = password.getBytes(StandardCharsets.UTF_8).length <= MAX_PASSWORD_BYTES;
        if (!lengthOk || !bytesOk) {
            throw new IllegalStateException("ADMIN_PASSWORD phải dài 8–72 ký tự và không quá 72 byte (D10)");
        }
        return password;
    }
}
