package com.skillexchange.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.skillexchange.IntegrationTestBase;
import com.skillexchange.user.User;
import com.skillexchange.user.UserRepository;

/**
 * Gửi lại link, giới hạn 60 giây, đăng ký lại email chưa xác thực (bước 5c):
 * TC07, TC42, TC43, TC72, TC77, TC78, TC80 (phần gửi lại), TC89, TC91; D7, D9, D42.
 */
class ResendVerificationTest extends IntegrationTestBase {

    private static final String EMAIL = "an@student.example.edu.vn";
    private static final Pattern TOKEN = Pattern.compile("verify-email\\?token=([A-Za-z0-9_-]+)");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---------- Hàm tiện ích ----------

    private ResultActions register(String password, String fullName) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + password
                        + "\",\"fullName\":\"" + fullName + "\"}"));
    }

    private ResultActions resend(String body) throws Exception {
        return mockMvc.perform(post("/api/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions resendFor(String email) throws Exception {
        return resend("{\"email\":\"" + email + "\"}");
    }

    private ResultActions verifyEmail(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\"}"));
    }

    /** Token trong thư thứ index (đếm từ 0) đã gửi. */
    private String tokenOfMail(int index) {
        Matcher matcher = TOKEN.matcher(sentMails().get(index).getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String passwordHash() {
        return jdbcTemplate.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, EMAIL);
    }

    /** Tạo user bằng Repository, không qua API nên không có thư nào (TC72). */
    private void createUser(String email, String status) {
        User user = User.register(email, passwordEncoder.encode("matkhau123"), "Có Sẵn");
        userRepository.save(user);
        jdbcTemplate.update("UPDATE users SET status = ? WHERE email = ?", status, email);
    }

    // ---------- Gửi lại link ----------

    @Test
    @DisplayName("TC07 gửi lại link sau hơn 60 giây: 204, có thư mới, token mới verify được")
    void tc07_resendAfter60Seconds() throws Exception {
        register("matkhau123", "An").andExpect(status().isCreated());
        clock.advance(Duration.ofSeconds(61));

        resendFor(EMAIL).andExpect(status().isNoContent());

        assertThat(sentMails()).hasSize(2);
        verifyEmail(tokenOfMail(1)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("TC42 sau khi gửi lại link, token cũ bị từ chối (400), token mới vẫn dùng được")
    void tc42_oldTokenInvalidAfterResend() throws Exception {
        register("matkhau123", "An").andExpect(status().isCreated());
        clock.advance(Duration.ofSeconds(61));
        resendFor(EMAIL).andExpect(status().isNoContent());

        verifyEmail(tokenOfMail(0)).andExpect(status().isBadRequest());
        verifyEmail(tokenOfMail(1)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("TC43 gửi lại hai lần cách nhau 30 giây (sau 60 giây đầu): cả hai 204, chỉ lần đầu có thư")
    void tc43_secondResendWithin60SecondsSendsNothing() throws Exception {
        register("matkhau123", "An").andExpect(status().isCreated());
        clock.advance(Duration.ofSeconds(61));
        resendFor(EMAIL).andExpect(status().isNoContent());
        clock.advance(Duration.ofSeconds(30));
        resendFor(EMAIL).andExpect(status().isNoContent());

        assertThat(sentMails()).hasSize(2); // thư đăng ký + đúng 1 thư gửi lại
    }

    @Test
    @DisplayName("TC89 gửi lại 30 giây sau khi đăng ký: 204, không có thư mới (thư đăng ký cũng tính)")
    void tc89_resendTooSoonAfterRegister() throws Exception {
        register("matkhau123", "An").andExpect(status().isCreated());
        clock.advance(Duration.ofSeconds(30));

        resendFor(EMAIL).andExpect(status().isNoContent());
        assertThat(sentMails()).hasSize(1);
    }

    @Test
    @DisplayName("D7 đúng 60 giây sau thư trước: được gửi (cần ít nhất 60 giây)")
    void d7_exactly60SecondsIsAllowed() throws Exception {
        register("matkhau123", "An").andExpect(status().isCreated());
        clock.advance(Duration.ofSeconds(60));

        resendFor(EMAIL).andExpect(status().isNoContent());
        assertThat(sentMails()).hasSize(2);
    }

    @Test
    @DisplayName("D9 gửi lại với email viết hoa, có khoảng trắng: vẫn tìm đúng tài khoản")
    void d9_resendNormalizesEmail() throws Exception {
        register("matkhau123", "An").andExpect(status().isCreated());
        clock.advance(Duration.ofSeconds(61));

        resendFor("  An@Student.Example.EDU.vn ").andExpect(status().isNoContent());
        assertThat(sentMails()).hasSize(2);
        assertThat(sentMails().get(1).getTo()).containsExactly(EMAIL);
    }

    @Test
    @DisplayName("TC72 email chưa đăng ký, tài khoản ACTIVE, tài khoản LOCKED: cả ba 204, không gửi thư")
    void tc72_resendNeverRevealsAccountState() throws Exception {
        createUser("active@student.example.edu.vn", "ACTIVE");
        createUser("locked@student.example.edu.vn", "LOCKED");
        clock.advance(Duration.ofHours(1));

        resendFor("chua-dang-ky@student.example.edu.vn").andExpect(status().isNoContent());
        resendFor("active@student.example.edu.vn").andExpect(status().isNoContent());
        resendFor("locked@student.example.edu.vn").andExpect(status().isNoContent());

        assertThat(sentMails()).isEmpty();
    }

    @Test
    @DisplayName("TC91 body {}, email rỗng, email \"abc\": 400 Problem Details, errors[].field = email, không gửi thư")
    void tc91_invalidEmailIs400() throws Exception {
        for (String body : new String[] {"{}", "{\"email\":\"\"}", "{\"email\":\"abc\"}"}) {
            resend(body)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.errors[0].field").value("email"));
        }
        assertThat(sentMails()).isEmpty();
    }

    @Test
    @DisplayName("D42 email dài hơn 255 ký tự: 400")
    void d42_tooLongEmailIs400() throws Exception {
        String longEmail = "a".repeat(250) + "@student.example.edu.vn";
        resendFor(longEmail).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    @DisplayName("TC80 (phần gửi lại) thư đăng ký gửi lỗi; sau khi máy chủ thư chạy lại, gửi lại (sau 60 giây) được")
    void tc80_resendWorksAfterMailServerRecovers() throws Exception {
        doThrow(new MailSendException("Mailpit đang tắt")).when(mailSender).send(any(SimpleMailMessage.class));
        register("matkhau123", "An").andExpect(status().isCreated());

        doNothing().when(mailSender).send(any(SimpleMailMessage.class)); // máy chủ thư chạy lại
        clock.advance(Duration.ofSeconds(61));
        resendFor(EMAIL).andExpect(status().isNoContent());

        assertThat(sentMails()).hasSize(2); // lần 1 lỗi, lần 2 thành công
        verifyEmail(tokenOfMail(1)).andExpect(status().isNoContent());
    }

    // ---------- Đăng ký lại email chưa xác thực (D8 + D7) ----------

    @Test
    @DisplayName("TC77 đăng ký lại sau hơn 60 giây: 201, ghi đè; token thư đầu 400, token thư mới 204")
    void tc77_reRegisterAfter60SecondsIssuesNewToken() throws Exception {
        register("matkhau111", "Kẻ Giả").andExpect(status().isCreated());
        clock.advance(Duration.ofSeconds(61));

        register("matkhau222", "An Thật").andExpect(status().isCreated());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM users", Long.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT full_name FROM users", String.class)).isEqualTo("An Thật");
        // Phần "đăng nhập bằng mật khẩu cũ/mới" kiểm ở bước 6; ở đây so trực tiếp với password_hash.
        assertThat(passwordEncoder.matches("matkhau222", passwordHash())).isTrue();
        assertThat(passwordEncoder.matches("matkhau111", passwordHash())).isFalse();

        assertThat(sentMails()).hasSize(2);
        verifyEmail(tokenOfMail(0)).andExpect(status().isBadRequest());
        verifyEmail(tokenOfMail(1)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("TC78 đăng ký lại sau 30 giây: 201, mật khẩu ghi đè, không có thư mới, token thư đầu vẫn dùng được")
    void tc78_reRegisterWithin60SecondsKeepsOldToken() throws Exception {
        register("matkhau111", "An").andExpect(status().isCreated());
        clock.advance(Duration.ofSeconds(30));

        register("matkhau222", "An").andExpect(status().isCreated());

        assertThat(passwordEncoder.matches("matkhau222", passwordHash())).isTrue();
        assertThat(sentMails()).hasSize(1);
        verifyEmail(tokenOfMail(0)).andExpect(status().isNoContent());
    }
}
