package com.skillexchange.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillexchange.IntegrationTestBase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Lỗi phát sinh bên trong controller/service phải thành Problem Details (D24).
 * TC73 ở đây chỉ gồm phần lỗi 500; /api/me và login làm ở bước 6, 8.
 */
@WithMockUser
@Import(ErrorHandlingTest.TestOnlyController.class)
class ErrorHandlingTest extends IntegrationTestBase {

    /** Controller chỉ có trong test, để tạo ra từng loại lỗi theo ý muốn. */
    @RestController
    @RequestMapping("/api/test-errors")
    static class TestOnlyController {

        record SampleRequest(@NotBlank String name, @Size(max = 5) String code) {
        }

        @PostMapping("/validate")
        void validate(@Valid @RequestBody SampleRequest request) {
        }

        @GetMapping("/crash")
        void crash() {
            throw new IllegalStateException("password_hash=$2a$10$bimat token_hash=abc123");
        }

        @GetMapping("/db-conflict")
        void dbConflict() {
            throw new DataIntegrityViolationException(
                    "duplicate key value violates unique constraint \"users_email_key\"");
        }

        @GetMapping("/forbidden")
        void forbidden() {
            throw new AccessDeniedException("không phải chủ sở hữu");
        }
    }

    @Test
    @DisplayName("TC73 lỗi 500 giả lập: Problem Details, không lộ stack trace, tên lớp lỗi hay dữ liệu nhạy cảm")
    void tc73_internalErrorDoesNotLeakDetails() throws Exception {
        String body = mockMvc.perform(get("/api/test-errors/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(
                "password_hash", "token_hash", "bimat", "IllegalStateException", "at com.skillexchange", "trace");
    }

    @Test
    @DisplayName("D24 dữ liệu sai: 400, Problem Details có errors[{field, message}]")
    void validationErrorHasFieldList() throws Exception {
        mockMvc.perform(post("/api/test-errors/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"  \", \"code\": \"qua-dai\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("name", "code")))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
    }

    @Test
    @DisplayName("D24 body JSON hỏng: 400 Problem Details, không phải 500")
    void malformedJsonIs400() throws Exception {
        mockMvc.perform(post("/api/test-errors/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("D24 tham số sai kiểu (categoryId=abc): 400 Problem Details")
    void wrongParameterTypeIs400() throws Exception {
        mockMvc.perform(get("/api/skills").param("categoryId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("D24 vi phạm ràng buộc DB lọt tới controller: 409, không lộ tên ràng buộc")
    void dataIntegrityViolationIs409WithoutDetails() throws Exception {
        String body = mockMvc.perform(get("/api/test-errors/db-conflict"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("users_email_key", "duplicate key");
    }

    @Test
    @DisplayName("D24 AccessDeniedException ném trong controller: 403 Problem Details, không thành 500")
    void accessDeniedInsideControllerIs403() throws Exception {
        mockMvc.perform(get("/api/test-errors/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }
}
