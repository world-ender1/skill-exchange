package com.skillexchange.exchange;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Phiên trao đổi (TK 4.7). Quyền Active: SecurityConfig chặn người không phải sinh viên ACTIVE (403) trước khi
 * tới đây (D87). Controller chỉ nhận request và trả response; luật nằm ở service.
 *
 * {id} kiểu long: "abc" thành 400 "Tham số id có giá trị không hợp lệ: abc" (D38) trước khi gọi service.
 */
@RestController
@RequestMapping("/api/sessions")
public class ExchangeSessionController {

    private final ExchangeSessionService sessionService;

    public ExchangeSessionController(ExchangeSessionService sessionService) {
        this.sessionService = sessionService;
    }

    /** Các phiên mình tham gia (D92). status không bắt buộc; sai giá trị → 400 (D38). */
    @GetMapping
    public List<SessionItem> list(@AuthenticationPrincipal Jwt jwt,
                                  @RequestParam(required = false) SessionStatus status) {
        return sessionService.list(userId(jwt), status);
    }

    @GetMapping("/{id}")
    public SessionDetail get(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return sessionService.get(userId(jwt), id);
    }

    /** Không có body, như nút bấm của giao diện. 200 SessionDetail mới (D93). */
    @PostMapping("/{id}/start")
    public SessionDetail start(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return sessionService.start(userId(jwt), id);
    }

    @PostMapping("/{id}/complete")
    public SessionDetail complete(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return sessionService.complete(userId(jwt), id);
    }

    /** Body {reason} bắt buộc (D88): @RequestBody mặc định required = true, nên không có body là 400. */
    @PostMapping("/{id}/cancel")
    public SessionDetail cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                @Valid @RequestBody ReasonRequest body) {
        return sessionService.cancel(userId(jwt), id, body.reason());
    }

    /** "Tôi là ai" lấy từ claim sub của vé, không bao giờ từ tham số hay body. */
    private static long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
