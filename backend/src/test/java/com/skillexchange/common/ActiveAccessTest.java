package com.skillexchange.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.skillexchange.IntegrationTestBase;
import com.skillexchange.TestAccounts;
import com.skillexchange.user.Role;
import com.skillexchange.user.UserRepository;
import com.skillexchange.user.UserStatus;

/**
 * Quyền Active (D33, D44): chỉ sinh viên ACTIVE dùng được matching, kiểm ở tầng filter nên 403 đứng trước 400.
 * Bước 8 chỉ có phần /api/matches của TC75, TC173, TC174, TC175, TC178; phần /api/users thêm ở bước 9.
 */
class ActiveAccessTest extends IntegrationTestBase {

    private static final String STUDENT = "sv@student.example.edu.vn";
    private static final String PENDING = "cho@student.example.edu.vn";
    private static final String ADMIN = "qtv@student.example.edu.vn";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private TestAccounts accounts;

    @BeforeEach
    void setUpAccounts() {
        accounts = new TestAccounts(mockMvc, jdbcTemplate, userRepository, passwordEncoder);
    }

    private String pendingToken() throws Exception {
        accounts.create(PENDING, "Chưa xác thực", UserStatus.PENDING_VERIFICATION, Role.STUDENT);
        return accounts.login(PENDING);
    }

    private String adminToken() throws Exception {
        accounts.create(ADMIN, "Quản trị viên", UserStatus.ACTIVE, Role.ADMIN);
        return accounts.login(ADMIN);
    }

    private ResultActions call(String token, String path) throws Exception {
        return mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private static void expectForbidden(ResultActions result) throws Exception {
        result.andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("TC75 tài khoản PENDING_VERIFICATION gọi /matches?type=TWO_WAY: 403")
    void tc75_pendingAccountCannotUseMatches() throws Exception {
        expectForbidden(call(pendingToken(), "/api/matches?type=TWO_WAY"));
    }

    @Test
    @DisplayName("TC173 admin gọi /matches?type=TWO_WAY: 403")
    void tc173_adminCannotUseMatches() throws Exception {
        expectForbidden(call(adminToken(), "/api/matches?type=TWO_WAY"));
    }

    @Test
    @DisplayName("TC174 tài khoản PENDING gọi /matches?type=FOO: 403, không phải 400")
    void tc174_forbiddenBeforeBadRequest() throws Exception {
        expectForbidden(call(pendingToken(), "/api/matches?type=FOO"));
    }

    @Test
    @DisplayName("D44 admin gọi /matches?type=FOO và /matches thiếu type: 403, không phải 400")
    void d44_adminGetsForbiddenEvenWithBadParameters() throws Exception {
        String token = adminToken();
        expectForbidden(call(token, "/api/matches?type=FOO"));
        expectForbidden(call(token, "/api/matches"));
    }

    @Test
    @DisplayName("TC175 không có token gọi /matches: 401")
    void tc175_noTokenIs401() throws Exception {
        mockMvc.perform(get("/api/matches?type=TWO_WAY"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("TC176 vé của tài khoản vừa bị LOCKED gọi /matches?type=TWO_WAY: 403")
    void tc176_lockedAfterLoginIs403() throws Exception {
        long id = accounts.activeStudent(STUDENT, "Sinh viên");
        String token = accounts.login(STUDENT);
        jdbcTemplate.update("UPDATE users SET status = 'LOCKED' WHERE id = ?", id);

        expectForbidden(call(token, "/api/matches?type=TWO_WAY"));
    }

    @Test
    @DisplayName("TC177 tài khoản PENDING_VERIFICATION gọi GET /api/me, /api/skills: vẫn 200")
    void tc177_pendingAccountStillUsesUserApis() throws Exception {
        String token = pendingToken();
        call(token, "/api/me").andExpect(status().isOk());
        call(token, "/api/skills").andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC178 sinh viên ACTIVE gọi /matches?type=TWO_WAY: 200")
    void tc178_activeStudentAllowed() throws Exception {
        accounts.activeStudent(STUDENT, "Sinh viên");
        call(accounts.login(STUDENT), "/api/matches?type=TWO_WAY").andExpect(status().isOk());
    }
}
