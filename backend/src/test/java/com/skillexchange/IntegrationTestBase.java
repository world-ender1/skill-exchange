package com.skillexchange;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Lớp nền cho mọi test tích hợp (D39).
 * - Chạy cả ứng dụng thật trên database skillexchange_test (profile "test").
 * - MockMvc gửi request HTTP giả vào controller, không cần mở cổng 8080.
 * - Trước mỗi test xóa dữ liệu người dùng; danh mục kỹ năng do V2 nạp được giữ nguyên.
 * Các bước sau sẽ mở rộng lớp này (đồng hồ giả, mock gửi thư, tạo vé JWT).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUserData() {
        // CASCADE kéo theo mọi bảng tham chiếu tới users; skills và skill_categories không bị đụng.
        jdbcTemplate.execute("TRUNCATE users RESTART IDENTITY CASCADE");
    }
}
