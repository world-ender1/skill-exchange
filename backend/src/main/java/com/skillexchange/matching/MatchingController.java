package com.skillexchange.matching;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * GET /api/matches (TK 4.5). Quyền Active: SecurityConfig đã chặn người không phải sinh viên ACTIVE (403)
 * trước khi request tới đây, kể cả khi tham số sai (D44).
 */
@RestController
@RequestMapping("/api/matches")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    /**
     * type bắt buộc: thiếu → 400 "Thiếu tham số", sai (FOO) → 400 qua handleTypeMismatch (D38).
     * limit 1–50, mặc định 10: @Min/@Max trên tham số được Spring kiểm trước khi gọi hàm,
     * sai thì 400 kèm errors[{field: "limit"}] như các lỗi kiểm dữ liệu khác.
     */
    @GetMapping
    public MatchListResponse getMatches(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam MatchType type,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "limit phải từ 1 đến 50")
            @Max(value = 50, message = "limit phải từ 1 đến 50")
            int limit) {
        // "Tôi là ai" lấy từ claim sub của vé, không bao giờ từ tham số (như MeController).
        return matchingService.findMatches(Long.parseLong(jwt.getSubject()), type, limit);
    }
}
