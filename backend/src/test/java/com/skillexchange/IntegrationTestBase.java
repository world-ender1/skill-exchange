package com.skillexchange;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Lớp nền cho mọi test tích hợp (D39).
 * - Chạy cả ứng dụng thật trên database skillexchange_test (profile "test").
 * - MockMvc gửi request HTTP giả vào controller, không cần mở cổng 8080.
 * - Trước mỗi test xóa dữ liệu người dùng; danh mục kỹ năng do V2 nạp được giữ nguyên.
 * - Đồng hồ giả (D18) đặt lại về START trước mỗi test; test vặn bằng clock.advance(...).
 * - JavaMailSender là mock: không gửi thư thật, test đọc lại thư "đã gửi" bằng sentMails().
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestBase.TestClockConfig.class)
public abstract class IntegrationTestBase {

    protected static final Instant START = Instant.parse("2026-11-02T08:00:00Z");

    /** Thay bean Clock thật (ClockConfig) bằng đồng hồ giả; @Primary để Spring chọn bean này. */
    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(START, ZoneOffset.UTC);
        }
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected MutableClock clock;

    // Mock tự được đặt lại (quên các lần gọi cũ) sau mỗi test.
    @MockitoBean
    protected JavaMailSender mailSender;

    @BeforeEach
    void cleanUserData() {
        // CASCADE kéo theo mọi bảng tham chiếu tới users; skills và skill_categories không bị đụng.
        jdbcTemplate.execute("TRUNCATE users RESTART IDENTITY CASCADE");
        clock.setInstant(START);
    }

    /** Các thư đã "gửi" qua mock, theo thứ tự gửi. */
    protected List<SimpleMailMessage> sentMails() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, atLeast(0)).send(captor.capture());
        return captor.getAllValues();
    }
}
