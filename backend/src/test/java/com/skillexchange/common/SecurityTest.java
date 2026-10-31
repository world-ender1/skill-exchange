package com.skillexchange.common;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import com.skillexchange.IntegrationTestBase;

/**
 * Cổng bảo mật ở tầng filter (D14, D24): lỗi 401/403 phải là Problem Details.
 * TC62 ở đây chỉ gồm phần API danh mục; /api/me và /api/me/availability làm ở bước 8, 9.
 */
class SecurityTest extends IntegrationTestBase {

    @Test
    @DisplayName("TC62 GET /api/skills, /api/skill-categories không có token: 401 application/problem+json")
    void tc62_catalogWithoutTokenIs401Problem() throws Exception {
        for (String path : new String[] {"/api/skills", "/api/skill-categories"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.instance").value(path))
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("Bearer")));
        }
    }

    @Test
    @DisplayName("D24 token không phải JWT hợp lệ: 401 application/problem+json")
    void garbageTokenIs401Problem() throws Exception {
        mockMvc.perform(get("/api/skills").header(HttpHeaders.AUTHORIZATION, "Bearer khong-phai-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("D24 sinh viên gọi /api/admin/**: 403 application/problem+json")
    void studentCallingAdminApiIs403Problem() throws Exception {
        mockMvc.perform(get("/api/admin/skills"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("D14 /api/auth/** mở cho người chưa đăng nhập: đường dẫn không có thì 404, không phải 401")
    void authPathsArePublic() throws Exception {
        mockMvc.perform(get("/api/auth/khong-ton-tai"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
