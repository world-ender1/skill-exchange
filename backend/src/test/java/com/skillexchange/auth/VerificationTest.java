package com.skillexchange.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.skillexchange.IntegrationTestBase;

/**
 * Thư có link và xác thực email (bước 5b): TC03, TC05, TC06, TC39–TC41, TC61, TC80 (phần đăng ký),
 * TC87, TC88; D18, D26, D27. Gửi lại link và giới hạn 60 giây ở ResendVerificationTest (5c).
 */
@ExtendWith(OutputCaptureExtension.class)
class VerificationTest extends IntegrationTestBase {

    private static final String EMAIL = "an@student.example.edu.vn";
    private static final Pattern LINK = Pattern.compile("http://localhost:5173/verify-email\\?token=([A-Za-z0-9_-]+)");

    // ---------- Hàm tiện ích ----------

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"matkhau123\",\"fullName\":\"Nguyễn An\"}"))
                .andExpect(status().isCreated());
    }

    /** Lấy token trong link của thư cuối cùng đã gửi. */
    private String tokenFromLastMail() {
        var mails = sentMails();
        assertThat(mails).isNotEmpty();
        Matcher matcher = LINK.matcher(mails.get(mails.size() - 1).getText());
        assertThat(matcher.find()).as("thư phải chứa link xác thực").isTrue();
        return matcher.group(1);
    }

    private ResultActions verifyEmail(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\"}"));
    }

    /** Gọi verify, mong đợi 400 Problem Details, trả về "detail" để so giữa các trường hợp (D26). */
    private String expectVerifyRejected(String token) throws Exception {
        String body = verifyEmail(token)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.detail");
    }

    private String userStatus(String email) {
        return jdbcTemplate.queryForObject("SELECT status FROM users WHERE email = ?", String.class, email);
    }

    private static String sha256Hex(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }

    // ---------- Thư khi đăng ký ----------

    @Test
    @DisplayName("TC03 đăng ký gửi đúng 1 thư tới email đó, có link ${FRONTEND_URL}/verify-email?token=...")
    void tc03_registerSendsVerificationMail() throws Exception {
        register(EMAIL);

        assertThat(sentMails()).hasSize(1);
        SimpleMailMessage mail = sentMails().get(0);
        assertThat(mail.getTo()).containsExactly(EMAIL);
        assertThat(mail.getText()).containsPattern(LINK);
    }

    @Test
    @DisplayName("TC61 DB không chứa token gốc; token_hash = SHA-256 hex của token trong thư")
    void tc61_onlyTokenHashIsStored() throws Exception {
        register(EMAIL);
        String token = tokenFromLastMail();

        String storedHash = jdbcTemplate.queryForObject("SELECT token_hash FROM user_tokens", String.class);
        assertThat(storedHash).isNotEqualTo(token).isEqualTo(sha256Hex(token)).hasSize(64);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_tokens WHERE token_hash = ?", Long.class, token)).isZero();
    }

    @Test
    @DisplayName("D18 created_at, expires_at của token lấy từ Clock: expires_at = created_at + 24 giờ")
    void d18_tokenTimesComeFromClock() throws Exception {
        register(EMAIL);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT purpose, created_at, expires_at, used_at FROM user_tokens");
        assertThat(row.get("purpose")).isEqualTo("VERIFY_EMAIL");
        assertThat(((Timestamp) row.get("created_at")).toInstant()).isEqualTo(START);
        assertThat(((Timestamp) row.get("expires_at")).toInstant()).isEqualTo(START.plus(Duration.ofHours(24)));
        assertThat(row.get("used_at")).isNull();
    }

    @Test
    @DisplayName("D27 thư chỉ được gửi SAU khi transaction đăng ký đã commit")
    void d27_mailIsSentAfterCommit() throws Exception {
        AtomicLong committedUsersWhenSending = new AtomicLong(-1);
        doAnswer(invocation -> {
            // JdbcTemplate dùng kết nối khác: chỉ thấy dữ liệu đã commit.
            committedUsersWhenSending.set(jdbcTemplate.queryForObject("SELECT count(*) FROM users", Long.class));
            return null;
        }).when(mailSender).send(any(SimpleMailMessage.class));

        register(EMAIL);

        assertThat(committedUsersWhenSending.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC80 (phần đăng ký) máy chủ thư lỗi: vẫn 201, user PENDING_VERIFICATION, có log lỗi")
    void tc80_mailFailureDoesNotFailRegistration(CapturedOutput output) throws Exception {
        doThrow(new MailSendException("Mailpit đang tắt")).when(mailSender).send(any(SimpleMailMessage.class));

        register(EMAIL);

        assertThat(userStatus(EMAIL)).isEqualTo("PENDING_VERIFICATION");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM user_tokens", Long.class)).isEqualTo(1);
        assertThat(output.getAll()).contains("ERROR").contains("Không gửi được thư xác thực");
    }

    // ---------- Xác thực thành công ----------

    @Test
    @DisplayName("TC05 verify bằng token trong thư: 204; user ACTIVE, email_verified_at và used_at được ghi")
    void tc05_verifySuccess() throws Exception {
        register(EMAIL);
        clock.advance(Duration.ofMinutes(5));

        verifyEmail(tokenFromLastMail()).andExpect(status().isNoContent());

        Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT status, email_verified_at FROM users WHERE email = ?", EMAIL);
        assertThat(user.get("status")).isEqualTo("ACTIVE");
        assertThat(((Timestamp) user.get("email_verified_at")).toInstant()).isEqualTo(START.plus(Duration.ofMinutes(5)));
        Timestamp usedAt = jdbcTemplate.queryForObject("SELECT used_at FROM user_tokens", Timestamp.class);
        assertThat(usedAt.toInstant()).isEqualTo(START.plus(Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("TC06 verify lúc 23 giờ 59 phút sau khi tạo token: 204, ACTIVE")
    void tc06_verifyJustBeforeExpiry() throws Exception {
        register(EMAIL);
        clock.advance(Duration.ofHours(23).plusMinutes(59));

        verifyEmail(tokenFromLastMail()).andExpect(status().isNoContent());
        assertThat(userStatus(EMAIL)).isEqualTo("ACTIVE");
    }

    // ---------- Xác thực thất bại: mọi trường hợp cùng một detail (D26) ----------

    @Test
    @DisplayName("TC39 + TC40 token không tồn tại: 400; token đã dùng: 400 cùng detail; không ai đổi trạng thái")
    void tc39_tc40_unknownAndUsedTokenRejectedWithSameDetail() throws Exception {
        register(EMAIL);
        String unknownDetail = expectVerifyRejected("khong-ton-tai-" + "x".repeat(30));
        assertThat(userStatus(EMAIL)).isEqualTo("PENDING_VERIFICATION");

        String token = tokenFromLastMail();
        verifyEmail(token).andExpect(status().isNoContent());
        String usedDetail = expectVerifyRejected(token);

        assertThat(usedDetail).isEqualTo(unknownDetail);
    }

    @Test
    @DisplayName("TC41 verify lúc 24 giờ 1 giây sau khi tạo token: 400, vẫn PENDING_VERIFICATION")
    void tc41_expiredTokenRejected() throws Exception {
        register(EMAIL);
        clock.advance(Duration.ofHours(24).plusSeconds(1));

        expectVerifyRejected(tokenFromLastMail());
        assertThat(userStatus(EMAIL)).isEqualTo("PENDING_VERIFICATION");
    }

    @Test
    @DisplayName("D26 verify đúng lúc 24 giờ (= expires_at): 400, đã hết hạn")
    void d26_tokenExpiresExactlyAt24Hours() throws Exception {
        register(EMAIL);
        clock.advance(Duration.ofHours(24));

        expectVerifyRejected(tokenFromLastMail());
    }

    @Test
    @DisplayName("TC87 token purpose = RESET_PASSWORD dùng cho verify-email: 400, vẫn PENDING_VERIFICATION")
    void tc87_resetPasswordTokenCannotVerifyEmail() throws Exception {
        register(EMAIL);
        String resetToken = "reset-token-" + "y".repeat(30);
        jdbcTemplate.update("""
                INSERT INTO user_tokens (user_id, purpose, token_hash, expires_at, created_at)
                SELECT id, 'RESET_PASSWORD', ?, ?, ? FROM users WHERE email = ?
                """, sha256Hex(resetToken),
                Timestamp.from(START.plus(Duration.ofHours(1))), Timestamp.from(START), EMAIL);

        expectVerifyRejected(resetToken);
        assertThat(userStatus(EMAIL)).isEqualTo("PENDING_VERIFICATION");
    }

    @Test
    @DisplayName("TC88 tài khoản bị chuyển LOCKED rồi verify bằng token hợp lệ: 400, vẫn LOCKED")
    void tc88_lockedUserCannotVerify() throws Exception {
        register(EMAIL);
        jdbcTemplate.update("UPDATE users SET status = 'LOCKED' WHERE email = ?", EMAIL);

        expectVerifyRejected(tokenFromLastMail());
        assertThat(userStatus(EMAIL)).isEqualTo("LOCKED");
    }

    @Test
    @DisplayName("D26 body thiếu token hoặc token rỗng: 400, errors[].field = token")
    void d26_blankTokenIsValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("token"));
        verifyEmail("").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("token"));
    }
}
